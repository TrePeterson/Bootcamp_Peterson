package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/** Converts the pipe-delimited inventory file to objects and back again. */
public class DealershipFileManager {
    private final Path filePath;

    public DealershipFileManager() {
        this(Path.of(System.getProperty("dealership.inventory", "inventory.csv")));
    }

    public DealershipFileManager(Path filePath) {
        this.filePath = Objects.requireNonNull(filePath, "Inventory path is required.")
                .toAbsolutePath().normalize();
    }

    public Path getFilePath() {
        return filePath;
    }

    public Dealership getDealership() throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            String firstLine = reader.readLine();
            if (firstLine == null) {
                throw invalidLine(1, "The dealership information is missing.", null);
            }

            // Some Windows editors place a UTF-8 byte-order mark before the name.
            if (firstLine.startsWith("\uFEFF")) {
                firstLine = firstLine.substring(1);
            }
            Dealership dealership = readDealershipDetails(firstLine);
            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }
                try {
                    dealership.addVehicle(readVehicle(line));
                } catch (IllegalArgumentException exception) {
                    throw invalidLine(lineNumber, exception.getMessage(), exception);
                }
            }
            return dealership;
        }
    }

    private Dealership readDealershipDetails(String line) throws IOException {
        String[] parts = line.split("\\|", -1);
        if (parts.length != 3) {
            throw invalidLine(1, "Expected name|address|phone.", null);
        }
        try {
            return new Dealership(parts[0].trim(), parts[1].trim(), parts[2].trim());
        } catch (IllegalArgumentException exception) {
            throw invalidLine(1, exception.getMessage(), exception);
        }
    }

    private Vehicle readVehicle(String line) {
        String[] parts = line.split("\\|", -1);
        if (parts.length != 8) {
            throw new IllegalArgumentException(
                    "Expected vin|year|make|model|type|color|odometer|price.");
        }
        return new Vehicle(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()),
                parts[2].trim(), parts[3].trim(), parts[4].trim(), parts[5].trim(),
                Integer.parseInt(parts[6].trim()), new BigDecimal(parts[7].trim()));
    }

    private IOException invalidLine(int lineNumber, String message, Exception cause) {
        return new IOException("Invalid inventory at line " + lineNumber + " in "
                + filePath + ": " + message, cause);
    }

    public void saveDealership(Dealership dealership) throws IOException {
        // Prepare and validate everything before touching the saved inventory.
        String contents = createFileContents(dealership);
        Path directory = filePath.getParent();
        if (!Files.isDirectory(directory)) {
            throw new IOException("Inventory folder does not exist: " + directory);
        }

        Path temporaryFile = Files.createTempFile(directory, "inventory-", ".tmp");
        try {
            Files.writeString(temporaryFile, contents, StandardCharsets.UTF_8);
            try {
                Files.move(temporaryFile, filePath, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporaryFile, filePath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            try {
                Files.deleteIfExists(temporaryFile);
            } catch (IOException cleanupFailure) {
                exception.addSuppressed(cleanupFailure);
            }
            throw exception;
        }
    }

    private String createFileContents(Dealership dealership) throws IOException {
        try {
            Objects.requireNonNull(dealership, "Dealership is required.");
            Dealership validated = new Dealership(dealership.getName(), dealership.getAddress(),
                    dealership.getPhone());
            StringBuilder contents = new StringBuilder();
            contents.append(validated.getName()).append('|')
                    .append(validated.getAddress()).append('|')
                    .append(validated.getPhone()).append('\n');
            Set<Integer> vins = new HashSet<>();

            for (Vehicle vehicle : dealership.getAllVehicles()) {
                Objects.requireNonNull(vehicle, "Inventory cannot contain a null vehicle.");
                // Setters can change a vehicle after it was added, so check again before saving.
                Vehicle checked = new Vehicle(vehicle.getVin(), vehicle.getYear(), vehicle.getMake(),
                        vehicle.getModel(), vehicle.getVehicleType(), vehicle.getColor(),
                        vehicle.getOdometer(), vehicle.getPrice());
                if (!vins.add(checked.getVin())) {
                    throw new IllegalArgumentException("Duplicate VIN: " + checked.getVin());
                }
                contents.append(checked.getVin()).append('|').append(checked.getYear()).append('|')
                        .append(checked.getMake()).append('|').append(checked.getModel()).append('|')
                        .append(checked.getVehicleType()).append('|').append(checked.getColor()).append('|')
                        .append(checked.getOdometer()).append('|')
                        .append(checked.getPrice().toPlainString()).append('\n');
            }
            return contents.toString();
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new IOException("Inventory was not saved: " + exception.getMessage(), exception);
        }
    }
}
