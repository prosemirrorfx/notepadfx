package prosemirrorfx.notepadfx.doc;

import java.nio.file.Path;
import java.util.*;

public final class Docs {
    static final Map<Path, Doc> OPEN = new LinkedHashMap<>();

    public static Doc of(Path p) {
        return OPEN.computeIfAbsent(p.toAbsolutePath().normalize(), Doc::new);
    }

    public static Optional<Doc> find(Path p) {
        return Optional.ofNullable(OPEN.get(p.toAbsolutePath().normalize()));
    }

    public static void close(Doc d) {
        OPEN.remove(d.path);
        if (d.hasEditor()) d.editor().close();
    }

    private Docs() {}
}
