package io.github.prosemirrorfx.notepadfx.ui;

import io.github.prosemirrorfx.notepadfx.toc.OutlineNode;
import javafx.scene.control.*;
import javafx.scene.layout.Region;

final class TocCell extends TreeCell<OutlineNode> {
    final TreeView<OutlineNode> tree;
    final Label label = new Label();

    TocCell(TreeView<OutlineNode> tree) {
        this.tree = tree;
        label.setWrapText(true);
        label.setMinHeight(Region.USE_PREF_SIZE);
        label.getStyleClass().add("np-toc-label");
    }

    @Override
    protected double computePrefHeight(double width) {
        if (getGraphic() == null || label.getMaxWidth() <= 0) return super.computePrefHeight(width);
        return label.prefHeight(label.getMaxWidth()) + snappedTopInset() + snappedBottomInset();
    }

    @Override
    protected void updateItem(OutlineNode n, boolean empty) {
        super.updateItem(n, empty);
        getStyleClass().removeAll("np-h1", "np-h2", "np-h3", "np-bullet", "np-para");
        label.maxWidthProperty().unbind();
        label.prefWidthProperty().unbind();
        if (empty || n == null) { setGraphic(null); setText(null); return; }
        var level = Math.max(0, tree.getTreeItemLevel(getTreeItem()) - 1);
        var width = tree.widthProperty().subtract(level * 14 + 46);
        label.maxWidthProperty().bind(width);
        label.prefWidthProperty().bind(width);
        label.setText(n.label());
        getStyleClass().add(switch (n.type()) {
            case "heading" -> "np-h" + Math.min(3, n.level());
            case "bullet" -> "np-bullet";
            default -> "np-para";
        });
        setText(null);
        setGraphic(label);
    }
}
