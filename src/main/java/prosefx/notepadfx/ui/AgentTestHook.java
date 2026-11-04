package prosefx.notepadfx.ui;

import prosefx.notepadfx.agent.*;
import prosefx.notepadfx.doc.*;
import java.nio.file.Path;
import javafx.scene.control.TextArea;
import javafx.scene.input.*;
import prosefx.view.DocView;

import static prosefx.notepadfx.ui.AgentServer.reply;

/** For tests that run a real notepadfx process: type as a person would, press a key in the close question. Only when the process was started with NOTEPADFX_TEST_HOOKS set. */
final class AgentTestHook {

    static boolean on() { return System.getenv("NOTEPADFX_TEST_HOOKS") != null; }

    static String run(String verb, Args a) {
        return switch (verb) {
            case "test-type" -> type(a);
            case "test-key" -> key(a.word(0));
            default -> reply(Exit.ERROR, "unknown test hook " + verb + "\n");
        };
    }

    static String type(Args a) {
        var d = Docs.find(Path.of(a.word(0))).orElse(null);
        if (d == null) return reply(Exit.NOT_OPEN, "not open\n");
        var ed = d.editor();
        var after = a.get("after", "");
        if (ed.node() instanceof TextArea t) t.insertText(t.getText().indexOf(after) + after.length(), a.get("text"));
        else if (ed.node() instanceof DocView v) {
            var hit = v.findAll(after, new prosefx.core.editor.Find.Options(false, false, false)).getFirst();
            v.selectRange(hit.to(), hit.to());
            v.model().type(a.get("text"));
        }
        return reply(Exit.DONE, "typed\n");
    }

    static String key(String name) {
        var p = ClosePrompt.current;
        if (p == null) return reply(Exit.ERROR, "no question is open\n");
        var c = KeyCode.valueOf(name);
        p.stage.getScene().getOnKeyPressed().handle(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", c, false, false, false, false));
        return reply(Exit.DONE, "pressed " + name + "\n");
    }

    private AgentTestHook() {}
}
