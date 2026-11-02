package prosefx.notepadfx.editor;

import java.util.*;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import prosefx.notepadfx.toc.OutlineNode;

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

    /** Spelling and grammar: on at the first call, then the next issue. False when this editor has none. */
    default boolean nextIssue() { return false; }

    default boolean stopSpeech() { return false; }

    /** Next (direction > 0) or previous comment (a paragraph or line starting `>>`), wrapping around; false when there is none. */
    default boolean comment(int direction) { return false; }

    /** The folder that relative pictures in the text are read against. Called before load. */
    default void baseDirectory(java.nio.file.Path dir) {}

    /** What to do with an address followed in the text (Ctrl+click, Ctrl+Enter). Called once when the editor is first shown. */
    default void links(java.util.function.Consumer<String> handler) {}

    default void close() {}
}
