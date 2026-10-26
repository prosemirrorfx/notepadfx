package io.github.prosemirrorfx.notepadfx.ui;

final class Theme {
    static String url;

    static String css() {
        if (url == null) url = Theme.class.getResource("/io/github/prosemirrorfx/notepadfx/notepadfx.css").toExternalForm();
        return url;
    }

    private Theme() {}
}
