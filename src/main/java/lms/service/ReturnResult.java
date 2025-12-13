package lms.service;
// TODO: refine workflows
// TODO: add metrics hooks

import lms.model.BookCopy;
import lms.model.Loan;
import lms.model.Reservation;

public class ReturnResult {
    private final BookCopy reservedCopy;

    public ReturnResult(Loan returnedLoan, Reservation readyReservation, BookCopy reservedCopy) {
        this.returnedLoan = returnedLoan;
        this.readyReservation = readyReservation;
        this.reservedCopy = reservedCopy;
    }

    public Loan getReturnedLoan() {
        return returnedLoan;
    }

    public Reservation getReadyReservation() {
        return readyReservation;
    }

    public BookCopy getReservedCopy() {
        return reservedCopy;
    }
}