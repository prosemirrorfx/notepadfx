package prosemirrorfx.notepadfx.ui;

import prosemirrorfx.notepadfx.sys.ShellIcons;
import java.nio.file.Path;
import java.util.function.Supplier;
import javafx.scene.image.ImageView;

final class FileIcon {
    static ImageView view() {
        var v = new ImageView();
        v.setFitWidth(16);
        v.setFitHeight(16);
        v.setPreserveRatio(true);
        v.setSmooth(true);
        return v;
    }

    static void show(ImageView v, Path p, Supplier<Path> stillShowing) {
        v.setImage(ShellIcons.cached(p));
        if (v.getImage() == null) ShellIcons.request(p, img -> { if (p.equals(stillShowing.get())) v.setImage(img); });
    }

    private FileIcon() {}
}
