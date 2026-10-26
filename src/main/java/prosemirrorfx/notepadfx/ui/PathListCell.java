package prosemirrorfx.notepadfx.ui;

import java.nio.file.Path;
import javafx.scene.control.*;

final class PathListCell extends ListCell<Path> {
    @Override
    protected void updateItem(Path p, boolean empty) {
        super.updateItem(p, empty);
        getStyleClass().remove("np-dir");
        if (empty || p == null) { setText(null); setTooltip(null); return; }
        setText(p.getFileName().toString());
        getStyleClass().add("np-dir");
        setTooltip(new Tooltip(p.toString()));
    }
}
