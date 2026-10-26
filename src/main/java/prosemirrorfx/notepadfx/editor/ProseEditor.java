package prosemirrorfx.notepadfx.editor;

import java.util.List;
import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import prosemirrorfx.notepadfx.toc.OutlineNode;
import prosemirrorfx.view.embed.MarkdownEditor;

public final class ProseEditor implements Editor {
    final MarkdownEditor md = new MarkdownEditor();
    final IntegerProperty caret = new SimpleIntegerProperty();

    public ProseEditor() {
        md.node().getStyleClass().add("np-prose");
        md.view().model().onChange((old, now) -> caret.set(now.getSelection().getHead()));
    }

    @Override public Node node() { return md.node(); }
    @Override public String text() { return md.getMarkdown(); }
    @Override public void load(String text) { md.setMarkdown(text); caret.set(md.caret()); }
    @Override public void markSaved() { md.markSaved(); }
    @Override public ReadOnlyBooleanProperty dirty() { return md.dirtyProperty(); }
    @Override public void onChange(Runnable listener) { md.onChange(listener); }
    @Override public List<OutlineNode> outline() { return ProseOutline.of(md.view().model().state().getDoc()); }
    @Override public ObservableValue<Number> caret() { return caret; }
    @Override public String where() { return ""; }
    @Override public void fontSize(double size) { md.view().setZoom(size / 15.0); }
    @Override public void focus() { md.focus(); }

    @Override
    public void goTo(int pos) {
        md.setCaret(pos);
        md.focus();
    }
}
