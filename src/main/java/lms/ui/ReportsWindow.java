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
import com.googlecode.lanterna.gui2.table.TableModel;
import com.googlecode.lanterna.gui2.Window;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lms.model.Member;
import lms.report.MemberLoanReportItem;
import lms.report.OverdueReportItem;
import lms.report.PopularBookItem;
import lms.report.ReportService;

public class ReportsWindow extends BasicWindow {
    private List<String> lastHeaders = new ArrayList<>();
    private List<String[]> lastRows = new ArrayList<>();

    public ReportsWindow(UiContext context) {
        super("Reports");
        this.context = context;
        this.table = new Table<>("Report");
        table.setCellSelection(false);

        Panel main = new Panel(new LinearLayout(Direction.VERTICAL));
        main.addComponent(new Label("Choose a report and follow the prompts."));
        main.addComponent(table);
        main.addComponent(buildActions());
        setComponent(main);
        setHints(java.util.Arrays.asList(Window.Hint.CENTERED, Window.Hint.EXPANDED));
    }

    private Panel buildActions() {
        Panel actions = new Panel(new LinearLayout(Direction.HORIZONTAL));
        actions.addComponent(new Button("Overdue report", this::showOverdue));
        actions.addComponent(new Button("Member report", this::showMemberReport));
        actions.addComponent(new Button("Popular books", this::showPopular));
        actions.addComponent(new Button("Export CSV", this::exportCsv));
        actions.addComponent(new Button("Close", this::close));
        return actions;
    }

    private void showOverdue() {
        ReportService reports = context.getReportService();
        List<OverdueReportItem> items = reports.buildOverdueReport(LocalDate.now());
        List<String> headers = Arrays.asList("ISBN", "Title", "Member", "Due", "Days", "Fine");
        List<String[]> rows = new ArrayList<>();
        for (OverdueReportItem item : items) {
            rows.add(new String[] {
                    item.getIsbn(),
                    item.getTitle(),
                    item.getMemberId(),
                    String.valueOf(item.getDueDate()),
                    String.valueOf(item.getDaysOverdue()),
                    formatMoney(item.getFineCents())
            });
        }
        updateTable(headers, rows);
    }

    private void showMemberReport() {
        Optional<String> memberOpt = selectMemberId("Member report");
        if (memberOpt.isEmpty()) {
            return;
        }
        List<MemberLoanReportItem> items = context.getReportService().buildMemberReport(memberOpt.get());
        List<String> headers = Arrays.asList("Loan ID", "ISBN", "Title", "Copy ID", "Loan Date", "Due Date");
        List<String[]> rows = new ArrayList<>();
        for (MemberLoanReportItem item : items) {
            rows.add(new String[] {
                    item.getLoanId(),
                    item.getIsbn(),
                    item.getTitle(),
                    item.getCopyId(),
                    String.valueOf(item.getLoanDate()),
                    String.valueOf(item.getDueDate())
            });
        }
        updateTable(headers, rows);
    }

    private void showPopular() {
        List<PopularBookItem> items = context.getReportService().buildPopularBooksReport(10);
        List<String> headers = Arrays.asList("ISBN", "Title", "Author", "Total Loans");
        List<String[]> rows = new ArrayList<>();
        for (PopularBookItem item : items) {
            rows.add(new String[] {
                    item.getIsbn(),
                    item.getTitle(),
                    item.getAuthor(),
                    String.valueOf(item.getTotalLoans())
            });
        }
        updateTable(headers, rows);
    }

    private void exportCsv() {
        if (lastRows.isEmpty()) {
            DialogUtils.showError(context.getGui(), "Export", "No report data to export.");
            return;
        }
        Optional<String> pathOpt = DialogUtils.promptText(context.getGui(), "Export CSV", "Path", "data/report.csv");
        if (pathOpt.isEmpty()) {
            return;
        }
        try {
            Path path = Paths.get(pathOpt.get());
            context.getCsvExporter().export(path, lastHeaders, lastRows);
            DialogUtils.showInfo(context.getGui(), "Export", "CSV exported to " + path);
        } catch (RuntimeException ex) {
            DialogUtils.showError(context.getGui(), "Export failed", ex.getMessage());
        }
    }

    private void updateTable(List<String> headers, List<String[]> rows) {
        TableModel<String> model = new TableModel<>(headers.toArray(new String[0]));
        for (String[] row : rows) {
            model.addRow(row);
        }
        table.setTableModel(model);
        this.lastHeaders = headers;
        this.lastRows = rows;
    }

    private String formatMoney(long cents) {
        return String.format("%.2f", cents / 100.0);
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
}