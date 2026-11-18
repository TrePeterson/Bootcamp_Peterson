package org.example;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Year;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AssetTest {

    @Test
    void baseAssetKeepsItsOriginalCostAsItsValue() {
        Asset asset = new Asset("Computer", "2026-01-10", new BigDecimal("1999.99"));

        assertEquals(new BigDecimal("1999.99"), asset.getValue());
    }

    @Test
    void assetCollectionUsesEachObjectsOwnValuationMethod() {
        ArrayList<Asset> assets = new ArrayList<>();
        assets.add(new Asset("Computer", "2026-01-10", new BigDecimal("1999.99")));
        assets.add(new House("Home", "2020-06-01", new BigDecimal("150000.00"),
                "10 Example Lane", 3, 2000, 8000));
        assets.add(new Vehicle("Car", "2026-01-10", new BigDecimal("20000.00"),
                "Ford Focus", Year.now().getValue(), 1000));

        // These are Asset references, but Java still calls the overridden methods.
        assertAll(
                () -> assertEquals(new BigDecimal("1999.99"), assets.get(0).getValue()),
                () -> assertEquals(new BigDecimal("182000.00"), assets.get(1).getValue()),
                () -> assertEquals(new BigDecimal("20000.00"), assets.get(2).getValue())
        );
    }

    @Test
    void originalCostCannotBeNegative() {
        assertThrows(IllegalArgumentException.class,
                () -> new Asset("Computer", "2026-01-10", new BigDecimal("-0.01")));
    }
}
