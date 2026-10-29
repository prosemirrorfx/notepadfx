package prosemirrorfx.notepadfx.toc;

import java.util.*;
import java.util.regex.*;

/**
 * What the outline shows: kinds switched on (h1..h6, para, b1, b2, b3 where b3 means depth 3 and deeper) and an
 * optional regex over the labels. A hidden entry's children move up to its parent, so hiding H2 keeps its H3s.
 * With a regex, an entry stays if it matches or something under it does (that one shown as context).
 */
public final class OutlineFilter {

    public static final List<String> KINDS = List.of("h1", "h2", "h3", "h4", "h5", "h6", "para", "b1", "b2", "b3");

    final Set<String> show;
    final Pattern regex;

    public OutlineFilter(Collection<String> show, Pattern regex) {
        this.show = Set.copyOf(show);
        this.regex = regex;
    }

    /** null when blank; throws PatternSyntaxException when the text is not a regex. Case-insensitive. */
    public static Pattern compile(String text) {
        return text == null || text.isBlank() ? null : Pattern.compile(text, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    public static String kind(OutlineNode n) {
        return switch (n.type()) {
            case "heading" -> "h" + n.level();
            case "bullet" -> "b" + Math.min(3, Math.max(1, n.depth() == null ? 1 : n.depth()));
            default -> "para";
        };
    }

    public boolean shown(OutlineNode n) { return show.contains(kind(n)); }

    public boolean matches(OutlineNode n) { return regex == null || regex.matcher(n.label()).find(); }

    /** Entries to show under one parent: each with the children it keeps, in document order. */
    public <T> List<T> apply(List<OutlineNode> nodes, Builder<T> build) {
        var out = new ArrayList<T>();
        for (var n : nodes) {
            var kids = apply(n.children(), build);
            if (shown(n) && (matches(n) || !kids.isEmpty())) out.add(build.make(n, kids, matches(n)));
            else out.addAll(kids);
        }
        return out;
    }

    public interface Builder<T> { T make(OutlineNode n, List<T> children, boolean match); }
}
