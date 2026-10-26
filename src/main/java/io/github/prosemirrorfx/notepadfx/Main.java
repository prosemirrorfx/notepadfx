package io.github.prosemirrorfx.notepadfx;

import io.github.prosemirrorfx.notepadfx.sys.Instance;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        var list = List.of(args);
        var primary = Instance.primary();
        if (!primary && Instance.handOff(list)) System.exit(0);
        if (list.stream().anyMatch(a -> a.startsWith("--snapshot="))) {
            System.err.println("notepadfx: --snapshot needs a running notepadfx");
            System.exit(2);
        }
        App.launchWith(args);
        System.exit(0);
    }
}
