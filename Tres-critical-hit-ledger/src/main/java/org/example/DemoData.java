package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Keeps presentation transactions in their own persistent, resettable ledger. */
public final class DemoData {
    private static final String HEADER = "date|time|description|vendor|amount";
    private static final String SEED_RESOURCE = "/org/example/desktop/demo-transactions.csv";
    private static final LocalDate SEED_TODAY = LocalDate.of(2026, 10, 31);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter
            .ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter
            .ofPattern("HH:mm:ss").withResolverStyle(ResolverStyle.STRICT);

    private DemoData() {
    }

    public static Path getFilePath() {
        String override = System.getProperty("ledger.demo.file");
        if (override != null && !override.isBlank()) {
            return Path.of(override).toAbsolutePath().normalize();
        }

        String localAppData = System.getenv("LOCALAPPDATA");
        Path folder = localAppData != null && !localAppData.isBlank()
                ? Path.of(localAppData, "PotaraFusionLedger", "demo")
                : Path.of(System.getProperty("user.home"), ".potara-fusion-ledger", "demo");
        return folder.resolve("transactions.csv").toAbsolutePath().normalize();
    }

    /** Creates the demo once. Opening it again preserves the user's demo edits. */
    public static FileManager open() throws IOException {
        Path demoPath = getFilePath();
        validateSeparateFrom(new FileManager().getFilePath());

        if (!Files.exists(demoPath, LinkOption.NOFOLLOW_LINKS)) {
            List<Transaction> seed = loadSeed(LocalDate.now());
            Files.createDirectories(demoPath.getParent());
            validateSeparateFrom(new FileManager().getFilePath());
            try {
                // CREATE_NEW cannot replace a demo created by another app instance.
                Files.writeString(demoPath, toCsv(seed), StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            } catch (FileAlreadyExistsException exception) {
                // Load that existing demo below rather than reseeding it.
            }
        }

        FileManager demoFiles = new FileManager(demoPath);
        demoFiles.loadTransactions();
        return demoFiles;
    }

    /** Called only after the UI confirms that the user wants to reset demo data. */
    public static void reset(FileManager demoFiles) throws IOException {
        if (demoFiles == null || !demoFiles.getFilePath().equals(getFilePath())) {
            throw new IOException("Reset is available only for the configured demo ledger. "
                    + "The normal ledger has not been changed.");
        }
        validateSeparateFrom(new FileManager().getFilePath());
        List<Transaction> seed = loadSeed(LocalDate.now());
        // Keep FileManager's atomic replacement and outside-edit detection intact.
        demoFiles.saveTransactions(seed);
    }

    /** Lets the UI verify its normal ledger before switching into demo mode. */
    public static void validateSeparateFrom(Path normalFile) throws IOException {
        if (normalFile == null) {
            throw new IOException("The normal ledger path is required before opening the demo.");
        }

        Path demoPath = getFilePath();
        Path normalPath = normalFile.toAbsolutePath().normalize();
        boolean sameFile = demoPath.equals(normalPath)
                || (Files.exists(demoPath) && Files.exists(normalPath)
                && Files.isSameFile(demoPath, normalPath))
                || resolveExistingParents(demoPath).equals(resolveExistingParents(normalPath));

        if (sameFile) {
            throw new IOException("The demo ledger and normal ledger must use different files. "
                    + "Choose a separate ledger.demo.file location. No transactions were changed.");
        }
    }

    private static Path resolveExistingParents(Path path) throws IOException {
        Path parent = path.toAbsolutePath().normalize();
        Deque<Path> missingParts = new ArrayDeque<>();
        while (parent != null && !Files.exists(parent, LinkOption.NOFOLLOW_LINKS)) {
            missingParts.addFirst(parent.getFileName());
            parent = parent.getParent();
        }
        if (parent == null) {
            throw new IOException("Cannot resolve the ledger folder: " + path);
        }

        // toRealPath follows existing symlinks, including linked parent folders.
        Path resolved = parent.toRealPath();
        for (Path part : missingParts) {
            resolved = resolved.resolve(part);
        }
        return resolved.normalize();
    }

    private static List<Transaction> loadSeed(LocalDate today) throws IOException {
        InputStream stream = DemoData.class.getResourceAsStream(SEED_RESOURCE);
        if (stream == null) {
            throw new IOException("The packaged demo transactions are missing. "
                    + "Rebuild the application before opening or resetting the demo.");
        }

        List<Transaction> transactions = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream,
                StandardCharsets.UTF_8.newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT)))) {
            if (!HEADER.equals(reader.readLine())) {
                throw new IOException("The packaged demo transactions have an invalid header.");
            }

            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }
                String[] fields = line.split("\\|", -1);
                try {
                    if (fields.length != 5 || !fields[0].matches("\\d{4}-\\d{2}-\\d{2}")
                            || !fields[1].matches("\\d{2}:\\d{2}:\\d{2}")) {
                        throw new IllegalArgumentException("Expected five valid transaction fields.");
                    }
                    LocalDate seedDate = LocalDate.parse(fields[0], DATE_FORMAT);
                    transactions.add(new Transaction(rebaseDate(seedDate, today),
                            LocalTime.parse(fields[1], TIME_FORMAT), fields[2], fields[3],
                            new BigDecimal(fields[4])));
                } catch (DateTimeException | IllegalArgumentException exception) {
                    throw new IOException("Invalid packaged demo transaction on line "
                            + lineNumber + ": " + exception.getMessage(), exception);
                }
            }
        }

        if (transactions.isEmpty()) {
            throw new IOException("The packaged demo must contain transactions.");
        }
        return transactions;
    }

    private static LocalDate rebaseDate(LocalDate seedDate, LocalDate today) {
        YearMonth seedMonth = YearMonth.from(seedDate);
        YearMonth referenceMonth = YearMonth.from(SEED_TODAY);
        if (seedMonth.equals(referenceMonth)) {
            // Month-to-date examples never land after today's day of the month.
            return today.withDayOfMonth(Math.min(seedDate.getDayOfMonth(), today.getDayOfMonth()));
        }
        if (seedMonth.equals(referenceMonth.minusMonths(1))) {
            return dayInMonth(YearMonth.from(today).minusMonths(1), seedDate.getDayOfMonth());
        }
        if (seedDate.getYear() == SEED_TODAY.getYear() - 1) {
            return dayInMonth(YearMonth.of(today.getYear() - 1, seedDate.getMonth()),
                    seedDate.getDayOfMonth());
        }
        if (seedDate.getYear() == SEED_TODAY.getYear() && seedDate.isBefore(SEED_TODAY)) {
            LocalDate rebased = dayInMonth(YearMonth.of(today.getYear(), seedDate.getMonth()),
                    seedDate.getDayOfMonth());
            return rebased.isAfter(today) ? today : rebased;
        }
        throw new IllegalArgumentException("The demo date is outside its supported reference period.");
    }

    private static LocalDate dayInMonth(YearMonth month, int day) {
        return month.atDay(Math.min(day, month.lengthOfMonth()));
    }

    private static String toCsv(List<Transaction> transactions) {
        StringBuilder csv = new StringBuilder(HEADER).append('\n');
        for (Transaction transaction : transactions) {
            csv.append(transaction.toCsvLine()).append('\n');
        }
        return csv.toString();
    }
}
