package org.example;

import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;

import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** A JavaFX front end that uses the same ledger rules and CSV storage as Main. */
public final class LedgerDesktopApp extends Application {
    private static final String STYLE = "/org/example/desktop/potara.css";
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US);
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private Stage window;
    private FileManager files;
    private LedgerService ledger;
    private Path normalFile;
    private boolean demoMode;
    private final UiMotion motion = new UiMotion();
    private VBox workspace;
    private ScrollPane workspaceScroll;
    private final HBox modeBanner = new HBox(12);
    private final Button demoButton = button("Presentation mode", "secondary-button");
    private final CashFlowChart cashFlow = new CashFlowChart();
    private TitledPane chartPanel;
    private final Button exportButton = button("Export PDF report", "secondary-button");
    private final Button openReportButton = button("Open saved report ↗", "text-button");
    private Path lastExport;
    private String appliedFilterSummary = "All time | All entries";
    private final ObservableList<Transaction> visible = FXCollections.observableArrayList();
    private final TableView<Transaction> table = new TableView<>(visible);
    private final TextField search = new TextField();
    private final ComboBox<String> period = new ComboBox<>();
    private final ToggleGroup types = new ToggleGroup();
    private final ToggleButton allTypes = new ToggleButton("All entries");
    private final DatePicker startDate = datePicker();
    private final DatePicker endDate = datePicker();
    private final TextField description = new TextField();
    private final TextField vendor = new TextField();
    private final TextField amount = new TextField();
    private final CheckBox exactVendor = new CheckBox("Match full vendor name");
    private final Label pageTitle = label("Your money, in focus.", "page-title");
    private final Label pageSubtitle = label("Two sides of your money. One powerful ledger.", "muted");
    private final Label deposits = label("$0.00", "metric-value", "positive");
    private final Label payments = label("$0.00", "metric-value", "negative");
    private final Label netChange = label("$0.00", "metric-value", "gold-text");
    private final Label resultCount = label("", "muted");
    private final Label status = label("Ready", "status-text");
    private final Circle statusDot = new Circle(4, Color.web("#b9e88a"));
    private final Label fileStatus = label("", "muted");
    private final Label filterError = label("", "error-label");
    private final Label scope = label("All-time overview", "eyebrow");
    private final List<Button> navigation = new ArrayList<>();
    private final VBox gearSection = new VBox(12);
    private final VBox advanced = new VBox(12);
    private Button advancedButton;
    private boolean changingFilters;
    private String currentView = "overview";

    @Override
    public void start(Stage stage) {
        window = stage;
        // Windows chooses the best size for the title bar, taskbar, and Alt+Tab.
        for (int size : new int[]{16, 32, 48, 64, 128, 256}) {
            String icon = "/org/example/desktop/icons/potara-" + size + ".png";
            window.getIcons().add(new Image(getClass().getResource(icon).toExternalForm()));
        }
        try {
            files = new FileManager();
            normalFile = files.getFilePath();
            ledger = new LedgerService(files);
        } catch (IOException | IllegalArgumentException ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            Stage errorWindow = (Stage) alert.getDialogPane().getScene().getWindow();
            errorWindow.getIcons().setAll(window.getIcons());
            alert.setTitle("Potara Fusion Ledger");
            alert.setHeaderText("The ledger could not be opened");
            alert.setContentText(ex.getMessage() + "\n\nCheck transactions.csv before restarting.");
            alert.showAndWait();
            javafx.application.Platform.exit();
            return;
        }

        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-root");
        root.setLeft(buildSidebarScroll());
        buildModeBanner();
        workspace = new VBox(24, modeBanner, buildHeader(), buildMetrics(), buildLedgerPanel(), buildGearSection());
        workspace.getStyleClass().add("workspace");
        workspaceScroll = new ScrollPane(workspace);
        workspaceScroll.setFitToWidth(true);
        workspaceScroll.getStyleClass().add("workspace-scroll");
        root.setCenter(workspaceScroll);
        root.setBottom(buildStatusBar());

        Scene scene = new Scene(root, 1440, 960);
        scene.getStylesheets().add(getClass().getResource(STYLE).toExternalForm());
        installShortcuts(scene);
        window.setScene(scene);
        window.setTitle("Potara Fusion Ledger | Potara Fusion Gear");
        window.setMinWidth(1100);
        window.setMinHeight(760);
        updateModeAppearance();
        window.show();
        navigation.get(0).getStyleClass().add("nav-selected");
        applyFilters();
        setStatus("Ledger loaded. You're ready to go.", false);
        motion.reveal(workspace);
    }

    @Override
    public void stop() {
        motion.stop();
    }

    private ScrollPane buildSidebarScroll() {
        VBox sidebar = buildSidebar();
        sidebar.setMinWidth(0);
        sidebar.setMinHeight(Region.USE_PREF_SIZE);
        ScrollPane scroll = new ScrollPane(sidebar);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setMinWidth(228);
        scroll.setPrefWidth(228);
        scroll.setMaxWidth(228);
        scroll.getStyleClass().add("sidebar-scroll");
        return scroll;
    }

    private VBox buildSidebar() {
        HBox emblem = new HBox(6, earring(), earring());
        emblem.setAlignment(Pos.CENTER_LEFT);
        VBox brand = new VBox(7, emblem, label("POTARA", "brand-name"),
                label("FUSION LEDGER", "brand-subtitle"));
        brand.setPadding(new Insets(4, 6, 25, 6));

        Button overview = navButton("Overview", "overview");
        Button transactions = navButton("Transactions", "transactions");
        Button reports = navButton("Reports & search", "reports");
        Button partner = navButton("Potara Customs", "partner");
        VBox links = new VBox(7, label("WORKSPACE", "nav-caption"), overview, transactions, reports);
        VBox partnerLinks = new VBox(8, label("CONNECTED BUSINESS", "nav-caption"), partner);
        partnerLinks.setPadding(new Insets(24, 0, 0, 0));
        demoButton.setWrapText(true);
        demoButton.setMaxWidth(Double.MAX_VALUE);
        demoButton.setOnAction(event -> switchMode(!demoMode));
        VBox demoControls = new VBox(10, label("PRESENT & EXPLORE", "nav-caption"), demoButton);
        demoControls.setPadding(new Insets(24, 0, 0, 0));
        CheckBox reduceMotion = new CheckBox("Reduce motion");
        reduceMotion.setId("reduce-motion");
        reduceMotion.selectedProperty().bindBidirectional(motion.reducedMotionProperty());
        reduceMotion.setTooltip(new Tooltip("Turn off decorative movement and fades for this session"));
        reduceMotion.setOnAction(event -> setStatus(reduceMotion.isSelected()
                ? "Reduced motion enabled. Actions still work immediately."
                : "Motion enabled. Gentle transitions are back.", false));
        VBox preferences = new VBox(10, reduceMotion,
                label("Ctrl+F  Search\nAlt+1–4  Switch views", "shortcut-hint"));
        preferences.getStyleClass().add("sidebar-preferences");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        Label footerCopy = label("Complete the setup.\nKeep the books.", "sidebar-copy");
        VBox footer = new VBox(8, label("POTARA FUSION GEAR", "sidebar-small-title"), footerCopy,
                label("PC accessories + upgrades", "sidebar-copy"));
        footer.getStyleClass().add("sidebar-footer");
        VBox sidebar = new VBox(12, brand, links, partnerLinks, demoControls, spacer, preferences, footer);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(228);
        sidebar.setMinWidth(228);
        return sidebar;
    }

    private StackPane earring() {
        Circle rim = new Circle(13, Color.web("#15271c"));
        rim.setStroke(Color.web("#e6c76d"));
        rim.setStrokeWidth(2);
        Circle gem = new Circle(6, Color.web("#b9e88a"));
        return new StackPane(rim, gem);
    }

    private Button navButton(String text, String view) {
        Button button = button(text, "nav-button");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(event -> navigate(view, button));
        button.setTooltip(new Tooltip(text + "  •  Alt+" + (navigation.size() + 1)));
        navigation.add(button);
        return button;
    }

    private HBox buildHeader() {
        VBox heading = new VBox(7, label("POTARA FUSION GEAR  /  FINANCE", "eyebrow"), pageTitle, pageSubtitle);
        heading.setMinWidth(0);
        pageTitle.setWrapText(true);
        pageSubtitle.setWrapText(true);
        HBox.setHgrow(heading, Priority.ALWAYS);
        Button refresh = button("Refresh", "secondary-button");
        refresh.setTooltip(new Tooltip("Reload saved transactions from the CSV file"));
        refresh.setOnAction(event -> reload());
        Button payment = button("− Payment", "secondary-button");
        payment.setOnAction(event -> addTransaction(false));
        Button deposit = button("+ Deposit", "primary-button");
        deposit.setOnAction(event -> addTransaction(true));
        HBox actions = new HBox(8, refresh, payment, deposit);
        actions.setAlignment(Pos.CENTER_RIGHT);
        HBox header = new HBox(15, heading, actions);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("workspace-header");
        return header;
    }

    private void installShortcuts(Scene scene) {
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN), () -> {
            if (workspaceScroll != null) workspaceScroll.setVvalue(0);
            search.requestFocus();
            search.selectAll();
        });
        KeyCode[] keys = {KeyCode.DIGIT1, KeyCode.DIGIT2, KeyCode.DIGIT3, KeyCode.DIGIT4};
        for (int i = 0; i < keys.length; i++) {
            Button destination = navigation.get(i);
            scene.getAccelerators().put(new KeyCodeCombination(keys[i], KeyCombination.ALT_DOWN), () -> {
                destination.fire();
                destination.requestFocus();
            });
        }
    }

    private VBox buildMetrics() {
        HBox cards = new HBox(14,
                metricCard("MONEY IN", deposits, "Deposits in this view", "metric-in"),
                metricCard("MONEY OUT", payments, "Payments in this view", "metric-out"),
                metricCard("NET CHANGE", netChange, "Money in minus money out", "metric-net"));
        for (Node card : cards.getChildren()) {
            HBox.setHgrow(card, Priority.ALWAYS);
            ((Region) card).setMaxWidth(Double.MAX_VALUE);
        }
        return new VBox(10, scope, cards);
    }

    private VBox metricCard(String title, Label value, String detail, String style) {
        VBox card = new VBox(10, label(title, "metric-title"), value, label(detail, "muted"));
        card.getStyleClass().addAll("metric-card", style);
        card.setMinWidth(0);
        return card;
    }

    private VBox buildGearSection() {
        Label instruction = label("Click the gear. Explore the transactions behind it.", "muted");
        HBox heading = new HBox(12, label("Your gear, connected", "section-title"), instruction);
        heading.setAlignment(Pos.CENTER_LEFT);
        HBox tiles = new HBox(14,
                gearTile("Keyboards", "keyboard", "Mechanical keys. Real numbers.", "keyboard.png"),
                gearTile("Mice", "mouse", "Every move, accounted for.", "mouse.png"),
                gearTile("Headsets", "headset", "Tune in to your sales.", "headset.png"));
        for (Node tile : tiles.getChildren()) {
            HBox.setHgrow(tile, Priority.ALWAYS);
            ((Region) tile).setMaxWidth(Double.MAX_VALUE);
        }
        gearSection.getChildren().setAll(heading, tiles);
        return gearSection;
    }

    private Button gearTile(String title, String term, String subtitle, String filename) {
        Button card = button("", "gear-tile");
        card.setMinWidth(0);
        card.setAccessibleText(title + ": show related ledger transactions");
        StackPane picture = new StackPane();
        picture.setMinHeight(110);
        picture.setPrefHeight(110);
        picture.getStyleClass().add("gear-picture");
        try (InputStream source = getClass().getResourceAsStream("/org/example/desktop/images/" + filename)) {
            if (source != null) {
                ImageView image = new ImageView(new Image(source, 360, 180, true, true));
                image.setFitHeight(110);
                image.setFitWidth(240);
                image.setPreserveRatio(true);
                picture.getChildren().add(image);
            }
        } catch (IOException ex) {
            picture.getChildren().add(label(title, "section-title"));
        }
        Region space = new Region();
        HBox.setHgrow(space, Priority.ALWAYS);
        HBox name = new HBox(label(title, "gear-title"), space, label("↗", "gold-text"));
        VBox text = new VBox(5, name, label(subtitle, "muted"));
        text.setPadding(new Insets(12, 15, 14, 15));
        VBox graphic = new VBox(picture, text);
        graphic.prefWidthProperty().bind(card.widthProperty().subtract(4));
        card.setGraphic(graphic);
        card.setOnAction(event -> {
            resetFilterValues();
            search.setText(term);
            applyFilters();
            setStatus("Showing transactions related to " + title.toLowerCase(Locale.ROOT) + ".", false);
            table.requestFocus();
        });
        return card;
    }

    private VBox buildLedgerPanel() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        advancedButton = button("Advanced filters", "text-button");
        advancedButton.setOnAction(event -> showAdvanced(!advanced.isVisible()));
        Button reset = button("Clear filters", "text-button");
        reset.setOnAction(event -> {
            if (currentView.equals("partner")) {
                navigate("transactions", navigation.get(1));
            } else {
                resetFilterValues();
                applyFilters();
            }
            setStatus("Filters cleared. Showing all transactions.", false);
        });
        HBox heading = new HBox(10, label("Transaction ledger", "section-title"), spacer, advancedButton, reset);
        heading.setAlignment(Pos.CENTER_LEFT);

        search.setPromptText("Search a description or vendor…");
        search.setAccessibleText("Search description or vendor");
        search.setPrefWidth(320);
        HBox.setHgrow(search, Priority.ALWAYS);
        search.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        period.getItems().setAll("All time", "Month to date", "Previous month", "Year to date", "Previous year");
        period.getSelectionModel().selectFirst();
        period.setPrefWidth(175);
        period.setAccessibleText("Report period");
        period.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        HBox typeButtons = new HBox(allTypes, typeToggle("Deposits", "deposit"), typeToggle("Payments", "payment"));
        typeButtons.getStyleClass().add("segmented-control");
        allTypes.setToggleGroup(types);
        allTypes.setUserData("all");
        allTypes.setSelected(true);
        motion.install(allTypes);
        types.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                // Wait until ToggleGroup finishes clearing its current selection.
                javafx.application.Platform.runLater(() -> {
                    if (types.getSelectedToggle() == null) {
                        allTypes.setSelected(true);
                    }
                });
            }
            applyFilters();
        });
        HBox toolbar = new HBox(12, search, period, typeButtons);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        buildAdvancedFilters();
        showAdvanced(false);
        configureTable();
        filterError.setWrapText(true);
        filterError.setVisible(false);
        filterError.setManaged(false);
        HBox footer = new HBox(resultCount);
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        Button detail = button("View selected entry", "text-button");
        detail.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        detail.setOnAction(event -> showDetails(table.getSelectionModel().getSelectedItem()));
        exportButton.setOnAction(event -> exportReport());
        openReportButton.setVisible(false);
        openReportButton.setManaged(false);
        openReportButton.setOnAction(event -> {
            if (lastExport != null && Files.isRegularFile(lastExport)) {
                getHostServices().showDocument(lastExport.toUri().toString());
            } else {
                setStatus("That report has been moved or removed. Export it again.", true);
            }
        });
        footer.getChildren().addAll(gap, detail, exportButton);
        footer.setAlignment(Pos.CENTER_LEFT);
        chartPanel = new TitledPane("Cash flow by month", cashFlow);
        chartPanel.setAnimated(false);
        chartPanel.getStyleClass().add("chart-panel");
        VBox panel = new VBox(15, heading, toolbar, advanced, filterError, chartPanel, table, footer, openReportButton);
        panel.getStyleClass().add("ledger-panel");
        return panel;
    }

    private ToggleButton typeToggle(String title, String value) {
        ToggleButton button = new ToggleButton(title);
        button.setToggleGroup(types);
        button.setUserData(value);
        motion.install(button);
        return button;
    }

    private void buildAdvancedFilters() {
        description.setPromptText("e.g. keyboard");
        vendor.setPromptText("e.g. Potara Customs");
        amount.setPromptText("e.g. -25.00");
        FlowPane fields = new FlowPane(12, 12,
                field("Start date", startDate), field("End date", endDate),
                field("Description contains", description), field("Vendor / customer", vendor), field("Exact signed amount", amount));
        Button apply = button("Apply filters", "primary-button");
        apply.setOnAction(event -> {
            if (applyFilters()) {
                setStatus("Filters applied. " + visible.size() + " matching transactions.", false);
            }
        });
        HBox controls = new HBox(16, exactVendor, apply);
        controls.setAlignment(Pos.CENTER_LEFT);
        advanced.getChildren().setAll(fields, controls,
                label("Leave fields blank to skip them. All active filters work together.", "muted"));
        advanced.getStyleClass().add("advanced-filters");
        description.setOnAction(event -> apply.fire());
        vendor.setOnAction(event -> apply.fire());
        amount.setOnAction(event -> apply.fire());
        for (DatePicker picker : List.of(startDate, endDate)) {
            picker.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                if (event.getCode() == KeyCode.ENTER) {
                    apply.fire();
                    event.consume();
                }
            });
            // JavaFX normally replaces the editor on Enter; keep invalid text visible for correction.
            picker.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
                if (event.getCode() == KeyCode.ENTER) {
                    event.consume();
                }
            });
        }
    }

    private VBox field(String name, Control input) {
        input.setPrefWidth(170);
        input.setAccessibleText(name);
        Label caption = label(name, "form-label");
        caption.setLabelFor(input);
        return new VBox(7, caption, input);
    }

    private void configureTable() {
        TableColumn<Transaction, String> date = new TableColumn<>("DATE / TIME");
        date.setCellValueFactory(value -> new ReadOnlyStringWrapper(
                value.getValue().getDate().format(DISPLAY_DATE) + "\n" + value.getValue().getTime().format(DISPLAY_TIME)));
        date.setPrefWidth(150);
        TableColumn<Transaction, String> title = new TableColumn<>("DESCRIPTION");
        title.setCellValueFactory(value -> new ReadOnlyStringWrapper(value.getValue().getDescription()));
        title.setPrefWidth(330);
        title.setCellFactory(column -> wrappingCell());
        TableColumn<Transaction, String> supplier = new TableColumn<>("VENDOR / CUSTOMER");
        supplier.setCellValueFactory(value -> new ReadOnlyStringWrapper(value.getValue().getVendor()));
        supplier.setPrefWidth(205);
        supplier.setCellFactory(column -> wrappingCell());
        TableColumn<Transaction, String> type = new TableColumn<>("TYPE");
        type.setCellValueFactory(value -> new ReadOnlyStringWrapper(value.getValue().isDeposit() ? "Deposit" : "Payment"));
        type.setPrefWidth(110);
        type.setCellFactory(column -> new TableCell<>() {
            @Override protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                setText(null);
                setGraphic(empty || value == null ? null : label(value, "type-badge", value.equals("Deposit") ? "badge-in" : "badge-out"));
            }
        });
        TableColumn<Transaction, BigDecimal> money = new TableColumn<>("AMOUNT");
        money.setCellValueFactory(value -> new ReadOnlyObjectWrapper<>(value.getValue().getAmount()));
        money.setPrefWidth(155);
        money.setCellFactory(column -> new TableCell<>() {
            @Override protected void updateItem(BigDecimal value, boolean empty) {
                super.updateItem(value, empty);
                getStyleClass().removeAll("positive", "negative", "amount-cell");
                setText(empty || value == null ? null : formatMoney(value));
                if (!empty && value != null) {
                    getStyleClass().addAll("amount-cell", value.signum() > 0 ? "positive" : "negative");
                }
                setAlignment(Pos.CENTER_RIGHT);
            }
        });
        table.getColumns().setAll(List.of(date, title, supplier, type, money));
        // Ledger order is always newest first, matching the capstone's console behavior.
        for (TableColumn<Transaction, ?> column : table.getColumns()) {
            column.setSortable(false);
            column.setReorderable(false);
        }
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setMinHeight(280);
        table.setPrefHeight(355);
        VBox emptyState = new VBox(8, label("No transactions match this view", "section-title"),
                label("Clear a filter, or add your first deposit or payment.", "muted"));
        emptyState.setAlignment(Pos.CENTER);
        table.setPlaceholder(emptyState);
        table.setAccessibleText("Transactions, newest first");
        table.setRowFactory(view -> {
            TableRow<Transaction> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    showDetails(row.getItem());
                }
            });
            return row;
        });
    }

    private TableCell<Transaction, String> wrappingCell() {
        return new TableCell<>() {
            private final Label text = new Label();
            { text.setWrapText(true); text.maxWidthProperty().bind(widthProperty().subtract(24)); }
            @Override protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                setText(null);
                text.setText(empty ? "" : value);
                setGraphic(empty ? null : text);
            }
        };
    }

    private HBox buildStatusBar() {
        status.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(status, Priority.ALWAYS);
        fileStatus.setMinWidth(Region.USE_PREF_SIZE);
        HBox bar = new HBox(10, statusDot, status, fileStatus);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("status-bar");
        fileStatus.setTooltip(new Tooltip(files.getFilePath().toString()));
        return bar;
    }

    private void navigate(String view, Button button) {
        currentView = view;
        for (Button link : navigation) {
            link.getStyleClass().remove("nav-selected");
        }
        button.getStyleClass().add("nav-selected");
        resetFilterValues();
        boolean overview = view.equals("overview");
        // Keep the overview visual; put the records first when doing ledger work.
        if (chartPanel != null) chartPanel.setExpanded(overview);
        gearSection.setVisible(overview);
        gearSection.setManaged(overview);
        showAdvanced(view.equals("reports") || view.equals("partner"));
        switch (view) {
            case "overview" -> {
                pageTitle.setText("Your money, in focus.");
                pageSubtitle.setText("Two sides of your money. One powerful ledger.");
            }
            case "transactions" -> {
                pageTitle.setText("Every transaction. One place.");
                pageSubtitle.setText("Find the details behind your income and expenses.");
            }
            case "reports" -> {
                pageTitle.setText("See the bigger picture.");
                pageSubtitle.setText("Choose a period or combine filters to explore your ledger.");
            }
            case "partner" -> {
                pageTitle.setText("Better, together.");
                pageSubtitle.setText("Potara Customs builds the PC. Potara Fusion Gear completes the setup.");
                vendor.setText("Potara Customs");
                exactVendor.setSelected(true);
            }
            default -> throw new IllegalArgumentException("Unknown view: " + view);
        }
        applyFilters();
        setStatus(button.getText() + " opened.", false);
        // Navigation takes effect immediately; motion never controls application state.
        if (workspaceScroll != null) workspaceScroll.setVvalue(0);
        if (workspace != null) motion.reveal(workspace);
    }

    private void showAdvanced(boolean show) {
        advanced.setVisible(show);
        advanced.setManaged(show);
        advancedButton.setText(show ? "Hide advanced filters" : "Advanced filters");
        if (show) motion.reveal(advanced);
    }

    private void resetFilterValues() {
        changingFilters = true;
        search.clear();
        period.getSelectionModel().selectFirst();
        allTypes.setSelected(true);
        startDate.setValue(null);
        startDate.getEditor().clear();
        endDate.setValue(null);
        endDate.getEditor().clear();
        description.clear();
        vendor.clear();
        amount.clear();
        exactVendor.setSelected(false);
        changingFilters = false;
    }

    private boolean applyFilters() {
        if (changingFilters || ledger == null || period.getValue() == null) {
            return false;
        }
        try {
            LocalDate first = readDate(startDate);
            LocalDate last = readDate(endDate);
            BigDecimal exactAmount = readAmountFilter();
            List<Transaction> matches = ledger.customSearch(first, last, description.getText(), vendor.getText(), exactAmount);
            LocalDate today = LocalDate.now();
            List<Transaction> report = switch (period.getValue()) {
                case "Month to date" -> ledger.reportMonthToDate(today);
                case "Previous month" -> ledger.reportPreviousMonth(today);
                case "Year to date" -> ledger.reportYearToDate(today);
                case "Previous year" -> ledger.reportPreviousYear(today);
                default -> ledger.getAll();
            };
            matches.retainAll(report);
            String term = search.getText().trim().toLowerCase(Locale.ROOT);
            String type = types.getSelectedToggle() == null ? "all" : types.getSelectedToggle().getUserData().toString();
            matches.removeIf(entry -> (!term.isEmpty() && !entry.getDescription().toLowerCase(Locale.ROOT).contains(term)
                    && !entry.getVendor().toLowerCase(Locale.ROOT).contains(term))
                    || (type.equals("deposit") && !entry.isDeposit())
                    || (type.equals("payment") && !entry.isPayment())
                    || (exactVendor.isSelected() && !vendor.getText().isBlank()
                    && !entry.getVendor().equalsIgnoreCase(vendor.getText().trim())));
            visible.setAll(matches);
            cashFlow.update(matches);
            appliedFilterSummary = describeFilters(first, last, exactAmount, type);
            deposits.setText(formatMoney(ledger.getTotalDeposits(matches)));
            payments.setText(formatMoney(ledger.getTotalPayments(matches)));
            netChange.setText(formatMoney(ledger.getNetChange(matches)));
            boolean filtered = !term.isEmpty() || !type.equals("all") || first != null || last != null
                    || !description.getText().isBlank() || !vendor.getText().isBlank() || exactAmount != null;
            scope.setText(period.getValue() + (filtered ? "  /  Filtered view" : "  /  Overview"));
            resultCount.setText(matches.size() + " of " + ledger.getAll().size() + " transactions  •  Newest first");
            fileStatus.setText((demoMode ? "DEMO ledger" : "Your ledger") + "  •  " + ledger.getAll().size() + " records");
            filterError.setVisible(false);
            filterError.setManaged(false);
            return true;
        } catch (IllegalArgumentException | DateTimeParseException ex) {
            filterError.setText("Filters not applied: " + ex.getMessage());
            filterError.setVisible(true);
            filterError.setManaged(true);
            setStatus("Check the highlighted filter message.", true);
            return false;
        }
    }

    private void buildModeBanner() {
        Label title = label("DEMO MODE • Fictional transactions", "demo-title");
        Label detail = label("Practice freely. Demo changes are saved separately from your ledger.", "muted");
        detail.setWrapText(true);
        VBox text = new VBox(4, title, detail);
        HBox.setHgrow(text, Priority.ALWAYS);
        Button reset = button("Reset demo", "secondary-button");
        reset.setOnAction(event -> resetDemo());
        Button leave = button("Back to my ledger", "secondary-button");
        leave.setOnAction(event -> switchMode(false));
        modeBanner.getChildren().setAll(text, reset, leave);
        modeBanner.setAlignment(Pos.CENTER_LEFT);
        modeBanner.getStyleClass().add("demo-banner");
    }

    private void switchMode(boolean useDemo) {
        if (useDemo == demoMode) return;
        try {
            DemoData.validateSeparateFrom(normalFile);
            FileManager candidate = useDemo ? DemoData.open() : new FileManager(normalFile);
            LedgerService candidateLedger = new LedgerService(candidate);
            // Change the screen only after the other ledger has loaded successfully.
            files = candidate;
            ledger = candidateLedger;
            demoMode = useDemo;
            updateModeAppearance();
            navigate("overview", navigation.get(0));
            setStatus(useDemo ? "Presentation mode ready. Changes affect the demo ledger only."
                    : "Welcome back. Your own ledger is loaded.", false);
        } catch (IOException | IllegalArgumentException ex) {
            setStatus("Could not switch ledgers. " + ex.getMessage(), true);
        }
    }

    private void resetDemo() {
        if (!demoMode) return;
        if (!confirm("Reset the demo ledger?", "This replaces the demo transactions with the fictional starter history, "
                + "using dates relative to today. Your own ledger will not change.", "Reset demo")) {
            setStatus("Reset cancelled. Your demo transactions are unchanged.", false);
            return;
        }
        try {
            DemoData.validateSeparateFrom(normalFile);
            DemoData.reset(files);
            ledger = new LedgerService(files);
            navigate("overview", navigation.get(0));
            setStatus("Demo reset. The starter transactions are ready for your presentation.", false);
        } catch (IOException | IllegalArgumentException ex) {
            setStatus("Demo reset failed. " + ex.getMessage(), true);
        }
    }

    private void updateModeAppearance() {
        modeBanner.setVisible(demoMode);
        modeBanner.setManaged(demoMode);
        demoButton.setText(demoMode ? "Back to my ledger" : "Presentation mode");
        window.setTitle((demoMode ? "[DEMO] " : "") + "Potara Fusion Ledger | Potara Fusion Gear");
        fileStatus.setTooltip(new Tooltip(files.getFilePath().toString()));
    }

    private boolean confirm(String heading, String message, String action) {
        ButtonType proceed = new ButtonType(action, ButtonBar.ButtonData.OK_DONE);
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION, message, proceed, ButtonType.CANCEL);
        confirmation.initOwner(window);
        confirmation.setTitle("Potara Fusion Ledger");
        confirmation.setHeaderText(heading);
        confirmation.getDialogPane().getStylesheets().add(getClass().getResource(STYLE).toExternalForm());
        confirmation.getDialogPane().lookupButton(ButtonType.CANCEL).getStyleClass().add("secondary-button");
        for (ButtonType type : confirmation.getButtonTypes()) {
            motion.install((ButtonBase) confirmation.getDialogPane().lookupButton(type));
        }
        return confirmation.showAndWait().orElse(ButtonType.CANCEL) == proceed;
    }

    private String describeFilters(LocalDate first, LocalDate last, BigDecimal exactAmount, String type) {
        List<String> parts = new ArrayList<>();
        parts.add("Period: " + period.getValue());
        parts.add("Type: " + switch (type) { case "deposit" -> "Deposits"; case "payment" -> "Payments"; default -> "All entries"; });
        if (!search.getText().isBlank()) parts.add("Description or vendor contains: " + search.getText().trim());
        if (first != null) parts.add("Start date (inclusive): " + first);
        if (last != null) parts.add("End date (inclusive): " + last);
        if (!description.getText().isBlank()) parts.add("Description contains: " + description.getText().trim());
        if (!vendor.getText().isBlank()) parts.add("Vendor " + (exactVendor.isSelected() ? "equals: " : "contains: ") + vendor.getText().trim());
        if (exactAmount != null) parts.add("Signed amount: " + exactAmount.toPlainString());
        return String.join(" | ", parts);
    }

    private void exportReport() {
        if (!applyFilters()) return;
        List<Transaction> snapshot = List.copyOf(visible);
        String filterSummary = appliedFilterSummary;
        boolean exportedDemo = demoMode;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save your Potara ledger report");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF report (*.pdf)", "*.pdf"));
        chooser.setInitialFileName((demoMode ? "Potara-Demo-" : "Potara-Ledger-")
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".pdf");
        File selected = chooser.showSaveDialog(window);
        if (selected == null) {
            setStatus("Export cancelled. No report was saved.", false);
            return;
        }
        Path target = selected.toPath().toAbsolutePath().normalize();
        if (!target.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            target = target.resolveSibling(target.getFileName() + ".pdf");
            if (Files.exists(target) && !confirm("Replace this PDF?", target.toString(), "Replace report")) {
                setStatus("Export cancelled. The existing report is unchanged.", false);
                return;
            }
        }
        try {
            for (Path protectedFile : List.of(normalFile, files.getFilePath(), DemoData.getFilePath())) {
                if (target.equals(protectedFile) || (Files.exists(target) && Files.exists(protectedFile)
                        && Files.isSameFile(target, protectedFile))) {
                    setStatus("Choose a different report filename. A report cannot replace a ledger file.", true);
                    return;
                }
            }
        } catch (IOException ex) {
            setStatus("Could not check the destination. " + ex.getMessage(), true);
            return;
        }
        final Path destination = target;
        exportButton.setDisable(true);
        exportButton.setText("Exporting…");
        ProgressIndicator progress = new ProgressIndicator();
        progress.setMinSize(16, 16);
        progress.setPrefSize(16, 16);
        progress.setMaxSize(16, 16);
        // With reduced motion, show a stationary ring beside the explicit busy text.
        progress.progressProperty().bind(Bindings.when(motion.reducedMotionProperty()).then(0.0).otherwise(-1.0));
        exportButton.setGraphic(progress);
        setStatus("Creating your report for " + snapshot.size() + " displayed transactions…", false);
        Task<Void> export = new Task<>() {
            @Override protected Void call() throws IOException {
                ReportExporter.export(destination, snapshot, filterSummary, exportedDemo);
                return null;
            }
        };
        export.setOnSucceeded(event -> {
            finishExport();
            lastExport = destination;
            openReportButton.setVisible(true);
            openReportButton.setManaged(true);
            openReportButton.setTooltip(new Tooltip(destination.toString()));
            motion.feedback(openReportButton);
            setStatus((exportedDemo ? "Demo report" : "Report") + " saved: " + destination
                    + " (" + snapshot.size() + " transactions).", false);
        });
        export.setOnFailed(event -> {
            finishExport();
            setStatus("Report was not saved. " + export.getException().getMessage(), true);
        });
        // Finish an in-progress file write even if the user closes the window.
        new Thread(export, "potara-report-export").start();
    }

    private void finishExport() {
        exportButton.setDisable(false);
        exportButton.setText("Export PDF report");
        if (exportButton.getGraphic() instanceof ProgressIndicator progress) {
            progress.progressProperty().unbind();
            progress.setProgress(0);
        }
        exportButton.setGraphic(null);
    }

    private BigDecimal readAmountFilter() {
        String text = amount.getText().trim();
        if (text.isEmpty()) {
            return null;
        }
        if (!text.matches("-?\\d+(\\.\\d{1,2})?")) {
            throw new IllegalArgumentException("Use an amount such as 25.00 or -25.00, with no commas.");
        }
        BigDecimal value = new BigDecimal(text);
        if (value.signum() == 0 || value.abs().compareTo(Transaction.MAX_AMOUNT) > 0) {
            throw new IllegalArgumentException("Amount must be nonzero and no larger than $999,999,999.99 in magnitude.");
        }
        return value;
    }

    private static DatePicker datePicker() {
        DatePicker picker = new DatePicker();
        picker.setPromptText("YYYY-MM-DD");
        picker.setConverter(new StringConverter<>() {
            @Override public String toString(LocalDate value) { return value == null ? "" : value.toString(); }
            @Override public LocalDate fromString(String text) {
                // Validation happens when filters are applied, so incomplete typing cannot crash the UI.
                try { return text == null || text.isBlank() ? null : LocalDate.parse(text.trim()); }
                catch (DateTimeParseException ex) { return null; }
            }
        });
        return picker;
    }

    private LocalDate readDate(DatePicker picker) {
        String text = picker.getEditor().getText().trim();
        if (text.isEmpty()) {
            return null;
        }
        if (!text.matches("\\d{4}-\\d{2}-\\d{2}")) {
            throw new IllegalArgumentException("Dates must use YYYY-MM-DD, such as 2026-10-31.");
        }
        LocalDate date = LocalDate.parse(text);
        if (date.getYear() < 1) {
            throw new IllegalArgumentException("The year must be between 0001 and 9999.");
        }
        return date;
    }

    private void addTransaction(boolean deposit) {
        if (TransactionDialog.show(window, deposit, ledger::addTransaction, motion)) {
            navigate("transactions", navigation.get(1));
            setStatus((deposit ? "Deposit" : "Payment") + " saved. Your ledger is up to date.", false);
        }
    }

    private void reload() {
        try {
            FileManager candidateFiles = new FileManager(files.getFilePath());
            LedgerService candidateLedger = new LedgerService(candidateFiles);
            files = candidateFiles;
            ledger = candidateLedger;
            boolean validFilters = applyFilters();
            if (!validFilters) {
                if (currentView.equals("partner")) {
                    navigate("transactions", navigation.get(1));
                } else {
                    resetFilterValues();
                    applyFilters();
                }
            }
            setStatus("Reloaded " + ledger.getAll().size() + " saved transactions."
                    + (validFilters ? "" : " Invalid filters were cleared."), false);
        } catch (IOException | IllegalArgumentException ex) {
            setStatus("Reload failed. " + ex.getMessage(), true);
        }
    }

    private void showDetails(Transaction entry) {
        if (entry == null) {
            return;
        }
        Dialog<Void> dialog = new Dialog<>();
        dialog.initOwner(window);
        dialog.setTitle("Transaction details");
        dialog.getDialogPane().getStylesheets().add(getClass().getResource(STYLE).toExternalForm());
        Label detail = label(entry.getDescription(), "dialog-title");
        detail.setWrapText(true);
        Label who = label(entry.getVendor(), "section-title");
        who.setWrapText(true);
        VBox content = new VBox(14, label(entry.isDeposit() ? "DEPOSIT" : "PAYMENT", "eyebrow"), detail,
                label(formatMoney(entry.getAmount()), "metric-value", entry.isDeposit() ? "positive" : "negative"), who,
                label(entry.getDate() + "  at  " + entry.getTime().format(DISPLAY_TIME), "muted"));
        content.setPadding(new Insets(24));
        content.setPrefWidth(480);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        motion.install((ButtonBase) dialog.getDialogPane().lookupButton(ButtonType.CLOSE));
        dialog.setOnShown(event -> motion.reveal(content));
        dialog.showAndWait();
    }

    private void setStatus(String message, boolean error) {
        status.setText(message);
        status.getStyleClass().remove("negative");
        if (error) {
            status.getStyleClass().add("negative");
        }
        status.setTooltip(new Tooltip(message));
        statusDot.setFill(Color.web(error ? "#efaaa4" : "#b9e88a"));
        motion.feedback(status);
    }

    private static String formatMoney(BigDecimal value) {
        return String.format(Locale.US, "%s$%,.2f", value.signum() < 0 ? "−" : "", value.abs());
    }

    private static Label label(String text, String... classes) {
        Label label = new Label(text);
        label.getStyleClass().addAll(classes);
        return label;
    }

    private Button button(String text, String style) {
        Button button = new Button(text);
        button.getStyleClass().add(style);
        motion.install(button);
        return button;
    }
}
