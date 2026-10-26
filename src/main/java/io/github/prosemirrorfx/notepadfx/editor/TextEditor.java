package io.github.prosemirrorfx.notepadfx.editor;

import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Region;

public final class TextEditor implements Editor {
    final TextArea area = new TextArea();
    final IntegerProperty caretLine = new SimpleIntegerProperty();

    public TextEditor() {
        area.setWrapText(true);
        area.getStyleClass().add("np-editor");
        area.caretPositionProperty().addListener((o, a, b) -> caretLine.set(lineAt(b.intValue())));
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
    @Override public ObservableValue<String> textChanges() { return area.textProperty(); }
    @Override public ObservableValue<Number> caretLine() { return caretLine; }
    @Override public void fontSize(double size) { area.setStyle("-fx-font-size: " + size + "px;"); }
    @Override public void focus() { area.requestFocus(); }

    @Override
    public void text(String text) {
        area.setText(text);
        area.positionCaret(0);
        area.setScrollTop(0);
    }

    @Override
    public void goToLine(int line) {
        var t = area.getText();
        var pos = 0;
        for (var l = 0; l < line && pos >= 0; l++) pos = t.indexOf('\n', pos) + 1;
        if (pos < 0) pos = t.length();
        area.positionCaret(pos);
        area.selectRange(pos, pos);
        var lines = Math.max(1, t.chars().filter(c -> c == '\n').count() + 1);
        area.setScrollTop(Math.max(0, (double) line / lines * contentHeight() - 40));
        area.requestFocus();
    }

    double contentHeight() {
        var content = area.lookup(".content");
        return content == null ? 0 : content.getBoundsInLocal().getHeight();
    }
}
