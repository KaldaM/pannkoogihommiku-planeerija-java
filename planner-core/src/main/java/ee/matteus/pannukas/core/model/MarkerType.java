package ee.matteus.pannukas.core.model;

public enum MarkerType {
    WC("WC", "#2563eb"),
    SECURITY("Turva", "#dc2626"),
    INFO("Info", "#0ea5e9"),
    START_FINISH("Start/finish", "#16a34a"),
    SAUNA("Saun/tünnisaun", "#ea580c"),
    MEMBER("Liige", "#7c3aed");

    private final String displayName;
    private final String defaultColorHex;

    MarkerType(String displayName, String defaultColorHex) {
        this.displayName = displayName;
        this.defaultColorHex = defaultColorHex;
    }

    public String displayName() {
        return displayName;
    }

    public String defaultColorHex() {
        return defaultColorHex;
    }
}
