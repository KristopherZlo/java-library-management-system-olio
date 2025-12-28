package lms.policy;

import lms.model.Member;

public class AdultLoanPolicy implements LoanPolicy {
    @Override
    public int loanDays(Member member) {
        return 21;
    }

    @Override
    public int maxLoans(Member member) {
        return 3;
    }
}