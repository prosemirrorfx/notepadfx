package io.github.prosemirrorfx.notepadfx.sys;

import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class Instance {
    static final Path RUN = Home.dir().resolve("run"), PORT = RUN.resolve("port"), LOCK = RUN.resolve("instance.lock");
    static FileLock lock;

    public static boolean primary() {
        try {
            Files.createDirectories(RUN);
            lock = FileChannel.open(LOCK, StandardOpenOption.CREATE, StandardOpenOption.WRITE).tryLock();
            return lock != null;
        } catch (Exception e) {
            return true;
        }
    }

    public static boolean handOff(List<String> args) {
        var deadline = System.currentTimeMillis() + 4000;
        while (System.currentTimeMillis() < deadline) {
            try (var s = new Socket(InetAddress.getLoopbackAddress(), Integer.parseInt(Files.readString(PORT).trim()));
                 var out = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), UTF_8), true)) {
                out.println(Messages.encode(args));
                return true;
            } catch (Exception e) {
                try { Thread.sleep(150); } catch (InterruptedException ie) { return false; }
            }
        }
        return false;
    }

    public static void serve(Consumer<List<String>> onMessage) {
        try {
            var server = new ServerSocket(0, 8, InetAddress.getLoopbackAddress());
            Files.writeString(PORT, String.valueOf(server.getLocalPort()));
            Thread.ofVirtual().name("instance").start(() -> accept(server, onMessage));
        } catch (Exception e) {
            System.err.println("notepadfx: second launches will open their own process: " + e);
        }
    }

    static void accept(ServerSocket server, Consumer<List<String>> onMessage) {
        while (!server.isClosed()) {
            try (var s = server.accept(); var in = new BufferedReader(new InputStreamReader(s.getInputStream(), UTF_8))) {
                var line = in.readLine();
                if (line != null) onMessage.accept(Messages.decode(line));
            } catch (Exception e) {
                System.err.println("notepadfx: hand-off failed: " + e);
            }
        }
    }

    private Instance() {}
}
