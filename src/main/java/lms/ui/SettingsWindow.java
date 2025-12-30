package lms.ui;
// TODO: polish layout
// TODO: add keyboard shortcuts

import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.Window;
import lms.storage.StorageMode;
import lms.util.AppConfigWriter;

public class SettingsWindow extends BasicWindow {

    public SettingsWindow(UiContext context) {
        super("Settings");
        this.context = context;
        this.statusLabel = new Label(currentModeText());

        Panel panel = new Panel(new LinearLayout(Direction.VERTICAL));
        panel.addComponent(new Label("Choose storage mode (restart required)."));
        panel.addComponent(statusLabel);
        panel.addComponent(new Button("Use SQLite", () -> switchMode(StorageMode.SQLITE)));
        panel.addComponent(new Button("Use File", () -> switchMode(StorageMode.FILE)));
        panel.addComponent(new Button("Close", this::close));
        setComponent(panel);
        setHints(java.util.Arrays.asList(Window.Hint.CENTERED));
    }

    private void switchMode(StorageMode mode) {
        try {
            AppConfigWriter.updateStorageMode(mode);
            statusLabel.setText("Current storage: " + mode + " (restart required)");
            DialogUtils.showInfo(context.getGui(), "Settings", "Storage mode updated. Restart to apply.");
        } catch (RuntimeException ex) {
            DialogUtils.showError(context.getGui(), "Settings error", ex.getMessage());
        }
    }

    private String currentModeText() {
        return "Current storage: " + context.getConfig().getStorageMode() + " (restart required)";
    }
}