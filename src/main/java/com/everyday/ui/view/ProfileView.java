package com.everyday.ui.view;

import com.everyday.model.UserProfile;
import com.everyday.service.ServiceRegistry;
import com.everyday.ui.component.Icons;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class ProfileView extends ScrollPane {

    private final ServiceRegistry registry;
    private final Stage primaryStage;
    private final Runnable profileUpdateNotifier;

    private TextField nameField;
    private TextField taglineField;
    private ImageView avatarPreview;
    private String selectedAvatarPath;

    public ProfileView(ServiceRegistry registry, Stage primaryStage, Runnable profileUpdateNotifier) {
        this.registry = registry;
        this.primaryStage = primaryStage;
        this.profileUpdateNotifier = profileUpdateNotifier;

        this.setFitToWidth(true);
        this.setStyle("-fx-background-color: transparent;");

        VBox content = new VBox(24);
        content.setPadding(new Insets(28, 36, 40, 36));

        // Header
        VBox titleBox = new VBox(4);
        Label title = new Label("User Profile");
        title.getStyleClass().add("section-title");
        Label subtitle = new Label("Local-first personal identity. No login or external account required.");
        subtitle.getStyleClass().add("section-subtitle");
        titleBox.getChildren().addAll(title, subtitle);
        content.getChildren().add(titleBox);

        // Profile Editor Card
        content.getChildren().add(buildProfileEditorCard());

        this.setContent(content);
        refresh();
    }

    private Node buildProfileEditorCard() {
        VBox card = new VBox(20);
        card.getStyleClass().add("glass-card");
        card.setPadding(new Insets(24));
        card.setMaxWidth(560);

        Label cardTitle = new Label("Edit Local Profile");
        cardTitle.getStyleClass().add("card-header-title");

        // Avatar row
        HBox avatarRow = new HBox(20);
        avatarRow.setAlignment(Pos.CENTER_LEFT);

        avatarPreview = new ImageView();
        avatarPreview.setFitWidth(80);
        avatarPreview.setFitHeight(80);
        Circle clip = new Circle(40, 40, 40);
        avatarPreview.setClip(clip);

        VBox avatarBtns = new VBox(8);
        Button chooseImgBtn = new Button("Choose Picture...");
        chooseImgBtn.getStyleClass().add("btn-secondary");
        chooseImgBtn.setOnAction(e -> handleChooseAvatar());

        Button resetImgBtn = new Button("Use Default Icon");
        resetImgBtn.getStyleClass().add("btn-secondary");
        resetImgBtn.setOnAction(e -> {
            selectedAvatarPath = null;
            updateAvatarDisplay(null);
        });

        avatarBtns.getChildren().addAll(chooseImgBtn, resetImgBtn);
        avatarRow.getChildren().addAll(avatarPreview, avatarBtns);

        // Fields
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(14);

        Label nameLbl = new Label("Display Name *");
        nameLbl.setStyle("-fx-text-fill: #E2E8F0; -fx-font-weight: 600;");
        nameField = new TextField();
        nameField.setPrefWidth(300);

        Label tagLbl = new Label("Account Role / Tagline");
        tagLbl.setStyle("-fx-text-fill: #E2E8F0; -fx-font-weight: 600;");
        taglineField = new TextField("Personal");
        taglineField.setPrefWidth(300);

        grid.add(nameLbl, 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(tagLbl, 0, 1);
        grid.add(taglineField, 1, 1);

        Button saveBtn = new Button("Save Profile Changes");
        saveBtn.getStyleClass().add("btn-primary");
        saveBtn.setOnAction(e -> handleSaveProfile());

        card.getChildren().addAll(cardTitle, avatarRow, grid, saveBtn);
        return card;
    }

    public void refresh() {
        try {
            UserProfile profile = registry.getProfileService().getProfile();
            nameField.setText(profile.getName());
            taglineField.setText(profile.getTagline());
            selectedAvatarPath = profile.getAvatarPath();
            updateAvatarDisplay(selectedAvatarPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateAvatarDisplay(String path) {
        if (path != null && new File(path).exists()) {
            try {
                avatarPreview.setImage(new Image(new File(path).toURI().toString()));
                return;
            } catch (Exception ignored) {}
        }
        // Fallback default avatar generator
        avatarPreview.setImage(new Image("https://api.dicebear.com/7.x/identicon/svg?seed=" + nameField.getText(), 80, 80, true, true, true));
    }

    private void handleChooseAvatar() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Profile Image");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files (*.png, *.jpg, *.jpeg)", "*.png", "*.jpg", "*.jpeg"));
        File file = fc.showOpenDialog(primaryStage);
        if (file != null) {
            selectedAvatarPath = file.getAbsolutePath();
            updateAvatarDisplay(selectedAvatarPath);
        }
    }

    private void handleSaveProfile() {
        String name = nameField.getText();
        if (name == null || name.trim().isEmpty()) {
            name = "User";
        }
        String tag = taglineField.getText();
        if (tag == null || tag.trim().isEmpty()) {
            tag = "Personal";
        }

        try {
            registry.getProfileService().updateProfile(name.trim(), tag.trim(), selectedAvatarPath);
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Profile saved successfully!", ButtonType.OK);
            alert.initOwner(primaryStage);
            alert.showAndWait();
            if (profileUpdateNotifier != null) {
                profileUpdateNotifier.run();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
