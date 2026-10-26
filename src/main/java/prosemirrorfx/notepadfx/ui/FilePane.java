package prosemirrorfx.notepadfx.ui;

import java.nio.file.*;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.VBox;

final class FilePane {
    final MainWindow w;
    final NavPanel nav;
    final TreeView<Path> tree = new TreeView<>();
    final TextField filter = Section.filter("Filter files (regex)");
    final VBox node;
    String root;
    int generation;

    FilePane(MainWindow w, NavPanel nav) {
        this.w = w;
        this.nav = nav;
        tree.setShowRoot(false);
        tree.getStyleClass().add("np-tree");
        tree.setCellFactory(t -> new PathCell(false));
        tree.setOnMouseClicked(e -> { if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) openSelected(); });
        tree.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) openSelected(); });
        tree.setContextMenu(FileMenu.forTree(w, tree, this::reload));
        filter.textProperty().subscribe(t -> filterChanged());
        filter.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.DOWN) { tree.requestFocus(); tree.getSelectionModel().selectFirst(); } });
        node = Section.pane(Section.header("Files"), filter, tree);
    }

    void openSelected() {
        var item = tree.getSelectionModel().getSelectedItem();
        if (item != null && Files.isRegularFile(item.getValue())) Windows.open(item.getValue());
    }

    void show(String project) {
        root = project;
        var ws = Workspaces.of(w.state.workspace());
        var saved = ws == null || project == null ? "" : ws.fileFilters().getOrDefault(project, "");
        if (!filter.getText().equals(saved)) filter.setText(saved);
        else reload();
    }

    void filterChanged() {
        var ws = Workspaces.of(w.state.workspace());
        if (ws != null && root != null) ws.fileFilters().put(root, filter.getText());
        reload();
    }

    void reload() {
        var gen = ++generation;
        if (root == null) { tree.setRoot(null); return; }
        var dir = Path.of(root);
        var regex = filter.getText();
        if (regex.isBlank()) { tree.setRoot(new FileTreeItem(dir)); tree.getRoot().setExpanded(true); return; }
        var match = ProjectPane.matcher(regex);
        Thread.ofVirtual().start(() -> {
            var found = FileTreeItem.filtered(dir, match);
            Platform.runLater(() -> { if (gen == generation) tree.setRoot(found); });
        });
    }
}
