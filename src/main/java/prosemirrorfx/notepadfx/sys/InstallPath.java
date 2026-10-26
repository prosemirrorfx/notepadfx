package prosemirrorfx.notepadfx.sys;

import jarrunner.jr.apputils.JrApp;
import java.nio.file.*;

public final class InstallPath {
    public static Path installed() {
        return Path.of(System.getenv().getOrDefault("LOCALAPPDATA", System.getProperty("user.home")), "Programs", "notepadfx", "notepadfx.exe");
    }

    public static Path installedOrRunning() {
        return Files.exists(installed()) ? installed() : JrApp.exe().orElse(installed());
    }

    private InstallPath() {}
}
