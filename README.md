# notepadfx

A fast markdown and text editor for the desktop, in JavaFX.

- Workspaces, with a projects pane and a files pane, each with a regex filter.
- Vertical tabs that show the whole file name, wrapped.
- An outline of the open file (headings, bullets and paragraphs) that wraps long entries and follows the caret.
- Several windows in one process. Launching notepadfx again opens a new window in the running process; a tab can be dragged out into its own window, or between windows.
- Keyboard first: every action is on a function key, and the legend along the bottom is clickable.
- Light and dark themes.

The editor area currently edits plain text. It will move to the rich editor of the prosemirrorfx modules once those are published.

## Keys

F2 save, F4 close tab, F5 refresh, F6 next pane, F7 files panes, F8 outline, F9 move tab to a new window, F10 new window, F11 theme, F12 open. Ctrl+Tab and Ctrl+PageUp/PageDown move between tabs.

## Build

Java 25 and Maven. `mvn verify` builds `target/notepadfx.jar` and `target/jr/notepadfx.exe` (a jarrunner launcher with the JVM in-process and an AOT cache).

State lives in `~/prosemirrorfx/notepadfx/session.json`.
