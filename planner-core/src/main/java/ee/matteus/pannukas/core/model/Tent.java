package ee.matteus.pannukas.core.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Tent extends PlannerObject implements EquipmentContainer {
    public static final double DEFAULT_OPACITY = 1.0;

    private double widthMeters;
    private double heightMeters;
    private double rotationDegrees;
    private String colorHex;
    private double opacity;
    private Position powerConnectionOffset = new Position(0, 0);
    private final List<Equipment> equipment = new ArrayList<>();

    public Tent(String id, String name, Position position) {
        super(id, name, position);
        this.widthMeters = 3.0;
        this.heightMeters = 3.0;
        this.colorHex = "#e74c3c";
        this.opacity = DEFAULT_OPACITY;
    }

    public double widthMeters() {
        return widthMeters;
    }

    public void setSizeMeters(double widthMeters, double heightMeters) {
        if (widthMeters <= 0 || heightMeters <= 0) {
            throw new IllegalArgumentException("Telgi mõõdud peavad olema positiivsed.");
        }
        this.widthMeters = widthMeters;
        this.heightMeters = heightMeters;
    }

    public double heightMeters() {
        return heightMeters;
    }

    public double rotationDegrees() {
        return rotationDegrees;
    }

    public void setRotationDegrees(double rotationDegrees) {
        this.rotationDegrees = rotationDegrees;
    }

    public String colorHex() {
        return colorHex;
    }

    public void setColorHex(String colorHex) {
        this.colorHex = colorHex;
    }

    public double opacity() {
        return opacity;
    }

    public void setOpacity(double opacity) {
        this.opacity = Math.max(0.0, Math.min(1.0, opacity));
    }

    @Override
    public Position powerConnectionOffset() {
        return powerConnectionOffset;
    }

    @Override
    public void setPowerConnectionOffset(Position offset) {
        powerConnectionOffset = offset == null ? new Position(0, 0) : offset;
    }

    @Override
    public List<Equipment> equipment() {
        return Collections.unmodifiableList(equipment);
    }

    @Override
    public void addEquipment(Equipment item) {
        equipment.add(item);
    }

    @Override
    public void removeEquipment(int index) {
        equipment.remove(index);
    }
}
