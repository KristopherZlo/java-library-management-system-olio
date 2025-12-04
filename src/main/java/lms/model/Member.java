package lms.model;
// TODO: review field validation
// TODO: add helper methods

public class Member extends User {

    public Member() {
    }

    public Member(String memberId, String name, String email, MemberType type) {
        this.type = type;
    }

    public String getMemberId() {
        return getId();
    }

    public void setMemberId(String memberId) {
        setId(memberId);
    }

    public MemberType getType() {
        return type;
    }

    public void setType(MemberType type) {
        this.type = type;
    }

    @Override
    public boolean canManageLibrary() {
        return false;
    }
}