package prosefx.notepadfx;

import jarrunner.jr.apputils.*;
import java.util.List;
import prosefx.notepadfx.agent.*;
import prosefx.notepadfx.sys.*;
import prosefx.notepadfx.ui.Windows;

public class Main {
    public static void main(String[] args) {
        Timing.mark("main");
        Timing.jvm();
        if (args.length > 0 && AgentCli.handles(args[0])) System.exit(AgentCli.run(List.of(args)));
        if (args.length > 0 && command(args[0], args.length > 1 ? args[1] : "")) return;
        var list = OpenArgs.normalize(List.of(args));
        if (!list.contains("--wait") && OpenArgs.wantsAnswer(list) && askRunning(list)) return;
        args = list.toArray(String[]::new);
        // --wait (an editor hook such as Ctrl+G in claude-code): its own process, never handed to a running notepadfx, ends when its file is closed
        if (list.contains("--wait")) { Windows.standalone = true; App.launchWith(args); System.exit(0); }
        var primary = Instance.primary();
        if (!primary && Instance.handOff(list)) { AotTraining.preload(); System.exit(0); }
        if (list.stream().anyMatch(a -> a.startsWith("--snapshot="))) {
            System.err.println("notepadfx: --snapshot needs a running notepadfx");
            System.exit(2);
        }
        App.launchWith(args);
        System.exit(0);
    }

    /** `file:line` and `--owner` want to hear whether the file and line were there: ask the running notepadfx; false when none runs, and the launch carries on. */
    static boolean askRunning(List<String> list) {
        var code = AgentCli.open(list);
        if (code == Exit.NOT_RUNNING) return false;
        System.exit(code);
        return true;
    }

    static boolean command(String verb, String arg) {
        if (List.of("install", "update", "uninstall", "explorer-menu", "association", "--version", "version").contains(verb)) AotTraining.preload();
        switch (verb) {
            case "install" -> {
                var r = Install.app("notepadfx").displayName("notepadfx")
                    .description("A fast markdown and text editor").desktopShortcut(true).run();
                r.problems().forEach(System.err::println);
                if (r.ok()) {
                    System.out.println("Installed: " + r.command());
                    System.out.println(ExplorerMenu.add(r.command()) ? "Explorer menu: Open with notepadfx" : "Explorer menu could not be added");
                    System.out.println(FileAssoc.add(r.command()) ? "Opens .md, .markdown and .txt; if Windows still asks, choose notepadfx once in Settings > Default apps" : "File association could not be added");
                }
                System.exit(r.ok() ? 0 : 1);
            }
            case "update" -> {
                var r = Updater.update();
                System.out.println(r.message());
                System.exit(r.ok() ? 0 : 1);
            }
            case "uninstall" -> {
                ExplorerMenu.remove();
                FileAssoc.remove();
                System.exit(Install.uninstall("notepadfx").ok() ? 0 : 1);
            }
            case "explorer-menu" -> {
                var ok = arg.equals("off") ? ExplorerMenu.remove() : ExplorerMenu.add(InstallPath.installedOrRunning());
                System.out.println(ok ? "Explorer menu " + (arg.equals("off") ? "removed" : "added: Open with notepadfx") : "Explorer menu change failed");
                System.exit(ok ? 0 : 1);
            }
            case "association" -> {
                var off = arg.equals("off");
                var ok = off ? FileAssoc.remove() : FileAssoc.add(InstallPath.installedOrRunning());
                System.out.println(ok ? (off ? "File association removed" : "notepadfx registered for .md, .markdown and .txt, with its icons") : "File association change failed");
                if (ok && !off && !FileAssoc.isDefault()) {
                    System.out.println("Windows keeps the user's own choice: opening Settings > Default apps, pick notepadfx there once");
                    FileAssoc.openDefaultAppsPage();
                }
                System.exit(ok ? 0 : 1);
            }
            case "--version", "version" -> {
                System.out.println("notepadfx " + JrApp.appVersion().orElse("(development build)"));
                System.exit(0);
            }
            default -> { return false; }
        }
        return true;
    }
}
