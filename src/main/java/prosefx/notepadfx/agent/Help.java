package prosefx.notepadfx.agent;

import java.io.IOException;

/** The text of `notepadfx --help`, kept in help.txt beside this class. */
final class Help {
    static String text() {
        try (var in = Help.class.getResourceAsStream("/prosefx/notepadfx/help.txt")) {
            return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (IOException | NullPointerException e) {
            return "notepadfx: help text missing\n";
        }
    }

    private Help() {}
}
