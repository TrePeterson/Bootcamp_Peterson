package org.example;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Year;
import java.util.Objects;
import java.util.regex.Pattern;

public class Vehicle extends Asset {
    private String makeModel;
    private int year;
    private int odometer;

    public Vehicle(String description, String dateAcquired, BigDecimal originalCost,
                   String makeModel, int year, int odometer) {
        super (description, dateAcquired, originalCost);
        setMakeModel(makeModel);
        setYear(year);
        setOdometer(odometer);
    }

    public String getMakeModel() {
        return makeModel;
    }

    public void setMakeModel(String makeModel) {
        this.makeModel = Objects.requireNonNull(makeModel, "Make and model are required.");
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        if (year < 1) {
            throw new IllegalArgumentException("Model year must be positive.");
        }
        this.year = year;
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

    @Override
    public BigDecimal getValue() {
        // Age uses the model year, not the date this owner acquired the vehicle.
        // Future model years count as age zero so depreciation cannot add value.
        int age = Math.max(0, Year.now().getValue() - year);
        BigDecimal value;

        if (age > 10) {
            value = new BigDecimal("1000.00");
        } else {
            BigDecimal yearlyRate;
            if (age <= 3) {
                yearlyRate = new BigDecimal("0.03");
            } else if (age <= 6) {
                yearlyRate = new BigDecimal("0.06");
            } else {
                yearlyRate = new BigDecimal("0.08");
            }

            // Working assumption: the current age bracket's rate applies to every
            // year of age, based on original cost (not compounded depreciation).
            BigDecimal depreciation = getOriginalCost()
                    .multiply(yearlyRate)
                    .multiply(BigDecimal.valueOf(age));
            value = getOriginalCost().subtract(depreciation);
        }

        if (odometer > 100_000 && !hasMileageExemption()) {
            value = value.multiply(new BigDecimal("0.75"));
        }

        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private boolean hasMileageExemption() {
        // Word boundaries prevent a name such as "NotHonda" from getting the exception.
        return Pattern.compile("\\b(honda|toyota)\\b", Pattern.CASE_INSENSITIVE)
                .matcher(makeModel)
                .find();
    }
}
