package ee.matteus.pannukas.core.model;

public class PowerOutlet {
    private final String id;
    private String name;
    private ConnectorType type;
    private int capacityWatts;

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

    public void rename(String name) {
        this.name = name == null ? "" : name.trim();
    }

    public ConnectorType type() {
        return type;
    }

    public void setType(ConnectorType type) {
        this.type = type == null ? ConnectorType.SCHUKO_230V : type;
    }

    public int capacityWatts() {
        return capacityWatts;
    }

    public void setCapacityWatts(int capacityWatts) {
        if (capacityWatts <= 0) {
            throw new IllegalArgumentException("Väljundi võimsus peab olema positiivne.");
        }
        this.capacityWatts = capacityWatts;
    }
}
