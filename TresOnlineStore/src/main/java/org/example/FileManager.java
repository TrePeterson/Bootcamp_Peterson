package org.example;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class FileManager {
    private static final DateTimeFormatter RECEIPT_FILE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private FileManager() {
        // All file operations are available through the static methods below.
    }

    public static ArrayList<Product> loadProducts() throws IOException {
        String customFile = System.getProperty("store.products");

        if (customFile != null) {
            Path productFile = readPath(customFile, "store.products");
            if (!productFile.isAbsolute()) {
                throw new IOException("store.products must name an absolute path to products.csv.");
            }

            try (BufferedReader reader = Files.newBufferedReader(productFile, StandardCharsets.UTF_8)) {
                return readProducts(reader);
            }
        }

        InputStream input = FileManager.class.getResourceAsStream("/products.csv");
        if (input == null) {
            throw new IOException("The bundled products.csv file could not be found.");
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8))) {
            return readProducts(reader);
        }
    }

    private static ArrayList<Product> readProducts(BufferedReader reader) throws IOException {
        ArrayList<Product> products = new ArrayList<>();
        Set<String> usedSkus = new HashSet<>();
        boolean headerFound = false;
        int lineNumber = 0;
        String line;

        while ((line = reader.readLine()) != null) {
            lineNumber++;
            // Some UTF-8 editors put a byte-order mark at the start of the file.
            if (lineNumber == 1 && line.startsWith("\uFEFF")) {
                line = line.substring(1);
            }
            if (line.isBlank()) {
                continue;
            }

            String[] fields = line.split("\\|", -1);
            if (!headerFound) {
                if (!isHeader(fields)) {
                    throw invalidLine(lineNumber,
                            "expected the header SKU|Product Name|Price|Department.");
                }
                headerFound = true;
                continue;
            }

            if (fields.length != 4) {
                throw invalidLine(lineNumber, "expected exactly four pipe-separated fields.");
            }
            for (int index = 0; index < fields.length; index++) {
                fields[index] = fields[index].trim();
                if (fields[index].isEmpty()) {
                    throw invalidLine(lineNumber, "SKU, name, price, and department are required.");
                }
            }

            String sku = fields[0];
            if (!usedSkus.add(sku.toLowerCase(Locale.ROOT))) {
                throw invalidLine(lineNumber, "duplicate SKU: " + sku + ".");
            }

            try {
                BigDecimal price = new BigDecimal(fields[2]);
                if (price.signum() < 0) {
                    throw invalidLine(lineNumber, "price cannot be negative.");
                }
                // This permits 12.500 but rejects an actual fraction of a cent, such as 12.501.
                price = price.setScale(2, RoundingMode.UNNECESSARY);
                products.add(new Product(sku, fields[1], price, fields[3]));
            } catch (NumberFormatException exception) {
                throw new IOException("Invalid products.csv line " + lineNumber
                        + ": price must be a finite decimal number.", exception);
            } catch (ArithmeticException exception) {
                throw new IOException("Invalid products.csv line " + lineNumber
                        + ": price cannot contain fractions of a cent.", exception);
            } catch (IllegalArgumentException exception) {
                throw new IOException("Invalid products.csv line " + lineNumber
                        + ": " + exception.getMessage(), exception);
            }
        }

        if (!headerFound) {
            throw new IOException("products.csv is empty or is missing its header.");
        }
        return products;
    }

    private static boolean isHeader(String[] fields) {
        return fields.length == 4
                && fields[0].trim().equalsIgnoreCase("SKU")
                && fields[1].trim().equalsIgnoreCase("Product Name")
                && fields[2].trim().equalsIgnoreCase("Price")
                && fields[3].trim().equalsIgnoreCase("Department");
    }

    private static IOException invalidLine(int lineNumber, String problem) {
        return new IOException("Invalid products.csv line " + lineNumber + ": " + problem);
    }

    public static Path saveReceipt(LocalDateTime saleTime, String receiptText) throws IOException {
        if (saleTime == null || receiptText == null) {
            throw new IllegalArgumentException("A sale time and receipt text are required.");
        }

        Path receiptDirectory = readPath(System.getProperty("store.receipts", "Receipts"),
                "store.receipts");
        Files.createDirectories(receiptDirectory);
        Path receiptFile = receiptDirectory.resolve(RECEIPT_FILE_FORMAT.format(saleTime) + ".txt");
        boolean created = false;

        try (BufferedWriter writer = Files.newBufferedWriter(receiptFile, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            created = true;
            writer.write(receiptText);
        } catch (IOException exception) {
            if (!created && exception instanceof FileAlreadyExistsException) {
                throw new IOException("A receipt already exists for this second. "
                        + "Please retry checkout in a moment. Existing receipt: " + receiptFile,
                        exception);
            }

            // Remove a partial receipt only when this call created it.
            if (created) {
                try {
                    Files.deleteIfExists(receiptFile);
                } catch (IOException cleanupException) {
                    exception.addSuppressed(cleanupException);
                }
            }
            throw exception;
        }

        return receiptFile;
    }

    private static Path readPath(String value, String setting) throws IOException {
        if (value.isBlank()) {
            throw new IOException(setting + " must not be blank.");
        }
        try {
            return Path.of(value);
        } catch (InvalidPathException exception) {
            throw new IOException(setting + " contains an invalid file path.", exception);
        }
    }
}
