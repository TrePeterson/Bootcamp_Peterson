package FinancialCalculators;

import java.util.Locale;
import java.util.Scanner;

public class Calculator2 {
    public static void calculateFutureValue(Scanner scanner) {
        System.out.println();
        System.out.println("--- Future Value Calculator ---");

        double deposit = Main.readNumber(scanner, "Initial deposit: ", false);
        double annualRate = Main.readNumber(scanner, "Annual interest rate (%): ", true);
        double years = Main.readNumber(scanner, "Number of years: ", false);

        // Daily compounding uses 365 days per year.
        double dailyRate = annualRate / 100 / 365;
        double numberOfDays = years * 365;

        double futureValue = deposit * Math.pow(1 + dailyRate, numberOfDays);
        double interestEarned = futureValue - deposit;

        System.out.printf(Locale.US, "Future value: $%,.2f%n", futureValue);
        System.out.printf(Locale.US, "Total interest earned: $%,.2f%n", interestEarned);
    }
}