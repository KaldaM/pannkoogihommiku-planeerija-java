package ee.matteus.plaanisepp.core.service;

import ee.matteus.plaanisepp.core.model.EventPlan;
import ee.matteus.plaanisepp.core.model.PowerSource;

import java.util.List;

public class PowerSummaryService {
    public List<PowerSummary> summaries(EventPlan plan) {
        return plan.powerSources().stream()
                .map(source -> new PowerSummary(source.id(), source.name(), source.totalCapacityWatts(), usedWatts(plan, source)))
                .toList();
    }

    private int usedWatts(EventPlan plan, PowerSource source) {
        return plan.powerConnections().stream()
                .filter(connection -> connection.sourceId().equals(source.id()))
                .mapToInt(plan::powerDemandWatts)
                .sum();
    }
}
