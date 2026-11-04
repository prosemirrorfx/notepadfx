package prosefx.notepadfx.ui;

import java.util.concurrent.CompletableFuture;
import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.stage.*;
import javafx.util.Duration;

/**
 * The question a session's request to close a file puts to the person, on the keyboard: Enter agrees (Save, when there are
 * unsaved changes), F9 discards them, Esc keeps the file open. It takes the focus while the person may be typing, so it
 * ignores every key but Esc for its first two seconds and shows the countdown: a stray Enter must never answer it.
 */
final class ClosePrompt {
    static final int ARM_SECONDS = 2;
    final CompletableFuture<CloseAnswer> answer = new CompletableFuture<>();
    final Stage stage = new Stage();
    final Label hint = new Label();
    boolean armed;
    /** The question on screen now, for a test to answer. */
    static ClosePrompt current;

    static CompletableFuture<CloseAnswer> ask(MainWindow w, String question, boolean unsaved, int timeoutSeconds) {
        var p = current = new ClosePrompt();
        p.show(w, question, unsaved, timeoutSeconds);
        return p.answer;
    }

    void show(MainWindow w, String question, boolean unsaved, int timeoutSeconds) {
        var text = new Label(question);
        text.setWrapText(true);
        var box = new VBox(12, text, hint);
        box.setPadding(new Insets(22));
        box.setPrefWidth(520);
        box.getStyleClass().addAll("np-root", Windows.theme());
        var scene = new Scene(box);
        scene.getStylesheets().add(Theme.css());
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) finish(CloseAnswer.CANCEL);
            else if (armed && e.getCode() == KeyCode.ENTER) finish(unsaved ? CloseAnswer.SAVE : CloseAnswer.CLOSE);
            else if (armed && unsaved && e.getCode() == KeyCode.F9) finish(CloseAnswer.DISCARD);
            e.consume();
        });
        stage.initOwner(w.stage);
        stage.initModality(Modality.NONE);
        stage.setTitle("notepadfx");
        stage.setScene(scene);
        stage.setOnHidden(e -> finish(CloseAnswer.CANCEL));
        countdown(unsaved);
        timeout(timeoutSeconds);
        stage.show();
        stage.toFront();
        stage.requestFocus();
    }

    void countdown(boolean unsaved) {
        var keys = unsaved ? "Enter saves and closes   F9 closes without saving   Esc keeps it open" : "Enter closes it   Esc keeps it open";
        var line = new Timeline();
        for (var s = 0; s <= ARM_SECONDS; s++) {
            var left = ARM_SECONDS - s;
            line.getKeyFrames().add(new KeyFrame(Duration.seconds(s), e -> {
                armed = left == 0;
                hint.setText(armed ? keys : "(keys work in " + left + " s)   " + keys);
            }));
        }
        line.play();
    }

    void timeout(int seconds) {
        var t = new PauseTransition(Duration.seconds(Math.max(ARM_SECONDS + 1, seconds)));
        t.setOnFinished(e -> finish(CloseAnswer.TIMEOUT));
        t.play();
    }

    void finish(CloseAnswer a) {
        if (answer.complete(a)) { stage.hide(); if (current == this) current = null; }
    }
}
