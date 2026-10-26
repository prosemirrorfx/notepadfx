package prosemirrorfx.notepadfx;

import prosemirrorfx.notepadfx.state.Store;
import prosemirrorfx.notepadfx.sys.*;
import prosemirrorfx.notepadfx.ui.Windows;
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
        Timing.mark("fx start");
        Platform.setImplicitExit(false);
        var session = Store.load();
        Timing.mark("session");
        Windows.start(session, args);
        Timing.mark("windows built");
        Platform.runLater(() -> Timing.mark("first pulse"));
        Instance.serve(a -> Platform.runLater(() -> Windows.handle(a)));
        IdleTrim.start();
    }

    @Override
    public void stop() { Windows.persist(); }
}
