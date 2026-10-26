package io.github.prosemirrorfx.notepadfx.ui;

import io.github.prosemirrorfx.notepadfx.doc.*;
import io.github.prosemirrorfx.notepadfx.state.*;
import java.nio.file.*;
import java.util.*;
import javafx.application.Platform;

public final class Windows {
    static final List<MainWindow> ALL = new ArrayList<>();
    static Session session;
    static MainWindow focused;

    public static void start(Session s, List<String> args) {
        session = s;
        Workspaces.load(s);
        if (s.windows().isEmpty()) newWindow();
        else for (var ws : List.copyOf(s.windows())) open(ws);
        if (!args.isEmpty()) handle(args);
    }

    public static void handle(List<String> args) {
        var snap = args.stream().filter(a -> a.startsWith("--snapshot=")).findFirst();
        if (snap.isPresent()) { Snapshots.write(ALL, snap.get().substring(11)); return; }
        var files = args.stream().filter(a -> !a.startsWith("-")).map(Path::of).toList();
        var fresh = files.isEmpty() || args.contains("--new-window") ? newWindow() : null;
        for (var f : files) { if (fresh != null && !Files.isDirectory(f)) fresh.show(Docs.of(f)); else open(f); }
        target().stage.toFront();
    }

    static MainWindow target() { return focused != null && ALL.contains(focused) ? focused : ALL.getLast(); }

    public static MainWindow newWindow() {
        var ws = new WindowState();
        if (!ALL.isEmpty()) ws.workspace(target().state.workspace()).project(target().state.project());
        return open(ws);
    }

    static MainWindow open(WindowState ws) {
        var w = new MainWindow(ws);
        ALL.add(w);
        w.show();
        return w;
    }

    public static void open(Path p) {
        if (Files.isDirectory(p)) { Workspaces.add(p); return; }
        var doc = Docs.of(p);
        var owner = windowOf(doc);
        (owner != null ? owner : target()).show(doc);
    }

    static MainWindow windowOf(Doc d) {
        return ALL.stream().filter(w -> w.tabs.contains(d)).findFirst().orElse(null);
    }

    static void move(Doc d, MainWindow to, int index) {
        var from = windowOf(d);
        if (from != null) from.tabs.detach(d);
        to.tabs.insert(d, index);
        to.show(d);
        if (from != null && from != to && from.tabs.isEmpty() && ALL.size() > 1) from.close();
    }

    static void tearOff(Doc d, double screenX, double screenY) {
        var from = windowOf(d);
        if (from != null && from.tabs.size() == 1) { from.stage.setX(screenX - 120); from.stage.setY(screenY - 20); return; }
        var ws = new WindowState().x(screenX - 120).y(screenY - 20).width(1000.0).height(720.0);
        if (from != null) ws.workspace(from.state.workspace()).project(from.state.project());
        move(d, open(ws), 0);
    }

    static void closed(MainWindow w) {
        if (ALL.size() == 1) { persist(); Platform.exit(); return; }
        ALL.remove(w);
        persist();
    }

    public static void persist() {
        session.windows(new ArrayList<>(ALL.stream().map(MainWindow::snapshot).toList()));
        Workspaces.save(session);
        Store.save(session);
    }

    public static void toggleTheme() {
        session.theme("dark".equals(session.theme()) ? "light" : "dark");
        ALL.forEach(MainWindow::applyTheme);
        persist();
    }

    static String theme() { return session.theme(); }

    static double fontSize() { return session.fontSize(); }

    private Windows() {}
}
