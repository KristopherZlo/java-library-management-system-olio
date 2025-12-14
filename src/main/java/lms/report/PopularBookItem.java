package lms.report;
// TODO: add filters
// TODO: support export variants

public class PopularBookItem {
    private final String author;
    private final int totalLoans;

    public PopularBookItem(String isbn, String title, String author, int totalLoans) {
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.totalLoans = totalLoans;
    }

    public String getIsbn() {
        return isbn;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public int getTotalLoans() {
        return totalLoans;
    }
}