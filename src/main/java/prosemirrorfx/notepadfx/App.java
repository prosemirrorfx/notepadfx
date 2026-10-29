package prosemirrorfx.notepadfx;

import prosemirrorfx.notepadfx.state.Store;
import prosemirrorfx.notepadfx.sys.*;
import prosemirrorfx.notepadfx.ui.*;
import java.util.List;
import javafx.application.*;
import javafx.stage.Stage;
import us.hebi.graalvm.reachability.annotations.Reachable;

// Native image: the stylesheets and icon tables of notepadfx and the editor view.
@Reachable(resources = {
    "/prosemirrorfx/notepadfx/*.css", "/prosemirrorfx/notepadfx/*.tsv",
    "/prosemirrorfx/view/*.css", "/prosemirrorfx/view/*.tsv"})
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
        Updates.start();
    }

    @Override
    public void stop() { Windows.persist(); }
}
