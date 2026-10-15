package com.everyday.ui.dialog;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class ConfirmDialog {

    public enum DeleteChoice {
        CANCEL,
        DELETE_OCCURRENCE,
        DELETE_ENTIRE_TASK
    }

    public static boolean confirm(Stage owner, String title, String message) {
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.UTILITY);
        dialog.setTitle(title);

        VBox root = new VBox(16);
        root.setPadding(new Insets(24));
        root.getStyleClass().addAll("glass-card");
        root.setStyle("-fx-background-color: #131D31;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #F8FAFC;");

        Label msgLabel = new Label(message);
        msgLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #94A3B8; -fx-wrap-text: true;");
        msgLabel.setPrefWidth(340);

        HBox btnBox = new HBox(12);
        btnBox.setAlignment(Pos.CENTER_RIGHT);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-secondary");

        Button confirmBtn = new Button("Confirm");
        confirmBtn.getStyleClass().add("btn-danger");

        final boolean[] result = {false};

        cancelBtn.setOnAction(e -> {
            result[0] = false;
            dialog.close();
        });

        confirmBtn.setOnAction(e -> {
            result[0] = true;
            dialog.close();
        });

        btnBox.getChildren().addAll(cancelBtn, confirmBtn);
        root.getChildren().addAll(titleLabel, msgLabel, btnBox);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(ConfirmDialog.class.getResource("/com/everyday/css/styles.css").toExternalForm());
        dialog.setScene(scene);
        dialog.showAndWait();

        return result[0];
    }

    public static DeleteChoice confirmRecurringTaskDelete(Stage owner, String taskName) {
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.UTILITY);
        dialog.setTitle("Delete Recurring Task");

        VBox root = new VBox(16);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: #131D31;");

        Label titleLabel = new Label("Delete Task: " + taskName);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #F8FAFC;");

        Label msgLabel = new Label("This is a recurring task. How would you like to delete it?");
        msgLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #94A3B8;");

        HBox btnBox = new HBox(10);
        btnBox.setAlignment(Pos.CENTER_RIGHT);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-secondary");

        Button deleteOccurrenceBtn = new Button("Delete this occurrence");
        deleteOccurrenceBtn.getStyleClass().add("btn-secondary");

        Button deleteEntireBtn = new Button("Delete entire recurring task");
        deleteEntireBtn.getStyleClass().add("btn-danger");

        final DeleteChoice[] choice = {DeleteChoice.CANCEL};

        cancelBtn.setOnAction(e -> {
            choice[0] = DeleteChoice.CANCEL;
            dialog.close();
        });

        deleteOccurrenceBtn.setOnAction(e -> {
            choice[0] = DeleteChoice.DELETE_OCCURRENCE;
            dialog.close();
        });

        deleteEntireBtn.setOnAction(e -> {
            choice[0] = DeleteChoice.DELETE_ENTIRE_TASK;
            dialog.close();
        });

        btnBox.getChildren().addAll(cancelBtn, deleteOccurrenceBtn, deleteEntireBtn);
        root.getChildren().addAll(titleLabel, msgLabel, btnBox);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(ConfirmDialog.class.getResource("/com/everyday/css/styles.css").toExternalForm());
        dialog.setScene(scene);
        dialog.showAndWait();

        return choice[0];
    }
}
