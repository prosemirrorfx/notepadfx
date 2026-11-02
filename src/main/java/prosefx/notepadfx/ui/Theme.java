package prosefx.notepadfx.ui;

final class Theme {
    static String url;

    static String css() {
        if (url == null) url = Theme.class.getResource("/prosefx/notepadfx/notepadfx.css").toExternalForm();
        return url;
    }

    private Theme() {}
}
