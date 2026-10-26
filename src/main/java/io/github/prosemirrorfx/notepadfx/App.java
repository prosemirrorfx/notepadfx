package io.github.prosemirrorfx.notepadfx;

import io.github.prosemirrorfx.notepadfx.state.Store;
import io.github.prosemirrorfx.notepadfx.sys.*;
import io.github.prosemirrorfx.notepadfx.ui.Windows;
import java.util.List;
import javafx.application.*;
import javafx.stage.Stage;

public class App extends Application {
    static List<String> args = List.of();

    public static void launchWith(String[] a) {
        args = List.of(a);
        launch(App.class, a);
    }

    @Override
    public void start(Stage ignored) {
        Platform.setImplicitExit(false);
        Windows.start(Store.load(), args);
        Instance.serve(a -> Platform.runLater(() -> Windows.handle(a)));
        IdleTrim.start();
    }

    @Override
    public void stop() { Windows.persist(); }
}
