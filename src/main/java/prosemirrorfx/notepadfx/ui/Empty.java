package prosemirrorfx.notepadfx.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

final class Empty {
    static VBox view() {
        var title = new Label("No file is open");
        title.getStyleClass().add("np-empty-title");
        var hint = new Label("F12 open a file    F7 files panes    F10 new window\nDouble-click a file in Files, or drop a tab here from another window.");
        hint.getStyleClass().add("np-empty-hint");
        var box = new VBox(10, title, hint);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("np-empty");
        return box;
    }

    private Empty() {}
}
