package ee.matteus.pannukas.core.model;

public class MarkerObject extends PlannerObject {
    private MarkerType markerType;
    private String colorHex;

    public MarkerObject(String id, String name, Position position) {
        super(id, name, position);
        this.markerType = MarkerType.WC;
        this.colorHex = "#0f766e";
    }

    public MarkerType markerType() {
        return markerType;
    }

    public void setMarkerType(MarkerType markerType) {
        this.markerType = markerType == null ? MarkerType.WC : markerType;
    }

    public String colorHex() {
        return colorHex;
    }

    public void setColorHex(String colorHex) {
        this.colorHex = colorHex == null || colorHex.isBlank() ? "#0f766e" : colorHex;
    }
}
