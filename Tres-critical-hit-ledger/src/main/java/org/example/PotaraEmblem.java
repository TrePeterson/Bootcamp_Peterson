package org.example;

import javafx.scene.Group;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.transform.Rotate;

/** A small, resolution-independent pair of Potara earrings for the sidebar. */
public final class PotaraEmblem {
    private static final Paint GOLD = new LinearGradient(0, 0, 1, 1, true,
            CycleMethod.NO_CYCLE, new Stop(0, Color.web("#fff4b4")),
            new Stop(0.48, Color.web("#ffd675")), new Stop(1, Color.web("#b57a27")));

    private PotaraEmblem() {
    }

    public static Region create() {
        Pane emblem = new Pane();
        emblem.setMinSize(112, 66);
        emblem.setPrefSize(112, 66);
        emblem.setMaxSize(112, 66);
        emblem.getStyleClass().add("potara-emblem");
        emblem.setAccessibleText("Paired Potara earrings");

        Path fusion = new Path(new MoveTo(55, 25), new LineTo(49, 36),
                new LineTo(56, 33), new LineTo(53, 43), new LineTo(64, 28),
                new LineTo(58, 31), new LineTo(61, 25), new ClosePath());
        fusion.setFill(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#8ec8ff")), new Stop(1, Color.web("#ffad62"))));
        fusion.setStroke(null);
        fusion.setOpacity(0.85);
        fusion.setEffect(new DropShadow(5, Color.web("#477bce", 0.35)));

        emblem.getChildren().addAll(fusion, earring(29, 8), earring(83, -8));
        return emblem;
    }

    private static Group earring(double x, double tilt) {
        Ellipse hoop = new Ellipse(0, 11, 6.2, 8);
        hoop.setFill(Color.TRANSPARENT);
        hoop.setStroke(GOLD);
        hoop.setStrokeWidth(3);

        Arc hoopGlint = new Arc(0, 11, 6.2, 8, 70, 80);
        hoopGlint.setType(ArcType.OPEN);
        hoopGlint.setFill(Color.TRANSPARENT);
        hoopGlint.setStroke(Color.web("#fff9d7"));
        hoopGlint.setStrokeWidth(1.3);
        hoopGlint.setStrokeLineCap(StrokeLineCap.ROUND);

        Line stem = new Line(0, 19, 0, 28);
        stem.setStroke(GOLD);
        stem.setStrokeWidth(3.3);
        stem.setStrokeLineCap(StrokeLineCap.ROUND);

        Circle gem = new Circle(0, 41, 12.5);
        gem.setFill(new RadialGradient(225, 0.35, 0.32, 0.25, 0.85, true,
                CycleMethod.NO_CYCLE, new Stop(0, Color.web("#e5ffc1")),
                new Stop(0.35, Color.web("#b9e88a")),
                new Stop(0.72, Color.web("#41b976")), new Stop(1, Color.web("#147452"))));
        gem.setStroke(GOLD);
        gem.setStrokeWidth(1.5);
        gem.setEffect(new DropShadow(4, Color.web("#62d89a", 0.28)));

        Polygon facet = new Polygon(-8.5, 33.5, 0, 29.5, 5.5, 39, -3, 45);
        facet.setFill(Color.web("#eeffd2", 0.16));
        Polygon lowerFacet = new Polygon(-3, 45, 5.5, 39, 9, 47, 0, 52);
        lowerFacet.setFill(Color.web("#0b6549", 0.19));
        Ellipse shine = new Ellipse(-4.5, 35.5, 2.3, 3.8);
        shine.setFill(Color.web("#f6ffe7", 0.88));
        shine.setRotate(32);
        Circle sparkle = new Circle(0, 32.5, 1.1, Color.web("#ffffff", 0.8));

        Rectangle cap = new Rectangle(-5.5, 25.5, 11, 5.5);
        cap.setArcWidth(3);
        cap.setArcHeight(3);
        cap.setFill(GOLD);
        cap.setStroke(Color.web("#fff0ac"));
        cap.setStrokeWidth(0.6);

        Group earring = new Group(hoop, hoopGlint, stem, gem, facet, lowerFacet, shine, sparkle, cap);
        earring.getTransforms().add(new Rotate(tilt, 0, 11));
        earring.setTranslateX(x);
        earring.setTranslateY(3);
        earring.setMouseTransparent(true);
        return earring;
    }
}
