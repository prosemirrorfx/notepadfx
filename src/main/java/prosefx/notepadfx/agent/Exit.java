package prosefx.notepadfx.agent;

/** What a command-line verb exits with; the same numbers are in `notepadfx --help`. */
public final class Exit {
    public static final int
        DONE = 0,
        ERROR = 1,
        NOT_RUNNING = 2,
        NOT_OPEN = 3,
        REFUSED = 4,
        STALE = 5,
        UNSAVED = 6,
        NO_ANSWER = 7,
        NOT_FOUND = 8;

    private Exit() {}
}
