package prosemirrorfx.notepadfx.toc;

import datapotter.datahelper.Data;
import java.util.*;

@Data
public final class OutlineNode extends OutlineNode_A {
    String type;
    Integer level;
    Integer depth;
    Integer rank;
    String label;
    Integer line;
    List<OutlineNode> children = new ArrayList<>();
}
