package prosemirrorfx.notepadfx.doc;

import java.nio.file.*;
import javafx.beans.property.*;
import prosemirrorfx.notepadfx.editor.*;

public final class Doc {
    public final Path path;
    public final BooleanProperty dirty = new SimpleBooleanProperty();
    public final IntegerProperty version = new SimpleIntegerProperty();
    Editor editor;
    FileText loaded;

    Doc(Path path) { this.path = path; }

    public String name() { return path.getFileName().toString(); }

    public boolean markdown() {
        var n = name().toLowerCase();
        return n.endsWith(".md") || n.endsWith(".markdown");
    }

    public String text() { return editor != null ? editor.text() : file().text(); }

    public boolean hasEditor() { return editor != null; }

    public Editor editor() {
        if (editor == null) {
            editor = markdown() ? new ProseEditor() : new TextEditor();
            editor.load(file().text());
            dirty.bind(editor.dirty());
            editor.onChange(() -> version.set(version.get() + 1));
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
        if (editor != null) editor.markSaved();
    }

    public void reload() {
        loaded = FileText.read(path);
        if (editor != null) editor.load(loaded.text());
        version.set(version.get() + 1);
    }
}
