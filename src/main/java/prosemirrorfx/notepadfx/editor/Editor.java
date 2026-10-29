package prosemirrorfx.notepadfx.editor;

import java.util.*;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import prosemirrorfx.notepadfx.toc.OutlineNode;

public interface Editor {
    enum Copy { FORMATTED, HTML, MARKDOWN, PLAIN }

    Node node();

    Node toolbar();

    String text();

    void load(String text);

    void markSaved();

    ReadOnlyBooleanProperty dirty();

    void onChange(Runnable listener);

    List<OutlineNode> outline();

    ObservableValue<Number> caret();

    int position();

    void position(int pos);

    void goTo(int anchor);

    String where();

    void cut();

    void copy();

    void paste(boolean plain);

    void copyAll(Copy format);

    void zoom(double zoom);

    ObservableValue<Number> zoomChanges();

    boolean lineNumbersSupported();

    void lineNumbers(boolean on);

    Set<String> folded();

    void folded(Collection<String> keys);

    ObservableValue<?> foldChanges();

    void collapseAll();

    void expandAll();

    void theme(boolean dark);

    void focus();

    /** Dictation, read aloud and their stop; false where the editor has none (plain text). */
    default boolean dictate() { return false; }

    default boolean read() { return false; }

    default boolean stopSpeech() { return false; }

    default void close() {}
}
