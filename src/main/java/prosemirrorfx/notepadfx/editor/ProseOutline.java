package prosemirrorfx.notepadfx.editor;

import com.atlassian.prosemirror.model.Node;
import java.util.*;
import prosemirrorfx.notepadfx.toc.*;

import static prosemirrorfx.core.pm.Pm.intAttr;
import static prosemirrorfx.core.schema.EditorSchema.*;

final class ProseOutline {
    static List<OutlineNode> of(Node doc) {
        var flat = new ArrayList<OutlineNode>();
        walk(doc, 0, 0, flat);
        return Outline.nest(flat);
    }

    static void walk(Node parent, int start, int depth, List<OutlineNode> out) {
        var pos = start;
        for (var i = 0; i < parent.getChildCount(); i++) {
            var n = parent.child(i);
            var inside = pos + 1;
            if (n.getType() == HEADING) out.add(Outline.node("heading", n.getTextContent(), inside).level(intAttr(n, "level")));
            else if (n.getType() == LIST_ITEM) {
                out.add(Outline.node("bullet", firstText(n), inside + 1).depth(depth));
                walk(n, inside, depth, out);
            } else if (n.getType() == BULLET_LIST || n.getType() == ORDERED_LIST) walk(n, inside, depth + 1, out);
            else if (n.getType() == PARAGRAPH && depth == 0) out.add(Outline.node("para", n.getTextContent(), inside));
            else if (!n.isTextblock() && !n.isLeaf() && n.getType() != LIST_ITEM) walk(n, inside, depth, out);
            pos += n.getNodeSize();
        }
    }

    static String firstText(Node item) {
        return item.getChildCount() > 0 && item.child(0).isTextblock() ? item.child(0).getTextContent() : item.getTextContent();
    }

    private ProseOutline() {}
}
