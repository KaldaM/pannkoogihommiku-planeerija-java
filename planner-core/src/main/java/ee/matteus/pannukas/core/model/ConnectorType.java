package ee.matteus.pannukas.core.model;

public enum ConnectorType {
    SCHUKO_230V("230V tavapesa"),
    INDUSTRIAL_16A("16A tööstusvool"),
    INDUSTRIAL_32A("32A tööstusvool");

    private final String displayName;

    ConnectorType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
