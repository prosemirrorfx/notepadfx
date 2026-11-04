package prosefx.notepadfx.agent;

/** Lines of a text the way an editor counts them: from 1, a final newline does not start another line. */
public final class Lines {

    public static int count(String t) {
        if (t.isEmpty()) return 0;
        var n = 1;
        for (var i = 0; i < t.length() - 1; i++) if (t.charAt(i) == '\n') n++;
        return n;
    }

    /** Offset where line (from 1) starts; count+1 gives the end of the text. */
    public static int start(String t, int line) {
        var at = 0;
        for (var l = 1; l < line && at < t.length(); l++) {
            var nl = t.indexOf('\n', at);
            at = nl < 0 ? t.length() : nl + 1;
        }
        return at;
    }

    /** Lines from..to (from 1, inclusive) with their newlines. */
    public static String slice(String t, int from, int to) { return t.substring(start(t, from), start(t, to + 1)); }

    /** Line number (from 1) holding the offset. */
    public static int at(String t, int offset) {
        var n = 1;
        for (var i = 0; i < offset && i < t.length(); i++) if (t.charAt(i) == '\n') n++;
        return n;
    }

    /** Without the newlines at the end, so a text and the same text with its last newline compare equal. */
    public static String chomp(String s) {
        var e = s.length();
        while (e > 0 && s.charAt(e - 1) == '\n') e--;
        return s.substring(0, e);
    }

    private Lines() {}
}
