package prosemirrorfx.notepadfx.ui;

import prosemirrorfx.notepadfx.doc.Doc;
import prosemirrorfx.notepadfx.editor.Editor;
import prosemirrorfx.notepadfx.sys.Shell;
import java.util.function.Consumer;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

final class AppBar {
    final MainWindow w;
    final HBox slot = new HBox();
    final Label zoomLabel = new Label();
    final FlowPane node = new FlowPane(2, 2);

    AppBar(MainWindow w) {
        this.w = w;
        zoomLabel.getStyleClass().add("np-zoom");
        zoomLabel.setContextMenu(zoomMenu());
        zoomLabel.setOnMouseClicked(e -> zoomLabel.getContextMenu().show(zoomLabel, e.getScreenX(), e.getScreenY()));
        node.getChildren().addAll(
            tool("note_add", "New file", w::newFile), tool("folder_open", "Open (F12)", w::openDialog), tool("save", "Save (F2)", w::save), sep(),
            tool("content_cut", "Cut (Ctrl+X)", () -> onEditor(Editor::cut)), copy(), paste(), sep(),
            tool("link", "Copy file path", () -> onDoc(d -> Shell.copy(d.path))),
            tool("drive_folder_upload", "Copy folder path", () -> onDoc(d -> Shell.copy(d.path.getParent()))),
            tool("folder_open", "Show in Explorer", () -> onDoc(d -> Shell.reveal(d.path))),
            tool("terminal", "Terminal in this folder", () -> onDoc(d -> Shell.terminal(d.path))),
            tool("open_in_new", "Open in the default app", () -> onDoc(d -> Shell.open(d.path))), sep(),
            tool("remove", "Zoom out", () -> w.area.stepZoom(-1)), zoomLabel, tool("add", "Zoom in", () -> w.area.stepZoom(1)),
            tool("settings", "Settings (F1)", () -> SettingsDialog.show(w)), sep(),
            slot);
        node.setAlignment(Pos.CENTER_LEFT);
        node.setRowValignment(VPos.CENTER);
        node.getStyleClass().add("np-appbar");
    }

    void editorToolbar(Node toolbar) {
        if (toolbar == null) slot.getChildren().clear();
        else slot.getChildren().setAll(toolbar);
    }

    void zoomShown(double zoom, boolean override) {
        zoomLabel.setText(Math.round(zoom * 100) + "%" + (override ? " •" : ""));
        zoomLabel.setTooltip(new Tooltip(override ? "This window's zoom (click for options)" : "App zoom (click for options)"));
    }

    ContextMenu zoomMenu() {
        return new ContextMenu(
            FileMenu.item("Use the app zoom in this window", () -> w.area.windowZoom(null)),
            FileMenu.item("Make this zoom the app zoom", () -> w.area.makeAppZoom()),
            FileMenu.item("Reset this window to 100%", () -> w.area.windowZoom(1.0)));
    }

    SplitMenuButton copy() {
        var b = split("content_copy", "Copy (Ctrl+C); arrow: copy the whole file as...", () -> onEditor(Editor::copy));
        for (var f : Editor.Copy.values())
            b.getItems().add(FileMenu.item("Copy all as " + f.name().toLowerCase(), () -> onEditor(e -> e.copyAll(f))));
        return b;
    }

    SplitMenuButton paste() {
        var b = split("content_paste", "Paste (Ctrl+V); arrow: paste as plain text", () -> onEditor(e -> e.paste(false)));
        b.getItems().add(FileMenu.item("Paste as plain text", () -> onEditor(e -> e.paste(true))));
        return b;
    }

    void onDoc(Consumer<Doc> act) { if (w.active.get() != null) act.accept(w.active.get()); }

    void onEditor(Consumer<Editor> act) { onDoc(d -> act.accept(d.editor())); }

    static Button tool(String icon, String tip, Runnable run) {
        var b = new Button();
        style(b, icon, tip);
        b.setOnAction(e -> run.run());
        return b;
    }

    static SplitMenuButton split(String icon, String tip, Runnable run) {
        var b = new SplitMenuButton();
        style(b, icon, tip);
        b.getStyleClass().add("np-split");
        b.setOnAction(e -> run.run());
        return b;
    }

    static void style(ButtonBase b, String icon, String tip) {
        b.setGraphic(Glyphs.of(icon));
        b.setTooltip(new Tooltip(tip));
        b.setFocusTraversable(false);
        b.setMnemonicParsing(false);
        b.getStyleClass().add("np-bar-button");
    }

    static Separator sep() {
        var s = new Separator(Orientation.VERTICAL);
        s.getStyleClass().add("np-bar-sep");
        return s;
    }
}
