package prosemirrorfx.notepadfx.ui;

import jarrunner.jr.apputils.*;
import prosemirrorfx.notepadfx.sys.AotTraining;
import java.time.Duration;
import javafx.application.Platform;
import javafx.beans.property.*;

public final class Updates {
    static final StringProperty NOTICE = new SimpleStringProperty("");
    static volatile boolean busy;

    public static void start() {
        Updater.inBackground(Duration.ofDays(1), r -> Platform.runLater(() -> shown(r)));
    }

    static void shown(UpdateResult r) {
        NOTICE.set(r.status() == UpdateResult.Status.NEWER ? "notepadfx " + r.latest() + " is available: click to update" : "");
    }

    /** jr is building the start-up cache from this run: load the recorded classes in the background and say so, quietly. */
    public static void tuneStartup() {
        if (!AotTraining.training()) return;
        NOTICE.set("First start of this version: tuning notepadfx so later starts are faster");
        Thread.ofPlatform().daemon().name("aot-profile").start(() -> {
            AotTraining.preload();
            Platform.runLater(() -> NOTICE.set("Tuning done: notepadfx starts faster from the next time you open it"));
        });
    }

    static String version() { return JrApp.appVersion().orElse("development build"); }

    static void checkNow() { run(false); }

    static void updateNow() { run(true); }

    static void run(boolean update) {
        if (busy) return;
        busy = true;
        NOTICE.set(update ? "Updating notepadfx..." : "Checking for updates...");
        Thread.ofVirtual().start(() -> {
            var r = update ? Updater.update() : Updater.check();
            Platform.runLater(() -> {
                busy = false;
                NOTICE.set(switch (r.status()) {
                    case NEWER -> "notepadfx " + r.latest() + " is available: click to update";
                    case UPDATED -> "Updated to " + r.latest() + ": it runs from the next start";
                    case CURRENT -> "notepadfx is up to date (" + version() + ")";
                    case UNAVAILABLE -> "Updates work in the installed notepadfx.exe only";
                    case ERROR -> "Update check failed: " + r.message();
                });
            });
        });
    }

    static void clicked() {
        if (NOTICE.get().endsWith("click to update")) updateNow();
    }

    private Updates() {}
}
