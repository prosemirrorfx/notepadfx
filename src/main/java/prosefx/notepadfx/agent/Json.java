package prosefx.notepadfx.agent;

import java.util.*;

/** Just enough JSON to answer a verb with --json: one flat object. */
public final class Json {

    public static String object(Map<String, ?> fields) {
        var sb = new StringBuilder("{");
        fields.forEach((k, v) -> sb.append(sb.length() > 1 ? "," : "").append(str(k)).append(':').append(v instanceof Number || v instanceof Boolean || v == null ? String.valueOf(v) : str(v.toString())));
        return sb.append("}\n").toString();
    }

    static String str(String s) {
        var sb = new StringBuilder("\"");
        for (var c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c < 0x20 ? String.format("\\u%04x", (int) c) : String.valueOf(c));
            }
        }
        return sb.append('"').toString();
    }

    private Json() {}
}
