package ee.matteus.pannukas.core.model;

public class PowerOutlet {
    private final String id;
    private final String name;
    private final ConnectorType type;
    private final int capacityWatts;

    public PowerOutlet(String id, ConnectorType type, int capacityWatts) {
        this(id, "", type, capacityWatts);
    }

    public PowerOutlet(String id, String name, ConnectorType type, int capacityWatts) {
        this.id = id;
        this.name = name == null ? "" : name.trim();
        this.type = type;
        this.capacityWatts = capacityWatts;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public ConnectorType type() {
        return type;
    }

    public int capacityWatts() {
        return capacityWatts;
    }
}
