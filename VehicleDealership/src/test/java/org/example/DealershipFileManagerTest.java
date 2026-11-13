package org.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class DealershipFileManagerTest {
    @TempDir
    Path folder;

    private final String header = "Ryuk Motors|123 Example Road|704-555-0100\n";

    @Test
    void loadsMetadataAndVehiclesFromPipeDelimitedFile() throws IOException {
        Path file = writeInventory(header + "10112|1993|Ford|Explorer|SUV|Red|525123|995.00\n");

        Dealership dealership = new DealershipFileManager(file).getDealership();

        assertEquals("Ryuk Motors", dealership.getName());
        assertEquals("123 Example Road", dealership.getAddress());
        assertEquals("704-555-0100", dealership.getPhone());
        assertEquals(1, dealership.getAllVehicles().size());
        Vehicle vehicle = dealership.getAllVehicles().get(0);
        assertEquals(10112, vehicle.getVin());
        assertEquals(1993, vehicle.getYear());
        assertEquals("Ford", vehicle.getMake());
        assertEquals("Explorer", vehicle.getModel());
        assertEquals("SUV", vehicle.getVehicleType());
        assertEquals("Red", vehicle.getColor());
        assertEquals(525123, vehicle.getOdometer());
        assertEquals(new BigDecimal("995.00"), vehicle.getPrice());
    }

    @Test
    void savesAddAndRemoveSoNewManagerSeesTheChanges() throws IOException {
        Path file = writeInventory(header + "10112|1993|Ford|Explorer|SUV|Red|525123|995.00\n");
        DealershipFileManager manager = new DealershipFileManager(file);
        Dealership dealership = manager.getDealership();
        Vehicle original = dealership.getAllVehicles().get(0);
        dealership.addVehicle(vehicle(20002));
        manager.saveDealership(dealership);

        Dealership afterAdd = new DealershipFileManager(file).getDealership();
        assertEquals(2, afterAdd.getAllVehicles().size());
        dealership.removeVehicle(original);
        manager.saveDealership(dealership);

        Dealership afterRemove = new DealershipFileManager(file).getDealership();
        assertEquals(1, afterRemove.getAllVehicles().size());
        assertEquals(20002, afterRemove.getAllVehicles().get(0).getVin());
        assertTrue(Files.readString(file).startsWith(header));
    }

    @Test
    void emptyInventoryKeepsDealershipDetails() throws IOException {
        Path file = folder.resolve("inventory.csv");
        Dealership dealership = new Dealership("Ryuk Motors", "123 Example Road", "704-555-0100");
        DealershipFileManager manager = new DealershipFileManager(file);

        manager.saveDealership(dealership);

        assertEquals(header, Files.readString(file));
        assertTrue(manager.getDealership().getAllVehicles().isEmpty());
    }

    @Test
    void malformedVehicleReportsItsLineWithoutChangingTheFile() throws IOException {
        String original = header + "10112|1993|Ford|Explorer|SUV|Red|525123|995.00\n"
                + "bad|record\n";
        Path file = writeInventory(original);

        IOException error = assertThrows(IOException.class,
                () -> new DealershipFileManager(file).getDealership());

        assertTrue(error.getMessage().contains("line 3"));
        assertEquals(original, Files.readString(file));
    }

    @Test
    void malformedNumberReportsTheVehicleLine() throws IOException {
        Path file = writeInventory(header + "10112|year|Ford|Explorer|SUV|Red|525123|995.00\n");

        IOException error = assertThrows(IOException.class,
                () -> new DealershipFileManager(file).getDealership());

        assertTrue(error.getMessage().contains("line 2"));
    }

    @Test
    void duplicateVinInSourceIsRejected() throws IOException {
        Path file = writeInventory(header
                + "10112|1993|Ford|Explorer|SUV|Red|525123|995.00\n"
                + "10112|2020|Honda|Civic|car|Black|20000|15000.00\n");

        IOException error = assertThrows(IOException.class,
                () -> new DealershipFileManager(file).getDealership());

        assertTrue(error.getMessage().contains("line 3"));
    }

    @Test
    void invalidHeaderAndEmptyFileAreRejected() throws IOException {
        Path file = writeInventory("Ryuk Motors|Missing phone\n");
        DealershipFileManager manager = new DealershipFileManager(file);
        assertThrows(IOException.class, manager::getDealership);
        Files.writeString(file, "");
        assertThrows(IOException.class, manager::getDealership);
    }

    @Test
    void missingFileDoesNotBecomeAnEmptyInventory() {
        Path file = folder.resolve("missing.csv");

        assertThrows(IOException.class, () -> new DealershipFileManager(file).getDealership());
        assertFalse(Files.exists(file));
    }

    @Test
    void duplicateVinCreatedBySetterCannotOverwriteSavedInventory() throws IOException {
        Path file = writeInventory(header);
        Dealership dealership = new DealershipFileManager(file).getDealership();
        Vehicle first = vehicle(1);
        Vehicle second = vehicle(2);
        dealership.addVehicle(first);
        dealership.addVehicle(second);
        second.setVin(1);

        assertThrows(IOException.class,
                () -> new DealershipFileManager(file).saveDealership(dealership));

        assertEquals(header, Files.readString(file));
        try (var files = Files.list(folder)) {
            assertEquals(1, files.count(), "No temporary files should be left after validation fails.");
        }
    }

    @Test
    void missingOutputFolderIsNotCreatedBehindTheUsersBack() {
        Path directory = folder.resolve("missing");
        Dealership dealership = new Dealership("Ryuk Motors", "123 Example Road", "704-555-0100");

        assertThrows(IOException.class,
                () -> new DealershipFileManager(directory.resolve("inventory.csv"))
                        .saveDealership(dealership));
        assertFalse(Files.exists(directory));
    }

    @Test
    void acceptsWindowsBomAndBlankLines() throws IOException {
        Path file = writeInventory("\uFEFF" + header + "\n"
                + "10112|1993|Ford|Explorer|SUV|Red|525123|995.00\n\n");

        Dealership dealership = new DealershipFileManager(file).getDealership();

        assertEquals("Ryuk Motors", dealership.getName());
        assertEquals(1, dealership.getAllVehicles().size());
    }

    @Test
    void replaceFailureKeepsExistingDestinationAndCleansTemporaryFile() throws IOException {
        Path destination = folder.resolve("inventory.csv");
        Files.createDirectory(destination);
        Path marker = destination.resolve("keep.txt");
        Files.writeString(marker, "existing data");
        Dealership dealership = new Dealership("Ryuk Motors", "123 Example Road", "704-555-0100");

        assertThrows(IOException.class,
                () -> new DealershipFileManager(destination).saveDealership(dealership));

        assertEquals("existing data", Files.readString(marker));
        try (var files = Files.list(folder)) {
            assertEquals(1, files.count(), "Failed saves should clean up the temporary file.");
        }
    }

    private Path writeInventory(String contents) throws IOException {
        Path file = folder.resolve("inventory.csv");
        Files.writeString(file, contents, StandardCharsets.UTF_8);
        return file;
    }

    private Vehicle vehicle(int vin) {
        return new Vehicle(vin, 2020, "Honda", "Civic", "car", "Black", 20000,
                new BigDecimal("15000.00"));
    }
}
