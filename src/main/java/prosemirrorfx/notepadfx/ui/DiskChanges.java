package prosemirrorfx.notepadfx.ui;

import prosemirrorfx.notepadfx.doc.*;

/**
 * What the window does when an open file is changed from outside. A file with nothing unsaved is reloaded where the caret
 * is, and the status line says so. A file with unsaved edits is NOT touched and no dialog is thrown up (a dialog would take
 * the keyboard from whatever the user is typing in): the status line says it, and the choice is made on purpose, by Reload
 * from disk in the tab's menu or by saving, which asks before it overwrites the newer file.
 */
final class DiskChanges {

    static void install() { DiskWatch.onChange(DiskChanges::check); }

    static void check(Doc d) {
        var w = Windows.windowOf(d);
        if (w == null) return;
        switch (d.syncFromDisk()) {
            case RELOADED -> w.status.flash(d.name() + " was changed on disk and has been reloaded");
            case CONFLICT -> w.status.flash(d.name() + " was changed on disk but has unsaved edits here: right-click its tab, Reload from disk, to take the new text; saving will ask first");
            case SAME -> {}
        }
    }

    private DiskChanges() {}
}
