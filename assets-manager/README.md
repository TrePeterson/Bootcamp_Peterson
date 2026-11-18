# Company 08 | Fire Force Asset Registry

A Fire Force-inspired Java 17 bootcamp exercise about inheritance and polymorphism. It prints a styled report for two fictional houses and two fictional vehicles, with orange, cyan, red, and white-gray console accents, bordered asset cards, and aligned money totals. It has no interactive menu and does not save data.

The sample assets are Shinra's Company 8 Residence, Maki's Training Retreat, a Matchbox-Inspired Support Truck, and Arthur's Knight Patrol Car. These are invented examples for the exercise, not claims about the series. The project folder remains `assets-manager`.

## Run in IntelliJ

1. Open this project's `pom.xml` as a project and load Maven changes.
2. Set the project SDK to Amazon Corretto 17.
3. Open `src/main/java/org/example/Main.java` and run its `main()` method.
4. Run the test classes under `src/test/java/org/example`, or run the Maven `test` lifecycle.

If Maven is on your terminal's PATH, use `mvn test` to compile and test, then `java -cp target/classes org.example.Main` to run the report.

Colors are enabled by default for an ANSI-capable console, including the IntelliJ Run console. For plain text, add `-Dassets.color=false` to the run configuration's **VM options**, or set the `NO_COLOR` environment variable. From a terminal, use:

```text
java -Dassets.color=false -cp target/classes org.example.Main
```

## How the classes fit together

- `Asset` holds the description, acquisition date (a String, as required), and original cost shared by every asset. Its default `getValue()` returns original cost.
- `House extends Asset` adds an address, condition, house size, and lot size. Its `getValue()` calculates the house and land value.
- `Vehicle extends Asset` adds make/model, model year, and mileage. Its `getValue()` calculates the age adjustment followed by the mileage adjustment.
- `Main` puts both types into one `ArrayList<Asset>`. Calling `asset.getValue()` selects the actual object's override. It uses `instanceof` and explicit downcasts to display an address for a house or year/make/model for a vehicle.
- `ConsoleHelper` handles the report's colors, borders, and currency formatting. It keeps display details in one reusable place; the asset classes still handle their own information and calculations.

Think of `Main` as the person reading out the report and `ConsoleHelper` as the person making the display board look good. `House` and `Vehicle` supply the facts and calculate their values; the helper decides how those facts appear on screen. Changing an accent color does not require changing a valuation formula.

`super(...)` passes the shared information to the parent constructor. `@Override` lets the compiler check that a child method really replaces a parent method. `BigDecimal` keeps decimal calculations precise; decimal constants are constructed from Strings, and calculated values are rounded to cents at the end.

## Valuation rules

House value is house square footage multiplied by the condition rate, plus $0.25 per square foot of land. Conditions 1 through 4 use $180, $130, $90, and $80 respectively.

Vehicle age is the current calendar year minus the model year. The working interpretation of the worksheet is **original cost minus (original cost times the current age bracket's yearly rate times age)**. This interpretation has not yet been confirmed with the instructor. It does not apply progressive brackets or compound depreciation.

- Ages 0 through 3: 3% of original cost per year.
- Ages 4 through 6: 6% of original cost per year.
- Ages 7 through 10: 8% of original cost per year.
- Older than 10: $1,000 before any mileage adjustment.
- More than 100,000 miles: reduce the resulting value by 25%, unless make/model contains the complete word Honda or Toyota, regardless of capitalization. Exactly 100,000 miles does not trigger this adjustment.

For example, a four-year-old vehicle bought for $20,000 is worth $15,200 before mileage adjustments. At 125,000 miles, the Ford example is worth $11,400; the Honda remains $15,200.

Additional implementation choices: reject negative cost, square footage, lot size, or mileage; reject condition codes outside 1 through 4 and model years below 1; treat a future model year as age zero. The worksheet's simplified rules are used for this exercise, not as real appraisal guidance.

The sample report totals $475,000.00 in original cost and $497,600.00 in calculated value. Its sample vehicle years are relative to the current year so the demonstration always uses four-year-old vehicles.
