package io.github.prosemirrorfx.notepadfx;

import io.github.prosemirrorfx.notepadfx.toc.Outline;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OutlineTest {
    @Test
    void headingsBulletsAndParagraphsNest() {
        var text = String.join("\n",
            "# Title", "", "Intro paragraph", "continues here", "",
            "## Part", "- first", "\t- nested **bold**", "- second", "",
            "```", "# not a heading", "```", "", "# Next");
        var roots = Outline.parse(text);
        assertEquals(2, roots.size());
        var title = roots.get(0);
        assertEquals("Title", title.label());
        assertEquals("Intro paragraph continues here", title.children().get(0).label());
        var part = title.children().get(1);
        assertEquals("Part", part.label());
        assertEquals(2, part.children().size());
        assertEquals("nested bold", part.children().get(0).children().get(0).label());
        assertEquals("Next", roots.get(1).label());
        assertEquals(14, roots.get(1).line());
    }

    @Test
    void tabIndentedLogseqStyleBullets() {
        var roots = Outline.parse("- a\n\t- b\n\t\t- c\n- d");
        assertEquals(2, roots.size());
        assertEquals("c", roots.get(0).children().get(0).children().get(0).label());
    }
}
