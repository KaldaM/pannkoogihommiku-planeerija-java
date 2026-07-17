package ee.matteus.pannukas.core.model;

public enum MarkerType {
    WC("WC"),
    SECURITY("Turva"),
    INFO("Info"),
    START_FINISH("Start/finish"),
    SAUNA("Saun/tünnisaun"),
    MEMBER("Liige");

    private final String displayName;

    MarkerType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
