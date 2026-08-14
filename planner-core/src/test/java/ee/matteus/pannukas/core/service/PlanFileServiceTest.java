package ee.matteus.pannukas.core.service;

import ee.matteus.pannukas.core.model.AreaObject;
import ee.matteus.pannukas.core.model.CustomObject;
import ee.matteus.pannukas.core.model.Equipment;
import ee.matteus.pannukas.core.model.EventPlan;
import ee.matteus.pannukas.core.model.LineObject;
import ee.matteus.pannukas.core.model.Position;
import ee.matteus.pannukas.core.model.Tent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlanFileServiceTest {
    @TempDir
    Path tempDirectory;

    private final PlanFileService service = new PlanFileService();

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
    }
}
