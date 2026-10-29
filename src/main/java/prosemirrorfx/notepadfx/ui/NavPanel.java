package prosemirrorfx.notepadfx.ui;

import javafx.geometry.Orientation;
import javafx.scene.control.SplitPane;

final class NavPanel {
    final MainWindow w;
    final WorkspacePane workspaces;
    final ProjectPane projects;
    final FilePane files;
    final SplitPane node = new SplitPane();
    boolean ready;

    NavPanel(MainWindow w) {
        this.w = w;
        workspaces = new WorkspacePane(w, this);
        projects = new ProjectPane(w, this);
        files = new FilePane(w, this);
        node.setOrientation(Orientation.VERTICAL);
        node.getItems().setAll(workspaces.node, projects.node, files.node);
        var at = w.state.navSplit();
        node.setDividerPositions(at != null && at.size() == 2 ? new double[]{at.get(0), at.get(1)} : new double[]{0.22, 0.48});
        node.getStyleClass().add("np-nav");
        node.setMinWidth(160);
    }

    void restore() {
        ready = true;
        workspaces.reload();
        projects.reload();
    }

    void workspaceChosen(String path) {
        if (!ready) return;
        if (path != null && path.equals(w.state.workspace())) return;
        w.state.workspace(path);
        w.state.project(null);
        projects.reload();
        files.show(null);
    }

    void projectChosen(String path) {
        if (!ready) return;
        w.state.project(path);
        files.show(path);
    }

    void refresh() {
        workspaces.reload();
        projects.reload();
        files.reload();
    }

    void refreshOpen() { workspaces.reload(); }
}
