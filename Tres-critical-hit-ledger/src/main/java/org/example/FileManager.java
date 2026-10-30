package org.example;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** Reads the ledger and replaces its file only after a complete new copy is ready. */
public class FileManager {
    private static final String HEADER = "date|time|description|vendor|amount";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter
            .ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter
            .ofPattern("HH:mm:ss").withResolverStyle(ResolverStyle.STRICT);

    private final Path filePath;
    private byte[] lastLoadedBytes;
    private boolean loadedSuccessfully;

    public FileManager() {
        this(Path.of(System.getProperty("ledger.file", "transactions.csv")));
    }

    public FileManager(Path filePath) {
        this.filePath = Objects.requireNonNull(filePath, "File path is required.")
                .toAbsolutePath().normalize();
    }

    public Path getFilePath() {
        return filePath;
    }

    public List<Transaction> loadTransactions() throws IOException {
        loadedSuccessfully = false;
        lastLoadedBytes = null;
        createFileIfMissing();

        byte[] fileBytes = Files.readAllBytes(filePath);
        String contents = decodeUtf8(fileBytes);
        if (contents.startsWith("\uFEFF")) {
            contents = contents.substring(1);
        }

        List<Transaction> transactions = new ArrayList<>();
        String[] lines = contents.split("\\r\\n|\\n|\\r", -1);
        boolean foundHeader = false;

        for (int index = 0; index < lines.length; index++) {
            String line = lines[index];
            if (line.isBlank()) {
                continue;
            }

            if (!foundHeader) {
                if (!line.equals(HEADER)) {
                    throw new IOException("Invalid header on line " + (index + 1)
                            + ". Expected: " + HEADER);
                }
                foundHeader = true;
            } else {
                transactions.add(parseTransaction(line, index + 1));
            }
        }

        if (!foundHeader) {
            throw new IOException("The ledger file is empty or has no header. Expected: "
                    + HEADER);
        }

        // Remember the exact bytes, so a later save can detect outside edits.
        lastLoadedBytes = fileBytes;
        loadedSuccessfully = true;
        return transactions;
    }

    public void saveTransactions(List<Transaction> transactions) throws IOException {
        if (!loadedSuccessfully) {
            throw new IOException("Load the ledger successfully before saving. "
                    + "The existing file has not been changed.");
        }
        if (transactions == null) {
            throw new IOException("The transaction list is required.");
        }

        StringBuilder contents = new StringBuilder(HEADER).append('\n');
        for (int index = 0; index < transactions.size(); index++) {
            Transaction transaction = transactions.get(index);
            if (transaction == null) {
                throw new IOException("Transaction " + (index + 1) + " is missing.");
            }

            String line = transaction.toCsvLine();
            if (line == null || line.contains("\n") || line.contains("\r")) {
                throw new IOException("Transaction " + (index + 1)
                        + " must fit on one CSV line.");
            }
            // Validate the entire candidate before touching the saved ledger.
            parseTransaction(line, index + 2);
            contents.append(line).append('\n');
        }

        byte[] newBytes = contents.toString().getBytes(StandardCharsets.UTF_8);
        verifyFileUnchanged();
        Path temporaryFile = Files.createTempFile(filePath.getParent(),
                ".ledger-save-", ".tmp");
        IOException saveFailure = null;

        try {
            Files.write(temporaryFile, newBytes, StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
            verifyFileUnchanged();

            // Atomic replacement prevents a partially written ledger from becoming live.
            try {
                Files.move(temporaryFile, filePath, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                throw new IOException("This location does not support a safe file replacement. "
                        + "The ledger was not saved. Use a local folder and try again.", exception);
            }

            lastLoadedBytes = newBytes;
        } catch (IOException exception) {
            saveFailure = exception;
            throw exception;
        } finally {
            // This is the unique temporary file created by this save attempt.
            try {
                Files.deleteIfExists(temporaryFile);
            } catch (IOException cleanupFailure) {
                if (saveFailure != null) {
                    saveFailure.addSuppressed(cleanupFailure);
                }
            }
        }
    }

    private void createFileIfMissing() throws IOException {
        if (Files.exists(filePath)) {
            return;
        }

        Files.createDirectories(filePath.getParent());
        try {
            Files.writeString(filePath, HEADER + "\n", StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (FileAlreadyExistsException exception) {
            // Another process created the file. Load it rather than replacing it.
        }
    }

    private String decodeUtf8(byte[] bytes) throws IOException {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException exception) {
            throw new IOException("The ledger must be saved as a UTF-8 text file.", exception);
        }
    }

    private Transaction parseTransaction(String line, int lineNumber) throws IOException {
        String[] fields = line.split("\\|", -1);
        if (fields.length != 5) {
            throw new IOException("Invalid transaction on line " + lineNumber
                    + ": expected five fields separated by |.");
        }

        try {
            String dateText = fields[0].trim();
            String timeText = fields[1].trim();
            if (!dateText.matches("\\d{4}-\\d{2}-\\d{2}")
                    || !timeText.matches("\\d{2}:\\d{2}:\\d{2}")) {
                throw new IllegalArgumentException("Use yyyy-MM-dd and HH:mm:ss.");
            }

            LocalDate date = LocalDate.parse(dateText, DATE_FORMAT);
            LocalTime time = LocalTime.parse(timeText, TIME_FORMAT);
            BigDecimal amount = new BigDecimal(fields[4].trim());
            return new Transaction(date, time, fields[2], fields[3], amount);
        } catch (DateTimeException | IllegalArgumentException exception) {
            throw new IOException("Invalid transaction on line " + lineNumber
                    + ": " + exception.getMessage(), exception);
        }
    }

    private void verifyFileUnchanged() throws IOException {
        byte[] currentBytes;
        try {
            currentBytes = Files.readAllBytes(filePath);
        } catch (NoSuchFileException exception) {
            throw new IOException("The ledger file was removed after it was loaded. "
                    + "Nothing was saved. Restore the file and restart the app.", exception);
        }

        if (!Arrays.equals(lastLoadedBytes, currentBytes)) {
            throw new IOException("The ledger file changed outside this app. "
                    + "Nothing was saved. Restart the app to load the latest transactions.");
        }
    }
}
