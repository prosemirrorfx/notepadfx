package io.github.prosemirrorfx.notepadfx.editor;

import javafx.beans.value.ObservableValue;
import javafx.scene.Node;

public interface Editor {
    Node node();

    String text();

    void text(String text);

    ObservableValue<String> textChanges();

    ObservableValue<Number> caretLine();

    void goToLine(int line);

    void fontSize(double size);

    void focus();
}
