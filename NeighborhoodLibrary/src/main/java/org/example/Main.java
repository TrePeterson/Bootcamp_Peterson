package org.example;

import java.util.Scanner;

public class Main {

    //Each book has a different ID. All books start available.
    private static final Book[] books = {
            new Book(1, "9780590353427", "Harry Potter and the Sorcerer's Stone"),
            new Book(2, "9780547928227", "The Hobbit"),
            new Book(3, "9780451524935", "1984"),
            new Book(4, "9780451526342", "Animal Farm"),
            new Book(5, "9780061120084", "To Kill a Mockingbird"),
            new Book(6, "9780743273565", "The Great Gatsby"),
            new Book(7, "9780141439518", "Pride and Prejudice"),
            new Book(8, "9780141441146", "Jane Eyre"),
            new Book(9, "9780486282114", "Frankenstein"),
            new Book(10, "9780486411095", "Dracula"),
            new Book(11, "9780147514011", "Little Women"),
            new Book(12, "9780061124952", "Charlotte's Web"),
            new Book(13, "9780064471046", "The Lion, the Witch and the Wardrobe"),
            new Book(14, "9780061122415", "The Alchemist"),
            new Book(15, "9780439023481", "The Hunger Games"),
            new Book(16, "9780439023498", "Catching Fire"),
            new Book(17, "9780439023511", "Mockingjay"),
            new Book(18, "9780786838653", "The Lightning Thief"),
            new Book(19, "9781423103349", "The Sea of Monsters"),
            new Book(20, "9780064401883", "The Secret Garden")
    };

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            displayHomeScreen();
            String choice = scanner.nextLine().trim();

            if (choice.equals("1")) {
                showAvailableBooks(scanner);
            } else if (choice.equals("2")) {
                showCheckedOutBooks(scanner);
            } else if (choice.equals("3")) {
                running = false;
                System.out.println("Thanks for visiting Tre's Neighborhood Library!");
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
        String heading = "ID | ISBN | Title";

        if (checkedOut) {
            heading += " | Borrower";
        }
        System.out.println(heading);

        for (Book book : books) {
            if (book.isCheckedOut() == checkedOut) {
                String details = book.getId() + " | " + book.getIsbn()
                        + " | " + book.getTitle();

                if (checkedOut) {
                    details += " | " + book.getCheckedOutTo();
                }

                System.out.println(details);
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
        System.out.print("Enter a book ID to check out, or X to return home: ");
        String input = scanner.nextLine().trim();

        if (input.equalsIgnoreCase("X")) {
            return;
        }

        Book book = findBookById(input);
        if (book == null) {
            return;
        }

        if (book.isCheckedOut()) {
            System.out.println("That book is already checked out. Returning home.");
            return;
        }

        System.out.print("Enter your name, or X to return home: ");
        String name = scanner.nextLine().trim();

        if (name.equalsIgnoreCase("X")) {
            return;
        }

        if (name.isEmpty()) {
            System.out.println("The borrower's name cannot be blank. Returning home.");
            return;
        }

        book.checkOut(name);
        System.out.println("Checked out: " + book.getTitle() + " to "
                + book.getCheckedOutTo() + ". Returning home.");
    }

    private static void checkInBook(Scanner scanner) {
        System.out.print("Enter the book ID to check in, or X to return home: ");
        String input = scanner.nextLine().trim();

        if (input.equalsIgnoreCase("X")) {
            return;
        }

        Book book = findBookById(input);
        if (book == null) {
            return;
        }

        if (!book.isCheckedOut()) {
            System.out.println("That book is already checked in. Returning home.");
            return;
        }

        book.checkIn();
        System.out.println("Checked in: " + book.getTitle() + ". Returning home.");
    }

    //Both checkout and check-in use this same lookup method.
    private static Book findBookById(String input) {
        int bookId;

        //Handle text or numbers too large for an int without crashing.
        try {
            bookId = Integer.parseInt(input);
        } catch (NumberFormatException exception) {
            System.out.println("Please enter a whole-number book ID. Returning home.");
            return null;
        }

        for (Book book : books) {
            if (book.getId() == bookId) {
                return book;
            }
        }

        System.out.println("No book has ID " + bookId + ". Returning home.");
        return null;
    }
}