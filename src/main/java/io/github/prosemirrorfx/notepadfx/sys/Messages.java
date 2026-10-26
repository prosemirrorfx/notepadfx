package io.github.prosemirrorfx.notepadfx.sys;

import java.nio.file.Path;
import java.util.*;

public final class Messages {
    static final String SEP = "\u001f";

    public static String encode(List<String> args) {
        return String.join(SEP, args.stream().map(a -> a.startsWith("-") ? a : Path.of(a).toAbsolutePath().toString()).toList());
    }

    public static List<String> decode(String line) {
        return line.isEmpty() ? List.of() : List.of(line.split(SEP));
    }

    private Messages() {}
}
