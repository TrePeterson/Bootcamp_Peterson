package org.example;

import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import javafx.util.Duration;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/** Plots exactly the filtered table rows; money is summed before converting to chart coordinates. */
public final class CashFlowChart extends VBox {
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMM uuuu", Locale.US);
    private final CategoryAxis months = new CategoryAxis();
    private final NumberAxis dollars = new NumberAxis();
    private final BarChart<String, Number> chart = new BarChart<>(months, dollars);
    private final ScrollPane scroll = new ScrollPane(chart);
    private final Label caption = new Label();
    private final Label empty = new Label("No matching activity to chart. Try clearing a filter.");

    public CashFlowChart() {
        getStyleClass().add("cash-flow");
        setSpacing(5);
        setAlignment(Pos.CENTER_LEFT);
        caption.getStyleClass().add("muted");
        caption.setWrapText(true);
        empty.getStyleClass().add("muted");
        empty.setMinHeight(95);
        chart.setAnimated(false);
        chart.setLegendVisible(true);
        chart.setCategoryGap(16);
        chart.setBarGap(3);
        chart.setPrefHeight(255);
        chart.setMinHeight(255);
        chart.setMaxHeight(255);
        chart.setVerticalGridLinesVisible(false);
        chart.setHorizontalGridLinesVisible(true);
        chart.setAlternativeRowFillVisible(false);
        chart.setAlternativeColumnFillVisible(false);
        chart.setAccessibleText("Monthly cash flow for the currently displayed transactions");
        months.setAnimated(false);
        months.setTickLabelRotation(0);
        dollars.setAnimated(false);
        dollars.setForceZeroInRange(true);
        dollars.setTickLabelFormatter(new StringConverter<>() {
            @Override public String toString(Number value) {
                double number = value.doubleValue();
                if (Math.abs(number) >= 1_000_000) return String.format(Locale.US, "$%.1fm", number / 1_000_000);
                if (Math.abs(number) >= 1_000) return String.format(Locale.US, "$%.1fk", number / 1_000);
                return String.format(Locale.US, "$%.0f", number);
            }
            @Override public Number fromString(String text) { return 0; }
        });
        scroll.setFitToHeight(true);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.getStyleClass().add("chart-scroll");
        chart.minWidthProperty().bind(scroll.widthProperty().subtract(4));
        getChildren().addAll(caption, scroll, empty);
    }

    /** Pure aggregation is also used by focused checks without displaying a window. */
    public static Map<YearMonth, MonthlyTotals> summarize(List<Transaction> entries) {
        Map<YearMonth, MonthlyTotals> totals = new TreeMap<>();
        for (Transaction entry : entries) {
            YearMonth month = YearMonth.from(entry.getDate());
            MonthlyTotals current = totals.getOrDefault(month,
                    new MonthlyTotals(new BigDecimal("0.00"), new BigDecimal("0.00")));
            totals.put(month, entry.isDeposit()
                    ? new MonthlyTotals(current.deposits().add(entry.getAmount()), current.payments())
                    : new MonthlyTotals(current.deposits(), current.payments().add(entry.getAmount().abs())));
        }
        return totals;
    }

    public record MonthlyTotals(BigDecimal deposits, BigDecimal payments) { }

    public void update(List<Transaction> entries) {
        Map<YearMonth, MonthlyTotals> totals = summarize(entries);
        boolean hasData = !totals.isEmpty();
        scroll.setVisible(hasData);
        scroll.setManaged(hasData);
        empty.setVisible(!hasData);
        empty.setManaged(!hasData);
        caption.setText("Same filters as the ledger • Months with activity • Hover a bar for exact dollars"
                + (totals.size() > 10 ? " • Scroll sideways for more months" : ""));
        XYChart.Series<String, Number> moneyIn = new XYChart.Series<>();
        moneyIn.setName("Money in");
        XYChart.Series<String, Number> moneyOut = new XYChart.Series<>();
        moneyOut.setName("Money out");
        months.setCategories(FXCollections.observableArrayList(totals.keySet().stream()
                .map(month -> month.format(MONTH)).toList()));
        totals.forEach((month, values) -> {
            moneyIn.getData().add(bar(month, values.deposits(), "Money in"));
            moneyOut.getData().add(bar(month, values.payments(), "Money out"));
        });
        chart.getData().setAll(List.of(moneyIn, moneyOut));
        chart.setPrefWidth(Math.max(650, totals.size() * 95.0 + 90));
    }

    private XYChart.Data<String, Number> bar(YearMonth month, BigDecimal value, String type) {
        XYChart.Data<String, Number> bar = new XYChart.Data<>(month.format(MONTH), value.doubleValue());
        String detail = month.format(MONTH) + " • " + type + ": " + String.format(Locale.US, "$%,.2f", value);
        bar.nodeProperty().addListener((observable, previous, node) -> installTooltip(node, detail));
        return bar;
    }

    private void installTooltip(Node node, String detail) {
        if (node == null) return;
        Tooltip tooltip = new Tooltip(detail);
        tooltip.setShowDelay(Duration.millis(100));
        Tooltip.install(node, tooltip);
        node.setAccessibleText(detail);
    }
}
