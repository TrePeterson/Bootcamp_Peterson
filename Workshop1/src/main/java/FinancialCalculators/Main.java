package FinancialCalculators;

import java.util.Locale;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        System.out.println("Welcome to Financial Calculators!");
        System.out.println("Enter numbers without dollar signs, percent signs, or commas.");

        while (running) {
            System.out.println();
            System.out.println("1. Mortgage Calculator");
            System.out.println("2. Future Value Calculator");
            System.out.println("3. Present Value of an Annuity Calculator");
            System.out.println("0. Exit");
            System.out.print("Select a calculator: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    calculateMortgage(scanner);
                    break;

                case "2":
                    Calculator2.calculateFutureValue(scanner);
                    break;

                case "3":
                    Calculator3.calculatePresentValue(scanner);
                    break;

                case "0":
                    running = false;
                    System.out.println("Thank you for using Tre's Financial Calculators!");
                    break;

                default:
                    System.out.println("Please enter 1, 2, 3, or 0.");
            }
        }

        scanner.close();
    }

    private static void calculateMortgage(Scanner scanner) {
        System.out.println();
        System.out.println("--- Mortgage Calculator ---");

        double principal = readNumber(scanner, "Loan amount: ", false);
        double annualRate = readNumber(scanner, "Annual interest rate (%): ", true);
        double years = readNumber(scanner, "Loan length in years: ", false);

        // Convert the annual percentage to a monthly decimal rate.
        double monthlyRate = annualRate / 100 / 12;
        double numberOfPayments = years * 12;

        double monthlyPayment;
        double totalInterest;

        if (monthlyRate == 0) {
            monthlyPayment = principal / numberOfPayments;
            totalInterest = 0;
        } else {
            double growthFactor = Math.pow(1 + monthlyRate, numberOfPayments);

            monthlyPayment = principal * monthlyRate * growthFactor
                    / (growthFactor - 1);

            totalInterest = monthlyPayment * numberOfPayments - principal;
        }

        System.out.printf(Locale.US, "Monthly payment: $%,.2f%n", monthlyPayment);
        System.out.printf(Locale.US, "Total interest paid: $%,.2f%n", totalInterest);
        System.out.println("Payments include principal and interest only; taxes and insurance are excluded.");
    }

    // Shared by all three calculator classes.
    public static double readNumber(Scanner scanner, String prompt, boolean allowZero) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();

            try {
                double value = Double.parseDouble(input);

                if (Double.isFinite(value)
                        && (value > 0 || (allowZero && value == 0))) {
                    return value;
                }

                if (allowZero) {
                    System.out.println("Please enter zero or a positive number.");
                } else {
                    System.out.println("Please enter a number greater than zero.");
                }
            } catch (NumberFormatException exception) {
                System.out.println("Please enter a number, such as 1000 or 7.625.");
            }
        }
    }
}