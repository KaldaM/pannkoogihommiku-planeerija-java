package ee.matteus.pannukas.core.service;

public record PowerSummary(String sourceId, String sourceName, int capacityWatts, int usedWatts) {
    public int remainingWatts() {
        return capacityWatts - usedWatts;
    }
}
