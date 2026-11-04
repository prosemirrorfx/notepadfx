package prosefx.notepadfx.ui;

import prosefx.notepadfx.agent.*;
import prosefx.notepadfx.doc.*;
import java.util.concurrent.CompletableFuture;

import static prosefx.notepadfx.ui.AgentServer.reply;

/**
 * `notepadfx close <file>`. The session that opened a file (`--owner`) may close it when nothing is unsaved. Anyone else
 * only asks, and the person answers. Unsaved changes are never lost without the person saying so: they are asked first,
 * whoever is closing.
 */
final class AgentClose {

    static void run(Doc d, Args a, CompletableFuture<String> answer) {
        var caller = a.get("owner", "");
        var owned = d.owner() != null && d.owner().equals(caller);
        var unsaved = d.dirty.get();
        var w = Windows.windowOf(d) != null ? Windows.windowOf(d) : Windows.target();
        if (owned && !unsaved) { close(w, d); answer.complete(reply(Exit.DONE, "closed " + d.path + "\n")); return; }
        if (a.has("no-ask")) {
            answer.complete(unsaved ? reply(Exit.UNSAVED, d.name() + " has unsaved changes, not closed\n") : reply(Exit.REFUSED, "only the session that opened " + d.name() + " may close it without asking\n"));
            return;
        }
        w.stage.toFront();
        ClosePrompt.ask(w, question(d, caller, owned, unsaved), unsaved, a.number("timeout", 120)).thenAccept(r -> answer.complete(finish(w, d, r)));
    }

    static String question(Doc d, String caller, boolean owned, boolean unsaved) {
        var who = caller.isEmpty() ? "A session" : "Session " + caller;
        var what = owned ? " wants to close " + d.name() : " asks to close " + d.name() + ", which it did not open";
        return who + what + (unsaved ? ".\n\nIt has changes you have not saved." : ".") + "\n\n" + d.path;
    }

    static String finish(MainWindow w, Doc d, CloseAnswer r) {
        switch (r) {
            case CANCEL -> { return reply(Exit.REFUSED, "the user kept " + d.name() + " open\n"); }
            case TIMEOUT -> { return reply(Exit.NO_ANSWER, "the user did not answer in time, " + d.name() + " is still open\n"); }
            case SAVE -> {
                Prompts.save(w, d);
                if (d.dirty.get()) return reply(Exit.REFUSED, d.name() + " was not saved, so it is still open\n");
            }
            default -> {}
        }
        close(w, d);
        return reply(Exit.DONE, "closed " + d.path + "\n");
    }

    static void close(MainWindow w, Doc d) {
        if (w.tabs.contains(d)) w.tabs.remove(d); else Docs.close(d);
    }

    private AgentClose() {}
}
