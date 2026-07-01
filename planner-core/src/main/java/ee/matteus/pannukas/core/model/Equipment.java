package ee.matteus.pannukas.core.model;

public class Equipment {
    private String name;
    private int requiredWatts;

    public Equipment(String name, int requiredWatts) {
        this.name = name;
        this.requiredWatts = requiredWatts;
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
