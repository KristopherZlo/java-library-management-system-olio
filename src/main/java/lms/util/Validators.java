package lms.util;

import lms.exception.ValidationException;

public final class Validators {
    private Validators() {
    }

    public static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(fieldName + " must not be empty");
        }
        return value.trim();
    }

    public static String validateIsbn(String isbn) {
        String cleaned = requireNonBlank(isbn, "ISBN").replace("-", "");
        if (!cleaned.matches("\\d{10}|\\d{13}")) {
            throw new ValidationException("ISBN must be 10 or 13 digits");
        }
        return cleaned;
    }

    public static String validateEmail(String email) {
        String cleaned = requireNonBlank(email, "Email");
        if (!cleaned.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new ValidationException("Email format is invalid");
        }
        return cleaned;
    }
}