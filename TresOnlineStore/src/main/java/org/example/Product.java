package org.example;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

public final class Product {
    private final String sku;
    private final String name;
    private final BigDecimal price;
    private final String department;

    public Product(String sku, String name, BigDecimal price, String department) {
        this.sku = requireText(sku, "SKU").toUpperCase(Locale.ROOT);
        this.name = requireText(name, "Product name");
        this.department = requireText(department, "Department");

        if (price == null || price.signum() < 0) {
            throw new IllegalArgumentException("Price must be zero or greater.");
        }
        try {
            this.price = price.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Price must not include fractions of a cent.", exception);
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value.trim();
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getDepartment() {
        return department;
    }
}
