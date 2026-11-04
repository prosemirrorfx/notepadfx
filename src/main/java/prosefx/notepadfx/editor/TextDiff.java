package prosefx.notepadfx.editor;

/** The one stretch that differs between two texts: old[from, oldEnd) became new[from, newEnd). */
final class TextDiff {
    final int from, oldEnd, newEnd;

    TextDiff(String old, String text) {
        var max = Math.min(old.length(), text.length());
        var p = 0;
        while (p < max && old.charAt(p) == text.charAt(p)) p++;
        var s = 0;
        while (s < max - p && old.charAt(old.length() - 1 - s) == text.charAt(text.length() - 1 - s)) s++;
        from = p;
        oldEnd = old.length() - s;
        newEnd = text.length() - s;
    }

    /** Where a caret that was at pos goes: stays when the change is after it or starts at it, shifts when the change is before it, to the start of the change when inside it. */
    int map(int pos) { return pos <= from ? pos : pos >= oldEnd ? pos + newEnd - oldEnd : from; }
}
