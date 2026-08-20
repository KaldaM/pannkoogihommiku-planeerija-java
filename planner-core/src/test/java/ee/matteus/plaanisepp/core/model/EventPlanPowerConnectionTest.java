package ee.matteus.plaanisepp.core.model;

import ee.matteus.plaanisepp.core.service.PowerSummary;
import ee.matteus.plaanisepp.core.service.PowerSummaryService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    @Test
    void refusesAlternativeConnectionWithoutDefaultPower() {
        EventPlan plan = new EventPlan("Test");
        PowerSource source = powerSource();
        Tent tent = new Tent("tent", "Telk", new Position(0, 0));
        tent.addEquipment(new Equipment("Pliit", 1200));
        plan.addObject(source);
        plan.addObject(tent);

        assertTrue(plan.addAlternativePowerConnection(
                source.id(), tent.id(), ConnectorType.SCHUKO_230V, "outlet"
        ).isEmpty());
    }

    @Test
    void keepsConnectionIdentifierWhenReconnectingConsumer() {
        EventPlan plan = new EventPlan("Test");
        PowerSource source = powerSource();
        Tent tent = new Tent("tent", "Telk", new Position(0, 0));
        tent.addEquipment(new Equipment("Pliit", 1200));
        plan.addObject(source);
        plan.addObject(tent);

        PowerConnection initialConnection = plan.connectToPower(
                source.id(), tent.id(), ConnectorType.SCHUKO_230V, "outlet"
        ).orElseThrow();
        PowerConnection reconnected = plan.connectToPower(
                source.id(), tent.id(), ConnectorType.SCHUKO_230V, "outlet"
        ).orElseThrow();

        assertFalse(initialConnection.id().isBlank());
        assertEquals(initialConnection.id(), reconnected.id());
    }

    @Test
    void keepsConnectionIdentifierWhenEditingCable() {
        EventPlan plan = new EventPlan("Test");
        PowerSource source = powerSource();
        Tent tent = new Tent("tent", "Telk", new Position(0, 0));
        tent.addEquipment(new Equipment("Pliit", 1200));
        plan.addObject(source);
        plan.addObject(tent);
        String connectionId = plan.connectToPower(
                source.id(), tent.id(), ConnectorType.SCHUKO_230V, "outlet"
        ).orElseThrow().id();

        plan.updateCableNotes(tent.id(), "Kaabel A");
        plan.updateCableLengthNotes(tent.id(), "25 m");
        plan.addCableRoutePoint(tent.id(), new Position(10, 20));
        plan.updateCableLabelOffset(tent.id(), new Position(5, 6));
        plan.resetCableLabelOffset(tent.id());

        assertEquals(connectionId, plan.findPowerConnectionForConsumer(tent.id()).orElseThrow().id());
    }

    @Test
    void assignsEquipmentOnlyToConnectionForItsOwnContainer() {
        EventPlan plan = new EventPlan("Test");
        PowerSource source = powerSource();
        Tent firstTent = new Tent("tent-1", "Esimene telk", new Position(0, 0));
        Equipment equipment = new Equipment("equipment-1", "Pliit", 1200);
        firstTent.addEquipment(equipment);
        Tent secondTent = new Tent("tent-2", "Teine telk", new Position(10, 10));
        secondTent.addEquipment(new Equipment("equipment-2", "Soojendi", 1000));
        plan.addObject(source);
        plan.addObject(firstTent);
        plan.addObject(secondTent);
        String firstConnectionId = plan.connectToPower(
                source.id(), firstTent.id(), ConnectorType.SCHUKO_230V, "outlet"
        ).orElseThrow().id();
        String secondConnectionId = plan.connectToPower(
                source.id(), secondTent.id(), ConnectorType.SCHUKO_230V, "outlet"
        ).orElseThrow().id();

        assertEquals(
                EquipmentPowerAssignmentResult.CONNECTION_BELONGS_TO_ANOTHER_CONSUMER,
                plan.assignEquipmentToPowerConnection(firstTent.id(), equipment.id(), secondConnectionId)
        );
        assertTrue(equipment.usesDefaultPower());

        assertEquals(
                EquipmentPowerAssignmentResult.SUCCESS,
                plan.assignEquipmentToPowerConnection(firstTent.id(), equipment.id(), firstConnectionId)
        );
        assertEquals(firstConnectionId, equipment.powerConnectionId());
    }

    @Test
    void clearsEquipmentOverrideWhenReferencedConnectionIsRemoved() {
        EventPlan plan = new EventPlan("Test");
        PowerSource source = powerSource();
        Tent tent = new Tent("tent", "Telk", new Position(0, 0));
        Equipment equipment = new Equipment("equipment", "Pliit", 1200);
        tent.addEquipment(equipment);
        plan.addObject(source);
        plan.addObject(tent);
        String connectionId = plan.connectToPower(
                source.id(), tent.id(), ConnectorType.SCHUKO_230V, "outlet"
        ).orElseThrow().id();
        plan.assignEquipmentToPowerConnection(tent.id(), equipment.id(), connectionId);

        assertTrue(plan.disconnectPowerConnection(connectionId));

        assertTrue(equipment.usesDefaultPower());
    }

    @Test
    void distributesEquipmentDemandBetweenDefaultAndAlternativeConnections() {
        EventPlan plan = new EventPlan("Test");
        PowerSource defaultSource = powerSource();
        PowerSource alternativeSource = new PowerSource("source-2", "Teine kapp", new Position(100, 100));
        alternativeSource.addOutlet(new PowerOutlet("outlet-2", ConnectorType.SCHUKO_230V, 11000));
        Tent tent = new Tent("tent", "Telk", new Position(0, 0));
        Equipment defaultEquipment = new Equipment("equipment-1", "Pliit", 1200);
        Equipment alternativeEquipment = new Equipment("equipment-2", "Külmik", 500);
        tent.addEquipment(defaultEquipment);
        tent.addEquipment(alternativeEquipment);
        plan.addObject(defaultSource);
        plan.addObject(alternativeSource);
        plan.addObject(tent);
        PowerConnection defaultConnection = plan.connectToPower(
                defaultSource.id(), tent.id(), ConnectorType.SCHUKO_230V, "outlet"
        ).orElseThrow();
        PowerConnection alternativeConnection = plan.addAlternativePowerConnection(
                alternativeSource.id(), tent.id(), ConnectorType.SCHUKO_230V, "outlet-2"
        ).orElseThrow();

        assertTrue(defaultConnection.defaultForConsumer());
        assertFalse(alternativeConnection.defaultForConsumer());
        assertEquals(2, plan.findPowerConnectionsForConsumer(tent.id()).size());

        plan.assignEquipmentToPowerConnection(tent.id(), alternativeEquipment.id(), alternativeConnection.id());

        assertEquals(1200, plan.powerDemandWatts(defaultConnection));
        assertEquals(500, plan.powerDemandWatts(alternativeConnection));
        assertEquals(1200, new PowerSummaryService().summaries(plan).get(0).usedWatts());
        assertEquals(500, new PowerSummaryService().summaries(plan).get(1).usedWatts());
    }

    @Test
    void reconnectingDefaultPowerKeepsAlternativeConnection() {
        EventPlan plan = new EventPlan("Test");
        PowerSource source = powerSource();
        Tent tent = new Tent("tent", "Telk", new Position(0, 0));
        tent.addEquipment(new Equipment("Pliit", 1200));
        plan.addObject(source);
        plan.addObject(tent);
        PowerConnection defaultConnection = plan.connectToPower(
                source.id(), tent.id(), ConnectorType.SCHUKO_230V, "outlet"
        ).orElseThrow();
        PowerConnection alternativeConnection = plan.addAlternativePowerConnection(
                source.id(), tent.id(), ConnectorType.SCHUKO_230V, "outlet"
        ).orElseThrow();

        PowerConnection reconnected = plan.connectToPower(
                source.id(), tent.id(), ConnectorType.SCHUKO_230V, "outlet"
        ).orElseThrow();
        plan.updateCableNotes(tent.id(), "Vaiketoite kaabel");

        assertEquals(defaultConnection.id(), reconnected.id());
        assertEquals(2, plan.findPowerConnectionsForConsumer(tent.id()).size());
        assertTrue(plan.powerConnections().stream().anyMatch(connection ->
                connection.id().equals(alternativeConnection.id())));
        assertEquals(
                "Vaiketoite kaabel",
                plan.findPowerConnectionForConsumer(tent.id()).orElseThrow().cableNotes()
        );
    }

    @Test
    void clearsInvalidLoadedEquipmentAssignment() {
        EventPlan plan = new EventPlan("Test");
        Tent tent = new Tent("tent", "Telk", new Position(0, 0));
        Equipment equipment = new Equipment("equipment", "Pliit", 1200, "missing-connection");
        tent.addEquipment(equipment);
        plan.addObject(tent);

        assertEquals(1, plan.clearInvalidEquipmentPowerAssignments());
        assertTrue(equipment.usesDefaultPower());
    }

    private PowerSource powerSource() {
        PowerSource source = new PowerSource("source", "Kapp", new Position(50, 50));
        source.addOutlet(new PowerOutlet("outlet", ConnectorType.SCHUKO_230V, 11000));
        return source;
    }
}
