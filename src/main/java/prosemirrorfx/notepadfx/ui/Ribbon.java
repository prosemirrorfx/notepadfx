package prosemirrorfx.notepadfx.ui;

import javafx.scene.control.*;
import javafx.scene.layout.*;

final class Ribbon {
    static VBox build(MainWindow w) {
        var spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        var box = new VBox(4,
            button("☰", "Files panes (F7)", w::toggleNav),
            button("≡", "Outline (F8)", w::toggleToc),
            button("⭳", "Open file (F12)", w::openDialog),
            button("◷", "Recent files (Shift+F12)", w::recent),
            button("«", "Previous comment, a >> paragraph (Shift+F5)", () -> w.onEditor(e -> e.comment(-1))),
            button("»", "Next comment, a >> paragraph (Shift+F6)", () -> w.onEditor(e -> e.comment(1))),
            button("⌂", "Add workspace folder", w.nav.workspaces::add),
            button("⧉", "New window (F10)", Windows::newWindow),
            spacer,
            button("⚙","Settings (F1)", () -> SettingsDialog.show(w)),
            button("◐", "Light or dark (F11)", Windows::toggleTheme));
        box.getStyleClass().add("np-ribbon");
        return box;
    }

    static Button button(String glyph, String tip, Runnable run) {
        var b = new Button(glyph);
        b.getStyleClass().add("np-ribbon-button");
        b.setTooltip(new Tooltip(tip));
        b.setFocusTraversable(false);
        b.setMnemonicParsing(false);
        b.setOnAction(e -> run.run());
        return b;
    }

    private Ribbon() {}
}
