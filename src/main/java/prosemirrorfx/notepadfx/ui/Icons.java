package prosemirrorfx.notepadfx.ui;

import prosemirrorfx.notepadfx.sys.Home;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.stage.Stage;

final class Icons {
    static final List<Integer> SIZES = List.of(16, 24, 32, 48, 64, 256);
    static final Path CACHE = Home.dir().resolve("cache");
    static List<Image> cached;

    static void apply(Stage stage) {
        if (cached == null) cached = fromCache();
        if (cached != null) { stage.getIcons().setAll(cached); return; }
        Thread.ofVirtual().start(() -> {
            var drawn = SIZES.stream().map(Icons::draw).toList();
            Platform.runLater(() -> { cached = drawn; Windows.ALL.forEach(w -> w.stage.getIcons().setAll(drawn)); });
        });
    }

    static List<Image> fromCache() {
        var images = new ArrayList<Image>();
        for (var s : SIZES) {
            var f = CACHE.resolve("icon-v1-" + s + ".png");
            if (!Files.exists(f)) return null;
            try (var in = Files.newInputStream(f)) { images.add(new Image(in)); } catch (IOException e) { return null; }
        }
        return images;
    }

    static Image draw(int size) {
        var img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        var g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.scale(size / 512.0, size / 512.0);
        g.setColor(new Color(0x1e293b));
        g.fill(new RoundRectangle2D.Double(0, 0, 512, 512, 200, 200));
        var page = new Path2D.Double();
        page.moveTo(104, 64); page.lineTo(336, 64); page.lineTo(408, 136); page.lineTo(408, 448); page.lineTo(104, 448); page.closePath();
        g.setColor(new Color(0x334155));
        g.fill(page);
        g.setColor(new Color(0x475569));
        g.fill(new Polygon(new int[]{336, 408, 336}, new int[]{64, 136, 136}, 3));
        var thick = size <= 24 ? 44f : 30f;
        g.setStroke(new BasicStroke(thick, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int[][] rows = size <= 24 ? new int[][]{{176, 120}, {272, 160}, {368, 100}} : new int[][]{{176, 120}, {240, 150}, {304, 90}, {368, 130}};
        for (var r : rows) {
            g.setColor(new Color(0x2dd4bf));
            g.drawLine(240, r[0], 240 - r[1] / 2 - 40, r[0]);
            g.setColor(new Color(0xe2e8f0));
            g.drawLine(272, r[0], 272 + r[1] / 2 + 40, r[0]);
        }
        g.dispose();
        return png(img, size);
    }

    static Image png(BufferedImage img, int size) {
        try {
            var bytes = new ByteArrayOutputStream();
            ImageIO.write(img, "png", bytes);
            Files.createDirectories(CACHE);
            Files.write(CACHE.resolve("icon-v1-" + size + ".png"), bytes.toByteArray());
            return new Image(new ByteArrayInputStream(bytes.toByteArray()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Icons() {}
}
