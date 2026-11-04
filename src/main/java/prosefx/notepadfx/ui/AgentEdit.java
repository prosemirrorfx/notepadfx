package prosefx.notepadfx.ui;

import prosefx.notepadfx.agent.*;
import prosefx.notepadfx.doc.*;
import java.util.*;

import static prosefx.notepadfx.ui.AgentServer.reply;

/** append, insert and replace: worked out on the live text, refused when the text has moved, applied as one undo step. */
final class AgentEdit {

    static String run(String verb, Doc d, Args a) {
        if (!a.has("text")) return reply(Exit.ERROR, "notepadfx " + verb + " needs the new text: --text, --text-file or --text-stdin\n");
        if (a.has("version") && a.number("version", -1) != d.version.get())
            return reply(Exit.STALE, "the text is at version " + d.version.get() + " now, you read version " + a.get("version") + "; read it again\n");
        var live = d.text();
        var change = switch (verb) {
            case "append" -> Edits.append(live, a.get("text"));
            case "insert" -> insert(live, a);
            case "replace" -> replace(live, a);
            default -> Change.fail(Exit.ERROR, "unknown verb " + verb);
        };
        if (!change.ok()) return reply(change.code(), change.problem() + "\n");
        d.editor().replaceText(change.text());
        var shown = change.lines() == 0 ? "removed lines" : "lines " + change.firstLine() + (change.lines() > 1 ? "-" + (change.firstLine() + change.lines() - 1) : "");
        var w = Windows.windowOf(d);
        if (w != null) w.status.flash("A session changed " + d.name() + ": " + shown + " (Ctrl+Z takes it back)");
        var m = new LinkedHashMap<String, Object>();
        m.put("file", d.path.toString());
        m.put("applied", true);
        m.put("version", d.version.get());
        m.put("first-line", change.firstLine());
        m.put("changed-lines", change.lines());
        m.put("lines", Lines.count(d.text()));
        m.put("unsaved", d.dirty.get());
        return reply(Exit.DONE, AgentFacts.lines(m, a.has("json")));
    }

    static Change insert(String live, Args a) {
        if (a.has("after-heading")) return Edits.insertAfterHeading(live, a.get("after-heading"), a.get("text"));
        if (!a.has("after-line") || !a.has("expect")) return Change.fail(Exit.ERROR, "insert needs --after-heading \"...\", or --after-line N with --expect (the text of that line)");
        return Edits.insertAfterLine(live, a.number("after-line", -1), a.get("expect"), a.get("text"));
    }

    static Change replace(String live, Args a) {
        var r = Args.range(a.get("lines"));
        if (r == null || !a.has("expect")) return Change.fail(Exit.ERROR, "replace needs --lines A-B and --expect (the text of those lines as you read it)");
        return Edits.replace(live, r[0], r[1], a.get("expect"), a.get("text"));
    }

    private AgentEdit() {}
}
