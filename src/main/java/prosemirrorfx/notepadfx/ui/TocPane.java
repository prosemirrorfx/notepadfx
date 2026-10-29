package prosemirrorfx.notepadfx.ui;

import prosemirrorfx.notepadfx.doc.Doc;
import prosemirrorfx.notepadfx.toc.*;
import java.util.*;
import java.util.regex.PatternSyntaxException;
import javafx.animation.PauseTransition;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

/**
 * The outline as a bird's-eye view: which kinds of entry show (H1..H6, paragraphs, bullets by depth) is chosen with
 * the buttons under the title, a regex narrows it further, each entry is one line with its source line number.
 * The choice is the same in every window and remembered (session.json), which keeps it simple.
 */
final class TocPane {
    final MainWindow w;
    final TreeView<OutlineNode> tree = new TreeView<>(new TreeItem<>());
    final TextField regex = Section.filter("Filter outline (regex)");
    final FlowPane kinds = new FlowPane(2, 2);
    final VBox node;
    final PauseTransition debounce = new PauseTransition(Duration.millis(300));
    final ChangeListener<Number> onText = (o, a, b) -> debounce.playFromStart();
    final ChangeListener<Number> onCaret = (o, a, b) -> follow(b.intValue());
    final Set<OutlineNode> context = new HashSet<>();
    Doc doc;

    TocPane(MainWindow w) {
        this.w = w;
        tree.setShowRoot(false);
        tree.getStyleClass().addAll("np-tree", "np-toc");
        tree.setCellFactory(t -> new TocCell(this));
        tree.setOnMouseClicked(e -> { if (e.getButton() == MouseButton.PRIMARY) jump(); });
        tree.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) jump(); });
        debounce.setOnFinished(e -> rebuild());
        kinds.getStyleClass().add("np-kinds");
        for (var k : OutlineFilter.KINDS) kinds.getChildren().add(kindButton(k));
        regex.setText(Windows.session.outlineFilter());
        regex.textProperty().addListener((o, a, b) -> { Windows.session.outlineFilter(b); debounce.playFromStart(); });
        regex.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.DOWN || e.getCode() == KeyCode.ENTER) { tree.requestFocus(); tree.getSelectionModel().selectFirst(); } });
        node = Section.pane(Section.header("Outline",
            Section.tool("⊟", "Collapse all", () -> expand(tree.getRoot(), false)),
            Section.tool("⊞", "Expand all", () -> expand(tree.getRoot(), true))), kinds, regex, tree);
        node.setMinWidth(140);
        w.active.subscribe(this::attach);
    }

    ToggleButton kindButton(String k) {
        var b = new ToggleButton(switch (k) { case "para" -> "¶"; case "b1" -> "•"; case "b2" -> "••"; case "b3" -> "•••"; default -> k.toUpperCase(); });
        b.getStyleClass().add("np-kind");
        b.setFocusTraversable(false);
        b.setMnemonicParsing(false);
        b.setTooltip(new Tooltip(switch (k) { case "para" -> "Paragraphs"; case "b1" -> "Bullets"; case "b2" -> "Sub-bullets"; case "b3" -> "Bullets three or more deep"; default -> "Headings " + k.toUpperCase(); }));
        b.setSelected(Windows.session.outlineShow().contains(k));
        b.setOnAction(e -> {
            var show = new ArrayList<>(Windows.session.outlineShow());
            show.remove(k);
            if (b.isSelected()) show.add(k);
            Windows.session.outlineShow(show);
            Windows.ALL.forEach(x -> x.toc.sync());
            Windows.persist();
        });
        return b;
    }

    /** Another window changed the shared choice: reflect it here. */
    void sync() {
        for (var n : kinds.getChildren()) if (n instanceof ToggleButton t) t.setSelected(Windows.session.outlineShow().contains(OutlineFilter.KINDS.get(kinds.getChildren().indexOf(n))));
        rebuild();
    }

    void attach(Doc d) {
        if (doc != null) {
            doc.version.removeListener(onText);
            if (doc.hasEditor()) doc.editor().caret().removeListener(onCaret);
        }
        doc = d;
        if (d != null) {
            d.version.addListener(onText);
            d.editor().caret().addListener(onCaret);
        }
        rebuild();
    }

    void rebuild() {
        var root = tree.getRoot();
        root.getChildren().clear();
        context.clear();
        regex.getStyleClass().remove("np-bad");
        if (doc == null) return;
        java.util.regex.Pattern p;
        try {
            p = OutlineFilter.compile(regex.getText());
        } catch (PatternSyntaxException e) {
            regex.getStyleClass().add("np-bad");
            p = null;
        }
        var filter = new OutlineFilter(Windows.session.outlineShow(), p);
        root.getChildren().setAll(filter.apply(doc.editor().outline(), (n, kids, match) -> {
            if (!match) context.add(n);
            var item = new TreeItem<>(n);
            item.setExpanded(true);
            item.getChildren().setAll(kids);
            return item;
        }));
    }

    static void expand(TreeItem<OutlineNode> item, boolean open) {
        for (var c : item.getChildren()) {
            c.setExpanded(open);
            expand(c, open);
        }
    }

    void jump() {
        var item = tree.getSelectionModel().getSelectedItem();
        if (item != null && doc != null) doc.editor().goTo(item.getValue().line());
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
