package prosefx.notepadfx.ui;

import prosefx.notepadfx.doc.*;
import java.util.regex.*;
import javafx.application.Platform;
import javafx.collections.*;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;

final class TabStrip {
    static final DataFormat DOC = new DataFormat("application/x-notepadfx-doc");
    final MainWindow w;
    /** Every open file of the window, in tab order; the list shows the ones the filter lets through. */
    final ObservableList<Doc> items = FXCollections.observableArrayList();
    final FilteredList<Doc> shown = new FilteredList<>(items);
    final ListView<Doc> list = new ListView<>(shown);
    final TextField filter = Section.filter("Filter (regex)");
    final VBox node;

    TabStrip(MainWindow w) {
        this.w = w;
        list.getStyleClass().add("np-tabs");
        list.setCellFactory(v -> new TabCell(this));
        list.getSelectionModel().selectedItemProperty().subscribe(d -> { if (d != null) w.active.set(d); });
        list.setOnDragOver(e -> { if (e.getDragboard().hasContent(DOC)) e.acceptTransferModes(TransferMode.MOVE); e.consume(); });
        list.setOnDragDropped(e -> dropped(e, list.getItems().size()));
        list.setPlaceholder(new Label(""));
        filter.textProperty().subscribe(this::refilter);
        filter.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ESCAPE) { filter.clear(); list.requestFocus(); e.consume(); } });
        VBox.setVgrow(list, Priority.ALWAYS);
        node = new VBox(Section.header("Open files"), filter, list);
        node.getStyleClass().add("np-pane");
        node.setMinWidth(120);
    }

    /** Files whose path matches the pattern (case does not matter); a pattern that is not a regex is looked for as plain text. */
    void refilter(String text) {
        var bad = false;
        if (text == null || text.isBlank()) shown.setPredicate(null);
        else {
            Pattern p;
            try { p = Pattern.compile(text.strip(), Pattern.CASE_INSENSITIVE); }
            catch (PatternSyntaxException e) { p = Pattern.compile(Pattern.quote(text.strip()), Pattern.CASE_INSENSITIVE); bad = true; }
            var found = p;
            shown.setPredicate(d -> found.matcher(d.path.toString()).find());
        }
        filter.getStyleClass().remove("np-bad");
        if (bad) filter.getStyleClass().add("np-bad");
        Platform.runLater(() -> { var a = w.active.get(); if (a != null && shown.contains(a)) list.getSelectionModel().select(a); });
    }

    void dropped(DragEvent e, int index) {
        var path = (String) e.getDragboard().getContent(DOC);
        var doc = path == null ? null : Docs.find(java.nio.file.Path.of(path)).orElse(null);
        if (doc != null) {
            var at = index >= shown.size() ? items.size() : items.indexOf(shown.get(index));
            var old = items.indexOf(doc);
            if (old >= 0 && old < at) at--;
            Windows.move(doc, w, at);
        }
        e.setDropCompleted(doc != null);
        e.consume();
    }

    ObservableList<Doc> docs() { return items; }

    boolean contains(Doc d) { return items.contains(d); }

    boolean isEmpty() { return items.isEmpty(); }

    int size() { return items.size(); }

    void insert(Doc d, int index) {
        if (contains(d)) items.remove(d);
        var i = index < 0 || index > size() ? size() : index;
        items.add(i, d);
    }

    void select(Doc d) {
        if (!shown.contains(d)) filter.clear();
        list.getSelectionModel().select(d);
        list.scrollTo(d);
    }

    void detach(Doc d) {
        var i = items.indexOf(d);
        items.remove(d);
        if (w.active.get() == d) after(i);
    }

    void remove(Doc d) {
        detach(d);
        Docs.close(d);
        w.nav.refreshOpen();
    }

    void after(int removedIndex) {
        if (isEmpty()) { w.tabsEmptied(); return; }
        w.show(items.get(Math.min(removedIndex, size() - 1)));
    }

    void step(int by) {
        if (isEmpty()) return;
        var i = Math.max(0, items.indexOf(w.active.get()));
        w.show(items.get(Math.floorMod(i + by, size())));
    }

    void focus() { list.requestFocus(); }
}
