package prosemirrorfx.notepadfx.editor;

import java.util.List;
import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Region;
import prosemirrorfx.notepadfx.toc.*;

public final class TextEditor implements Editor {
    final TextArea area = new TextArea();
    final IntegerProperty caretLine = new SimpleIntegerProperty();
    final ReadOnlyBooleanWrapper dirty = new ReadOnlyBooleanWrapper();
    String saved = "";
    boolean loading;

    public TextEditor() {
        area.setWrapText(true);
        area.getStyleClass().add("np-editor");
        area.caretPositionProperty().addListener((o, a, b) -> caretLine.set(lineAt(b.intValue())));
        area.textProperty().addListener((o, a, b) -> { if (!loading) dirty.set(!b.equals(saved)); });
        area.widthProperty().subscribe(w -> readableWidth(w.doubleValue()));
        area.skinProperty().subscribe(s -> readableWidth(area.getWidth()));
    }

    void readableWidth(double w) {
        if (area.lookup(".content") instanceof Region r) {
            var side = Math.max(28, (w - 880) / 2);
            r.setPadding(new Insets(28, side, 160, side));
        }
    }

    int lineAt(int pos) {
        var t = area.getText();
        var line = 0;
        for (var i = 0; i < pos && i < t.length(); i++) if (t.charAt(i) == '\n') line++;
        return line;
    }

    @Override public Node node() { return area; }
    @Override public String text() { return area.getText(); }
    @Override public ReadOnlyBooleanProperty dirty() { return dirty.getReadOnlyProperty(); }
    @Override public void onChange(Runnable r) { area.textProperty().addListener((o, a, b) -> { if (!loading) r.run(); }); }
    @Override public List<OutlineNode> outline() { return Outline.parse(text()); }
    @Override public ObservableValue<Number> caret() { return caretLine; }
    @Override public String where() { return "Ln " + (caretLine.get() + 1); }
    @Override public void fontSize(double size) { area.setStyle("-fx-font-size: " + size + "px;"); }
    @Override public void focus() { area.requestFocus(); }
    @Override public void theme(boolean dark) {}

    @Override
    public void load(String text) {
        loading = true;
        try { area.setText(text); } finally { loading = false; }
        markSaved();
        area.positionCaret(0);
        area.setScrollTop(0);
    }

    @Override
    public void markSaved() {
        saved = area.getText();
        dirty.set(false);
    }

    @Override
    public void goTo(int line) {
        var t = area.getText();
        var pos = 0;
        for (var l = 0; l < line && pos >= 0; l++) pos = t.indexOf('\n', pos) + 1;
        if (pos < 0) pos = t.length();
        area.positionCaret(pos);
        area.selectRange(pos, pos);
        var lines = Math.max(1, t.chars().filter(c -> c == '\n').count() + 1);
        var content = area.lookup(".content");
        var height = content == null ? 0 : content.getBoundsInLocal().getHeight();
        area.setScrollTop(Math.max(0, (double) line / lines * height - 40));
        area.requestFocus();
    }
}
