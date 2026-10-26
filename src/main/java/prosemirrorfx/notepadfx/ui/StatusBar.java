package prosemirrorfx.notepadfx.ui;

import prosemirrorfx.notepadfx.doc.Doc;
import java.nio.file.Files;
import java.time.*;
import java.time.format.DateTimeFormatter;
import javafx.animation.PauseTransition;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

final class StatusBar {
    static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    final MainWindow w;
    final Label info = new Label(), path = new Label(), updateNotice = new Label();
    final FlowPane legend = new FlowPane(2, 0);
    final VBox node;
    final PauseTransition debounce = new PauseTransition(Duration.millis(400));
    final ChangeListener<Object> update = (o, a, b) -> debounce.playFromStart();
    Doc doc;

    StatusBar(MainWindow w) {
        this.w = w;
        for (var a : Action.values()) {
            var b = new Button(a.key.getName() + " " + a.label);
            b.getStyleClass().add("np-legend");
            b.setFocusTraversable(false);
            b.setMnemonicParsing(false);
            b.setOnAction(e -> a.run(w));
            legend.getChildren().add(b);
        }
        info.getStyleClass().add("np-info");
        path.getStyleClass().addAll("np-info", "np-path");
        path.setWrapText(true);
        updateNotice.getStyleClass().addAll("np-info", "np-update");
        updateNotice.textProperty().bind(Updates.NOTICE);
        updateNotice.visibleProperty().bind(Updates.NOTICE.isNotEmpty());
        updateNotice.managedProperty().bind(updateNotice.visibleProperty());
        updateNotice.setOnMouseClicked(e -> Updates.clicked());
        var top = new FlowPane(16, 0, updateNotice, info, path);
        top.setMinHeight(Region.USE_PREF_SIZE);
        top.prefWrapLengthProperty().bind(top.widthProperty());
        legend.setMinHeight(Region.USE_PREF_SIZE);
        legend.prefWrapLengthProperty().bind(legend.widthProperty());
        node = new VBox(1, top, legend);
        node.getStyleClass().add("np-status");
        debounce.setOnFinished(e -> refresh());
        w.active.subscribe(this::attach);
    }

    void attach(Doc d) {
        if (doc != null) { doc.dirty.removeListener(update); doc.version.removeListener(update); if (doc.hasEditor()) doc.editor().caret().removeListener(update); }
        doc = d;
        if (d != null) { d.dirty.addListener(update); d.version.addListener(update); d.editor().caret().addListener(update); }
        refresh();
    }

    void refresh() {
        if (doc == null) { info.setText(""); path.setText(""); return; }
        var text = doc.text();
        var where = doc.editor().where();
        info.setText(String.join("   ",
            doc.dirty.get() ? "● unsaved" : "saved",
            where.isEmpty() ? "" : where,
            words(text) + " words",
            text.length() + " chars",
            fileFacts()).replaceAll(" {6,}", "   "));
        path.setText(doc.path.toString());
    }

    String fileFacts() {
        try {
            var modified = LocalDateTime.ofInstant(Files.getLastModifiedTime(doc.path).toInstant(), ZoneId.systemDefault());
            return "modified " + WHEN.format(modified) + "   " + size(Files.size(doc.path)) + "   " + doc.encoding();
        } catch (Exception e) {
            return "not on disk";
        }
    }

    static String size(long bytes) {
        return bytes < 1024 ? bytes + " B" : bytes < 1024 * 1024 ? (bytes / 1024) + " KB" : String.format("%.1f MB", bytes / 1048576.0);
    }

    static int words(String text) {
        var n = 0;
        var in = false;
        for (var i = 0; i < text.length(); i++) {
            var c = text.charAt(i);
            var t = Character.getType(c);
            var letter = Character.isLetterOrDigit(c) || t == Character.NON_SPACING_MARK || t == Character.COMBINING_SPACING_MARK;
            if (letter && !in) n++;
            in = letter;
        }
        return n;
    }
}
