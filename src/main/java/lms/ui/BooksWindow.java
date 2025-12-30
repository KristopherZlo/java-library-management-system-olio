package lms.ui;
// TODO: polish layout
// TODO: add keyboard shortcuts

import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.table.Table;
import com.googlecode.lanterna.gui2.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lms.model.Book;
import lms.model.CopyStatus;

public class BooksWindow extends BasicWindow {
    private String selectedIsbn;

    public BooksWindow(UiContext context) {
        super("Books");
        this.context = context;
        this.table = new Table<>("Selected", "Book ID", "ISBN", "Title", "Author", "Year", "Genre", "Loans", "Available");
        table.setCellSelection(false);
        table.setSelectAction(this::selectBookFromTable);

        Panel main = new Panel(new LinearLayout(Direction.VERTICAL));
        main.addComponent(new Label("Use arrows and Enter/Select to choose a book. Selected is marked with *."));
        main.addComponent(table);
        main.addComponent(buildActions());
        setComponent(main);
        setHints(java.util.Arrays.asList(Window.Hint.CENTERED, Window.Hint.EXPANDED));

        refreshTable(context.getLibraryService().listBooks());
    }

    private Panel buildActions() {
        Panel actions = new Panel(new LinearLayout(Direction.HORIZONTAL));
        actions.addComponent(new Button("Add", this::addBook));
        actions.addComponent(new Button("Edit", this::editBook));
        actions.addComponent(new Button("Add copies", this::addCopies));
        actions.addComponent(new Button("View copies", this::viewCopies));
        actions.addComponent(new Button("Remove", this::removeBook));
        actions.addComponent(new Button("Select", this::selectBookFromTable));
        actions.addComponent(new Button("Search", this::searchBooks));
        actions.addComponent(new Button("Refresh", this::refresh));
        actions.addComponent(new Button("Close", this::close));
        return actions;
    }

    private void refresh() {
        refreshTable(context.getLibraryService().listBooks());
    }

    private void addBook() {
        Optional<String> isbnOpt = DialogUtils.promptText(context.getGui(), "Add Book", "ISBN", "");
        if (isbnOpt.isEmpty()) {
            return;
        }
        Optional<String> bookIdOpt = DialogUtils.promptText(context.getGui(), "Add Book", "Book ID (optional)", "");
        Optional<String> titleOpt = DialogUtils.promptText(context.getGui(), "Add Book", "Title", "");
        if (titleOpt.isEmpty()) {
            return;
        }
        Optional<String> authorOpt = DialogUtils.promptText(context.getGui(), "Add Book", "Author", "");
        if (authorOpt.isEmpty()) {
            return;
        }
        Optional<Integer> yearOpt = DialogUtils.promptInt(context.getGui(), "Add Book", "Year", 2024);
        if (yearOpt.isEmpty()) {
            return;
        }
        Optional<String> genreOpt = DialogUtils.promptText(context.getGui(), "Add Book", "Genre", "");
        if (genreOpt.isEmpty()) {
            return;
        }
        Optional<Integer> copiesOpt = DialogUtils.promptInt(context.getGui(), "Add Book", "Copies count", 1);
        if (copiesOpt.isEmpty()) {
            return;
        }
        try {
            Book book = new Book(isbnOpt.get(), titleOpt.get(), authorOpt.get(), yearOpt.get(), genreOpt.get());
            if (bookIdOpt.isPresent() && !bookIdOpt.get().trim().isEmpty()) {
                book.setBookId(bookIdOpt.get().trim());
            }
            context.getLibraryService().addBook(book, copiesOpt.get());
            refresh();
        } catch (RuntimeException ex) {
            DialogUtils.showError(context.getGui(), "Error", ex.getMessage());
        }
    }

    private void editBook() {
        Optional<String> isbnOpt = resolveBookSelection("Edit book");
        if (isbnOpt.isEmpty()) {
            return;
        }
        try {
            Book book = context.getLibraryService().getBookByKey(isbnOpt.get());
            String currentBookId = book.getBookId() == null ? "" : book.getBookId();
            Optional<String> bookIdOpt = DialogUtils.promptText(context.getGui(), "Edit Book",
                    "Book ID (leave empty to clear)", currentBookId);
            if (bookIdOpt.isEmpty()) {
                return;
            }
            Optional<String> titleOpt = DialogUtils.promptText(context.getGui(), "Edit Book", "Title", book.getTitle());
            if (titleOpt.isEmpty()) {
                return;
            }
            Optional<String> authorOpt = DialogUtils.promptText(context.getGui(), "Edit Book", "Author", book.getAuthor());
            if (authorOpt.isEmpty()) {
                return;
            }
            Optional<Integer> yearOpt = DialogUtils.promptInt(context.getGui(), "Edit Book", "Year", book.getYear());
            if (yearOpt.isEmpty()) {
                return;
            }
            Optional<String> genreOpt = DialogUtils.promptText(context.getGui(), "Edit Book", "Genre", book.getGenre());
            if (genreOpt.isEmpty()) {
                return;
            }
            context.getLibraryService().updateBook(book.getIsbn(), bookIdOpt.get(), titleOpt.get(),
                    authorOpt.get(), yearOpt.get(), genreOpt.get());
            refresh();
        } catch (RuntimeException ex) {
            DialogUtils.showError(context.getGui(), "Error", ex.getMessage());
        }
    }

    private void addCopies() {
        Optional<String> isbnOpt = resolveBookSelection("Add copies");
        if (isbnOpt.isEmpty()) {
            return;
        }
        Optional<Integer> countOpt = DialogUtils.promptInt(context.getGui(), "Add Copies", "Copies count", 1);
        if (countOpt.isEmpty()) {
            return;
        }
        try {
            for (int i = 0; i < countOpt.get(); i++) {
                context.getLibraryService().addCopy(isbnOpt.get());
            }
            refresh();
        } catch (RuntimeException ex) {
            DialogUtils.showError(context.getGui(), "Error", ex.getMessage());
        }
    }

    private void viewCopies() {
        Optional<String> isbnOpt = resolveBookSelection("View copies");
        if (isbnOpt.isEmpty()) {
            return;
        }
        context.getGui().addWindowAndWait(new CopiesWindow(context, isbnOpt.get()));
    }

    private void removeBook() {
        Optional<String> isbnOpt = resolveBookSelection("Remove book");
        if (isbnOpt.isEmpty()) {
            return;
        }
        try {
            if (DialogUtils.confirm(context.getGui(), "Remove Book",
                    "Remove book " + isbnOpt.get() + "? This cannot be undone.")) {
                context.getLibraryService().removeBook(isbnOpt.get());
                refresh();
            }
        } catch (RuntimeException ex) {
            DialogUtils.showError(context.getGui(), "Error", ex.getMessage());
        }
    }

    private void searchBooks() {
        Optional<String> queryOpt = DialogUtils.promptText(context.getGui(), "Search Books", "Query", "");
        if (queryOpt.isEmpty()) {
            return;
        }
        try {
            refreshTable(context.getLibraryService().searchBooks(queryOpt.get()));
        } catch (RuntimeException ex) {
            DialogUtils.showError(context.getGui(), "Error", ex.getMessage());
        }
    }

    private void refreshTable(List<Book> books) {
        table.getTableModel().clear();
        int selectedRowIndex = -1;
        boolean selectedFound = false;
        for (int i = 0; i < books.size(); i++) {
            Book book = books.get(i);
            boolean isSelected = book.getIsbn().equals(selectedIsbn);
            if (isSelected) {
                selectedFound = true;
                selectedRowIndex = i;
            }
            String bookId = book.getBookId() == null ? "-" : book.getBookId();
            table.getTableModel().addRow(
                    isSelected ? "*" : "",
                    bookId,
                    book.getIsbn(),
                    book.getTitle(),
                    book.getAuthor(),
                    String.valueOf(book.getYear()),
                    book.getGenre(),
                    String.valueOf(book.getTotalLoans()),
                    String.valueOf(context.getLibraryService().getCopiesByIsbn(book.getIsbn()).stream()
                            .filter(copy -> copy.getStatus() == CopyStatus.AVAILABLE)
                            .count())
            );
        }
        if (!selectedFound) {
            selectedIsbn = null;
        }
        if (selectedRowIndex >= 0) {
            table.setSelectedRow(selectedRowIndex);
        }
    }

    private Optional<String> selectedBookIsbnFromRow() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= table.getTableModel().getRowCount()) {
            return Optional.empty();
        }
        return Optional.ofNullable(table.getTableModel().getRow(row).get(2));
    }

    private void selectBookFromTable() {
        Optional<String> selected = selectedBookIsbnFromRow();
        if (selected.isEmpty()) {
            DialogUtils.showInfo(context.getGui(), "Select book", "No book selected.");
            return;
        }
        setSelectedIsbn(selected.get(), true);
    }

    private void setSelectedIsbn(String isbn, boolean refresh) {
        selectedIsbn = isbn;
        if (refresh) {
            refresh();
        }
    }

    private Optional<String> resolveBookSelection(String title) {
        if (selectedIsbn != null) {
            return Optional.of(selectedIsbn);
        }
        Optional<String> selectedRow = selectedBookIsbnFromRow();
        if (selectedRow.isPresent()) {
            setSelectedIsbn(selectedRow.get(), true);
            return selectedRow;
        }
        List<Book> books = context.getLibraryService().listBooks();
        if (books.isEmpty()) {
            DialogUtils.showInfo(context.getGui(), title, "No books available.");
            return Optional.empty();
        }
        List<DialogUtils.Choice<String>> choices = new ArrayList<>();
        for (Book book : books) {
            String bookId = book.getBookId() == null ? "-" : book.getBookId();
            String label = bookId + " | " + book.getIsbn() + " | " + book.getTitle() + " (" + book.getAuthor() + ")";
            choices.add(new DialogUtils.Choice<>(label, book.getIsbn()));
        }
        Optional<String> chosen = DialogUtils.promptChoice(context.getGui(), title, "Choose book", choices);
        chosen.ifPresent(isbn -> setSelectedIsbn(isbn, true));
        return chosen;
    }
}