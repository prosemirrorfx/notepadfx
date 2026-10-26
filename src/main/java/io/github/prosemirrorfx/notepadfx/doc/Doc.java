package io.github.prosemirrorfx.notepadfx.doc;

import io.github.prosemirrorfx.notepadfx.editor.*;
import java.nio.file.*;
import javafx.beans.property.*;

public final class Doc {
    public final Path path;
    public final BooleanProperty dirty = new SimpleBooleanProperty();
    final StringProperty text = new SimpleStringProperty();
    Editor editor;
    FileText loaded;

    Doc(Path path) { this.path = path; }

    public String name() { return path.getFileName().toString(); }

    public boolean markdown() {
        var n = name().toLowerCase();
        return n.endsWith(".md") || n.endsWith(".markdown");
    }

    public String text() {
        if (editor != null) return editor.text();
        return file().text();
    }

    public ReadOnlyStringProperty textProperty() {
        if (text.get() == null) text.set(text());
        return text;
    }

    public Editor editor() {
        if (editor == null) {
            editor = new TextEditor();
            editor.text(file().text());
            editor.textChanges().addListener((o, a, b) -> {
                dirty.set(!b.equals(loaded.text()));
                text.set(b);
            });
        }
        return editor;
    }

    FileText file() {
        if (loaded == null) loaded = FileText.read(path);
        return loaded;
    }

    public void save() throws Exception {
        var t = text();
        Files.write(path, file().encode(t));
        loaded.text(t);
        dirty.set(false);
    }

    public void reload() {
        loaded = FileText.read(path);
        if (editor != null) editor.text(loaded.text());
        dirty.set(false);
    }

    public boolean hasEditor() { return editor != null; }
}
