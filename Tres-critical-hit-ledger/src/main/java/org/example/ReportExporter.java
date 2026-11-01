package org.example;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Exports a snapshot of the visible results without changing the ledger. */
public final class ReportExporter {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter GENERATED =
            DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss z", Locale.US);
    private static final String FONT_FOLDER = "/org/example/pdf/";

    private ReportExporter() {
    }

    public static void export(Path destination, List<Transaction> transactions,
                              String filterSummary, boolean demo) throws IOException {
        Objects.requireNonNull(destination, "A PDF destination is required.");
        Objects.requireNonNull(transactions, "Transactions are required.");

        Path target = destination.toAbsolutePath().normalize();
        if (!target.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new IOException("Choose a filename ending in .pdf.");
        }
        if (!Files.isDirectory(target.getParent()) || Files.isSymbolicLink(target)) {
            throw new IOException("Choose a PDF file in an existing folder, not a symbolic link.");
        }

        List<Transaction> snapshot = new ArrayList<>(List.copyOf(transactions));
        snapshot.sort(Comparator.comparing(Transaction::getDate)
                .thenComparing(Transaction::getTime).reversed());
        String filters = filterSummary == null || filterSummary.isBlank()
                ? "All transactions (no filters)" : filterSummary.strip();

        // Build beside the destination, then replace it only after the PDF is complete.
        Path temporary = Files.createTempFile(target.getParent(), ".potara-report-", ".pdf");
        try {
            try (PDDocument document = new PDDocument()) {
                PDDocumentInformation information = document.getDocumentInformation();
                information.setTitle("Potara Fusion Ledger - " + (demo ? "Demo " : "") + "Transaction Report");
                information.setAuthor("Potara Fusion Gear");
                information.setSubject("A snapshot of the selected ledger transactions");
                information.setCreator("Potara Fusion Ledger");

                try (Layout layout = new Layout(document, demo)) {
                    layout.writeReport(snapshot, filters);
                    layout.writeFooters();
                }
                document.save(temporary.toFile());
            }
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ex) {
                throw new IOException("This folder does not support safe PDF replacement. "
                        + "Choose a local folder and try again.", ex);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    /** Handles page space in points; transaction rows stay together where possible. */
    private static final class Layout implements AutoCloseable {
        private static final float PAGE_WIDTH = 792;
        private static final float PAGE_HEIGHT = 612;
        private static final float MARGIN = 42;
        private static final float WIDTH = PAGE_WIDTH - MARGIN * 2;
        private static final float BOTTOM = 65;
        private static final float FONT_SIZE = 9;
        private static final float LEADING = 13;
        private static final float PADDING = 9;
        private static final float[] COLUMNS = {79, 61, 243, 205, 120};
        private static final Color INK = new Color(28, 42, 38);
        private static final Color MUTED = new Color(87, 100, 95);
        private static final Color GREEN = new Color(28, 105, 69);
        private static final Color RED = new Color(168, 49, 59);
        private static final Color PURPLE = new Color(104, 67, 143);
        private static final Color GOLD = new Color(178, 139, 50);
        private static final Color PALE = new Color(245, 248, 245);
        private static final Color RULE = new Color(218, 226, 219);

        private final PDDocument document;
        private final PDFont regular;
        private final PDFont bold;
        private final boolean demo;
        private final String generated = ZonedDateTime.now().format(GENERATED);
        private PDPageContentStream stream;
        private float y;
        private boolean escapedCharacters;

        private Layout(PDDocument document, boolean demo) throws IOException {
            this.document = document;
            this.demo = demo;
            regular = loadFont(document, "NotoSans-Regular.ttf");
            bold = loadFont(document, "NotoSans-Bold.ttf");
        }

        private void writeReport(List<Transaction> rows, String filters) throws IOException {
            newPage(true);
            BigDecimal deposits = new BigDecimal("0.00");
            BigDecimal payments = new BigDecimal("0.00");
            LocalDate earliest = null;
            LocalDate latest = null;
            for (Transaction row : rows) {
                if (row.isDeposit()) {
                    deposits = deposits.add(row.getAmount());
                } else {
                    payments = payments.add(row.getAmount().abs());
                }
                if (earliest == null || row.getDate().isBefore(earliest)) earliest = row.getDate();
                if (latest == null || row.getDate().isAfter(latest)) latest = row.getDate();
            }
            summaryCard("DEPOSITS", deposits, GREEN, MARGIN);
            summaryCard("PAYMENTS", payments, RED, MARGIN + 240);
            summaryCard("NET CHANGE", deposits.subtract(payments), PURPLE, MARGIN + 480);
            y -= 82;

            paragraph("ACTIVE FILTERS", bold, 9, GREEN, 13);
            paragraph(filters, regular, 9, INK, 13);
            y -= 9;
            String range = rows.isEmpty() ? "No entries in this snapshot"
                    : earliest + " to " + latest;
            paragraph("Entry date range: " + range + "    |    Transactions: " + rows.size(),
                    regular, 9, MUTED, 13);
            paragraph("Totals describe these results only. Payments are shown as an expense total.",
                    regular, 8, MUTED, 12);
            y -= 15;
            tableHeader();

            if (rows.isEmpty()) {
                fill(MARGIN, y - 64, WIDTH, 64, PALE);
                text("No transactions match these filters.", bold, 12, INK, MARGIN + 16, y - 27);
                text("Change your filters in the app and export again to include other entries.",
                        regular, 9, MUTED, MARGIN + 16, y - 46);
                y -= 64;
            } else {
                for (int index = 0; index < rows.size(); index++) {
                    transactionRow(rows.get(index), index % 2 == 1);
                }
            }
        }

        private void newPage(boolean first) throws IOException {
            close();
            PDPage page = new PDPage(new PDRectangle(PAGE_WIDTH, PAGE_HEIGHT));
            document.addPage(page);
            stream = new PDPageContentStream(document, page);
            y = PAGE_HEIGHT - MARGIN;
            text("POTARA FUSION LEDGER", bold, first ? 21 : 15, GREEN, MARGIN, y);
            text("Potara Fusion Gear  /  Transaction report", regular, 9, MUTED, MARGIN, y - 19);
            if (demo) {
                fill(PAGE_WIDTH - MARGIN - 87, y - 20, 87, 28, new Color(244, 236, 251));
                text("DEMO DATA", bold, 10, PURPLE, PAGE_WIDTH - MARGIN - 75, y - 10);
            }
            text("Generated " + generated, regular, 8, MUTED, MARGIN, y - 40);
            line(MARGIN, y - 53, PAGE_WIDTH - MARGIN, y - 53, GOLD);
            y -= 69;
        }

        private void summaryCard(String title, BigDecimal amount, Color color, float x) throws IOException {
            fill(x, y - 62, 228, 62, PALE);
            fill(x, y - 62, 3, 62, color);
            text(title, bold, 9, color, x + 14, y - 18);
            String value = money(amount);
            float size = fitSize(bold, value, 19, 200);
            text(value, bold, size, INK, x + 14, y - 45);
        }

        private void paragraph(String value, PDFont font, float size,
                               Color color, float leading) throws IOException {
            for (String part : wrap(value, font, size, WIDTH)) {
                if (y - leading < BOTTOM) newPage(false);
                text(part, font, size, color, MARGIN, y);
                y -= leading;
            }
        }

        private void tableHeader() throws IOException {
            // Leave room for at least the first row, not just a stranded heading.
            if (y < BOTTOM + 92) newPage(false);
            fill(MARGIN, y - 27, WIDTH, 27, GREEN);
            String[] titles = {"DATE", "TIME", "DESCRIPTION", "VENDOR / CUSTOMER", "AMOUNT"};
            float x = MARGIN;
            for (int column = 0; column < COLUMNS.length; column++) {
                float labelX = column == 4
                        ? x + COLUMNS[column] - PADDING - width(bold, titles[column], 8)
                        : x + PADDING;
                text(titles[column], bold, 8, Color.WHITE, labelX, y - 17);
                x += COLUMNS[column];
            }
            y -= 27;
        }

        private void transactionRow(Transaction row, boolean shaded) throws IOException {
            String[] values = {row.getDate().toString(), row.getTime().format(TIME),
                    row.getDescription(), row.getVendor(), money(row.getAmount())};
            List<List<String>> cells = new ArrayList<>();
            int lineCount = 1;
            for (int column = 0; column < values.length; column++) {
                List<String> lines = wrap(values[column], regular, FONT_SIZE,
                        COLUMNS[column] - PADDING * 2);
                cells.add(lines);
                lineCount = Math.max(lineCount, lines.size());
            }

            float height = lineCount * LEADING + PADDING * 2;
            if (y - height < BOTTOM) {
                newPage(false);
                tableHeader();
            }
            // Extremely long escaped Unicode text can span pages without losing any content.
            int startLine = 0;
            while (startLine < lineCount) {
                int linesOnPage = Math.min(lineCount - startLine,
                        (int) ((y - BOTTOM - PADDING * 2) / LEADING));
                if (linesOnPage < 1) {
                    newPage(false);
                    tableHeader();
                    continue;
                }
                height = linesOnPage * LEADING + PADDING * 2;
                if (shaded) fill(MARGIN, y - height, WIDTH, height, PALE);
                float x = MARGIN;
                for (int column = 0; column < cells.size(); column++) {
                    List<String> lines = cells.get(column);
                    for (int offset = 0; offset < linesOnPage; offset++) {
                        int lineIndex = startLine + offset;
                        if (lineIndex >= lines.size()) break;
                        String value = lines.get(lineIndex);
                        float textX = column == 4
                                ? x + COLUMNS[column] - PADDING - width(regular, value, FONT_SIZE)
                                : x + PADDING;
                        Color color = column == 4 ? (row.isDeposit() ? GREEN : RED) : INK;
                        text(value, regular, FONT_SIZE, color, textX,
                                y - PADDING - FONT_SIZE - offset * LEADING);
                    }
                    x += COLUMNS[column];
                }
                y -= height;
                line(MARGIN, y, PAGE_WIDTH - MARGIN, y, RULE);
                startLine += linesOnPage;
                if (startLine < lineCount) {
                    newPage(false);
                    tableHeader();
                }
            }
        }

        private void writeFooters() throws IOException {
            close();
            for (int index = 0; index < document.getNumberOfPages(); index++) {
                stream = new PDPageContentStream(document, document.getPage(index),
                        PDPageContentStream.AppendMode.APPEND, true, true);
                line(MARGIN, 49, PAGE_WIDTH - MARGIN, 49, GOLD);
                String label = demo ? "DEMO - Fictional transactions" : "Snapshot of selected transactions";
                text(label, regular, 8, MUTED, MARGIN, 34);
                String page = "Page " + (index + 1) + " of " + document.getNumberOfPages();
                text(page, regular, 8, MUTED, PAGE_WIDTH - MARGIN - width(regular, page, 8), 34);
                if (escapedCharacters) {
                    text("Characters unavailable in the embedded font are preserved as [U+XXXX] codes.",
                            regular, 7, MUTED, MARGIN, 20);
                }
                close();
            }
        }

        private List<String> wrap(String raw, PDFont font, float size, float maximum) throws IOException {
            String safe = safeText(raw, font);
            List<String> lines = new ArrayList<>();
            StringBuilder current = new StringBuilder();
            for (String word : safe.split("\\s+")) {
                if (word.isEmpty()) continue;
                String candidate = current.isEmpty() ? word : current + " " + word;
                if (width(font, candidate, size) <= maximum) {
                    current.setLength(0);
                    current.append(candidate);
                    continue;
                }
                if (!current.isEmpty()) {
                    lines.add(current.toString());
                    current.setLength(0);
                }
                for (int offset = 0; offset < word.length(); ) {
                    int codePoint = word.codePointAt(offset);
                    String character = new String(Character.toChars(codePoint));
                    if (!current.isEmpty() && width(font, current + character, size) > maximum) {
                        lines.add(current.toString());
                        current.setLength(0);
                    }
                    current.append(character);
                    offset += Character.charCount(codePoint);
                }
            }
            if (!current.isEmpty() || lines.isEmpty()) lines.add(current.toString());
            return lines;
        }

        private String safeText(String value, PDFont font) throws IOException {
            StringBuilder safe = new StringBuilder();
            for (int offset = 0; offset < value.length(); ) {
                int codePoint = value.codePointAt(offset);
                String character = new String(Character.toChars(codePoint));
                if (Character.isISOControl(codePoint) || Character.isWhitespace(codePoint)) {
                    safe.append(' ');
                } else {
                    try {
                        font.encode(character);
                        safe.append(character);
                    } catch (IllegalArgumentException ex) {
                        // Never silently drop a name or let an unsupported glyph abort the report.
                        safe.append(String.format(Locale.ROOT, "[U+%04X]", codePoint));
                        escapedCharacters = true;
                    }
                }
                offset += Character.charCount(codePoint);
            }
            return safe.toString();
        }

        private void text(String value, PDFont font, float size, Color color, float x, float baseline)
                throws IOException {
            stream.beginText();
            stream.setFont(font, size);
            stream.setNonStrokingColor(color);
            stream.newLineAtOffset(x, baseline);
            stream.showText(value);
            stream.endText();
        }

        private void fill(float x, float bottom, float width, float height, Color color) throws IOException {
            stream.setNonStrokingColor(color);
            stream.addRect(x, bottom, width, height);
            stream.fill();
        }

        private void line(float x1, float y1, float x2, float y2, Color color) throws IOException {
            stream.setStrokingColor(color);
            stream.setLineWidth(0.6f);
            stream.moveTo(x1, y1);
            stream.lineTo(x2, y2);
            stream.stroke();
        }

        private static float width(PDFont font, String value, float size) throws IOException {
            return font.getStringWidth(value) * size / 1000;
        }

        private static float fitSize(PDFont font, String value, float desired, float maximum)
                throws IOException {
            return Math.min(desired, desired * maximum / Math.max(1, width(font, value, desired)));
        }

        @Override
        public void close() throws IOException {
            if (stream != null) {
                stream.close();
                stream = null;
            }
        }
    }

    private static PDFont loadFont(PDDocument document, String filename) throws IOException {
        try (InputStream input = ReportExporter.class.getResourceAsStream(FONT_FOLDER + filename)) {
            if (input == null) throw new IOException("The bundled PDF font is missing: " + filename);
            return PDType0Font.load(document, input);
        }
    }

    private static String money(BigDecimal value) {
        return NumberFormat.getCurrencyInstance(Locale.US).format(value);
    }
}
