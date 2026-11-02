package prosefx.notepadfx.ui;

import prosefx.notepadfx.doc.Doc;
import prosefx.notepadfx.editor.Editor;
import prosefx.notepadfx.sys.*;
import java.util.*;
import javafx.scene.control.*;
import javafx.scene.input.*;

import static prosefx.notepadfx.ui.FileMenu.item;

/** The menu of an open file's tab (right click): everything about that file in one place, so it needs no hunting in the panes. */
final class TabMenu {

    static ContextMenu of(MainWindow w, Doc d) {
        var docs = w.tabs.docs();
        var at = docs.indexOf(d);
        var m = new ContextMenu(
            item("Save", () -> Prompts.save(w, d)),
            item("Reload from disk", () -> reload(w, d)),
            item("Close", () -> close(w, List.of(d))),
            item("Close the others", () -> close(w, docs.stream().filter(x -> x != d).toList())),
            item("Close the tabs below", () -> close(w, List.copyOf(docs.subList(at + 1, docs.size())))),
            new SeparatorMenuItem(),
            copyAll(w, d),
            item("Paste into this file", () -> { w.show(d); d.editor().paste(false); }),
            new SeparatorMenuItem(),
            item("Copy path", () -> Shell.copy(d.path)),
            item("Copy folder path", () -> Shell.copy(d.path.getParent())),
            item("Copy file name", () -> { var c = new ClipboardContent(); c.putString(d.name()); Clipboard.getSystemClipboard().setContent(c); }),
            new SeparatorMenuItem(),
            item("Reveal in Explorer", () -> Shell.reveal(d.path)),
            item("Terminal here", () -> Shell.terminal(d.path)),
            item("Open in the other editor", () -> external(w, d)),
            item("Open with...", () -> Shell.openWith(d.path)),
            new SeparatorMenuItem(),
            item(d.source() ? "Show formatted" : "Edit source", () -> { w.show(d); w.toggleSource(); }),
            item("Move to a new window", () -> Windows.tearOff(d, w.stage.getX() + 160, w.stage.getY() + 60)));
        m.getItems().stream().filter(i -> "Edit source".equals(i.getText()) || "Show formatted".equals(i.getText())).forEach(i -> i.setDisable(!d.markdown()));
        return m;
    }

    static Menu copyAll(MainWindow w, Doc d) {
        var m = new Menu("Copy all as");
        m.setMnemonicParsing(false);
        for (var f : Editor.Copy.values()) m.getItems().add(item(f.name().charAt(0) + f.name().substring(1).toLowerCase(), () -> d.editor().copyAll(f)));
        return m;
    }

    static void close(MainWindow w, List<Doc> docs) {
        if (docs.isEmpty() || !Prompts.confirmClose(w, docs)) return;
        docs.forEach(w.tabs::remove);
    }

    static void reload(MainWindow w, Doc d) {
        if (d.dirty.get() && !Prompts.yes(w, "Reload from disk", d.name() + " has unsaved changes. Discard them and read the file again?")) return;
        if (d.conflict()) d.takeDisk(); else d.reload();
    }

    /** The editor chosen in Settings; vscode until chosen. */
    static void external(MainWindow w, Doc d) {
        var problem = ExternalEditor.open(Windows.session.externalEditor(), d.path);
        if (problem != null) Prompts.error(w, problem, null);
    }

    private TabMenu() {}
}
