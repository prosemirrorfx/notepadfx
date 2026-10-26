package prosemirrorfx.notepadfx.doc;

import java.util.*;
import javafx.application.Platform;
import prosemirrorfx.notepadfx.editor.Editor;
import prosemirrorfx.notepadfx.toc.OutlineNode;

// maps the caret between the WYSIWYG and the source view by outline entry, until the view offers an exact map (inbox R11)
final class CaretMap {
    static int index(List<OutlineNode> outline, int caret) {
        var flat = flat(outline);
        var at = -1;
        for (var i = 0; i < flat.size(); i++) if (flat.get(i).line() <= caret) at = i;
        return at;
    }

    static void restore(Editor e, int index) {
        if (index < 0) return;
        var flat = flat(e.outline());
        if (flat.isEmpty()) return;
        var anchor = flat.get(Math.min(index, flat.size() - 1)).line();
        Platform.runLater(() -> Platform.runLater(() -> e.goTo(anchor)));
    }

    static List<OutlineNode> flat(List<OutlineNode> nodes) {
        var out = new ArrayList<OutlineNode>();
        for (var n : nodes) { out.add(n); out.addAll(flat(n.children())); }
        return out;
    }

    private CaretMap() {}
}
