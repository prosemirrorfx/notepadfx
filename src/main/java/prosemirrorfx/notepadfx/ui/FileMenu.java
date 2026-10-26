package prosemirrorfx.notepadfx.ui;

import prosemirrorfx.notepadfx.sys.Shell;
import java.nio.file.*;
import java.util.function.Supplier;
import javafx.scene.control.*;

final class FileMenu {
    static ContextMenu forWorkspaces(MainWindow w, WorkspacePane pane) {
        Supplier<Path> sel = () -> value(pane.tree.getSelectionModel().getSelectedItem());
        var remove = item("Remove from workspaces", () -> {
            var p = sel.get();
            var ws = p == null ? null : Workspaces.of(p.toString());
            if (ws != null) Workspaces.remove(ws);
        });
        var m = common(w, sel, pane::reload);
        m.getItems().addAll(new SeparatorMenuItem(), item("Add workspace folder", pane::add), remove);
        return m;
    }

    static ContextMenu forList(MainWindow w, ListView<Path> list, Runnable refresh) {
        return common(w, () -> list.getSelectionModel().getSelectedItem(), refresh);
    }

    static ContextMenu forTree(MainWindow w, TreeView<Path> tree, Runnable refresh) {
        var m = common(w, () -> value(tree.getSelectionModel().getSelectedItem()), refresh);
        m.getItems().addFirst(item("Open", () -> {
            var p = value(tree.getSelectionModel().getSelectedItem());
            if (p != null && Files.isRegularFile(p)) Windows.open(p);
        }));
        return m;
    }

    static ContextMenu common(MainWindow w, Supplier<Path> sel, Runnable refresh) {
        return new ContextMenu(
            item("New file here", () -> create(w, sel.get(), false, refresh)),
            item("New folder here", () -> create(w, sel.get(), true, refresh)),
            item("Rename", () -> rename(w, sel.get(), refresh)),
            item("Move to recycle bin", () -> trash(w, sel.get(), refresh)),
            new SeparatorMenuItem(),
            item("Reveal in Explorer", () -> Shell.reveal(sel.get())),
            item("Terminal here", () -> Shell.terminal(sel.get())),
            item("Copy path", () -> Shell.copy(sel.get())),
            new SeparatorMenuItem(),
            item("Refresh", refresh));
    }

    static void create(MainWindow w, Path at, boolean folder, Runnable refresh) {
        if (at == null) return;
        var dir = Files.isDirectory(at) ? at : at.getParent();
        var name = Prompts.ask(w, folder ? "New folder" : "New file", folder ? "Folder name" : "File name", folder ? "" : "untitled.md");
        if (name == null || name.isBlank()) return;
        try {
            var p = dir.resolve(name.trim());
            if (folder) Files.createDirectories(p); else Files.createFile(p);
            refresh.run();
            if (!folder) Windows.open(p);
        } catch (Exception e) { Prompts.error(w, "Could not create " + name, e); }
    }

    static void rename(MainWindow w, Path p, Runnable refresh) {
        if (p == null) return;
        var name = Prompts.ask(w, "Rename", "New name", p.getFileName().toString());
        if (name == null || name.isBlank() || name.equals(p.getFileName().toString())) return;
        try { Files.move(p, p.resolveSibling(name.trim())); refresh.run(); }
        catch (Exception e) { Prompts.error(w, "Could not rename " + p.getFileName(), e); }
    }

    static void trash(MainWindow w, Path p, Runnable refresh) {
        if (p == null || !Prompts.yes(w, "Move to recycle bin", p.toString())) return;
        if (Shell.trash(p)) refresh.run(); else Prompts.error(w, "Could not move " + p.getFileName() + " to the recycle bin", null);
    }

    static MenuItem item(String text, Runnable run) {
        var i = new MenuItem(text);
        i.setMnemonicParsing(false);
        i.setOnAction(e -> run.run());
        return i;
    }

    static Path value(TreeItem<Path> i) { return i == null ? null : i.getValue(); }

    private FileMenu() {}
}
