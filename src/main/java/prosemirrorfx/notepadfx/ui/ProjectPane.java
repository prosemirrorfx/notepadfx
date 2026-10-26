package prosemirrorfx.notepadfx.ui;

import java.nio.file.*;
import java.util.Comparator;
import java.util.regex.*;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;

final class ProjectPane {
    final MainWindow w;
    final NavPanel nav;
    final ListView<Path> list = new ListView<>();
    final TextField filter = Section.filter("Filter projects (regex)");
    final VBox node;
    boolean quiet;

    ProjectPane(MainWindow w, NavPanel nav) {
        this.w = w;
        this.nav = nav;
        list.getStyleClass().add("np-list");
        list.setCellFactory(v -> new PathListCell());
        list.getSelectionModel().selectedItemProperty().subscribe(p -> { if (p != null && !quiet) nav.projectChosen(p.toString()); });
        list.setContextMenu(FileMenu.forList(w, list, this::reload));
        filter.textProperty().subscribe(t -> filterChanged());
        filter.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.DOWN) { list.requestFocus(); list.getSelectionModel().selectFirst(); } });
        node = Section.pane(Section.header("Projects"), filter, list);
    }

    void filterChanged() {
        var ws = Workspaces.of(w.state.workspace());
        if (ws != null) ws.projectFilter(filter.getText());
        reload();
    }

    void reload() {
        var ws = Workspaces.of(w.state.workspace());
        quiet = true;
        try {
            if (ws == null) { list.getItems().clear(); return; }
            if (!filter.getText().equals(ws.projectFilter())) filter.setText(ws.projectFilter());
            var match = matcher(ws.projectFilter());
            try (var s = Files.list(Path.of(ws.path()))) {
                list.getItems().setAll(s.filter(Files::isDirectory).filter(p -> match.test(p.getFileName().toString()))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString().toLowerCase())).toList());
            } catch (Exception e) {
                list.getItems().clear();
            }
            select(w.state.project());
        } finally {
            quiet = false;
        }
    }

    void select(String path) {
        if (path == null) return;
        list.getItems().stream().filter(p -> p.toString().equalsIgnoreCase(path)).findFirst().ifPresent(p -> {
            list.getSelectionModel().select(p);
            list.scrollTo(p);
            nav.projectChosen(p.toString());
        });
    }

    static java.util.function.Predicate<String> matcher(String regex) {
        if (regex == null || regex.isBlank()) return s -> true;
        try {
            return Pattern.compile(regex, Pattern.CASE_INSENSITIVE).asPredicate();
        } catch (PatternSyntaxException e) {
            var plain = regex.toLowerCase();
            return s -> s.toLowerCase().contains(plain);
        }
    }
}
