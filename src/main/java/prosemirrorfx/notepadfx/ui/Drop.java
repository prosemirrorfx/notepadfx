package prosemirrorfx.notepadfx.ui;

import javafx.scene.Node;
import javafx.scene.input.*;

// Files dragged in from Explorer open like command-line arguments (folders become workspaces).
// A filter, because the tab list accepts drags for itself and would otherwise swallow a file coming from outside.
final class Drop {
    static void install(Node root) {
        root.addEventFilter(DragEvent.DRAG_OVER, e -> {
            if (!e.getDragboard().hasFiles()) return;
            e.acceptTransferModes(TransferMode.COPY);
            e.consume();
        });
        root.addEventFilter(DragEvent.DRAG_DROPPED, e -> {
            if (!e.getDragboard().hasFiles()) return;
            for (var f : e.getDragboard().getFiles()) Windows.open(f.toPath());
            e.setDropCompleted(true);
            e.consume();
        });
    }

    private Drop() {}
}
