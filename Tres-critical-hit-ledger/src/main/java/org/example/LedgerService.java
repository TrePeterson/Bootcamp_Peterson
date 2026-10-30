package org.example;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Keeps ledger rules and searches separate from console prompts and file handling. */
public class LedgerService {
    private final FileManager fileManager;
    private final List<Transaction> transactions;

    public LedgerService(FileManager fileManager) throws IOException {
        this.fileManager = Objects.requireNonNull(fileManager, "File manager is required.");
        this.transactions = new ArrayList<>(fileManager.loadTransactions());
    }

    public void addTransaction(Transaction transaction) throws IOException {
        Objects.requireNonNull(transaction, "Transaction is required.");
        List<Transaction> updated = new ArrayList<>(transactions);
        updated.add(transaction);

        // A failed save must not appear as a successful deposit or payment in memory.
        fileManager.saveTransactions(updated);
        transactions.add(transaction);
    }

    public List<Transaction> getAll() {
        List<Transaction> sorted = new ArrayList<>(transactions);
        sorted.sort(Comparator.comparing(Transaction::getDate)
                .thenComparing(Transaction::getTime).reversed());
        return sorted;
    }

    public List<Transaction> getDeposits() {
        List<Transaction> result = new ArrayList<>();
        for (Transaction transaction : getAll()) {
            if (transaction.isDeposit()) {
                result.add(transaction);
            }
        }
        return result;
    }

    public List<Transaction> getPayments() {
        List<Transaction> result = new ArrayList<>();
        for (Transaction transaction : getAll()) {
            if (transaction.isPayment()) {
                result.add(transaction);
            }
        }
        return result;
    }

    public List<Transaction> reportMonthToDate(LocalDate today) {
        return betweenDates(today.withDayOfMonth(1), today);
    }

    public List<Transaction> reportPreviousMonth(LocalDate today) {
        YearMonth previousMonth = YearMonth.from(today).minusMonths(1);
        return betweenDates(previousMonth.atDay(1), previousMonth.atEndOfMonth());
    }

    public List<Transaction> reportYearToDate(LocalDate today) {
        return betweenDates(today.withDayOfYear(1), today);
    }

    public List<Transaction> reportPreviousYear(LocalDate today) {
        int previousYear = today.getYear() - 1;
        return betweenDates(LocalDate.of(previousYear, 1, 1),
                LocalDate.of(previousYear, 12, 31));
    }

    public List<Transaction> searchByVendor(String vendor) {
        String requestedVendor = normalize(vendor);
        List<Transaction> result = new ArrayList<>();
        if (requestedVendor.isEmpty()) {
            return result;
        }
        for (Transaction transaction : getAll()) {
            if (normalize(transaction.getVendor()).equals(requestedVendor)) {
                result.add(transaction);
            }
        }
        return result;
    }

    public List<Transaction> customSearch(LocalDate start, LocalDate end,
                                          String description, String vendor, BigDecimal amount) {
        if (start != null && end != null && start.isAfter(end)) {
            throw new IllegalArgumentException("Start date cannot be after end date.");
        }
        String descriptionFilter = normalize(description);
        String vendorFilter = normalize(vendor);
        List<Transaction> result = new ArrayList<>();

        for (Transaction transaction : getAll()) {
            if (start != null && transaction.getDate().isBefore(start)) {
                continue;
            }
            if (end != null && transaction.getDate().isAfter(end)) {
                continue;
            }
            if (!normalize(transaction.getDescription()).contains(descriptionFilter)) {
                continue;
            }
            if (!normalize(transaction.getVendor()).contains(vendorFilter)) {
                continue;
            }
            // compareTo treats 12.5 and 12.50 as the same monetary amount.
            if (amount != null && transaction.getAmount().compareTo(amount) != 0) {
                continue;
            }
            result.add(transaction);
        }
        return result;
    }

    private List<Transaction> betweenDates(LocalDate start, LocalDate end) {
        return customSearch(start, end, null, null, null);
    }

    private static String normalize(String text) {
        return text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
    }

    public BigDecimal getTotalDeposits(List<Transaction> entries) {
        BigDecimal total = new BigDecimal("0.00");
        for (Transaction transaction : entries) {
            if (transaction.isDeposit()) {
                total = total.add(transaction.getAmount());
            }
        }
        return total;
    }

    public BigDecimal getTotalPayments(List<Transaction> entries) {
        BigDecimal total = new BigDecimal("0.00");
        for (Transaction transaction : entries) {
            if (transaction.isPayment()) {
                total = total.add(transaction.getAmount().abs());
            }
        }
        return total;
    }

    public BigDecimal getNetChange(List<Transaction> entries) {
        return getTotalDeposits(entries).subtract(getTotalPayments(entries));
    }
}
