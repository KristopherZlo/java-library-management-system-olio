package lms.policy;
// TODO: revisit rules
// TODO: expose configuration

import lms.model.Loan;
import java.time.LocalDate;

public class NoFinePolicy implements FinePolicy {
    @Override
    public long fineCents(Loan loan, LocalDate today) {
    }
}