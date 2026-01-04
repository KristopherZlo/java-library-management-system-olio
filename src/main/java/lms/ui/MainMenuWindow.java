package lms.ui;

import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.Window;

public class MainMenuWindow extends BasicWindow {
    public MainMenuWindow(UiContext context) {
        super("Library Management System");
        Panel panel = new Panel();
        panel.setLayoutManager(new LinearLayout(Direction.VERTICAL));

        panel.addComponent(new Label("Use arrows and Enter. Press F1 for help."));
        panel.addComponent(new Button("Books", () -> context.getGui().addWindowAndWait(new BooksWindow(context))));
        panel.addComponent(new Button("Members", () -> context.getGui().addWindowAndWait(new MembersWindow(context))));
        panel.addComponent(new Button("Loans", () -> context.getGui().addWindowAndWait(new LoansWindow(context))));
        panel.addComponent(new Button("Reservations", () -> context.getGui().addWindowAndWait(new ReservationsWindow(context))));
        panel.addComponent(new Button("Reports", () -> context.getGui().addWindowAndWait(new ReportsWindow(context))));
        panel.addComponent(new Button("Settings", () -> context.getGui().addWindowAndWait(new SettingsWindow(context))));
        panel.addComponent(new Button("Help", () -> context.getGui().addWindowAndWait(new HelpWindow())));
        panel.addComponent(new Button("Exit", this::close));

        setComponent(panel);
        setHints(java.util.Arrays.asList(Window.Hint.CENTERED));
    }
}