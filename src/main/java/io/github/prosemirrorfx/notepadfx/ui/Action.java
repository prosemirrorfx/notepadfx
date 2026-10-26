package io.github.prosemirrorfx.notepadfx.ui;

import java.util.function.Consumer;
import javafx.scene.input.KeyCode;

import static javafx.scene.input.KeyCode.*;

public enum Action {
    SAVE(F2, "Save", MainWindow::save),
    CLOSE(F4, "Close", MainWindow::closeActive),
    REFRESH(F5, "Refresh", MainWindow::refresh),
    PANE(F6, "Next pane", MainWindow::nextPane),
    FILES(F7, "Files", MainWindow::toggleNav),
    OUTLINE(F8, "Outline", MainWindow::toggleToc),
    TEAR(F9, "Tab to window", MainWindow::tearOffActive),
    WINDOW(F10, "New window", w -> Windows.newWindow()),
    THEME(F11, "Theme", w -> Windows.toggleTheme()),
    OPEN(F12, "Open", MainWindow::openDialog);

    public final KeyCode key;
    public final String label;
    final Consumer<MainWindow> run;

    Action(KeyCode key, String label, Consumer<MainWindow> run) {
        this.key = key;
        this.label = label;
        this.run = run;
    }

    public void run(MainWindow w) { run.accept(w); }

    public static Action of(KeyCode k) {
        for (var a : values()) if (a.key == k) return a;
        return null;
    }
}
