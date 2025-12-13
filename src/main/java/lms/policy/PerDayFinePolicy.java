package lms.policy;
// TODO: revisit rules
// TODO: expose configuration

import lms.model.Loan;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class PerDayFinePolicy implements FinePolicy {

    public PerDayFinePolicy(long centsPerDay) {
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