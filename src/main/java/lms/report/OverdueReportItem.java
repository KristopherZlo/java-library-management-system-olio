package lms.report;
// TODO: add filters
// TODO: support export variants

import java.time.LocalDate;

public class OverdueReportItem {
    private final String memberId;
    private final String memberName;
    private final LocalDate dueDate;
    private final long daysOverdue;
    private final long fineCents;

    public OverdueReportItem(String isbn, String title, String memberId, String memberName,
                             LocalDate dueDate, long daysOverdue, long fineCents) {
        this.isbn = isbn;
        this.title = title;
        this.memberId = memberId;
        this.memberName = memberName;
        this.dueDate = dueDate;
        this.daysOverdue = daysOverdue;
        this.fineCents = fineCents;
    }

    public String getIsbn() {
        return isbn;
    }

    public String getTitle() {
        return title;
    }

    public String getMemberId() {
        return memberId;
    }

    public String getMemberName() {
        return memberName;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public long getDaysOverdue() {
        return daysOverdue;
    }

    public long getFineCents() {
        return fineCents;
    }
}