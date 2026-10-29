package prosemirrorfx.notepadfx.state;

import datapotter.datahelper.Data;
import datapotter.datahelper.json.Json_I;
import java.util.*;

@Data
public final class WindowState extends WindowState_A implements Json_I<WindowState> {
    Double x, y, width = 1280.0, height = 820.0;
    Boolean maximized = false;
    Boolean navVisible = true;
    Boolean tocVisible = true;
    Double navWidth = 300.0, tabsWidth = 210.0, tocWidth = 260.0;
    List<Double> navSplit = new ArrayList<>(List.of(0.22, 0.48));
    String workspace;
    String project;
    List<String> files = new ArrayList<>();
    String activeFile;
    Double zoom;
}
