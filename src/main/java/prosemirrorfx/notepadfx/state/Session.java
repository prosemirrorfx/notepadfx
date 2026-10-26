package prosemirrorfx.notepadfx.state;

import datapotter.datahelper.Data;
import datapotter.datahelper.json.Json_I;
import java.util.*;

@Data
public final class Session extends Session_A implements Json_I<Session> {
    List<WorkspaceState> workspaces = new ArrayList<>();
    List<WindowState> windows = new ArrayList<>();
    String theme = "dark";
    Double fontSize = 15.0;
    Boolean outlineBullets = true;
    Boolean outlineParagraphs = true;
}
