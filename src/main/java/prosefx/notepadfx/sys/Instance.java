package prosefx.notepadfx.sys;

import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class Instance {
    static final Path RUN = Home.dir().resolve("run"), PORT = RUN.resolve("port"), LOCK = RUN.resolve("instance.lock"), ASKABLE = RUN.resolve("port.askable");
    static final long LONGEST_WAIT_MINUTES = 30;
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

    /** Sends a command-line verb to the running notepadfx and waits (up to waitMillis) for its answer; empty when none is running. */
    public static Optional<String> request(List<String> args, long waitMillis) {
        try {
            if (!Files.readString(PORT).trim().equals(Files.readString(ASKABLE).trim()))
                return Optional.of("2\nthe running notepadfx is an older version that does not take these commands: close it and start the new one\n");
        } catch (Exception e) {
            return Optional.empty();
        }
        try (var s = new Socket(InetAddress.getLoopbackAddress(), Integer.parseInt(Files.readString(PORT).trim()))) {
            s.setSoTimeout((int) Math.min(Integer.MAX_VALUE, waitMillis));
            var out = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), UTF_8), true);
            out.println(Messages.pack(args));
            return Optional.of(new String(s.getInputStream().readAllBytes(), UTF_8));
        } catch (SocketTimeoutException e) {
            return Optional.of("7\nnotepadfx did not answer in time\n");
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /** onMessage takes a launch (no answer); onRequest takes a verb that wants one and says it when ready, which may be after a person has answered a question. */
    public static void serve(Consumer<List<String>> onMessage, Function<List<String>, CompletableFuture<String>> onRequest) {
        try {
            var server = new ServerSocket(0, 8, InetAddress.getLoopbackAddress());
            Files.writeString(PORT, String.valueOf(server.getLocalPort()));
            Files.writeString(ASKABLE, String.valueOf(server.getLocalPort()));
            Thread.ofVirtual().name("instance").start(() -> accept(server, onMessage, onRequest));
        } catch (Exception e) {
            System.err.println("notepadfx: second launches will open their own process: " + e);
        }
    }

    static void accept(ServerSocket server, Consumer<List<String>> onMessage, Function<List<String>, CompletableFuture<String>> onRequest) {
        while (!server.isClosed()) {
            try {
                var s = server.accept();
                Thread.ofVirtual().name("instance-client").start(() -> answer(s, onMessage, onRequest));
            } catch (Exception e) {
                System.err.println("notepadfx: hand-off failed: " + e);
            }
        }
    }

    static void answer(Socket s, Consumer<List<String>> onMessage, Function<List<String>, CompletableFuture<String>> onRequest) {
        try (s; var in = new BufferedReader(new InputStreamReader(s.getInputStream(), UTF_8))) {
            var line = in.readLine();
            if (line == null) return;
            var args = Messages.decode(line);
            if (!Messages.isAgent(args)) { onMessage.accept(args); return; }
            var reply = onRequest.apply(Messages.unpack(args)).get(LONGEST_WAIT_MINUTES, TimeUnit.MINUTES);
            s.getOutputStream().write(reply.getBytes(UTF_8));
        } catch (Exception e) {
            System.err.println("notepadfx: hand-off failed: " + e);
        }
    }

    private Instance() {}
}
