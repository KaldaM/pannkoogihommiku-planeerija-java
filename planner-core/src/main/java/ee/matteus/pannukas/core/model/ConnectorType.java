package ee.matteus.pannukas.core.model;

public enum ConnectorType {
    SCHUKO_230V("230V tavapesa", 3500),
    INDUSTRIAL_16A("16A tööstusvool", 11000),
    INDUSTRIAL_32A("32A tööstusvool", 22000);

    private final String displayName;
    private final int defaultCapacityWatts;

    ConnectorType(String displayName, int defaultCapacityWatts) {
        this.displayName = displayName;
        this.defaultCapacityWatts = defaultCapacityWatts;
    }

    public String displayName() {
        return displayName;
    }

    public int defaultCapacityWatts() {
        return defaultCapacityWatts;
    }
}
