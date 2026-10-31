package org.example;

import de.vandermeer.asciitable.AT_Row;
import de.vandermeer.asciitable.AsciiTable;
import de.vandermeer.asciitable.CWC_FixedWidth;
import de.vandermeer.asciithemes.a7.A7_Grids;
import de.vandermeer.skb.interfaces.transformers.textformat.TextAlignment;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Shared display tools. This class formats output; it never saves or changes transactions. */
public final class ConsoleHelper {
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
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private ConsoleHelper() {
        // Call these utility methods directly, such as ConsoleHelper.printHeader(...).
    }

    public static void success(String message) {
        System.out.println(color(GREEN, message));
    }

    public static void error(String message) {
        System.out.println(color(RED, message));
    }

    public static void notice(String message) {
        System.out.println(color(GOLD, message));
    }

    public static void prompt(String message) {
        System.out.print(color(CYAN, message));
    }

    public static void printHeader(String title) {
        System.out.println();
        System.out.println(color(CYAN, DIVIDER));
        System.out.println(color(BOLD + PURPLE, title));
        System.out.println(color(CYAN, DIVIDER));
    }

    public static String money(BigDecimal amount) {
        return String.format(Locale.US, "%s$%,.2f", amount.signum() < 0 ? "-" : "", amount.abs());
    }

    public static void printTransactions(List<Transaction> entries) {
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

    public static void printTotals(BigDecimal totalDeposits, BigDecimal totalPayments,
                                   BigDecimal netChange) {
        AsciiTable table = createTable(27, 25);
        table.addRule();
        table.addRow("TOTALS FOR THIS VIEW", "Amount");
        table.addRule();
        AT_Row deposits = table.addRow("Deposits", money(totalDeposits));
        AT_Row payments = table.addRow("Payments (money out)", money(totalPayments));
        AT_Row netChangeRow = table.addRow("Net change", money(netChange));
        table.addRule();
        table.setPaddingLeftRight(1);
        table.setTextAlignment(TextAlignment.LEFT);
        for (AT_Row row : List.of(deposits, payments, netChangeRow)) {
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
}
