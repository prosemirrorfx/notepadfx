package prosemirrorfx.notepadfx.editor;

import org.junit.jupiter.api.Test;
import prosemirrorfx.markdown.Lossless;

import static org.junit.jupiter.api.Assertions.*;

class ProseOutlineTest {
    @Test
    void headingsBulletsAndParagraphsFromTheDocument() {
        var doc = Lossless.read(String.join("\n",
            "# Title", "", "Intro paragraph", "", "## Part", "", "- first", "  - nested", "- second", "", "# Next", ""));
        var roots = ProseOutline.of(doc);
        assertEquals(2, roots.size());
        var title = roots.get(0);
        assertEquals("Title", title.label());
        assertEquals("Intro paragraph", title.children().get(0).label());
        var part = title.children().get(1);
        assertEquals("Part", part.label());
        assertEquals(2, part.children().size());
        assertEquals("first", part.children().get(0).label());
        assertEquals("nested", part.children().get(0).children().get(0).label());
        assertEquals("Next", roots.get(1).label());
        assertTrue(roots.get(1).line() > part.children().get(1).line());
    }
}
