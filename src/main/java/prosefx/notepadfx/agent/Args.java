package prosefx.notepadfx.agent;

import java.util.*;

/** `--name value`, `--name=value`, flags and positional words of one verb. */
public final class Args {
    public static final Set<String> VALUED = Set.of("owner", "lines", "text", "text-file", "expect", "expect-file", "version", "after-line", "after-heading", "timeout", "line", "end-line", "col");

    final Map<String, String> opts = new LinkedHashMap<>();
    final List<String> words = new ArrayList<>();

    public static Args parse(List<String> raw) {
        var a = new Args();
        for (var i = 0; i < raw.size(); i++) {
            var t = raw.get(i);
            if (!t.startsWith("--") || t.length() < 3) { a.words.add(t); continue; }
            var eq = t.indexOf('=');
            if (eq > 0) a.opts.put(t.substring(2, eq), t.substring(eq + 1));
            else if (VALUED.contains(t.substring(2)) && i + 1 < raw.size()) a.opts.put(t.substring(2), raw.get(++i));
            else a.opts.put(t.substring(2), "");
        }
        return a;
    }

    public boolean has(String name) { return opts.containsKey(name); }

    public String get(String name) { return opts.get(name); }

    public String get(String name, String otherwise) { return opts.getOrDefault(name, otherwise); }

    public int number(String name, int otherwise) {
        try { return opts.containsKey(name) ? Integer.parseInt(opts.get(name).strip()) : otherwise; } catch (NumberFormatException e) { return otherwise; }
    }

    public List<String> words() { return words; }

    public String word(int i) { return i < words.size() ? words.get(i) : null; }

    /** The options as a request line: every one as --name=value, so the other end needs no knowledge of which take a value. */
    public List<String> asRequest(String verb) {
        var out = new ArrayList<String>();
        out.add(verb);
        out.addAll(words);
        opts.forEach((k, v) -> out.add("--" + k + "=" + v));
        return out;
    }

    /** From, to of "5-9" or "5"; null when it is neither. */
    public static int[] range(String s) {
        if (s == null) return null;
        var m = java.util.regex.Pattern.compile("\\s*(\\d+)\\s*(?:-\\s*(\\d+)\\s*)?").matcher(s);
        if (!m.matches()) return null;
        var from = Integer.parseInt(m.group(1));
        return new int[]{from, m.group(2) == null ? from : Integer.parseInt(m.group(2))};
    }
}
