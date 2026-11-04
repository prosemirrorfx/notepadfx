package prosefx.notepadfx.ui;

import prosefx.notepadfx.agent.*;
import prosefx.notepadfx.doc.*;
import java.util.*;

import static prosefx.notepadfx.ui.AgentServer.reply;

/** What a session may ask about the files that are open: which ones, how one stands, and its live text. */
final class AgentFacts {

    static Map<String, Object> facts(Doc d) {
        var m = new LinkedHashMap<String, Object>();
        var text = d.text();
        m.put("file", d.path.toString());
        m.put("open", true);
        m.put("owner", d.owner() == null ? "none" : d.owner());
        m.put("unsaved", d.dirty.get());
        m.put("version", d.version.get());
        m.put("lines", Lines.count(text));
        return m;
    }

    static String lines(Map<String, ?> facts, boolean json) {
        if (json) return Json.object(facts);
        var sb = new StringBuilder();
        facts.forEach((k, v) -> sb.append(k).append(": ").append(v instanceof Boolean b ? (b ? "yes" : "no") : v).append('\n'));
        return sb.toString();
    }

    static String files(Args a) {
        var docs = Windows.ALL.stream().flatMap(w -> w.tabs.docs().stream()).toList();
        if (a.has("json")) return reply(Exit.DONE, "[" + String.join(",", docs.stream().map(d -> Json.object(facts(d)).strip()).toList()) + "]\n");
        return reply(Exit.DONE, docs.stream().map(d -> d.path + "\n").reduce("", String::concat));
    }

    static String status(Doc d, Args a) {
        var m = facts(d);
        m.put("conflict", d.conflict());
        return reply(Exit.DONE, lines(m, a.has("json")));
    }

    static String notOpen(Args a) {
        var m = new LinkedHashMap<String, Object>();
        m.put("file", a.word(0));
        m.put("open", false);
        return reply(Exit.NOT_OPEN, lines(m, a.has("json")));
    }

    static String read(Doc d, Args a) {
        var text = d.text();
        var n = Lines.count(text);
        var range = a.has("lines") ? Args.range(a.get("lines")) : new int[]{1, Math.max(1, n)};
        if (range == null || range[0] < 1 || range[1] < range[0] || (n > 0 && range[0] > n)) return reply(Exit.ERROR, "lines must be like 5 or 5-9 within 1-" + n + "\n");
        var part = n == 0 ? "" : Lines.slice(text, range[0], Math.min(range[1], n));
        var m = facts(d);
        m.put("from", n == 0 ? 0 : range[0]);
        m.put("to", n == 0 ? 0 : Math.min(range[1], n));
        if (a.has("json")) { m.put("text", part); return reply(Exit.DONE, Json.object(m)); }
        return reply(Exit.DONE, lines(m, false) + "---\n" + part);
    }

    private AgentFacts() {}
}
