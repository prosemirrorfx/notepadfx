package prosemirrorfx.notepadfx.sys;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.file.Path;
import javafx.scene.image.*;

import static java.lang.foreign.ValueLayout.*;

final class Win32Icons {
    static final int SHGFI_ICON = 0x100, SHGFI_SMALLICON = 0x1, SHGFI_USEFILEATTRIBUTES = 0x10,
        FILE_ATTRIBUTE_DIRECTORY = 0x10, FILE_ATTRIBUTE_NORMAL = 0x80, SHFILEINFO_SIZE = 696, ICONINFO_SIZE = 32, BITMAP_SIZE = 32;
    static MethodHandle shGetFileInfo, getIconInfo, destroyIcon, getDC, releaseDC, getDIBits, getObject, deleteObject, coInit;
    static boolean ready, comDone, reported;

    static synchronized boolean init() {
        if (ready) return true;
        try {
            var l = Linker.nativeLinker();
            var a = Arena.global();
            var shell = SymbolLookup.libraryLookup("shell32", a);
            var user = SymbolLookup.libraryLookup("user32", a);
            var gdi = SymbolLookup.libraryLookup("gdi32", a);
            var ole = SymbolLookup.libraryLookup("ole32", a);
            shGetFileInfo = l.downcallHandle(shell.find("SHGetFileInfoW").orElseThrow(), FunctionDescriptor.of(JAVA_LONG, ADDRESS, JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT));
            getIconInfo = l.downcallHandle(user.find("GetIconInfo").orElseThrow(), FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
            destroyIcon = l.downcallHandle(user.find("DestroyIcon").orElseThrow(), FunctionDescriptor.of(JAVA_INT, ADDRESS));
            getDC = l.downcallHandle(user.find("GetDC").orElseThrow(), FunctionDescriptor.of(ADDRESS, ADDRESS));
            releaseDC = l.downcallHandle(user.find("ReleaseDC").orElseThrow(), FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
            getDIBits = l.downcallHandle(gdi.find("GetDIBits").orElseThrow(), FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT, JAVA_INT, ADDRESS, ADDRESS, JAVA_INT));
            getObject = l.downcallHandle(gdi.find("GetObjectW").orElseThrow(), FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS));
            deleteObject = l.downcallHandle(gdi.find("DeleteObject").orElseThrow(), FunctionDescriptor.of(JAVA_INT, ADDRESS));
            coInit = l.downcallHandle(ole.find("CoInitializeEx").orElseThrow(), FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT));
            ready = true;
        } catch (Throwable t) {
            System.err.println("notepadfx: shell icons unavailable: " + t);
        }
        return ready;
    }

    static Image load(Path p, boolean directory, boolean perFile) {
        if (!init()) return null;
        try (var arena = Arena.ofConfined()) {
            if (!comDone) { var hr = (int) coInit.invokeExact(MemorySegment.NULL, 2); comDone = true; }
            var info = arena.allocate(SHFILEINFO_SIZE);
            var flags = SHGFI_ICON | SHGFI_SMALLICON | (perFile ? 0 : SHGFI_USEFILEATTRIBUTES);
            var attrs = directory ? FILE_ATTRIBUTE_DIRECTORY : FILE_ATTRIBUTE_NORMAL;
            var ok = (long) shGetFileInfo.invokeExact(arena.allocateFrom(p.toString(), java.nio.charset.StandardCharsets.UTF_16LE), attrs, info, SHFILEINFO_SIZE, flags);
            var icon = info.get(ADDRESS, 0);
            if (ok == 0 || icon.equals(MemorySegment.NULL)) return null;
            try {
                return pixels(arena, icon);
            } finally {
                var ignored = (int) destroyIcon.invokeExact(icon);
            }
        } catch (Throwable t) {
            if (!reported) { reported = true; System.err.println("notepadfx: shell icon failed for " + p + ": " + t); }
            return null;
        }
    }

    static Image pixels(Arena arena, MemorySegment icon) throws Throwable {
        var ii = arena.allocate(ICONINFO_SIZE);
        if ((int) getIconInfo.invokeExact(icon, ii) == 0) return null;
        var mask = ii.get(ADDRESS, 16);
        var color = ii.get(ADDRESS, 24);
        try {
            if (color.equals(MemorySegment.NULL)) return null;
            var bm = arena.allocate(BITMAP_SIZE);
            if ((int) getObject.invokeExact(color, BITMAP_SIZE, bm) == 0) return null;
            var w = bm.get(JAVA_INT, 4);
            var h = bm.get(JAVA_INT, 8);
            if (w <= 0 || h <= 0 || w > 256 || h > 256) return null;
            var header = arena.allocate(40 + 1024);
            header.set(JAVA_INT, 0, 40);
            header.set(JAVA_INT, 4, w);
            header.set(JAVA_INT, 8, -h);
            header.set(JAVA_SHORT, 12, (short) 1);
            header.set(JAVA_SHORT, 14, (short) 32);
            var bits = arena.allocate((long) w * h * 4);
            var dc = (MemorySegment) getDC.invokeExact(MemorySegment.NULL);
            try {
                if ((int) getDIBits.invokeExact(dc, color, 0, h, bits, header, 0) == 0) return null;
            } finally {
                var ignored = (int) releaseDC.invokeExact(MemorySegment.NULL, dc);
            }
            var argb = bits.toArray(JAVA_INT);
            var anyAlpha = false;
            for (var px : argb) if ((px >>> 24) != 0) { anyAlpha = true; break; }
            if (!anyAlpha) for (var i = 0; i < argb.length; i++) if (argb[i] != 0) argb[i] |= 0xFF000000;
            var img = new WritableImage(w, h);
            img.getPixelWriter().setPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), argb, 0, w);
            return img;
        } finally {
            if (!color.equals(MemorySegment.NULL)) { var ignored = (int) deleteObject.invokeExact(color); }
            if (!mask.equals(MemorySegment.NULL)) { var ignored = (int) deleteObject.invokeExact(mask); }
        }
    }

    private Win32Icons() {}
}
