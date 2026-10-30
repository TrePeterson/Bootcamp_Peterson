package org.example;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;
import java.util.Scanner;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    private static final String STORE_NAME = "TRE'S CRITICAL HIT SHOP";
    private static final String LINE = "=".repeat(78);
    private static final String NL = System.lineSeparator();
    private static final Pattern MANGA_PART = Pattern.compile("\\b(volume|chapter) (\\d+)\\b");
    private static final Pattern NEGATIVE_NUMBER = Pattern.compile("(^|\\s)-\\s*\\d");

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            ArrayList<Product> products = FileManager.loadProducts();
            ShoppingCart cart = new ShoppingCart();

            title(STORE_NAME);
            System.out.println("Roll for loot. Find your favorites. Make them yours.");
            System.out.println("[READY] " + products.size() + " products loaded.");

            while (true) {
                title("HOME | CART: " + cart.getItemCount() + " ITEMS | " + money(cart.getTotal()));
                System.out.println("  1. Shop products");
                System.out.println("  2. View shopping cart");
                System.out.println("  3. Exit store");
                String choice = menuInput(scanner);

                if (choice == null) {
                    goodbye();
                    return;
                }

                switch (choice) {
                    case "1":
                        productsScreen(products, cart, scanner);
                        break;
                    case "2":
                        cartScreen(cart, scanner);
                        break;
                    case "3":
                        if (cart.isEmpty() || confirm(scanner,
                                "Leave without purchasing the items in your cart?")) {
                            goodbye();
                            return;
                        }
                        System.out.println("[CANCELED] Your cart is still here.");
                        break;
                    default:
                        System.out.println("[TRY AGAIN] Choose 1, 2, or 3.");
                }
            }
        } catch (IOException e) {
            System.out.println("[ERROR] The store could not load its catalog.");
            System.out.println("Details: " + e.getMessage());
            System.out.println("Place products.csv inside src/main/resources and run Main again.");
        }
    }

    private static void productsScreen(ArrayList<Product> products, ShoppingCart cart,
                                       Scanner scanner) {
        ArrayList<Product> visible = new ArrayList<>(products);

        while (true) {
            title("EXPLORE THE SHOP");
            displayProducts(visible);
            System.out.println("Showing " + visible.size() + " of " + products.size() + " products.");
            System.out.println("\n  1. Show all products");
            System.out.println("  2. Search by name");
            System.out.println("  3. Search by price range");
            System.out.println("  4. Browse a department");
            System.out.println("  5. Add to cart by name or SKU");
            System.out.println("  6. Return home");
            String choice = menuInput(scanner);

            if (choice == null || choice.equals("6") || choice.equalsIgnoreCase("X")) {
                return;
            }

            ArrayList<Product> results = null;
            switch (choice) {
                case "1":
                    visible = new ArrayList<>(products);
                    System.out.println("[RESET] Showing the full catalog.");
                    break;
                case "2":
                    results = searchByName(products, scanner);
                    break;
                case "3":
                    results = searchByPrice(products, scanner);
                    break;
                case "4":
                    results = searchByDepartment(products, scanner);
                    break;
                case "5":
                    addToCart(products, cart, scanner);
                    break;
                default:
                    System.out.println("[TRY AGAIN] Choose an option from 1 to 6.");
                    continue;
            }

            if (results != null) {
                visible = results;
                System.out.println("[SEARCH COMPLETE] Found " + results.size() + " product(s).");
            }
            pause(scanner);
        }
    }

    private static ArrayList<Product> searchByName(ArrayList<Product> products, Scanner scanner) {
        String search = readText(scanner, "Enter all or part of a product name");
        if (search == null) {
            canceled();
            return null;
        }

        return findMatchingProducts(products, search);
    }

    private static ArrayList<Product> searchByPrice(ArrayList<Product> products, Scanner scanner) {
        BigDecimal minimum = readMoney(scanner, "Minimum price");
        if (minimum == null) {
            canceled();
            return null;
        }

        BigDecimal maximum;
        while (true) {
            maximum = readMoney(scanner, "Maximum price");
            if (maximum == null) {
                canceled();
                return null;
            }
            if (maximum.compareTo(minimum) >= 0) {
                break;
            }
            System.out.println("[TRY AGAIN] Maximum must be at least " + money(minimum) + ".");
        }

        ArrayList<Product> matches = new ArrayList<>();
        for (Product product : products) {
            if (product.getPrice().compareTo(minimum) >= 0
                    && product.getPrice().compareTo(maximum) <= 0) {
                matches.add(product);
            }
        }
        return matches;
    }

    private static ArrayList<Product> searchByDepartment(ArrayList<Product> products,
                                                         Scanner scanner) {
        TreeSet<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Product product : products) {
            names.add(product.getDepartment());
        }
        ArrayList<String> departments = new ArrayList<>(names);
        if (departments.isEmpty()) {
            return new ArrayList<>();
        }

        System.out.println("\nChoose a department:");
        for (int index = 0; index < departments.size(); index++) {
            System.out.println("  " + (index + 1) + ". " + departments.get(index));
        }

        Integer selected;
        while (true) {
            selected = readQuantity(scanner, "Department number");
            if (selected == null) {
                canceled();
                return null;
            }
            if (selected <= departments.size()) {
                break;
            }
            System.out.println("[TRY AGAIN] Choose one of the listed departments.");
        }

        String department = departments.get(selected - 1);
        ArrayList<Product> matches = new ArrayList<>();
        for (Product product : products) {
            if (product.getDepartment().equalsIgnoreCase(department)) {
                matches.add(product);
            }
        }
        System.out.println("[DEPARTMENT] " + department);
        return matches;
    }

    private static void addToCart(ArrayList<Product> products, ShoppingCart cart, Scanner scanner) {
        Product selected = selectProductForCart(products, scanner);
        if (selected == null) {
            canceled();
            return;
        }

        System.out.println("Selected: " + selected.getName() + " | " + money(selected.getPrice()));
        Integer quantity = readQuantity(scanner, "Quantity to add");
        if (quantity == null) {
            canceled();
            return;
        }

        try {
            cart.addProduct(selected, quantity);
            System.out.println("[ADDED] " + quantity + " x " + selected.getName());
            System.out.println("Cart: " + cart.getItemCount() + " items | " + money(cart.getTotal()));
        } catch (IllegalArgumentException e) {
            System.out.println("[NOT ADDED] " + e.getMessage());
        }
    }

    private static Product selectProductForCart(ArrayList<Product> products, Scanner scanner) {
        System.out.println("\nFind your next favorite by typing part of its name.");
        System.out.println("Examples: death, one punch, DBZ, Karlach, or an exact SKU.");

        while (true) {
            String search = readText(scanner, "Product name or SKU");
            if (search == null) {
                return null;
            }

            ArrayList<Product> matches = findMatchingProducts(products, search);
            if (matches.isEmpty()) {
                System.out.println("[NO MATCH] Try another name, keyword, or SKU.");
                continue;
            }

            while (true) {
                if (matches.size() == 1) {
                    return matches.get(0);
                }

                System.out.println("\n[FOUND] Choose from these " + matches.size() + " matches:");
                for (int index = 0; index < matches.size(); index++) {
                    Product product = matches.get(index);
                    System.out.printf(Locale.US, "#%-3d %-8s | %-48s | %-14s | $%9s%n",
                            index + 1, product.getSku(), product.getName(),
                            product.getDepartment(), amount(product.getPrice()));
                }

                System.out.println("For manga, enter a volume/chapter: 2, vol 2, or chapter 2.");
                System.out.println("Or enter # plus a result number (like #1), a keyword, or a listed SKU.");
                System.out.println("Enter B to start a new search.");
                String answer = readText(scanner, "Which product or manga volume/chapter?");
                if (answer == null) {
                    return null;
                }
                if (answer.equalsIgnoreCase("B")) {
                    break;
                }
                if (NEGATIVE_NUMBER.matcher(answer).find()) {
                    System.out.println("[TRY AGAIN] Volume/chapter numbers cannot be negative.");
                    continue;
                }

                // #2 selects result 2. A bare 2 means manga volume/chapter 2.
                if (answer.startsWith("#")) {
                    try {
                        int resultNumber = Integer.parseInt(answer.substring(1).trim());
                        if (resultNumber >= 1 && resultNumber <= matches.size()) {
                            return matches.get(resultNumber - 1);
                        }
                    } catch (NumberFormatException e) {
                        // Keep the current choices visible when a number is invalid.
                    }
                    System.out.println("[TRY AGAIN] Choose #1 through #" + matches.size() + ".");
                    continue;
                }

                String normalized = normalizeSearch(answer);
                String number = null;
                String kind = null;
                Matcher requestedPart = MANGA_PART.matcher(normalized);
                if (normalized.matches("\\d+")) {
                    number = normalized;
                } else if (requestedPart.matches()) {
                    kind = requestedPart.group(1);
                    number = requestedPart.group(2);
                }

                ArrayList<Product> narrowed = new ArrayList<>();
                if (number != null) {
                    for (Product product : matches) {
                        if (matchesMangaPart(product, kind, number)) {
                            narrowed.add(product);
                        }
                    }
                } else {
                    narrowed = findMatchingProducts(matches, answer);
                }

                if (narrowed.isEmpty()) {
                    System.out.println("[NO MATCH] That choice is not in the current results.");
                    System.out.println("Try a listed volume/chapter, choose #number, or enter B to search again.");
                } else {
                    matches = narrowed;
                }
            }
        }
    }

    private static boolean matchesMangaPart(Product product, String kind, String number) {
        if (!product.getDepartment().equalsIgnoreCase("Manga")) {
            return false;
        }
        Matcher part = MANGA_PART.matcher(normalizeSearch(product.getName()));
        while (part.find()) {
            if ((kind == null || kind.equals(part.group(1))) && number.equals(part.group(2))) {
                return true;
            }
        }
        return false;
    }

    private static ArrayList<Product> findMatchingProducts(ArrayList<Product> products, String search) {
        ArrayList<Product> matches = new ArrayList<>();
        for (Product product : products) {
            if (product.getSku().equalsIgnoreCase(search.trim())) {
                matches.add(product);
                return matches;
            }
        }

        if (NEGATIVE_NUMBER.matcher(search).find()) {
            return matches;
        }
        String query = normalizeSearch(search);
        if (query.isEmpty()) {
            return matches;
        }
        String[] words = query.split(" ");
        for (Product product : products) {
            String name = normalizeSearch(product.getName());
            boolean allWordsMatch = true;
            for (String word : words) {
                // Numeric tokens match exactly: volume 2 must not match volume 20.
                boolean found = word.matches("\\d+")
                        ? (" " + name + " ").contains(" " + word + " ")
                        : name.contains(word);
                if (!found) {
                    allWordsMatch = false;
                    break;
                }
            }
            if (allWordsMatch) {
                matches.add(product);
            }
        }
        matches.sort(Comparator.comparing(Product::getName, String.CASE_INSENSITIVE_ORDER));
        return matches;
    }

    private static String normalizeSearch(String value) {
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("\\bdbz\\b", "dragon ball z")
                .replaceAll("\\bopm\\b", "one punch man")
                .replaceAll("\\bvol(?:ume)?\\.?\\s*(\\d+)\\b", " volume $1 ")
                .replaceAll("\\bch(?:apter)?\\.?\\s*(\\d+)\\b", " chapter $1 ")
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .replaceAll("\\b0+(?=\\d)", "")
                .trim()
                .replaceAll("\\s+", " ");
    }

    private static void cartScreen(ShoppingCart cart, Scanner scanner) {
        while (true) {
            title("YOUR LOOT BAG");
            System.out.print(formatCart(cart));
            System.out.println("\n  1. Check out");
            System.out.println("  2. Remove a product");
            System.out.println("  3. Return home");
            String choice = menuInput(scanner);

            if (choice == null || choice.equals("3") || choice.equalsIgnoreCase("X")) {
                return;
            }

            switch (choice) {
                case "1":
                    if (checkout(cart, scanner)) {
                        pause(scanner);
                        return;
                    }
                    break;
                case "2":
                    removeFromCart(cart, scanner);
                    break;
                default:
                    System.out.println("[TRY AGAIN] Choose 1, 2, or 3.");
                    continue;
            }
            pause(scanner);
        }
    }

    private static void removeFromCart(ShoppingCart cart, Scanner scanner) {
        if (cart.isEmpty()) {
            System.out.println("[EMPTY CART] Browse products to start your collection.");
            return;
        }

        CartItem item = null;
        while (item == null) {
            String sku = readText(scanner, "SKU to remove");
            if (sku == null) {
                canceled();
                return;
            }
            item = cart.findItem(sku);
            if (item == null) {
                System.out.println("[NO MATCH] That product is not in your cart.");
            }
        }

        System.out.println(item.getProduct().getName() + " | In cart: " + item.getQuantity());
        Integer quantity;
        while (true) {
            quantity = readQuantity(scanner, "How many to remove? Enter "
                    + item.getQuantity() + " to remove the whole line");
            if (quantity == null) {
                canceled();
                return;
            }
            if (quantity <= item.getQuantity()) {
                break;
            }
            System.out.println("[TRY AGAIN] You only have " + item.getQuantity() + " in the cart.");
        }

        cart.removeProduct(item.getProduct().getSku(), quantity);
        System.out.println("[REMOVED] " + quantity + " x " + item.getProduct().getName());
        System.out.println("New cart total: " + money(cart.getTotal()));
    }

    private static boolean checkout(ShoppingCart cart, Scanner scanner) {
        if (cart.isEmpty()) {
            System.out.println("[EMPTY CART] Add a product before checking out.");
            return false;
        }

        title("CHECKOUT");
        BigDecimal total = cart.getTotal();
        System.out.println("Amount due: " + money(total));

        BigDecimal paid;
        while (true) {
            paid = readMoney(scanner, "Cash payment amount");
            if (paid == null) {
                canceled();
                return false;
            }
            if (paid.compareTo(total) >= 0) {
                break;
            }
            System.out.println("[INSUFFICIENT PAYMENT] You are short " + money(total.subtract(paid))
                    + ". Enter a new payment amount of at least " + money(total) + ".");
        }

        BigDecimal change = paid.subtract(total);
        System.out.println("Payment: " + money(paid) + " | Change: " + money(change));
        if (!confirm(scanner, "Complete this purchase?")) {
            canceled();
            return false;
        }

        LocalDateTime saleTime = LocalDateTime.now();
        String receipt = buildReceipt(cart, saleTime, paid);
        Path receiptFile;
        try {
            receiptFile = FileManager.saveReceipt(saleTime, receipt);
        } catch (IOException e) {
            System.out.println("[CHECKOUT NOT COMPLETED] The receipt could not be saved.");
            System.out.println("Your cart is still here. Details: " + e.getMessage());
            return false;
        }

        // Complete the sale only after the receipt has been saved.
        System.out.print(receipt);
        cart.clear();
        System.out.println("[PURCHASE COMPLETE] Your cart is empty. Returning home.");
        System.out.println("Receipt saved: " + receiptFile.toAbsolutePath());
        return true;
    }

    public static String buildReceipt(ShoppingCart cart, LocalDateTime saleTime, BigDecimal paid) {
        BigDecimal total = cart.getTotal();
        return NL + LINE + NL + STORE_NAME + " | SALES RECEIPT" + NL
                + "Order date: " + saleTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + NL
                + LINE + NL + formatCart(cart)
                + "Amount paid: " + money(paid) + NL
                + "Change given: " + money(paid.subtract(total)) + NL
                + "Thanks for shopping. May your next roll be a critical hit!" + NL
                + LINE + NL;
    }

    private static void displayProducts(ArrayList<Product> products) {
        if (products.isEmpty()) {
            System.out.println("No products match. Try another search or choose 1 to reset.");
            return;
        }

        ArrayList<Product> sorted = new ArrayList<>(products);
        sorted.sort(Comparator.comparing(Product::getName, String.CASE_INSENSITIVE_ORDER));
        int nameWidth = 32;
        int departmentWidth = 14;
        for (Product product : sorted) {
            nameWidth = Math.max(nameWidth, product.getName().length());
            departmentWidth = Math.max(departmentWidth, product.getDepartment().length());
        }

        String header = "%-8s | %-" + nameWidth + "s | %-" + departmentWidth + "s | %10s%n";
        String row = "%-8s | %-" + nameWidth + "s | %-" + departmentWidth + "s | $%9s%n";
        System.out.printf(header, "SKU", "PRODUCT", "DEPARTMENT", "PRICE");
        System.out.println("-".repeat(27 + nameWidth + departmentWidth));
        for (Product product : sorted) {
            System.out.printf(row, product.getSku(), product.getName(),
                    product.getDepartment(), amount(product.getPrice()));
        }
    }

    public static String formatCart(ShoppingCart cart) {
        if (cart.isEmpty()) {
            return "Your cart is empty. Your next favorite is waiting in the shop!" + NL
                    + "Sales total: $0.00" + NL;
        }

        int nameWidth = 32;
        for (CartItem item : cart.getItems()) {
            nameWidth = Math.max(nameWidth, item.getProduct().getName().length());
        }
        String header = "%-8s | %-" + nameWidth + "s | %10s | %10s | %16s%n";
        String row = "%-8s | %-" + nameWidth + "s | %10d | $%9s | $%15s%n";
        StringBuilder output = new StringBuilder();
        output.append(String.format(header, "SKU", "PRODUCT", "QUANTITY", "UNIT PRICE", "LINE TOTAL"));
        output.append("-".repeat(56 + nameWidth)).append(NL);
        for (CartItem item : cart.getItems()) {
            output.append(String.format(Locale.US, row, item.getProduct().getSku(),
                    item.getProduct().getName(), item.getQuantity(),
                    amount(item.getProduct().getPrice()), amount(item.getLineTotal())));
        }
        output.append("Items: ").append(cart.getItemCount()).append(NL);
        output.append("Sales total: ").append(money(cart.getTotal())).append(NL);
        return output.toString();
    }

    private static String amount(BigDecimal value) {
        return String.format(Locale.US, "%,.2f", value);
    }

    private static String money(BigDecimal value) {
        return "$" + amount(value);
    }

    private static void title(String text) {
        System.out.println(NL + LINE + NL + "  " + text + NL + LINE);
    }

    private static String menuInput(Scanner scanner) {
        System.out.print("Your choice: ");
        return scanner.hasNextLine() ? scanner.nextLine().trim() : null;
    }

    private static String readInput(Scanner scanner, String prompt) {
        System.out.print(prompt + " (X to cancel): ");
        if (!scanner.hasNextLine()) {
            return null;
        }
        String input = scanner.nextLine().trim();
        return input.equalsIgnoreCase("X") ? null : input;
    }

    private static String readText(Scanner scanner, String prompt) {
        while (true) {
            String input = readInput(scanner, prompt);
            if (input == null || !input.isEmpty()) {
                return input;
            }
            System.out.println("[TRY AGAIN] Please enter a value.");
        }
    }

    private static Integer readQuantity(Scanner scanner, String prompt) {
        while (true) {
            String input = readInput(scanner, prompt);
            if (input == null) {
                return null;
            }
            try {
                int quantity = Integer.parseInt(input);
                if (quantity > 0) {
                    return quantity;
                }
            } catch (NumberFormatException e) {
                // Re-prompt with a message the customer can act on.
            }
            System.out.println("[TRY AGAIN] Enter a whole number greater than zero.");
        }
    }

    private static BigDecimal readMoney(Scanner scanner, String prompt) {
        while (true) {
            String input = readInput(scanner, prompt);
            if (input == null) {
                return null;
            }
            if (input.startsWith("$")) {
                input = input.substring(1).trim();
            }
            try {
                BigDecimal value = new BigDecimal(input).setScale(2, RoundingMode.UNNECESSARY);
                if (value.signum() >= 0) {
                    return value;
                }
            } catch (NumberFormatException | ArithmeticException e) {
                // Reject invalid numbers and fractions of a cent.
            }
            System.out.println("[TRY AGAIN] Enter a nonnegative price with at most 2 decimal places.");
        }
    }

    private static boolean confirm(Scanner scanner, String prompt) {
        while (true) {
            String answer = readInput(scanner, prompt + " Y/N");
            if (answer == null || answer.equalsIgnoreCase("N")) {
                return false;
            }
            if (answer.equalsIgnoreCase("Y")) {
                return true;
            }
            System.out.println("[TRY AGAIN] Enter Y to confirm or N to cancel.");
        }
    }

    private static void canceled() {
        System.out.println("[CANCELED] No changes were made.");
    }

    private static void pause(Scanner scanner) {
        System.out.print(NL + "Press Enter to continue...");
        if (scanner.hasNextLine()) {
            scanner.nextLine();
        }
    }

    private static void goodbye() {
        System.out.println(NL + "[CLOSED] Thanks for visiting " + STORE_NAME + "!");
    }
}
