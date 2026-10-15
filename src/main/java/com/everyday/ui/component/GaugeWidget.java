package com.everyday.ui.component;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;

/**
 * Modern semi-circular gauge inspired by the dashboard aesthetic.
 */
public class GaugeWidget extends VBox {

    private final Arc trackArc;
    private final Arc valueArc;
    private final Label percentageLabel;
    private final Label statusLabel;
    private final Label titleLabel;

    public GaugeWidget(String title, String subtitle) {
        this.setAlignment(Pos.CENTER);
        this.setSpacing(6);
        this.getStyleClass().add("glass-card");

        titleLabel = new Label(title);
        titleLabel.getStyleClass().add("card-header-title");

        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("card-subtitle");

        double radius = 70;

        // Background track arc (180 degrees semi-circle)
        trackArc = new Arc(0, 0, radius, radius, 180, -180);
        trackArc.setType(ArcType.OPEN);
        trackArc.setFill(Color.TRANSPARENT);
        trackArc.setStroke(Color.web("#1E293B"));
        trackArc.setStrokeWidth(12);
        trackArc.setStrokeLineCap(StrokeLineCap.ROUND);

        // Value arc
        valueArc = new Arc(0, 0, radius, radius, 180, 0);
        valueArc.setType(ArcType.OPEN);
        valueArc.setFill(Color.TRANSPARENT);
        valueArc.setStroke(Color.web("#00D2FF"));
        valueArc.setStrokeWidth(12);
        valueArc.setStrokeLineCap(StrokeLineCap.ROUND);

        DropShadow glow = new DropShadow();
        glow.setColor(Color.web("#00D2FF", 0.5));
        glow.setRadius(12);
        glow.setSpread(0.2);
        valueArc.setEffect(glow);

        percentageLabel = new Label("0%");
        percentageLabel.setStyle("-fx-font-size: 26px; -fx-font-weight: 800; -fx-text-fill: #F8FAFC;");

        statusLabel = new Label("Incomplete");
        statusLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: #94A3B8;");

        VBox centerBox = new VBox(2, percentageLabel, statusLabel);
        centerBox.setAlignment(Pos.CENTER);
        centerBox.setTranslateY(20);

        StackPane arcPane = new StackPane(trackArc, valueArc, centerBox);
        arcPane.setPrefSize(180, 120);

        this.getChildren().addAll(titleLabel, subtitleLabel, arcPane);
    }

    public void setValue(double percentage, String status) {
        double clamped = Math.max(0.0, Math.min(100.0, percentage));
        double angle = -(clamped / 100.0) * 180.0;
        valueArc.setLength(angle);

        percentageLabel.setText(String.format("%.1f%%", clamped));
        statusLabel.setText(status);

        if (clamped >= 100.0) {
            valueArc.setStroke(Color.web("#10B981"));
            statusLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: #34D399;");
        } else if (clamped >= 50.0) {
            valueArc.setStroke(Color.web("#00D2FF"));
            statusLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: #60A5FA;");
        } else {
            valueArc.setStroke(Color.web("#EF4444"));
            statusLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: #F87171;");
        }
    }
}
