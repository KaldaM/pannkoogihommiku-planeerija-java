package ee.matteus.plaanisepp.core.model;

import java.util.UUID;

public class Equipment {
    private final String id;
    private String name;
    private int requiredWatts;
    private String powerConnectionId;

    public Equipment(String name, int requiredWatts) {
        this(UUID.randomUUID().toString(), name, requiredWatts);
    }

    public Equipment(String id, String name, int requiredWatts) {
        this(id, name, requiredWatts, "");
    }

    public Equipment(String id, String name, int requiredWatts, String powerConnectionId) {
        this.id = id == null || id.isBlank() ? UUID.randomUUID().toString() : id;
        this.name = name;
        setRequiredWatts(requiredWatts);
        assignPowerConnection(powerConnectionId);
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public void rename(String name) {
        this.name = name;
    }

    public int requiredWatts() {
        return requiredWatts;
    }

    public void setRequiredWatts(int requiredWatts) {
        if (requiredWatts < 0) {
            throw new IllegalArgumentException("Vooluvajadus ei saa olla negatiivne.");
        }
        this.requiredWatts = requiredWatts;
    }

    public String powerConnectionId() {
        return powerConnectionId;
    }

    public boolean usesDefaultPower() {
        return powerConnectionId.isEmpty();
    }

    public void assignPowerConnection(String powerConnectionId) {
        this.powerConnectionId = powerConnectionId == null ? "" : powerConnectionId.trim();
    }

    public void useDefaultPower() {
        powerConnectionId = "";
    }
}
