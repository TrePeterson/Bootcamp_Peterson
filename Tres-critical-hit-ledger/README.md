# POTARA FUSION LEDGER

**Two sides of your money. One powerful ledger.**

A Java application with a console interface and an optional JavaFX desktop interface for recording deposits, tracking payments, and exploring transaction history. Built for the Java Focus Academy Accounting Ledger capstone, the app follows the business theme of **Potara Fusion Gear**, which supplies keyboards, mice, monitors, headsets, and RAM/SSD upgrades.

**Potara Customs builds your PC. Potara Fusion Gear completes your setup.**

The theme connects Potara Fusion Gear's setup accessories and upgrades with **Potara Customs**, a PC-building business. The demonstration records include collaboration sales to Potara Customs and expenses for inventory and shipping. These are fictional transactions used to demonstrate the ledger.

The Dragon Ball Potara-inspired interface uses gold borders, green headings and prompts, and purple accents.

Transactions are saved locally, so the ledger is ready when you reopen the application.

## Features

- **Record deposits and payments:** review each transaction and confirm before saving.
- **Keep transaction history:** load and save records in `transactions.csv`.
- **Browse the ledger:** view all transactions, deposits only, or payments only, newest first.
- **Run calendar reports:** month to date, previous month, year to date, and previous year.
- **Search by vendor:** find a vendor's transactions without worrying about capitalization.
- **Combine custom filters:** search by date range, partial description, partial vendor name, and exact amount. Leave a field blank to skip it.
- **See totals:** view deposits, payments, and net change for the displayed records.
- **Use a polished console interface:** colored tables, aligned dollar amounts, wrapped descriptions, clear prompts, cancellation, and saved/not-saved messages.
- **Choose the desktop interface:** a JavaFX dashboard with transaction totals, a searchable table, calendar reports, advanced filters, and deposit/payment forms that require review before saving.
- **Explore connected transactions:** click a keyboard, mouse, or headset illustration to filter transaction text, or open the Potara Customs view to match that vendor exactly.

## Technology

| Technology | Purpose |
|---|---|
| Java 17 / Amazon Corretto 17 | Application runtime |
| Maven | Build configuration and dependencies |
| JavaFX 17.0.18 | Optional desktop interface, tables, forms, and dialogs |
| JavaFX Maven Plugin 0.0.8 | Starts the desktop application with its JavaFX runtime |
| ASCII Table 0.3.2 | Table layout and text wrapping |
| `BigDecimal` | Exact dollar-and-cent calculations |
| `LocalDate` and `LocalTime` | Dates, times, and calendar reports |
| Java collections | Store, sort, and filter transaction history |
| Java file I/O | Read and safely replace the ledger file |
| ANSI escape codes | Console colors |

## Getting Started

1. Clone or download this repository.
2. Open the project's root `pom.xml` in IntelliJ IDEA as a Maven project.
3. Select **Amazon Corretto 17**, or another Java 17 JDK, as the project SDK.
4. Let Maven download the dependencies. Internet access is needed for the first download.
5. Use the project root—the folder containing `pom.xml`—as the run configuration's working directory.
6. Choose either the console or desktop launch instructions below.

### Run the console

Open `src/main/java/org/example/Main.java` and run its `main` method in IntelliJ. The console prints the full path to its data file at startup.

### Run the desktop interface

From the project root, run:

```shell
mvn javafx:run
```

In IntelliJ, you can also open the **Maven** tool window, expand **Plugins > javafx**, and double-click **javafx:run**. Reload the Maven project if the plugin is not listed. This launches `DesktopLauncher`, which starts `LedgerDesktopApp`; JavaFX is downloaded through Maven and is not bundled into Corretto itself.

Both interfaces use the same `transactions.csv` and the same transaction, reporting, and file-saving classes. If the selected file does not exist, it creates an empty ledger with the required header. Use one writing instance at a time: close the console before recording transactions in the desktop app, or vice versa.

To compile and package from the project root with Maven installed:

```shell
mvn package
```

Run the console through IntelliJ with its Maven dependencies, or use the JavaFX launch command above for the desktop app. The generated JAR is a standard build artifact, not a standalone executable JAR.

## Console Navigation

```text
HOME
  D  Add deposit
  P  Make payment
  L  View ledger and reports
  X  Exit

LEDGER
  A  All transactions
  D  Deposits only
  P  Payments only
  R  Reports and custom search
  H  Return home

REPORTS
  1  Month to date
  2  Previous month
  3  Year to date
  4  Previous year
  5  Search by vendor
  6  Custom search
  0  Back to ledger
```

Menu letters accept uppercase or lowercase. Enter **X** during transaction entry or search prompts to cancel.

### Recording a transaction

Choose **D** or **P** from Home, enter the details, and review the preview. Pressing Enter at the date or time prompt uses the current date or time. Enter a positive amount for either transaction type; the payment screen applies the negative sign automatically. Confirm with **Y** to save, or **N** to cancel.

### Searching and reporting

The vendor report matches the full vendor name, ignoring capitalization. Custom Search supports partial vendor names and descriptions. Every filter you provide must match, and date ranges include both endpoints. An amount filter matches exactly: `25.00` finds deposits of that amount; `-25.00` finds payments.

Calendar reports use the computer's current local date. Previous month/year reports cover the entire previous calendar period; month/year-to-date reports end today. Future-dated records still appear in the full ledger.

## Desktop Navigation

- **Overview:** shows totals, the transaction table, and clickable hardware illustrations. The totals always describe the records in the current view.
- **Transactions:** focuses on the ledger. Select **All entries**, **Deposits**, or **Payments**, and use the search field to match a description or vendor without worrying about capitalization.
- **Reports & search:** choose **Month to date**, **Previous month**, **Year to date**, or **Previous year**. Advanced filters add inclusive start/end dates, a partial description, a vendor, and an exact signed amount. Check **Match full vendor name** for an exact vendor match. All active filters must match; blank fields are skipped.
- **Potara Customs:** opens the transactions for the full vendor name `Potara Customs`, ignoring capitalization.
- **Hardware cards:** clicking a keyboard, mouse, or headset illustration searches transaction text for that hardware. These images are filter shortcuts, not products for sale or a separate inventory.
- **+ Deposit / − Payment:** enter the date, time, description, vendor/customer, and positive amount. Choose **Review & save**, inspect the confirmation, and select **Save**. **Back** returns to the form; **Cancel** leaves without recording the transaction. Payments receive a negative sign automatically.
- **View selected entry:** select a table row and click this button, or double-click a row, to read the complete transaction details.
- **Clear filters:** returns the table and totals to all transactions. **Refresh** reloads saved records from the CSV file.

The desktop interface reports successful saves in its status bar and displays input or save problems in the relevant form. A failed save keeps the form open. If another program changes the file, refresh the desktop ledger before making a new entry; in the console, restart before saving again.

## Data File

`transactions.csv` lives beside `pom.xml` when using the project root as the working directory. Despite the `.csv` extension, its fields are separated by pipes (`|`):

```text
date|time|description|vendor|amount
2026-10-30|15:10:00|Accessory order shipping|Capsule Shipping|-18.75
2026-10-31|10:00:00|Fusion setup bundle sale|Potara Customs|399.97
```

- Dates use `YYYY-MM-DD`; times use 24-hour `HH:mm:ss`.
- Deposits are positive; payments are negative.
- Files use UTF-8 encoding.
- Amounts must be nonzero, have at most two decimal places, and stay within $999,999,999.99 in magnitude.
- Descriptions and vendor names allow up to 120 characters and cannot contain pipes or control characters.

The included sample file contains **20 fictional transactions** across 2025 and 2026, including October 31, 2026. The sample dates are fixed, so calendar report results depend on the current date. Preserve your own ledger when replacing or updating demonstration data. The two lines above are examples from the sample history.

## Project Structure

The application is branded **POTARA FUSION LEDGER** for the Potara Fusion Gear business theme. The existing project folder and Maven artifact name remain `Tres-critical-hit-ledger`.

```text
Tres-critical-hit-ledger/
├── pom.xml
├── README.md
├── transactions.csv
├── docs/
│   └── ASSET_PROMPTS.md
└── src/main/
    ├── java/org/example/
    │   ├── Main.java
    │   ├── ConsoleHelper.java
    │   ├── DesktopLauncher.java
    │   ├── LedgerDesktopApp.java
    │   ├── TransactionDialog.java
    │   ├── Transaction.java
    │   ├── LedgerService.java
    │   └── FileManager.java
    └── resources/org/example/desktop/
        ├── potara.css
        └── images/
            ├── keyboard.png
            ├── mouse.png
            └── headset.png
```

| Class | Responsibility |
|---|---|
| `Main` | Runs menus, collects input, and connects user actions to the ledger. |
| `ConsoleHelper` | Displays colored headings, prompts, messages, and formatted tables. |
| `DesktopLauncher` | Starts the optional JavaFX desktop application. |
| `LedgerDesktopApp` | Builds the dashboard, table, filters, navigation, and transaction-detail view. |
| `TransactionDialog` | Collects deposit/payment details, validates input, requests confirmation, and calls the existing save operation. |
| `Transaction` | Represents one deposit or payment, validates its fields, and formats its CSV record. |
| `LedgerService` | Sorts and filters transactions, calculates totals, and coordinates saving. |
| `FileManager` | Loads and validates the CSV file and writes updated transaction history. |

There are **eight Java classes**. `Main` and `ConsoleHelper` handle the console; `DesktopLauncher`, `LedgerDesktopApp`, and `TransactionDialog` handle the desktop interface. Both front ends use `Transaction`, `LedgerService`, and `FileManager` for the same rules and saved data. In simple terms, the front end is what the user sees and clicks or types into; the back end checks, searches, totals, and saves the records.

Desktop colors and layout are defined in `potara.css`. The three PNG illustrations are packaged resources. Their prompts and provenance are documented in [Asset prompts](docs/ASSET_PROMPTS.md).

## Design Choices

**Exact money calculations:** `BigDecimal` avoids binary floating-point rounding problems when adding and subtracting money.

**Clear feedback:** a preview and confirmation precede each save. The application reports success only after the file save completes. Invalid input prompts the user to try again.

**Safer file updates:** saves write a temporary file and then request an atomic replacement. The in-memory ledger updates only after saving succeeds. Malformed files stop loading with an error instead of silently discarding records; detected outside edits prevent a save until the ledger is reloaded with desktop **Refresh** or the application is restarted.

**Two interfaces, shared rules:** the desktop app adds a visual front end while retaining the console entry point and existing ledger logic. Both interfaces work with the same CSV file rather than maintaining separate copies of a transaction history.

**Potara-inspired color:** the desktop uses a dark green workspace, gold highlights, green deposits, muted red payments, and purple filter controls. The console uses gold borders, green headings, and purple accents. Labels and signs also identify transaction types. Console ANSI colors are applied after ASCII Table calculates the layout; desktop styling lives in CSS. Neither interface writes display formatting to the CSV file.

## Optional Settings

Add these as **VM options** in IntelliJ's run configuration:

| Option | Effect |
|---|---|
| `-Dledger.color=false` | Turns off ANSI colors for plain console output. |
| `-Dledger.file=path/to/transactions.csv` | Selects a different ledger file. Relative paths use the working directory. |

For the desktop Maven launch, pass the data-file property directly:

```shell
mvn javafx:run -Dledger.file=demo-check.csv
```

The JavaFX plugin uses the project root as its working directory. `ledger.color` applies only to the console; the desktop interface uses its CSS stylesheet.

## Testing the Application

Use a separate data file, such as `-Dledger.file=demo-check.csv`, to try this walkthrough without changing the included sample history:

1. Record a deposit of **$25.50** and confirm the save.
2. Record a payment of **$5.25** and confirm the save.
3. View all transactions. Expect deposits of **$25.50**, payments of **$5.25**, and net change of **$20.25** in a fresh ledger.
4. Check deposit/payment filters, vendor search, and a custom search.
5. Try invalid input and cancel an entry; neither should create a transaction.
6. Close and reopen the application with the same data-file setting. Both saved records should remain.

Development checks for the console and shared ledger classes also covered calendar boundaries, newest-first sorting, combined search filters, malformed files, failed saves, external file edits, and table formatting. These checks are separate from the Maven build; `mvn package` alone is not a full behavioral test or proof that every desktop interaction works.

The JavaFX implementation was compiled and launched on Amazon Corretto 17. Separate development checks covered 37 transaction-form validation cases and 63 controller/filter cases, including date-entry handling, combined filters, calendar reports, toggle selection, refresh behavior, and outside-edit detection. Those checks used copied data files; the project ledger was preserved. The dashboard and Potara Customs view were also inspected in the native desktop window.

For a desktop walkthrough, start it with the separate data file above, add the same deposit/payment pair, inspect the review dialogs, combine filters, open an entry's details, and restart to check persistence. Try the hardware cards against the supplied fictional sample history, where relevant descriptions are present.

## Current Scope

This is a single-user application backed by a local file. Run one instance per ledger; outside-edit detection is not a multi-user database lock. Saving requires filesystem support for atomic replacement.

The displayed net change is deposits minus payments in the selected records. It is not a bank account balance. Bank connections, transaction editing/deletion, authentication, and automatic backups are outside the current feature set.

## Credits

(This Readme was 100% written by AI)

Created by **Tre Peterson** for the Java Focus Academy Accounting Ledger capstone.

Tables use [ASCII Table by Sven van der Meer](https://github.com/vdmeer/asciitable), version 0.3.2, distributed under the Apache License 2.0. The presentation takes inspiration from Dragon Ball's Potara theme. Potara Fusion Gear and its connection with Potara Customs provide the business theme for the fictional demonstration transactions.

The desktop interface uses [OpenJFX / JavaFX](https://openjfx.io/openjfx-docs/) and its Maven runtime configuration.

The keyboard, mouse, and headset illustrations were created with the built-in OpenAI image-generation tool. They are illustrative hardware artwork, not photographs of actual products or a real product catalog. See [Asset prompts](docs/ASSET_PROMPTS.md) for the exact prompts and links to the included images.
