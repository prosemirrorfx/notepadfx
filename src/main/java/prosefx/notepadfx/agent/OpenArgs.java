package prosefx.notepadfx.agent;

import java.util.*;
import java.util.regex.Pattern;

/** The ways a launch can say where to go and who it is: `file:22`, `file:22:5`, `--line 22 --end-line 25`, `--owner name`, all turned into `--name=value` words, which survive the trip to the running notepadfx. */
public final class OpenArgs {
    static final Pattern PLACE = Pattern.compile("(.+?):(\\d+)(?::(\\d+))?");
    static final Set<String> VALUED = Set.of("--owner", "--line", "--end-line", "--col");

    public static List<String> normalize(List<String> in) {
        var out = new ArrayList<String>();
        for (var i = 0; i < in.size(); i++) {
            var t = in.get(i);
            if (VALUED.contains(t) && i + 1 < in.size()) out.add(t + "=" + in.get(++i));
            else if (!t.startsWith("-") && PLACE.matcher(t).matches()) {
                var m = PLACE.matcher(t);
                m.matches();
                out.add(m.group(1));
                out.add("--line=" + m.group(2));
                if (m.group(3) != null) out.add("--col=" + m.group(3));
            } else out.add(t);
        }
        return out;
    }

    /** True when the launch carries a place or an owner, so it wants an answer about the file and line. */
    public static boolean wantsAnswer(List<String> normalized) {
        return normalized.stream().anyMatch(a -> a.startsWith("--owner=") || a.startsWith("--line=")) && normalized.stream().filter(a -> !a.startsWith("-")).count() == 1;
    }

    private OpenArgs() {}
}
