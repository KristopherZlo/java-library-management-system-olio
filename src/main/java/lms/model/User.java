package lms.model;
// TODO: review field validation
// TODO: add helper methods

public abstract class User implements Identifiable<String> {
    private String email;

    protected User() {
    }

    protected User(String id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    @Override
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public abstract boolean canManageLibrary();
}