package prosemirrorfx.notepadfx.ui;

import prosemirrorfx.notepadfx.toc.OutlineNode;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * One line per entry, cut with an ellipsis (the whole text on hover), and its source line number at the right.
 * The row is sized after the cell has placed it, so the label gets exactly the room left at its indent.
 */
final class TocCell extends TreeCell<OutlineNode> {
    final TocPane pane;
    final Label label = new Label(), number = new Label();
    final HBox row = new HBox(6, label, number);

    TocCell(TocPane pane) {
        this.pane = pane;
        label.setWrapText(false);
        label.setTextOverrun(OverrunStyle.ELLIPSIS);
        label.setMinWidth(0);
        label.setMaxWidth(Double.MAX_VALUE);
        label.getStyleClass().add("np-toc-label");
        HBox.setHgrow(label, Priority.ALWAYS);
        number.getStyleClass().add("np-toc-number");
        number.setMinWidth(Region.USE_PREF_SIZE);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMinWidth(0);
        row.setPrefWidth(10);
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        if (getGraphic() == row) row.resize(Math.max(0, getWidth() - row.getLayoutX() - snappedRightInset()), row.getHeight());
    }

    @Override
    protected void updateItem(OutlineNode n, boolean empty) {
        super.updateItem(n, empty);
        getStyleClass().removeAll("np-h1", "np-h2", "np-h3", "np-bullet", "np-para", "np-context");
        if (empty || n == null) { setGraphic(null); setText(null); setTooltip(null); return; }
        label.setText(n.label());
        number.setText(n.number() == null ? "" : String.valueOf(n.number()));
        setTooltip(new Tooltip(n.label()));
        getStyleClass().add(switch (n.type()) {
            case "heading" -> "np-h" + Math.min(3, n.level());
            case "bullet" -> "np-bullet";
            default -> "np-para";
        });
        if (pane.context.contains(n)) getStyleClass().add("np-context");
        setText(null);
        setGraphic(row);
    }
}
