package prosemirrorfx.notepadfx.ui;

import java.nio.file.Path;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;

final class PathListCell extends ListCell<Path> {
    final ImageView icon = FileIcon.view();

    @Override
    protected void updateItem(Path p, boolean empty) {
        super.updateItem(p, empty);
        getStyleClass().remove("np-dir");
        if (empty || p == null) { setText(null); setTooltip(null); setGraphic(null); return; }
        setText(p.getFileName().toString());
        getStyleClass().add("np-dir");
        setTooltip(new Tooltip(p.toString()));
        FileIcon.show(icon, p, this::getItem);
        setGraphic(icon);
    }
}
