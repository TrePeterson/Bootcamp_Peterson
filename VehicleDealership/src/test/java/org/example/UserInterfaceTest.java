package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class UserInterfaceTest {
    @TempDir
    Path directory;

    private Path inventory;

    @BeforeEach
    void createInventory() throws IOException {
        inventory = directory.resolve("inventory.csv");
        Files.writeString(inventory, """
                Test Motors|100 Test Road|555-0100
                10112|2020|Honda|Civic|car|Blue|40000|15000.00
                10113|2019|Toyota|Tacoma|truck|Red|75000|24000.00
                10114|2022|Honda|CR-V|SUV|Blue|20000|28000.00
                10115|2023|Kia|Carnival|van|Silver|10000|32000.00
                """, StandardCharsets.UTF_8);
    }

    static Stream<Arguments> searchCommands() {
        return Stream.of(
                Arguments.of("price range", lines("1", "15000", "24000", "", "99"),
                        new String[]{"Civic", "Tacoma"}, new String[]{"CR-V", "Carnival"}),
                Arguments.of("partial make/model", lines("2", "HON", "ci", "", "99"),
                        new String[]{"Civic"}, new String[]{"Tacoma", "CR-V", "Carnival"}),
                Arguments.of("year range", lines("3", "2022", "2023", "", "99"),
                        new String[]{"CR-V", "Carnival"}, new String[]{"Civic", "Tacoma"}),
                Arguments.of("color", lines("4", "bLuE", "", "99"),
                        new String[]{"Civic", "CR-V"}, new String[]{"Tacoma", "Carnival"}),
                Arguments.of("mileage range", lines("5", "0", "20000", "", "99"),
                        new String[]{"CR-V", "Carnival"}, new String[]{"Civic", "Tacoma"}),
                Arguments.of("type", lines("6", "sUv", "", "99"),
                        new String[]{"CR-V"}, new String[]{"Civic", "Tacoma", "Carnival"}),
                Arguments.of("all vehicles", lines("7", "", "99"),
                        new String[]{"Civic", "Tacoma", "CR-V", "Carnival"}, new String[]{}));
    }

    @ParameterizedTest(name = "Menu displays the correct {0} results")
    @MethodSource("searchCommands")
    void menuSearchesDisplayOnlyMatchingVehicles(String description, String input,
                                                String[] expected, String[] excluded) {
        String output = runConsole(input);

        for (String model : expected) {
            assertTrue(output.contains(model), description + " should include " + model);
        }
        for (String model : excluded) {
            assertFalse(output.contains(model), description + " should exclude " + model);
        }
    }

    @Test
    void confirmedAddIsStillPresentAfterReloadingFromDisk() throws IOException {
        String output = runConsole(lines("8", "10500", "2024", "Mazda", "Miata", "car",
                "White", "5000", "29999.95", "Y", "", "99"));

        Vehicle saved = new DealershipFileManager(inventory).getDealership().findVehicleByVin(10500);
        assertNotNull(saved);
        assertEquals("Miata", saved.getModel());
        assertEquals(new BigDecimal("29999.95"), saved.getPrice());
        assertTrue(output.contains("[OK] Added Mazda Miata (VIN 10500). Saved to inventory.csv."),
                "Only a completed add and disk save should produce this confirmation.");
    }

    @Test
    void confirmedRemovalIsStillRemovedAfterReloadingFromDisk() throws IOException {
        String output = runConsole(lines("9", "10112", "Y", "", "99"));

        Dealership reloaded = new DealershipFileManager(inventory).getDealership();
        assertNull(reloaded.findVehicleByVin(10112));
        assertEquals(3, reloaded.getAllVehicles().size());
        assertTrue(output.contains("[OK] Removed Honda Civic (VIN 10112). Saved to inventory.csv."),
                "Only a completed removal and disk save should produce this confirmation.");
    }

    @Test
    void cancellingHalfwayThroughAnAddLeavesTheFileUnchanged() throws IOException {
        byte[] before = Files.readAllBytes(inventory);

        runConsole(lines("8", "10500", "2024", "Mazda", "X", "", "99"));

        assertArrayEquals(before, Files.readAllBytes(inventory));
    }

    @Test
    void decliningTheAddConfirmationLeavesTheFileUnchanged() throws IOException {
        byte[] before = Files.readAllBytes(inventory);

        runConsole(lines("8", "10500", "2024", "Mazda", "Miata", "car",
                "White", "5000", "29999.95", "N", "", "99"));

        assertArrayEquals(before, Files.readAllBytes(inventory));
    }

    @Test
    void decliningRemovalKeepsTheVehicle() throws IOException {
        byte[] before = Files.readAllBytes(inventory);

        runConsole(lines("9", "10112", "N", "", "99"));

        assertArrayEquals(before, Files.readAllBytes(inventory));
    }

    @Test
    void badNumbersAndReversedRangesCanBeCorrectedWithoutRestarting() {
        String output = runConsole(lines("1", "abc", "-5", "15000", "10000", "24000", "", "99"));

        assertTrue(output.contains("Civic"));
        assertTrue(output.contains("Tacoma"));
        assertFalse(output.contains("CR-V"));
        assertTrue(output.contains("[!]"), "Invalid input should receive a helpful error.");
    }

    @Test
    void unknownMenuChoicesCanBeCorrectedWithoutLeavingTheApplication() {
        String output = runConsole(lines("banana", "42", "7", "", "99"));

        assertTrue(output.contains("[!]"));
        assertTrue(output.contains("Civic"));
        assertTrue(output.contains("Carnival"));
    }

    @Test
    void unknownVinDoesNotRemoveAnyVehicles() throws IOException {
        byte[] before = Files.readAllBytes(inventory);

        String output = runConsole(lines("9", "99999", "", "99"));

        assertArrayEquals(before, Files.readAllBytes(inventory));
        assertTrue(output.toLowerCase().contains("no vehicle"));
    }

    @Test
    void duplicateVinCanBeCorrectedBeforeSaving() throws IOException {
        runConsole(lines("8", "10112", "10500", "2024", "Mazda", "Miata", "car",
                "White", "5000", "29999.95", "Y", "", "99"));

        Dealership reloaded = new DealershipFileManager(inventory).getDealership();
        assertEquals(5, reloaded.getAllVehicles().size());
        assertEquals("Civic", reloaded.findVehicleByVin(10112).getModel());
        assertEquals("Miata", reloaded.findVehicleByVin(10500).getModel());
    }

    @Test
    void emptySearchReturnsToTheMenuSoAnotherSearchCanRun() {
        String output = runConsole(lines("4", "Purple", "", "6", "van", "", "99"));

        assertTrue(output.toLowerCase().contains("no vehicles"));
        assertTrue(output.contains("Carnival"));
    }

    @Test
    void endOfInputDuringAnAddDoesNotSaveAnIncompleteVehicle() throws IOException {
        byte[] before = Files.readAllBytes(inventory);

        assertDoesNotThrow(() -> runConsole(lines("8", "10500")));

        assertArrayEquals(before, Files.readAllBytes(inventory));
    }

    @Test
    void failedSaveDoesNotChangeTheLoadedInventoryOrOriginalFile() throws IOException {
        byte[] before = Files.readAllBytes(inventory);
        Dealership original = new DealershipFileManager(inventory).getDealership();
        DealershipFileManager failingManager = new DealershipFileManager(inventory) {
            @Override
            public Dealership getDealership() {
                return original;
            }

            @Override
            public void saveDealership(Dealership dealership) throws IOException {
                throw new IOException("Simulated disk failure");
            }
        };

        String output = runConsole(lines("8", "10500", "2024", "Mazda", "Miata", "car",
                "White", "5000", "29999.95", "Y", "", "7", "", "99"), failingManager);

        assertArrayEquals(before, Files.readAllBytes(inventory));
        assertNull(original.findVehicleByVin(10500));
        assertTrue(output.toLowerCase().contains("not changed"));
        assertFalse(output.contains("[OK] Added Mazda Miata"), "A failed save must not claim success.");
        assertFalse(output.contains("Saved to inventory.csv."));
        int listStart = output.lastIndexOf("THE FULL LINEUP");
        assertTrue(listStart >= 0, "The user should still be able to open the vehicle list after a save error.");
        String listAfterFailure = output.substring(listStart);
        assertTrue(listAfterFailure.contains("Civic"));
        assertFalse(listAfterFailure.contains("Miata"), "A failed save must not change the UI's inventory.");
    }

    private String runConsole(String input) {
        return runConsole(input, new DealershipFileManager(inventory));
    }

    private String runConsole(String input, DealershipFileManager manager) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (Scanner scanner = new Scanner(input);
             PrintStream output = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            new UserInterface(scanner, output, manager, false).display();
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }

    private static String lines(String... values) {
        return String.join("\n", values) + "\n";
    }
}
