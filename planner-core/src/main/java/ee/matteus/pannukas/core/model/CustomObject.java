package ee.matteus.pannukas.core.model;

public class CustomObject extends PlannerObject {
    private CustomObjectShape shape;
    private String colorHex;

    public CustomObject(String id, String name, Position position) {
        super(id, name, position);
        this.shape = CustomObjectShape.SQUARE;
        this.colorHex = "#9ca3af";
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
}
