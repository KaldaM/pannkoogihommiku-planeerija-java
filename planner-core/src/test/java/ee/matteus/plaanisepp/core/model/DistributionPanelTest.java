package ee.matteus.plaanisepp.core.model;

import ee.matteus.plaanisepp.core.service.PowerSummary;
import ee.matteus.plaanisepp.core.service.PowerSummaryService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DistributionPanelTest {
    @Test
    void propagatesDownstreamDemandToUpstreamSource() {
        EventPlan plan = new EventPlan("Test");
        PowerSource mainSource = source("main", "Põhikilp", "main-outlet");
        DistributionPanel firstPanel = panel("panel-1", "Esimene alajaotuskilp", "panel-1-outlet");
        DistributionPanel secondPanel = panel("panel-2", "Teine alajaotuskilp", "panel-2-outlet");
        Tent tent = new Tent("tent", "Telk", new Position(20, 20));
        tent.addEquipment(new Equipment("Pliit", 1200));
        plan.addObject(mainSource);
        plan.addObject(firstPanel);
        plan.addObject(secondPanel);
        plan.addObject(tent);

        assertTrue(plan.connectToPower(
                mainSource.id(), firstPanel.id(), ConnectorType.SCHUKO_230V, "main-outlet"
        ).isPresent());
        assertTrue(plan.connectToPower(
                firstPanel.id(), secondPanel.id(), ConnectorType.SCHUKO_230V, "panel-1-outlet"
        ).isPresent());
        assertTrue(plan.connectToPower(
                secondPanel.id(), tent.id(), ConnectorType.SCHUKO_230V, "panel-2-outlet"
        ).isPresent());

        List<PowerSummary> summaries = new PowerSummaryService().summaries(plan);
        assertEquals(1200, summaryFor(summaries, mainSource.id()).usedWatts());
        assertEquals(1200, summaryFor(summaries, firstPanel.id()).usedWatts());
        assertEquals(1200, summaryFor(summaries, secondPanel.id()).usedWatts());
    }

    @Test
    void refusesSelfConnectionAndCycleBetweenDistributionPanels() {
        EventPlan plan = new EventPlan("Test");
        DistributionPanel first = panel("panel-1", "Esimene kilp", "outlet-1");
        DistributionPanel second = panel("panel-2", "Teine kilp", "outlet-2");
        plan.addObject(first);
        plan.addObject(second);

        assertTrue(plan.connectToPower(
                first.id(), first.id(), ConnectorType.SCHUKO_230V, "outlet-1"
        ).isEmpty());
        assertTrue(plan.connectToPower(
                first.id(), second.id(), ConnectorType.SCHUKO_230V, "outlet-1"
        ).isPresent());
        assertTrue(plan.connectToPower(
                second.id(), first.id(), ConnectorType.SCHUKO_230V, "outlet-2"
        ).isEmpty());
        assertEquals(1, plan.powerConnections().size());
    }

    private PowerSource source(String id, String name, String outletId) {
        PowerSource source = new PowerSource(id, name, new Position(0, 0));
        source.addOutlet(new PowerOutlet(outletId, ConnectorType.SCHUKO_230V, 11000));
        return source;
    }

    private DistributionPanel panel(String id, String name, String outletId) {
        DistributionPanel panel = new DistributionPanel(id, name, new Position(10, 10));
        panel.addOutlet(new PowerOutlet(outletId, ConnectorType.SCHUKO_230V, 11000));
        return panel;
    }

    private PowerSummary summaryFor(List<PowerSummary> summaries, String sourceId) {
        return summaries.stream()
                .filter(summary -> summary.sourceId().equals(sourceId))
                .findFirst()
                .orElseThrow();
    }
}
