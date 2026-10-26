package prosemirrorfx.notepadfx.ui;

import java.nio.file.*;
import java.util.*;
import java.util.function.Predicate;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;

final class FileTreeItem extends TreeItem<Path> {
    static final Set<String> SKIP = Set.of(".git", "target", "node_modules", ".idea", ".gradle", "build", "out", ".claude");
    static final int MAX = 4000, DEPTH = 10;
    boolean loaded;
    final boolean leaf;

    FileTreeItem(Path p) {
        super(p);
        leaf = !Files.isDirectory(p);
    }

    @Override public boolean isLeaf() { return leaf; }

    @Override
    public ObservableList<TreeItem<Path>> getChildren() {
        if (!loaded) {
            loaded = true;
            super.getChildren().setAll(list(getValue()).stream().map(FileTreeItem::new).toList());
        }
        return super.getChildren();
    }

    static List<Path> list(Path dir) {
        try (var s = Files.list(dir)) {
            return s.filter(p -> !SKIP.contains(p.getFileName().toString()))
                .sorted(Comparator.comparing((Path p) -> !Files.isDirectory(p)).thenComparing(p -> p.getFileName().toString().toLowerCase()))
                .toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    static TreeItem<Path> filtered(Path root, Predicate<String> match) {
        var top = new TreeItem<>(root);
        var count = new int[1];
        add(top, root, match, 0, count);
        top.setExpanded(true);
        return top;
    }

    static boolean add(TreeItem<Path> parent, Path dir, Predicate<String> match, int depth, int[] count) {
        var any = false;
        for (var p : list(dir)) {
            if (count[0] >= MAX) break;
            if (Files.isDirectory(p)) {
                if (depth >= DEPTH) continue;
                var item = new TreeItem<>(p);
                var keep = match.test(p.getFileName().toString());
                if (add(item, p, match, depth + 1, count) || keep) {
                    item.setExpanded(true);
                    parent.getChildren().add(item);
                    any = true;
                }
            } else if (match.test(p.getFileName().toString())) {
                parent.getChildren().add(new TreeItem<>(p));
                count[0]++;
                any = true;
            }
        }
        return any;
    }
}
