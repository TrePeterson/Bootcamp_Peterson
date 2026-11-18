package org.example;

import java.math.BigDecimal;
import java.util.Objects;

public class Asset {
    private String description;
    private String dateAcquired;
    private BigDecimal originalCost;

    public Asset(String description, String dateAcquired, BigDecimal originalCost) {
        setDescription(description);
        setDateAcquired(dateAcquired);
        setOriginalCost(originalCost);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = Objects.requireNonNull(description, "Description is required.");
    }

    public String getDateAcquired() {
        return dateAcquired;
    }

    public void setDateAcquired(String dateAcquired) {
        this.dateAcquired = Objects.requireNonNull(dateAcquired, "Acquisition date is required.");
    }

    public BigDecimal getOriginalCost() {
        return originalCost;
    }

    public void setOriginalCost(BigDecimal originalCost) {
        Objects.requireNonNull(originalCost, "Original cost is required.");
        if (originalCost.signum() < 0) {
            throw new IllegalArgumentException("Original cost cannot be negative.");
        }
        this.originalCost = originalCost;
    }

    // Subclasses replace this default calculation with their own valuation rules.
    public BigDecimal getValue() {
        return originalCost;
    }
}
