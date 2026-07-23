package ee.matteus.pannukas.core.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AreaObject extends PlannerObject {
    public static final double DEFAULT_OPACITY = 0.35;

    private final List<Position> points = new ArrayList<>();
    private String colorHex;
    private double opacity;

    public AreaObject(String id, String name, Position position) {
        super(id, name, position);
        this.colorHex = "#f59e0b";
        this.opacity = DEFAULT_OPACITY;
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

    @Override
    public void moveTo(Position position) {
        if (locked()) {
            return;
        }
        double deltaX = position.x() - position().x();
        double deltaY = position.y() - position().y();
        super.moveTo(position);
        for (int index = 0; index < points.size(); index++) {
            Position point = points.get(index);
            points.set(index, new Position(point.x() + deltaX, point.y() + deltaY));
        }
    }

    public String colorHex() {
        return colorHex;
    }

    public void setColorHex(String colorHex) {
        this.colorHex = colorHex == null || colorHex.isBlank() ? "#f59e0b" : colorHex;
    }

    public double opacity() {
        return opacity;
    }

    public void setOpacity(double opacity) {
        this.opacity = Math.max(0.0, Math.min(1.0, opacity));
    }
}
