package prosemirrorfx.notepadfx.ui;

import prosemirrorfx.notepadfx.doc.Doc;
import java.nio.file.Path;
import java.util.List;
import java.util.function.BiFunction;

final class Titles {
    interface Handler extends BiFunction<Path, String, String> {}

    static final List<Handler> HANDLERS = List.of(Titles::prp, (p, project) -> p.getFileName().toString());

    static String of(MainWindow w, Doc d) {
        var project = project(w, d.path);
        var name = HANDLERS.stream().map(h -> h.apply(d.path, project)).filter(t -> t != null).findFirst().orElse(d.name());
        return "notepadfx - " + (project.isEmpty() ? "" : project + " - ") + name + (d.dirty.get() ? " ●" : "") + " (" + d.path + ")";
    }

    static String project(MainWindow w, Path file) {
        var ws = w.state.workspace();
        if (ws != null) {
            var root = Path.of(ws);
            if (file.startsWith(root) && root.relativize(file).getNameCount() > 1) return root.relativize(file).getName(0).toString();
        }
        var dir = file.getParent();
        if (dir != null && dir.getFileName() != null && dir.getFileName().toString().equals("prp") && dir.getParent() != null) dir = dir.getParent();
        return dir == null || dir.getFileName() == null ? "" : dir.getFileName().toString();
    }

    static String prp(Path p, String project) {
        var m = TabLabel.PRP.matcher(p.getFileName().toString());
        if (!m.matches()) return null;
        var rest = m.group(3).replace('_', ' ');
        var closed = m.group(4) != null ? " (closed)" : "";
        return "PRP " + m.group(1) + " " + rest + closed;
    }

    private Titles() {}
}
