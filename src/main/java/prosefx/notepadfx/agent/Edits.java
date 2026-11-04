package prosefx.notepadfx.agent;

import java.util.ArrayList;

/**
 * The three changes an AI session may ask for, worked out on the text as it is now. Each one names what it expects to
 * find; when the text has moved the change is refused (Exit.STALE) and nothing is touched, so the session reads again
 * and retries. Nothing here is applied blindly over what a person typed.
 */
public final class Edits {

    /** Adds to the end. The end cannot have moved under the writer, so nothing is expected. */
    public static Change append(String live, String text) {
        var open = live.isEmpty() || live.endsWith("\n");
        var sep = open ? "" : "\n";
        return splice(live, live.length(), live.length(), sep + (open && !text.endsWith("\n") ? text + "\n" : text), sep.length());
    }

    /** Adds after line N (0 = at the very start); expect is the text line N holds now. */
    public static Change insertAfterLine(String live, int line, String expect, String text) {
        var n = Lines.count(live);
        if (line < 0 || line > n) return Change.fail(Exit.STALE, "the text has " + n + " lines now, there is no line " + line);
        if (line > 0) {
            var now = Lines.chomp(Lines.slice(live, line, line));
            if (!now.equals(Lines.chomp(expect))) return Change.fail(Exit.STALE, "line " + line + " now reads: " + now);
        }
        var at = Lines.start(live, line + 1);
        var sep = at == live.length() && !live.isEmpty() && !live.endsWith("\n") ? "\n" : "";
        return splice(live, at, at, sep + (text.endsWith("\n") ? text : text + "\n"), sep.length());
    }

    /** Adds right after the line that holds this heading (with or without its # marks); it must be there exactly once. */
    public static Change insertAfterHeading(String live, String heading, String text) {
        var all = live.split("\n", -1);
        var found = new ArrayList<Integer>();
        var want = heading.strip();
        for (var i = 0; i < all.length; i++) if (isHeading(all[i].strip(), want)) found.add(i + 1);
        if (found.isEmpty()) return Change.fail(Exit.STALE, "no heading reads: " + heading);
        if (found.size() > 1) return Change.fail(Exit.STALE, "the heading appears on lines " + found + ", name it by line instead");
        return insertAfterLine(live, found.getFirst(), all[found.getFirst() - 1], text);
    }

    static boolean isHeading(String line, String want) {
        if (line.equals(want)) return true;
        return !want.startsWith("#") && line.startsWith("#") && line.replaceFirst("^#{1,6}\\s+", "").equals(want);
    }

    /** Replaces lines from..to (from 1, inclusive), only if they still read exactly what the writer saw. An empty text deletes them. */
    public static Change replace(String live, int from, int to, String expect, String text) {
        var n = Lines.count(live);
        if (from < 1 || to < from || to > n) return Change.fail(Exit.STALE, "the text has " + n + " lines now, there are no lines " + from + "-" + to);
        var now = Lines.slice(live, from, to);
        if (!Lines.chomp(now).equals(Lines.chomp(expect))) return Change.fail(Exit.STALE, "lines " + from + "-" + to + " now read: " + Lines.chomp(now));
        var body = text.isEmpty() || text.endsWith("\n") || !now.endsWith("\n") ? text : text + "\n";
        return splice(live, Lines.start(live, from), Lines.start(live, to + 1), body, 0);
    }

    /** skip: how many leading characters of insert are a separator the edit added, not part of what was asked for. */
    static Change splice(String live, int from, int to, String insert, int skip) {
        var text = live.substring(0, from) + insert + live.substring(to);
        return new Change().text(text).firstLine(Lines.at(text, from + skip)).lines(Lines.count(insert.substring(skip)));
    }

    private Edits() {}
}
