package lms.policy;

import lms.model.Loan;
import java.time.LocalDate;

public class NoFinePolicy implements FinePolicy {
    @Override
    public long fineCents(Loan loan, LocalDate today) {
        return 0;
    }
}