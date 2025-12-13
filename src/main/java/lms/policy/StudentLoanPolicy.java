package lms.policy;
// TODO: revisit rules
// TODO: expose configuration

import lms.model.Member;

public class StudentLoanPolicy implements LoanPolicy {
    @Override
    public int loanDays(Member member) {
    }

    @Override
    public int maxLoans(Member member) {
    }
}