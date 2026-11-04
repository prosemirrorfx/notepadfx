package prosefx.notepadfx.editor;

import java.util.concurrent.*;
import javafx.application.Platform;
import org.junit.jupiter.api.*;
import prosefx.core.commands.Commands;

import static org.junit.jupiter.api.Assertions.*;

/** An AI session changes the live text of an editor that holds a person's unsaved typing: the typing and the caret stay, one undo takes the change back. Headless JavaFX. */
class ReplaceTextTest {
    static final String DOC = "# Questions\n\n>> one\n\ntext in the middle\n\n## Two\n\n>> two\n";

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

    @Test void plainTextKeepsTheTypingTheCaretAndUndoesInOneStep() throws Exception {
        fx(() -> {
            var e = new TextEditor();
            e.load(DOC);
            var at = DOC.indexOf("middle") + 3;
            e.area.positionCaret(at);
            e.area.insertText(at, "TYPED");
            e.area.positionCaret(at + 5);
            var typed = e.text();
            e.replaceText(typed + "\n## Three\n\n>> new question\n");
            assertEquals(typed + "\n## Three\n\n>> new question\n", e.text());
            assertEquals(at + 5, e.area.getCaretPosition());
            assertTrue(e.dirty().get());
            e.area.undo();
            assertEquals(typed, e.text());
            return null;
        });
    }

    @Test void plainTextChangeBeforeTheCaretShiftsIt() throws Exception {
        fx(() -> {
            var e = new TextEditor();
            e.load("a\nb\nc\n");
            e.area.positionCaret(4);
            e.replaceText("a\nXX\nb\nc\n");
            assertEquals(7, e.area.getCaretPosition());
            return null;
        });
    }

    @Test void formattedEditorKeepsTheTypingAndTheCaretWhenTheEndGrows() throws Exception {
        fx(() -> {
            var e = new ProseEditor();
            e.load(DOC);
            var model = e.md.view().model();
            var sel = e.md.findAll("middle", new prosefx.core.editor.Find.Options(false, false, false)).getFirst();
            e.md.selectRange(sel.to(), sel.to());
            model.type("TYPED");
            var caret = e.md.caret();
            var typed = e.text();
            assertTrue(typed.contains("middleTYPED"), typed);
            var added = typed + "\n## Three\n\n>> new question\n";
            e.replaceText(added);
            assertEquals(added, e.text());
            assertEquals(caret, e.md.caret(), "the caret moved");
            assertTrue(e.dirty().get());
            model.run(Commands.undo);
            assertEquals(typed, e.text(), "one undo did not take the change back");
            return null;
        });
    }

    @Test void formattedEditorChangesOnlyTheNamedBlock() throws Exception {
        fx(() -> {
            var e = new ProseEditor();
            e.load(DOC);
            var before = e.md.view().model().state().getDoc();
            e.replaceText(DOC.replace(">> one", ">> one, answered"));
            assertEquals(DOC.replace(">> one", ">> one, answered"), e.text());
            var now = e.md.view().model().state().getDoc();
            assertSame(before.getContent().child(0), now.getContent().child(0));
            return null;
        });
    }
}
