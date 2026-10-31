package prosemirrorfx.notepadfx.sys;

import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Opens a file in another editor, chosen in Settings. The setting is a command line: the program and any arguments, with
 * "{file}" where the path goes (at the end when it is not written). `code.exe` alone is enough for vscode.
 */
public final class ExternalEditor {

    public static final String DEFAULT = "code.exe";
    static final Pattern WORD = Pattern.compile("\"([^\"]*)\"|(\\S+)");

    /** The command split into its words, quotes removed, with the file in place; empty when the command is blank. */
    public static List<String> words(String command, Path file) {
        var out = new ArrayList<String>();
        var m = WORD.matcher(command == null ? "" : command);
        var placed = false;
        while (m.find()) {
            var w = m.group(1) != null ? m.group(1) : m.group(2);
            if (w.contains("{file}")) { placed = true; w = w.replace("{file}", file.toString()); }
            out.add(w);
        }
        if (!out.isEmpty() && !placed) out.add(file.toString());
        return out;
    }

    /** Starts the editor; null when it started, otherwise what went wrong. */
    public static String open(String command, Path file) {
        var cmd = words(command == null || command.isBlank() ? DEFAULT : command, file);
        try {
            new ProcessBuilder(cmd).directory(file.getParent().toFile()).start();
            return null;
        } catch (Exception e) {
            return "Could not start " + cmd.getFirst() + ": " + e.getMessage();
        }
    }

    private ExternalEditor() {}
}
