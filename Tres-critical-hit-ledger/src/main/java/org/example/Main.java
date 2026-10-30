package org.example;

import de.vandermeer.asciitable.AT_Row;
import de.vandermeer.asciitable.AsciiTable;
import de.vandermeer.asciitable.CWC_FixedWidth;
import de.vandermeer.asciithemes.a7.A7_Grids;
import de.vandermeer.skb.interfaces.transformers.textformat.TextAlignment;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

public class Main {
    private static final String APP_NAME = "TRE'S CRITICAL HIT LEDGER";
    private static final String DIVIDER = "=".repeat(104);
    private static final boolean USE_COLOR = Boolean.parseBoolean(
            System.getProperty("ledger.color", "true"));
    private static final String RESET = "\u001B[0m";
    private static final String CYAN = "\u001B[36m";
    private static final String GREEN = "\u001B[32m";
    private static final String RED = "\u001B[31m";
    private static final String GOLD = "\u001B[33m";
    private static final String PURPLE = "\u001B[35m";
    private static final String BOLD = "\u001B[1m";
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss").withResolverStyle(ResolverStyle.STRICT);
    private static boolean inputClosed;

    public static void main(String[] args) {
        FileManager fileManager;
        LedgerService ledger;

        try {
            fileManager = new FileManager();
            ledger = new LedgerService(fileManager);
        } catch (IOException | IllegalArgumentException exception) {
            System.out.println(color(RED, "The ledger could not be opened: " + exception.getMessage()));
            System.out.println("Check transactions.csv and restart. Existing history has not been replaced.");
            return;
        }

        printHeader(APP_NAME);
        System.out.println(color(GOLD, "Track your gold. Know your numbers."));
        System.out.println(color(GREEN, "Loaded " + ledger.getAll().size() + " transaction(s). Your ledger is ready."));
        System.out.println("Data file: " + fileManager.getFilePath());

        try (Scanner scanner = new Scanner(System.in)) {
            runHome(scanner, ledger);
        }

        System.out.println();
        System.out.println("Session closed. Your saved transactions will be here next time.");
        System.out.println("Thanks for using " + APP_NAME + "!");
    }

    private static void runHome(Scanner scanner, LedgerService ledger) {
        while (!inputClosed) {
            printHeader("HOME | " + APP_NAME);
            List<Transaction> entries = ledger.getAll();
            System.out.println("Recorded transactions: " + entries.size()
                    + " | Recorded net change: " + money(ledger.getNetChange(entries)));
            System.out.println("D) Add deposit");
            System.out.println("P) Make payment");
            System.out.println("L) View ledger and reports");
            System.out.println("X) Exit");

            String choice = readLine(scanner, "Your choice: ");
            if (choice == null) {
                return;
            }

            switch (choice.toUpperCase(Locale.ROOT)) {
                case "D" -> addTransaction(scanner, ledger, true);
                case "P" -> addTransaction(scanner, ledger, false);
                case "L" -> runLedger(scanner, ledger);
                case "X" -> { return; }
                default -> System.out.println("Please choose D, P, L, or X.");
            }
        }
    }

    private static void runLedger(Scanner scanner, LedgerService ledger) {
        while (!inputClosed) {
            printHeader("LEDGER | Every transaction tells a story");
            System.out.println("A) All transactions");
            System.out.println("D) Deposits only");
            System.out.println("P) Payments only");
            System.out.println("R) Reports and custom search");
            System.out.println("H) Return home");

            String choice = readLine(scanner, "Your choice: ");
            if (choice == null) {
                return;
            }

            switch (choice.toUpperCase(Locale.ROOT)) {
                case "A" -> showResults(scanner, ledger, "ALL TRANSACTIONS", ledger.getAll());
                case "D" -> showResults(scanner, ledger, "DEPOSITS", ledger.getDeposits());
                case "P" -> showResults(scanner, ledger, "PAYMENTS", ledger.getPayments());
                case "R" -> runReports(scanner, ledger);
                case "H", "X" -> { return; }
                default -> System.out.println("Please choose A, D, P, R, or H.");
            }
        }
    }

    private static void runReports(Scanner scanner, LedgerService ledger) {
        while (!inputClosed) {
            LocalDate today = LocalDate.now();
            printHeader("REPORTS | Follow the money");
            System.out.println("Today's date: " + today);
            System.out.println("1) Month to date");
            System.out.println("2) Previous month");
            System.out.println("3) Year to date");
            System.out.println("4) Previous year");
            System.out.println("5) Search by vendor");
            System.out.println("6) Custom search");
            System.out.println("0) Back to ledger");

            String choice = readLine(scanner, "Your choice: ");
            if (choice == null) {
                return;
            }

            switch (choice.toUpperCase(Locale.ROOT)) {
                case "1" -> showResults(scanner, ledger,
                        "MONTH TO DATE | " + today.withDayOfMonth(1) + " through " + today,
                        ledger.reportMonthToDate(today));
                case "2" -> {
                    YearMonth previous = YearMonth.from(today).minusMonths(1);
                    showResults(scanner, ledger,
                            "PREVIOUS MONTH | " + previous.atDay(1) + " through " + previous.atEndOfMonth(),
                            ledger.reportPreviousMonth(today));
                }
                case "3" -> showResults(scanner, ledger,
                        "YEAR TO DATE | " + today.withDayOfYear(1) + " through " + today,
                        ledger.reportYearToDate(today));
                case "4" -> {
                    int previousYear = today.getYear() - 1;
                    showResults(scanner, ledger,
                            "PREVIOUS YEAR | " + previousYear + "-01-01 through " + previousYear + "-12-31",
                            ledger.reportPreviousYear(today));
                }
                case "5" -> searchVendor(scanner, ledger);
                case "6" -> customSearch(scanner, ledger);
                case "0", "X" -> { return; }
                default -> System.out.println("Please choose a report from 1 to 6, or 0 to go back.");
            }
        }
    }

    private static void addTransaction(Scanner scanner, LedgerService ledger, boolean deposit) {
        printHeader(deposit ? "ADD DEPOSIT | Money in" : "MAKE PAYMENT | Money out");
        System.out.println("Enter X at any prompt to cancel. Nothing is saved until you confirm.");
        LocalDateTime now = LocalDateTime.now().withNano(0);

        String dateText = readDateText(scanner, "Date [Enter for " + now.toLocalDate() + "]: ");
        if (dateText == null) { canceled(); return; }
        LocalDate date = dateText.isEmpty() ? now.toLocalDate() : LocalDate.parse(dateText);

        String timeText = readTimeText(scanner, "Time [Enter for " + now.toLocalTime().format(TIME_FORMAT) + "]: ");
        if (timeText == null) { canceled(); return; }
        LocalTime time = timeText.isEmpty() ? now.toLocalTime() : LocalTime.parse(timeText, TIME_FORMAT);

        String description = readText(scanner, "Description: ", false);
        if (description == null) { canceled(); return; }
        String vendor = readText(scanner, "Vendor / customer: ", false);
        if (vendor == null) { canceled(); return; }

        System.out.println(deposit
                ? "Enter the amount received as a positive number, such as 25.00."
                : "Enter the amount paid as a positive number. We will record it as a payment.");
        String amountText = readMoneyText(scanner, "Amount: ", false, false);
        if (amountText == null) { canceled(); return; }
        BigDecimal amount = new BigDecimal(amountText);
        if (!deposit) {
            amount = amount.negate();
        }

        Transaction transaction = new Transaction(date, time, description, vendor, amount);
        printHeader("REVIEW BEFORE SAVING");
        printTransactionTable(List.of(transaction));
        if (!confirm(scanner, "Save this " + (deposit ? "deposit" : "payment") + "? (Y/N): ")) {
            canceled();
            return;
        }

        try {
            ledger.addTransaction(transaction);
            System.out.println(color(GREEN, "SAVED: " + (deposit ? "Deposit" : "Payment") + " of " + money(amount.abs())
                    + " for " + description + "."));
            System.out.println("Your transaction is recorded in transactions.csv.");
            System.out.println("Recorded net change: " + money(ledger.getNetChange(ledger.getAll())));
        } catch (IOException exception) {
            System.out.println(color(RED, "NOT SAVED: " + exception.getMessage()));
            System.out.println("This transaction was not added to the ledger. Resolve the file problem before retrying.");
        }
        pause(scanner);
    }

    private static void searchVendor(Scanner scanner, LedgerService ledger) {
        printHeader("SEARCH BY VENDOR");
        System.out.println("Enter the full vendor name. Capitalization does not matter.");
        System.out.println("For part of a name, use Custom Search. Enter X to cancel.");
        String vendor = readText(scanner, "Vendor: ", false);
        if (vendor == null) { canceled(); return; }
        showResults(scanner, ledger, "VENDOR | " + vendor, ledger.searchByVendor(vendor));
    }

    private static void customSearch(Scanner scanner, LedgerService ledger) {
        printHeader("CUSTOM SEARCH | Find the transactions you need");
        System.out.println("Press Enter to skip any filter, or X to cancel.");
        System.out.println("Results must match ALL filters you provide. Dates include both endpoints.");
        System.out.println("Description and vendor accept partial names and ignore capitalization.");

        String startText = readDateText(scanner, "Start date (YYYY-MM-DD): ");
        if (startText == null) { canceled(); return; }
        LocalDate start = startText.isEmpty() ? null : LocalDate.parse(startText);

        LocalDate end;
        while (true) {
            String endText = readDateText(scanner, "End date (YYYY-MM-DD): ");
            if (endText == null) { canceled(); return; }
            end = endText.isEmpty() ? null : LocalDate.parse(endText);
            if (start == null || end == null || !end.isBefore(start)) {
                break;
            }
            System.out.println("The end date must be on or after " + start + ". Please try again.");
        }

        String description = readText(scanner, "Description contains: ", true);
        if (description == null) { canceled(); return; }
        String vendor = readText(scanner, "Vendor contains: ", true);
        if (vendor == null) { canceled(); return; }
        System.out.println("Amount is an exact match: 25.00 for a deposit, -25.00 for a payment.");
        String amountText = readMoneyText(scanner, "Amount: ", true, true);
        if (amountText == null) { canceled(); return; }
        BigDecimal amount = amountText.isEmpty() ? null : new BigDecimal(amountText);

        List<Transaction> results = ledger.customSearch(start, end, description, vendor, amount);
        showResults(scanner, ledger, "CUSTOM SEARCH RESULTS", results);
    }

    private static void showResults(Scanner scanner, LedgerService ledger,
                                    String title, List<Transaction> entries) {
        printHeader(title);
        if (entries.isEmpty()) {
            System.out.println(color(GOLD, "No transactions found for this view. Try another report or different filters."));
        } else {
            printTransactionTable(entries);
            System.out.println("Showing " + entries.size() + " transaction(s), newest first.");
        }
        printTotals(ledger, entries);
        pause(scanner);
    }

    private static void printTransactionTable(List<Transaction> entries) {
        AsciiTable table = createTable(12, 10, 34, 24, 18);
        table.addRule();
        table.addRow("Date", "Time", "Description", "Vendor / Customer", "Amount");
        table.addRule();
        List<AT_Row> rows = new ArrayList<>();
        for (Transaction entry : entries) {
            rows.add(table.addRow(entry.getDate(), entry.getTime().format(TIME_FORMAT),
                    entry.getDescription(), entry.getVendor(), money(entry.getAmount())));
            table.addRule();
        }
        table.setPaddingLeftRight(1);
        table.setTextAlignment(TextAlignment.LEFT);
        for (AT_Row row : rows) {
            row.getCells().get(4).getContext().setTextAlignment(TextAlignment.RIGHT);
        }
        printColoredTable(table);
    }

    private static void printTotals(LedgerService ledger, List<Transaction> entries) {
        AsciiTable table = createTable(27, 25);
        table.addRule();
        table.addRow("TOTALS FOR THIS VIEW", "Amount");
        table.addRule();
        AT_Row deposits = table.addRow("Deposits", money(ledger.getTotalDeposits(entries)));
        AT_Row payments = table.addRow("Payments (money out)", money(ledger.getTotalPayments(entries)));
        AT_Row netChange = table.addRow("Net change", money(ledger.getNetChange(entries)));
        table.addRule();
        table.setPaddingLeftRight(1);
        table.setTextAlignment(TextAlignment.LEFT);
        for (AT_Row row : List.of(deposits, payments, netChange)) {
            row.getCells().get(1).getContext().setTextAlignment(TextAlignment.RIGHT);
        }
        printColoredTable(table);
        System.out.println("Net change = deposits minus payments for the displayed transactions.");
    }

    private static AsciiTable createTable(int... columnWidths) {
        AsciiTable table = new AsciiTable();
        CWC_FixedWidth widths = new CWC_FixedWidth();
        int totalWidth = columnWidths.length + 1;
        for (int width : columnWidths) {
            widths.add(width);
            totalWidth += width;
        }
        table.getRenderer().setCWC(widths);
        table.getContext().setWidth(totalWidth);
        // ASCII borders stay readable in terminals that do not use UTF-8.
        table.getContext().setGrid(A7_Grids.minusBarPlusEquals());
        return table;
    }

    private static void printColoredTable(AsciiTable table) {
        // Render first: invisible ANSI color codes must not affect column widths.
        String rendered = table.render();
        if (!USE_COLOR) {
            System.out.println(rendered);
            return;
        }

        boolean header = true;
        for (String line : rendered.split("\\R")) {
            if (line.startsWith("+")) {
                System.out.println(color(CYAN, line));
            } else if (line.startsWith("|")) {
                System.out.println(colorTableRow(line, header));
                header = false;
            } else {
                System.out.println(line);
            }
        }
    }

    private static String colorTableRow(String line, boolean header) {
        String[] cells = line.split("\\|", -1);
        String label = cells[1].trim();
        StringBuilder result = new StringBuilder(color(CYAN, "|"));
        for (int index = 1; index < cells.length - 1; index++) {
            String cell = cells[index];
            if (header) {
                cell = color(BOLD + PURPLE, cell);
            } else if (index == cells.length - 2) {
                String amount = cell.trim();
                String shade = amount.startsWith("-") || label.equals("Payments (money out)")
                        ? RED : GREEN;
                if (amount.equals("$0.00")) {
                    shade = GOLD;
                }
                cell = color(BOLD + shade, cell);
            } else if (label.equals("Net change")) {
                cell = color(BOLD + GOLD, cell);
            }
            result.append(cell).append(color(CYAN, "|"));
        }
        return result.toString();
    }

    private static String color(String shade, String text) {
        return USE_COLOR ? shade + text + RESET : text;
    }

    private static String money(BigDecimal amount) {
        return String.format(Locale.US, "%s$%,.2f", amount.signum() < 0 ? "-" : "", amount.abs());
    }

    private static String readLine(Scanner scanner, String prompt) {
        System.out.print(color(CYAN, prompt));
        if (!scanner.hasNextLine()) {
            inputClosed = true;
            return null;
        }
        return scanner.nextLine().trim();
    }

    private static String readText(Scanner scanner, String prompt, boolean allowBlank) {
        while (true) {
            String value = readLine(scanner, prompt);
            if (value == null || value.equalsIgnoreCase("X")) {
                return null;
            }
            if (value.isEmpty()) {
                if (allowBlank) { return ""; }
                System.out.println("Please enter a value, or X to cancel.");
                continue;
            }
            if (value.length() > Transaction.MAX_TEXT_LENGTH) {
                System.out.println("Please use " + Transaction.MAX_TEXT_LENGTH + " characters or fewer.");
                continue;
            }
            // A pipe would create an extra column in our transaction file.
            if (value.contains("|") || value.chars().anyMatch(Character::isISOControl)) {
                System.out.println("Please use ordinary text without pipes (|), tabs, or control characters.");
                continue;
            }
            return value;
        }
    }

    private static String readDateText(Scanner scanner, String prompt) {
        while (true) {
            String value = readLine(scanner, prompt);
            if (value == null || value.equalsIgnoreCase("X")) { return null; }
            if (value.isEmpty()) { return ""; }
            try {
                if (!value.matches("\\d{4}-\\d{2}-\\d{2}")) {
                    throw new DateTimeParseException("Expected YYYY-MM-DD", value, 0);
                }
                LocalDate date = LocalDate.parse(value);
                if (date.getYear() < 1) {
                    throw new DateTimeParseException("Year must be positive", value, 0);
                }
                return value;
            } catch (DateTimeParseException exception) {
                System.out.println("Use a real date in YYYY-MM-DD format, such as 2026-10-30.");
            }
        }
    }

    private static String readTimeText(Scanner scanner, String prompt) {
        while (true) {
            String value = readLine(scanner, prompt);
            if (value == null || value.equalsIgnoreCase("X")) { return null; }
            if (value.isEmpty()) { return ""; }
            try {
                if (!value.matches("\\d{2}:\\d{2}:\\d{2}")) {
                    throw new DateTimeParseException("Expected HH:mm:ss", value, 0);
                }
                LocalTime.parse(value, TIME_FORMAT);
                return value;
            } catch (DateTimeParseException exception) {
                System.out.println("Use a real 24-hour time with seconds, such as 14:30:00.");
            }
        }
    }

    private static String readMoneyText(Scanner scanner, String prompt,
                                        boolean allowBlank, boolean allowSigned) {
        while (true) {
            String value = readLine(scanner, prompt);
            if (value == null || value.equalsIgnoreCase("X")) { return null; }
            if (value.isEmpty() && allowBlank) { return ""; }
            value = value.replaceFirst("^([+-]?)\\$", "$1");
            try {
                if (value.length() > 20 || !value.matches("[+-]?\\d+(\\.\\d{1,2})?")) {
                    throw new NumberFormatException("Expected dollars and cents");
                }
                BigDecimal amount = new BigDecimal(value).setScale(2);
                if (amount.signum() == 0 || (!allowSigned && amount.signum() < 0)
                        || amount.abs().compareTo(Transaction.MAX_AMOUNT) > 0) {
                    throw new NumberFormatException("Amount out of range");
                }
                return amount.toPlainString();
            } catch (NumberFormatException exception) {
                System.out.println(allowSigned
                        ? "Enter a nonzero amount, such as 25.00 or -25.00, with at most two decimal places."
                        : "Enter a positive amount, such as 25.00, with at most two decimal places.");
                System.out.println("Maximum amount: $999,999,999.99. Do not include commas. X cancels.");
            }
        }
    }

    private static boolean confirm(Scanner scanner, String prompt) {
        while (true) {
            String answer = readLine(scanner, prompt);
            if (answer == null) { return false; }
            switch (answer.toUpperCase(Locale.ROOT)) {
                case "Y", "YES" -> { return true; }
                case "N", "NO", "X" -> { return false; }
                default -> System.out.println("Please enter Y to save or N to cancel.");
            }
        }
    }

    private static void pause(Scanner scanner) {
        if (!inputClosed) {
            readLine(scanner, "Press Enter to continue...");
        }
    }

    private static void canceled() {
        System.out.println(color(GOLD, "Canceled. No transaction was saved."));
    }

    private static void printHeader(String title) {
        System.out.println();
        System.out.println(color(CYAN, DIVIDER));
        System.out.println(color(BOLD + PURPLE, title));
        System.out.println(color(CYAN, DIVIDER));
    }
}
