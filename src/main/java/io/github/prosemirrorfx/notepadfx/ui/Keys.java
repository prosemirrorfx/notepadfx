package io.github.prosemirrorfx.notepadfx.ui;

import javafx.scene.Scene;
import javafx.scene.input.*;

import static javafx.scene.input.KeyCode.*;

final class Keys {
    static void install(Scene scene, MainWindow w) {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            var a = e.isShortcutDown() || e.isAltDown() ? null : Action.of(e.getCode());
            if (a == null) a = combo(e);
            if (a != null) { a.run(w); e.consume(); return; }
            if (e.isControlDown() && (e.getCode() == TAB || e.getCode() == PAGE_DOWN || e.getCode() == PAGE_UP)) {
                w.tabs.step(e.isShiftDown() || e.getCode() == PAGE_UP ? -1 : 1);
                e.consume();
            }
        });
    }

    static Action combo(KeyEvent e) {
        if (!e.isShortcutDown() || e.isAltDown()) return null;
        return switch (e.getCode()) {
            case S -> Action.SAVE;
            case W -> Action.CLOSE;
            case O -> Action.OPEN;
            case N -> Action.WINDOW;
            default -> null;
        };
    }

    private Keys() {}
}
