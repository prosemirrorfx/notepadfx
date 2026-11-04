package prosefx.notepadfx.agent;

import datapotter.datahelper.Data;

/** The result of working out an edit on a text: the new text and where it landed, or why it cannot be made (code is an Exit value). */
@Data
public final class Change extends Change_A {
    int code = Exit.DONE;
    String problem;
    String text;
    int firstLine;
    int lines;

    public static Change fail(int code, String problem) { return new Change().code(code).problem(problem); }

    public boolean ok() { return code == Exit.DONE; }
}
