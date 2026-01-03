package lms;
// TODO: add cli flags
// TODO: add shutdown hook

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import java.io.IOException;
import lms.policy.LoanPolicyResolver;
import lms.policy.PerDayFinePolicy;
import lms.report.CsvExporter;
import lms.report.ReportService;
import lms.service.LibraryService;
import lms.storage.LibraryStorage;
import lms.storage.StorageFactory;
import lms.ui.HelpWindow;
import lms.ui.MainMenuWindow;
import lms.ui.UiContext;
import lms.util.AppConfig;
import lms.util.DateProvider;
import lms.util.DemoDataSeeder;
import lms.util.LoggerConfig;
import lms.util.SystemDateProvider;

public class Main {
    public static void main(String[] args) {

        LibraryStorage storage = StorageFactory.create(config);
        DateProvider dateProvider = new SystemDateProvider();
        LibraryService libraryService = new LibraryService(
                storage,
                new LoanPolicyResolver(),
                new PerDayFinePolicy(50),
                dateProvider
        );
        if (config.isDemoEnabled()) {
            DemoDataSeeder.seedIfEmpty(libraryService);
        }
        ReportService reportService = new ReportService(storage, libraryService, dateProvider);
        CsvExporter csvExporter = new CsvExporter();

        DefaultTerminalFactory terminalFactory = new DefaultTerminalFactory()
                .setInitialTerminalSize(new TerminalSize(110, 35));
        Screen screen = null;
        try {
            Terminal terminal;
            try {
                terminal = terminalFactory.createTerminal();
            } catch (IOException ex) {
                terminalFactory.setPreferTerminalEmulator(true)
                        .setAutoOpenTerminalEmulatorWindow(true)
                        .setTerminalEmulatorTitle("Library Management System");
                terminal = terminalFactory.createTerminalEmulator();
            }
            screen = new TerminalScreen(terminal);
            screen.startScreen();
            MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
            gui.addListener((textGUI, keyStroke) -> {
                if (keyStroke != null && keyStroke.getKeyType() == KeyType.F1) {
                    gui.addWindowAndWait(new HelpWindow());
                    return true;
                }
                return false;
            });

            UiContext context = new UiContext(gui, libraryService, reportService, csvExporter, config);
            gui.addWindowAndWait(new MainMenuWindow(context));
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (screen != null) {
                try {
                    screen.stopScreen();
                } catch (IOException ignored) {
                    // Ignore shutdown errors.
                }
            }
            storage.close();
        }
    }
}