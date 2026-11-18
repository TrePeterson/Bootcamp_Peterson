package org.example;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/** Shared report styling; asset classes are responsible for the calculations. */
public final class ConsoleHelper {
    private static final int CONTENT_WIDTH = 72;
    private static final int LABEL_WIDTH = 20;
    private static final String RESET = "\u001B[0m";
    private static final String ORANGE = "\u001B[38;5;208m";
    private static final String CYAN = "\u001B[96m";
    private static final String RED = "\u001B[91m";
    private static final String WHITE = "\u001B[97m";
    private static final String GRAY = "\u001B[90m";
    private static final NumberFormat MONEY = NumberFormat.getCurrencyInstance(Locale.US);

    // IntelliJ's Run console can display ANSI even when System.console() is null.
    private static final boolean COLORS_ENABLED =
            Boolean.parseBoolean(System.getProperty("assets.color", "true"))
                    && System.getenv("NO_COLOR") == null;

    private ConsoleHelper() {
        // Use static helpers directly: ConsoleHelper.field(...).
    }

    public static void banner(int valuationYear) {
        border('=', ORANGE);
        centered("F I R E   F O R C E", ORANGE);
        centered("COMPANY 08 / ASSET REGISTRY", CYAN);
        centered("PROPERTY + FLEET REPORT", WHITE);
        border('-', ORANGE);
        centered("Valuation year: " + valuationYear + "  /  Fictional training inventory", GRAY);
        border('=', ORANGE);
    }

    public static void startCard(String title) {
        System.out.println();
        border('-', CYAN);
        printLine(title, ORANGE);
        border('-', CYAN);
    }

    public static void field(String label, String value) {
        printLine(String.format("%-" + LABEL_WIDTH + "s %s", label, value), WHITE);
    }

    public static void money(String label, BigDecimal amount) {
        // Pad the plain text first so invisible ANSI codes don't affect alignment.
        String row = String.format("%-" + LABEL_WIDTH + "s %"
                + (CONTENT_WIDTH - LABEL_WIDTH - 1) + "s", label, MONEY.format(amount));
        printLine(row, CYAN);
    }

    public static void endCard() {
        border('-', CYAN);
    }

    public static void summary(int assetCount, BigDecimal originalCost, BigDecimal currentValue) {
        startCard("COMPANY 08 / INVENTORY TOTALS");
        field("Assets accounted for:", String.valueOf(assetCount));
        money("Original investment:", originalCost);
        money("Current value:", currentValue);
        endCard();
        System.out.println();
        System.out.println(color("[COMPLETE] ", ORANGE)
                + "Report ready. All " + assetCount + " assets valued.");
        System.out.println(color("LATOM. / End of report", RED));
    }

    private static void centered(String text, String shade) {
        int padding = Math.max(0, (CONTENT_WIDTH - text.length()) / 2);
        printLine(" ".repeat(padding) + text, shade);
    }

    private static void border(char character, String shade) {
        System.out.println(color("+" + String.valueOf(character).repeat(CONTENT_WIDTH + 2) + "+", shade));
    }

    private static void printLine(String text, String shade) {
        // Long descriptions wrap inside the card instead of breaking its border.
        while (text.length() > CONTENT_WIDTH) {
            int splitAt = text.lastIndexOf(' ', CONTENT_WIDTH);
            if (splitAt <= 0) {
                splitAt = CONTENT_WIDTH;
            }
            printPaddedLine(text.substring(0, splitAt), shade);
            text = text.substring(splitAt).stripLeading();
        }
        printPaddedLine(text, shade);
    }

    private static void printPaddedLine(String text, String shade) {
        String padded = String.format("%-" + CONTENT_WIDTH + "s", text);
        System.out.println(color("| ", CYAN) + color(padded, shade) + color(" |", CYAN));
    }

    private static String color(String text, String shade) {
        return COLORS_ENABLED ? shade + text + RESET : text;
    }
}
