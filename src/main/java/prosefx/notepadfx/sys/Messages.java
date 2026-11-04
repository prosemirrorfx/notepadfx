package prosefx.notepadfx.sys;

import java.nio.file.Path;
import java.util.*;

public final class Messages {
    static final String SEP = "\u001f", AGENT = "@agent";

    public static String encode(List<String> args) {
        return String.join(SEP, args.stream().map(a -> a.startsWith("-") ? a : Path.of(a).toAbsolutePath().toString()).toList());
    }

    public static List<String> decode(String line) {
        return line.isEmpty() ? List.of() : List.of(line.split(SEP, -1));
    }

    /** A request from a command-line verb that wants an answer: arguments exactly as given (text may hold anything), one line. */
    public static String pack(List<String> args) {
        return AGENT + SEP + String.join(SEP, args.stream().map(Messages::escape).toList());
    }

    public static boolean isAgent(List<String> decoded) { return !decoded.isEmpty() && decoded.getFirst().equals(AGENT); }

    public static List<String> unpack(List<String> decoded) { return decoded.subList(1, decoded.size()).stream().map(Messages::unescape).toList(); }

    static String escape(String s) { return s.replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "\\r").replace(SEP, "\\u"); }

    static String unescape(String s) {
        var out = new StringBuilder();
        for (var i = 0; i < s.length(); i++) {
            var c = s.charAt(i);
            if (c != '\\' || i == s.length() - 1) { out.append(c); continue; }
            var n = s.charAt(++i);
            out.append(n == 'n' ? '\n' : n == 'r' ? '\r' : n == 'u' ? SEP.charAt(0) : n);
        }
        return out.toString();
    }

    private Messages() {}
}
