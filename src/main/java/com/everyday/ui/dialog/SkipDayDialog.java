package com.everyday.ui.dialog;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.time.LocalDate;

public class SkipDayDialog {

    public static class SkipResult {
        public LocalDate date;
        public String reason;
        public SkipResult(LocalDate date, String reason) {
            this.date = date;
            this.reason = reason;
        }
    }

    public static SkipResult show(Stage owner, LocalDate defaultDate) {
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.UTILITY);
        dialog.setTitle("Skip Day");

        VBox root = new VBox(16);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: #131D31; -fx-pref-width: 400px;");

        Label titleLabel = new Label("Intentionally Skip Day");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #F8FAFC;");

        Label noteLabel = new Label("Skipping preserves your streak without breaking it or increasing it. A reason is required.");
        noteLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #94A3B8; -fx-wrap-text: true;");

        Label dateLabel = new Label("Date:");
        dateLabel.setStyle("-fx-text-fill: #E2E8F0; -fx-font-weight: 600;");
        DatePicker datePicker = new DatePicker(defaultDate != null ? defaultDate : LocalDate.now());
        datePicker.setMaxWidth(Double.MAX_VALUE);

        Label reasonLabel = new Label("Reason (Required):");
        reasonLabel.setStyle("-fx-text-fill: #E2E8F0; -fx-font-weight: 600;");
        TextField reasonInput = new TextField();
        reasonInput.setPromptText("e.g., Travelling, Sick, Family emergency");

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 12px;");
        errorLabel.setVisible(false);

        HBox btnBox = new HBox(12);
        btnBox.setAlignment(Pos.CENTER_RIGHT);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-secondary");

        Button submitBtn = new Button("Confirm Skip");
        submitBtn.getStyleClass().add("btn-primary");

        final SkipResult[] result = {null};

        cancelBtn.setOnAction(e -> dialog.close());

        submitBtn.setOnAction(e -> {
            String reason = reasonInput.getText();
            if (reason == null || reason.trim().isEmpty()) {
                errorLabel.setText("Please provide a reason to skip this day.");
                errorLabel.setVisible(true);
                return;
            }
            result[0] = new SkipResult(datePicker.getValue(), reason.trim());
            dialog.close();
        });

        btnBox.getChildren().addAll(cancelBtn, submitBtn);
        root.getChildren().addAll(titleLabel, noteLabel, dateLabel, datePicker, reasonLabel, reasonInput, errorLabel, btnBox);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(SkipDayDialog.class.getResource("/com/everyday/css/styles.css").toExternalForm());
        dialog.setScene(scene);
        dialog.showAndWait();

        return result[0];
    }
}
