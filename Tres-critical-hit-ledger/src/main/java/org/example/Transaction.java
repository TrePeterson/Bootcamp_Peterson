package org.example;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** One immutable deposit or payment in the ledger. */
public final class Transaction {
    public static final int MAX_TEXT_LENGTH = 120;
    public static final BigDecimal MAX_AMOUNT = new BigDecimal("999999999.99");
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final LocalDate date;
    private final LocalTime time;
    private final String description;
    private final String vendor;
    private final BigDecimal amount;

    public Transaction(LocalDate date, LocalTime time, String description,
                       String vendor, BigDecimal amount) {
        if (date == null || time == null || amount == null) {
            throw new IllegalArgumentException("Date, time, and amount are required.");
        }
        if (date.getYear() < 1 || date.getYear() > 9999) {
            throw new IllegalArgumentException("Date must use a year from 0001 through 9999.");
        }
        this.date = date;
        this.time = time.withNano(0);
        this.description = validateText(description, "Description");
        this.vendor = validateText(vendor, "Vendor");

        try {
            // Reject fractional cents instead of silently changing the user's money.
            this.amount = amount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException("Amount must have no fractional cents.", ex);
        }
        if (this.amount.signum() == 0) {
            throw new IllegalArgumentException("Amount must be greater than zero in magnitude.");
        }
        if (this.amount.abs().compareTo(MAX_AMOUNT) > 0) {
            throw new IllegalArgumentException("Amount cannot exceed $" + MAX_AMOUNT + ".");
        }
    }

    private static String validateText(String value, String label) {
        if (value == null) {
            throw new IllegalArgumentException(label + " is required.");
        }
        // A pipe or line break would change the structure of a saved CSV record.
        if (value.indexOf('|') >= 0 || value.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException(label + " cannot contain pipes or control characters.");
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_TEXT_LENGTH) {
            throw new IllegalArgumentException(label + " must contain 1 to "
                    + MAX_TEXT_LENGTH + " characters.");
        }
        return trimmed;
    }

    public LocalDate getDate() {
        return date;
    }

    public LocalTime getTime() {
        return time;
    }

    public String getDescription() {
        return description;
    }

    public String getVendor() {
        return vendor;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public boolean isDeposit() {
        return amount.signum() > 0;
    }

    public boolean isPayment() {
        return amount.signum() < 0;
    }

    public String toCsvLine() {
        return date + "|" + time.format(FILE_TIME) + "|" + description
                + "|" + vendor + "|" + amount.toPlainString();
    }
}
