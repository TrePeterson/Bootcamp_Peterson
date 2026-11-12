package org.example;

import java.io.IOException;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Scanner;

/** Runs the menus. The dealership handles inventory; the file manager handles disk storage. */
public class UserInterface {
    private static final int WIDTH = 109;
    private static final BigDecimal MAX_PRICE = new BigDecimal("999999999.99");
    private static final String RED = "\u001B[91m";
    private static final String VIOLET = "\u001B[95m";
    private static final String GRAY = "\u001B[90m";
    private static final String WHITE = "\u001B[97m";
    private static final String RESET = "\u001B[0m";
    private static final String ROW = "| %10s | %4s | %-10s | %-18s | %-5s | %-9s | %13s | %15s |";

    private final Scanner scanner;
    private final PrintStream output;
    private final DealershipFileManager fileManager;
    private final boolean useColor;
    private Dealership dealership;

    public UserInterface() {
        this(new Scanner(System.in), System.out, new DealershipFileManager(),
                !"false".equalsIgnoreCase(System.getProperty("dealership.color", "true"))
                        && System.getenv("NO_COLOR") == null);
    }

    // Supplying input/output and a file manager lets tests exercise the actual menus safely.
    public UserInterface(Scanner scanner, PrintStream output,
                         DealershipFileManager fileManager, boolean useColor) {
        this.scanner = scanner;
        this.output = output;
        this.fileManager = fileManager;
        this.useColor = useColor;
    }

    public void display() {
        if (!init()) {
            return;
        }
        try {
            while (true) {
                displayMenu();
                String choice = readLine();
                if (choice.equals("99")) {
                    info("Session closed. Thanks for visiting " + dealership.getName() + ".");
                    return;
                }
                try {
                    switch (choice) {
                        case "1" -> processGetByPriceRequest();
                        case "2" -> processGetByMakeModelRequest();
                        case "3" -> processGetByYearRequest();
                        case "4" -> processGetByColorRequest();
                        case "5" -> processGetByMileageRequest();
                        case "6" -> processGetByVehicleTypeRequest();
                        case "7" -> processGetAllVehiclesRequest();
                        case "8" -> processAddVehicleRequest();
                        case "9" -> processRemoveVehicleRequest();
                        default -> {
                            warning("Choose a menu number from 1 to 9, or 99 to quit.");
                            continue;
                        }
                    }
                } catch (CancelledInput exception) {
                    info("Action cancelled. Inventory unchanged.");
                }
                output.print("\n  Press Enter to return to the main menu: ");
                readLine();
            }
        } catch (NoSuchElementException exception) {
            info("Input closed. Completed saves are already in inventory.csv.");
        }
    }

    private boolean init() {
        try {
            dealership = fileManager.getDealership();
            success("Inventory loaded: " + dealership.getAllVehicles().size() + " vehicles.");
            return true;
        } catch (IOException exception) {
            warning("Unable to open the dealership: " + exception.getMessage());
            info("Inventory location: " + fileManager.getFilePath());
            info("Open the project folder in IntelliJ and use that folder as the working directory.");
            return false;
        }
    }

    private void displayMenu() {
        BigDecimal inventoryValue = BigDecimal.ZERO;
        for (Vehicle vehicle : dealership.getAllVehicles()) {
            inventoryValue = inventoryValue.add(vehicle.getPrice());
        }
        output.println();
        border(RED);
        boxLine(dealership.getName().toUpperCase(Locale.ROOT), WHITE);
        boxLine("THE INVENTORY NOTEBOOK  /  Find your next chapter.", VIOLET);
        boxLine(dealership.getAddress() + "  |  " + dealership.getPhone(), GRAY);
        border(RED);
        boxLine(dealership.getAllVehicles().size() + " vehicles in stock  |  Total asking value: "
                + money(inventoryValue), WHITE);
        border(GRAY);
        boxLine("SEARCH THE LOT", VIOLET);
        boxLine("  1  Price range", WHITE);
        boxLine("  2  Make / model", WHITE);
        boxLine("  3  Year range", WHITE);
        boxLine("  4  Color", WHITE);
        boxLine("  5  Mileage range", WHITE);
        boxLine("  6  Vehicle type", WHITE);
        boxLine("  7  List all vehicles", WHITE);
        border(GRAY);
        boxLine("MANAGE INVENTORY", VIOLET);
        boxLine("  8  Add a vehicle", WHITE);
        boxLine("  9  Remove a vehicle", WHITE);
        boxLine(" 99  Quit", WHITE);
        border(RED);
        output.print("  Your choice > ");
    }

    public void processGetByPriceRequest() {
        formTitle("PRICE RANGE", "Both ends of the range are included. Use numbers such as 15000 or 15000.50.");
        BigDecimal minimum = readMoney("Minimum price");
        BigDecimal maximum;
        do {
            maximum = readMoney("Maximum price");
            if (maximum.compareTo(minimum) < 0) {
                warning("Maximum price must be at least " + money(minimum) + ".");
            }
        } while (maximum.compareTo(minimum) < 0);
        displayVehicles(dealership.getVehiclesByPrice(minimum, maximum));
    }

    public void processGetByMakeModelRequest() {
        formTitle("MAKE / MODEL", "Partial names work. Leave either field blank to search any make or model.");
        while (true) {
            String make = readText("Make (example: hon)", true);
            String model = readText("Model (example: civic)", true);
            if (make.isBlank() && model.isBlank()) {
                warning("Enter at least part of a make or model, or use menu option 7 to list everything.");
                continue;
            }
            displayVehicles(dealership.getVehiclesByMakeModel(make, model));
            return;
        }
    }

    public void processGetByYearRequest() {
        formTitle("YEAR RANGE", "Both the earliest and latest years are included.");
        int minimum = readInteger("Earliest year", 1886, 9999);
        int maximum = readInteger("Latest year (at least " + minimum + ")", minimum, 9999);
        displayVehicles(dealership.getVehiclesByYear(minimum, maximum));
    }

    public void processGetByColorRequest() {
        formTitle("COLOR SEARCH", "Enter the color name. Capitalization does not matter.");
        displayVehicles(dealership.getVehiclesByColor(readText("Color (example: Black)", false)));
    }

    public void processGetByMileageRequest() {
        formTitle("MILEAGE RANGE", "Use whole miles without commas. Both limits are included.");
        int minimum = readInteger("Minimum miles", 0, Integer.MAX_VALUE);
        int maximum = readInteger("Maximum miles (at least " + minimum + ")", minimum, Integer.MAX_VALUE);
        displayVehicles(dealership.getVehiclesByMileage(minimum, maximum));
    }

    public void processGetByVehicleTypeRequest() {
        formTitle("VEHICLE TYPE", "Choose car, truck, SUV, or van.");
        displayVehicles(dealership.getVehiclesByType(readVehicleType()));
    }

    public void processGetAllVehiclesRequest() {
        output.println();
        box("THE FULL LINEUP");
        displayVehicles(dealership.getAllVehicles());
    }

    public void processAddVehicleRequest() {
        formTitle("ADD A VEHICLE", "Enter the vehicle details, then review them before saving.");
        int vin;
        while (true) {
            vin = readInteger("VIN / inventory number (positive whole number)", 1, Integer.MAX_VALUE);
            if (dealership.findVehicleByVin(vin) == null) {
                break;
            }
            warning("VIN " + vin + " is already in the inventory. Enter a different VIN.");
        }
        int year = readInteger("Year", 1886, 9999);
        String make = readText("Make", false);
        String model = readText("Model", false);
        String type = readVehicleType();
        String color = readText("Color", false);
        int odometer = readInteger("Odometer in whole miles", 0, Integer.MAX_VALUE);
        BigDecimal price = readMoney("Asking price (example: 19995.00)");
        Vehicle vehicle = new Vehicle(vin, year, make, model, type, color, odometer, price);
        box("REVIEW NEW VEHICLE");
        displayVehicles(List.of(vehicle));
        if (!confirm("Save this vehicle? (Y/N)")) {
            info("Add cancelled. Inventory unchanged.");
            return;
        }
        Dealership updated = copyDealership();
        updated.addVehicle(vehicle);
        saveChange(updated, "Added " + vehicle.getMake() + " " + vehicle.getModel() + " (VIN " + vin + ").");
    }

    public void processRemoveVehicleRequest() {
        if (dealership.getAllVehicles().isEmpty()) {
            info("There are no vehicles to remove.");
            return;
        }
        formTitle("REMOVE A VEHICLE", "Use a VIN from the vehicle list. You will review it before removal.");
        int vin = readInteger("VIN to remove", 1, Integer.MAX_VALUE);
        Vehicle vehicle = dealership.findVehicleByVin(vin);
        if (vehicle == null) {
            info("No vehicle has VIN " + vin + ". Nothing was removed.");
            return;
        }
        displayVehicles(List.of(vehicle));
        if (!confirm("Remove this vehicle from inventory? (Y/N)")) {
            info("Removal cancelled. Inventory unchanged.");
            return;
        }
        Dealership updated = copyDealership();
        updated.removeVehicle(vehicle);
        saveChange(updated, "Removed " + vehicle.getMake() + " " + vehicle.getModel() + " (VIN " + vin + ").");
    }

    private Dealership copyDealership() {
        Dealership copy = new Dealership(dealership.getName(), dealership.getAddress(), dealership.getPhone());
        for (Vehicle vehicle : dealership.getAllVehicles()) {
            copy.addVehicle(vehicle);
        }
        return copy;
    }

    private void saveChange(Dealership updated, String confirmation) {
        try {
            // Commit the screen's new inventory only AFTER the disk write succeeds.
            fileManager.saveDealership(updated);
            dealership = updated;
            success(confirmation + " Saved to inventory.csv.");
            info("Inventory now contains " + dealership.getAllVehicles().size() + " vehicles.");
        } catch (IOException exception) {
            warning("Could not save. Your inventory was not changed.");
            info(exception.getMessage());
        }
    }

    private void displayVehicles(List<Vehicle> vehicles) {
        if (vehicles.isEmpty()) {
            info("No vehicles found. Try different search values or list all vehicles.");
            return;
        }
        List<Vehicle> sorted = new ArrayList<>(vehicles);
        sorted.sort(Comparator.comparing(Vehicle::getMake, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Vehicle::getModel, String.CASE_INSENSITIVE_ORDER)
                .thenComparingInt(Vehicle::getVin));
        output.println();
        border(GRAY);
        output.println(color(VIOLET, String.format(Locale.US, ROW,
                "VIN", "YEAR", "MAKE", "MODEL", "TYPE", "COLOR", "MILES", "PRICE")));
        border(GRAY);
        for (Vehicle vehicle : sorted) {
            output.println(String.format(Locale.US, ROW, vehicle.getVin(), vehicle.getYear(),
                    shorten(vehicle.getMake(), 10), shorten(vehicle.getModel(), 18), vehicle.getVehicleType(),
                    shorten(vehicle.getColor(), 9), String.format(Locale.US, "%,d", vehicle.getOdometer()),
                    money(vehicle.getPrice())));
        }
        border(GRAY);
        success("Showing " + sorted.size() + " vehicle" + (sorted.size() == 1 ? "." : "s."));
        for (Vehicle vehicle : sorted) {
            if (vehicle.getMake().length() > 10 || vehicle.getModel().length() > 18 || vehicle.getColor().length() > 9) {
                info("Full details for VIN " + vehicle.getVin() + ": " + vehicle.getMake() + " "
                        + vehicle.getModel() + " / " + vehicle.getColor());
            }
        }
    }

    private int readInteger(String prompt, int minimum, int maximum) {
        while (true) {
            String input = readFormInput(prompt);
            try {
                int value = Integer.parseInt(input);
                if (value >= minimum && value <= maximum) {
                    return value;
                }
            } catch (NumberFormatException exception) {
                // Fall through to the same helpful message for letters or an oversized number.
            }
            warning("Enter a whole number from " + minimum + " to " + maximum + " without commas.");
        }
    }

    private BigDecimal readMoney(String prompt) {
        while (true) {
            String input = readFormInput(prompt);
            if (input.matches("[0-9]+(?:\\.[0-9]{1,2})?")) {
                BigDecimal amount = new BigDecimal(input);
                if (amount.compareTo(MAX_PRICE) <= 0) {
                    return amount.setScale(2);
                }
            }
            warning("Enter an amount from 0 to 999999999.99, with up to two decimals and no $ or commas.");
        }
    }

    private String readText(String prompt, boolean allowBlank) {
        while (true) {
            String input = readFormInput(prompt);
            if (allowBlank && input.isEmpty()) {
                return "";
            }
            try {
                return Vehicle.requireText(input, "Text");
            } catch (IllegalArgumentException exception) {
                warning(exception.getMessage());
            }
        }
    }

    private String readVehicleType() {
        while (true) {
            String type = readFormInput("Vehicle type: car / truck / SUV / van").toLowerCase(Locale.ROOT);
            switch (type) {
                case "car", "truck", "van" -> { return type; }
                case "suv" -> { return "SUV"; }
                default -> warning("Choose car, truck, SUV, or van.");
            }
        }
    }

    private boolean confirm(String prompt) {
        while (true) {
            String answer = readFormInput(prompt);
            if (answer.equalsIgnoreCase("y")) {
                return true;
            }
            if (answer.equalsIgnoreCase("n")) {
                return false;
            }
            warning("Enter Y to confirm or N to cancel.");
        }
    }

    private String readFormInput(String prompt) {
        box(prompt);
        output.print("  > ");
        String input = readLine();
        if (input.equalsIgnoreCase("x")) {
            throw new CancelledInput();
        }
        return input;
    }

    private String readLine() {
        if (!scanner.hasNextLine()) {
            throw new NoSuchElementException("Input ended.");
        }
        return scanner.nextLine().trim();
    }

    private void formTitle(String title, String instructions) {
        output.println();
        box(title);
        info(instructions);
        info("Type X at any prompt to cancel and return to the menu.");
    }

    private void box(String text) {
        border(GRAY);
        boxLine(text, WHITE);
        border(GRAY);
    }

    private void boxLine(String text, String accent) {
        int contentWidth = WIDTH - 4;
        for (int start = 0; start < text.length(); start += contentWidth) {
            String part = text.substring(start, Math.min(start + contentWidth, text.length()));
            output.println(color(accent, "| " + String.format(Locale.US, "%-" + contentWidth + "s", part) + " |"));
        }
    }

    private void border(String accent) {
        output.println(color(accent, "+" + "-".repeat(WIDTH - 2) + "+"));
    }

    private void success(String message) {
        output.println(color(VIOLET, "  [OK] ") + message);
    }

    private void info(String message) {
        output.println(color(GRAY, "  [INFO] ") + message);
    }

    private void warning(String message) {
        output.println(color(RED, "  [!] ") + message);
    }

    private String color(String accent, String text) {
        return useColor ? accent + text + RESET : text;
    }

    private static String shorten(String text, int length) {
        return text.length() <= length ? text : text.substring(0, length - 1) + "~";
    }

    private static String money(BigDecimal amount) {
        return String.format(Locale.US, "$%,.2f", amount);
    }

    // A single signal unwinds any form when the user chooses X, before a save takes place.
    private static class CancelledInput extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
