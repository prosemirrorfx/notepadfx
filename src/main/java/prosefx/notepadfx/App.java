package prosefx.notepadfx;

import prosefx.notepadfx.state.Store;
import prosefx.notepadfx.sys.*;
import prosefx.notepadfx.ui.*;
import java.util.*;
import javafx.application.*;
import javafx.stage.Stage;
import us.hebi.graalvm.reachability.annotations.Reachable;

// Native image: the stylesheets and icon tables of notepadfx and the editor view.
@Reachable(resources = {
    "/prosefx/notepadfx/*.css", "/prosefx/notepadfx/*.tsv", "/prosefx/notepadfx/*.txt",
    "/prosefx/view/*.css", "/prosefx/view/*.tsv"})
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
        if (Windows.standalone) session.windows(new ArrayList<>());
        Timing.mark("session");
        Windows.start(session, args);
        Timing.mark("windows built");
        Platform.runLater(() -> Timing.mark("first pulse"));
        if (Windows.standalone) return;
        Instance.serve(a -> Platform.runLater(() -> Windows.handle(a)), AgentServer::request);
        IdleTrim.start();
        Updates.start();
        Updates.tuneStartup();
    }

    @Override
    public void stop() { Windows.persist(); }
}
