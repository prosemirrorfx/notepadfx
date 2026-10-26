package prosemirrorfx.notepadfx.ui;

import prosemirrorfx.notepadfx.doc.*;
import prosemirrorfx.notepadfx.state.WindowState;
import java.nio.file.*;
import java.util.*;
import prosemirrorfx.notepadfx.sys.*;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.InputEvent;
import javafx.scene.layout.*;
import javafx.stage.*;

public final class MainWindow {
    final Stage stage = new Stage();
    final WindowState state;
    final ObjectProperty<Doc> active = new SimpleObjectProperty<>();
    final TabStrip tabs;
    final NavPanel nav;
    final TocPane toc;
    final EditorArea area;
    final SplitPane split = new SplitPane();
    final BorderPane root = new BorderPane();

    MainWindow(WindowState state) {
        this.state = state;
        tabs = new TabStrip(this);
        nav = new NavPanel(this);
        toc = new TocPane(this);
        area = new EditorArea(this);
        root.getStyleClass().add("np-root");
        root.setLeft(Ribbon.build(this));
        root.setCenter(split);
        root.setBottom(new StatusBar(this).node);
        SplitPane.setResizableWithParent(nav.node, false);
        SplitPane.setResizableWithParent(tabs.node, false);
        SplitPane.setResizableWithParent(toc.node, false);
        layoutPanes();
        var scene = new Scene(root, state.width(), state.height());
        scene.getStylesheets().add(Theme.css());
        Keys.install(scene, this);
        scene.addEventFilter(InputEvent.ANY, e -> IdleTrim.touch());
        stage.setScene(scene);
        Icons.apply(stage);
        applyTheme();
        stage.setTitle("notepadfx");
        stage.focusedProperty().subscribe(f -> { if (f) Windows.focused = this; });
        stage.setOnCloseRequest(e -> { if (!Prompts.confirmClose(this, tabs.docs())) e.consume(); else close(); });
        active.subscribe(this::activate);
        restoreGeometry();
        nav.restore();
        for (var f : state.files()) if (Files.isRegularFile(Path.of(f))) tabs.insert(Docs.of(Path.of(f)), -1);
        Timing.mark("window built");
    }

    void show() {
        stage.show();
        Timing.mark("shown");
        Platform.runLater(this::restoreActive);
    }

    void restoreActive() {
        if (active.get() != null) return;
        var restored = state.activeFile() == null ? null : Docs.find(Path.of(state.activeFile())).filter(tabs::contains).orElse(null);
        if (restored == null && !tabs.isEmpty()) restored = tabs.docs().getFirst();
        if (restored != null) show(restored);
        Timing.mark("file shown");
    }

    void show(Doc d) {
        if (!tabs.contains(d)) tabs.insert(d, -1);
        tabs.select(d);
        active.set(d);
        stage.toFront();
    }

    void activate(Doc d) { area.show(d); }

    void layoutPanes() {
        var items = new ArrayList<Node>();
        if (state.navVisible()) items.add(nav.node);
        items.add(tabs.node);
        items.add(area.node);
        if (state.tocVisible()) items.add(toc.node);
        split.getItems().setAll(items);
        placeDividers();
        Platform.runLater(this::placeDividers);
    }

    void placeDividers() {
        var w = split.getWidth() > 0 ? split.getWidth() : state.width();
        var x = 0.0;
        var i = 0;
        if (state.navVisible()) split.setDividerPosition(i++, (x += state.navWidth()) / w);
        split.setDividerPosition(i++, (x += state.tabsWidth()) / w);
        if (state.tocVisible()) split.setDividerPosition(i, 1 - state.tocWidth() / w);
    }

    void restoreGeometry() {
        state.width(Math.max(640, state.width())).height(Math.max(420, state.height()));
        stage.setWidth(state.width());
        stage.setHeight(state.height());
        if (state.x() != null && onScreen(state.x(), state.y())) { stage.setX(state.x()); stage.setY(state.y()); }
        stage.setMaximized(Boolean.TRUE.equals(state.maximized()));
    }

    static boolean onScreen(double x, double y) {
        return !Screen.getScreensForRectangle(x + 40, y + 10, 80, 20).isEmpty();
    }

    WindowState snapshot() {
        if (!stage.isIconified() && stage.isShowing()) {
            if (!stage.isMaximized()) state.x(stage.getX()).y(stage.getY()).width(stage.getWidth()).height(stage.getHeight());
            state.maximized(stage.isMaximized());
            if (state.navVisible() && nav.node.getWidth() > 0) state.navWidth(nav.node.getWidth());
            if (tabs.node.getWidth() > 0) state.tabsWidth(tabs.node.getWidth());
            if (state.tocVisible() && toc.node.getWidth() > 0) state.tocWidth(toc.node.getWidth());
        }
        state.files(new ArrayList<>(tabs.docs().stream().map(d -> d.path.toString()).toList()));
        state.activeFile(active.get() == null ? null : active.get().path.toString());
        area.rememberCaret();
        return state;
    }

    void applyTheme() {
        area.reapply();
        root.getStyleClass().removeAll("dark", "light");
        root.getStyleClass().add(Windows.theme());
    }

    void close() {
        snapshot();
        Windows.closed(this);
        tabs.docs().forEach(Docs::close);
        stage.close();
    }

    void save() { Prompts.save(this, active.get()); }

    void closeActive() {
        var d = active.get();
        if (d != null && Prompts.confirmClose(this, List.of(d))) tabs.remove(d);
    }

    void refresh() {
        nav.refresh();
        var d = active.get();
        if (d != null && !d.dirty.get()) d.reload();
    }

    void toggleNav() { state.navVisible(!state.navVisible()); snapshot(); layoutPanes(); }

    void toggleToc() { state.tocVisible(!state.tocVisible()); snapshot(); layoutPanes(); }

    void tearOffActive() {
        var d = active.get();
        if (d != null) Windows.tearOff(d, stage.getX() + 160, stage.getY() + 60);
    }

    void newFile() {
        var p = Prompts.newFile(this);
        if (p != null) Windows.open(p);
    }

    void openDialog() { Prompts.openFiles(this).forEach(Windows::open); }

    void nextPane() { Focus.next(this); }

    void toggleSource() {
        var d = active.get();
        if (d == null || !d.toggleSource()) return;
        Folds.restore(d);
        Folds.track(d);
        active.set(null);
        active.set(d);
    }

    void tabsEmptied() { active.set(null); }
}
