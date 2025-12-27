package lms.policy;

import lms.model.Member;

public interface LoanPolicy {
    int loanDays(Member member);

    int maxLoans(Member member);
}