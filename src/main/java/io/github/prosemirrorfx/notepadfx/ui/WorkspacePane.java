package io.github.prosemirrorfx.notepadfx.ui;

import io.github.prosemirrorfx.notepadfx.doc.*;
import java.nio.file.Path;
import javafx.collections.ListChangeListener;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.VBox;

final class WorkspacePane {
    final MainWindow w;
    final NavPanel nav;
    final TreeView<Path> tree = new TreeView<>(new TreeItem<>());
    final VBox node;

    WorkspacePane(MainWindow w, NavPanel nav) {
        this.w = w;
        this.nav = nav;
        tree.setShowRoot(false);
        tree.getStyleClass().add("np-tree");
        tree.setCellFactory(t -> new PathCell(true));
        tree.getSelectionModel().selectedItemProperty().subscribe(this::chosen);
        tree.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) chosen(tree.getSelectionModel().getSelectedItem()); });
        tree.setContextMenu(FileMenu.forWorkspaces(w, this));
        Workspaces.LIST.addListener((ListChangeListener<Object>) c -> reload());
        w.tabs.docs().addListener((ListChangeListener<Doc>) c -> reload());
        node = Section.pane(Section.header("Workspaces", Section.tool("+", "Add a workspace folder", this::add)), tree);
        reload();
    }

    void add() {
        var dir = Prompts.chooseDir(w, "Add workspace folder");
        if (dir != null) { Workspaces.add(dir); select(dir.toString()); }
    }

    void chosen(TreeItem<Path> item) {
        if (item == null || item.getValue() == null) return;
        if (item.getParent() == tree.getRoot()) nav.workspaceChosen(item.getValue().toString());
        else Docs.find(item.getValue()).ifPresent(w::show);
    }

    void reload() {
        var selected = w.state.workspace();
        var roots = tree.getRoot().getChildren();
        roots.clear();
        for (var ws : Workspaces.LIST) {
            var dir = Path.of(ws.path());
            var item = new TreeItem<>(dir);
            for (var d : w.tabs.docs()) if (d.path.startsWith(dir)) item.getChildren().add(new TreeItem<>(d.path));
            item.setExpanded(true);
            roots.add(item);
        }
        var loose = w.tabs.docs().stream().filter(d -> Workspaces.LIST.stream().noneMatch(ws -> d.path.startsWith(Path.of(ws.path())))).toList();
        if (!loose.isEmpty()) {
            var other = new TreeItem<Path>(null);
            loose.forEach(d -> other.getChildren().add(new TreeItem<>(d.path)));
            other.setExpanded(true);
            roots.add(other);
        }
        selectQuietly(selected);
    }

    void select(String path) {
        selectQuietly(path);
        nav.workspaceChosen(path);
    }

    void selectQuietly(String path) {
        if (path == null) return;
        tree.getRoot().getChildren().stream().filter(i -> i.getValue() != null && i.getValue().toString().equalsIgnoreCase(path))
            .findFirst().ifPresent(i -> tree.getSelectionModel().select(i));
    }
}
