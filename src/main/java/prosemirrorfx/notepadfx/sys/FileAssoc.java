package prosemirrorfx.notepadfx.sys;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.*;
import java.util.List;
import javax.imageio.ImageIO;

// notepadfx as an opener of .md, .markdown and .txt, per user (HKCU), written as a .reg file like ExplorerMenu.
// Windows keeps the user's own choice ("UserChoice") out of reach of programs, so this registers notepadfx, makes it the
// default where the user has not chosen one, and lists it under Settings > Default apps for the one click that settles the rest.
public final class FileAssoc {
    static final String CLASSES = "HKEY_CURRENT_USER\\Software\\Classes\\";
    static final String MD = "notepadfx.markdown", TXT = "notepadfx.text";
    static final List<String> MD_EXT = List.of(".md", ".markdown"), TXT_EXT = List.of(".txt");
    static final String CAPS = "HKEY_CURRENT_USER\\Software\\notepadfx\\Capabilities";
    static final String REGISTERED = "HKEY_CURRENT_USER\\Software\\RegisteredApplications";

    public static boolean add(Path exe) {
        var e = ExplorerMenu.esc(exe.toAbsolutePath().toString());
        var mdIcon = ExplorerMenu.esc(icon("markdown.ico", "M\u2193", new Color(0x2dd4bf)).toString());
        var txtIcon = ExplorerMenu.esc(icon("text.ico", "txt", new Color(0x94a3b8)).toString());
        var reg = new StringBuilder("Windows Registry Editor Version 5.00\r\n");
        progId(reg, MD, "Markdown document", mdIcon, e);
        progId(reg, TXT, "Text document", txtIcon, e);
        for (var x : MD_EXT) extension(reg, x, MD);
        for (var x : TXT_EXT) extension(reg, x, TXT);
        reg.append("\r\n[").append(CAPS).append("]\r\n")
            .append("\"ApplicationName\"=\"notepadfx\"\r\n")
            .append("\"ApplicationDescription\"=\"A fast markdown and text editor\"\r\n")
            .append("\r\n[").append(CAPS).append("\\FileAssociations]\r\n");
        for (var x : MD_EXT) reg.append("\"").append(x).append("\"=\"").append(MD).append("\"\r\n");
        for (var x : TXT_EXT) reg.append("\"").append(x).append("\"=\"").append(TXT).append("\"\r\n");
        reg.append("\r\n[").append(REGISTERED).append("]\r\n\"notepadfx\"=\"Software\\\\notepadfx\\\\Capabilities\"\r\n");
        var ok = ExplorerMenu.importReg(reg.toString());
        refreshIcons();
        return ok;
    }

    public static boolean remove() {
        var reg = new StringBuilder("Windows Registry Editor Version 5.00\r\n");
        for (var p : List.of(MD, TXT)) reg.append("\r\n[-").append(CLASSES).append(p).append("]\r\n");
        for (var x : MD_EXT) release(reg, x, MD);
        for (var x : TXT_EXT) release(reg, x, TXT);
        reg.append("\r\n[-HKEY_CURRENT_USER\\Software\\notepadfx]\r\n")
            .append("\r\n[").append(REGISTERED).append("]\r\n\"notepadfx\"=-\r\n");
        var ok = ExplorerMenu.importReg(reg.toString());
        refreshIcons();
        return ok;
    }

    public static boolean present() { return query(CLASSES + MD) != null; }

    /** True when Windows currently opens .md with notepadfx (the user's own choice wins over anything written here). */
    public static boolean isDefault() {
        var choice = query("HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\FileExts\\.md\\UserChoice");
        if (choice != null) return choice.contains(MD);
        var d = query(CLASSES + ".md");
        return d != null && d.contains(MD);
    }

    /** Opens Settings > Default apps, where the one click that Windows reserves for the user is made. */
    public static void openDefaultAppsPage() {
        try { new ProcessBuilder("explorer.exe", "ms-settings:defaultapps").start(); } catch (Exception e) { System.err.println("notepadfx: " + e); }
    }

    static void progId(StringBuilder reg, String id, String name, String icon, String exe) {
        reg.append("\r\n[").append(CLASSES).append(id).append("]\r\n@=\"").append(name).append("\"\r\n")
            .append("\r\n[").append(CLASSES).append(id).append("\\DefaultIcon]\r\n@=\"").append(icon).append("\"\r\n")
            .append("\r\n[").append(CLASSES).append(id).append("\\shell\\open]\r\n@=\"Open with notepadfx\"\r\n")
            .append("\r\n[").append(CLASSES).append(id).append("\\shell\\open\\command]\r\n@=\"\\\"").append(exe).append("\\\" \\\"%1\\\"\"\r\n");
    }

    static void extension(StringBuilder reg, String ext, String id) {
        reg.append("\r\n[").append(CLASSES).append(ext).append("]\r\n@=\"").append(id).append("\"\r\n")
            .append("\r\n[").append(CLASSES).append(ext).append("\\OpenWithProgids]\r\n\"").append(id).append("\"=hex(0):\r\n");
    }

    // only take back what is ours: another program's default for the extension stays
    static void release(StringBuilder reg, String ext, String id) {
        var d = query(CLASSES + ext);
        if (d != null && d.contains(id)) reg.append("\r\n[").append(CLASSES).append(ext).append("]\r\n@=-\r\n");
        reg.append("\r\n[").append(CLASSES).append(ext).append("\\OpenWithProgids]\r\n\"").append(id).append("\"=-\r\n");
    }

    static String query(String key) {
        try {
            var p = new ProcessBuilder("reg.exe", "query", key).redirectErrorStream(true).start();
            var out = new String(p.getInputStream().readAllBytes());
            return p.waitFor() == 0 ? out : null;
        } catch (Exception e) {
            return null;
        }
    }

    static void refreshIcons() {
        try { new ProcessBuilder("ie4uinit.exe", "-show").start().waitFor(); } catch (Exception ignored) {}
    }

    // ---- the icons: a page like the app icon with the file's mark on it, saved as .ico with PNG entries ----

    static Path icon(String name, String mark, Color color) {
        var file = Home.dir().resolve(name);
        try {
            var sizes = new int[]{16, 32, 48, 256};
            var png = new byte[sizes.length][];
            for (var i = 0; i < sizes.length; i++) png[i] = png(draw(sizes[i], mark, color));
            var head = 6 + 16 * sizes.length;
            var buf = ByteBuffer.allocate(head + java.util.Arrays.stream(png).mapToInt(b -> b.length).sum()).order(ByteOrder.LITTLE_ENDIAN);
            buf.putShort((short) 0).putShort((short) 1).putShort((short) sizes.length);
            var offset = head;
            for (var i = 0; i < sizes.length; i++) {
                var dim = (byte) (sizes[i] == 256 ? 0 : sizes[i]);
                buf.put(dim).put(dim).put((byte) 0).put((byte) 0)
                    .putShort((short) 1).putShort((short) 32).putInt(png[i].length).putInt(offset);
                offset += png[i].length;
            }
            for (var b : png) buf.put(b);
            Files.write(file, buf.array());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return file;
    }

    static BufferedImage draw(int size, String mark, Color color) {
        var img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        var g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.scale(size / 512.0, size / 512.0);
        var page = new Path2D.Double();
        page.moveTo(64, 16); page.lineTo(336, 16); page.lineTo(448, 128); page.lineTo(448, 496); page.lineTo(64, 496); page.closePath();
        g.setColor(new Color(0x1e293b));
        g.fill(page);
        g.setColor(new Color(0x475569));
        g.fill(new Polygon(new int[]{336, 448, 336}, new int[]{16, 128, 128}, 3));
        g.setColor(color);
        g.fill(new RoundRectangle2D.Double(64, 300, 384, 150, 40, 40));
        g.setColor(new Color(0x0f172a));
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, mark.length() > 2 ? 120 : 140));
        var fm = g.getFontMetrics();
        g.drawString(mark, 256 - fm.stringWidth(mark) / 2f, 375 + (fm.getAscent() - fm.getDescent()) / 2f);
        g.setColor(new Color(0xe2e8f0));
        g.setStroke(new BasicStroke(28f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(120, 180, 280, 180);
        g.drawLine(120, 240, 380, 240);
        g.dispose();
        return img;
    }

    static byte[] png(BufferedImage img) throws IOException {
        var out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    private FileAssoc() {}
}
