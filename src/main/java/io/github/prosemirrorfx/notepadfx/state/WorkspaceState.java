package io.github.prosemirrorfx.notepadfx.state;

import datapotter.datahelper.Data;
import datapotter.datahelper.json.Json_I;
import java.util.*;

@Data
public final class WorkspaceState extends WorkspaceState_A implements Json_I<WorkspaceState> {
    String path;
    String projectFilter = "";
    Map<String, String> fileFilters = new LinkedHashMap<>();
}
