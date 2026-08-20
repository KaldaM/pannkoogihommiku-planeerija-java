package ee.matteus.plaanisepp.core.service;

import ee.matteus.plaanisepp.core.model.AreaObject;
import ee.matteus.plaanisepp.core.model.ConnectorType;
import ee.matteus.plaanisepp.core.model.CustomObject;
import ee.matteus.plaanisepp.core.model.DistributionPanel;
import ee.matteus.plaanisepp.core.model.Equipment;
import ee.matteus.plaanisepp.core.model.EventPlan;
import ee.matteus.plaanisepp.core.model.LineObject;
import ee.matteus.plaanisepp.core.model.Position;
import ee.matteus.plaanisepp.core.model.PowerConnection;
import ee.matteus.plaanisepp.core.model.PowerOutlet;
import ee.matteus.plaanisepp.core.model.PowerSource;
import ee.matteus.plaanisepp.core.model.Tent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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

        try (ZipFile zipFile = new ZipFile(file.toFile())) {
            Properties manifest = readProperties(zipFile, "manifest.properties");
            Properties planProperties = readProperties(zipFile, "plan.properties");
            assertEquals("pannukas-plan-package", manifest.getProperty("format"));
            assertEquals(
                    Integer.toString(PlanFileService.CURRENT_FORMAT_VERSION),
                    manifest.getProperty("formatVersion")
            );
            assertEquals(
                    Integer.toString(PlanFileService.CURRENT_FORMAT_VERSION),
                    planProperties.getProperty("formatVersion")
            );
        }
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
    void loadsExplicitVersionOnePlan() throws IOException {
        Path file = tempDirectory.resolve("version-one-plan.pplan");
        Files.writeString(file, """
                format=pannukas-plan-v1
                formatVersion=1
                plan.name=Versioon 1 plaan
                objects.count=0
                connections.count=0
                """);

        EventPlan loadedPlan = service.load(file);

        assertEquals("Versioon 1 plaan", loadedPlan.name());
    }

    @Test
    void upgradesVersionOnePlanOnNextSave() throws IOException {
        Path file = tempDirectory.resolve("upgraded-plan.pplan");
        byte[] mapImage = testPng();
        Path sourceImage = tempDirectory.resolve("vana-kaart.png");
        Files.write(sourceImage, mapImage);
        Properties legacyProperties = new Properties();
        legacyProperties.setProperty("format", "pannukas-plan-v1");
        legacyProperties.setProperty("formatVersion", "1");
        legacyProperties.setProperty("plan.name", "Uuendatav plaan");
        legacyProperties.setProperty("plan.mapImagePath", sourceImage.toString());
        legacyProperties.setProperty("objects.count", "0");
        legacyProperties.setProperty("connections.count", "0");
        try (var output = Files.newOutputStream(file)) {
            legacyProperties.store(output, "Versioon 1 testplaan");
        }
        EventPlan plan = service.load(file);

        service.save(plan, file);
        Files.delete(sourceImage);

        try (ZipFile zipFile = new ZipFile(file.toFile())) {
            assertNotNull(zipFile.getEntry("manifest.properties"));
            assertNotNull(zipFile.getEntry("plan.properties"));
            assertNotNull(zipFile.getEntry("assets/map.png"));
        }
        EventPlan upgradedPlan = service.load(file);
        assertEquals("Uuendatav plaan", upgradedPlan.name());
        assertArrayEquals(mapImage, upgradedPlan.packagedMapImage());
    }

    @Test
    void rejectsPlanFromNewerFormatVersion() throws IOException {
        Path file = tempDirectory.resolve("future-plan.pplan");
        writePackage(file, 4, null, null);

        IOException exception = assertThrows(IOException.class, () -> service.load(file));

        assertTrue(exception.getMessage().contains("uuema rakenduse versiooniga"));
        assertTrue(exception.getMessage().contains("uuenda rakendust"));
    }

    @Test
    void rejectsPlainPropertiesFileMarkedAsVersionTwo() throws IOException {
        Path file = tempDirectory.resolve("invalid-version-two.pplan");
        Files.writeString(file, """
                formatVersion=2
                plan.name=Vale versioon 2
                objects.count=0
                connections.count=0
                """);

        IOException exception = assertThrows(IOException.class, () -> service.load(file));

        assertTrue(exception.getMessage().contains("ZIP-pakett"));
    }

    @Test
    void savesAndLoadsCurrentPackageWithoutMapImage() throws IOException {
        EventPlan plan = new EventPlan("Kaardita plaan");
        Path file = tempDirectory.resolve("without-map.pplan");

        service.save(plan, file);
        EventPlan loadedPlan = service.load(file);

        assertEquals("Kaardita plaan", loadedPlan.name());
        assertEquals("", loadedPlan.mapImagePath());
        assertFalse(loadedPlan.hasPackagedMapImage());
        try (ZipFile zipFile = new ZipFile(file.toFile())) {
            Properties manifest = readProperties(zipFile, "manifest.properties");
            assertNull(manifest.getProperty("mapEntry"));
        }
    }

    @Test
    void embedsUserMapImageAndKeepsItForLaterSaves() throws IOException {
        byte[] mapImage = testPng();
        Path sourceImage = tempDirectory.resolve("kasutaja-kaart.png");
        Files.write(sourceImage, mapImage);
        EventPlan plan = new EventPlan("Kaardiga plaan");
        plan.setMapImagePath(sourceImage.toString());
        Path firstFile = tempDirectory.resolve("with-map.pplan");

        service.save(plan, firstFile);

        assertTrue(plan.hasPackagedMapImage());
        assertEquals("package:/assets/map.png", plan.mapImagePath());
        Files.delete(sourceImage);
        Path secondFile = tempDirectory.resolve("resaved-with-map.pplan");
        service.save(plan, secondFile);

        EventPlan loadedPlan = service.load(secondFile);
        assertTrue(loadedPlan.hasPackagedMapImage());
        assertEquals("assets/map.png", loadedPlan.packagedMapImageEntry());
        assertEquals("package:/assets/map.png", loadedPlan.mapImagePath());
        assertArrayEquals(mapImage, loadedPlan.packagedMapImage());
        try (ZipFile zipFile = new ZipFile(secondFile.toFile())) {
            assertNotNull(zipFile.getEntry("assets/map.png"));
        }
    }

    @Test
    void embedsJpegMapUsingCanonicalPackageEntry() throws IOException {
        byte[] mapImage = testJpeg();
        Path sourceImage = tempDirectory.resolve("kasutaja-kaart.jpeg");
        Files.write(sourceImage, mapImage);
        EventPlan plan = new EventPlan("JPEG-kaardiga plaan");
        plan.setMapImagePath(sourceImage.toString());
        Path file = tempDirectory.resolve("with-jpeg-map.pplan");

        service.save(plan, file);
        EventPlan loadedPlan = service.load(file);

        assertEquals("package:/assets/map.jpg", loadedPlan.mapImagePath());
        assertArrayEquals(mapImage, loadedPlan.packagedMapImage());
        try (ZipFile zipFile = new ZipFile(file.toFile())) {
            assertNotNull(zipFile.getEntry("assets/map.jpg"));
        }
    }

    @Test
    void keepsClasspathMapReferenceWithoutEmbeddingIt() throws IOException {
        EventPlan plan = new EventPlan("Vaikekaart");
        plan.setMapImagePath("classpath:/maps/tavakaart.png");
        Path file = tempDirectory.resolve("classpath-map.pplan");

        service.save(plan, file);
        EventPlan loadedPlan = service.load(file);

        assertEquals("classpath:/maps/tavakaart.png", loadedPlan.mapImagePath());
        assertFalse(loadedPlan.hasPackagedMapImage());
    }

    @Test
    void rejectsIncompleteVersionTwoPackage() throws IOException {
        Path file = tempDirectory.resolve("incomplete.pplan");
        writePackage(file, 2, null, null);

        IOException exception = assertThrows(IOException.class, () -> service.load(file));

        assertTrue(exception.getMessage().contains("plan.properties"));
    }

    @Test
    void rejectsPackageWithInvalidMapImage() throws IOException {
        Path file = tempDirectory.resolve("invalid-map.pplan");
        byte[] invalidPng = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
        writePackage(file, 2, """
                format=pannukas-plan-v2
                formatVersion=2
                plan.name=Vigase kaardiga plaan
                plan.mapImagePath=package:/assets/map.png
                objects.count=0
                connections.count=0
                """, invalidPng);

        IOException exception = assertThrows(IOException.class, () -> service.load(file));

        assertTrue(exception.getMessage().contains("Kaardipildi"));
    }

    @Test
    void reportsMalformedPlanDataAsFileError() throws IOException {
        Path file = tempDirectory.resolve("malformed-data.pplan");
        writePackage(file, 2, """
                format=pannukas-plan-v2
                formatVersion=2
                plan.name=Vigaste andmetega plaan
                objects.count=mitte-arv
                connections.count=0
                """, null);

        IOException exception = assertThrows(IOException.class, () -> service.load(file));

        assertTrue(exception.getMessage().contains("andmed ei ole korrektsed"));
    }

    @Test
    void failedSaveDoesNotOverwriteExistingPlanFile() throws IOException {
        Path file = tempDirectory.resolve("existing.pplan");
        Files.writeString(file, "existing content");
        EventPlan plan = new EventPlan("Puuduva kaardiga plaan");
        plan.setMapImagePath(tempDirectory.resolve("missing-map.png").toString());

        IOException exception = assertThrows(IOException.class, () -> service.save(plan, file));

        assertTrue(exception.getMessage().contains("Kaardipilti ei leitud"));
        assertEquals("existing content", Files.readString(file));
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
        area.addEquipment(new Equipment("equipment-1", "Valgusti", 500));
        area.addEquipment(new Equipment("equipment-2", "Soojendi", 1500));
        LineObject line = new LineObject("line-1", "Valguskett", new Position(30, 40));
        line.addEquipment(new Equipment("equipment-3", "Lambid", 750));
        plan.addObject(area);
        plan.addObject(line);
        Path file = tempDirectory.resolve("area-line-equipment.pplan");

        service.save(plan, file);
        EventPlan loadedPlan = service.load(file);

        AreaObject loadedArea = (AreaObject) loadedPlan.objects().get(0);
        assertEquals(2, loadedArea.equipment().size());
        assertEquals("equipment-1", loadedArea.equipment().get(0).id());
        assertEquals("equipment-2", loadedArea.equipment().get(1).id());
        assertEquals("Valgusti", loadedArea.equipment().get(0).name());
        assertEquals(2000, loadedArea.requiredWatts());

        LineObject loadedLine = (LineObject) loadedPlan.objects().get(1);
        assertEquals(1, loadedLine.equipment().size());
        assertEquals("equipment-3", loadedLine.equipment().getFirst().id());
        assertEquals("Lambid", loadedLine.equipment().getFirst().name());
        assertEquals(750, loadedLine.requiredWatts());
    }

    @Test
    void assignsIdWhenOlderEquipmentHasNone() throws IOException {
        Path file = tempDirectory.resolve("old-equipment-plan.pplan");
        Files.writeString(file, """
                plan.name=Vana plaan
                objects.count=1
                object.0.type=TENT
                object.0.id=tent-1
                object.0.name=Telk
                object.0.x=10
                object.0.y=20
                object.0.equipment.count=1
                object.0.equipment.0.name=Pliit
                object.0.equipment.0.requiredWatts=1200
                connections.count=0
                """);

        EventPlan loadedPlan = service.load(file);

        Tent loadedTent = (Tent) loadedPlan.objects().getFirst();
        assertEquals(1, loadedTent.equipment().size());
        assertFalse(loadedTent.equipment().getFirst().id().isBlank());
        assertEquals("Pliit", loadedTent.equipment().getFirst().name());
    }

    @Test
    void assignsIdWhenOlderPowerConnectionHasNone() throws IOException {
        Path file = tempDirectory.resolve("old-power-connection-plan.pplan");
        Files.writeString(file, """
                plan.name=Vana plaan
                objects.count=2
                object.0.type=POWER_SOURCE
                object.0.id=source-1
                object.0.name=Kapp
                object.0.x=0
                object.0.y=0
                object.0.outlets.count=1
                object.0.outlet.0.id=outlet-1
                object.0.outlet.0.type=SCHUKO_230V
                object.0.outlet.0.capacityWatts=11000
                object.1.type=TENT
                object.1.id=tent-1
                object.1.name=Telk
                object.1.x=10
                object.1.y=20
                object.1.equipment.count=1
                object.1.equipment.0.name=Pliit
                object.1.equipment.0.requiredWatts=1200
                connections.count=1
                connection.0.sourceId=source-1
                connection.0.consumerId=tent-1
                connection.0.connectorType=SCHUKO_230V
                connection.0.outletId=outlet-1
                """);

        EventPlan loadedPlan = service.load(file);

        assertEquals(1, loadedPlan.powerConnections().size());
        assertFalse(loadedPlan.powerConnections().getFirst().id().isBlank());
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
        String areaConnectionId = plan.connectToPower(source.id(), area.id(), ConnectorType.SCHUKO_230V, "outlet")
                .orElseThrow()
                .id();
        String lineConnectionId = plan.connectToPower(source.id(), line.id(), ConnectorType.SCHUKO_230V, "outlet")
                .orElseThrow()
                .id();
        Path file = tempDirectory.resolve("area-line-connections.pplan");

        service.save(plan, file);
        EventPlan loadedPlan = service.load(file);

        assertEquals(2, loadedPlan.powerConnections().size());
        assertEquals("area", loadedPlan.powerConnections().get(0).consumerId());
        assertEquals("line", loadedPlan.powerConnections().get(1).consumerId());
        assertEquals(areaConnectionId, loadedPlan.powerConnections().get(0).id());
        assertEquals(lineConnectionId, loadedPlan.powerConnections().get(1).id());
        assertEquals(800, new PowerSummaryService().summaries(loadedPlan).getFirst().usedWatts());
    }

    @Test
    void savesAndLoadsMultiplePowerConnectionsAndEquipmentAssignment() throws IOException {
        EventPlan plan = new EventPlan("Test");
        PowerSource firstSource = new PowerSource("source-1", "Esimene kapp", new Position(0, 0));
        firstSource.addOutlet(new PowerOutlet("outlet-1", ConnectorType.SCHUKO_230V, 11000));
        PowerSource secondSource = new PowerSource("source-2", "Teine kapp", new Position(100, 100));
        secondSource.addOutlet(new PowerOutlet("outlet-2", ConnectorType.SCHUKO_230V, 11000));
        Tent tent = new Tent("tent-1", "Telk", new Position(20, 20));
        Equipment cooker = new Equipment("equipment-1", "Pliit", 1200);
        Equipment fridge = new Equipment("equipment-2", "Külmik", 500);
        tent.addEquipment(cooker);
        tent.addEquipment(fridge);
        plan.addObject(firstSource);
        plan.addObject(secondSource);
        plan.addObject(tent);
        PowerConnection defaultConnection = plan.connectToPower(
                firstSource.id(), tent.id(), ConnectorType.SCHUKO_230V, "outlet-1"
        ).orElseThrow();
        PowerConnection alternativeConnection = plan.addAlternativePowerConnection(
                secondSource.id(), tent.id(), ConnectorType.SCHUKO_230V, "outlet-2"
        ).orElseThrow();
        defaultConnection = plan.connectToPower(
                firstSource.id(), tent.id(), ConnectorType.SCHUKO_230V, "outlet-1"
        ).orElseThrow();
        plan.updateCableRoutePointsForConnection(
                alternativeConnection.id(), List.of(new Position(30, 40), new Position(50, 60)));
        plan.updateCableLabelOffsetForConnection(alternativeConnection.id(), new Position(7, 8));
        plan.assignEquipmentToPowerConnection(tent.id(), fridge.id(), alternativeConnection.id());
        Path file = tempDirectory.resolve("multiple-power-connections.pplan");

        service.save(plan, file);
        EventPlan loadedPlan = service.load(file);

        assertEquals(3, PlanFileService.CURRENT_FORMAT_VERSION);
        assertEquals(2, loadedPlan.findPowerConnectionsForConsumer(tent.id()).size());
        PowerConnection loadedDefault = loadedPlan.findPowerConnectionForConsumer(tent.id()).orElseThrow();
        PowerConnection loadedAlternative = loadedPlan.powerConnections().stream()
                .filter(connection -> connection.id().equals(alternativeConnection.id()))
                .findFirst()
                .orElseThrow();
        assertEquals(defaultConnection.id(), loadedDefault.id());
        assertFalse(loadedAlternative.defaultForConsumer());
        assertEquals(List.of(new Position(30, 40), new Position(50, 60)), loadedAlternative.routePoints());
        assertEquals(new Position(7, 8), loadedAlternative.cableLabelOffset());
        Tent loadedTent = (Tent) loadedPlan.findObject(tent.id()).orElseThrow();
        assertEquals(alternativeConnection.id(), loadedTent.equipment().get(1).powerConnectionId());
        assertEquals(1200, new PowerSummaryService().summaries(loadedPlan).get(0).usedWatts());
        assertEquals(500, new PowerSummaryService().summaries(loadedPlan).get(1).usedWatts());
    }

    @Test
    void loadsVersionTwoConnectionAsDefaultPower() throws IOException {
        Path file = tempDirectory.resolve("version-two-power.pplan");
        writePackage(file, 2, """
                format=pannukas-plan-v2
                formatVersion=2
                plan.name=Versioon 2 vool
                objects.count=2
                object.0.type=POWER_SOURCE
                object.0.id=source-1
                object.0.name=Kapp
                object.0.outlets.count=1
                object.0.outlet.0.id=outlet-1
                object.0.outlet.0.type=SCHUKO_230V
                object.0.outlet.0.capacityWatts=11000
                object.1.type=TENT
                object.1.id=tent-1
                object.1.name=Telk
                object.1.equipment.count=1
                object.1.equipment.0.name=Pliit
                object.1.equipment.0.requiredWatts=1200
                connections.count=1
                connection.0.id=connection-1
                connection.0.sourceId=source-1
                connection.0.consumerId=tent-1
                connection.0.connectorType=SCHUKO_230V
                connection.0.outletId=outlet-1
                """, null);

        EventPlan loadedPlan = service.load(file);

        PowerConnection connection = loadedPlan.findPowerConnectionForConsumer("tent-1").orElseThrow();
        assertTrue(connection.defaultForConsumer());
        assertEquals("connection-1", connection.id());
        Tent loadedTent = (Tent) loadedPlan.findObject("tent-1").orElseThrow();
        assertTrue(loadedTent.equipment().getFirst().usesDefaultPower());
    }

    @Test
    void savesAndLoadsDistributionPanelPowerChain() throws IOException {
        EventPlan plan = new EventPlan("Test");
        PowerSource mainSource = new PowerSource("source", "Põhikilp", new Position(0, 0));
        mainSource.addOutlet(new PowerOutlet("source-outlet", ConnectorType.SCHUKO_230V, 11000));
        DistributionPanel panel = new DistributionPanel("panel", "Alajaotuskilp", new Position(30, 40));
        panel.setPowerConnectionOffset(new Position(4, 5));
        panel.addOutlet(new PowerOutlet("panel-outlet", "Väljund 1", ConnectorType.SCHUKO_230V, 3500));
        Tent tent = new Tent("tent", "Telk", new Position(60, 70));
        tent.addEquipment(new Equipment("equipment", "Pliit", 1200));
        plan.addObject(mainSource);
        plan.addObject(panel);
        plan.addObject(tent);
        plan.connectToPower(mainSource.id(), panel.id(), ConnectorType.SCHUKO_230V, "source-outlet");
        plan.connectToPower(panel.id(), tent.id(), ConnectorType.SCHUKO_230V, "panel-outlet");
        Path file = tempDirectory.resolve("distribution-panel.pplan");

        service.save(plan, file);
        EventPlan loadedPlan = service.load(file);

        DistributionPanel loadedPanel = (DistributionPanel) loadedPlan.findObject(panel.id()).orElseThrow();
        assertEquals("Alajaotuskilp", loadedPanel.name());
        assertEquals(new Position(30, 40), loadedPanel.position());
        assertEquals(new Position(4, 5), loadedPanel.powerConnectionOffset());
        assertEquals(1, loadedPanel.outlets().size());
        assertEquals("Väljund 1", loadedPanel.outlets().getFirst().name());
        assertEquals(3500, loadedPanel.outlets().getFirst().capacityWatts());
        assertEquals(2, loadedPlan.powerConnections().size());
        assertEquals(1200, new PowerSummaryService().summaries(loadedPlan).stream()
                .filter(summary -> summary.sourceId().equals(mainSource.id()))
                .findFirst()
                .orElseThrow()
                .usedWatts());
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

    private Properties readProperties(ZipFile zipFile, String entryName) throws IOException {
        Properties properties = new Properties();
        ZipEntry entry = zipFile.getEntry(entryName);
        assertNotNull(entry);
        try (InputStream input = zipFile.getInputStream(entry)) {
            properties.load(input);
        }
        return properties;
    }

    private void writePackage(Path file, int formatVersion, String planContents, byte[] mapImage) throws IOException {
        Properties manifest = new Properties();
        manifest.setProperty("format", "pannukas-plan-package");
        manifest.setProperty("formatVersion", Integer.toString(formatVersion));
        manifest.setProperty("planEntry", "plan.properties");
        if (mapImage != null) {
            manifest.setProperty("mapEntry", "assets/map.png");
        }

        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(file))) {
            output.putNextEntry(new ZipEntry("manifest.properties"));
            manifest.store(output, "Test manifest");
            output.closeEntry();
            if (planContents != null) {
                output.putNextEntry(new ZipEntry("plan.properties"));
                output.write(planContents.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1));
                output.closeEntry();
            }
            if (mapImage != null) {
                output.putNextEntry(new ZipEntry("assets/map.png"));
                output.write(mapImage);
                output.closeEntry();
            }
        }
    }

    private byte[] testPng() {
        return Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII="
        );
    }

    private byte[] testJpeg() throws IOException {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, 0x336699);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            assertTrue(ImageIO.write(image, "jpeg", output));
            return output.toByteArray();
        }
    }
}
