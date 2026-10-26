package io.github.prosemirrorfx.notepadfx.ui;

import io.github.prosemirrorfx.notepadfx.doc.Doc;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

final class StatusBar {
    final MainWindow w;
    final Label info = new Label();
    final HBox node = new HBox(2);
    final ChangeListener<Object> update = (o, a, b) -> refresh();
    Doc doc;

    StatusBar(MainWindow w) {
        this.w = w;
        for (var a : Action.values()) {
            var b = new Button(a.key.getName() + " " + a.label);
            b.getStyleClass().add("np-legend");
            b.setFocusTraversable(false);
            b.setMnemonicParsing(false);
            b.setOnAction(e -> a.run(w));
            node.getChildren().add(b);
        }
        var spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        info.getStyleClass().add("np-info");
        node.getChildren().addAll(spacer, info);
        node.setAlignment(Pos.CENTER_LEFT);
        node.getStyleClass().add("np-status");
        w.active.subscribe(this::attach);
    }

    void attach(Doc d) {
        if (doc != null) { doc.dirty.removeListener(update); doc.editor().caretLine().removeListener(update); }
        doc = d;
        if (d != null) { d.dirty.addListener(update); d.editor().caretLine().addListener(update); }
        refresh();
    }

    void refresh() {
        if (doc == null) { info.setText(""); return; }
        var line = doc.editor().caretLine().getValue().intValue() + 1;
        info.setText((doc.dirty.get() ? "● unsaved  " : "") + "Ln " + line + "   " + doc.path);
    }
}
