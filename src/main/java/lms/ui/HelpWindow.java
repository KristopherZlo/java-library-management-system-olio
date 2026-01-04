package lms.ui;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.TextBox;
import com.googlecode.lanterna.gui2.Window;

public class HelpWindow extends BasicWindow {
    public HelpWindow() {
        super("Help");
        Panel panel = new Panel(new LinearLayout(Direction.VERTICAL));
        TextBox textBox = new TextBox(new TerminalSize(90, 22), TextBox.Style.MULTI_LINE);
        textBox.setReadOnly(true);
        textBox.setText(buildHelpText());
        textBox.setCaretPosition(0, 0);
        panel.addComponent(textBox);
        panel.addComponent(new Button("Close", this::close));
        setComponent(panel);
        setHints(java.util.Arrays.asList(Window.Hint.CENTERED));
    }

    private String buildHelpText() {
        return String.join("\n",
                "Library Management System - Help",
                "",
                "Navigation:",
                "- Tab / Shift+Tab: move focus between controls",
                "- Arrow keys: navigate tables and lists",
                "- Enter: activate buttons or select items in tables",
                "- F1: open this help window",
                "- Use Close/Exit buttons to leave screens",
                "- Selected items are marked with * in tables",
                "",
                "Book identifiers:",
                "- Most actions let you choose books and members from a list.",
                "- When adding a book, enter the ISBN (Book ID is optional).",
                "- Search uses 2- and 3-gram fuzzy matching and ranks closest matches first.",
                "- Short queries rely on 2-grams; if no close results, top suggestions are shown.",
                "",
                "Main screens and actions:",
                "Books:",
                "- Add: create a book (Book ID optional)",
                "- Edit: update book details",
                "- Add copies: add copies by ISBN/Book ID",
                "- View copies: list copies by ISBN/Book ID",
                "- Remove: remove a book by ISBN/Book ID",
                "- Search: title, author, genre, ISBN, or Book ID",
                "- Available column shows how many copies can be loaned",
                "",
                "Members:",
                "- Add, Edit, Remove, Select, Actions, Search",
                "- Actions lets you loan or reserve a book for a selected member",
                "- Search uses 2- and 3-gram fuzzy matching",
                "",
                "Loans:",
                "- Loan: choose member and book from a list",
                "- Loan: set a custom due date if needed",
                "- Change loan date or due date for a selected loan",
                "- Return selected or return by Copy ID",
                "- Overdue only filter",
                "",
                "Reservations:",
                "- Add reservation by choosing member and book",
                "- Edit member or status on a reservation",
                "- Remove a reservation (Select + Remove)",
                "",
                "Reports:",
                "- Overdue report, Member report (choose member), Popular books, Export CSV",
                "",
                "Settings:",
                "- Switch storage mode (restart required)"
        );
    }
}