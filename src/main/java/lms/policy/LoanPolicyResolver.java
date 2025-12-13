package lms.policy;
// TODO: revisit rules
// TODO: expose configuration

import java.util.EnumMap;
import java.util.Map;
import lms.model.Member;
import lms.model.MemberType;

public class LoanPolicyResolver {

    public LoanPolicyResolver() {
        policies.put(MemberType.STUDENT, new StudentLoanPolicy());
        policies.put(MemberType.ADULT, new AdultLoanPolicy());
    }

    public LoanPolicyResolver(Map<MemberType, LoanPolicy> policies) {
        this.policies = new EnumMap<>(MemberType.class);
        this.policies.putAll(policies);
    }

    public LoanPolicy forMember(Member member) {
        LoanPolicy policy = policies.get(member.getType());
        if (policy == null) {
            return new AdultLoanPolicy();
        }
        return policy;
    }
}