package FinancialCalculators;

import java.util.Locale;
import java.util.Scanner;

public class Calculator3 {
    public static void calculatePresentValue(Scanner scanner) {
        System.out.println();
        System.out.println("--- Present Value of an Annuity Calculator ---");

        double monthlyPayout = Main.readNumber(scanner, "Monthly payout: ", false);
        double annualRate = Main.readNumber(scanner, "Expected annual interest rate (%): ", true);
        double years = Main.readNumber(scanner, "Number of years: ", false);

        double monthlyRate = annualRate / 100 / 12;
        double numberOfPayments = years * 12;

        double presentValue;

        // An ordinary annuity pays at the end of each month.
        if (monthlyRate == 0) {
            presentValue = monthlyPayout * numberOfPayments;
        } else {
            presentValue = monthlyPayout
                    * (1 - Math.pow(1 + monthlyRate, -numberOfPayments))
                    / monthlyRate;
        }

        System.out.printf(Locale.US, "Present value: $%,.2f%n", presentValue);
    }
}