package prosemirrorfx.notepadfx.doc;

import java.nio.file.*;
import javafx.beans.property.*;
import prosemirrorfx.notepadfx.editor.*;

public final class Doc {
    public final Path path;
    public final BooleanProperty dirty = new SimpleBooleanProperty();
    public final IntegerProperty version = new SimpleIntegerProperty();
    final BooleanProperty differsFromFile = new SimpleBooleanProperty();
    Editor editor;
    FileText loaded;
    boolean source;

    Doc(Path path) { this.path = path; }

    public String name() { return path.getFileName().toString(); }

    public boolean markdown() {
        var n = name().toLowerCase();
        return n.endsWith(".md") || n.endsWith(".markdown");
    }

    public boolean source() { return source; }

    public String text() { return editor != null ? editor.text() : file().text(); }

    public boolean hasEditor() { return editor != null; }

    public String encoding() {
        var f = file();
        return "UTF-8" + (Boolean.TRUE.equals(f.bom()) ? " BOM" : "") + (Boolean.TRUE.equals(f.crlf()) ? "  CRLF" : "  LF");
    }

    public Editor editor() {
        if (editor == null) attach(markdown() && !source ? new ProseEditor() : new TextEditor(), file().text());
        return editor;
    }

    void attach(Editor e, String text) {
        editor = e;
        editor.load(text);
        differsFromFile.set(!text.equals(file().text()));
        dirty.unbind();
        dirty.bind(editor.dirty().or(differsFromFile));
        editor.onChange(() -> version.set(version.get() + 1));
    }

    public boolean toggleSource() {
        if (!markdown()) return false;
        var old = editor();
        var text = old.text();
        var at = CaretMap.index(old.outline(), old.caret().getValue().intValue());
        source = !source;
        attach(source ? new TextEditor() : new ProseEditor(), text);
        CaretMap.restore(editor, at);
        version.set(version.get() + 1);
        return true;
    }

    FileText file() {
        if (loaded == null) loaded = FileText.read(path);
        return loaded;
    }

    public void save() throws Exception {
        var t = text();
        Files.write(path, file().encode(t));
        loaded.text(t);
        differsFromFile.set(false);
        if (editor != null) editor.markSaved();
    }

    public void reload() {
        loaded = FileText.read(path);
        differsFromFile.set(false);
        if (editor != null) editor.load(loaded.text());
        version.set(version.get() + 1);
    }
}
