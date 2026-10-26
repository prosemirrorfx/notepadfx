package io.github.prosemirrorfx.notepadfx.state;

import io.github.prosemirrorfx.notepadfx.sys.Home;
import java.nio.file.*;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class Store {
    static final Path FILE = Home.dir().resolve("session.json");

    public static Session load() {
        try {
            return Files.exists(FILE) ? new Session().fromJson(Files.readString(FILE, UTF_8)) : new Session();
        } catch (Exception e) {
            System.err.println("notepadfx: session.json unreadable, kept as session.json.bad, starting fresh: " + e);
            try { Files.copy(FILE, FILE.resolveSibling("session.json.bad"), StandardCopyOption.REPLACE_EXISTING); } catch (Exception ignored) {}
            return new Session();
        }
    }

    public static void save(Session s) {
        try {
            var tmp = FILE.resolveSibling("session.json.tmp");
            Files.writeString(tmp, s.toJson(), UTF_8);
            Files.move(tmp, FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e) {
            System.err.println("notepadfx: could not save session: " + e);
        }
    }

    private Store() {}
}
