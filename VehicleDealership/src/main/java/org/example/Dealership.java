package org.example;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Owns the vehicle inventory and the rules for searching or changing it. */
public class Dealership {
    private String name;
    private String address;
    private String phone;
    private final ArrayList<Vehicle> inventory;

    public Dealership(String name, String address, String phone) {
        setName(name);
        setAddress(address);
        setPhone(phone);
        inventory = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = Vehicle.requireText(name, "Dealership name");
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = Vehicle.requireText(address, "Address");
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = Vehicle.requireText(phone, "Phone");
    }

    public List<Vehicle> getVehiclesByPrice(BigDecimal min, BigDecimal max) {
        if (min == null || max == null || min.signum() < 0 || min.compareTo(max) > 0) {
            throw new IllegalArgumentException("Enter a nonnegative minimum price no higher than the maximum.");
        }
        List<Vehicle> matches = new ArrayList<>();
        for (Vehicle vehicle : inventory) {
            if (vehicle.getPrice().compareTo(min) >= 0 && vehicle.getPrice().compareTo(max) <= 0) {
                matches.add(vehicle);
            }
        }
        return matches;
    }

    public List<Vehicle> getVehiclesByMakeModel(String make, String model) {
        String makeQuery = normalizeQuery(make);
        String modelQuery = normalizeQuery(model);
        if (makeQuery.isEmpty() && modelQuery.isEmpty()) {
            throw new IllegalArgumentException("Enter at least part of a make or model.");
        }
        List<Vehicle> matches = new ArrayList<>();
        for (Vehicle vehicle : inventory) {
            if (vehicle.getMake().toLowerCase(Locale.ROOT).contains(makeQuery)
                    && vehicle.getModel().toLowerCase(Locale.ROOT).contains(modelQuery)) {
                matches.add(vehicle);
            }
        }
        return matches;
    }

    public List<Vehicle> getVehiclesByYear(int min, int max) {
        requireRange(min, max, "Year");
        List<Vehicle> matches = new ArrayList<>();
        for (Vehicle vehicle : inventory) {
            if (vehicle.getYear() >= min && vehicle.getYear() <= max) {
                matches.add(vehicle);
            }
        }
        return matches;
    }

    public List<Vehicle> getVehiclesByColor(String color) {
        String query = Vehicle.requireText(color, "Color");
        List<Vehicle> matches = new ArrayList<>();
        for (Vehicle vehicle : inventory) {
            if (vehicle.getColor().equalsIgnoreCase(query)) {
                matches.add(vehicle);
            }
        }
        return matches;
    }

    public List<Vehicle> getVehiclesByMileage(int min, int max) {
        requireRange(min, max, "Mileage");
        List<Vehicle> matches = new ArrayList<>();
        for (Vehicle vehicle : inventory) {
            if (vehicle.getOdometer() >= min && vehicle.getOdometer() <= max) {
                matches.add(vehicle);
            }
        }
        return matches;
    }

    public List<Vehicle> getVehiclesByType(String vehicleType) {
        String query = Vehicle.requireText(vehicleType, "Vehicle type");
        List<Vehicle> matches = new ArrayList<>();
        for (Vehicle vehicle : inventory) {
            if (vehicle.getVehicleType().equalsIgnoreCase(query)) {
                matches.add(vehicle);
            }
        }
        return matches;
    }

    public List<Vehicle> getAllVehicles() {
        // A separate list prevents callers from bypassing addVehicle/removeVehicle.
        return new ArrayList<>(inventory);
    }

    public Vehicle findVehicleByVin(int vin) {
        for (Vehicle vehicle : inventory) {
            if (vehicle.getVin() == vin) {
                return vehicle;
            }
        }
        return null;
    }

    public void addVehicle(Vehicle vehicle) {
        if (vehicle == null) {
            throw new IllegalArgumentException("Choose a vehicle to add.");
        }
        if (findVehicleByVin(vehicle.getVin()) != null) {
            throw new IllegalArgumentException("VIN " + vehicle.getVin() + " is already in the inventory.");
        }
        inventory.add(vehicle);
    }

    public boolean removeVehicle(Vehicle vehicle) {
        if (vehicle == null) {
            return false;
        }
        for (int index = 0; index < inventory.size(); index++) {
            if (inventory.get(index).getVin() == vehicle.getVin()) {
                inventory.remove(index);
                return true;
            }
        }
        return false;
    }

    private static String normalizeQuery(String query) {
        if (query == null || query.isBlank()) {
            return "";
        }
        return Vehicle.requireText(query, "Search text").toLowerCase(Locale.ROOT);
    }

    private static void requireRange(int min, int max, String label) {
        if (min < 0 || min > max) {
            throw new IllegalArgumentException(label + " minimum must be nonnegative and no higher than the maximum.");
        }
    }
}
