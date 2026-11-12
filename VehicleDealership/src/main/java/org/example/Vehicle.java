package org.example;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/** The details for one vehicle in the dealership's inventory. */
public class Vehicle {
    private int vin;
    private int year;
    private String make;
    private String model;
    private String vehicleType;
    private String color;
    private int odometer;
    private BigDecimal price;

    public Vehicle(int vin, int year, String make, String model, String vehicleType,
                   String color, int odometer, BigDecimal price) {
        setVin(vin);
        setYear(year);
        setMake(make);
        setModel(model);
        setVehicleType(vehicleType);
        setColor(color);
        setOdometer(odometer);
        setPrice(price);
    }

    public int getVin() {
        return vin;
    }

    public void setVin(int vin) {
        if (vin <= 0) {
            throw new IllegalArgumentException("VIN must be a positive whole number.");
        }
        this.vin = vin;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        if (year < 1886 || year > 9999) {
            throw new IllegalArgumentException("Year must be between 1886 and 9999.");
        }
        this.year = year;
    }

    public String getMake() {
        return make;
    }

    public void setMake(String make) {
        this.make = requireText(make, "Make");
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = requireText(model, "Model");
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        String type = requireText(vehicleType, "Vehicle type").toLowerCase(Locale.ROOT);
        switch (type) {
            case "car", "truck", "van" -> this.vehicleType = type;
            case "suv" -> this.vehicleType = "SUV";
            default -> throw new IllegalArgumentException("Vehicle type must be car, truck, SUV, or van.");
        }
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = requireText(color, "Color");
    }

    public int getOdometer() {
        return odometer;
    }

    public void setOdometer(int odometer) {
        if (odometer < 0) {
            throw new IllegalArgumentException("Mileage cannot be negative.");
        }
        this.odometer = odometer;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        if (price == null || price.signum() < 0
                || price.compareTo(new BigDecimal("999999999.99")) > 0) {
            throw new IllegalArgumentException("Price must be between $0.00 and $999,999,999.99.");
        }
        try {
            // Reject fractions of a cent instead of silently changing the asking price.
            this.price = price.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Price can have no more than two decimal places.", exception);
        }
    }

    // Pipe characters and control characters would break the inventory file format.
    static String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " cannot be blank.");
        }
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == '|' || Character.isISOControl(character)) {
                throw new IllegalArgumentException(label + " cannot contain pipes or control characters.");
            }
        }
        return value.trim();
    }
}
