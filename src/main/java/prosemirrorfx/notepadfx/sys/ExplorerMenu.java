package prosemirrorfx.notepadfx.sys;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;

// "Open with notepadfx" in Explorer's right-click menu, per user (HKCU), written as a .reg file so no path is ever quoted on a command line
public final class ExplorerMenu {
    static final List<String> KEYS = List.of(
        "HKEY_CURRENT_USER\\Software\\Classes\\*\\shell\\notepadfx",
        "HKEY_CURRENT_USER\\Software\\Classes\\Directory\\shell\\notepadfx",
        "HKEY_CURRENT_USER\\Software\\Classes\\Directory\\Background\\shell\\notepadfx");

    public static boolean add(Path exe) {
        var e = esc(exe.toAbsolutePath().toString());
        var reg = new StringBuilder("Windows Registry Editor Version 5.00\r\n");
        entry(reg, KEYS.get(0), e, "%1");
        entry(reg, KEYS.get(1), e, "%V");
        entry(reg, KEYS.get(2), e, "%V");
        return importReg(reg.toString());
    }

    public static boolean remove() {
        var reg = new StringBuilder("Windows Registry Editor Version 5.00\r\n");
        for (var k : KEYS) reg.append("\r\n[-").append(k).append("]\r\n");
        return importReg(reg.toString());
    }

    public static boolean present() {
        try {
            var p = new ProcessBuilder("reg.exe", "query", KEYS.get(0)).redirectErrorStream(true).start();
            p.getInputStream().readAllBytes();
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    static void entry(StringBuilder reg, String key, String exe, String arg) {
        reg.append("\r\n[").append(key).append("]\r\n")
            .append("@=\"Open with notepadfx\"\r\n")
            .append("\"Icon\"=\"\\\"").append(exe).append("\\\",0\"\r\n")
            .append("\r\n[").append(key).append("\\command]\r\n")
            .append("@=\"\\\"").append(exe).append("\\\" \\\"").append(arg).append("\\\"\"\r\n");
    }

    static String esc(String s) { return s.replace("\\", "\\\\").replace("\"", "\\\""); }

    static boolean importReg(String content) {
        try {
            var file = Files.createTempFile("notepadfx-menu", ".reg");
            var bom = new byte[]{(byte) 0xFF, (byte) 0xFE};
            var body = content.getBytes(StandardCharsets.UTF_16LE);
            var all = new byte[bom.length + body.length];
            System.arraycopy(bom, 0, all, 0, 2);
            System.arraycopy(body, 0, all, 2, body.length);
            Files.write(file, all);
            var p = new ProcessBuilder("reg.exe", "import", file.toString()).redirectErrorStream(true).start();
            p.getInputStream().readAllBytes();
            var ok = p.waitFor() == 0;
            Files.deleteIfExists(file);
            return ok;
        } catch (Exception e) {
            System.err.println("notepadfx: explorer menu: " + e);
            return false;
        }
    }

    private ExplorerMenu() {}
}
