package prosefx.notepadfx.ui;

import java.nio.file.*;
import java.util.concurrent.*;
import javafx.application.Platform;
import org.junit.jupiter.api.*;
import prosefx.notepadfx.state.*;

import static org.junit.jupiter.api.Assertions.*;

/** Opening a file, from every route the app has after the file is chosen: Windows.open, a command-line argument, the tab menu actions. Headless JavaFX. */
class OpenFileTest {

    static MainWindow w;

    @BeforeAll static void boot() throws Exception {
        System.setProperty("glass.platform", "Headless");
        System.setProperty("prism.order", "sw");
        var ready = new CountDownLatch(1);
        try { Platform.startup(ready::countDown); } catch (IllegalStateException again) { ready.countDown(); }
        ready.await(10, TimeUnit.SECONDS);
        fx(() -> { Windows.session = new Session(); Windows.standalone = true; w = new MainWindow(new WindowState()); Windows.ALL.add(w); w.stage.show(); return null; });
    }

    static <T> T fx(Callable<T> c) throws Exception {
        var f = new FutureTask<>(c);
        Platform.runLater(f);
        return f.get(15, TimeUnit.SECONDS);
    }

    static void settle() throws Exception { for (var k = 0; k < 3; k++) { fx(() -> { w.root.applyCss(); w.root.layout(); return null; }); Thread.sleep(50); } }

    @Test void aChosenFileOpensInATabAndShowsItsText(@org.junit.jupiter.api.io.TempDir Path dir) throws Exception {
        var p = dir.resolve("note.md");
        Files.writeString(p, "# Hello\n\n>> a comment\n");
        fx(() -> { Windows.open(p); return null; });
        settle();
        assertTrue(fx(() -> w.tabs.docs().stream().anyMatch(d -> d.path.equals(p.toAbsolutePath().normalize()))), "no tab for the file");
        assertEquals(p.toAbsolutePath().normalize(), fx(() -> w.active.get().path));
        assertTrue(fx(() -> w.active.get().text()).contains("a comment"));
        assertNotNull(fx(() -> w.area.node.lookup(".pm-view")), "the editor is not in the window");
    }

    @Test void aTextFileAndAFileWithAnotherExtensionOpenToo(@org.junit.jupiter.api.io.TempDir Path dir) throws Exception {
        var t = dir.resolve("a.txt");
        var j = dir.resolve("data.jsonl");
        Files.writeString(t, "plain\n");
        Files.writeString(j, "{\"a\":1}\n");
        fx(() -> { Windows.open(t); Windows.open(j); return null; });
        settle();
        assertEquals(j.toAbsolutePath().normalize(), fx(() -> w.active.get().path));
        assertEquals("{\"a\":1}\n", fx(() -> w.active.get().text()));
    }

    @Test void openingAnOpenFileJustShowsItAgain(@org.junit.jupiter.api.io.TempDir Path dir) throws Exception {
        var a = dir.resolve("one.md");
        var b = dir.resolve("two.md");
        Files.writeString(a, "one\n");
        Files.writeString(b, "two\n");
        fx(() -> { Windows.open(a); Windows.open(b); Windows.open(a); return null; });
        settle();
        assertEquals(a.toAbsolutePath().normalize(), fx(() -> w.active.get().path));
        assertEquals(1, fx(() -> w.tabs.docs().stream().filter(d -> d.path.equals(a.toAbsolutePath().normalize())).count()));
    }

    @Test void aFileWithASpaceAndANonAsciiNameOpens(@org.junit.jupiter.api.io.TempDir Path dir) throws Exception {
        var p = dir.resolve("заметки notes (1).md");
        Files.writeString(p, "text\n");
        fx(() -> { Windows.open(p); return null; });
        settle();
        assertEquals("text\n", fx(() -> w.active.get().text()));
    }

    @Test void aRelativeLinkInANoteOpensTheOtherNoteInATab(@org.junit.jupiter.api.io.TempDir Path dir) throws Exception {
        var main = dir.resolve("main.md");
        var other = dir.resolve("sub folder/other.md");
        Files.createDirectories(other.getParent());
        Files.writeString(other, "# Other\n");
        Files.writeString(main, "see [the other](<sub folder/other.md#top>) here\n");
        fx(() -> { Windows.open(main); return null; });
        settle();
        fx(() -> {
            var view = (prosefx.view.DocView) w.active.get().editor().node();
            view.model().select(prosefx.core.pm.Pm.cursor(view.model().state().getDoc(), 1 + "see [the oth".length() - 1));
            assertTrue(view.activate());
            return null;
        });
        settle();
        assertEquals(other.toAbsolutePath().normalize(), fx(() -> w.active.get().path));
    }

    @Test void linkAddressesAreResolvedAgainstTheNotesFolder() {
        var base = Path.of("C:/notes/a");
        assertEquals(Path.of("C:/notes/b/x y.md"), OpenLinks.resolve(base, "../b/x%20y.md#heading"));
        assertEquals(Path.of("C:/notes/a/x.md"), OpenLinks.resolve(base, "x.md?raw=1"));
        assertNull(OpenLinks.resolve(base, "#only-a-heading"));
        assertNull(OpenLinks.resolve(null, "relative.md"));
    }

    /** Failed before: the status bar made the editor first, so the set-up for a newly shown file (line numbers, caret, folds) was skipped. */
    @Test void aNewlyOpenedFileGetsTheLineNumbersSetting(@org.junit.jupiter.api.io.TempDir Path dir) throws Exception {
        var p = dir.resolve("numbered.md");
        Files.writeString(p, "# a\n\ntext\n");
        fx(() -> { Windows.session.lineNumbers(true); Windows.open(p); return null; });
        settle();
        try {
            assertTrue(fx(() -> ((prosefx.view.DocView) w.active.get().editor().node()).lineNumbers()));
        } finally {
            fx(() -> { Windows.session.lineNumbers(false); return null; });
        }
    }

    /** Each action once: the editor's own source and copy-all buttons are left out, because the app bar has them. */
    @Test void theToolbarHasNoRepeatedButtons(@org.junit.jupiter.api.io.TempDir Path dir) throws Exception {
        var p = dir.resolve("bar.md");
        Files.writeString(p, "# Bar\n\n| a | b |\n|---|---|\n| x | y |\n\ntext\n");
        fx(() -> { Windows.open(p); return null; });
        settle();
        var tips = fx(() -> { var out = new java.util.ArrayList<String>(); tooltips(w.stage.getScene().getRoot(), out); return out; });
        assertEquals(1, tips.stream().filter(t -> t.toLowerCase().contains("source")).count(), "source buttons: " + tips);
        assertEquals(0, tips.stream().filter(t -> t.startsWith("Copy all")).count(), "the editor's Copy all: " + tips);
        for (var want : java.util.List.of("Strikethrough", "Highlight", "Inline code", "Code block", "Table"))
            assertTrue(tips.stream().anyMatch(t -> t.startsWith(want)), want + " missing: " + tips);
        var shot = System.getProperty("shot");
        if (shot != null) {
            var img = fx(() -> { w.stage.setWidth(1200); w.stage.setHeight(520); w.root.applyCss(); w.root.layout(); return null; });
            settle();
            var snap = fx(() -> w.stage.getScene().snapshot(null));
            int iw = (int) snap.getWidth(), ih = (int) snap.getHeight();
            var out = new java.awt.image.BufferedImage(iw, ih, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            var r = snap.getPixelReader();
            for (var y = 0; y < ih; y++) for (var x = 0; x < iw; x++) out.setRGB(x, y, r.getArgb(x, y));
            javax.imageio.ImageIO.write(out, "png", new java.io.File(shot));
        }
    }

    static void tooltips(javafx.scene.Node n, java.util.List<String> out) {
        if (n instanceof javafx.scene.control.Control c && c.getTooltip() != null && c.isVisible()) out.add(c.getTooltip().getText());
        if (n instanceof javafx.scene.Parent p) for (var k : p.getChildrenUnmodifiable()) tooltips(k, out);
    }
}
