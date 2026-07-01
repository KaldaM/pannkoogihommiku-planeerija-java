package ee.matteus.pannukas.core.service;

import ee.matteus.pannukas.core.model.EventPlan;
import ee.matteus.pannukas.core.model.PowerConsumer;
import ee.matteus.pannukas.core.model.PowerSource;

import java.util.List;

public class PowerSummaryService {
    public List<PowerSummary> summaries(EventPlan plan) {
        return plan.powerSources().stream()
                .map(source -> new PowerSummary(source.name(), source.totalCapacityWatts(), usedWatts(plan, source)))
                .toList();
    }

    private int usedWatts(EventPlan plan, PowerSource source) {
        return plan.powerConnections().stream()
                .filter(connection -> connection.sourceId().equals(source.id()))
                .map(connection -> plan.findObject(connection.consumerId()))
                .flatMap(Optional -> Optional.stream())
                .filter(PowerConsumer.class::isInstance)
                .map(PowerConsumer.class::cast)
                .mapToInt(PowerConsumer::requiredWatts)
                .sum();
    }
}
