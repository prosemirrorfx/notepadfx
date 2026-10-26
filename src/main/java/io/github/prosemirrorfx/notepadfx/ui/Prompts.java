package io.github.prosemirrorfx.notepadfx.ui;

import io.github.prosemirrorfx.notepadfx.doc.Doc;
import java.io.File;
import java.nio.file.Path;
import java.util.*;
import javafx.scene.control.*;
import javafx.stage.*;

final class Prompts {
    static Path lastDir;

    static List<Path> openFiles(MainWindow w) {
        var fc = new FileChooser();
        fc.setTitle("Open");
        if (lastDir != null) fc.setInitialDirectory(lastDir.toFile());
        fc.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Markdown and text", "*.md", "*.markdown", "*.txt"),
            new FileChooser.ExtensionFilter("All files", "*.*"));
        var files = fc.showOpenMultipleDialog(w.stage);
        if (files == null || files.isEmpty()) return List.of();
        lastDir = files.getFirst().getParentFile().toPath();
        return files.stream().map(File::toPath).toList();
    }

    static Path chooseDir(MainWindow w, String title) {
        var dc = new DirectoryChooser();
        dc.setTitle(title);
        if (lastDir != null) dc.setInitialDirectory(lastDir.toFile());
        var f = dc.showDialog(w.stage);
        if (f != null) lastDir = f.toPath();
        return f == null ? null : f.toPath();
    }

    static void save(MainWindow w, Doc d) {
        if (d == null) return;
        try { d.save(); } catch (Exception e) { error(w, "Could not save " + d.name(), e); }
    }

    static boolean confirmClose(MainWindow w, List<Doc> docs) {
        for (var d : docs) {
            if (!d.dirty.get()) continue;
            w.show(d);
            var save = new ButtonType("Save", ButtonBar.ButtonData.YES);
            var discard = new ButtonType("Discard", ButtonBar.ButtonData.NO);
            var a = alert(w, Alert.AlertType.CONFIRMATION, d.name() + " has unsaved changes.", save, discard, ButtonType.CANCEL);
            var r = a.showAndWait().orElse(ButtonType.CANCEL);
            if (r == ButtonType.CANCEL) return false;
            if (r == save) { save(w, d); if (d.dirty.get()) return false; }
        }
        return true;
    }

    static String ask(MainWindow w, String title, String label, String initial) {
        var d = new TextInputDialog(initial);
        d.initOwner(w.stage);
        d.setTitle(title);
        d.setHeaderText(null);
        d.setContentText(label);
        return d.showAndWait().orElse(null);
    }

    static boolean yes(MainWindow w, String title, String text) {
        var a = alert(w, Alert.AlertType.CONFIRMATION, text, ButtonType.OK, ButtonType.CANCEL);
        a.setTitle(title);
        return a.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    static void error(MainWindow w, String text, Exception e) {
        alert(w, Alert.AlertType.ERROR, text + (e == null ? "" : "\n" + e.getMessage()), ButtonType.OK).showAndWait();
    }

    static Alert alert(MainWindow w, Alert.AlertType type, String text, ButtonType... buttons) {
        var a = new Alert(type, text, buttons);
        a.initOwner(w.stage);
        a.setHeaderText(null);
        a.getDialogPane().getStylesheets().add(Theme.css());
        a.getDialogPane().getStyleClass().addAll("np-root", Windows.theme());
        return a;
    }

    private Prompts() {}
}
