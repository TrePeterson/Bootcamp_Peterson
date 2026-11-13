package org.example;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class VehicleTest {
    @Test
    void constructorRetainsAllVehicleDetailsAndNormalizesTypeAndMoney() {
        Vehicle vehicle = new Vehicle(10112, 1993, " Ford ", "Explorer", "suv",
                "Red", 525123, new BigDecimal("995"));

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
    void settersUpdateTheVehicleDetails() {
        Vehicle vehicle = sampleVehicle();

        vehicle.setVin(77);
        vehicle.setYear(2020);
        vehicle.setMake("Toyota");
        vehicle.setModel("Sienna");
        vehicle.setVehicleType("VAN");
        vehicle.setColor("White");
        vehicle.setOdometer(10000);
        vehicle.setPrice(new BigDecimal("20000.50"));

        assertEquals(77, vehicle.getVin());
        assertEquals(2020, vehicle.getYear());
        assertEquals("Toyota", vehicle.getMake());
        assertEquals("Sienna", vehicle.getModel());
        assertEquals("van", vehicle.getVehicleType());
        assertEquals("White", vehicle.getColor());
        assertEquals(10000, vehicle.getOdometer());
        assertEquals(new BigDecimal("20000.50"), vehicle.getPrice());
    }

    @Test
    void invalidNumbersAreRejectedWithoutChangingExistingValues() {
        Vehicle vehicle = sampleVehicle();

        assertThrows(IllegalArgumentException.class, () -> vehicle.setVin(0));
        assertThrows(IllegalArgumentException.class, () -> vehicle.setYear(1885));
        assertThrows(IllegalArgumentException.class, () -> vehicle.setYear(10000));
        assertThrows(IllegalArgumentException.class, () -> vehicle.setOdometer(-1));
        assertThrows(IllegalArgumentException.class, () -> vehicle.setPrice(new BigDecimal("-0.01")));
        assertThrows(IllegalArgumentException.class, () -> vehicle.setPrice(new BigDecimal("1000000000")));
        assertThrows(IllegalArgumentException.class, () -> vehicle.setPrice(new BigDecimal("10.001")));
        assertThrows(IllegalArgumentException.class, () -> vehicle.setPrice(null));

        assertEquals(1, vehicle.getVin());
        assertEquals(2022, vehicle.getYear());
        assertEquals(15000, vehicle.getOdometer());
        assertEquals(new BigDecimal("25000.00"), vehicle.getPrice());
    }

    @Test
    void blankOrFileBreakingTextAndUnknownTypesAreRejected() {
        Vehicle vehicle = sampleVehicle();

        assertThrows(IllegalArgumentException.class, () -> vehicle.setMake("  "));
        assertThrows(IllegalArgumentException.class, () -> vehicle.setModel(null));
        assertThrows(IllegalArgumentException.class, () -> vehicle.setColor("Blue|Red"));
        assertThrows(IllegalArgumentException.class, () -> vehicle.setModel("Camry\nLE"));
        assertThrows(IllegalArgumentException.class, () -> vehicle.setMake("\tToyota"));
        assertThrows(IllegalArgumentException.class, () -> vehicle.setVehicleType("boat"));
    }

    @Test
    void zeroMileageAndZeroPriceAreAllowed() {
        Vehicle vehicle = sampleVehicle();
        vehicle.setOdometer(0);
        vehicle.setPrice(BigDecimal.ZERO);

        assertEquals(0, vehicle.getOdometer());
        assertEquals(new BigDecimal("0.00"), vehicle.getPrice());
    }

    private Vehicle sampleVehicle() {
        return new Vehicle(1, 2022, "Toyota", "Camry", "car", "Blue", 15000,
                new BigDecimal("25000.00"));
    }
}
