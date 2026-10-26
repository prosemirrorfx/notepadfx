package io.github.prosemirrorfx.notepadfx.sys;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.util.concurrent.*;

public final class IdleTrim {
    static volatile long last = System.currentTimeMillis();
    static boolean pagedOut, collected;
    static MethodHandle setWorkingSet, currentProcess;

    public static void touch() {
        last = System.currentTimeMillis();
        pagedOut = collected = false;
    }

    public static void start() {
        if (!System.getProperty("os.name", "").startsWith("Windows")) return;
        try {
            var linker = Linker.nativeLinker();
            var k32 = SymbolLookup.libraryLookup("kernel32", Arena.global());
            currentProcess = linker.downcallHandle(k32.find("GetCurrentProcess").orElseThrow(), FunctionDescriptor.of(ValueLayout.ADDRESS));
            setWorkingSet = linker.downcallHandle(k32.find("SetProcessWorkingSetSize").orElseThrow(),
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG));
        } catch (Throwable t) {
            System.err.println("notepadfx: idle trim unavailable: " + t);
            return;
        }
        var ex = Executors.newSingleThreadScheduledExecutor(r -> Thread.ofPlatform().daemon().name("idle-trim").unstarted(r));
        ex.scheduleWithFixedDelay(IdleTrim::tick, 5, 5, TimeUnit.SECONDS);
    }

    static void tick() {
        var idle = System.currentTimeMillis() - last;
        if (idle > 120_000 && !collected) { collected = true; System.gc(); pageOut(); }
        else if (idle > 15_000 && !pagedOut) { pagedOut = true; pageOut(); }
    }

    static void pageOut() {
        try {
            var self = (MemorySegment) currentProcess.invokeExact();
            var ok = (int) setWorkingSet.invokeExact(self, -1L, -1L);
        } catch (Throwable t) {
            System.err.println("notepadfx: idle trim failed: " + t);
        }
    }

    private IdleTrim() {}
}
