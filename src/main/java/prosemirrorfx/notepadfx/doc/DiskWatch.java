package prosemirrorfx.notepadfx.doc;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import javafx.application.Platform;

import static java.nio.file.StandardWatchEventKinds.*;

/**
 * Tells the app when an open file is changed from outside. One WatchService (the operating system calls back, so nothing
 * polls) watching the folder of each open file; an editor that saves by writing a temporary file and renaming it shows up
 * as a created file, so both kinds of event count. A burst of events is gathered into one call, on the JavaFX thread.
 * Folders where the system sends no events (some network drives) are covered by checkAll(), which the window calls when
 * it gets the focus back.
 */
public final class DiskWatch {

    static WatchService service;
    static final Map<Path, WatchKey> KEYS = new HashMap<>();
    static Consumer<Doc> handler = d -> {};

    /** What to do with a document that may have changed on disk; set once by the window code. */
    public static void onChange(Consumer<Doc> h) { handler = h; }

    static synchronized void watch(Path file) {
        var dir = file.getParent();
        if (dir == null || KEYS.containsKey(dir)) return;
        try {
            if (service == null) { service = FileSystems.getDefault().newWatchService(); start(service); }
            KEYS.put(dir, dir.register(service, ENTRY_MODIFY, ENTRY_CREATE));
        } catch (IOException | RuntimeException e) {
            System.err.println("notepadfx: not watching " + dir + ": " + e);
        }
    }

    static void start(WatchService s) {
        var t = new Thread(() -> run(s), "notepadfx-diskwatch");
        t.setDaemon(true);
        t.start();
    }

    static void run(WatchService s) {
        try {
            while (true) {
                var key = s.take();
                Thread.sleep(250);
                var changed = new LinkedHashSet<Path>();
                var dir = (Path) key.watchable();
                for (var e : key.pollEvents()) if (e.context() instanceof Path name) changed.add(dir.resolve(name).toAbsolutePath().normalize());
                key.reset();
                if (!changed.isEmpty()) Platform.runLater(() -> changed.forEach(p -> Docs.find(p).ifPresent(handler)));
            }
        } catch (InterruptedException | ClosedWatchServiceException e) {
            // the app is ending
        }
    }

    /** Looks at every open file; for a focus change, and for folders that send no events. */
    public static void checkAll() { Docs.all().forEach(handler); }

    private DiskWatch() {}
}
