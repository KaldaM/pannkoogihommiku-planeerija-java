package ee.matteus.pannukas.core.model;

public class CustomObject extends PlannerObject {
    private CustomObjectShape shape;
    private String colorHex;
    private double widthMeters;
    private double heightMeters;
    private double rotationDegrees;

    public CustomObject(String id, String name, Position position) {
        super(id, name, position);
        this.shape = CustomObjectShape.SQUARE;
        this.colorHex = "#9ca3af";
        this.widthMeters = 1.0;
        this.heightMeters = 1.0;
        this.rotationDegrees = 0;
    }

    public CustomObjectShape shape() {
        return shape;
    }

    public void setShape(CustomObjectShape shape) {
        this.shape = shape == null ? CustomObjectShape.SQUARE : shape;
    }

    public String colorHex() {
        return colorHex;
    }

    public void setColorHex(String colorHex) {
        this.colorHex = colorHex == null || colorHex.isBlank() ? "#9ca3af" : colorHex;
    }

    public double widthMeters() {
        return widthMeters;
    }

    public double heightMeters() {
        return heightMeters;
    }

    public double rotationDegrees() {
        return rotationDegrees;
    }

    public void setSizeMeters(double widthMeters, double heightMeters) {
        if (widthMeters <= 0 || heightMeters <= 0) {
            throw new IllegalArgumentException("Objekti mõõdud peavad olema positiivsed.");
        }
        this.widthMeters = widthMeters;
        this.heightMeters = heightMeters;
    }

    public void setRotationDegrees(double rotationDegrees) {
        this.rotationDegrees = rotationDegrees;
    }
}
