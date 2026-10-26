package prosemirrorfx.notepadfx.ui;

import prosemirrorfx.notepadfx.doc.Doc;
import java.util.*;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;

final class TabCell extends ListCell<Doc> {
    final TabStrip strip;
    final Label chip = new Label(), name = new Label(), dot = new Label("●");
    final Button close = new Button("✕");
    final Region spacer = new Region();
    final HBox top = new HBox(6);
    final VBox box = new VBox(2);

    TabCell(TabStrip strip) {
        this.strip = strip;
        chip.getStyleClass().add("np-chip");
        name.getStyleClass().add("np-tab-name");
        name.setWrapText(true);
        name.setMinHeight(Region.USE_PREF_SIZE);
        dot.getStyleClass().add("np-dirty");
        close.getStyleClass().add("np-tab-close");
        close.setFocusTraversable(false);
        close.setOnAction(e -> { if (getItem() != null) closeDoc(getItem()); });
        HBox.setHgrow(spacer, Priority.ALWAYS);
        top.setAlignment(Pos.CENTER_LEFT);
        setOnDragDetected(this::dragStart);
        setOnDragOver(e -> { if (e.getDragboard().hasContent(TabStrip.DOC)) e.acceptTransferModes(TransferMode.MOVE); e.consume(); });
        setOnDragDropped(e -> strip.dropped(e, isEmpty() ? strip.size() : getIndex()));
        setOnDragDone(this::dragDone);
        setOnMouseClicked(e -> { if (e.getButton() == MouseButton.MIDDLE && getItem() != null) closeDoc(getItem()); });
    }

    void closeDoc(Doc d) {
        if (Prompts.confirmClose(strip.w, List.of(d))) strip.remove(d);
    }

    void dragStart(MouseEvent e) {
        if (getItem() == null) return;
        var db = startDragAndDrop(TransferMode.MOVE);
        db.setContent(Map.of(TabStrip.DOC, getItem().path.toString()));
        db.setDragView(snapshot(null, null));
        e.consume();
    }

    void dragDone(DragEvent e) {
        if (e.getTransferMode() == null && getItem() != null) Windows.tearOff(getItem(), e.getScreenX(), e.getScreenY());
        e.consume();
    }

    void arrange(String chipText) {
        name.maxWidthProperty().unbind();
        name.prefWidthProperty().unbind();
        if (chipText == null) {
            top.getChildren().setAll(name, spacer, dot, close);
            box.getChildren().setAll(top);
            name.maxWidthProperty().bind(strip.list.widthProperty().subtract(64));
            name.prefWidthProperty().bind(strip.list.widthProperty().subtract(64));
        } else {
            chip.setText(chipText);
            top.getChildren().setAll(chip, spacer, dot, close);
            box.getChildren().setAll(top, name);
            name.maxWidthProperty().bind(strip.list.widthProperty().subtract(36));
            name.prefWidthProperty().bind(strip.list.widthProperty().subtract(36));
        }
    }

    @Override
    protected double computePrefHeight(double width) {
        if (getGraphic() == null || name.getMaxWidth() <= 0) return super.computePrefHeight(width);
        var nameHeight = name.prefHeight(name.getMaxWidth());
        var rows = box.getChildren().size() == 2 ? top.prefHeight(-1) + 2 + nameHeight : Math.max(top.prefHeight(-1), nameHeight);
        return rows + snappedTopInset() + snappedBottomInset();
    }

    @Override
    protected void updateItem(Doc d, boolean empty) {
        super.updateItem(d, empty);
        dot.visibleProperty().unbind();
        if (empty || d == null) { setGraphic(null); setTooltip(null); return; }
        var label = TabLabel.of(d.path);
        arrange(label.chip());
        name.setText(label.name());
        dot.visibleProperty().bind(d.dirty);
        setTooltip(new Tooltip(d.path.toString()));
        setGraphic(box);
    }
}
