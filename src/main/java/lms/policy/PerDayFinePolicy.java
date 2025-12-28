package lms.policy;

import lms.model.Loan;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class PerDayFinePolicy implements FinePolicy {
    private final long centsPerDay;

    public PerDayFinePolicy(long centsPerDay) {
        this.centsPerDay = centsPerDay;
    }

    @Override
    public long fineCents(Loan loan, LocalDate today) {
        if (loan.getDueDate() == null || !today.isAfter(loan.getDueDate())) {
            return 0;
        }
        long days = ChronoUnit.DAYS.between(loan.getDueDate(), today);
        return days * centsPerDay;
    }
}