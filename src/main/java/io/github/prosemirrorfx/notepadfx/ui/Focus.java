package io.github.prosemirrorfx.notepadfx.ui;

import java.util.*;
import javafx.scene.Node;

final class Focus {
    static void next(MainWindow w) {
        var stops = new ArrayList<Runnable>();
        var owners = new ArrayList<Node>();
        if (w.state.navVisible()) {
            stops.add(() -> w.nav.workspaces.tree.requestFocus()); owners.add(w.nav.workspaces.node);
            stops.add(() -> w.nav.projects.filter.requestFocus()); owners.add(w.nav.projects.node);
            stops.add(() -> w.nav.files.filter.requestFocus()); owners.add(w.nav.files.node);
        }
        stops.add(w.tabs::focus); owners.add(w.tabs.node);
        stops.add(() -> { if (w.active.get() != null) w.active.get().editor().focus(); }); owners.add(w.editorHost);
        if (w.state.tocVisible()) { stops.add(w.toc::focus); owners.add(w.toc.node); }
        var focused = w.stage.getScene().getFocusOwner();
        var at = -1;
        for (var i = 0; i < owners.size(); i++) if (inside(focused, owners.get(i))) at = i;
        stops.get((at + 1) % stops.size()).run();
    }

    static boolean inside(Node n, Node owner) {
        for (var p = n; p != null; p = p.getParent()) if (p == owner) return true;
        return false;
    }

    private Focus() {}
}
