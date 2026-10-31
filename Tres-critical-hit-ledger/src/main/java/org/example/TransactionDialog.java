package org.example;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;
import java.util.Objects;

/** Collects and confirms a transaction; the caller supplies the existing save operation. */
public final class TransactionDialog {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter
            .ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter
            .ofPattern("HH:mm:ss").withResolverStyle(ResolverStyle.STRICT);

    private TransactionDialog() {
    }

    @FunctionalInterface
    public interface TransactionSaver {
        void save(Transaction transaction) throws IOException;
    }

    public static boolean show(Window owner, boolean deposit, TransactionSaver saver) {
        Objects.requireNonNull(saver, "A transaction saver is required.");
        String transactionType = deposit ? "deposit" : "payment";
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle(deposit ? "Add deposit" : "Make payment");
        dialog.setResizable(true);
        if (owner != null) {
            dialog.initOwner(owner);
            dialog.initModality(Modality.WINDOW_MODAL);
        } else {
            dialog.initModality(Modality.APPLICATION_MODAL);
        }

        LocalDateTime now = LocalDateTime.now().withNano(0);
        DatePicker date = createDatePicker(now.toLocalDate());
        TextField time = new TextField(now.toLocalTime().format(TIME_FORMAT));
        time.setPromptText("HH:mm:ss");
        time.setAccessibleHelp("Use a 24-hour time with seconds, such as 14:30:00.");
        TextField description = createTextField("What was this transaction for?");
        TextField vendor = createTextField("Business or customer name");
        TextField amount = new TextField();
        amount.setPromptText("25.50");
        amount.setAccessibleHelp("Enter a positive dollar amount with up to two decimal places.");

        GridPane form = new GridPane();
        form.setHgap(16);
        form.setVgap(14);
        ColumnConstraints labelColumn = new ColumnConstraints();
        labelColumn.setMinWidth(125);
        ColumnConstraints fieldColumn = new ColumnConstraints();
        fieldColumn.setHgrow(Priority.ALWAYS);
        fieldColumn.setFillWidth(true);
        form.getColumnConstraints().addAll(labelColumn, fieldColumn);
        addField(form, 0, "Date", date);
        addField(form, 1, "Time", time);
        addField(form, 2, "Description", description);
        addField(form, 3, "Vendor / customer", vendor);
        addField(form, 4, "Amount ($)", amount);

        Label title = new Label(deposit ? "Record money coming in" : "Record money going out");
        title.getStyleClass().add("dialog-title");
        Label instructions = new Label(deposit
                ? "Enter a positive amount received. Review the details before saving."
                : "Enter a positive amount paid. The ledger will record it as a negative payment.");
        instructions.getStyleClass().add("muted");
        instructions.setWrapText(true);
        Label fieldHint = new Label("Dates use YYYY-MM-DD. Description and vendor allow up to "
                + Transaction.MAX_TEXT_LENGTH + " characters each.");
        fieldHint.getStyleClass().add("muted");
        fieldHint.setWrapText(true);
        Label error = new Label();
        error.getStyleClass().add("error-label");
        error.setWrapText(true);
        error.setVisible(false);
        error.setManaged(false);

        VBox content = new VBox(16, title, instructions, form, fieldHint, error);
        content.setPadding(new Insets(12));
        content.setPrefWidth(530);
        DialogPane pane = dialog.getDialogPane();
        pane.setContent(content);
        styleDialog(pane);

        ButtonType reviewType = new ButtonType("Review & save", ButtonBar.ButtonData.OK_DONE);
        pane.getButtonTypes().addAll(reviewType, ButtonType.CANCEL);
        Button reviewButton = (Button) pane.lookupButton(reviewType);
        reviewButton.getStyleClass().add("primary-button");
        reviewButton.setDefaultButton(true);
        Button cancelButton = (Button) pane.lookupButton(ButtonType.CANCEL);
        cancelButton.getStyleClass().add("secondary-button");
        cancelButton.setCancelButton(true);

        // Consuming a failed or canceled action keeps the dialog and entered values open.
        reviewButton.addEventFilter(ActionEvent.ACTION, event -> {
            error.setVisible(false);
            error.setManaged(false);
            Transaction transaction;
            try {
                transaction = readTransaction(date, time, description, vendor, amount, deposit);
            } catch (IllegalArgumentException exception) {
                showError(error, exception.getMessage());
                event.consume();
                return;
            }

            Window dialogWindow = pane.getScene().getWindow();
            if (!confirm(dialogWindow, transaction, transactionType)) {
                event.consume();
                return;
            }

            try {
                saver.save(transaction);
            } catch (IOException | RuntimeException exception) {
                String detail = exception.getMessage();
                showError(error, "Could not save this " + transactionType + ". "
                        + (detail == null || detail.isBlank() ? "Please check the ledger file and try again." : detail));
                event.consume();
            }
        });

        // The review action reaches this converter only after saving successfully.
        dialog.setResultConverter(button -> button == reviewType);
        dialog.setOnShown(event -> Platform.runLater(description::requestFocus));
        return dialog.showAndWait().orElse(false);
    }

    private static DatePicker createDatePicker(LocalDate initialDate) {
        DatePicker picker = new DatePicker(initialDate);
        picker.setPromptText("YYYY-MM-DD");
        picker.setMaxWidth(Double.MAX_VALUE);
        picker.setAccessibleHelp("Choose a date or enter it in YYYY-MM-DD format.");
        picker.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate value) {
                return value == null ? "" : value.format(DATE_FORMAT);
            }

            @Override
            public LocalDate fromString(String value) {
                try {
                    return parseDate(value);
                } catch (IllegalArgumentException exception) {
                    // Let the form show an inline error instead of throwing on focus changes.
                    return null;
                }
            }
        });
        picker.getEditor().setText(initialDate.format(DATE_FORMAT));
        return picker;
    }

    private static TextField createTextField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setTextFormatter(new TextFormatter<String>(change ->
                change.getControlNewText().length() <= Transaction.MAX_TEXT_LENGTH ? change : null));
        field.setAccessibleHelp("Required. Maximum " + Transaction.MAX_TEXT_LENGTH
                + " characters. Pipes and control characters are not allowed.");
        return field;
    }

    private static void addField(GridPane form, int row, String text, Node field) {
        Label label = new Label(text);
        label.getStyleClass().add("form-label");
        label.setLabelFor(field);
        form.add(label, 0, row);
        form.add(field, 1, row);
        GridPane.setHgrow(field, Priority.ALWAYS);
    }

    private static Transaction readTransaction(DatePicker date, TextField time,
                                               TextField description, TextField vendor,
                                               TextField amount, boolean deposit) {
        // Read the editor text so an uncommitted date edit cannot silently use an old date.
        LocalDate selectedDate = parseDate(date.getEditor().getText());
        String timeText = time.getText().trim();
        LocalTime selectedTime;
        try {
            if (!timeText.matches("\\d{2}:\\d{2}:\\d{2}")) {
                throw new DateTimeParseException("Invalid time format", timeText, 0);
            }
            selectedTime = LocalTime.parse(timeText, TIME_FORMAT);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Use a real time in HH:mm:ss format, such as 14:30:00.");
        }

        String amountText = amount.getText().trim();
        if (amountText.startsWith("$")) {
            amountText = amountText.substring(1).trim();
        }
        if (amountText.length() > 20 || !amountText.matches("\\d+(\\.\\d{1,2})?")) {
            throw new IllegalArgumentException("Enter a positive amount with up to two decimal places, such as 25.50.");
        }
        BigDecimal enteredAmount = new BigDecimal(amountText);
        if (enteredAmount.signum() <= 0 || enteredAmount.compareTo(Transaction.MAX_AMOUNT) > 0) {
            throw new IllegalArgumentException("Amount must be greater than $0.00 and no more than $999,999,999.99.");
        }
        BigDecimal signedAmount = deposit ? enteredAmount : enteredAmount.negate();
        return new Transaction(selectedDate, selectedTime, description.getText(), vendor.getText(), signedAmount);
    }

    private static LocalDate parseDate(String value) {
        String text = value == null ? "" : value.trim();
        try {
            if (!text.matches("\\d{4}-\\d{2}-\\d{2}")) {
                throw new DateTimeParseException("Invalid date format", text, 0);
            }
            LocalDate date = LocalDate.parse(text, DATE_FORMAT);
            if (date.getYear() < 1) {
                throw new DateTimeParseException("Year must be positive", text, 0);
            }
            return date;
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Choose a real date from 0001 through 9999 using YYYY-MM-DD.");
        }
    }

    private static boolean confirm(Window owner, Transaction transaction, String transactionType) {
        ButtonType saveType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        ButtonType backType = new ButtonType("Back", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION, "", saveType, backType);
        confirmation.initOwner(owner);
        confirmation.initModality(Modality.WINDOW_MODAL);
        confirmation.setTitle("Confirm " + transactionType);
        confirmation.setHeaderText("Ready to save this " + transactionType + "?");
        confirmation.setGraphic(null);
        String amount = String.format(Locale.US, "%s$%,.2f",
                transaction.isPayment() ? "-" : "", transaction.getAmount().abs());
        Label summary = new Label("Date: " + transaction.getDate()
                + "\nTime: " + transaction.getTime().format(TIME_FORMAT)
                + "\nDescription: " + transaction.getDescription()
                + "\nVendor / customer: " + transaction.getVendor()
                + "\nAmount: " + amount);
        summary.setWrapText(true);
        summary.setMaxWidth(500);
        summary.getStyleClass().add("form-label");
        DialogPane pane = confirmation.getDialogPane();
        pane.setContent(summary);
        pane.setPrefWidth(540);
        styleDialog(pane);
        pane.lookupButton(saveType).getStyleClass().add("primary-button");
        pane.lookupButton(backType).getStyleClass().add("secondary-button");
        return confirmation.showAndWait().filter(saveType::equals).isPresent();
    }

    private static void showError(Label label, String message) {
        label.setText(message);
        label.setAccessibleText(message);
        label.setManaged(true);
        label.setVisible(true);
    }

    private static void styleDialog(DialogPane pane) {
        pane.getStyleClass().add("transaction-dialog");
        pane.setStyle("-fx-background-color: #14151b; -fx-base: #14151b; "
                + "-fx-control-inner-background: #20222d; -fx-text-base-color: #f4f4f5; "
                + "-fx-text-background-color: #f4f4f5;");
        URL stylesheet = TransactionDialog.class.getResource("/org/example/desktop/potara.css");
        if (stylesheet != null) {
            pane.getStylesheets().add(stylesheet.toExternalForm());
        }
    }
}
