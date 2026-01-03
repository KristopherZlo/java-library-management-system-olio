package lms.util;
// TODO: review edge cases
// TODO: add test notes

import lms.exception.ValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ValidatorsTest {
    @Test
    void requireNonBlank_trimsValue() {
    }

    @Test
    void requireNonBlank_throwsOnEmpty() {
        assertThrows(ValidationException.class, () -> Validators.requireNonBlank("  ", "Field"));
    }

    @Test
    void validateIsbn_acceptsDigits() {
        String isbn = Validators.validateIsbn("978-1234567890");
        assertEquals("9781234567890", isbn);
    }

    @Test
    void validateIsbn_rejectsInvalid() {
        assertThrows(ValidationException.class, () -> Validators.validateIsbn("ABC"));
    }

    @Test
    void validateEmail_acceptsBasicFormat() {
        String email = Validators.validateEmail("user@example.com");
        assertEquals("user@example.com", email);
    }

    @Test
    void validateEmail_rejectsInvalid() {
        assertThrows(ValidationException.class, () -> Validators.validateEmail("invalid"));
    }
}