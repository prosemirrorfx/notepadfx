package prosemirrorfx.notepadfx;

import jarrunner.jr.apputils.*;
import java.util.List;
import prosemirrorfx.notepadfx.sys.*;

public class Main {
    public static void main(String[] args) {
        Timing.mark("main");
        Timing.jvm();
        if (args.length > 0 && command(args[0])) return;
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

    static boolean command(String verb) {
        switch (verb) {
            case "install" -> {
                var r = Install.app("notepadfx").displayName("notepadfx")
                    .description("A fast markdown and text editor").desktopShortcut(true).run();
                r.problems().forEach(System.err::println);
                if (r.ok()) System.out.println("Installed: " + r.command());
                System.exit(r.ok() ? 0 : 1);
            }
            case "update" -> {
                var r = Updater.update();
                System.out.println(r.message());
                System.exit(r.ok() ? 0 : 1);
            }
            case "uninstall" -> System.exit(Install.uninstall("notepadfx").ok() ? 0 : 1);
            case "--version", "version" -> {
                System.out.println("notepadfx " + JrApp.appVersion().orElse("(development build)"));
                System.exit(0);
            }
            default -> { return false; }
        }
        return true;
    }
}
