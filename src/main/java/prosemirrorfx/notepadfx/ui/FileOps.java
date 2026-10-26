package prosemirrorfx.notepadfx.ui;

import java.io.IOException;
import java.nio.file.*;

final class FileOps {
    static void duplicate(MainWindow w, Path p, Runnable refresh) {
        if (p == null) return;
        try {
            var copy = freeName(p);
            if (Files.isDirectory(p)) copyTree(p, copy); else Files.copy(p, copy);
            refresh.run();
        } catch (Exception e) {
            Prompts.error(w, "Could not duplicate " + p.getFileName(), e);
        }
    }

    static void move(MainWindow w, Path p, Runnable refresh) {
        if (p == null) return;
        var dir = Prompts.chooseDir(w, "Move " + p.getFileName() + " to");
        if (dir == null) return;
        var target = dir.resolve(p.getFileName());
        if (Files.exists(target)) { Prompts.error(w, target + " already exists", null); return; }
        try {
            Files.move(p, target);
            refresh.run();
        } catch (Exception e) {
            Prompts.error(w, "Could not move " + p.getFileName(), e);
        }
    }

    static Path freeName(Path p) {
        var name = p.getFileName().toString();
        var dot = Files.isDirectory(p) ? -1 : name.lastIndexOf('.');
        var base = dot > 0 ? name.substring(0, dot) : name;
        var ext = dot > 0 ? name.substring(dot) : "";
        for (var i = 1; ; i++) {
            var c = p.resolveSibling(base + " copy" + (i == 1 ? "" : " " + i) + ext);
            if (!Files.exists(c)) return c;
        }
    }

    static void copyTree(Path from, Path to) throws IOException {
        try (var walk = Files.walk(from)) {
            for (var src : (Iterable<Path>) walk::iterator) {
                var dst = to.resolve(from.relativize(src).toString());
                if (Files.isDirectory(src)) Files.createDirectories(dst); else Files.copy(src, dst);
            }
        }
    }

    private FileOps() {}
}
