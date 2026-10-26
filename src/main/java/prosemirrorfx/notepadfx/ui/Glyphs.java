package prosemirrorfx.notepadfx.ui;

import java.io.*;
import java.util.*;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.shape.SVGPath;

import static java.nio.charset.StandardCharsets.UTF_8;

final class Glyphs {
    static Map<String, String> paths;

    static Node of(String name) {
        if (paths == null) paths = load();
        var p = new SVGPath();
        p.setContent(paths.getOrDefault(name, ""));
        p.getStyleClass().add("np-glyph");
        p.setScaleX(0.75);
        p.setScaleY(0.75);
        return new Group(p);
    }

    static Map<String, String> load() {
        var map = new HashMap<String, String>();
        try (var in = new BufferedReader(new InputStreamReader(Glyphs.class.getResourceAsStream("/prosemirrorfx/notepadfx/icons.tsv"), UTF_8))) {
            in.lines().filter(l -> !l.startsWith("#") && l.contains("\t")).forEach(l -> {
                var tab = l.indexOf('\t');
                map.put(l.substring(0, tab), l.substring(tab + 1));
            });
        } catch (IOException e) {
            System.err.println("notepadfx: icons: " + e);
        }
        return map;
    }

    private Glyphs() {}
}
