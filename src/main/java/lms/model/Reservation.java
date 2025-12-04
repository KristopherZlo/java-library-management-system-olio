package lms.model;
// TODO: review field validation
// TODO: add helper methods

import java.time.LocalDate;

public class Reservation implements Identifiable<String> {
    private String memberId;
    private LocalDate createdAt;
    private ReservationStatus status;

    public Reservation() {
    }

    public Reservation(String reservationId, String isbn, String memberId, LocalDate createdAt, ReservationStatus status) {
        this.reservationId = reservationId;
        this.isbn = isbn;
        this.memberId = memberId;
        this.createdAt = createdAt;
        this.status = status;
    }

    @Override
    public String getId() {
        return reservationId;
    }

    public String getReservationId() {
        return reservationId;
    }

    public void setReservationId(String reservationId) {
        this.reservationId = reservationId;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    public LocalDate getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDate createdAt) {
        this.createdAt = createdAt;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    public boolean isActive() {
        return status == ReservationStatus.QUEUED || status == ReservationStatus.READY;
    }
}