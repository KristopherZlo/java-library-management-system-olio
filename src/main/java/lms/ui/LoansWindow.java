package lms.ui;

import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.CheckBox;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.table.Table;
import com.googlecode.lanterna.gui2.Window;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lms.model.Book;
import lms.model.Loan;
import lms.model.Member;
import lms.service.ReturnResult;

public class LoansWindow extends BasicWindow {
    private final UiContext context;
    private final Table<String> table;
    private final CheckBox overdueOnly;
    private String selectedLoanId;
    private String selectedCopyId;

    public LoansWindow(UiContext context) {
        super("Loans");
        this.context = context;
        this.table = new Table<>("Selected", "Loan ID", "Copy ID", "Member ID", "Loan Date", "Due Date", "Overdue");
        this.overdueOnly = new CheckBox("Overdue only");
        table.setCellSelection(false);
        table.setSelectAction(this::selectLoanFromTable);

        Panel main = new Panel(new LinearLayout(Direction.VERTICAL));
        main.addComponent(new Label("Use arrows and Enter/Select to choose a loan. Selected is marked with *."));
        main.addComponent(table);
        main.addComponent(overdueOnly);
        main.addComponent(buildActions());
        setComponent(main);
        setHints(java.util.Arrays.asList(Window.Hint.CENTERED, Window.Hint.EXPANDED));

        refresh();
    }

    private Panel buildActions() {
        Panel actions = new Panel(new LinearLayout(Direction.HORIZONTAL));
        actions.addComponent(new Button("Loan", this::loanBySelection));
        actions.addComponent(new Button("Change loan date", this::changeLoanDate));
        actions.addComponent(new Button("Change due date", this::changeDueDate));
        actions.addComponent(new Button("Return selected", this::returnSelected));
        actions.addComponent(new Button("Return by ID", this::returnByCopyId));
        actions.addComponent(new Button("Select", this::selectLoanFromTable));
        actions.addComponent(new Button("Refresh", this::refresh));
        actions.addComponent(new Button("Close", this::close));
        return actions;
    }

    private void refresh() {
        List<Loan> loans = overdueOnly.isChecked()
                ? context.getLibraryService().getOverdueLoans(LocalDate.now())
                : context.getLibraryService().getActiveLoans();
        refreshTable(loans);
    }

    private void loanBySelection() {
        Optional<String> memberOpt = selectMemberId("Loan");
        if (memberOpt.isEmpty()) {
            return;
        }
        Optional<String> bookOpt = selectBookKey("Loan");
        if (bookOpt.isEmpty()) {
            return;
        }
        try {
            String suggestedDue = context.getLibraryService()
                    .suggestDueDate(memberOpt.get(), null)
                    .toString();
            Optional<String> dueOpt = DialogUtils.promptText(context.getGui(),
                    "Loan", "Due date (YYYY-MM-DD, leave empty for default)", suggestedDue);
            if (dueOpt.isEmpty()) {
                return;
            }
            String rawDue = dueOpt.get().trim();
            Loan loan;
            if (rawDue.isEmpty()) {
                loan = context.getLibraryService().loanByIsbn(bookOpt.get(), memberOpt.get());
            } else {
                LocalDate dueDate;
                try {
                    dueDate = LocalDate.parse(rawDue);
                } catch (DateTimeParseException ex) {
                    DialogUtils.showError(context.getGui(), "Invalid date", "Use format YYYY-MM-DD.");
                    return;
                }
                loan = context.getLibraryService().loanByIsbn(bookOpt.get(), memberOpt.get(), null, dueDate);
            }
            DialogUtils.showInfo(context.getGui(), "Loan created",
                    "Member: " + memberOpt.get() + "\nCopy ID: " + loan.getCopyId() + "\nDue: " + loan.getDueDate());
            refresh();
        } catch (RuntimeException ex) {
            DialogUtils.showError(context.getGui(), "Error", ex.getMessage());
        }
    }

    private void changeLoanDate() {
        Optional<Loan> loanOpt = resolveLoanSelection("Change loan date");
        if (loanOpt.isEmpty()) {
            return;
        }
        Loan loan = loanOpt.get();
        String current = loan.getLoanDate() == null ? "" : loan.getLoanDate().toString();
        Optional<String> dateOpt = DialogUtils.promptText(context.getGui(),
                "Change loan date", "New loan date (YYYY-MM-DD)", current);
        if (dateOpt.isEmpty()) {
            return;
        }
        LocalDate newDate;
        try {
            newDate = LocalDate.parse(dateOpt.get());
        } catch (DateTimeParseException ex) {
            DialogUtils.showError(context.getGui(), "Invalid date", "Use format YYYY-MM-DD.");
            return;
        }
        try {
            Loan updated = context.getLibraryService().updateLoanDate(loan.getLoanId(), newDate);
            DialogUtils.showInfo(context.getGui(), "Loan updated",
                    "Loan ID: " + updated.getLoanId()
                            + "\nLoan date: " + updated.getLoanDate()
                            + "\nDue date: " + updated.getDueDate());
            refresh();
        } catch (RuntimeException ex) {
            DialogUtils.showError(context.getGui(), "Error", ex.getMessage());
        }
    }

    private void changeDueDate() {
        Optional<Loan> loanOpt = resolveLoanSelection("Change due date");
        if (loanOpt.isEmpty()) {
            return;
        }
        Loan loan = loanOpt.get();
        String current = loan.getDueDate() == null ? "" : loan.getDueDate().toString();
        Optional<String> dateOpt = DialogUtils.promptText(context.getGui(),
                "Change due date", "New due date (YYYY-MM-DD)", current);
        if (dateOpt.isEmpty()) {
            return;
        }
        LocalDate newDate;
        try {
            newDate = LocalDate.parse(dateOpt.get().trim());
        } catch (DateTimeParseException ex) {
            DialogUtils.showError(context.getGui(), "Invalid date", "Use format YYYY-MM-DD.");
            return;
        }
        try {
            Loan updated = context.getLibraryService().updateLoanDueDate(loan.getLoanId(), newDate);
            DialogUtils.showInfo(context.getGui(), "Loan updated",
                    "Loan ID: " + updated.getLoanId()
                            + "\nLoan date: " + updated.getLoanDate()
                            + "\nDue date: " + updated.getDueDate());
            refresh();
        } catch (RuntimeException ex) {
            DialogUtils.showError(context.getGui(), "Error", ex.getMessage());
        }
    }

    private void returnByCopyId() {
        Optional<String> copyOpt = DialogUtils.promptText(context.getGui(), "Return", "Copy ID",
                selectedCopyId == null ? "" : selectedCopyId);
        if (copyOpt.isEmpty()) {
            return;
        }
        try {
            ReturnResult result = context.getLibraryService().returnByCopyId(copyOpt.get());
            StringBuilder message = new StringBuilder();
            message.append("Returned loan ").append(result.getReturnedLoan().getLoanId());
            if (result.getReadyReservation() != null) {
                message.append("\nReservation ready for member ")
                        .append(result.getReadyReservation().getMemberId())
                        .append(" (copy reserved: ")
                        .append(result.getReservedCopy().getCopyId())
                        .append(")");
            }
            DialogUtils.showInfo(context.getGui(), "Return completed", message.toString());
            refresh();
        } catch (RuntimeException ex) {
            DialogUtils.showError(context.getGui(), "Error", ex.getMessage());
        }
    }

    private void returnSelected() {
        Optional<String> copyOpt = resolveCopyIdForReturn();
        if (copyOpt.isEmpty()) {
            return;
        }
        try {
            ReturnResult result = context.getLibraryService().returnByCopyId(copyOpt.get());
            StringBuilder message = new StringBuilder();
            message.append("Returned loan ").append(result.getReturnedLoan().getLoanId());
            if (result.getReadyReservation() != null) {
                message.append("\nReservation ready for member ")
                        .append(result.getReadyReservation().getMemberId())
                        .append(" (copy reserved: ")
                        .append(result.getReservedCopy().getCopyId())
                        .append(")");
            }
            DialogUtils.showInfo(context.getGui(), "Return completed", message.toString());
            refresh();
        } catch (RuntimeException ex) {
            DialogUtils.showError(context.getGui(), "Error", ex.getMessage());
        }
    }

    private void refreshTable(List<Loan> loans) {
        table.getTableModel().clear();
        LocalDate today = LocalDate.now();
        int selectedRowIndex = -1;
        boolean selectedFound = false;
        for (int i = 0; i < loans.size(); i++) {
            Loan loan = loans.get(i);
            boolean isSelected = loan.getLoanId().equals(selectedLoanId);
            if (isSelected) {
                selectedFound = true;
                selectedRowIndex = i;
            }
            table.getTableModel().addRow(
                    isSelected ? "*" : "",
                    loan.getLoanId(),
                    loan.getCopyId(),
                    loan.getMemberId(),
                    String.valueOf(loan.getLoanDate()),
                    String.valueOf(loan.getDueDate()),
                    loan.isOverdue(today) ? "YES" : "NO"
            );
        }
        if (!selectedFound) {
            selectedLoanId = null;
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
        return Optional.ofNullable(table.getTableModel().getRow(row).get(2));
    }

    private Optional<String> selectedLoanIdFromRow() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= table.getTableModel().getRowCount()) {
            return Optional.empty();
        }
        return Optional.ofNullable(table.getTableModel().getRow(row).get(1));
    }

    private void selectLoanFromTable() {
        Optional<String> loanId = selectedLoanIdFromRow();
        Optional<String> copyId = selectedCopyIdFromRow();
        if (loanId.isEmpty() || copyId.isEmpty()) {
            DialogUtils.showInfo(context.getGui(), "Select loan", "No loan selected.");
            return;
        }
        setSelectedLoan(loanId.get(), copyId.get(), true);
    }

    private void setSelectedLoan(String loanId, String copyId, boolean refresh) {
        selectedLoanId = loanId;
        selectedCopyId = copyId;
        if (refresh) {
            refresh();
        }
    }

    private Optional<String> resolveCopyIdForReturn() {
        if (selectedCopyId != null) {
            return Optional.of(selectedCopyId);
        }
        Optional<String> copyId = selectedCopyIdFromRow();
        Optional<String> loanId = selectedLoanIdFromRow();
        if (copyId.isPresent() && loanId.isPresent()) {
            setSelectedLoan(loanId.get(), copyId.get(), true);
            return copyId;
        }
        List<Loan> loans = context.getLibraryService().getActiveLoans();
        if (loans.isEmpty()) {
            DialogUtils.showInfo(context.getGui(), "Return", "No active loans.");
            return Optional.empty();
        }
        List<DialogUtils.Choice<String>> choices = new ArrayList<>();
        for (Loan loan : loans) {
            String label = loan.getLoanId() + " | " + loan.getCopyId() + " | " + loan.getMemberId()
                    + " | Due " + loan.getDueDate();
            choices.add(new DialogUtils.Choice<>(label, loan.getLoanId()));
        }
        Optional<String> chosenLoanId = DialogUtils.promptChoice(context.getGui(), "Return", "Choose loan", choices);
        if (chosenLoanId.isEmpty()) {
            return Optional.empty();
        }
        for (Loan loan : loans) {
            if (loan.getLoanId().equals(chosenLoanId.get())) {
                setSelectedLoan(loan.getLoanId(), loan.getCopyId(), true);
                return Optional.ofNullable(loan.getCopyId());
            }
        }
        return Optional.empty();
    }

    private Optional<Loan> resolveLoanSelection(String title) {
        List<Loan> loans = overdueOnly.isChecked()
                ? context.getLibraryService().getOverdueLoans(LocalDate.now())
                : context.getLibraryService().getActiveLoans();
        if (loans.isEmpty()) {
            DialogUtils.showInfo(context.getGui(), title, "No loans available.");
            return Optional.empty();
        }
        if (selectedLoanId != null) {
            for (Loan loan : loans) {
                if (loan.getLoanId().equals(selectedLoanId)) {
                    return Optional.of(loan);
                }
            }
        }
        Optional<String> rowLoanId = selectedLoanIdFromRow();
        if (rowLoanId.isPresent()) {
            for (Loan loan : loans) {
                if (loan.getLoanId().equals(rowLoanId.get())) {
                    setSelectedLoan(loan.getLoanId(), loan.getCopyId(), true);
                    return Optional.of(loan);
                }
            }
        }
        List<DialogUtils.Choice<String>> choices = new ArrayList<>();
        for (Loan loan : loans) {
            String label = loan.getLoanId() + " | " + loan.getCopyId() + " | " + loan.getMemberId()
                    + " | Loan " + loan.getLoanDate();
            choices.add(new DialogUtils.Choice<>(label, loan.getLoanId()));
        }
        Optional<String> chosenLoanId = DialogUtils.promptChoice(context.getGui(), title, "Choose loan", choices);
        if (chosenLoanId.isEmpty()) {
            return Optional.empty();
        }
        for (Loan loan : loans) {
            if (loan.getLoanId().equals(chosenLoanId.get())) {
                setSelectedLoan(loan.getLoanId(), loan.getCopyId(), true);
                return Optional.of(loan);
            }
        }
        return Optional.empty();
    }

    private Optional<String> selectMemberId(String title) {
        List<Member> members = context.getLibraryService().listMembers();
        if (members.isEmpty()) {
            DialogUtils.showInfo(context.getGui(), title, "No members available.");
            return Optional.empty();
        }
        List<DialogUtils.Choice<String>> choices = new ArrayList<>();
        for (Member member : members) {
            choices.add(new DialogUtils.Choice<>(
                    member.getMemberId() + " | " + member.getName() + " | " + member.getType().name(),
                    member.getMemberId()));
        }
        return DialogUtils.promptChoice(context.getGui(), title, "Choose member", choices);
    }

    private Optional<String> selectBookKey(String title) {
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
        return DialogUtils.promptChoice(context.getGui(), title, "Choose book", choices);
    }
}