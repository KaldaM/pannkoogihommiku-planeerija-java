package ee.matteus.pannukas.core.model;

public class TextObject extends PlannerObject {
    private String colorHex;

    public TextObject(String id, String name, Position position) {
        super(id, name, position);
        this.colorHex = "#111827";
    }

    public String colorHex() {
        return colorHex;
    }

    public void setColorHex(String colorHex) {
        this.colorHex = colorHex == null || colorHex.isBlank() ? "#111827" : colorHex;
    }
}
