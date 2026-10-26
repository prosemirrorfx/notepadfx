package prosemirrorfx.notepadfx.sys;

import java.nio.file.*;
import javafx.scene.input.*;

public final class Shell {
    public static void reveal(Path p) {
        if (p == null) return;
        run(Files.isDirectory(p) ? new String[]{"explorer.exe", p.toString()} : new String[]{"explorer.exe", "/select,", p.toString()});
    }

    public static void terminal(Path p) {
        if (p == null) return;
        var dir = Files.isDirectory(p) ? p : p.getParent();
        var pb = new ProcessBuilder("cmd.exe", "/c", "start", "cmd.exe").directory(dir.toFile());
        try { pb.start(); } catch (Exception e) { System.err.println("notepadfx: terminal: " + e); }
    }

    public static void copy(Path p) {
        if (p == null) return;
        var c = new ClipboardContent();
        c.putString(p.toString());
        Clipboard.getSystemClipboard().setContent(c);
    }

    public static boolean trash(Path p) {
        try {
            return java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().moveToTrash(p.toFile());
        } catch (Exception e) {
            return false;
        }
    }

    static void run(String... cmd) {
        try { new ProcessBuilder(cmd).start(); } catch (Exception e) { System.err.println("notepadfx: " + e); }
    }

    private Shell() {}
}
