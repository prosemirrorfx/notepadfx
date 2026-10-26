package io.github.prosemirrorfx.notepadfx.ui;

import io.github.prosemirrorfx.notepadfx.doc.Doc;
import io.github.prosemirrorfx.notepadfx.toc.*;
import java.util.List;
import javafx.animation.PauseTransition;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

final class TocPane {
    final MainWindow w;
    final TreeView<OutlineNode> tree = new TreeView<>(new TreeItem<>());
    final VBox node;
    final PauseTransition debounce = new PauseTransition(Duration.millis(300));
    final ChangeListener<String> onText = (o, a, b) -> debounce.playFromStart();
    final ChangeListener<Number> onCaret = (o, a, b) -> follow(b.intValue());
    Doc doc;

    TocPane(MainWindow w) {
        this.w = w;
        tree.setShowRoot(false);
        tree.getStyleClass().addAll("np-tree", "np-toc");
        tree.setCellFactory(t -> new TocCell(tree));
        tree.setOnMouseClicked(e -> { if (e.getButton() == MouseButton.PRIMARY) jump(); });
        tree.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) jump(); });
        debounce.setOnFinished(e -> rebuild());
        node = Section.pane(Section.header("Outline",
            Section.tool("•", "Show or hide bullets", () -> toggle(true)),
            Section.tool("¶", "Show or hide paragraphs", () -> toggle(false))), tree);
        node.setMinWidth(140);
        w.active.subscribe(this::attach);
    }

    void attach(Doc d) {
        if (doc != null) {
            doc.textProperty().removeListener(onText);
            if (doc.hasEditor()) doc.editor().caretLine().removeListener(onCaret);
        }
        doc = d;
        if (d != null) {
            d.textProperty().addListener(onText);
            d.editor().caretLine().addListener(onCaret);
        }
        rebuild();
    }

    void rebuild() {
        var root = tree.getRoot();
        root.getChildren().clear();
        if (doc == null) return;
        add(root, Outline.parse(doc.text()));
    }

    void toggle(boolean bullets) {
        var s = Windows.session;
        if (bullets) s.outlineBullets(!s.outlineBullets()); else s.outlineParagraphs(!s.outlineParagraphs());
        Windows.ALL.forEach(x -> x.toc.rebuild());
        Windows.persist();
    }

    static boolean shown(OutlineNode n) {
        return switch (n.type()) {
            case "bullet" -> Windows.session.outlineBullets();
            case "para" -> Windows.session.outlineParagraphs();
            default -> true;
        };
    }

    static void add(TreeItem<OutlineNode> parent, List<OutlineNode> nodes) {
        for (var n : nodes) {
            if (!shown(n)) continue;
            var item = new TreeItem<>(n);
            item.setExpanded(true);
            add(item, n.children());
            parent.getChildren().add(item);
        }
    }

    void jump() {
        var item = tree.getSelectionModel().getSelectedItem();
        if (item != null && doc != null) doc.editor().goToLine(item.getValue().line());
    }

    void follow(int line) {
        if (tree.isFocused()) return;
        TreeItem<OutlineNode> best = null;
        for (var i = 0; i < tree.getExpandedItemCount(); i++) {
            var item = tree.getTreeItem(i);
            if (item != null && item.getValue() != null && item.getValue().line() <= line) best = item;
        }
        if (best != null) {
            tree.getSelectionModel().select(best);
            var row = tree.getRow(best);
            if (row >= 0) tree.scrollTo(Math.max(0, row - 3));
        }
    }

    void focus() { tree.requestFocus(); }
}
