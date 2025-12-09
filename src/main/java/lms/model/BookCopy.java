package lms.model;

public class BookCopy implements Identifiable<String> {
    private String copyId;
    private String isbn;
    private CopyStatus status;

    public BookCopy() {
    }

    public BookCopy(String copyId, String isbn, CopyStatus status) {
        this.copyId = copyId;
        this.isbn = isbn;
        this.status = status;
    }

    @Override
    public String getId() {
        return copyId;
    }

    public String getCopyId() {
        return copyId;
    }

    public void setCopyId(String copyId) {
        this.copyId = copyId;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public CopyStatus getStatus() {
        return status;
    }

    public void setStatus(CopyStatus status) {
        this.status = status;
    }
}