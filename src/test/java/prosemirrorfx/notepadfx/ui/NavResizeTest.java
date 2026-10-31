package prosemirrorfx.notepadfx.ui;

import java.util.concurrent.*;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.input.*;
import org.junit.jupiter.api.*;
import prosemirrorfx.notepadfx.state.*;

import static org.junit.jupiter.api.Assertions.*;

/** The Workspaces, Projects and Files panes, resized with real mouse events on the dividers (headless JavaFX, no window on screen). */
class NavResizeTest {

    static MainWindow w;

    @BeforeAll static void boot() throws Exception {
        System.setProperty("glass.platform", "Headless");
        System.setProperty("prism.order", "sw");
        var ready = new CountDownLatch(1);
        try { Platform.startup(ready::countDown); } catch (IllegalStateException again) { ready.countDown(); }
        ready.await(10, TimeUnit.SECONDS);
        fx(() -> { Windows.session = new Session(); Windows.standalone = true; w = new MainWindow(new WindowState()); w.stage.show(); return null; });
        pulse();
    }

    static <T> T fx(Callable<T> c) throws Exception {
        var f = new FutureTask<>(c);
        Platform.runLater(f);
        return f.get(15, TimeUnit.SECONDS);
    }

    static void pulse() throws Exception { for (var k = 0; k < 4; k++) { fx(() -> { w.root.applyCss(); w.root.layout(); return null; }); Thread.sleep(60); } }

    double[] heights() throws Exception {
        return fx(() -> new double[]{w.nav.workspaces.node.getHeight(), w.nav.projects.node.getHeight(), w.nav.files.node.getHeight()});
    }

    void drag(javafx.scene.Node divider, double dy) throws Exception {
        fx(() -> {
            var c = divider.localToScene(divider.getLayoutBounds().getWidth() / 2, divider.getLayoutBounds().getHeight() / 2);
            var s = divider.localToScreen(divider.getLayoutBounds().getWidth() / 2, divider.getLayoutBounds().getHeight() / 2);
            fire(divider, MouseEvent.MOUSE_PRESSED, c, s);
            for (var k = 1; k <= 6; k++) fire(divider, MouseEvent.MOUSE_DRAGGED, c.add(0, dy * k / 6), s.add(0, dy * k / 6));
            fire(divider, MouseEvent.MOUSE_RELEASED, c.add(0, dy), s.add(0, dy));
            return null;
        });
        pulse();
    }

    static void fire(javafx.scene.Node n, javafx.event.EventType<MouseEvent> type, Point2D scene, Point2D screen) {
        javafx.event.Event.fireEvent(n, new MouseEvent(type, scene.getX(), scene.getY(), screen.getX(), screen.getY(), MouseButton.PRIMARY, 1,
            false, false, false, false, type != MouseEvent.MOUSE_RELEASED, false, false, true, false, true, null));
    }

    @Test void theTwoDividersMoveWhenDragged() throws Exception {
        var dividers = fx(() -> w.nav.node.lookupAll(".split-pane-divider").stream().toList());
        assertEquals(2, dividers.size());
        var before = heights();
        drag(dividers.get(0), 60);
        var mid = heights();
        assertTrue(mid[0] > before[0] + 30, "first pane did not grow: " + before[0] + " -> " + mid[0]);
        assertTrue(mid[1] < before[1] - 30, "second pane did not shrink: " + before[1] + " -> " + mid[1]);
        drag(dividers.get(1), -80);
        var after = heights();
        assertTrue(after[2] > mid[2] + 40, "third pane did not grow: " + mid[2] + " -> " + after[2]);
    }

    /** The reason the panes could not be resized: the app's CSS made each divider 0 px high, so there was nothing to grab. */
    @Test void everyDividerHasRoomToBeGrabbedWithAMouse() throws Exception {
        var thickness = fx(() -> w.nav.node.lookupAll(".split-pane-divider").stream().mapToDouble(n -> n.getLayoutBounds().getHeight()).min().orElse(0));
        assertTrue(thickness >= 5, "divider only " + thickness + " px high");
        var grabbable = fx(() -> w.nav.node.lookupAll(".split-pane-divider").stream().allMatch(n -> n.contains(n.getLayoutBounds().getWidth() / 2, n.getLayoutBounds().getHeight() / 2)));
        assertTrue(grabbable);
    }
}
