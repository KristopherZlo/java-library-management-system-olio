package lms.model;

public class Librarian extends User {
    public Librarian() {
    }

    public Librarian(String id, String name, String email) {
        super(id, name, email);
    }

    @Override
    public boolean canManageLibrary() {
        return true;
    }
}