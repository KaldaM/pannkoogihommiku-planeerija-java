package ee.matteus.pannukas.core.model;

import java.util.UUID;

public class Equipment {
    private final String id;
    private String name;
    private int requiredWatts;

    public Equipment(String name, int requiredWatts) {
        this(UUID.randomUUID().toString(), name, requiredWatts);
    }

    public Equipment(String id, String name, int requiredWatts) {
        this.id = id == null || id.isBlank() ? UUID.randomUUID().toString() : id;
        this.name = name;
        setRequiredWatts(requiredWatts);
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
}
