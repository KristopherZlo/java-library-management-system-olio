package lms.util;

import java.time.LocalDate;

public class SystemDateProvider implements DateProvider {
    @Override
    public LocalDate today() {
        return LocalDate.now();
    }
}