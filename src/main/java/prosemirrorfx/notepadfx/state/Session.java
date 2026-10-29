package prosemirrorfx.notepadfx.state;

import datapotter.datahelper.Data;
import datapotter.datahelper.json.Json_I;
import java.util.*;

@Data
public final class Session extends Session_A implements Json_I<Session> {
    List<WorkspaceState> workspaces = new ArrayList<>();
    List<WindowState> windows = new ArrayList<>();
    String theme = "dark";
    Double zoom = 1.0;
    Boolean lineNumbers = false;
    Map<String, Integer> carets = new LinkedHashMap<>();
    Map<String, Double> zooms = new LinkedHashMap<>();
    Map<String, FoldState> folds = new LinkedHashMap<>();
    Boolean outlineBullets = true;
    Boolean outlineParagraphs = true;
    List<String> outlineShow = new ArrayList<>(List.of("h1", "h2", "h3", "h4", "h5", "h6", "para", "b1", "b2", "b3"));
    String outlineFilter = "";
}
