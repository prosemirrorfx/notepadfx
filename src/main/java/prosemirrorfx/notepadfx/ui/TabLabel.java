package prosemirrorfx.notepadfx.ui;

import java.nio.file.Path;
import java.util.regex.Pattern;

record TabLabel(String chip, String name) {
    static final Pattern PRP = Pattern.compile("^(\\d{2,3})-prp([.-])(.+?)(\\.closed)?\\.(md|txt)$");

    static TabLabel of(Path p) {
        var file = p.getFileName().toString();
        var m = PRP.matcher(file);
        if (!m.matches()) return new TabLabel(null, file);
        var dir = p.getParent();
        var project = dir != null && dir.getFileName().toString().equals("prp") && dir.getParent() != null
            ? dir.getParent().getFileName().toString() : "";
        var rest = m.group(3).replace('_', ' ');
        var closed = m.group(4) != null ? " (closed)" : "";
        var chip = (project.isEmpty() ? "" : shorten(project) + " ") + m.group(1);
        return new TabLabel(chip, (m.group(2).equals(".") ? "· " : "") + rest + closed);
    }

    static String shorten(String s) {
        var t = s.replaceAll("^\\d{4}-\\d{2}-\\d{2}\\s*", "");
        return t.length() > 14 ? t.substring(0, 14) : t;
    }
}
