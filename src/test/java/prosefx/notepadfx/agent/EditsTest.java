package prosefx.notepadfx.agent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EditsTest {
    static final String DOC = "# Questions\n\n>> one\n\ntext\n";

    @Test void appendGoesAtTheEndAndSaysWhere() {
        var c = Edits.append(DOC, "\n## Two\n");
        assertTrue(c.ok());
        assertEquals(DOC + "\n## Two\n", c.text());
        assertEquals(6, c.firstLine());
        assertEquals(2, c.lines());
    }

    @Test void appendToATextWithoutAFinalNewlineStartsANewLine() {
        var c = Edits.append("a", "b");
        assertEquals("a\nb", c.text());
        assertEquals(2, c.firstLine());
    }

    @Test void appendKeepsAFinalNewline() { assertEquals("a\nb\n", Edits.append("a\n", "b").text()); }

    @Test void insertAfterALineThatStillReadsAsExpected() {
        var c = Edits.insertAfterLine(DOC, 3, ">> one", "answer");
        assertEquals("# Questions\n\n>> one\nanswer\n\ntext\n", c.text());
        assertEquals(4, c.firstLine());
    }

    @Test void insertAfterALineThatMovedIsStaleAndTouchesNothing() {
        var c = Edits.insertAfterLine(DOC, 3, ">> something else", "x");
        assertEquals(Exit.STALE, c.code());
        assertNull(c.text());
        assertTrue(c.problem().contains(">> one"));
    }

    @Test void insertAtTheStartNeedsNoExpectation() { assertEquals("new\n" + DOC, Edits.insertAfterLine(DOC, 0, "", "new").text()); }

    @Test void insertAfterAHeadingFindsItWithOrWithoutTheMarks() {
        assertEquals("# Questions\nx\n\n>> one\n\ntext\n", Edits.insertAfterHeading(DOC, "# Questions", "x").text());
        assertEquals("# Questions\nx\n\n>> one\n\ntext\n", Edits.insertAfterHeading(DOC, "Questions", "x").text());
    }

    @Test void aMissingOrRepeatedHeadingIsStale() {
        assertEquals(Exit.STALE, Edits.insertAfterHeading(DOC, "# Gone", "x").code());
        assertEquals(Exit.STALE, Edits.insertAfterHeading("# A\n# A\n", "# A", "x").code());
    }

    @Test void replaceOnlyWhatTheWriterSaw() {
        var c = Edits.replace(DOC, 3, 3, ">> one", ">> one, edited");
        assertEquals("# Questions\n\n>> one, edited\n\ntext\n", c.text());
        assertEquals(Exit.STALE, Edits.replace(DOC, 3, 3, ">> two", "y").code());
        assertEquals(Exit.STALE, Edits.replace(DOC, 5, 9, "text", "y").code());
    }

    @Test void replaceWithNothingDeletesTheLines() { assertEquals("# Questions\n\ntext\n", Edits.replace(DOC, 3, 4, ">> one\n", "").text()); }
}
