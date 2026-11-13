# Ryuk Motors

A Death Note-inspired dealership inventory app built by Tre Peterson for the Object-Oriented Car Dealership workshop. The console uses crimson, violet, and cool-gray accents, boxed menus, and aligned vehicle tables.

## What it does

| Menu | Action |
| --- | --- |
| 1 | Find vehicles in an inclusive price range |
| 2 | Search by part of a make and/or model, regardless of capitalization |
| 3 | Find vehicles in an inclusive year range |
| 4 | Find vehicles by color, regardless of capitalization |
| 5 | Find vehicles in an inclusive mileage range |
| 6 | Find cars, trucks, SUVs, or vans |
| 7 | List the full inventory, sorted by make and model |
| 8 | Review and add a vehicle, then save it |
| 9 | Review and remove a vehicle by VIN, then save the change |
| 99 | Exit |

Every successful add or removal immediately updates `inventory.csv`. A confirmation appears after the save completes. Invalid input is explained and can be corrected without restarting. Type **X** at any form prompt to cancel. Press **Enter** after viewing results to return to the menu.

## Run in IntelliJ

1. Choose **File > Open** and open this project's `pom.xml` as a project.
2. Set the project SDK to **Amazon Corretto 17** (another Java 17 JDK also works) and load the Maven project.
3. Open `src/main/java/org/example/Program.java` and run its `main()` method.
4. The run configuration's **working directory must be this project's root folder**, which contains both `pom.xml` and `inventory.csv`.

This application uses a console window; it does not need JavaFX or an external table library.

## Build and test

With Maven and JDK 17 configured, run these commands from the project root:

```text
mvn verify
java -jar target/vehicle-dealership-1.0-SNAPSHOT.jar
```

IntelliJ also provides Maven: open the Maven tool window, expand **Lifecycle**, and run **verify**. You can run the tests from the green gutter icons in `src/test/java/org/example`.

The JUnit tests cover inclusive search boundaries, case-insensitive matching, model validation, duplicate VINs, file round trips, malformed files, menu navigation, cancellation, EOF, and simulated save failures. Tests use temporary inventory files so they do not change the real inventory.

To disable ANSI color (for a terminal that shows escape characters), add the VM option:

```text
-Ddealership.color=false
```

The `NO_COLOR` environment variable also disables color. To use a different inventory file, set `-Ddealership.inventory=/path/to/inventory.csv` in VM options. On Windows, forward slashes work in that path too.

## How the five classes work

| Class | Its job | Think of it as... |
| --- | --- | --- |
| `Program` | Creates the user interface and calls `display()` | The ignition key |
| `UserInterface` | Shows menus, asks questions, displays results, and coordinates saves | The front desk |
| `Dealership` | Holds the inventory and searches, adds, or removes vehicles | The inventory manager |
| `Vehicle` | Stores the eight details of one vehicle and validates them | One vehicle's information card |
| `DealershipFileManager` | Reads and writes the pipe-delimited inventory file | The filing cabinet |

For example, choosing **1** calls `processGetByPriceRequest()` in `UserInterface`. That method reads two prices and asks `Dealership.getVehiclesByPrice()` to find matches. The dealership loops through its inventory, compares each price with both limits, and returns a list. The UI displays that list using the same `displayVehicles()` helper used by all other searches.

### Why the extra pieces exist

- **`ArrayList<Vehicle>`** is a list that can grow or shrink when vehicles arrive or leave.
- **`BigDecimal`** stores prices as decimal amounts. The app accepts cents without silently rounding extra fractions of a cent. `compareTo()` compares monetary values even when their written decimal scales differ.
- **`getAllVehicles()` returns a separate list.** Removing a row from a search result does not secretly remove it from the dealership. The vehicle objects themselves remain mutable through the workbook's required setters.
- **One formatting helper** prints every result table. Prices and mileages line up consistently, and long text gets its full details below the table.
- **A candidate inventory** is prepared for an add/removal. The UI adopts it only after saving succeeds, so a failed write does not leave the screen showing an unsaved change.
- **A temporary file** holds the completed new file before it replaces the original. The file manager requests an atomic replacement when the filesystem supports it; otherwise it uses a normal replacement.
- **Small exceptions handle problems.** Invalid model data is rejected. File errors become helpful messages. The private `CancelledInput` signal returns from any form when the user enters X before a save.
- **Injectable input/output** means tests can type into the menus and read the answers automatically, without opening IntelliJ's Run window.

## Inventory format

The first line stores the dealership name, address, and phone:

```text
Ryuk Motors|13 Black Ink Avenue|704-555-0130
```

Following lines store `vin|year|make|model|vehicleType|color|odometer|price`:

```text
44901|2012|Honda|Civic|car|Gray|103221|6995.00
```

There is no column-header row. Text cannot contain pipes or control characters because they would damage this simple file format. A malformed file reports the offending line and stops loading; it is never silently replaced with an empty inventory.

The bundled 20 vehicles are **fictional demonstration inventory**, not actual listings or current market valuations. The first three vehicle rows follow the workbook example. The dealership address and phone are fictional.

`data/inventory-starter.csv` is a separate copy of the starting inventory. To reset the demo, first close the app and back up your current `inventory.csv`. Then copy the starter file over `inventory.csv`. Resetting replaces your added/removed vehicle data.

## Deliberate choices and limits

- VIN is a **positive integer inventory identifier**, as required by the workbook. This is not a real 17-character automotive VIN system.
- Year input accepts 1886–9999; this is an input guard, not a claim about which model years are currently on sale.
- Mileage accepts whole nonnegative miles. Asking prices accept $0.00–$999,999,999.99 and at most two decimal places in the menu.
- Make/model searches use partial matching; a blank make or model means any value for that field. At least one must be entered. Color and type searches match the entire value without regard to capitalization.
- All range boundaries are inclusive. Maximums must be at least their minimums.
- Add and remove require a Y/N confirmation. There is no edit, sales contract, financing, or customer-payment feature in this workshop.
- Run one copy of the app per inventory file. The app loads at startup and does not merge simultaneous changes made by other programs.

## A quick practice walkthrough

1. Choose **7** to inspect the 20 starter vehicles.
2. Choose **2**, enter `hon` for make and `civ` for model. Both Honda Civic entries appear.
3. Choose **1**, enter `0` then `7000`. The three workbook-example vehicles appear.
4. Choose **8**, add a vehicle with an unused integer VIN, review it, and confirm with Y.
5. Exit with **99** and run again. The vehicle is still there.
6. Choose **9**, enter that VIN, and confirm Y. Run again to confirm it stays removed.
