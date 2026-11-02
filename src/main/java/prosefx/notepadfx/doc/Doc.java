package prosefx.notepadfx.doc;

import java.nio.file.*;
import javafx.beans.property.*;
import prosefx.notepadfx.editor.*;

public final class Doc {
    public final Path path;
    public final BooleanProperty dirty = new SimpleBooleanProperty();
    public final IntegerProperty version = new SimpleIntegerProperty();
    /** Completes when the document is closed (tab or window), for `--wait` callers such as an editor hook. */
    public final java.util.concurrent.CompletableFuture<Void> closed = new java.util.concurrent.CompletableFuture<>();
    final BooleanProperty differsFromFile = new SimpleBooleanProperty();
    Editor editor;
    FileText loaded;
    boolean source;
    boolean shownOnce;

    Doc(Path path) { this.path = path; }

    public String name() { return path.getFileName().toString(); }

    public boolean markdown() {
        var n = name().toLowerCase();
        return n.endsWith(".md") || n.endsWith(".markdown");
    }

    public boolean source() { return source; }

    /** True the first time it is asked, for the set-up an editor gets when its file is first shown (the status bar may have created the editor by then, so "has no editor yet" cannot tell). */
    public boolean firstShow() { var first = !shownOnce; shownOnce = true; return first; }

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
        editor.baseDirectory(path.getParent());
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
        old.close();
        source = !source;
        attach(source ? new TextEditor() : new ProseEditor(), text);
        CaretMap.restore(editor, at);
        version.set(version.get() + 1);
        return true;
    }

    FileText file() {
        if (loaded == null) { loaded = FileText.read(path); stamp = stampOf(path); }
        return loaded;
    }

    public void save() throws Exception {
        var t = text();
        Files.write(path, file().encode(t));
        stamp = stampOf(path);
        loaded.text(t);
        differsFromFile.set(false);
        if (editor != null) editor.markSaved();
    }

    // ---- the file changed under us (another editor, a script, another tool)

    /** What was found when the file on disk was compared with what this document last read or wrote. */
    public enum Disk { SAME, RELOADED, CONFLICT }

    long stamp = -1;
    FileText pending;

    static long stampOf(Path p) {
        try { return Files.getLastModifiedTime(p).toMillis() * 31 + Files.size(p); } catch (java.io.IOException e) { return -1; }
    }

    /**
     * Compares the file with what was last read or written. Unchanged: SAME. Changed and nothing unsaved here: the new
     * text is loaded with the caret kept (RELOADED). Changed while there are unsaved edits: CONFLICT, and the disk text
     * waits for takeDisk() or keepMine().
     */
    public Disk syncFromDisk() {
        var s = stampOf(path);
        if (loaded == null || s == stamp || s == -1) return Disk.SAME;
        var disk = FileText.read(path);
        stamp = s;
        if (disk.text().equals(loaded.text())) { loaded = disk; return Disk.SAME; }
        if (editor != null && dirty.get()) { pending = disk; return Disk.CONFLICT; }
        loaded = disk;
        replaceKeepingPlace();
        return Disk.RELOADED;
    }

    /** True while the file on disk holds text this document has not taken (see CONFLICT). */
    public boolean conflict() { return pending != null; }

    /** Resolves a CONFLICT by dropping the unsaved edits for the text on disk. */
    public void takeDisk() {
        loaded = pending != null ? pending : FileText.read(path);
        pending = null;
        stamp = stampOf(path);
        replaceKeepingPlace();
    }

    /** Resolves a CONFLICT the other way: the edits stay, the document is still unsaved, and saving overwrites the disk text. */
    public void keepMine() {
        if (pending != null) loaded = pending;
        pending = null;
        differsFromFile.set(editor != null && !editor.text().equals(loaded.text()));
    }

    void replaceKeepingPlace() {
        differsFromFile.set(false);
        if (editor != null) {
            var at = editor.position();
            editor.load(loaded.text());
            editor.position(at);
        }
        version.set(version.get() + 1);
    }

    public void reload() {
        loaded = FileText.read(path);
        stamp = stampOf(path);
        differsFromFile.set(false);
        if (editor != null) editor.load(loaded.text());
        version.set(version.get() + 1);
    }
}
