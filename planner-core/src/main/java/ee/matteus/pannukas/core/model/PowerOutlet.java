package ee.matteus.pannukas.core.model;

public class PowerOutlet {
    private final String id;
    private final ConnectorType type;
    private final int capacityWatts;

    public PowerOutlet(String id, ConnectorType type, int capacityWatts) {
        this.id = id;
        this.type = type;
        this.capacityWatts = capacityWatts;
    }

    public String id() {
        return id;
    }

    public ConnectorType type() {
        return type;
    }

    public int capacityWatts() {
        return capacityWatts;
    }
}
