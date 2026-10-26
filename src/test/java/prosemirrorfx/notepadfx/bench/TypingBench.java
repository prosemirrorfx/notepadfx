package prosemirrorfx.notepadfx.bench;

import java.nio.file.*;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.input.*;
import javafx.stage.Stage;
import prosemirrorfx.notepadfx.editor.ProseEditor;

// Not a test: a typing benchmark. java -cp <test classpath> prosemirrorfx.notepadfx.bench.TypingBench <file.md> [ln]
// types into the markdown editor and prints how long each keystroke takes until the next layout pulse is done
public class TypingBench {
    static Runnable pending;

    public static void main(String[] args) throws Exception {
        var text = Files.readString(Path.of(args[0]));
        var lineNumbers = args.length > 1 && args[1].equals("ln");
        Platform.startup(() -> {
            var ed = new ProseEditor();
            var stage = new Stage();
            var scene = new Scene(new javafx.scene.layout.StackPane(ed.node()), 1000, 800);
            scene.addPostLayoutPulseListener(() -> { var p = pending; pending = null; if (p != null) Platform.runLater(p); });
            stage.setScene(scene);
            ed.lineNumbers(lineNumbers);
            ed.theme(true);
            ed.load(text);
            stage.show();
            ed.focus();
            ed.position(Math.min(text.length() / 2, 2000));
            var wait = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2));
            wait.setOnFinished(e -> type(scene, 0, new long[60]));
            wait.play();
        });
    }

    static void type(Scene scene, int i, long[] t) {
        if (i == t.length) {
            java.util.Arrays.sort(t);
            System.out.printf("keystrokes %d  median %.1f ms  p90 %.1f ms  max %.1f ms%n", t.length, t[t.length / 2] / 1e6, t[t.length * 9 / 10] / 1e6, t[t.length - 1] / 1e6);
            Platform.exit();
            return;
        }
        var target = scene.getFocusOwner() != null ? scene.getFocusOwner() : scene.getRoot();
        var start = System.nanoTime();
        var ch = String.valueOf((char) ('a' + i % 26));
        javafx.event.Event.fireEvent(target, new KeyEvent(KeyEvent.KEY_TYPED, ch, "", KeyCode.UNDEFINED, false, false, false, false));
        pending = () -> { t[i] = System.nanoTime() - start; type(scene, i + 1, t); };
        Platform.requestNextPulse();
    }
}
