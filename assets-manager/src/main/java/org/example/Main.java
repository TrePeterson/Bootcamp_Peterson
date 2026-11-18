package org.example;

import java.math.BigDecimal;
import java.time.Year;
import java.util.ArrayList;

public class Main {
    public static void main(String[] args) {
        int currentYear = Year.now().getValue();
        ArrayList<Asset> assets = createAssets(currentYear);
        ConsoleHelper.banner(currentYear);

        BigDecimal totalOriginalCost = BigDecimal.ZERO;
        BigDecimal totalValue = BigDecimal.ZERO;
        int assetNumber = 1;

        for (Asset asset : assets) {
            ConsoleHelper.startCard(String.format("ASSET %02d / %s", assetNumber, asset.getDescription()));
            ConsoleHelper.field("Acquired:", asset.getDateAcquired());

            // Check the actual type before casting to access its extra fields.
            if (asset instanceof House) {
                House house = (House) asset;
                ConsoleHelper.field("Asset type:", "House / company residence");
                ConsoleHelper.field("Address:", house.getAddress());
                ConsoleHelper.field("House / lot:", String.format("%,d sq ft / %,d sq ft",
                        house.getSquareFoot(), house.getLotSize()));
            } else if (asset instanceof Vehicle) {
                Vehicle vehicle = (Vehicle) asset;
                ConsoleHelper.field("Asset type:", "Vehicle / support fleet");
                ConsoleHelper.field("Vehicle:", vehicle.getYear() + " " + vehicle.getMakeModel());
                ConsoleHelper.field("Mileage:", String.format("%,d miles", vehicle.getOdometer()));
            }

            // Polymorphism: Java chooses House.getValue() or Vehicle.getValue().
            BigDecimal currentValue = asset.getValue();
            ConsoleHelper.money("Original cost:", asset.getOriginalCost());
            ConsoleHelper.money("Current value:", currentValue);
            ConsoleHelper.endCard();
            totalOriginalCost = totalOriginalCost.add(asset.getOriginalCost());
            totalValue = totalValue.add(currentValue);
            assetNumber++;
        }

        ConsoleHelper.summary(assets.size(), totalOriginalCost, totalValue);
    }

    private static ArrayList<Asset> createAssets(int currentYear) {
        ArrayList<Asset> assets = new ArrayList<>();

        // Fan-inspired sample assets and addresses, not canonical or real property.
        assets.add(new House("Shinra's Company 8 Residence", "2021-06-15", new BigDecimal("310000.00"),
                "8 Ember Lane, Tokyo Training District", 1, 2000, 8000));
        assets.add(new House("Maki's Training Retreat", "2023-04-20", new BigDecimal("125000.00"),
                "24 Blue Flame Way, Tokyo Training District", 3, 1200, 4000));
        assets.add(new Vehicle("Matchbox-Inspired Support Truck", "2024-03-10", new BigDecimal("20000.00"),
                "Ford Ranger", currentYear - 4, 125000));
        assets.add(new Vehicle("Arthur's Knight Patrol Car", "2024-09-05", new BigDecimal("20000.00"),
                "Honda Civic", currentYear - 4, 125000));
        return assets;
    }
}
