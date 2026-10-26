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
            Folds.restore(d);
            Folds.track(d);
        }
        apply(d);
        refreshZoom();
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
            d.editor().zoom(zoomFor(d));
            d.editor().theme("dark".equals(Windows.theme()));
        } finally {
            applying = false;
        }
    }

    void title() { w.stage.setTitle(shown == null ? "notepadfx" : Titles.of(w, shown)); }

    Double fileZoom(Doc d) { return d == null ? null : Windows.session.zooms().get(d.path.toString()); }

    double windowZoom() { return w.state.zoom() != null ? w.state.zoom() : Windows.session.zoom(); }

    double zoomFor(Doc d) { var f = fileZoom(d); return f != null ? f : windowZoom(); }

    double zoom() { return zoomFor(shown); }

    void stepZoom(int dir) { setFileZoom(clamp(Math.round((zoom() + dir * STEP) * 10) / 10.0)); }

    void setFileZoom(Double z) {
        if (shown == null) return;
        if (z == null) Windows.session.zooms().remove(shown.path.toString());
        else Windows.session.zooms().put(shown.path.toString(), z);
        apply(shown);
        refreshZoom();
        Windows.persist();
    }

    void windowZoom(Double z) {
        w.state.zoom(z);
        if (shown != null) Windows.session.zooms().remove(shown.path.toString());
        reapply();
        Windows.persist();
    }

    void makeAppZoom() {
        Windows.session.zoom(zoom());
        w.state.zoom(null);
        if (shown != null) Windows.session.zooms().remove(shown.path.toString());
        Windows.ALL.forEach(x -> x.area.reapply());
        Windows.persist();
    }

    void editorZoomed(double z) {
        if (!applying && Math.abs(z - zoom()) > 0.001) setFileZoom(clamp(z));
    }

    void reapply() {
        for (var d : w.tabs.docs()) if (d.hasEditor()) apply(d);
        refreshZoom();
    }

    void refreshZoom() {
        var level = fileZoom(shown) != null ? "this file" : w.state.zoom() != null ? "this window" : "the app";
        bar.zoomShown(zoom(), level);
    }

    void rememberCaret() {
        if (shown != null && shown.hasEditor()) Windows.session.carets().put(shown.path.toString(), shown.editor().position());
    }

    static double clamp(double z) { return Math.max(MIN, Math.min(MAX, z)); }
}
