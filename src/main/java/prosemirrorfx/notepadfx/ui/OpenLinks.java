package prosemirrorfx.notepadfx.ui;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/**
 * What following a link in a note does: a web or mail address goes to the system's browser or mail program; a path (relative to the
 * note's folder, or absolute, with any #heading or ?query dropped) opens in notepadfx, as a tab for a file or as a workspace for a folder.
 */
final class OpenLinks {

    static void follow(Path base, String address) {
        var a = address.strip();
        var lower = a.toLowerCase();
        if (lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("mailto:")) { browse(a); return; }
        var target = resolve(base, a);
        if (target != null && Files.exists(target)) Windows.open(target);
        else Windows.target().status.flash("Nothing to open at " + a);
    }

    /** The file a link points to, or null when it is not a path we can read. */
    static Path resolve(Path base, String address) {
        try {
            if (address.regionMatches(true, 0, "file:", 0, 5)) return Path.of(URI.create(address.replace(" ", "%20")));
            var cut = address.indexOf('#') >= 0 ? address.substring(0, address.indexOf('#')) : address;
            if (cut.indexOf('?') >= 0) cut = cut.substring(0, cut.indexOf('?'));
            if (cut.isBlank()) return null;
            var p = Path.of(URLDecoder.decode(cut.replace("+", "%2B"), StandardCharsets.UTF_8));
            return p.isAbsolute() ? p : base == null ? null : base.resolve(p).normalize();
        } catch (RuntimeException e) {
            return null;
        }
    }

    static void browse(String address) {
        Thread.ofVirtual().start(() -> {
            try { java.awt.Desktop.getDesktop().browse(URI.create(address)); } catch (Exception e) { System.err.println("notepadfx: could not open " + address + ": " + e); }
        });
    }

    private OpenLinks() {}
}
