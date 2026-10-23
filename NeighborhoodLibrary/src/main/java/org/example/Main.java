package org.example;

import java.util.Locale;
import java.util.Scanner;

public class Main {

    //IDs identify books internally. Users search by title.
    private static final Book[] books = {
            new Book(1, "9781421585642", "One-Punch Man, Vol. 1"),
            new Book(2, "9781421585659", "One-Punch Man, Vol. 2"),
            new Book(3, "9781421564616", "One-Punch Man, Vol. 3"),
            new Book(4, "9781421569208", "One-Punch Man, Vol. 4"),
            new Book(5, "9781421569543", "One-Punch Man, Vol. 5"),
            new Book(6, "9781569319307", "Dragon Ball Z, Vol. 1"),
            new Book(7, "9781569319314", "Dragon Ball Z, Vol. 2"),
            new Book(8, "9781569319321", "Dragon Ball Z, Vol. 3"),
            new Book(9, "9781569319338", "Dragon Ball Z, Vol. 4"),
            new Book(10, "9781569319345", "Dragon Ball Z, Vol. 5"),
            new Book(11, "9781421501680", "Death Note, Vol. 1"),
            new Book(12, "9781421501697", "Death Note, Vol. 2"),
            new Book(13, "9781421501703", "Death Note, Vol. 3"),
            new Book(14, "9781421503318", "Death Note, Vol. 4"),
            new Book(15, "9781421506265", "Death Note, Vol. 5"),
            new Book(16, "9781506716015", "Cyberpunk 2077: Trauma Team"),
            new Book(17, "9781506725666", "Cyberpunk 2077: You Have My Word"),
            new Book(18, "9781506726274", "Cyberpunk 2077: Blackout"),
            new Book(19, "9781506726830", "Cyberpunk 2077: XOXO"),
            new Book(20, "9781506726847", "Cyberpunk 2077: Kickdown")
    };

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            displayHomeScreen();
            String choice = scanner.nextLine().trim();

            if (choice.equals("1")) {
                showAvailableBooks(scanner);
                printReturnToMenu();
            } else if (choice.equals("2")) {
                showCheckedOutBooks(scanner);
                printReturnToMenu();
            } else if (choice.equals("3")) {
                running = false;
                System.out.println("Thanks for visiting the Neighborhood Library!");
            } else {
                System.out.println("Invalid choice. Please enter 1, 2, or 3.");
            }
        }

        scanner.close();
    }

    private static void displayHomeScreen() {
        System.out.println("\n=== NEIGHBORHOOD LIBRARY ===");
        System.out.println("1. Show Available Books");
        System.out.println("2. Show Checked Out Books");
        System.out.println("3. Exit");
        System.out.print("Enter your choice: ");
    }

    private static void printReturnToMenu() {
        System.out.println("\n----------------------------------------");
        System.out.println("Returning to the main menu...");
        System.out.println("----------------------------------------");
    }

    private static void showAvailableBooks(Scanner scanner) {
        System.out.println("\n=== AVAILABLE BOOKS ===");
        displayBooksByStatus(false);
        checkOutBook(scanner);
    }

    private static void showCheckedOutBooks(Scanner scanner) {
        System.out.println("\n=== CHECKED OUT BOOKS ===");
        displayBooksByStatus(true);

        while (true) {
            System.out.println("C - Check in a book");
            System.out.println("X - Return to the home screen");
            System.out.print("Enter your choice: ");
            String choice = scanner.nextLine().trim();

            if (choice.equalsIgnoreCase("C")) {
                checkInBook(scanner);
                return;
            } else if (choice.equalsIgnoreCase("X")) {
                return;
            } else {
                System.out.println("Invalid choice. Please enter C or X.");
            }
        }
    }

    //true shows checked-out books; false shows available books.
    private static void displayBooksByStatus(boolean checkedOut) {
        boolean found = false;
        String heading = "Title | ISBN";

        if (checkedOut) {
            heading += " | Borrower";
        }
        System.out.println(heading);

        for (Book book : books) {
            if (book.isCheckedOut() == checkedOut) {
                System.out.println(formatBookDetails(book, checkedOut));
                found = true;
            }
        }

        if (!found) {
            if (checkedOut) {
                System.out.println("No books are currently checked out.");
            } else {
                System.out.println("No books are currently available.");
            }
        }
    }

    private static void checkOutBook(Scanner scanner) {
        Book book = findBookByTitle(scanner, false);

        if (book == null) {
            return;
        }

        System.out.println("\nSelected: " + book.getTitle());
        System.out.print("Enter your name, or X to return home: ");
        String name = scanner.nextLine().trim();

        if (name.equalsIgnoreCase("X")) {
            return;
        }

        if (name.isEmpty()) {
            System.out.println("The borrower's name cannot be blank.");
            return;
        }

        book.checkOut(name);
        System.out.println("\nCheckout complete!");
        System.out.println("\"" + book.getTitle() + "\" is now checked out to "
                + book.getCheckedOutTo() + ".");
    }

    private static void checkInBook(Scanner scanner) {
        Book book = findBookByTitle(scanner, true);

        if (book == null) {
            return;
        }

        book.checkIn();
        System.out.println("\nReturn complete!");
        System.out.println("\"" + book.getTitle() + "\" is now available to borrow.");
    }

    private static String formatBookDetails(Book book, boolean showBorrower) {
        String details = book.getTitle() + " | " + book.getIsbn();

        if (showBorrower) {
            details += " | " + book.getCheckedOutTo();
        }

        return details;
    }

    //Search available books for checkout, or borrowed books for check-in.
    private static Book findBookByTitle(Scanner scanner, boolean checkedOut) {
        System.out.print("Enter a title or part of a title (X to return home): ");
        String input = scanner.nextLine().trim();

        if (input.equalsIgnoreCase("X")) {
            return null;
        }

        String searchText = normalizeTitle(input);

        if (searchText.isEmpty()) {
            System.out.println("Please enter a title or part of a title.");
            return null;
        }

        Book[] matches = new Book[books.length];
        int matchCount = 0;
        boolean titleExists = false;

        for (Book book : books) {
            if (normalizeTitle(book.getTitle()).contains(searchText)) {
                titleExists = true;

                if (book.isCheckedOut() == checkedOut) {
                    matches[matchCount] = book;
                    matchCount++;
                }
            }
        }

        if (matchCount == 0) {
            if (!titleExists) {
                System.out.println("No titles match \"" + input + "\".");
            } else if (checkedOut) {
                System.out.println("Matching books are already checked in.");
            } else {
                System.out.println("Matching books are already checked out.");
            }

            return null;
        }

        if (matchCount == 1) {
            return matches[0];
        }

        System.out.println("\nSeveral titles match your search:");

        for (int i = 0; i < matchCount; i++) {
            System.out.println((i + 1) + ". "
                    + formatBookDetails(matches[i], checkedOut));
        }

        //These numbers select search results; they are not book IDs.
        while (true) {
            System.out.print("Enter the result number (1-" + matchCount
                    + "), or X to return home: ");

            String choice = scanner.nextLine().trim();

            if (choice.equalsIgnoreCase("X")) {
                return null;
            }

            int selection;

            try {
                selection = Integer.parseInt(choice);
            } catch (NumberFormatException exception) {
                System.out.println("Please enter a listed result number or X.");
                continue;
            }

            if (selection >= 1 && selection <= matchCount) {
                return matches[selection - 1];
            }

            System.out.println("Please choose a result from 1 to " + matchCount + ".");
        }
    }

    //Ignore capitalization, spaces and punctuation when matching titles.
    private static String normalizeTitle(String title) {
        return title.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}