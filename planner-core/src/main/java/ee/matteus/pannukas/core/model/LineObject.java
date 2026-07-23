package ee.matteus.pannukas.core.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LineObject extends PlannerObject {
    public static final double DEFAULT_WIDTH_PIXELS = 3.0;

    private final List<Position> points = new ArrayList<>();
    private String colorHex;
    private double widthPixels;

    public LineObject(String id, String name, Position position) {
        super(id, name, position);
        this.colorHex = "#0f766e";
        this.widthPixels = DEFAULT_WIDTH_PIXELS;
    }

    public List<Position> points() {
        return Collections.unmodifiableList(points);
    }

    public void setPoints(List<Position> points) {
        this.points.clear();
        if (points != null) {
            this.points.addAll(points.stream()
                    .filter(point -> point != null)
                    .toList());
        }
    }

    public String colorHex() {
        return colorHex;
    }

    public void setColorHex(String colorHex) {
        this.colorHex = colorHex == null || colorHex.isBlank() ? "#0f766e" : colorHex;
    }

    public double widthPixels() {
        return widthPixels;
    }

    public void setWidthPixels(double widthPixels) {
        if (widthPixels <= 0) {
            throw new IllegalArgumentException("Joone laius peab olema positiivne.");
        }
        this.widthPixels = widthPixels;
    }
}
