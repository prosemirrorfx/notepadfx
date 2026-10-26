package prosemirrorfx.notepadfx.ui;

import prosemirrorfx.notepadfx.doc.Doc;
import prosemirrorfx.notepadfx.state.FoldState;
import java.nio.file.Files;
import java.util.ArrayList;

// remembers which bullets and headings are folded, per file; dropped when the file changed outside notepadfx
final class Folds {
    static void restore(Doc d) {
        var s = Windows.session.folds().get(key(d));
        if (s == null || s.keys().isEmpty()) return;
        if (s.modified() == null || s.modified() != modified(d)) { Windows.session.folds().remove(key(d)); return; }
        d.editor().folded(s.keys());
    }

    static void track(Doc d) {
        d.editor().foldChanges().addListener((o, a, b) -> remember(d));
        d.dirty.addListener((o, a, b) -> { if (!b) remember(d); });
    }

    static void remember(Doc d) {
        if (!d.hasEditor() || d.dirty.get()) return;
        var keys = d.editor().folded();
        if (keys.isEmpty()) Windows.session.folds().remove(key(d));
        else Windows.session.folds().put(key(d), new FoldState().keys(new ArrayList<>(keys)).modified(modified(d)));
    }

    static long modified(Doc d) {
        try { return Files.getLastModifiedTime(d.path).toMillis(); } catch (Exception e) { return -1; }
    }

    static String key(Doc d) { return d.path.toString(); }

    private Folds() {}
}
