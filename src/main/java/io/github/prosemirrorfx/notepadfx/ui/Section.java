package io.github.prosemirrorfx.notepadfx.ui;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

final class Section {
    static HBox header(String title, Node... tools) {
        var label = new Label(title.toUpperCase());
        label.getStyleClass().add("np-section-title");
        var spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        var box = new HBox(4, label, spacer);
        box.getChildren().addAll(tools);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().add("np-section-header");
        return box;
    }

    static Button tool(String glyph, String tip, Runnable run) {
        var b = new Button(glyph);
        b.getStyleClass().add("np-tool");
        b.setTooltip(new Tooltip(tip));
        b.setFocusTraversable(false);
        b.setOnAction(e -> run.run());
        return b;
    }

    static TextField filter(String prompt) {
        var f = new TextField();
        f.setPromptText(prompt);
        f.getStyleClass().add("np-filter");
        return f;
    }

    static VBox pane(Node header, Node... rest) {
        var box = new VBox(header);
        box.getChildren().addAll(rest);
        if (rest.length > 0) VBox.setVgrow(rest[rest.length - 1], Priority.ALWAYS);
        box.getStyleClass().add("np-pane");
        return box;
    }

    private Section() {}
}
