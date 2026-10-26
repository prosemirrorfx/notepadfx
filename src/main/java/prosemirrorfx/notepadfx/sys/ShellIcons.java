package prosemirrorfx.notepadfx.sys;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.scene.image.*;

public final class ShellIcons {
    static final Set<String> PER_FILE = Set.of(".exe", ".lnk", ".ico", ".url", ".appref-ms");
    static final Map<String, Image> CACHE = new ConcurrentHashMap<>();
    static final Map<String, List<Consumer<Image>>> WAITING = new HashMap<>();
    static final boolean WINDOWS = System.getProperty("os.name", "").startsWith("Windows");
    static ExecutorService worker;

    public static Image cached(Path p) { return CACHE.get(key(p)); }

    public static void request(Path p, Consumer<Image> done) {
        if (!WINDOWS) return;
        var key = key(p);
        var hit = CACHE.get(key);
        if (hit != null) { done.accept(hit); return; }
        var waiting = WAITING.get(key);
        if (waiting != null) { waiting.add(done); return; }
        WAITING.put(key, new ArrayList<>(List.of(done)));
        worker().submit(() -> {
            var image = Win32Icons.load(p, Files.isDirectory(p), PER_FILE.contains(ext(p)));
            Platform.runLater(() -> {
                if (image != null) CACHE.put(key, image);
                var list = WAITING.remove(key);
                if (image != null && list != null) list.forEach(c -> c.accept(image));
            });
        });
    }

    static String key(Path p) {
        if (Files.isDirectory(p)) return p.getParent() == null ? "drive:" + p : "<dir>";
        var e = ext(p);
        return PER_FILE.contains(e) ? p.toString().toLowerCase() : e;
    }

    static String ext(Path p) {
        var n = p.getFileName() == null ? p.toString() : p.getFileName().toString().toLowerCase();
        var dot = n.lastIndexOf('.');
        return dot < 0 ? "<none>" : n.substring(dot);
    }

    static synchronized ExecutorService worker() {
        if (worker == null) worker = Executors.newSingleThreadExecutor(r -> Thread.ofPlatform().daemon().name("shell-icons").unstarted(r));
        return worker;
    }

    private ShellIcons() {}
}
