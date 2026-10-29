package prosemirrorfx.notepadfx.ui;

import java.util.function.Consumer;
import javafx.scene.input.KeyCode;
import prosemirrorfx.notepadfx.editor.Editor;

import static javafx.scene.input.KeyCode.*;

public enum Action {
    SETTINGS(F1, "Settings", SettingsDialog::show),
    SAVE(F2, "Save", MainWindow::save),
    DICTATE(F3, "Dictate", w -> w.onEditor(Editor::dictate)),
    CLOSE(F4, "Close", MainWindow::closeActive),
    REFRESH(F5, "Refresh", MainWindow::refresh),
    PANE(F6, "Next pane", MainWindow::nextPane),
    FILES(F7, "Files", MainWindow::toggleNav),
    OUTLINE(F8, "Outline", MainWindow::toggleToc),
    READ(F9, "Read aloud", w -> w.onEditor(Editor::read)),
    WINDOW(F10, "New window", w -> Windows.newWindow()),
    THEME(F11, "Theme", w -> Windows.toggleTheme()),
    OPEN(F12, "Open", MainWindow::openDialog),
    TEAR(F9, true, "Tab to window", MainWindow::tearOffActive);

    public final KeyCode key;
    public final boolean shift;
    public final String label;
    final Consumer<MainWindow> run;

    Action(KeyCode key, String label, Consumer<MainWindow> run) { this(key, false, label, run); }

    Action(KeyCode key, boolean shift, String label, Consumer<MainWindow> run) {
        this.key = key;
        this.shift = shift;
        this.label = label;
        this.run = run;
    }

    public void run(MainWindow w) { run.accept(w); }

    public String keyName() { return (shift ? "Shift+" : "") + key.getName(); }

    public static Action of(KeyCode k, boolean shift) {
        for (var a : values()) if (a.key == k && a.shift == shift) return a;
        return null;
    }
}
