package ee.matteus.pannukas.core.service;

import ee.matteus.pannukas.core.model.AreaObject;
import ee.matteus.pannukas.core.model.ConnectorType;
import ee.matteus.pannukas.core.model.CustomObject;
import ee.matteus.pannukas.core.model.Equipment;
import ee.matteus.pannukas.core.model.EventPlan;
import ee.matteus.pannukas.core.model.LineObject;
import ee.matteus.pannukas.core.model.Position;
import ee.matteus.pannukas.core.model.PowerOutlet;
import ee.matteus.pannukas.core.model.PowerSource;
import ee.matteus.pannukas.core.model.Tent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlanFileServiceTest {
    @TempDir
    Path tempDirectory;

    private final PlanFileService service = new PlanFileService();

    @Test
    void writesCurrentFormatVersionAndLoadsVersionedPlan() throws IOException {
        EventPlan plan = new EventPlan("Versiooniga plaan");
        Path file = tempDirectory.resolve("versioned-plan.pplan");

        service.save(plan, file);

        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(file)) {
            properties.load(input);
        }
        assertEquals(
                Integer.toString(PlanFileService.CURRENT_FORMAT_VERSION),
                properties.getProperty("formatVersion")
        );
        assertEquals("Versiooniga plaan", service.load(file).name());
    }

    @Test
    void loadsLegacyPlanWithoutFormatVersion() throws IOException {
        Path file = tempDirectory.resolve("unversioned-plan.pplan");
        Files.writeString(file, """
                format=pannukas-plan-v1
                plan.name=Versioonita plaan
                objects.count=0
                connections.count=0
                """);

        EventPlan loadedPlan = service.load(file);

        assertEquals("Versioonita plaan", loadedPlan.name());
    }

    @Test
    void rejectsPlanFromNewerFormatVersion() throws IOException {
        Path file = tempDirectory.resolve("future-plan.pplan");
        Files.writeString(file, """
                formatVersion=2
                plan.name=Tuleviku plaan
                objects.count=0
                connections.count=0
                """);

        IOException exception = assertThrows(IOException.class, () -> service.load(file));

        assertTrue(exception.getMessage().contains("uuema rakenduse versiooniga"));
        assertTrue(exception.getMessage().contains("uuenda rakendust"));
    }

    @Test
    void savesAndLoadsCustomObjectOpacity() throws IOException {
        EventPlan plan = new EventPlan("Test");
        CustomObject object = new CustomObject("object-1", "Objekt", new Position(10, 20));
        object.setOpacity(0.45);
        plan.addObject(object);
        Path file = tempDirectory.resolve("opacity.pplan");

        service.save(plan, file);
        EventPlan loadedPlan = service.load(file);

        CustomObject loadedObject = (CustomObject) loadedPlan.objects().getFirst();
        assertEquals(0.45, loadedObject.opacity(), 0.0001);
    }

    @Test
    void usesFullOpacityWhenOlderPlanHasNoOpacityValue() throws IOException {
        Path file = tempDirectory.resolve("old-plan.pplan");
        Files.writeString(file, """
                plan.name=Vana plaan
                objects.count=1
                object.0.type=CUSTOM_OBJECT
                object.0.id=object-1
                object.0.name=Objekt
                object.0.x=10
                object.0.y=20
                object.0.shape=SQUARE
                object.0.colorHex=#9ca3af
                object.0.widthMeters=2
                object.0.heightMeters=3
                object.0.rotationDegrees=0
                connections.count=0
                """);

        EventPlan loadedPlan = service.load(file);

        CustomObject loadedObject = (CustomObject) loadedPlan.objects().getFirst();
        assertEquals(CustomObject.DEFAULT_OPACITY, loadedObject.opacity(), 0.0001);
    }

    @Test
    void savesAndLoadsTentOpacity() throws IOException {
        EventPlan plan = new EventPlan("Test");
        Tent tent = new Tent("tent-1", "Telk", new Position(10, 20));
        tent.setOpacity(0.6);
        plan.addObject(tent);
        Path file = tempDirectory.resolve("tent-opacity.pplan");

        service.save(plan, file);
        EventPlan loadedPlan = service.load(file);

        Tent loadedTent = (Tent) loadedPlan.objects().getFirst();
        assertEquals(0.6, loadedTent.opacity(), 0.0001);
    }

    @Test
    void usesFullOpacityWhenOlderTentHasNoOpacityValue() throws IOException {
        Path file = tempDirectory.resolve("old-tent-plan.pplan");
        Files.writeString(file, """
                plan.name=Vana plaan
                objects.count=1
                object.0.type=TENT
                object.0.id=tent-1
                object.0.name=Telk
                object.0.x=10
                object.0.y=20
                object.0.widthMeters=3
                object.0.heightMeters=3
                object.0.rotationDegrees=0
                object.0.colorHex=#e74c3c
                object.0.equipment.count=0
                connections.count=0
                """);

        EventPlan loadedPlan = service.load(file);

        Tent loadedTent = (Tent) loadedPlan.objects().getFirst();
        assertEquals(Tent.DEFAULT_OPACITY, loadedTent.opacity(), 0.0001);
        assertEquals(new Position(0, 0), loadedTent.powerConnectionOffset());
    }

    @Test
    void savesAndLoadsAreaAndLineEquipment() throws IOException {
        EventPlan plan = new EventPlan("Test");
        AreaObject area = new AreaObject("area-1", "Lava", new Position(10, 20));
        area.addEquipment(new Equipment("Valgusti", 500));
        area.addEquipment(new Equipment("Soojendi", 1500));
        LineObject line = new LineObject("line-1", "Valguskett", new Position(30, 40));
        line.addEquipment(new Equipment("Lambid", 750));
        plan.addObject(area);
        plan.addObject(line);
        Path file = tempDirectory.resolve("area-line-equipment.pplan");

        service.save(plan, file);
        EventPlan loadedPlan = service.load(file);

        AreaObject loadedArea = (AreaObject) loadedPlan.objects().get(0);
        assertEquals(2, loadedArea.equipment().size());
        assertEquals("Valgusti", loadedArea.equipment().get(0).name());
        assertEquals(2000, loadedArea.requiredWatts());

        LineObject loadedLine = (LineObject) loadedPlan.objects().get(1);
        assertEquals(1, loadedLine.equipment().size());
        assertEquals("Lambid", loadedLine.equipment().getFirst().name());
        assertEquals(750, loadedLine.requiredWatts());
    }

    @Test
    void olderAreaAndLineWithoutEquipmentLoadWithEmptyLists() throws IOException {
        Path file = tempDirectory.resolve("old-area-line-plan.pplan");
        Files.writeString(file, """
                plan.name=Vana plaan
                objects.count=2
                object.0.type=AREA_OBJECT
                object.0.id=area-1
                object.0.name=Ala
                object.0.x=10
                object.0.y=20
                object.0.points.count=0
                object.1.type=LINE_OBJECT
                object.1.id=line-1
                object.1.name=Joon
                object.1.x=30
                object.1.y=40
                object.1.points.count=0
                connections.count=0
                """);

        EventPlan loadedPlan = service.load(file);

        AreaObject loadedArea = (AreaObject) loadedPlan.objects().get(0);
        LineObject loadedLine = (LineObject) loadedPlan.objects().get(1);
        assertEquals(0, loadedArea.equipment().size());
        assertEquals(0, loadedLine.equipment().size());
        assertEquals(0, loadedArea.requiredWatts());
        assertEquals(0, loadedLine.requiredWatts());
        assertEquals(new Position(0, 0), loadedArea.powerConnectionOffset());
        assertEquals(new Position(0, 0), loadedLine.powerConnectionOffset());
    }

    @Test
    void savesAndLoadsAreaAndLinePowerConnections() throws IOException {
        EventPlan plan = new EventPlan("Test");
        PowerSource source = new PowerSource("source", "Kapp", new Position(50, 50));
        source.addOutlet(new PowerOutlet("outlet", ConnectorType.SCHUKO_230V, 11000));
        AreaObject area = new AreaObject("area", "Lava", new Position(10, 20));
        area.addEquipment(new Equipment("Valgusti", 500));
        LineObject line = new LineObject("line", "Valguskett", new Position(30, 40));
        line.addEquipment(new Equipment("Lambid", 300));
        plan.addObject(source);
        plan.addObject(area);
        plan.addObject(line);
        plan.connectToPower(source.id(), area.id(), ConnectorType.SCHUKO_230V, "outlet");
        plan.connectToPower(source.id(), line.id(), ConnectorType.SCHUKO_230V, "outlet");
        Path file = tempDirectory.resolve("area-line-connections.pplan");

        service.save(plan, file);
        EventPlan loadedPlan = service.load(file);

        assertEquals(2, loadedPlan.powerConnections().size());
        assertEquals("area", loadedPlan.powerConnections().get(0).consumerId());
        assertEquals("line", loadedPlan.powerConnections().get(1).consumerId());
        assertEquals(800, new PowerSummaryService().summaries(loadedPlan).getFirst().usedWatts());
    }

    @Test
    void savesAndLoadsPowerConnectionOffsets() throws IOException {
        EventPlan plan = new EventPlan("Test");
        Tent tent = new Tent("tent", "Telk", new Position(10, 20));
        tent.setPowerConnectionOffset(new Position(4, -6));
        AreaObject area = new AreaObject("area", "Ala", new Position(30, 40));
        area.setPowerConnectionOffset(new Position(-12, 8));
        LineObject line = new LineObject("line", "Joon", new Position(50, 60));
        line.setPowerConnectionOffset(new Position(15, 20));
        plan.addObject(tent);
        plan.addObject(area);
        plan.addObject(line);
        Path file = tempDirectory.resolve("power-connection-offsets.pplan");

        service.save(plan, file);
        EventPlan loadedPlan = service.load(file);

        Tent loadedTent = (Tent) loadedPlan.objects().get(0);
        AreaObject loadedArea = (AreaObject) loadedPlan.objects().get(1);
        LineObject loadedLine = (LineObject) loadedPlan.objects().get(2);
        assertEquals(new Position(4, -6), loadedTent.powerConnectionOffset());
        assertEquals(new Position(-12, 8), loadedArea.powerConnectionOffset());
        assertEquals(new Position(15, 20), loadedLine.powerConnectionOffset());
    }
}
