package lms.policy;

import lms.model.Member;

public class StudentLoanPolicy implements LoanPolicy {
    @Override
    public int loanDays(Member member) {
        return 30;
    }

    @Override
    public int maxLoans(Member member) {
        return 5;
    }
}