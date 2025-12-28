package lms.policy;

import lms.model.Loan;
import java.time.LocalDate;

public interface FinePolicy {
    long fineCents(Loan loan, LocalDate today);
}