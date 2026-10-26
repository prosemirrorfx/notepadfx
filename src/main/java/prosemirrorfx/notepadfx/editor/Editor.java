package prosemirrorfx.notepadfx.editor;

import java.util.List;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import prosemirrorfx.notepadfx.toc.OutlineNode;

public interface Editor {
    Node node();

    String text();

    void load(String text);

    void markSaved();

    ReadOnlyBooleanProperty dirty();

    void onChange(Runnable listener);

    List<OutlineNode> outline();

    ObservableValue<Number> caret();

    void goTo(int anchor);

    String where();

    void fontSize(double size);

    void theme(boolean dark);

    void focus();
}
