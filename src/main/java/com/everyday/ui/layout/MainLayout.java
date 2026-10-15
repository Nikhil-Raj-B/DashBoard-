package com.everyday.ui.layout;

import com.everyday.model.UserProfile;
import com.everyday.service.ServiceRegistry;
import com.everyday.ui.component.Icons;
import com.everyday.ui.view.*;
import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class MainLayout extends BorderPane {

    private final ServiceRegistry registry;
    private final Stage primaryStage;

    private StackPane contentArea;
    private final Map<String, Button> navButtons = new HashMap<>();
    private String currentView = "Dashboard";

    // Bottom profile area widgets
    private Label profileNameLabel;
    private Label profileTagLabel;
    private ImageView profileAvatarView;

    // View caches
    private DashboardView dashboardView;
    private DailyTasksView dailyTasksView;
    private ProjectsView projectsView;
    private CalendarView calendarView;
    private AnalyticsView analyticsView;
    private StreaksView streaksView;
    private SettingsView settingsView;
    private ProfileView profileView;

    // Top toast container for in-app reminders
    private VBox toastContainer;

    public MainLayout(ServiceRegistry registry, Stage primaryStage) {
        this.registry = registry;
        this.primaryStage = primaryStage;

        this.getStyleClass().add("main-window");

        // 1. Sidebar on Left
        this.setLeft(buildSidebar());

        // 2. Central Content Area with Toast Stack
        contentArea = new StackPane();
        contentArea.getStyleClass().add("content-pane");

        toastContainer = new VBox(8);
        toastContainer.setAlignment(Pos.TOP_RIGHT);
        toastContainer.setPadding(new Insets(16, 24, 0, 0));
        toastContainer.setMouseTransparent(true);
        toastContainer.setPickOnBounds(false);

        StackPane centerStack = new StackPane(contentArea, toastContainer);
        this.setCenter(centerStack);

        // Setup notification callback
        registry.getNotificationService().setReminderCallback(this::showReminderToast);

        // Initialize views
        initViews();
        navigateTo("Dashboard");
    }

    private void initViews() {
        dashboardView = new DashboardView(registry, this::navigateTo);
        dailyTasksView = new DailyTasksView(registry, primaryStage);
        projectsView = new ProjectsView(registry, primaryStage);
        calendarView = new CalendarView(registry, primaryStage);
        analyticsView = new AnalyticsView(registry);
        streaksView = new StreaksView(registry, primaryStage);
        settingsView = new SettingsView(registry, primaryStage, this::updateProfileDisplay);
        profileView = new ProfileView(registry, primaryStage, this::updateProfileDisplay);
    }

    private Node buildSidebar() {
        VBox sidebar = new VBox(20);
        sidebar.setPrefWidth(240);
        sidebar.getStyleClass().add("sidebar");

        // Branding
        HBox brandBox = new HBox(8);
        brandBox.setAlignment(Pos.CENTER_LEFT);
        brandBox.setPadding(new Insets(4, 8, 12, 8));

        Label brandIcon = new Label();
        brandIcon.setGraphic(Icons.getIcon("flame", 22, "#3B82F6"));

        Label brandText = new Label("EveryDay");
        brandText.getStyleClass().add("brand-logo-text");

        brandBox.getChildren().addAll(brandIcon, brandText);

        // Navigation Menu
        VBox menuBox = new VBox(6);
        menuBox.getChildren().addAll(
                createNavButton("Dashboard", "home"),
                createNavButton("Daily Tasks", "check-square"),
                createNavButton("Projects", "folder"),
                createNavButton("Calendar", "calendar"),
                createNavButton("Analytics", "chart"),
                createNavButton("Streaks", "flame")
        );

        // Separator
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Secondary Settings / Profile links
        VBox secondaryBox = new VBox(6);
        secondaryBox.getChildren().addAll(
                createNavButton("Settings", "settings"),
                createNavButton("Profile", "user")
        );

        // Bottom-left profile card
        HBox profileCard = buildBottomProfileCard();

        sidebar.getChildren().addAll(brandBox, menuBox, spacer, secondaryBox, profileCard);
        return sidebar;
    }

    private Button createNavButton(String title, String iconName) {
        Button btn = new Button(title);
        btn.getStyleClass().add("nav-button");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setGraphic(Icons.getIcon(iconName, 18, "#94A3B8"));
        btn.setOnAction(e -> navigateTo(title));
        navButtons.put(title, btn);
        return btn;
    }

    private HBox buildBottomProfileCard() {
        HBox card = new HBox(12);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("sidebar-profile-card");

        profileAvatarView = new ImageView();
        profileAvatarView.setFitWidth(36);
        profileAvatarView.setFitHeight(36);
        Circle clip = new Circle(18, 18, 18);
        profileAvatarView.setClip(clip);
        profileAvatarView.getStyleClass().add("profile-avatar");

        VBox textBox = new VBox(2);
        profileNameLabel = new Label("Nikhil");
        profileNameLabel.getStyleClass().add("profile-name");

        profileTagLabel = new Label("Personal");
        profileTagLabel.getStyleClass().add("profile-tag");

        textBox.getChildren().addAll(profileNameLabel, profileTagLabel);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        card.getChildren().addAll(profileAvatarView, textBox);
        card.setOnMouseClicked(e -> navigateTo("Profile"));

        updateProfileDisplay();
        return card;
    }

    public void updateProfileDisplay() {
        try {
            UserProfile profile = registry.getProfileService().getProfile();
            profileNameLabel.setText(profile.getName());
            profileTagLabel.setText(profile.getTagline());

            if (profile.getAvatarPath() != null && new File(profile.getAvatarPath()).exists()) {
                try {
                    profileAvatarView.setImage(new Image(new File(profile.getAvatarPath()).toURI().toString()));
                    return;
                } catch (Exception ignored) {}
            }
            // Fallback default icon image
            profileAvatarView.setImage(new Image("https://api.dicebear.com/7.x/identicon/svg?seed=" + profile.getName(), 36, 36, true, true, true));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void navigateTo(String viewName) {
        this.currentView = viewName;

        // Update nav button active classes
        for (Map.Entry<String, Button> entry : navButtons.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(viewName)) {
                entry.getValue().getStyleClass().remove("nav-button-active");
                entry.getValue().getStyleClass().add("nav-button-active");
            } else {
                entry.getValue().getStyleClass().remove("nav-button-active");
            }
        }

        Node targetNode = null;
        switch (viewName) {
            case "Dashboard":
                dashboardView.refresh();
                targetNode = dashboardView;
                break;
            case "Daily Tasks":
                dailyTasksView.refresh();
                targetNode = dailyTasksView;
                break;
            case "Projects":
                projectsView.refresh();
                targetNode = projectsView;
                break;
            case "Calendar":
                calendarView.refresh();
                targetNode = calendarView;
                break;
            case "Analytics":
                analyticsView.refresh();
                targetNode = analyticsView;
                break;
            case "Streaks":
                streaksView.refresh();
                targetNode = streaksView;
                break;
            case "Settings":
                settingsView.refresh();
                targetNode = settingsView;
                break;
            case "Profile":
                profileView.refresh();
                targetNode = profileView;
                break;
            default:
                targetNode = dashboardView;
                break;
        }

        if (targetNode != null) {
            contentArea.getChildren().clear();
            contentArea.getChildren().add(targetNode);

            // Subtle smooth transition
            FadeTransition ft = new FadeTransition(Duration.millis(180), targetNode);
            ft.setFromValue(0.85);
            ft.setToValue(1.0);
            ft.play();
        }
    }

    public void showReminderToast(String message) {
        HBox toast = new HBox(10);
        toast.setAlignment(Pos.CENTER_LEFT);
        toast.setStyle("-fx-background-color: #1E293B; -fx-border-color: #3B82F6; -fx-border-width: 1.5px; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 12 16; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 10, 0, 0, 4);");

        Label bell = new Label("🔔");
        Label text = new Label(message);
        text.setStyle("-fx-text-fill: #F8FAFC; -fx-font-weight: 600;");

        toast.getChildren().addAll(bell, text);
        toastContainer.getChildren().add(toast);

        // Disappear after 5 seconds
        FadeTransition ft = new FadeTransition(Duration.millis(5000), toast);
        ft.setFromValue(1.0);
        ft.setToValue(0.0);
        ft.setOnFinished(e -> toastContainer.getChildren().remove(toast));
        ft.play();
    }
}
