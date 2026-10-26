package prosemirrorfx.notepadfx.ui;

import prosemirrorfx.notepadfx.doc.Doc;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.scene.layout.*;

final class EditorArea {
    static final double STEP = 0.1, MIN = 0.5, MAX = 3.0;
    final MainWindow w;
    final StackPane host = new StackPane();
    final AppBar bar;
    final VBox node;
    final ChangeListener<Number> onEditorZoom = (o, a, b) -> editorZoomed(b.doubleValue());
    final ChangeListener<Boolean> onDirty = (o, a, b) -> title();
    Doc shown;
    boolean applying;

    EditorArea(MainWindow w) {
        this.w = w;
        bar = new AppBar(w);
        host.getStyleClass().add("np-editor-host");
        host.getChildren().setAll(Empty.view());
        VBox.setVgrow(host, Priority.ALWAYS);
        node = new VBox(bar.node, host);
        refreshZoom();
    }

    void show(Doc d) {
        if (shown != null && shown.hasEditor()) {
            shown.editor().zoomChanges().removeListener(onEditorZoom);
            shown.dirty.removeListener(onDirty);
            Windows.session.carets().put(shown.path.toString(), shown.editor().position());
        }
        shown = d;
        title();
        if (d == null) { host.getChildren().setAll(Empty.view()); bar.editorToolbar(null); return; }
        var fresh = !d.hasEditor();
        var ed = d.editor();
        if (fresh) {
            ed.lineNumbers(Windows.session.lineNumbers());
            var caret = Windows.session.carets().get(d.path.toString());
            if (caret != null) ed.position(caret);
        }
        apply(d);
        ed.zoomChanges().addListener(onEditorZoom);
        d.dirty.addListener(onDirty);
        title();
        host.getChildren().setAll(ed.node());
        bar.editorToolbar(ed.toolbar());
        Platform.runLater(ed::focus);
    }

    void apply(Doc d) {
        applying = true;
        try {
            d.editor().zoom(zoom());
            d.editor().theme("dark".equals(Windows.theme()));
        } finally {
            applying = false;
        }
    }

    void title() { w.stage.setTitle(shown == null ? "notepadfx" : Titles.of(w, shown)); }

    double zoom() { return w.state.zoom() != null ? w.state.zoom() : Windows.session.zoom(); }

    void stepZoom(int dir) { windowZoom(clamp(Math.round((zoom() + dir * STEP) * 10) / 10.0)); }

    void windowZoom(Double z) {
        w.state.zoom(z);
        reapply();
        Windows.persist();
    }

    void makeAppZoom() {
        Windows.session.zoom(zoom());
        w.state.zoom(null);
        Windows.ALL.forEach(x -> x.area.reapply());
        Windows.persist();
    }

    void editorZoomed(double z) {
        if (!applying && Math.abs(z - zoom()) > 0.001) windowZoom(clamp(z));
    }

    void reapply() {
        for (var d : w.tabs.docs()) if (d.hasEditor()) apply(d);
        refreshZoom();
    }

    void refreshZoom() { bar.zoomShown(zoom(), w.state.zoom() != null); }

    void rememberCaret() {
        if (shown != null && shown.hasEditor()) Windows.session.carets().put(shown.path.toString(), shown.editor().position());
    }

    static double clamp(double z) { return Math.max(MIN, Math.min(MAX, z)); }
}
