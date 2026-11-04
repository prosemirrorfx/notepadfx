package prosefx.notepadfx.ui;

import prosefx.notepadfx.agent.*;
import prosefx.notepadfx.doc.*;
import java.nio.file.*;
import java.util.*;
import javafx.application.Platform;

import static prosefx.notepadfx.ui.AgentServer.reply;

/** Opening a file from the command line: `file:line`, `--line`, `--end-line` (select those lines) and `--owner` (who may close it). */
final class AgentOpen {

    static String verb(Args a) {
        if (a.word(0) == null) return reply(Exit.ERROR, "notepadfx open needs a file\n");
        var p = Path.of(a.word(0));
        if (!Files.isRegularFile(p)) return reply(Exit.NOT_FOUND, "no such file: " + p + "\n");
        var line = a.number("line", 0);
        var end = a.number("end-line", line);
        var was = Docs.find(p).isPresent();
        Windows.open(p);
        var d = Docs.of(p);
        if (!was && a.has("owner")) d.owner(a.get("owner"));
        var n = Lines.count(d.text());
        select(d, line, end);
        var m = new LinkedHashMap<String, Object>();
        m.put("file", d.path.toString());
        m.put("owner", d.owner() == null ? "none" : d.owner());
        m.put("lines", n);
        if (line > n) return reply(Exit.NOT_FOUND, "opened " + d.name() + ", which has " + n + " lines: there is no line " + line + "\n");
        return reply(Exit.DONE, AgentFacts.lines(m, a.has("json")));
    }

    /** After a launch that carried options (the one-way route): owner, and the lines to select. */
    static void after(Path f, boolean wasOpen, List<String> args) {
        var d = Docs.find(f).orElse(null);
        if (d == null) return;
        var owner = option(args, "--owner=");
        if (!wasOpen && owner != null) d.owner(owner);
        var line = number(option(args, "--line="));
        select(d, line, Math.max(line, number(option(args, "--end-line="))));
    }

    /** Selected a moment later: the editor has to be in the window before it can scroll to the lines. */
    static void select(Doc d, int line, int end) {
        if (line < 1) return;
        Platform.runLater(() -> Platform.runLater(() -> {
            var w = Windows.windowOf(d);
            if (w == null) return;
            d.editor().selectLines(line, Math.max(line, end));
            w.stage.toFront();
            w.stage.requestFocus();
        }));
    }

    static String option(List<String> args, String prefix) {
        return args.stream().filter(a -> a.startsWith(prefix)).map(a -> a.substring(prefix.length())).findFirst().orElse(null);
    }

    static int number(String s) {
        try { return s == null ? 0 : Integer.parseInt(s.strip()); } catch (NumberFormatException e) { return 0; }
    }

    private AgentOpen() {}
}
