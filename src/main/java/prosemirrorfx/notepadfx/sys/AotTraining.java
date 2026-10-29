package prosemirrorfx.notepadfx.sys;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * jr builds notepadfx's AOT cache from the first run after a new jar arrives, from whatever classes that run loaded.
 * That run is often short (handing a file to the open window, an update), which left a cache of a sixth of the classes
 * a real start needs. So while jr is training, notepadfx loads the classes of a recorded real session
 * (META-INF/notepadfx/aot-profile.json) by name, without initialising them: no code of theirs runs, the cache just
 * learns them. The user's machine only ever reads the list.
 */
public final class AotTraining {

    static final String PROFILE = "/META-INF/notepadfx/aot-profile.json";
    static final Pattern NAME = Pattern.compile("\"([\\w.$]+)\"");
    static volatile Boolean training;

    /** True when this JVM is recording for an AOT cache (jr's first run after a new jar). */
    public static boolean training() {
        if (training == null) training = ManagementFactory.getRuntimeMXBean().getInputArguments().stream()
                .anyMatch(a -> a.startsWith("-XX:AOTCacheOutput") || a.startsWith("-XX:AOTMode=record"));
        return training;
    }

    /** Loads the recorded classes when training; returns how many loaded (0 when not training). */
    public static int preload() {
        if (!training()) return 0;
        var loader = AotTraining.class.getClassLoader();
        var n = 0;
        try (var in = AotTraining.class.getResourceAsStream(PROFILE)) {
            if (in == null) return 0;
            var m = NAME.matcher(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            while (m.find()) {
                try {
                    Class.forName(m.group(1), false, loader);
                    n++;
                } catch (Throwable notHere) {
                    // a class of another JDK build or platform: the rest still count
                }
            }
        } catch (IOException e) {
            System.err.println("notepadfx: AOT profile unreadable: " + e);
        }
        Timing.mark("aot profile loaded " + n);
        return n;
    }

    private AotTraining() {}
}
