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
import java.util.List;
import java.util.Optional;
import lms.model.BookCopy;

public class CopiesWindow extends BasicWindow {
    private String resolvedIsbn;
    private final Table<String> table;
    private String selectedCopyId;

    public CopiesWindow(UiContext context, String bookKey) {
        super("Copies");
        this.context = context;
        this.bookKey = bookKey;
        this.table = new Table<>("Selected", "Copy ID", "Status");
        table.setCellSelection(false);
        table.setSelectAction(this::selectCopyFromTable);

        Panel panel = new Panel(new LinearLayout(Direction.VERTICAL));
        panel.addComponent(new Label("Use arrows and Enter/Select to choose a copy."));
        panel.addComponent(table);
        Panel actions = new Panel(new LinearLayout(Direction.HORIZONTAL));
        actions.addComponent(new Button("Copy ID", this::copySelectedCopyId));
        actions.addComponent(new Button("Select", this::selectCopyFromTable));
        actions.addComponent(new Button("Close", this::close));
        panel.addComponent(actions);
        setComponent(panel);
        setHints(java.util.Arrays.asList(Window.Hint.CENTERED));

        try {
            lms.model.Book book = context.getLibraryService().getBookByKey(bookKey);
            this.resolvedIsbn = book.getIsbn();
            String bookId = book.getBookId() == null ? "-" : book.getBookId();
            setTitle("Copies for " + bookId + " / " + book.getIsbn());
            refresh();
        } catch (RuntimeException ex) {
            DialogUtils.showError(context.getGui(), "Error", ex.getMessage());
            close();
        }
    }

    private void refresh() {
        if (resolvedIsbn == null) {
            return;
        }
        List<BookCopy> copies = context.getLibraryService().getCopiesByIsbn(resolvedIsbn);
        table.getTableModel().clear();
        int selectedRowIndex = -1;
        boolean selectedFound = false;
        for (int i = 0; i < copies.size(); i++) {
            BookCopy copy = copies.get(i);
            boolean isSelected = copy.getCopyId().equals(selectedCopyId);
            if (isSelected) {
                selectedFound = true;
                selectedRowIndex = i;
            }
            table.getTableModel().addRow(
                    isSelected ? "*" : "",
                    copy.getCopyId(),
                    copy.getStatus().name()
            );
        }
        if (!selectedFound) {
            selectedCopyId = null;
        }
        if (selectedRowIndex >= 0) {
            table.setSelectedRow(selectedRowIndex);
        }
    }

    private Optional<String> selectedCopyIdFromRow() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= table.getTableModel().getRowCount()) {
            return Optional.empty();
        }
        return Optional.ofNullable(table.getTableModel().getRow(row).get(1));
    }

    private void selectCopyFromTable() {
        Optional<String> selected = selectedCopyIdFromRow();
        if (selected.isEmpty()) {
            DialogUtils.showInfo(context.getGui(), "Select copy", "No copy selected.");
            return;
        }
        setSelectedCopyId(selected.get(), true);
    }

    private void copySelectedCopyId() {
        Optional<String> copyIdOpt = selectedCopyIdFromRow();
        if (copyIdOpt.isEmpty()) {
            DialogUtils.showInfo(context.getGui(), "Copy ID", "No copy selected.");
            return;
        }
        setSelectedCopyId(copyIdOpt.get(), false);
        boolean copied = DialogUtils.copyToClipboard(copyIdOpt.get());
        if (copied) {
            DialogUtils.showInfo(context.getGui(), "Copy ID", "Copied to clipboard: " + copyIdOpt.get());
        } else {
            DialogUtils.showInfo(context.getGui(), "Copy ID", "Copy ID: " + copyIdOpt.get());
        }
    }

    private void setSelectedCopyId(String copyId, boolean refresh) {
        selectedCopyId = copyId;
        if (refresh) {
            refresh();
        }
    }
}