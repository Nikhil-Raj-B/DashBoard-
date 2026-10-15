package com.everyday.ui.component;

import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;

/**
 * Provides clean, crisp vector SVG icons for all dashboard actions and navigation.
 */
public class Icons {

    public static Node getIcon(String name, double size, Color color) {
        SVGPath path = new SVGPath();
        path.setContent(getPathContent(name));
        path.setFill(color != null ? color : Color.web("#94A3B8"));

        // Normalizing bounds
        double scale = size / 24.0;
        path.setScaleX(scale);
        path.setScaleY(scale);
        return path;
    }

    public static Node getIcon(String name, double size, String hexColor) {
        return getIcon(name, size, Color.web(hexColor));
    }

    public static Node getIcon(String name) {
        return getIcon(name, 18, Color.web("#94A3B8"));
    }

    private static String getPathContent(String name) {
        if (name == null) return getPathContent("circle");
        switch (name.toLowerCase()) {
            case "dashboard":
            case "home":
                return "M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z M9 22V12h6v10";
            case "tasks":
            case "check-square":
                return "M9 11l3 3L22 4 M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11";
            case "projects":
            case "folder":
                return "M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z";
            case "calendar":
                return "M19 4H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2z M16 2v4 M8 2v4 M3 10h18";
            case "analytics":
            case "chart":
                return "M18 20V10 M12 20V4 M6 20v-6";
            case "streaks":
            case "flame":
            case "fire":
                return "M12 2c-.5 2.5-2.5 4-4 6-2 2.5-3 5.5-3 8a7 7 0 0 0 14 0c0-3.5-2-6-4-8-1-1-1.5-3.5-3-6z M12 18a3 3 0 0 1-3-3c0-1.5 1-2.5 3-4 2 1.5 3 2.5 3 4a3 3 0 0 1-3 3z";
            case "settings":
                return "M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z M12 8a4 4 0 1 0 0 8 4 4 0 0 0 0-8z";
            case "profile":
            case "user":
                return "M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2 M12 3a4 4 0 1 0 0 8 4 4 0 0 0 0-8z";
            case "plus":
                return "M12 5v14 M5 12h14";
            case "trash":
                return "M3 6h18 M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2";
            case "edit":
                return "M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7 M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z";
            case "check":
                return "M20 6L9 17l-5-5";
            case "clock":
                return "M12 2a10 10 0 1 0 10 10A10 10 0 0 0 12 2zm1 10.414V7a1 1 0 0 0-2 0v6a1 1 0 0 0 .293.707l3 3a1 1 0 0 0 1.414-1.414z";
            case "skip":
                return "M5 4l10 8-10 8V4z M19 5v14";
            case "award":
                return "M12 15l-4 6 1.5-8.5-5.5-5.5 8.5-1 3.5-8 3.5 8 8.5 1-5.5 5.5 1.5 8.5z";
            case "arrow-right":
                return "M5 12h14 M12 5l7 7-7 7";
            case "refresh":
                return "M23 4v6h-6 M1 20v-6h6 M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15";
            default:
                return "M12 2a10 10 0 1 0 10 10A10 10 0 0 0 12 2z";
        }
    }
}
