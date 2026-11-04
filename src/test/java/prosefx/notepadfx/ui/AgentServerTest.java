package prosefx.notepadfx.ui;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import javafx.application.Platform;
import javafx.scene.control.TextArea;
import javafx.scene.input.*;
import org.junit.jupiter.api.*;
import prosefx.notepadfx.doc.Docs;
import prosefx.notepadfx.state.*;
import prosefx.view.DocView;

import static org.junit.jupiter.api.Assertions.*;

/** The command-line verbs as the running notepadfx answers them: sync edits over a person's unsaved typing, ownership, and the close question. Headless JavaFX. */
class AgentServerTest {
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

    /** Asks the way the socket does; returns {code, text}. */
    static String[] ask(String... args) throws Exception {
        var r = AgentServer.request(List.of(args)).get(20, TimeUnit.SECONDS);
        var nl = r.indexOf('\n');
        return new String[]{r.substring(0, nl), r.substring(nl + 1)};
    }

    static void key(KeyCode c) throws Exception {
        fx(() -> { var s = ClosePrompt.current.stage.getScene(); s.getOnKeyPressed().handle(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", c, false, false, false, false)); return null; });
    }

    static void waitForPrompt() throws Exception {
        for (var i = 0; i < 100 && fx(() -> ClosePrompt.current == null); i++) Thread.sleep(50);
        assertNotNull(fx(() -> ClosePrompt.current), "no question was put to the person");
    }

    static void typeInto(Path p, String text) throws Exception {
        fx(() -> {
            var ed = Docs.of(p).editor();
            if (ed.node() instanceof TextArea a) a.insertText(a.getText().indexOf("middle") + 6, text);
            else { var f = ((DocView) ed.node()); var hit = f.findAll("middle", new prosefx.core.editor.Find.Options(false, false, false)).getFirst(); f.selectRange(hit.to(), hit.to()); f.model().type(text); }
            return null;
        });
    }

    static Path file(Path dir, String name) throws Exception {
        var p = dir.resolve(name);
        Files.writeString(p, "# Questions\n\n>> one\n\ntext in the middle\n\n## Two\n\n>> two\n");
        return p.toAbsolutePath().normalize();
    }

    @Test void readGivesTheLiveTextAndItsVersion(@org.junit.jupiter.api.io.TempDir Path dir) throws Exception {
        var p = file(dir, "a.md");
        assertEquals("0", ask("open", p.toString(), "--owner=me")[0]);
        var r = ask("read", p.toString(), "--lines=3-3");
        assertEquals("0", r[0]);
        assertTrue(r[1].contains("lines: 9") && r[1].endsWith("---\n>> one\n"), r[1]);
        assertTrue(ask("read", p.toString(), "--json")[1].contains("\"owner\":\"me\""));
    }

    @Test void anAppendKeepsThePersonsUnsavedTypingInTheMiddleOfAMarkdownFile(@org.junit.jupiter.api.io.TempDir Path dir) throws Exception {
        var p = file(dir, "b.md");
        ask("open", p.toString());
        typeInto(p, "TYPED");
        var before = ask("read", p.toString())[1];
        assertTrue(before.contains("middleTYPED") && before.contains("unsaved: yes"), before);
        var done = ask("append", p.toString(), "--text=\n## Three\n\n>> new question\n");
        assertEquals("0", done[0], done[1]);
        var now = fx(() -> Docs.of(p).text());
        assertTrue(now.contains("middleTYPED") && now.endsWith(">> two\n\n## Three\n\n>> new question\n"), now);
        assertEquals("# Questions\n\n>> one\n\ntext in the middle\n\n## Two\n\n>> two\n", Files.readString(p), "the file on disk was touched");
    }

    @Test void anAppendKeepsThePersonsUnsavedTypingInAPlainTextFile(@org.junit.jupiter.api.io.TempDir Path dir) throws Exception {
        var p = file(dir, "c.txt");
        ask("open", p.toString());
        typeInto(p, "TYPED");
        assertEquals("0", ask("append", p.toString(), "--text=more\n")[0]);
        var now = fx(() -> Docs.of(p).text());
        assertTrue(now.contains("middleTYPED") && now.endsWith(">> two\nmore\n"), now);
    }

    @Test void aChangeThatNamesTextThatMovedIsRefusedAndNothingChanges(@org.junit.jupiter.api.io.TempDir Path dir) throws Exception {
        var p = file(dir, "d.md");
        ask("open", p.toString());
        var before = fx(() -> Docs.of(p).text());
        var r = ask("replace", p.toString(), "--lines=3-3", "--expect=>> not what is there", "--text=x");
        assertEquals("5", r[0]);
        assertEquals(before, fx(() -> Docs.of(p).text()));
        var v = ask("insert", p.toString(), "--after-heading=## Gone", "--text=x");
        assertEquals("5", v[0]);
        var old = ask("append", p.toString(), "--version=-5", "--text=x");
        assertEquals("5", old[0]);
        assertEquals(before, fx(() -> Docs.of(p).text()));
    }

    @Test void aChangeThatNamesTheRightTextIsMade(@org.junit.jupiter.api.io.TempDir Path dir) throws Exception {
        var p = file(dir, "e.md");
        ask("open", p.toString());
        var r = ask("replace", p.toString(), "--lines=3-3", "--expect=>> one", "--text=>> one, answered");
        assertEquals("0", r[0], r[1]);
        assertTrue(fx(() -> Docs.of(p).text()).contains(">> one, answered\n"));
    }

    @Test void theOwnerClosesItsOwnCleanFileAndNobodyElseDoes(@org.junit.jupiter.api.io.TempDir Path dir) throws Exception {
        var p = file(dir, "f.md");
        ask("open", p.toString(), "--owner=me");
        assertEquals("4", ask("close", p.toString(), "--owner=other", "--no-ask")[0]);
        assertEquals("4", ask("close", p.toString(), "--no-ask")[0]);
        assertTrue(fx(() -> Docs.find(p).isPresent()));
        assertEquals("0", ask("close", p.toString(), "--owner=me")[0]);
        assertFalse(fx(() -> Docs.find(p).isPresent()));
        assertEquals("3", ask("status", p.toString())[0]);
    }

    @Test void aFileAPersonOpenedHasNoOwnerAndEveryCloseIsAQuestion(@org.junit.jupiter.api.io.TempDir Path dir) throws Exception {
        var p = file(dir, "g.md");
        fx(() -> { Windows.open(p); return null; });
        assertTrue(ask("status", p.toString())[1].contains("owner: none"));
        var pending = CompletableFuture.supplyAsync(() -> { try { return ask("close", p.toString(), "--owner=me"); } catch (Exception e) { throw new RuntimeException(e); } });
        waitForPrompt();
        key(KeyCode.ENTER);
        Thread.sleep(300);
        assertFalse(pending.isDone(), "Enter closed the file inside the first two seconds");
        key(KeyCode.ESCAPE);
        assertEquals("4", pending.get(5, TimeUnit.SECONDS)[0]);
        assertTrue(fx(() -> Docs.find(p).isPresent()), "Esc did not keep the file open");
    }

    @Test void aSessionAsksAndEnterAfterTheCountdownCloses(@org.junit.jupiter.api.io.TempDir Path dir) throws Exception {
        var p = file(dir, "h.md");
        fx(() -> { Windows.open(p); return null; });
        var pending = CompletableFuture.supplyAsync(() -> { try { return ask("close", p.toString(), "--owner=someone"); } catch (Exception e) { throw new RuntimeException(e); } });
        waitForPrompt();
        Thread.sleep(2300);
        key(KeyCode.ENTER);
        assertEquals("0", pending.get(5, TimeUnit.SECONDS)[0]);
        assertFalse(fx(() -> Docs.find(p).isPresent()));
    }

    @Test void unsavedChangesAreNeverLostWithoutTheirAnswer(@org.junit.jupiter.api.io.TempDir Path dir) throws Exception {
        var p = file(dir, "i.txt");
        ask("open", p.toString(), "--owner=me");
        typeInto(p, "TYPED");
        assertEquals("6", ask("close", p.toString(), "--owner=me", "--no-ask")[0]);
        var pending = CompletableFuture.supplyAsync(() -> { try { return ask("close", p.toString(), "--owner=me"); } catch (Exception e) { throw new RuntimeException(e); } });
        waitForPrompt();
        key(KeyCode.ESCAPE);
        assertEquals("4", pending.get(5, TimeUnit.SECONDS)[0]);
        assertTrue(fx(() -> Docs.of(p).text()).contains("middleTYPED"));
        var again = CompletableFuture.supplyAsync(() -> { try { return ask("close", p.toString(), "--owner=me"); } catch (Exception e) { throw new RuntimeException(e); } });
        waitForPrompt();
        Thread.sleep(2300);
        key(KeyCode.ENTER);
        assertEquals("0", again.get(5, TimeUnit.SECONDS)[0]);
        assertTrue(Files.readString(p).contains("middleTYPED"), "Save was not done before closing");
    }

    @Test void discardIsItsOwnKey(@org.junit.jupiter.api.io.TempDir Path dir) throws Exception {
        var p = file(dir, "j.txt");
        ask("open", p.toString(), "--owner=me");
        typeInto(p, "TYPED");
        var pending = CompletableFuture.supplyAsync(() -> { try { return ask("close", p.toString(), "--owner=me"); } catch (Exception e) { throw new RuntimeException(e); } });
        waitForPrompt();
        Thread.sleep(2300);
        key(KeyCode.F9);
        assertEquals("0", pending.get(5, TimeUnit.SECONDS)[0]);
        assertFalse(Files.readString(p).contains("TYPED"));
    }

    @Test void noAnswerInTimeLeavesTheFileOpen(@org.junit.jupiter.api.io.TempDir Path dir) throws Exception {
        var p = file(dir, "k.md");
        fx(() -> { Windows.open(p); return null; });
        var r = ask("close", p.toString(), "--owner=x", "--timeout=3");
        assertEquals("7", r[0]);
        assertTrue(fx(() -> Docs.find(p).isPresent()));
    }

    @Test void openingAtALineSelectsItAndAMissingLineIsReported(@org.junit.jupiter.api.io.TempDir Path dir) throws Exception {
        var p = file(dir, "l.txt");
        assertEquals("0", ask("open", p.toString(), "--line=3", "--end-line=4")[0]);
        Thread.sleep(300);
        assertEquals(">> one\n", fx(() -> ((TextArea) Docs.of(p).editor().node()).getSelectedText()));
        assertEquals("8", ask("open", p.toString(), "--line=99")[0]);
        assertEquals("8", ask("open", dir.resolve("nope.md").toString())[0]);
    }
}
