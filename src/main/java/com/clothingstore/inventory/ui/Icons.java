package com.clothingstore.inventory.ui;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Labeled;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.shape.SVGPath;

import java.util.Map;

/**
 * Small line-icon set drawn as vector paths on a 24x24 grid.
 * <p>
 * Replaces the Unicode emoji the KPI cards used before: JavaFX can't render
 * colour emoji reliably (on Windows and Linux they show as boxes, blanks or
 * monochrome glyphs), whereas these paths render the same everywhere and
 * need no extra dependency or icon font.
 */
public final class Icons {

    private Icons() {}

    private static final Map<String, String> PATHS = Map.ofEntries(
            Map.entry("dashboard", "M3 3h7v7H3z M14 3h7v7h-7z M14 14h7v7h-7z M3 14h7v7H3z"),
            Map.entry("box", "M21 8l-9-5-9 5 9 5 9-5z M3 8v8l9 5 9-5V8 M12 13v8"),
            Map.entry("cart", "M3 4h2l2.4 11h11.2L21 7H6.2 M10 20a1 1 0 1 1-2 0a1 1 0 1 1 2 0z M19 20a1 1 0 1 1-2 0a1 1 0 1 1 2 0z"),
            Map.entry("receipt", "M5 3h14v18l-3-2-2 2-2-2-2 2-2-2-3 2z M9 8h6 M9 12h6 M9 16h3"),
            Map.entry("chart", "M4 20V11 M10 20V5 M16 20v-6 M21 20H3"),
            Map.entry("alert", "M12 3.5L2.5 20h19L12 3.5z M12 10v4 M12 17.2v0.1"),
            Map.entry("x-circle", "M21 12a9 9 0 1 1-18 0a9 9 0 1 1 18 0z M9 9l6 6 M15 9l-6 6"),
            Map.entry("check-circle", "M21 12a9 9 0 1 1-18 0a9 9 0 1 1 18 0z M8 12.5l2.5 2.5L16 9.5"),
            Map.entry("tag", "M3 12V3h9l9 9-9 9z M7.5 7.5h0.1"),
            Map.entry("calendar", "M4 5h16v16H4z M4 10h16 M8 3v4 M16 3v4"),
            Map.entry("trend", "M3 17l6-6 4 4 8-8 M15 7h6v6"),
            Map.entry("coins", "M21 12a9 9 0 1 1-18 0a9 9 0 1 1 18 0z M12 7v10 M15 9.5c0-1.4-1.3-2-3-2s-3 .8-3 2 1.3 1.8 3 2.2 3 1 3 2.3-1.3 2-3 2-3-.7-3-2"),
            Map.entry("average", "M21 12a9 9 0 1 1-18 0a9 9 0 1 1 18 0z M5.6 18.4L18.4 5.6"),
            Map.entry("plus", "M12 5v14 M5 12h14"),
            Map.entry("edit", "M4 20h4L19 9l-4-4L4 16v4z M14 6l4 4"),
            Map.entry("trash", "M4 7h16 M9 7V4h6v3 M6 7l1 13h10l1-13 M10 11v5 M14 11v5"),
            Map.entry("restock", "M12 19V6 M6 11l6-6 6 6 M5 21h14"),
            Map.entry("folder", "M3 6h7l2 2h9v11H3z"),
            Map.entry("search", "M17 11a6 6 0 1 1-12 0a6 6 0 1 1 12 0z M20 20l-4.5-4.5"),
            Map.entry("logout", "M15 4h4v16h-4 M10 8l-4 4 4 4 M6 12h10"),
            Map.entry("key", "M10 14a4 4 0 1 1-1-5 M9 9l11 0 M17 9v3 M20 9v4"),
            Map.entry("image", "M4 5h16v14H4z M4 16l5-5 4 4 3-3 4 4 M15.5 9h0.1"),
            Map.entry("chevron-left", "M15 6l-6 6 6 6"),
            Map.entry("chevron-right", "M9 6l6 6-6 6"),
            Map.entry("user", "M16 8a4 4 0 1 1-8 0a4 4 0 1 1 8 0z M4 21c0-4 3.6-6 8-6s8 2 8 6"),
            Map.entry("ban", "M21 12a9 9 0 1 1-18 0a9 9 0 1 1 18 0z M5.6 5.6l12.8 12.8"),
            Map.entry("shirt", "M8 3L3 6l2 5 3-1v11h8V10l3 1 2-5-5-3c0 2-1.6 3-4 3S8 5 8 3z"),
            Map.entry("inbox", "M3 13l3-8h12l3 8v6H3z M3 13h5l1 2h6l1-2h5")
    );

    /** An icon at the given pixel size, drawn in the given colour. */
    public static Node of(String name, double size, Paint color) {
        SVGPath path = path(name);
        path.setStroke(color);
        return wrap(path, size);
    }

    /** An icon that follows a style class's -fx-stroke (set in styles.css). */
    public static Node styled(String name, double size, String styleClass) {
        SVGPath path = path(name);
        path.getStyleClass().addAll("icon", styleClass);
        return wrap(path, size);
    }

    /**
     * Puts an icon on a button/label whose colour always matches the control's
     * text colour, including hover/pressed/disabled states set by the theme.
     */
    public static <T extends Labeled> T on(T control, String name, double size) {
        SVGPath path = path(name);
        path.strokeProperty().bind(control.textFillProperty());
        control.setGraphic(wrap(path, size));
        control.setGraphicTextGap(8);
        return control;
    }

    private static SVGPath path(String name) {
        String data = PATHS.get(name);
        if (data == null) throw new IllegalArgumentException("Unknown icon: " + name);
        SVGPath path = new SVGPath();
        path.setContent(data);
        path.setFill(Color.TRANSPARENT);
        path.setStroke(Color.BLACK);
        path.setStrokeWidth(2);
        path.setStrokeLineCap(StrokeLineCap.ROUND);
        path.setStrokeLineJoin(StrokeLineJoin.ROUND);
        return path;
    }

    private static Node wrap(SVGPath path, double size) {
        double scale = size / 24.0;
        // An invisible 24x24 frame keeps every icon the same footprint, however
        // much of the grid its strokes actually cover.
        javafx.scene.shape.Rectangle frame = new javafx.scene.shape.Rectangle(0, 0, 24, 24);
        frame.setFill(Color.TRANSPARENT);
        Group inner = new Group(frame, path);
        inner.setScaleX(scale);
        inner.setScaleY(scale);
        // The outer Group reports the scaled bounds, so layout reserves size x size.
        Group g = new Group(inner);
        g.setMouseTransparent(true);
        g.setFocusTraversable(false);
        return g;
    }
}
