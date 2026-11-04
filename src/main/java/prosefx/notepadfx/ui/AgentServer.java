package prosefx.notepadfx.ui;

import prosefx.notepadfx.agent.*;
import prosefx.notepadfx.doc.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import javafx.application.Platform;

/** The running notepadfx's side of the command-line verbs: runs on the JavaFX thread, answers "code, newline, text". */
public final class AgentServer {

    public static CompletableFuture<String> request(List<String> raw) {
        var answer = new CompletableFuture<String>();
        Platform.runLater(() -> {
            try { handle(raw, answer); } catch (Exception e) { answer.complete(reply(Exit.ERROR, "notepadfx: " + e + "\n")); }
        });
        return answer;
    }

    static void handle(List<String> raw, CompletableFuture<String> answer) {
        var a = Args.parse(raw.subList(1, raw.size()));
        var verb = raw.getFirst();
        if (verb.startsWith("test-") && AgentTestHook.on()) { answer.complete(AgentTestHook.run(verb, a)); return; }
        if (verb.equals("files")) { answer.complete(AgentFacts.files(a)); return; }
        if (verb.equals("open")) { answer.complete(AgentOpen.verb(a)); return; }
        if (a.word(0) == null) { answer.complete(reply(Exit.ERROR, "notepadfx " + verb + " needs a file\n")); return; }
        var found = Docs.find(Path.of(a.word(0)));
        if (found.isEmpty()) { answer.complete(AgentFacts.notOpen(a)); return; }
        var d = found.get();
        switch (verb) {
            case "status" -> answer.complete(AgentFacts.status(d, a));
            case "read" -> answer.complete(AgentFacts.read(d, a));
            case "close" -> AgentClose.run(d, a, answer);
            default -> answer.complete(AgentEdit.run(verb, d, a));
        }
    }

    static String reply(int code, String text) { return code + "\n" + text; }

    private AgentServer() {}
}
