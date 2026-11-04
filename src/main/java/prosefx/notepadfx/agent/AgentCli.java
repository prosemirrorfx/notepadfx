package prosefx.notepadfx.agent;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import prosefx.notepadfx.sys.Instance;

import static java.nio.charset.StandardCharsets.UTF_8;

/** The command-line side of the agent verbs: turns the words into a request, asks the running notepadfx, prints what it says and returns the exit code. */
public final class AgentCli {
    public static final Set<String> VERBS = Set.of("files", "status", "read", "append", "insert", "replace", "close", "open");
    static final int DEFAULT_WAIT_SECONDS = 120;

    public static boolean handles(String first) { return known(first) || Set.of("--help", "-h", "help").contains(first); }

    /** The verbs, and the two test hooks when the process was started with NOTEPADFX_TEST_HOOKS. */
    static boolean known(String verb) { return VERBS.contains(verb) || verb.startsWith("test-") && System.getenv("NOTEPADFX_TEST_HOOKS") != null; }

    public static int run(List<String> argv) { return run(argv, false); }

    /** A launch such as `notepadfx --owner me file:22`: the same as the open verb, but quiet when it worked and when no notepadfx runs (the launch then starts one). */
    public static int open(List<String> launch) {
        var open = new ArrayList<String>();
        open.add("open");
        open.addAll(launch);
        return run(open, true);
    }

    static int run(List<String> argv, boolean launch) {
        var verb = argv.getFirst();
        if (!known(verb)) { System.out.print(Help.text()); return Exit.DONE; }
        try {
            var a = Args.parse(argv.subList(1, argv.size()));
            if (a.has("help")) { System.out.print(Help.text()); return Exit.DONE; }
            var file = a.word(0);
            if (file != null && !verb.equals("test-key")) a.words().set(0, Path.of(file).toAbsolutePath().normalize().toString());
            load(a, "text", "text-file", a.has("text-stdin"));
            load(a, "expect", "expect-file", false);
            var wait = a.number("timeout", DEFAULT_WAIT_SECONDS);
            a.opts.putIfAbsent("timeout", String.valueOf(wait));
            a.opts.remove("text-stdin");
            var reply = Instance.request(a.asRequest(verb), (wait + 15) * 1000L);
            if (reply.isEmpty()) return launch ? Exit.NOT_RUNNING : say(Exit.NOT_RUNNING, "notepadfx is not running\n", false);
            var text = reply.get();
            var nl = text.indexOf('\n');
            var code = Integer.parseInt(text.substring(0, nl));
            return launch && code == Exit.DONE ? code : say(code, text.substring(nl + 1), a.has("json"));
        } catch (IOException | RuntimeException e) {
            return say(Exit.ERROR, "notepadfx: " + e.getMessage() + "\n", false);
        }
    }

    static void load(Args a, String name, String fileOption, boolean stdin) throws IOException {
        if (stdin) a.opts.put(name, new String(System.in.readAllBytes(), UTF_8).replace("\r\n", "\n"));
        else if (a.has(fileOption)) a.opts.put(name, Files.readString(Path.of(a.get(fileOption)), UTF_8).replace("\r\n", "\n"));
        a.opts.remove(fileOption);
    }

    /** UTF-8 whatever the console's code page: the text of a file must reach the session as it is. */
    static int say(int code, String text, boolean json) {
        var out = new java.io.PrintStream(new java.io.FileOutputStream(code == Exit.DONE || json ? java.io.FileDescriptor.out : java.io.FileDescriptor.err), true, UTF_8);
        out.print(text);
        out.flush();
        return code;
    }

    private AgentCli() {}
}
