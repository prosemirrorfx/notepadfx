package prosemirrorfx.notepadfx.sys;

import java.nio.file.*;

public final class Home {
    static final Path DIR = Path.of(System.getProperty("user.home"), "prosemirrorfx", "notepadfx");

    public static Path dir() {
        try { Files.createDirectories(DIR); } catch (Exception e) { throw new RuntimeException(e); }
        return DIR;
    }

    private Home() {}
}
