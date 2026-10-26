package prosemirrorfx.notepadfx.state;

import datapotter.datahelper.Data;
import datapotter.datahelper.json.Json_I;
import java.util.*;

@Data
public final class FoldState extends FoldState_A implements Json_I<FoldState> {
    List<String> keys = new ArrayList<>();
    Long modified;
}
