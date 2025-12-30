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
import lms.model.Member;
import lms.model.Reservation;
import lms.model.ReservationStatus;

public class ReservationsWindow extends BasicWindow {
    private String selectedReservationId;

    public ReservationsWindow(UiContext context) {
        super("Reservations");
        this.context = context;
        this.table = new Table<>("Selected", "Reservation ID", "ISBN", "Member ID", "Created", "Status");
        table.setCellSelection(false);
        table.setSelectAction(this::selectReservationFromTable);

        Panel main = new Panel(new LinearLayout(Direction.VERTICAL));
        main.addComponent(new Label("Use arrows and Enter/Select to choose a reservation."));
        main.addComponent(table);
        main.addComponent(buildActions());
        setComponent(main);
        setHints(java.util.Arrays.asList(Window.Hint.CENTERED, Window.Hint.EXPANDED));

        refresh();
    }

    private Panel buildActions() {
        Panel actions = new Panel(new LinearLayout(Direction.HORIZONTAL));
        actions.addComponent(new Button("Add reservation", this::addReservation));
        actions.addComponent(new Button("Edit", this::editReservation));
        actions.addComponent(new Button("Remove", this::removeReservation));
        actions.addComponent(new Button("Select", this::selectReservationFromTable));
        actions.addComponent(new Button("Refresh", this::refresh));
        actions.addComponent(new Button("Close", this::close));
        return actions;
    }

    private void refresh() {
        refreshTable(context.getLibraryService().listReservations());
    }

    private void addReservation() {
        Optional<String> memberOpt = selectMemberId("Reserve");
        if (memberOpt.isEmpty()) {
            return;
        }
        Optional<String> isbnOpt = selectBookKey("Reserve");
        if (isbnOpt.isEmpty()) {
            return;
        }
        try {
            Reservation reservation = context.getLibraryService().reserveByIsbn(isbnOpt.get(), memberOpt.get());
            DialogUtils.showInfo(context.getGui(), "Reserved",
                    "Reservation ID: " + reservation.getReservationId());
            refresh();
        } catch (RuntimeException ex) {
            DialogUtils.showError(context.getGui(), "Error", ex.getMessage());
        }
    }

    private void editReservation() {
        Optional<String> reservationOpt = resolveReservationId();
        if (reservationOpt.isEmpty()) {
            return;
        }
        String reservationId = reservationOpt.get();
        Reservation reservation = findReservationById(reservationId);
        if (reservation == null) {
            DialogUtils.showError(context.getGui(), "Error", "Reservation not found: " + reservationId);
            return;
        }
        Optional<String> memberOpt = selectMemberId("Edit reservation (current: " + reservation.getMemberId() + ")");
        if (memberOpt.isEmpty()) {
            return;
        }
        ReservationStatus currentStatus = reservation.getStatus();
        List<DialogUtils.Choice<ReservationStatus>> statusChoices = new ArrayList<>();
        for (ReservationStatus status : ReservationStatus.values()) {
            String label = status.name();
            if (status == currentStatus) {
                label += " (current)";
            }
            statusChoices.add(new DialogUtils.Choice<>(label, status));
        }
        Optional<ReservationStatus> statusOpt = DialogUtils.promptChoice(
                context.getGui(),
                "Edit reservation",
                "Choose status (current: " + currentStatus.name() + ")",
                statusChoices
        );
        if (statusOpt.isEmpty()) {
            return;
        }
        try {
            context.getLibraryService().updateReservation(reservationId, memberOpt.get(), statusOpt.get());
            refresh();
        } catch (RuntimeException ex) {
            DialogUtils.showError(context.getGui(), "Error", ex.getMessage());
        }
    }

    private void removeReservation() {
        Optional<String> reservationOpt = resolveReservationId();
        if (reservationOpt.isEmpty()) {
            return;
        }
        String reservationId = reservationOpt.get();
        if (!DialogUtils.confirm(context.getGui(), "Remove reservation",
                "Remove reservation " + reservationId + "? This cannot be undone.")) {
            return;
        }
        try {
            context.getLibraryService().removeReservation(reservationId);
            if (reservationId.equals(selectedReservationId)) {
                selectedReservationId = null;
            }
            refresh();
        } catch (RuntimeException ex) {
            DialogUtils.showError(context.getGui(), "Error", ex.getMessage());
        }
    }

    private void refreshTable(List<Reservation> reservations) {
        table.getTableModel().clear();
        int selectedRowIndex = -1;
        boolean selectedFound = false;
        for (int i = 0; i < reservations.size(); i++) {
            Reservation reservation = reservations.get(i);
            boolean isSelected = reservation.getReservationId().equals(selectedReservationId);
            if (isSelected) {
                selectedFound = true;
                selectedRowIndex = i;
            }
            table.getTableModel().addRow(
                    isSelected ? "*" : "",
                    reservation.getReservationId(),
                    reservation.getIsbn(),
                    reservation.getMemberId(),
                    String.valueOf(reservation.getCreatedAt()),
                    reservation.getStatus().name()
            );
        }
        if (!selectedFound) {
            selectedReservationId = null;
        }
        if (selectedRowIndex >= 0) {
            table.setSelectedRow(selectedRowIndex);
        }
    }

    private Optional<String> selectedReservationIdFromRow() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= table.getTableModel().getRowCount()) {
            return Optional.empty();
        }
        return Optional.ofNullable(table.getTableModel().getRow(row).get(1));
    }

    private void selectReservationFromTable() {
        Optional<String> selected = selectedReservationIdFromRow();
        if (selected.isEmpty()) {
            DialogUtils.showInfo(context.getGui(), "Select reservation", "No reservation selected.");
            return;
        }
        setSelectedReservationId(selected.get(), true);
    }

    private void setSelectedReservationId(String reservationId, boolean refresh) {
        selectedReservationId = reservationId;
        if (refresh) {
            refresh();
        }
    }

    private Optional<String> resolveReservationId() {
        if (selectedReservationId != null) {
            return Optional.of(selectedReservationId);
        }
        Optional<String> selectedRow = selectedReservationIdFromRow();
        if (selectedRow.isPresent()) {
            setSelectedReservationId(selectedRow.get(), true);
            return selectedRow;
        }
        List<Reservation> reservations = context.getLibraryService().listReservations();
        if (reservations.isEmpty()) {
            DialogUtils.showInfo(context.getGui(), "Remove reservation", "No reservations available.");
            return Optional.empty();
        }
        List<DialogUtils.Choice<String>> choices = new ArrayList<>();
        for (Reservation reservation : reservations) {
            String label = reservation.getReservationId() + " | " + reservation.getIsbn() + " | "
                    + reservation.getMemberId() + " | " + reservation.getStatus().name();
            choices.add(new DialogUtils.Choice<>(label, reservation.getReservationId()));
        }
        Optional<String> chosen = DialogUtils.promptChoice(context.getGui(), "Remove reservation", "Choose reservation", choices);
        chosen.ifPresent(id -> setSelectedReservationId(id, true));
        return chosen;
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

    private Reservation findReservationById(String reservationId) {
        for (Reservation reservation : context.getLibraryService().listReservations()) {
            if (reservationId.equals(reservation.getReservationId())) {
                return reservation;
            }
        }
        return null;
    }
}