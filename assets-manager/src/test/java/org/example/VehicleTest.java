package org.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.Year;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VehicleTest {

    @ParameterizedTest(name = "Age {0} gives a vehicle value of {1}")
    @CsvSource({
            "0, 20000.00",
            "3, 18200.00",
            "4, 15200.00",
            "6, 12800.00",
            "7, 8800.00",
            "10, 4000.00",
            "11, 1000.00"
    })
    void handlesTheEdgesOfEachAgeBracket(int age, String expected) {
        Vehicle vehicle = createVehicle(age, "Ford Focus", 50000);

        assertEquals(new BigDecimal(expected), vehicle.getValue());
    }

    @ParameterizedTest(name = "{0} at {1} miles is worth {2}")
    @CsvSource({
            "Ford Focus, 100000, 15200.00",
            "Ford Focus, 100001, 11400.00",
            "Honda Accord, 100001, 15200.00",
            "toyota corolla, 100001, 15200.00",
            "NotHonda Example, 100001, 11400.00"
    })
    void appliesTheMileagePenaltyOnlyWhenRequired(String makeModel, int mileage, String expected) {
        Vehicle vehicle = createVehicle(4, makeModel, mileage);

        assertEquals(new BigDecimal(expected), vehicle.getValue());
    }

    @Test
    void mileagePenaltyAlsoAppliesToTheOverTenYearValue() {
        Vehicle vehicle = createVehicle(11, "Ford Focus", 100001);

        assertEquals(new BigDecimal("750.00"), vehicle.getValue());
    }

    @Test
    void futureModelYearDoesNotProduceNegativeAgeDepreciation() {
        Vehicle vehicle = createVehicle(-1, "Ford Focus", 1000);

        assertEquals(new BigDecimal("20000.00"), vehicle.getValue());
    }

    @Test
    void changingMileageAndMakeRecalculatesItsValue() {
        Vehicle vehicle = createVehicle(4, "Ford Focus", 100000);
        vehicle.setOdometer(100001);
        assertEquals(new BigDecimal("11400.00"), vehicle.getValue());

        vehicle.setMakeModel("Toyota Corolla");
        assertEquals(new BigDecimal("15200.00"), vehicle.getValue());
    }

    @Test
    void roundsTheFinalCalculatedValueToCents() {
        Vehicle vehicle = new Vehicle("Car", "2020-06-01", new BigDecimal("100.01"),
                "Ford Focus", Year.now().getValue() - 1, 100001);

        // 100.01 x 0.97 x 0.75 = 72.757275, rounded once to 72.76.
        assertEquals(new BigDecimal("72.76"), vehicle.getValue());
    }

    @Test
    void rejectsInvalidYearAndNegativeMileage() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new Vehicle("Car", "2020-06-01", new BigDecimal("20000.00"),
                                "Ford Focus", 0, 1000)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> createVehicle(4, "Ford Focus", -1))
        );
    }

    private Vehicle createVehicle(int age, String makeModel, int mileage) {
        return new Vehicle("Car", "2020-06-01", new BigDecimal("20000.00"),
                makeModel, Year.now().getValue() - age, mileage);
    }
}
