package prosemirrorfx.notepadfx.ui;

import java.nio.file.*;
import javafx.scene.control.*;

final class PathCell extends TreeCell<Path> {
    final boolean workspaces;

    PathCell(boolean workspaces) { this.workspaces = workspaces; }

    @Override
    protected void updateItem(Path p, boolean empty) {
        super.updateItem(p, empty);
        getStyleClass().removeAll("np-dir", "np-file", "np-md", "np-ws", "np-open");
        if (empty) { setText(null); setGraphic(null); setTooltip(null); return; }
        if (p == null) { setText("Other open files"); getStyleClass().add("np-ws"); return; }
        var top = workspaces && getTreeItem().getParent() != null && getTreeItem().getParent().getParent() == null;
        setText(top ? label(p) : p.getFileName() == null ? p.toString() : p.getFileName().toString());
        getStyleClass().add(top ? "np-ws" : workspaces ? "np-open" : kind(p));
        setTooltip(new Tooltip(p.toString()));
    }

    static String label(Path p) {
        var name = p.getFileName() == null ? p.toString() : p.getFileName().toString();
        var parent = p.getParent() == null ? "" : "  " + p.getParent();
        return name + parent;
    }

    static String kind(Path p) {
        if (Files.isDirectory(p)) return "np-dir";
        var n = p.getFileName().toString().toLowerCase();
        return n.endsWith(".md") ? "np-md" : "np-file";
    }
}
