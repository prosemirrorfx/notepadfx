package prosemirrorfx.notepadfx.ui;

import prosemirrorfx.notepadfx.doc.*;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;

final class TabStrip {
    static final DataFormat DOC = new DataFormat("application/x-notepadfx-doc");
    final MainWindow w;
    final ListView<Doc> list = new ListView<>();
    final VBox node;

    TabStrip(MainWindow w) {
        this.w = w;
        list.getStyleClass().add("np-tabs");
        list.setCellFactory(v -> new TabCell(this));
        list.getSelectionModel().selectedItemProperty().subscribe(d -> { if (d != null) w.active.set(d); });
        list.setOnDragOver(e -> { if (e.getDragboard().hasContent(DOC)) e.acceptTransferModes(TransferMode.MOVE); e.consume(); });
        list.setOnDragDropped(e -> dropped(e, list.getItems().size()));
        list.setPlaceholder(new Label(""));
        VBox.setVgrow(list, Priority.ALWAYS);
        node = new VBox(Section.header("Open files"), list);
        node.getStyleClass().add("np-pane");
        node.setMinWidth(120);
    }

    void dropped(DragEvent e, int index) {
        var path = (String) e.getDragboard().getContent(DOC);
        var doc = path == null ? null : Docs.find(java.nio.file.Path.of(path)).orElse(null);
        if (doc != null) {
            var old = list.getItems().indexOf(doc);
            if (old >= 0 && old < index) index--;
            Windows.move(doc, w, index);
        }
        e.setDropCompleted(doc != null);
        e.consume();
    }

    ObservableList<Doc> docs() { return list.getItems(); }

    boolean contains(Doc d) { return list.getItems().contains(d); }

    boolean isEmpty() { return list.getItems().isEmpty(); }

    int size() { return list.getItems().size(); }

    void insert(Doc d, int index) {
        if (contains(d)) list.getItems().remove(d);
        var i = index < 0 || index > size() ? size() : index;
        list.getItems().add(i, d);
    }

    void select(Doc d) {
        list.getSelectionModel().select(d);
        list.scrollTo(d);
    }

    void detach(Doc d) {
        var i = list.getItems().indexOf(d);
        list.getItems().remove(d);
        if (w.active.get() == d) after(i);
    }

    void remove(Doc d) {
        detach(d);
        Docs.close(d);
        w.nav.refreshOpen();
    }

    void after(int removedIndex) {
        if (isEmpty()) { w.tabsEmptied(); return; }
        w.show(list.getItems().get(Math.min(removedIndex, size() - 1)));
    }

    void step(int by) {
        if (isEmpty()) return;
        var i = Math.max(0, list.getItems().indexOf(w.active.get()));
        w.show(list.getItems().get(Math.floorMod(i + by, size())));
    }

    void focus() { list.requestFocus(); }
}
