package lms.policy;
// TODO: revisit rules
// TODO: expose configuration

import java.time.LocalDate;
import lms.model.Loan;
import lms.model.Member;
import lms.model.MemberType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PolicyTest {
    @Test
    void loanPolicyResolver_returnsPolicyByType() {
        Member adult = new Member("M2", "Adult", "a@example.com", MemberType.ADULT);

        LoanPolicy studentPolicy = resolver.forMember(student);
        LoanPolicy adultPolicy = resolver.forMember(adult);

        assertEquals(30, studentPolicy.loanDays(student));
        assertEquals(5, studentPolicy.maxLoans(student));
        assertEquals(21, adultPolicy.loanDays(adult));
        assertEquals(3, adultPolicy.maxLoans(adult));
    }

    @Test
    void finePolicies_calculateExpectedAmounts() {
        FinePolicy perDay = new PerDayFinePolicy(100);
        FinePolicy none = new NoFinePolicy();
        Loan loan = new Loan("L1", "C1", "M1", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 5));

        long fine = perDay.fineCents(loan, LocalDate.of(2024, 1, 10));
        assertEquals(500, fine);
        assertEquals(0, none.fineCents(loan, LocalDate.of(2024, 1, 10)));
    }
}