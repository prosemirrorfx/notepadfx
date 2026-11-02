package prosefx.notepadfx.doc;

import java.nio.file.*;
import java.util.concurrent.*;
import javafx.application.Platform;
import org.junit.jupiter.api.*;
import prosefx.notepadfx.sys.ExternalEditor;

import static org.junit.jupiter.api.Assertions.*;

/** A file changed from outside: reloaded when nothing is unsaved, left alone (and remembered) when there are edits. Headless JavaFX. */
class DiskSyncTest {

    @BeforeAll static void boot() throws Exception {
        System.setProperty("glass.platform", "Headless");
        System.setProperty("prism.order", "sw");
        var ready = new CountDownLatch(1);
        try { Platform.startup(ready::countDown); } catch (IllegalStateException again) { ready.countDown(); }
        ready.await(10, TimeUnit.SECONDS);
    }

    static <T> T fx(Callable<T> c) throws Exception {
        var f = new FutureTask<>(c);
        Platform.runLater(f);
        return f.get(15, TimeUnit.SECONDS);
    }

    static void touch(Path p, String text) throws Exception {
        Files.writeString(p, text);
        Files.setLastModifiedTime(p, java.nio.file.attribute.FileTime.fromMillis(System.currentTimeMillis() + 5_000 * (++n)));
    }

    static int n;

    @Test void anUntouchedFileIsTheSame(@org.junit.jupiter.api.io.TempDir Path dir) throws Exception {
        var p = dir.resolve("a.txt");
        Files.writeString(p, "one\n");
        fx(() -> { var d = Docs.of(p); d.editor(); assertEquals(Doc.Disk.SAME, d.syncFromDisk()); return null; });
    }

    @Test void aCleanDocumentIsReloadedAndKeepsItsCaret(@org.junit.jupiter.api.io.TempDir Path dir) throws Exception {
        var p = dir.resolve("b.txt");
        Files.writeString(p, "one two three\n");
        fx(() -> {
            var d = Docs.of(p);
            d.editor().position(4);
            touch(p, "one two three four\n");
            assertEquals(Doc.Disk.RELOADED, d.syncFromDisk());
            assertEquals("one two three four\n", d.text());
            assertEquals(4, d.editor().position());
            assertFalse(d.dirty.get());
            return null;
        });
    }

    @Test void unsavedEditsAreKeptAndTheConflictWaitsForAChoice(@org.junit.jupiter.api.io.TempDir Path dir) throws Exception {
        var p = dir.resolve("c.txt");
        Files.writeString(p, "base\n");
        fx(() -> {
            var d = Docs.of(p);
            d.editor();
            ((javafx.scene.control.TextArea) d.editor().node()).setText("my edit\n");
            assertTrue(d.dirty.get());
            touch(p, "theirs\n");
            assertEquals(Doc.Disk.CONFLICT, d.syncFromDisk());
            assertTrue(d.conflict());
            assertEquals("my edit\n", d.text());
            d.takeDisk();
            assertEquals("theirs\n", d.text());
            assertFalse(d.conflict());
            return null;
        });
    }

    @Test void keepingMineLeavesTheDocumentUnsavedSoSavingOverwrites(@org.junit.jupiter.api.io.TempDir Path dir) throws Exception {
        var p = dir.resolve("d.txt");
        Files.writeString(p, "base\n");
        fx(() -> {
            var d = Docs.of(p);
            d.editor();
            ((javafx.scene.control.TextArea) d.editor().node()).setText("mine\n");
            touch(p, "theirs\n");
            assertEquals(Doc.Disk.CONFLICT, d.syncFromDisk());
            d.keepMine();
            assertTrue(d.dirty.get());
            d.save();
            assertEquals("mine\n", Files.readString(p));
            assertEquals(Doc.Disk.SAME, d.syncFromDisk());
            return null;
        });
    }

    @Test void theOtherEditorCommandTakesArgumentsQuotesAndAPlaceholder() {
        var f = Path.of("C:/x/a b.md");
        assertEquals(java.util.List.of("code.exe", f.toString()), ExternalEditor.words("code.exe", f));
        assertEquals(java.util.List.of("C:/Program Files/x/e.exe", "--reuse", f.toString()), ExternalEditor.words("\"C:/Program Files/x/e.exe\" --reuse", f));
        assertEquals(java.util.List.of("e", "--file=" + f, "-x"), ExternalEditor.words("e --file={file} -x", f));
        assertTrue(ExternalEditor.words("  ", f).isEmpty());
    }

    @Test void theOperatingSystemTellsUsWhenAnotherProgramWritesTheFile(@org.junit.jupiter.api.io.TempDir Path dir) throws Exception {
        var p = dir.resolve("w.md");
        Files.writeString(p, "first\n");
        var told = new CountDownLatch(1);
        var seen = new java.util.concurrent.atomic.AtomicReference<Doc>();
        DiskWatch.onChange(d -> { if (d.name().equals("w.md")) { seen.set(d); told.countDown(); } });
        try {
            var d = fx(() -> Docs.of(p));
            Thread.sleep(300);
            Files.writeString(p, "second, from outside\n");
            assertTrue(told.await(8, TimeUnit.SECONDS), "no change event within 8 s");
            assertSame(d, seen.get());
        } finally {
            DiskWatch.onChange(x -> {});
        }
    }
}
