package prosemirrorfx.notepadfx.ui;

import prosemirrorfx.notepadfx.sys.*;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

final class SettingsDialog {
    static void show(MainWindow w) {
        var s = Windows.session;
        var theme = new ChoiceBox<String>();
        theme.getItems().addAll("dark", "light");
        theme.setValue(s.theme());
        var zoom = new Spinner<Integer>(50, 300, (int) Math.round(s.zoom() * 100), 10);
        zoom.setEditable(true);
        var lines = check("Show line numbers in new markdown editors", s.lineNumbers());
        var bullets = check("Outline shows bullets", s.outlineBullets());
        var paras = check("Outline shows paragraphs", s.outlineParagraphs());
        var nav = check("Files panes visible in this window (F7)", w.state.navVisible());
        var toc = check("Outline visible in this window (F8)", w.state.tocVisible());
        var menuWas = ExplorerMenu.present();
        var menu = check("\"Open with notepadfx\" in Explorer's right-click menu", menuWas);

        var grid = new GridPane(12, 10);
        grid.setPadding(new Insets(16));
        grid.addRow(0, new Label("Theme"), theme);
        grid.addRow(1, new Label("App zoom (%)"), zoom);
        grid.add(new Label("A window can override the app zoom with the - and + buttons in its toolbar."), 0, 2, 2, 1);
        grid.add(lines, 0, 3, 2, 1);
        grid.add(bullets, 0, 4, 2, 1);
        grid.add(paras, 0, 5, 2, 1);
        grid.add(nav, 0, 6, 2, 1);
        grid.add(toc, 0, 7, 2, 1);
        grid.add(menu, 0, 9, 2, 1);
        var check = new Button("Check for updates");
        check.setMnemonicParsing(false);
        check.setOnAction(e -> Updates.checkNow());
        grid.addRow(8, new Label("Version " + Updates.version()), check);

        var d = new Dialog<ButtonType>();
        d.initOwner(w.stage);
        d.setTitle("notepadfx settings");
        d.getDialogPane().setContent(grid);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        d.getDialogPane().getStylesheets().add(Theme.css());
        d.getDialogPane().getStyleClass().addAll("np-root", Windows.theme());
        if (d.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        var themeChanged = !theme.getValue().equals(s.theme());
        s.zoom(zoom.getValue() / 100.0).lineNumbers(lines.isSelected())
            .outlineBullets(bullets.isSelected()).outlineParagraphs(paras.isSelected());
        if (themeChanged) Windows.toggleTheme();
        if (menu.isSelected() != menuWas) { if (menu.isSelected()) ExplorerMenu.add(InstallPath.installedOrRunning()); else ExplorerMenu.remove(); }
        if (nav.isSelected() != w.state.navVisible()) w.toggleNav();
        if (toc.isSelected() != w.state.tocVisible()) w.toggleToc();
        Windows.ALL.forEach(x -> { x.area.reapply(); x.toc.rebuild(); });
        Windows.persist();
    }

    static CheckBox check(String text, Boolean value) {
        var c = new CheckBox(text);
        c.setMnemonicParsing(false);
        c.setSelected(Boolean.TRUE.equals(value));
        return c;
    }

    private SettingsDialog() {}
}
