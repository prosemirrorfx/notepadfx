package prosemirrorfx.notepadfx.editor;

import java.util.List;
import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import prosemirrorfx.notepadfx.toc.OutlineNode;
import prosemirrorfx.view.*;
import prosemirrorfx.view.embed.MarkdownEditor;

public final class ProseEditor implements Editor {
    static final Theme
        DARK = Theme.DARK.with("bg", "#1e1e1e").with("text", "#dcddde").with("code", "#c4b5fd").with("link", "#5eead4").with("active-line", "#262b30").with("caret", "#2dd4bf"),
        LIGHT = Theme.LIGHT.with("text", "#2e3338").with("code", "#5b21b6").with("link", "#0f766e").with("caret", "#0d9488");
    final MarkdownEditor md = new MarkdownEditor();
    final IntegerProperty caret = new SimpleIntegerProperty();
    Node toolbar;

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
    @Override public int position() { return md.caret(); }
    @Override public void position(int pos) { md.setCaret(pos); }
    @Override public String where() { return ""; }
    @Override public void cut() { md.cut(); md.focus(); }
    @Override public void copy() { md.copy(); md.focus(); }
    @Override public void paste(boolean plain) { if (plain) md.pastePlain(); else md.paste(); md.focus(); }
    @Override public void copyAll(Copy format) { md.view().copyAll(CopyFormat.valueOf(format.name())); }
    @Override public void zoom(double zoom) { if (md.view().zoom() != zoom) md.view().setZoom(zoom); }
    @Override public ObservableValue<Number> zoomChanges() { return md.view().zoomProperty(); }
    @Override public boolean lineNumbersSupported() { return true; }
    @Override public void lineNumbers(boolean on) { md.view().setLineNumbers(on); }
    @Override public void theme(boolean dark) { md.view().setTheme(dark ? DARK : LIGHT); }
    @Override public void focus() { md.focus(); }

    @Override
    public Node toolbar() {
        if (toolbar == null) toolbar = md.toolbar();
        return toolbar;
    }

    @Override
    public void goTo(int pos) {
        md.setCaret(pos);
        md.focus();
    }
}
