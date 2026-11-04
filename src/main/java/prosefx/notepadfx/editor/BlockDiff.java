package prosefx.notepadfx.editor;

import com.atlassian.prosemirror.history.HistoryKt;
import com.atlassian.prosemirror.model.*;
import com.atlassian.prosemirror.state.PMEditorState;
import java.util.*;
import prosefx.core.pm.Pm;
import prosefx.markdown.*;
import prosefx.notepadfx.agent.Lines;
import prosefx.view.embed.MarkdownEditor;

/**
 * Makes the formatted editor hold new Markdown by replacing only the top-level blocks that differ, in one transaction.
 * Blocks before and after stay the very same nodes, so the caret and selection map through untouched, a person's
 * unsaved typing in them survives, and one undo brings the old blocks back. If the text written back would still differ
 * (a blank line between blocks is part of the block before it), the range grows by a block each side, at worst to the whole document.
 * Stand-in until prosefx-view offers a replace-a-source-range call (requirements inbox, PRP 19).
 */
final class BlockDiff {

    static void apply(MarkdownEditor md, String text) {
        var model = md.view().model();
        var state = model.state();
        var a = state.getDoc().getContent();
        var b = Lossless.read(text).getContent();
        var start = 0;
        while (start < a.getChildCount() && start < b.getChildCount() && same(a.child(start), b.child(start))) start++;
        var endA = a.getChildCount();
        var endB = b.getChildCount();
        while (endA > start && endB > start && same(a.child(endA - 1), b.child(endB - 1))) { endA--; endB--; }
        if (start == endA && start == endB) return;
        for (var margin : new int[]{0, 1, 2, Integer.MAX_VALUE}) {
            var s = (int) Math.max(0, (long) start - margin);
            var ea = (int) Math.min(a.getChildCount(), (long) endA + margin);
            var tr = state.getTr();
            tr.replace(from(a, s), from(a, ea), Pm.slice(blocks(b, s, endB + (ea - endA)), 0, 0));
            if (Lines.chomp(Lossless.write(tr.getDoc())).equals(Lines.chomp(text)) || margin == Integer.MAX_VALUE) {
                HistoryKt.closeHistory(tr);
                model.dispatch(tr);
                return;
            }
        }
    }

    static boolean same(Node x, Node y) {
        return Sig.of(x, true).equals(Sig.of(y, true)) && Objects.equals(x.getAttrs().get("gap"), y.getAttrs().get("gap"));
    }

    static int from(Fragment f, int index) {
        var at = 0;
        for (var i = 0; i < index; i++) at += f.child(i).getNodeSize();
        return at;
    }

    static Fragment blocks(Fragment f, int from, int to) {
        var list = new ArrayList<Node>();
        for (var i = from; i < to; i++) list.add(f.child(i));
        return Pm.frag(list.toArray(Node[]::new));
    }

    private BlockDiff() {}
}
