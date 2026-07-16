package ee.matteus.pannukas.core.model;

public enum CustomObjectShape {
    SQUARE("Ruut"),
    CIRCLE("Ring"),
    TEXT("Tekst");

    private final String displayName;

    CustomObjectShape(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
