package io.github.prosemirrorfx.notepadfx.ui;

import io.github.prosemirrorfx.notepadfx.state.*;
import java.nio.file.*;
import java.util.ArrayList;
import javafx.collections.*;

public final class Workspaces {
    static final ObservableList<WorkspaceState> LIST = FXCollections.observableArrayList();

    static void load(Session s) {
        LIST.setAll(s.workspaces().stream().filter(w -> w.path() != null && Files.isDirectory(Path.of(w.path()))).toList());
    }

    static void save(Session s) { s.workspaces(new ArrayList<>(LIST)); }

    public static void add(Path dir) {
        var p = dir.toAbsolutePath().normalize().toString();
        if (LIST.stream().noneMatch(w -> w.path().equalsIgnoreCase(p))) LIST.add(new WorkspaceState().path(p));
        Windows.persist();
    }

    static void remove(WorkspaceState w) {
        LIST.remove(w);
        Windows.persist();
    }

    static WorkspaceState of(String path) {
        return path == null ? null : LIST.stream().filter(w -> w.path().equalsIgnoreCase(path)).findFirst().orElse(null);
    }

    private Workspaces() {}
}
