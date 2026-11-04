package prosefx.notepadfx;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.*;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The whole route as an AI session uses it: a real notepadfx process (headless, its own state folder, its own temp files)
 * and the command line run against it as separate processes. Fails on a build without the verbs, passes with them.
 */
class AgentProcessTest {
    static final String DOC = "# Questions\n\n>> one\n\ntext in the middle\n\n## Two\n\n>> two\n";
    static Path home, dir;
    static Process instance;

    record Run(int code, String out, String err) {}

    static ProcessBuilder java(String... args) {
        var cmd = new ArrayList<>(List.of(Path.of(System.getProperty("java.home"), "bin", "java").toString(), "-Dglass.platform=Headless", "-Dprism.order=sw", "-cp", System.getProperty("java.class.path"), "prosefx.notepadfx.Main"));
        cmd.addAll(List.of(args));
        var pb = new ProcessBuilder(cmd);
        pb.environment().put("NOTEPADFX_HOME", home.toString());
        pb.environment().put("NOTEPADFX_TEST_HOOKS", "1");
        return pb;
    }

    static Run cli(String stdin, String... args) throws Exception { return finish(start(stdin, args)); }

    static Process start(String stdin, String... args) throws IOException {
        var p = java(args).start();
        try (var in = p.getOutputStream()) { if (stdin != null) in.write(stdin.getBytes(UTF_8)); }
        return p;
    }

    static Run finish(Process p) throws Exception {
        var out = new String(p.getInputStream().readAllBytes(), UTF_8);
        var err = new String(p.getErrorStream().readAllBytes(), UTF_8);
        assertTrue(p.waitFor(60, TimeUnit.SECONDS), "the command did not end");
        return new Run(p.exitValue(), out, err);
    }

    static Path file(String name) throws IOException {
        var p = dir.resolve(name);
        Files.writeString(p, DOC);
        return p.toAbsolutePath().normalize();
    }

    /** Presses a key in the running notepadfx's close question, once the question is there. */
    static void press(String key) throws Exception {
        for (var i = 0; i < 60; i++) {
            if (cli(null, "test-key", key).code() == 0) return;
            Thread.sleep(250);
        }
        fail("the close question never appeared");
    }

    @BeforeAll static void boot() throws Exception {
        home = Files.createTempDirectory("notepadfx-e2e-home");
        dir = Files.createTempDirectory("notepadfx-e2e-files");
        instance = java().redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
        for (var i = 0; i < 120 && !Files.exists(home.resolve("run").resolve("port")); i++) Thread.sleep(250);
        assertTrue(Files.exists(home.resolve("run").resolve("port")), "the test notepadfx did not start");
        Thread.sleep(1000);
    }

    @AfterAll static void stop() throws Exception {
        if (instance != null) { instance.destroyForcibly(); instance.waitFor(10, TimeUnit.SECONDS); }
        for (var root : List.of(home, dir)) try (var walk = Files.walk(root)) { walk.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete()); }
    }

    @Test void helpIsPrintedAndSaysWhatTheExitCodesMean() throws Exception {
        var r = cli(null, "--help");
        assertEquals(0, r.code());
        assertTrue(r.out().contains("Exit codes") && r.out().contains("append") && r.out().contains("close"), r.out());
    }

    @Test void aSessionWorksOnAFileThePersonHasOpenAndTypedIn() throws Exception {
        var f = file("work.md");
        assertEquals(0, cli(null, "--owner", "e2e", f + ":3").code());
        var st = cli(null, "status", f.toString());
        assertTrue(st.out().contains("owner: e2e") && st.out().contains("open: yes") && st.out().contains("unsaved: no"), st.out());
        assertEquals(0, cli(null, "test-type", f.toString(), "--after=middle", "--text=TYPED").code());

        var add = cli("\n## Three ś\n\n>> a new question\nwith a second line\n", "append", f.toString(), "--text-stdin");
        assertEquals(0, add.code(), add.err() + add.out());
        var read = cli(null, "read", f.toString(), "--json");
        assertEquals(0, read.code());
        assertTrue(read.out().contains("middleTYPED"), read.out());
        assertTrue(read.out().contains("## Three ś\\n\\n>> a new question\\nwith a second line\\n"), read.out());
        assertTrue(read.out().contains("\"unsaved\":true"), read.out());
        assertEquals(DOC, Files.readString(f), "the file on disk was touched");

        var stale = cli(null, "replace", f.toString(), "--lines", "3-3", "--expect", ">> not there", "--text", "x");
        assertEquals(5, stale.code(), stale.err());
        assertEquals(0, cli(null, "replace", f.toString(), "--lines", "3-3", "--expect", ">> one", "--text", ">> one, answered").code());
    }

    @Test void onlyTheOwnerClosesAndTheUnsavedAreAskedAbout() throws Exception {
        var f = file("close.md");
        assertEquals(0, cli(null, "open", f.toString(), "--owner", "me").code());

        var other = start(null, "close", f.toString(), "--owner", "someone", "--timeout", "40");
        press("ESCAPE");
        assertEquals(4, finish(other).code(), "Esc did not refuse");
        assertEquals(0, cli(null, "status", f.toString()).code(), "the file was closed by a stranger");

        assertEquals(0, cli(null, "test-type", f.toString(), "--after=middle", "--text=TYPED").code());
        assertEquals(6, cli(null, "close", f.toString(), "--owner", "me", "--no-ask").code());
        var mine = start(null, "close", f.toString(), "--owner", "me", "--timeout", "40");
        Thread.sleep(2600);
        press("ENTER");
        assertEquals(0, finish(mine).code());
        assertTrue(Files.readString(f).contains("middleTYPED"), "the typing was not saved before closing");
        assertEquals(3, cli(null, "status", f.toString()).code());
        assertEquals(3, cli(null, "read", f.toString()).code());
    }

    @Test void theExclusiveRouteCloseWriteOpenAtALine() throws Exception {
        var f = file("exclusive.md");
        assertEquals(0, cli(null, "--owner", "me", f.toString()).code());
        assertEquals(0, cli(null, "close", f.toString(), "--owner", "me").code());
        Files.writeString(f, DOC + "\n>> written by the session\n");
        assertEquals(0, cli(null, "--owner", "me", f + ":11").code());
        var read = cli(null, "read", f.toString(), "--lines", "11");
        assertTrue(read.out().endsWith("---\n>> written by the session\n"), read.out());
        assertEquals(8, cli(null, "--owner", "me", f + ":99").code());
    }

    @Test void anOlderRunningNotepadfxIsNeverSentACommand() throws Exception {
        var marker = home.resolve("run").resolve("port.askable");
        var good = Files.readString(marker);
        try {
            Files.writeString(marker, "1");
            var r = cli(null, "files");
            assertEquals(2, r.code());
            assertTrue(r.err().contains("older version"), r.err());
        } finally {
            Files.writeString(marker, good);
        }
    }

    @Test void filesListsWhatIsOpen() throws Exception {
        var f = file("listed.md");
        cli(null, "open", f.toString());
        var r = cli(null, "files");
        assertEquals(0, r.code());
        assertTrue(r.out().contains(f.toString()), r.out());
    }
}
