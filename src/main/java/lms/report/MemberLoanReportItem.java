package lms.report;
// TODO: add filters
// TODO: support export variants

import java.time.LocalDate;

public class MemberLoanReportItem {
    private final String title;
    private final String copyId;
    private final LocalDate loanDate;
    private final LocalDate dueDate;

    public MemberLoanReportItem(String loanId, String isbn, String title, String copyId,
                                LocalDate loanDate, LocalDate dueDate) {
        this.loanId = loanId;
        this.isbn = isbn;
        this.title = title;
        this.copyId = copyId;
        this.loanDate = loanDate;
        this.dueDate = dueDate;
    }

    public String getLoanId() {
        return loanId;
    }

    public String getIsbn() {
        return isbn;
    }

    public String getTitle() {
        return title;
    }

    public String getCopyId() {
        return copyId;
    }

    public LocalDate getLoanDate() {
        return loanDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }
}