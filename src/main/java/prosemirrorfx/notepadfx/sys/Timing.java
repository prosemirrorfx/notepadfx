package prosemirrorfx.notepadfx.sys;

import java.lang.management.ManagementFactory;

public final class Timing {
    static final long START = System.nanoTime();
    static final boolean ON = System.getenv("NOTEPADFX_TIMING") != null;

    public static void mark(String what) {
        if (ON) System.err.println("notepadfx: " + what + " at " + (System.nanoTime() - START) / 1_000_000 + " ms after main");
    }

    public static void jvm() {
        if (ON) System.err.println("notepadfx: jvm uptime at main " + ManagementFactory.getRuntimeMXBean().getUptime() + " ms, jr before jvm " + System.getProperty("io.github.jarrunner.jr.beforeJvmMicros") + " us");
    }

    private Timing() {}
}
