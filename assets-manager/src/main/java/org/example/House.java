package org.example;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public class House extends Asset {
    private String address;
    private int condition;
    private int squareFoot;
    private int lotSize;

    public House(String description, String dateAcquired, BigDecimal originalCost,
                 String address, int condition, int squareFoot, int lotSize) {
        // The parent constructor sets the three fields shared by all assets.
        super(description, dateAcquired, originalCost);
        setAddress(address);
        setCondition(condition);
        setSquareFoot(squareFoot);
        setLotSize(lotSize);
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = Objects.requireNonNull(address, "Address is required.");
    }

    public int getCondition() {
        return condition;
    }

    public void setCondition(int condition) {
        if (condition < 1 || condition > 4) {
            throw new IllegalArgumentException("Condition must be 1 (excellent), 2 (good), 3 (fair), or 4 (poor).");
        }
        this.condition = condition;
    }

    public int getSquareFoot() {
        return squareFoot;
    }

    public void setSquareFoot(int squareFoot) {
        if (squareFoot < 0) {
            throw new IllegalArgumentException("House square footage cannot be negative.");
        }
        this.squareFoot = squareFoot;
    }

    public int getLotSize() {
        return lotSize;
    }

    public void setLotSize(int lotSize) {
        if (lotSize < 0) {
            throw new IllegalArgumentException("Lot size cannot be negative.");
        }
        this.lotSize = lotSize;
    }

    @Override
    public BigDecimal getValue() {
        BigDecimal pricePerSquareFoot;
        switch (condition) {
            case 1:
                pricePerSquareFoot = new BigDecimal("180.00");
                break;
            case 2:
                pricePerSquareFoot = new BigDecimal("130.00");
                break;
            case 3:
                pricePerSquareFoot = new BigDecimal("90.00");
                break;
            case 4:
                pricePerSquareFoot = new BigDecimal("80.00");
                break;
            default:
                throw new IllegalStateException("Unknown house condition.");
        }

        BigDecimal houseValue = pricePerSquareFoot.multiply(BigDecimal.valueOf(squareFoot));
        BigDecimal landValue = new BigDecimal("0.25").multiply(BigDecimal.valueOf(lotSize));
        return houseValue.add(landValue).setScale(2, RoundingMode.HALF_UP);
    }
}
