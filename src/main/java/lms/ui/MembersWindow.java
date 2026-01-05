package lms.ui;

import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.table.Table;
import com.googlecode.lanterna.gui2.Window;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lms.model.Book;
import lms.model.Loan;
import lms.model.Member;
import lms.model.MemberType;
import lms.model.Reservation;

public class MembersWindow extends BasicWindow {
    private final UiContext context;
    private final Table<String> table;
    private String selectedMemberId;
    private List<Member> displayedMembers = new ArrayList<>();

    public MembersWindow(UiContext context) {
        super("Members");
        this.context = context;
        this.table = new Table<>("Selected", "Member ID", "Name", "Email", "Type");
        table.setCellSelection(false);
        table.setSelectAction(this::selectMemberFromTable);

        Panel main = new Panel(new LinearLayout(Direction.VERTICAL));
        main.addComponent(new Label("Use arrows and Enter/Select to choose a member. Search finds by name or ID."));
        main.addComponent(table);
        main.addComponent(buildActions());
        setComponent(main);
        setHints(java.util.Arrays.asList(Window.Hint.CENTERED, Window.Hint.EXPANDED));

        refreshTable(context.getLibraryService().listMembers());
        updateTitle();
    }

    private Panel buildActions() {
        Panel actions = new Panel(new LinearLayout(Direction.HORIZONTAL));
        actions.addComponent(new Button("Add", this::addMember));
        actions.addComponent(new Button("Edit", this::editMember));
        actions.addComponent(new Button("Remove", this::removeMember));
        actions.addComponent(new Button("Select", this::selectMemberFromTable));
        actions.addComponent(new Button("Actions", this::memberActions));
        actions.addComponent(new Button("Search", this::searchMembers));
        actions.addComponent(new Button("Refresh", this::refresh));
        actions.addComponent(new Button("Close", this::close));
        return actions;
    }

    private void refresh() {
        refreshTable(context.getLibraryService().listMembers());
    }

    private void searchMembers() {
        Optional<String> queryOpt = DialogUtils.promptText(context.getGui(), "Search members", "Name or Member ID", "");
        if (queryOpt.isEmpty()) {
            return;
        }
        if (queryOpt.get().trim().isEmpty()) {
            DialogUtils.showInfo(context.getGui(), "Search members", "Please enter a name or ID.");
            return;
        }
        try {
            List<Member> results = context.getLibraryService().searchMembers(queryOpt.get());
            if (results.isEmpty()) {
                DialogUtils.showInfo(context.getGui(), "Search members", "No matches found.");
            }
            refreshTable(results);
        } catch (RuntimeException ex) {
            DialogUtils.showError(context.getGui(), "Error", ex.getMessage());
        }
    }

    private void addMember() {
        Optional<String> nameOpt = DialogUtils.promptText(context.getGui(), "Add Member", "Name", "");
        if (nameOpt.isEmpty()) {
            return;
        }
        Optional<String> emailOpt = DialogUtils.promptText(context.getGui(), "Add Member", "Email", "");
        if (emailOpt.isEmpty()) {
            return;
        }
        List<DialogUtils.Choice<MemberType>> choices = Arrays.asList(
                new DialogUtils.Choice<>("STUDENT", MemberType.STUDENT),
                new DialogUtils.Choice<>("ADULT", MemberType.ADULT)
        );
        Optional<MemberType> typeOpt = DialogUtils.promptChoice(context.getGui(), "Add Member", "Choose member type", choices);
        if (typeOpt.isEmpty()) {
            return;
        }
        try {
            Member member = new Member(null, nameOpt.get(), emailOpt.get(), typeOpt.get());
            context.getLibraryService().addMember(member);
            refresh();
        } catch (RuntimeException ex) {
            DialogUtils.showError(context.getGui(), "Error", ex.getMessage());
        }
    }

    private void editMember() {
        Optional<String> idOpt = resolveMemberId();
        if (idOpt.isEmpty()) {
            return;
        }
        String memberId = idOpt.get();
        Member member = findMemberById(memberId);
        if (member == null) {
            DialogUtils.showError(context.getGui(), "Error", "Member not found: " + memberId);
            return;
        }
        Optional<String> nameOpt = DialogUtils.promptText(context.getGui(), "Edit Member", "Name", member.getName());
        if (nameOpt.isEmpty()) {
            return;
        }
        Optional<String> emailOpt = DialogUtils.promptText(context.getGui(), "Edit Member", "Email", member.getEmail());
        if (emailOpt.isEmpty()) {
            return;
        }
        String typeLabel = "Choose member type (current: " + member.getType().name() + ")";
        List<DialogUtils.Choice<MemberType>> choices = Arrays.asList(
                new DialogUtils.Choice<>("STUDENT", MemberType.STUDENT),
                new DialogUtils.Choice<>("ADULT", MemberType.ADULT)
        );
        Optional<MemberType> typeOpt = DialogUtils.promptChoice(context.getGui(), "Edit Member", typeLabel, choices);
        if (typeOpt.isEmpty()) {
            return;
        }
        try {
            context.getLibraryService().updateMember(memberId, nameOpt.get(), emailOpt.get(), typeOpt.get());
            refresh();
        } catch (RuntimeException ex) {
            DialogUtils.showError(context.getGui(), "Error", ex.getMessage());
        }
    }

    private void removeMember() {
        Optional<String> idOpt = resolveMemberId();
        if (idOpt.isEmpty()) {
            return;
        }
        try {
            if (DialogUtils.confirm(context.getGui(), "Remove Member",
                    "Remove member " + idOpt.get() + "? This cannot be undone.")) {
                context.getLibraryService().removeMember(idOpt.get());
                if (idOpt.get().equals(selectedMemberId)) {
                    selectedMemberId = null;
                    updateTitle();
                }
                refresh();
            }
        } catch (RuntimeException ex) {
            DialogUtils.showError(context.getGui(), "Error", ex.getMessage());
        }
    }

    private void memberActions() {
        Optional<String> memberIdOpt = resolveMemberId();
        if (memberIdOpt.isEmpty()) {
            return;
        }
        String memberId = memberIdOpt.get();
        Member member = findMemberById(memberId);
        Optional<MemberAction> actionOpt = selectMemberAction(member);
        if (actionOpt.isEmpty()) {
            return;
        }
        switch (actionOpt.get()) {
            case LOAN:
                loanToMember(memberId, member);
                break;
            case RESERVE:
                reserveForMember(memberId, member);
                break;
            case COPY_ID:
                copyMemberId(memberId, member);
                break;
            default:
                break;
        }
    }

    private Optional<MemberAction> selectMemberAction(Member member) {
        String memberLabel = memberSummary(member);
        String description = memberLabel.isEmpty() ? "Choose action" : "Member: " + memberLabel;
        List<DialogUtils.Choice<MemberAction>> choices = Arrays.asList(
                new DialogUtils.Choice<>("Loan book", MemberAction.LOAN),
                new DialogUtils.Choice<>("Reserve book", MemberAction.RESERVE),
                new DialogUtils.Choice<>("Copy member ID", MemberAction.COPY_ID)
        );
        return DialogUtils.promptChoice(context.getGui(), "Member actions", description, choices);
    }

    private void loanToMember(String memberId, Member member) {
        String memberLabel = memberSummary(member);
        Optional<String> bookOpt = selectBookKey("Loan to member", memberLabel);
        if (bookOpt.isEmpty()) {
            return;
        }
        try {
            String suggestedDue = context.getLibraryService()
                    .suggestDueDate(memberId, null)
                    .toString();
            Optional<String> dueOpt = DialogUtils.promptText(context.getGui(),
                    "Loan to member", "Due date (YYYY-MM-DD, leave empty for default)", suggestedDue);
            if (dueOpt.isEmpty()) {
                return;
            }
            String rawDue = dueOpt.get().trim();
            Loan loan;
            if (rawDue.isEmpty()) {
                loan = context.getLibraryService().loanByIsbn(bookOpt.get(), memberId);
            } else {
                LocalDate dueDate;
                try {
                    dueDate = LocalDate.parse(rawDue);
                } catch (DateTimeParseException ex) {
                    DialogUtils.showError(context.getGui(), "Invalid date", "Use format YYYY-MM-DD.");
                    return;
                }
                loan = context.getLibraryService().loanByIsbn(bookOpt.get(), memberId, null, dueDate);
            }
            DialogUtils.showInfo(context.getGui(), "Loan created",
                    (memberLabel.isEmpty() ? "" : "Member: " + memberLabel + "\n")
                            + "Copy ID: " + loan.getCopyId() + "\nDue: " + loan.getDueDate());
            refresh();
        } catch (RuntimeException ex) {
            DialogUtils.showError(context.getGui(), "Error", ex.getMessage());
        }
    }

    private void reserveForMember(String memberId, Member member) {
        String memberLabel = memberSummary(member);
        Optional<String> bookOpt = selectBookKey("Reserve for member", memberLabel);
        if (bookOpt.isEmpty()) {
            return;
        }
        try {
            Reservation reservation = context.getLibraryService().reserveByIsbn(bookOpt.get(), memberId);
            DialogUtils.showInfo(context.getGui(), "Reserved",
                    (memberLabel.isEmpty() ? "" : "Member: " + memberLabel + "\n")
                            + "Reservation ID: " + reservation.getReservationId());
            refresh();
        } catch (RuntimeException ex) {
            DialogUtils.showError(context.getGui(), "Error", ex.getMessage());
        }
    }

    private void copyMemberId(String memberId, Member member) {
        String memberLabel = memberSummary(member);
        boolean copied = DialogUtils.copyToClipboard(memberId);
        if (copied) {
            DialogUtils.showInfo(context.getGui(), "Copy member ID",
                    (memberLabel.isEmpty() ? "" : "Member: " + memberLabel + "\n")
                            + "Copied to clipboard: " + memberId);
        } else {
            DialogUtils.showInfo(context.getGui(), "Copy member ID",
                    (memberLabel.isEmpty() ? "" : "Member: " + memberLabel + "\n")
                            + "Member ID: " + memberId);
        }
    }

    private Optional<String> resolveMemberId() {
        if (selectedMemberId != null) {
            return Optional.of(selectedMemberId);
        }
        Optional<String> selected = selectedRowMemberId();
        if (selected.isPresent()) {
            setSelectedMemberId(selected.get(), true);
            return selected;
        }
        List<Member> members = displayedMembers;
        if (members.isEmpty()) {
            DialogUtils.showInfo(context.getGui(), "Select member", "No members available.");
            return Optional.empty();
        }
        List<DialogUtils.Choice<String>> choices = new ArrayList<>();
        for (Member member : members) {
            choices.add(new DialogUtils.Choice<>(
                    member.getMemberId() + " | " + member.getName() + " | " + member.getType().name(),
                    member.getMemberId()));
        }
        Optional<String> chosen = DialogUtils.promptChoice(context.getGui(), "Select member", "Choose member", choices);
        chosen.ifPresent(memberId -> setSelectedMemberId(memberId, true));
        return chosen;
    }

    private Optional<String> selectBookKey(String title, String memberLabel) {
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
        String description = memberLabel == null || memberLabel.isEmpty()
                ? "Choose book"
                : "Member: " + memberLabel;
        return DialogUtils.promptChoice(context.getGui(), title, description, choices);
    }

    private Member findMemberById(String memberId) {
        for (Member member : context.getLibraryService().listMembers()) {
            if (memberId.equals(member.getMemberId())) {
                return member;
            }
        }
        return null;
    }

    private String memberSummary(Member member) {
        if (member == null) {
            return "";
        }
        return member.getMemberId() + " | " + member.getName() + " | " + member.getType().name();
    }

    private Optional<String> selectedRowMemberId() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= table.getTableModel().getRowCount()) {
            return Optional.empty();
        }
        return Optional.ofNullable(table.getTableModel().getRow(row).get(1));
    }

    private void refreshTable(List<Member> members) {
        displayedMembers = new ArrayList<>(members);
        table.getTableModel().clear();
        int selectedRowIndex = -1;
        boolean selectedFound = false;
        for (int i = 0; i < members.size(); i++) {
            Member member = members.get(i);
            boolean isSelected = member.getMemberId().equals(selectedMemberId);
            if (isSelected) {
                selectedFound = true;
                selectedRowIndex = i;
            }
            table.getTableModel().addRow(
                    isSelected ? "*" : "",
                    member.getMemberId(),
                    member.getName(),
                    member.getEmail(),
                    member.getType().name()
            );
        }
        if (!selectedFound) {
            selectedMemberId = null;
            updateTitle();
        }
        if (selectedRowIndex >= 0) {
            table.setSelectedRow(selectedRowIndex);
        }
    }

    private void selectMemberFromTable() {
        Optional<String> selected = selectedRowMemberId();
        if (selected.isEmpty()) {
            DialogUtils.showInfo(context.getGui(), "Select member", "No member selected.");
            return;
        }
        setSelectedMemberId(selected.get(), true);
    }

    private void setSelectedMemberId(String memberId, boolean refresh) {
        selectedMemberId = memberId;
        updateTitle();
        if (refresh) {
            refresh();
        }
    }

    private void updateTitle() {
        if (selectedMemberId == null || selectedMemberId.isEmpty()) {
            setTitle("Members");
        } else {
            setTitle("Members (Selected: " + selectedMemberId + ")");
        }
    }

    private enum MemberAction {
        LOAN,
        RESERVE,
        COPY_ID
    }
}