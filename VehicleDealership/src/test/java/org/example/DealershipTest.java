package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DealershipTest {
    private Dealership dealership;
    private Vehicle civic;
    private Vehicle explorer;
    private Vehicle ranger;

    @BeforeEach
    void createInventory() {
        dealership = new Dealership("Test Motors", "123 Sample Lane", "704-555-0100");
        civic = new Vehicle(1, 2012, "Honda", "Civic", "car", "Gray", 103221,
                new BigDecimal("6995.00"));
        explorer = new Vehicle(2, 1993, "Ford", "Explorer", "SUV", "Red", 525123,
                new BigDecimal("995.00"));
        ranger = new Vehicle(3, 2001, "Ford", "Ranger", "truck", "Yellow", 172544,
                new BigDecimal("1995.00"));
        dealership.addVehicle(civic);
        dealership.addVehicle(explorer);
        dealership.addVehicle(ranger);
    }

    @Test
    void metadataCanBeReadAndChangedWithoutChangingInventory() {
        dealership.setName("Updated Motors");
        dealership.setAddress("456 Example Street");
        dealership.setPhone("704-555-0199");

        assertEquals("Updated Motors", dealership.getName());
        assertEquals("456 Example Street", dealership.getAddress());
        assertEquals("704-555-0199", dealership.getPhone());
        assertEquals(3, dealership.getAllVehicles().size());
    }

    @Test
    void priceSearchIncludesBothBoundariesAndComparesNumericValue() {
        List<Vehicle> matches = dealership.getVehiclesByPrice(new BigDecimal("995"),
                new BigDecimal("1995.000"));

        assertEquals(List.of(explorer, ranger), matches);
        assertEquals(List.of(civic), dealership.getVehiclesByPrice(new BigDecimal("6995"),
                new BigDecimal("6995")));
    }

    @Test
    void makeModelSearchAcceptsPartialCaseInsensitiveTextAndEitherBlankField() {
        assertEquals(List.of(explorer, ranger), dealership.getVehiclesByMakeModel(" fO ", ""));
        assertEquals(List.of(civic), dealership.getVehiclesByMakeModel("", "IVI"));
        assertEquals(List.of(explorer), dealership.getVehiclesByMakeModel("ford", "plo"));
        assertTrue(dealership.getVehiclesByMakeModel("Honda", "Ranger").isEmpty());
    }

    @Test
    void yearSearchIncludesBothBoundaries() {
        assertEquals(List.of(explorer, ranger), dealership.getVehiclesByYear(1993, 2001));
        assertEquals(List.of(civic), dealership.getVehiclesByYear(2012, 2012));
    }

    @Test
    void colorSearchMatchesWholeColorWithoutCaseOrOuterSpaceDifferences() {
        assertEquals(List.of(civic), dealership.getVehiclesByColor(" gRAY "));
        assertTrue(dealership.getVehiclesByColor("gra").isEmpty());
    }

    @Test
    void mileageSearchIncludesBothBoundaries() {
        assertEquals(List.of(civic, ranger), dealership.getVehiclesByMileage(103221, 172544));
        assertEquals(List.of(explorer), dealership.getVehiclesByMileage(525123, 525123));
    }

    @Test
    void typeSearchIsCaseInsensitiveAndFindsNoUnrelatedTypes() {
        assertEquals(List.of(explorer), dealership.getVehiclesByType(" suv "));
        assertEquals(List.of(ranger), dealership.getVehiclesByType("TRUCK"));
        assertTrue(dealership.getVehiclesByType("van").isEmpty());
    }

    @Test
    void duplicateVinAndNullVehicleCannotBeAdded() {
        Vehicle duplicate = new Vehicle(1, 2024, "Toyota", "Corolla", "car", "White", 0,
                new BigDecimal("25000"));

        assertThrows(IllegalArgumentException.class, () -> dealership.addVehicle(duplicate));
        assertThrows(IllegalArgumentException.class, () -> dealership.addVehicle(null));
        assertEquals(3, dealership.getAllVehicles().size());
        assertSame(civic, dealership.findVehicleByVin(1));
    }

    @Test
    void removingByVinRemovesOnlyTheRequestedVehicle() {
        Vehicle sameVin = new Vehicle(2, 1993, "Ford", "Explorer", "SUV", "Red", 525123,
                new BigDecimal("995"));

        assertTrue(dealership.removeVehicle(sameVin));
        assertEquals(List.of(civic, ranger), dealership.getAllVehicles());
        assertNull(dealership.findVehicleByVin(2));
        assertFalse(dealership.removeVehicle(sameVin));
        assertFalse(dealership.removeVehicle(null));
    }

    @Test
    void changingReturnedListDoesNotChangeDealershipInventory() {
        List<Vehicle> vehicles = dealership.getAllVehicles();
        vehicles.clear();

        assertEquals(3, dealership.getAllVehicles().size());
    }

    @Test
    void invalidSearchRangesAndEmptyQueriesAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> dealership.getVehiclesByPrice(BigDecimal.TEN, BigDecimal.ONE));
        assertThrows(IllegalArgumentException.class,
                () -> dealership.getVehiclesByPrice(new BigDecimal("-1"), BigDecimal.TEN));
        assertThrows(IllegalArgumentException.class,
                () -> dealership.getVehiclesByPrice(null, BigDecimal.TEN));
        assertThrows(IllegalArgumentException.class, () -> dealership.getVehiclesByYear(2020, 2000));
        assertThrows(IllegalArgumentException.class, () -> dealership.getVehiclesByMileage(-1, 2000));
        assertThrows(IllegalArgumentException.class, () -> dealership.getVehiclesByMakeModel(" ", ""));
        assertThrows(IllegalArgumentException.class, () -> dealership.getVehiclesByColor(""));
        assertThrows(IllegalArgumentException.class, () -> dealership.getVehiclesByType(" "));
    }

    @Test
    void emptyInventoryProducesEmptyResults() {
        Dealership empty = new Dealership("Empty Motors", "123 Sample Lane", "704-555-0100");

        assertTrue(empty.getAllVehicles().isEmpty());
        assertTrue(empty.getVehiclesByPrice(BigDecimal.ZERO, BigDecimal.TEN).isEmpty());
        assertTrue(empty.getVehiclesByYear(2000, 2025).isEmpty());
        assertNull(empty.findVehicleByVin(1));
    }
}
