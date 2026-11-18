package org.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HouseTest {

    @ParameterizedTest(name = "Condition {0} gives a house value of {1}")
    @CsvSource({
            "1, 362000.00",
            "2, 262000.00",
            "3, 182000.00",
            "4, 162000.00"
    })
    void usesTheConditionRateAndAddsTheLotValue(int condition, String expected) {
        House house = createHouse(condition, 2000, 8000);

        assertEquals(new BigDecimal(expected), house.getValue());
    }

    @Test
    void changingHouseDetailsRecalculatesItsValue() {
        House house = createHouse(1, 1000, 1);
        assertEquals(new BigDecimal("180000.25"), house.getValue());

        house.setCondition(4);
        house.setSquareFoot(1500);
        house.setLotSize(3);

        assertEquals(new BigDecimal("120000.75"), house.getValue());
    }

    @Test
    void rejectsInvalidConditionAndNegativeMeasurements() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> createHouse(0, 1000, 8000)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> createHouse(5, 1000, 8000)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> createHouse(1, -1, 8000)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> createHouse(1, 1000, -1))
        );
    }

    private House createHouse(int condition, int squareFoot, int lotSize) {
        return new House("Home", "2020-06-01", new BigDecimal("150000.00"),
                "10 Example Lane", condition, squareFoot, lotSize);
    }
}
