package io.github.prosemirrorfx.notepadfx.ui;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;

final class Snapshots {
    static void write(List<MainWindow> windows, String target) {
        for (var i = 0; i < windows.size(); i++) {
            var img = windows.get(i).stage.getScene().snapshot(null);
            var w = (int) img.getWidth();
            var h = (int) img.getHeight();
            var out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            var reader = img.getPixelReader();
            for (var y = 0; y < h; y++) for (var x = 0; x < w; x++) out.setRGB(x, y, reader.getArgb(x, y));
            var file = Path.of(i == 0 ? target : target.replaceFirst("(\\.png)?$", "-" + (i + 1) + ".png"));
            try { ImageIO.write(out, "png", file.toFile()); } catch (Exception e) { System.err.println("notepadfx: snapshot: " + e); }
        }
    }

    private Snapshots() {}
}
