package io.github.prosemirrorfx.notepadfx.toc;

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

    record Item(String type, int level, int cols, StringBuilder text, int line) {}

    public static List<OutlineNode> parse(String text) {
        var items = scan(text);
        var unit = items.stream().filter(i -> i.cols > 0).mapToInt(Item::cols).min().orElse(4);
        unit = Math.min(4, unit);
        var roots = new ArrayList<OutlineNode>();
        var stack = new ArrayDeque<OutlineNode>();
        var base = 0;
        for (var r : items) {
            var n = new OutlineNode().type(r.type).label(tidy(r.text.toString())).line(r.line);
            int rank;
            switch (r.type) {
                case "heading" -> { n.level(r.level); rank = r.level; base = r.level; }
                case "bullet" -> { n.depth(1 + Math.round((float) r.cols / unit)); rank = base + n.depth(); }
                default -> rank = base + 1;
            }
            n.rank(rank);
            while (!stack.isEmpty() && closes(stack.peek(), r, rank)) stack.pop();
            (stack.isEmpty() ? roots : stack.peek().children()).add(n);
            stack.push(n);
        }
        return roots;
    }

    static boolean closes(OutlineNode top, Item r, int rank) {
        return r.type.equals("heading")
            ? !top.type().equals("heading") || top.level() >= r.level
            : top.rank() >= rank;
    }

    static List<Item> scan(String text) {
        var items = new ArrayList<Item>();
        var inFence = false;
        var boundary = true;
        Item para = null;
        var lines = text.split("\\r?\\n", -1);
        for (var i = 0; i < lines.length; i++) {
            var l = lines[i];
            if (FENCE.matcher(l).matches()) { inFence = !inFence; para = null; boundary = false; continue; }
            if (inFence) continue;
            if (l.isBlank() || RULE.matcher(l).matches()) { boundary = true; para = null; continue; }
            var h = HEADING.matcher(l);
            if (h.matches()) { items.add(new Item("heading", h.group(1).length(), 0, new StringBuilder(h.group(2)), i)); para = null; boundary = true; continue; }
            var b = BULLET.matcher(l);
            if (b.matches() && !b.group(2).isBlank()) { items.add(new Item("bullet", 0, columns(b.group(1)), new StringBuilder(b.group(2)), i)); para = null; boundary = false; continue; }
            if (para != null) { para.text.append(' ').append(l.trim()); continue; }
            if (boundary && !l.startsWith(" ") && !l.startsWith("\t") && !TABLE.matcher(l).matches()) {
                para = new Item("para", 0, 0, new StringBuilder(l.trim()), i);
                items.add(para);
            }
            boundary = false;
        }
        return items;
    }

    static int columns(String indent) {
        var c = 0;
        for (var ch : indent.toCharArray()) c += ch == '\t' ? 4 : 1;
        return c;
    }

    static String tidy(String s) {
        var t = NOISE.matcher(s).replaceAll("").replaceAll("\\s+", " ").trim();
        t = t.length() > MAX_LABEL ? t.substring(0, MAX_LABEL) : t;
        return t.isEmpty() ? "(empty)" : t;
    }

    private Outline() {}
}
