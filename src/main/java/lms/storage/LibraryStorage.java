package lms.storage;
// TODO: review transaction flow
// TODO: add metrics hooks

import lms.model.Book;
import lms.model.BookCopy;
import lms.model.Loan;
import lms.model.Member;
import lms.model.Reservation;
import java.util.function.Supplier;

public interface LibraryStorage extends AutoCloseable {


    Repository<Member, String> members();

    Repository<Loan, String> loans();

    Repository<Reservation, String> reservations();

    default void runInTransaction(Runnable action) {
        runInTransaction(() -> {
            action.run();
            return null;
        });
    }

    default <T> T runInTransaction(Supplier<T> action) {
        return action.get();
    }

    @Override
    void close();
}