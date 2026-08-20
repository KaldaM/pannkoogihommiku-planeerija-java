package ee.matteus.plaanisepp.core.model;

import ee.matteus.plaanisepp.core.service.PowerSummary;
import ee.matteus.plaanisepp.core.service.PowerSummaryService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventPlanPowerConnectionTest {
    @Test
    void connectsTentAreaAndLineAndIncludesTheirEquipmentInSummary() {
        EventPlan plan = new EventPlan("Test");
        PowerSource source = powerSource();
        Tent tent = new Tent("tent", "Telk", new Position(0, 0));
        tent.addEquipment(new Equipment("Pliit", 1200));
        AreaObject area = new AreaObject("area", "Lava", new Position(10, 10));
        area.addEquipment(new Equipment("Valgusti", 500));
        LineObject line = new LineObject("line", "Valguskett", new Position(20, 20));
        line.addEquipment(new Equipment("Lambid", 300));
        plan.addObject(source);
        plan.addObject(tent);
        plan.addObject(area);
        plan.addObject(line);

        assertTrue(plan.connectToPower(source.id(), tent.id(), ConnectorType.SCHUKO_230V, "outlet").isPresent());
        assertTrue(plan.connectToPower(source.id(), area.id(), ConnectorType.SCHUKO_230V, "outlet").isPresent());
        assertTrue(plan.connectToPower(source.id(), line.id(), ConnectorType.SCHUKO_230V, "outlet").isPresent());

        assertEquals(3, plan.powerConsumers().size());
        assertEquals(3, plan.powerConnections().size());
        PowerSummary summary = new PowerSummaryService().summaries(plan).getFirst();
        assertEquals(2000, summary.usedWatts());
        assertEquals(9000, summary.remainingWatts());
    }

    @Test
    void refusesConnectionForObjectThatIsNotPowerConsumer() {
        EventPlan plan = new EventPlan("Test");
        PowerSource source = powerSource();
        CustomObject object = new CustomObject("object", "Objekt", new Position(0, 0));
        plan.addObject(source);
        plan.addObject(object);

        assertTrue(plan.connectToPower(
                source.id(),
                object.id(),
                ConnectorType.SCHUKO_230V,
                "outlet"
        ).isEmpty());
        assertTrue(plan.powerConnections().isEmpty());
    }

    private PowerSource powerSource() {
        PowerSource source = new PowerSource("source", "Kapp", new Position(50, 50));
        source.addOutlet(new PowerOutlet("outlet", ConnectorType.SCHUKO_230V, 11000));
        return source;
    }
}
