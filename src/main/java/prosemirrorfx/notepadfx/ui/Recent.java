package prosemirrorfx.notepadfx.ui;

import java.nio.file.*;
import java.util.Locale;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.VBox;
import javafx.stage.*;

// Shift+F12: files opened before, newest first. Type to narrow, arrows and Enter to open, Esc to leave, Delete to forget one.
final class Recent {
    static void show(MainWindow w) {
        var all = Windows.session.recent().stream().map(Path::of).toList();
        var filter = new TextField();
        filter.setPromptText("Recent files: type to narrow");
        var list = new ListView<Path>();
        list.setCellFactory(v -> new ListCell<>() {
            @Override protected void updateItem(Path p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) { setText(null); setTooltip(null); return; }
                var dir = p.getParent() == null ? "" : p.getParent().toString();
                setText(p.getFileName() + "    " + dir);
                setOpacity(Files.exists(p) ? 1.0 : 0.45);
                setTooltip(new Tooltip(p.toString()));
            }
        });
        Runnable refill = () -> {
            var q = filter.getText().toLowerCase(Locale.ROOT).trim();
            list.setItems(FXCollections.observableArrayList(all.stream().filter(p -> q.isEmpty() || p.toString().toLowerCase(Locale.ROOT).contains(q)).toList()));
            if (!list.getItems().isEmpty()) list.getSelectionModel().selectFirst();
        };
        filter.textProperty().subscribe(refill);

        var box = new VBox(8, filter, list);
        box.setPadding(new Insets(10));
        box.getStyleClass().addAll("np-root", Windows.theme());
        var stage = new Stage(StageStyle.UTILITY);
        stage.initOwner(w.stage);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle("Recent files");
        var scene = new Scene(box, 640, 420);
        scene.getStylesheets().add(Theme.css());
        stage.setScene(scene);

        Runnable open = () -> {
            var p = list.getSelectionModel().getSelectedItem();
            if (p == null) return;
            if (!Files.exists(p)) { Windows.forget(p); Windows.persist(); refill.run(); return; }
            stage.close();
            Windows.open(p);
        };
        list.setOnMouseClicked(e -> { if (e.getClickCount() == 2) open.run(); });
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            switch (e.getCode()) {
                case ESCAPE -> { stage.close(); e.consume(); }
                case ENTER -> { open.run(); e.consume(); }
                case DOWN -> { list.getSelectionModel().selectNext(); list.scrollTo(list.getSelectionModel().getSelectedIndex()); e.consume(); }
                case UP -> { list.getSelectionModel().selectPrevious(); list.scrollTo(list.getSelectionModel().getSelectedIndex()); e.consume(); }
                case DELETE -> {
                    var p = list.getSelectionModel().getSelectedItem();
                    if (p != null && e.isShiftDown()) { Windows.forget(p); Windows.persist(); refill.run(); e.consume(); }
                }
                default -> {}
            }
        });
        refill.run();
        stage.show();
        filter.requestFocus();
    }

    private Recent() {}
}
