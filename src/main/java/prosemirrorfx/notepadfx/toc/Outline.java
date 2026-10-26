package prosemirrorfx.notepadfx.toc;

import java.util.*;
import java.util.regex.Pattern;

public final class Outline {
    static final Pattern
        HEADING = Pattern.compile("^[ \\t]*(#{1,6})\\s+(.*)$"),
        BULLET = Pattern.compile("^([ \\t]*)(?:[-*+]|\\d+[.)])\\s+(.*)$"),
        FENCE = Pattern.compile("^\\s*(```|~~~).*"),
        TABLE = Pattern.compile("^\\s*\\|.*"),
        RULE = Pattern.compile("^\\s*([-*_])(\\s*\\1){2,}\\s*$"),
        NOISE = Pattern.compile("\\*\\*|__|`");
    static final int MAX_LABEL = 400;

    public static List<OutlineNode> parse(String text) {
        var flat = scan(text);
        var unit = Math.min(4, flat.stream().filter(n -> n.depth() != null && n.depth() > 0).mapToInt(OutlineNode::depth).min().orElse(4));
        for (var n : flat) if ("bullet".equals(n.type())) n.depth(1 + Math.round((float) n.depth() / unit));
        return nest(flat);
    }

    public static List<OutlineNode> nest(List<OutlineNode> flat) {
        var roots = new ArrayList<OutlineNode>();
        var stack = new ArrayDeque<OutlineNode>();
        var base = 0;
        for (var n : flat) {
            n.label(tidy(n.label()));
            int rank;
            switch (n.type()) {
                case "heading" -> { rank = n.level(); base = n.level(); }
                case "bullet" -> rank = base + n.depth();
                default -> rank = base + 1;
            }
            n.rank(rank);
            while (!stack.isEmpty() && closes(stack.peek(), n, rank)) stack.pop();
            (stack.isEmpty() ? roots : stack.peek().children()).add(n);
            stack.push(n);
        }
        return roots;
    }

    static boolean closes(OutlineNode top, OutlineNode n, int rank) {
        return n.type().equals("heading")
            ? !top.type().equals("heading") || top.level() >= n.level()
            : top.rank() >= rank;
    }

    static List<OutlineNode> scan(String text) {
        var items = new ArrayList<OutlineNode>();
        var inFence = false;
        var boundary = true;
        OutlineNode para = null;
        var lines = text.split("\\r?\\n", -1);
        for (var i = 0; i < lines.length; i++) {
            var l = lines[i];
            if (FENCE.matcher(l).matches()) { inFence = !inFence; para = null; boundary = false; continue; }
            if (inFence) continue;
            if (l.isBlank() || RULE.matcher(l).matches()) { boundary = true; para = null; continue; }
            var h = HEADING.matcher(l);
            if (h.matches()) { items.add(node("heading", h.group(2), i).level(h.group(1).length())); para = null; boundary = true; continue; }
            var b = BULLET.matcher(l);
            if (b.matches() && !b.group(2).isBlank()) { items.add(node("bullet", b.group(2), i).depth(columns(b.group(1)))); para = null; boundary = false; continue; }
            if (para != null) { para.label(para.label() + " " + l.trim()); continue; }
            if (boundary && !l.startsWith(" ") && !l.startsWith("\t") && !TABLE.matcher(l).matches()) {
                para = node("para", l.trim(), i);
                items.add(para);
            }
            boundary = false;
        }
        return items;
    }

    public static OutlineNode node(String type, String label, int anchor) {
        return new OutlineNode().type(type).label(label).line(anchor);
    }

    static int columns(String indent) {
        var c = 0;
        for (var ch : indent.toCharArray()) c += ch == '\t' ? 4 : 1;
        return c;
    }

    static String tidy(String s) {
        var t = NOISE.matcher(s == null ? "" : s).replaceAll("").replaceAll("\\s+", " ").trim();
        t = t.length() > MAX_LABEL ? t.substring(0, MAX_LABEL) : t;
        return t.isEmpty() ? "(empty)" : t;
    }

    private Outline() {}
}
