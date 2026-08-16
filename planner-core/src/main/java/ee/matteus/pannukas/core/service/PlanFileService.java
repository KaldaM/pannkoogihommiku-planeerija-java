package ee.matteus.pannukas.core.service;

import ee.matteus.pannukas.core.model.ConnectorType;
import ee.matteus.pannukas.core.model.AreaObject;
import ee.matteus.pannukas.core.model.CustomObject;
import ee.matteus.pannukas.core.model.CustomObjectShape;
import ee.matteus.pannukas.core.model.Equipment;
import ee.matteus.pannukas.core.model.EquipmentContainer;
import ee.matteus.pannukas.core.model.EventPlan;
import ee.matteus.pannukas.core.model.LineObject;
import ee.matteus.pannukas.core.model.MarkerObject;
import ee.matteus.pannukas.core.model.MarkerType;
import ee.matteus.pannukas.core.model.PlannerObject;
import ee.matteus.pannukas.core.model.Position;
import ee.matteus.pannukas.core.model.PowerConnection;
import ee.matteus.pannukas.core.model.PowerConnectable;
import ee.matteus.pannukas.core.model.PowerOutlet;
import ee.matteus.pannukas.core.model.PowerSource;
import ee.matteus.pannukas.core.model.TextObject;
import ee.matteus.pannukas.core.model.Tent;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class PlanFileService {
    public static final int CURRENT_FORMAT_VERSION = 2;
    private static final int LEGACY_FORMAT_VERSION = 1;
    private static final String FORMAT_VERSION_PROPERTY = "formatVersion";
    private static final String PACKAGE_FORMAT = "pannukas-plan-package";
    private static final String PLAN_FORMAT_V2 = "pannukas-plan-v2";
    private static final String MANIFEST_ENTRY = "manifest.properties";
    private static final String PLAN_ENTRY = "plan.properties";
    private static final String MAP_ENTRY_PROPERTY = "mapEntry";
    private static final long MAX_MANIFEST_BYTES = 64 * 1024;
    private static final long MAX_PLAN_BYTES = 5 * 1024 * 1024;
    private static final long MAX_MAP_BYTES = 50 * 1024 * 1024;
    private static final long MAX_MAP_PIXELS = 50_000_000;
    private static final int MAX_PACKAGE_ENTRIES = 20;

    public void save(EventPlan plan, Path file) throws IOException {
        MapAsset mapAsset = mapAssetFor(plan);
        String storedMapPath = mapAsset == null
                ? plan.mapImagePath()
                : "package:/" + mapAsset.entryName();
        Properties planProperties = createPlanProperties(plan, storedMapPath);
        Properties manifest = new Properties();
        manifest.setProperty("format", PACKAGE_FORMAT);
        manifest.setProperty(FORMAT_VERSION_PROPERTY, Integer.toString(CURRENT_FORMAT_VERSION));
        manifest.setProperty("planEntry", PLAN_ENTRY);
        if (mapAsset != null) {
            manifest.setProperty(MAP_ENTRY_PROPERTY, mapAsset.entryName());
        }

        Path target = file.toAbsolutePath();
        Path parent = target.getParent();
        if (parent == null || !Files.isDirectory(parent)) {
            throw new IOException("Plaani sihtkausta ei leitud: " + target);
        }

        Path temporaryFile = Files.createTempFile(parent, ".pplan-", ".tmp");
        boolean saved = false;
        try {
            writePackage(temporaryFile, manifest, planProperties, mapAsset);
            loadPackage(temporaryFile);
            replaceFile(temporaryFile, target);
            saved = true;
        } finally {
            if (!saved) {
                Files.deleteIfExists(temporaryFile);
            }
        }

        if (mapAsset != null) {
            plan.setPackagedMapImage(mapAsset.entryName(), mapAsset.data());
        }
    }

    private Properties createPlanProperties(EventPlan plan, String mapImagePath) {
        Properties properties = new Properties();
        properties.setProperty("format", PLAN_FORMAT_V2);
        properties.setProperty(FORMAT_VERSION_PROPERTY, Integer.toString(CURRENT_FORMAT_VERSION));
        properties.setProperty("plan.name", plan.name());
        properties.setProperty("plan.mapImagePath", mapImagePath);
        properties.setProperty("plan.pixelsPerMeter", Double.toString(plan.pixelsPerMeter()));
        properties.setProperty("plan.objectLabelFontSize", Double.toString(plan.objectLabelFontSize()));
        properties.setProperty("plan.cableLabelFontSize", Double.toString(plan.cableLabelFontSize()));
        properties.setProperty("layers.showCables", Boolean.toString(plan.showCables()));
        properties.setProperty("layers.showCableLabels", Boolean.toString(plan.showCableLabels()));
        properties.setProperty("layers.show230VCables", Boolean.toString(plan.showCableType(ConnectorType.SCHUKO_230V)));
        properties.setProperty("layers.show16ACables", Boolean.toString(plan.showCableType(ConnectorType.INDUSTRIAL_16A)));
        properties.setProperty("layers.show32ACables", Boolean.toString(plan.showCableType(ConnectorType.INDUSTRIAL_32A)));
        properties.setProperty("layers.show63ACables", Boolean.toString(plan.showCableType(ConnectorType.INDUSTRIAL_63A)));
        properties.setProperty("layers.showObjectLabels", Boolean.toString(plan.showObjectLabels()));
        properties.setProperty("layers.showTents", Boolean.toString(plan.showTents()));
        properties.setProperty("layers.showPowerSources", Boolean.toString(plan.showPowerSources()));
        properties.setProperty("layers.showCustomObjects", Boolean.toString(plan.showCustomObjects()));
        properties.setProperty("layers.showTextObjects", Boolean.toString(plan.showTextObjects()));
        properties.setProperty("layers.showMarkerObjects", Boolean.toString(plan.showMarkerObjects()));
        properties.setProperty("layers.showAreaObjects", Boolean.toString(plan.showAreaObjects()));
        properties.setProperty("layers.showLineObjects", Boolean.toString(plan.showLineObjects()));
        properties.setProperty("hiddenGroups.count", Integer.toString(plan.hiddenGroups().size()));

        int hiddenGroupIndex = 0;
        for (String hiddenGroup : plan.hiddenGroups()) {
            properties.setProperty("hiddenGroup." + hiddenGroupIndex, hiddenGroup);
            hiddenGroupIndex++;
        }

        properties.setProperty("objects.count", Integer.toString(plan.objects().size()));

        List<PlannerObject> objects = plan.objects();
        for (int index = 0; index < objects.size(); index++) {
            writeObject(properties, "object." + index + ".", objects.get(index));
        }

        properties.setProperty("connections.count", Integer.toString(plan.powerConnections().size()));
        for (int index = 0; index < plan.powerConnections().size(); index++) {
            PowerConnection connection = plan.powerConnections().get(index);
            String prefix = "connection." + index + ".";
            properties.setProperty(prefix + "sourceId", connection.sourceId());
            properties.setProperty(prefix + "consumerId", connection.consumerId());
            properties.setProperty(prefix + "connectorType", connection.connectorType().name());
            properties.setProperty(prefix + "outletId", connection.outletId());
            properties.setProperty(prefix + "cableNotes", connection.cableNotes());
            properties.setProperty(prefix + "cableLengthNotes", connection.cableLengthNotes());
            properties.setProperty(prefix + "customCableLabelPosition", Boolean.toString(connection.customCableLabelPosition()));
            properties.setProperty(prefix + "cableLabelOffsetX", Double.toString(connection.cableLabelOffset().x()));
            properties.setProperty(prefix + "cableLabelOffsetY", Double.toString(connection.cableLabelOffset().y()));
            properties.setProperty(prefix + "routePoints.count", Integer.toString(connection.routePoints().size()));
            for (int pointIndex = 0; pointIndex < connection.routePoints().size(); pointIndex++) {
                Position point = connection.routePoints().get(pointIndex);
                String pointPrefix = prefix + "routePoint." + pointIndex + ".";
                properties.setProperty(pointPrefix + "x", Double.toString(point.x()));
                properties.setProperty(pointPrefix + "y", Double.toString(point.y()));
            }
        }

        return properties;
    }

    public EventPlan load(Path file) throws IOException {
        return isZipPackage(file) ? loadPackage(file) : loadLegacyPlan(file);
    }

    private EventPlan loadLegacyPlan(Path file) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(file)) {
            properties.load(input);
        }
        validateLegacyFormatVersion(properties);
        return readPlan(properties);
    }

    private EventPlan readPlan(Properties properties) {
        EventPlan plan = new EventPlan(properties.getProperty("plan.name", "Pannkoogihommik"));
        plan.setMapImagePath(properties.getProperty("plan.mapImagePath", ""));
        plan.setPixelsPerMeter(doubleValue(properties, "plan.pixelsPerMeter", EventPlan.DEFAULT_PIXELS_PER_METER));
        plan.setObjectLabelFontSize(doubleValue(properties, "plan.objectLabelFontSize", EventPlan.DEFAULT_OBJECT_LABEL_FONT_SIZE));
        plan.setCableLabelFontSize(doubleValue(properties, "plan.cableLabelFontSize", EventPlan.DEFAULT_CABLE_LABEL_FONT_SIZE));
        plan.setShowCables(booleanValue(properties, "layers.showCables", true));
        plan.setShowCableLabels(booleanValue(properties, "layers.showCableLabels", true));
        plan.setShowCableType(ConnectorType.SCHUKO_230V, booleanValue(properties, "layers.show230VCables", true));
        plan.setShowCableType(ConnectorType.INDUSTRIAL_16A, booleanValue(properties, "layers.show16ACables", true));
        plan.setShowCableType(ConnectorType.INDUSTRIAL_32A, booleanValue(properties, "layers.show32ACables", true));
        plan.setShowCableType(ConnectorType.INDUSTRIAL_63A, booleanValue(properties, "layers.show63ACables", true));
        plan.setShowObjectLabels(booleanValue(properties, "layers.showObjectLabels", true));
        plan.setShowTents(booleanValue(properties, "layers.showTents", true));
        plan.setShowPowerSources(booleanValue(properties, "layers.showPowerSources", true));
        plan.setShowCustomObjects(booleanValue(properties, "layers.showCustomObjects", true));
        plan.setShowTextObjects(booleanValue(properties, "layers.showTextObjects", true));
        plan.setShowMarkerObjects(booleanValue(properties, "layers.showMarkerObjects", true));
        plan.setShowAreaObjects(booleanValue(properties, "layers.showAreaObjects", true));
        plan.setShowLineObjects(booleanValue(properties, "layers.showLineObjects", true));

        int objectCount = intValue(properties, "objects.count", 0);
        for (int index = 0; index < objectCount; index++) {
            plan.addObject(readObject(properties, "object." + index + "."));
        }

        int connectionCount = intValue(properties, "connections.count", 0);
        for (int index = 0; index < connectionCount; index++) {
            String prefix = "connection." + index + ".";
            plan.connectToPower(
                    properties.getProperty(prefix + "sourceId", ""),
                    properties.getProperty(prefix + "consumerId", ""),
                    ConnectorType.valueOf(properties.getProperty(prefix + "connectorType", ConnectorType.SCHUKO_230V.name())),
                    properties.getProperty(prefix + "outletId", ""),
                    properties.getProperty(prefix + "cableNotes", ""),
                    properties.getProperty(prefix + "cableLengthNotes", properties.getProperty(prefix + "cableNotes", ""))
            ).ifPresent(connection -> plan.updateCableRoutePoints(
                    connection.consumerId(),
                    readRoutePoints(properties, prefix)
            ));
            if (Boolean.parseBoolean(properties.getProperty(prefix + "customCableLabelPosition", "false"))) {
                plan.updateCableLabelOffset(
                        properties.getProperty(prefix + "consumerId", ""),
                        new Position(
                                doubleValue(properties, prefix + "cableLabelOffsetX", 0),
                                doubleValue(properties, prefix + "cableLabelOffsetY", 0)
                        )
                );
            }
        }

        int hiddenGroupCount = intValue(properties, "hiddenGroups.count", 0);
        for (int index = 0; index < hiddenGroupCount; index++) {
            plan.setGroupHidden(properties.getProperty("hiddenGroup." + index, ""), true);
        }

        return plan;
    }

    private void validateLegacyFormatVersion(Properties properties) throws IOException {
        String value = properties.getProperty(FORMAT_VERSION_PROPERTY);
        if (value == null || value.isBlank()) {
            return;
        }

        int formatVersion = parseFormatVersion(value);

        if (formatVersion > CURRENT_FORMAT_VERSION) {
            throw newerVersionException(formatVersion);
        }
        if (formatVersion > LEGACY_FORMAT_VERSION) {
            throw new IOException("Versioon 2 plaanifail ei ole korrektne ZIP-pakett.");
        }
        if (formatVersion < 1) {
            throw new IOException("Plaanifaili vormingu versiooni " + formatVersion + " ei toetata.");
        }
    }

    private EventPlan loadPackage(Path file) throws IOException {
        try (ZipFile zipFile = new ZipFile(file.toFile())) {
            validatePackageEntries(zipFile);
            Properties manifest = readProperties(zipFile, MANIFEST_ENTRY, MAX_MANIFEST_BYTES);
            if (!PACKAGE_FORMAT.equals(manifest.getProperty("format"))) {
                throw new IOException("Plaanipaketi manifesti vorming ei ole korrektne.");
            }

            int formatVersion = parseFormatVersion(manifest.getProperty(FORMAT_VERSION_PROPERTY, ""));
            if (formatVersion > CURRENT_FORMAT_VERSION) {
                throw newerVersionException(formatVersion);
            }
            if (formatVersion != CURRENT_FORMAT_VERSION) {
                throw new IOException("Plaanipaketi vormingu versiooni " + formatVersion + " ei toetata.");
            }
            if (!PLAN_ENTRY.equals(manifest.getProperty("planEntry"))) {
                throw new IOException("Plaanipaketi plaaniandmete kirje ei ole korrektne.");
            }

            Properties planProperties = readProperties(zipFile, PLAN_ENTRY, MAX_PLAN_BYTES);
            if (!PLAN_FORMAT_V2.equals(planProperties.getProperty("format"))) {
                throw new IOException("Plaanipaketi plaaniandmete vorming ei ole korrektne.");
            }
            int planVersion = parseFormatVersion(planProperties.getProperty(FORMAT_VERSION_PROPERTY, ""));
            if (planVersion != formatVersion) {
                throw new IOException("Plaanipaketi manifesti ja plaaniandmete versioonid ei ühti.");
            }

            EventPlan plan = readPlan(planProperties);
            String mapEntry = manifest.getProperty(MAP_ENTRY_PROPERTY, "").trim();
            if (!mapEntry.isEmpty()) {
                validateMapEntryName(mapEntry);
                if (!("package:/" + mapEntry).equals(plan.mapImagePath())) {
                    throw new IOException("Plaanipaketi kaardipildi viide ei ühti manifestiga.");
                }
                byte[] mapData = readEntry(zipFile, mapEntry, MAX_MAP_BYTES);
                validateMapImage(mapData);
                plan.setPackagedMapImage(mapEntry, mapData);
            } else if (plan.mapImagePath().startsWith("package:/")) {
                throw new IOException("Plaanipaketis viidatud kaardipilt puudub.");
            } else if (!plan.mapImagePath().isBlank() && !plan.mapImagePath().startsWith("classpath:")) {
                throw new IOException("Versioon 2 plaanipakett sisaldab välist kaardipildi viidet.");
            }
            return plan;
        } catch (java.util.zip.ZipException exception) {
            throw new IOException("Plaanipakett ei ole korrektne ZIP-fail.", exception);
        } catch (RuntimeException exception) {
            throw new IOException("Plaanipaketi andmed ei ole korrektsed.", exception);
        }
    }

    private void writePackage(
            Path file,
            Properties manifest,
            Properties planProperties,
            MapAsset mapAsset
    ) throws IOException {
        try (OutputStream output = Files.newOutputStream(file);
             ZipOutputStream zipOutput = new ZipOutputStream(output)) {
            writePropertiesEntry(zipOutput, MANIFEST_ENTRY, manifest, "Pannkoogihommiku plaanipakett");
            writePropertiesEntry(zipOutput, PLAN_ENTRY, planProperties, "Pannkoogihommiku plaaniandmed");
            if (mapAsset != null) {
                zipOutput.putNextEntry(new ZipEntry(mapAsset.entryName()));
                zipOutput.write(mapAsset.data());
                zipOutput.closeEntry();
            }
        }
    }

    private void writePropertiesEntry(
            ZipOutputStream zipOutput,
            String entryName,
            Properties properties,
            String comment
    ) throws IOException {
        zipOutput.putNextEntry(new ZipEntry(entryName));
        properties.store(zipOutput, comment);
        zipOutput.closeEntry();
    }

    private void replaceFile(Path temporaryFile, Path target) throws IOException {
        try {
            Files.move(
                    temporaryFile,
                    target,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporaryFile, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private boolean isZipPackage(Path file) throws IOException {
        try (InputStream input = Files.newInputStream(file)) {
            byte[] signature = input.readNBytes(4);
            return signature.length == 4
                    && signature[0] == 'P'
                    && signature[1] == 'K'
                    && ((signature[2] == 3 && signature[3] == 4)
                    || (signature[2] == 5 && signature[3] == 6)
                    || (signature[2] == 7 && signature[3] == 8));
        }
    }

    private void validatePackageEntries(ZipFile zipFile) throws IOException {
        Set<String> entryNames = new HashSet<>();
        int entryCount = 0;
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            entryCount++;
            if (entryCount > MAX_PACKAGE_ENTRIES) {
                throw new IOException("Plaanipakett sisaldab liiga palju kirjeid.");
            }
            String name = entry.getName();
            if (name.isBlank()
                    || name.startsWith("/")
                    || name.startsWith("\\")
                    || name.contains("\\")
                    || name.equals("..")
                    || name.startsWith("../")
                    || name.endsWith("/..")
                    || name.contains("/../")) {
                throw new IOException("Plaanipakett sisaldab ebaturvalist kirje nime: " + name);
            }
            if (!entryNames.add(name)) {
                throw new IOException("Plaanipakett sisaldab korduvat kirjet: " + name);
            }
        }
    }

    private Properties readProperties(ZipFile zipFile, String entryName, long maximumBytes) throws IOException {
        byte[] data = readEntry(zipFile, entryName, maximumBytes);
        Properties properties = new Properties();
        try (InputStream input = new ByteArrayInputStream(data)) {
            properties.load(input);
        }
        return properties;
    }

    private byte[] readEntry(ZipFile zipFile, String entryName, long maximumBytes) throws IOException {
        ZipEntry entry = zipFile.getEntry(entryName);
        if (entry == null || entry.isDirectory()) {
            throw new IOException("Plaanipaketis puudub kirje: " + entryName);
        }
        if (entry.getSize() > maximumBytes) {
            throw new IOException("Plaanipaketi kirje on liiga suur: " + entryName);
        }

        try (InputStream input = zipFile.getInputStream(entry);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            long total = 0;
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                total += bytesRead;
                if (total > maximumBytes) {
                    throw new IOException("Plaanipaketi kirje on liiga suur: " + entryName);
                }
                output.write(buffer, 0, bytesRead);
            }
            return output.toByteArray();
        }
    }

    private MapAsset mapAssetFor(EventPlan plan) throws IOException {
        if (plan.hasPackagedMapImage()) {
            byte[] data = plan.packagedMapImage();
            validateMapImage(data);
            return new MapAsset(canonicalMapEntry(data), data);
        }

        String imagePath = plan.mapImagePath();
        if (imagePath == null || imagePath.isBlank() || imagePath.startsWith("classpath:")) {
            return null;
        }
        if (imagePath.startsWith("package:/")) {
            throw new IOException("Plaanis viidatud pakitud kaardipilt puudub.");
        }

        Path source = Path.of(imagePath);
        if (!Files.isRegularFile(source)) {
            throw new IOException("Kaardipilti ei leitud: " + imagePath);
        }
        if (Files.size(source) > MAX_MAP_BYTES) {
            throw new IOException("Kaardipilt on suurem kui 50 MB.");
        }
        byte[] data = Files.readAllBytes(source);
        validateMapImage(data);
        return new MapAsset(canonicalMapEntry(data), data);
    }

    private void validateMapEntryName(String entryName) throws IOException {
        if (!(entryName.equals("assets/map.png")
                || entryName.equals("assets/map.jpg")
                || entryName.equals("assets/map.jpeg"))) {
            throw new IOException("Plaanipaketi kaardipildi kirje nimi ei ole toetatud: " + entryName);
        }
    }

    private void validateMapImage(byte[] data) throws IOException {
        if (!isPng(data) && !isJpeg(data)) {
            throw new IOException("Kaardipilt ei ole toetatud PNG- või JPEG-fail.");
        }

        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
            if (input == null) {
                throw new IOException("Kaardipildi sisu ei ole loetav.");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new IOException("Kaardipildi sisu ei ole loetav.");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || (long) width * height > MAX_MAP_PIXELS) {
                    throw new IOException("Kaardipildi mõõtmed ei ole toetatud.");
                }
                reader.read(0);
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException exception) {
            throw new IOException("Kaardipildi sisu ei ole loetav.", exception);
        }
    }

    private String canonicalMapEntry(byte[] data) {
        return isPng(data) ? "assets/map.png" : "assets/map.jpg";
    }

    private boolean isPng(byte[] data) {
        byte[] signature = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
        if (data.length < signature.length) {
            return false;
        }
        for (int index = 0; index < signature.length; index++) {
            if (data[index] != signature[index]) {
                return false;
            }
        }
        return true;
    }

    private boolean isJpeg(byte[] data) {
        return data.length >= 3
                && data[0] == (byte) 0xFF
                && data[1] == (byte) 0xD8
                && data[2] == (byte) 0xFF;
    }

    private int parseFormatVersion(String value) throws IOException {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            throw new IOException("Plaanifaili vormingu versioon ei ole korrektne: " + value, exception);
        }
    }

    private IOException newerVersionException(int formatVersion) {
        return new IOException(
                "Plaanifail on loodud uuema rakenduse versiooniga "
                        + "(faili vorming " + formatVersion
                        + ", toetatud kuni " + CURRENT_FORMAT_VERSION + "). "
                        + "Faili avamiseks uuenda rakendust."
        );
    }

    private record MapAsset(String entryName, byte[] data) {
        private MapAsset {
            data = data.clone();
        }

        @Override
        public byte[] data() {
            return data.clone();
        }
    }

    private void writeObject(Properties properties, String prefix, PlannerObject object) {
        properties.setProperty(prefix + "id", object.id());
        properties.setProperty(prefix + "name", object.name());
        properties.setProperty(prefix + "x", Double.toString(object.position().x()));
        properties.setProperty(prefix + "y", Double.toString(object.position().y()));
        properties.setProperty(prefix + "locked", Boolean.toString(object.locked()));
        properties.setProperty(prefix + "groupName", object.groupName());
        properties.setProperty(prefix + "notes", object.notes());
        properties.setProperty(prefix + "showMapLabel", Boolean.toString(object.showMapLabel()));
        properties.setProperty(prefix + "customMapLabelPosition", Boolean.toString(object.customMapLabelPosition()));
        properties.setProperty(prefix + "mapLabelOffsetX", Double.toString(object.mapLabelOffset().x()));
        properties.setProperty(prefix + "mapLabelOffsetY", Double.toString(object.mapLabelOffset().y()));
        if (object instanceof PowerConnectable connectable) {
            properties.setProperty(prefix + "powerConnectionOffsetX", Double.toString(connectable.powerConnectionOffset().x()));
            properties.setProperty(prefix + "powerConnectionOffsetY", Double.toString(connectable.powerConnectionOffset().y()));
        }

        if (object instanceof Tent tent) {
            writeTent(properties, prefix, tent);
        } else if (object instanceof PowerSource source) {
            writePowerSource(properties, prefix, source);
        } else if (object instanceof MarkerObject markerObject) {
            writeMarkerObject(properties, prefix, markerObject);
        } else if (object instanceof TextObject textObject) {
            writeTextObject(properties, prefix, textObject);
        } else if (object instanceof AreaObject areaObject) {
            writeAreaObject(properties, prefix, areaObject);
        } else if (object instanceof LineObject lineObject) {
            writeLineObject(properties, prefix, lineObject);
        } else if (object instanceof CustomObject customObject) {
            writeCustomObject(properties, prefix, customObject);
        }
    }

    private void writeTent(Properties properties, String prefix, Tent tent) {
        properties.setProperty(prefix + "type", "TENT");
        properties.setProperty(prefix + "widthMeters", Double.toString(tent.widthMeters()));
        properties.setProperty(prefix + "heightMeters", Double.toString(tent.heightMeters()));
        properties.setProperty(prefix + "rotationDegrees", Double.toString(tent.rotationDegrees()));
        properties.setProperty(prefix + "colorHex", tent.colorHex());
        properties.setProperty(prefix + "opacity", Double.toString(tent.opacity()));
        writeEquipment(properties, prefix, tent);
    }

    private void writePowerSource(Properties properties, String prefix, PowerSource source) {
        properties.setProperty(prefix + "type", "POWER_SOURCE");
        properties.setProperty(prefix + "outlets.count", Integer.toString(source.outlets().size()));
        for (int index = 0; index < source.outlets().size(); index++) {
            PowerOutlet outlet = source.outlets().get(index);
            String outletPrefix = prefix + "outlet." + index + ".";
            properties.setProperty(outletPrefix + "id", outlet.id());
            properties.setProperty(outletPrefix + "name", outlet.name());
            properties.setProperty(outletPrefix + "type", outlet.type().name());
            properties.setProperty(outletPrefix + "capacityWatts", Integer.toString(outlet.capacityWatts()));
        }
    }

    private void writeCustomObject(Properties properties, String prefix, CustomObject object) {
        properties.setProperty(prefix + "type", "CUSTOM_OBJECT");
        properties.setProperty(prefix + "shape", object.shape().name());
        properties.setProperty(prefix + "colorHex", object.colorHex());
        properties.setProperty(prefix + "opacity", Double.toString(object.opacity()));
        properties.setProperty(prefix + "widthMeters", Double.toString(object.widthMeters()));
        properties.setProperty(prefix + "heightMeters", Double.toString(object.heightMeters()));
        properties.setProperty(prefix + "rotationDegrees", Double.toString(object.rotationDegrees()));
    }

    private void writeTextObject(Properties properties, String prefix, TextObject object) {
        properties.setProperty(prefix + "type", "TEXT_OBJECT");
        properties.setProperty(prefix + "colorHex", object.colorHex());
        properties.setProperty(prefix + "fontSize", Double.toString(object.fontSize()));
    }

    private void writeMarkerObject(Properties properties, String prefix, MarkerObject object) {
        properties.setProperty(prefix + "type", "MARKER_OBJECT");
        properties.setProperty(prefix + "markerType", object.markerType().name());
        properties.setProperty(prefix + "colorHex", object.colorHex());
    }

    private void writeAreaObject(Properties properties, String prefix, AreaObject object) {
        properties.setProperty(prefix + "type", "AREA_OBJECT");
        properties.setProperty(prefix + "colorHex", object.colorHex());
        properties.setProperty(prefix + "opacity", Double.toString(object.opacity()));
        writePoints(properties, prefix, object.points());
        writeEquipment(properties, prefix, object);
    }

    private void writeLineObject(Properties properties, String prefix, LineObject object) {
        properties.setProperty(prefix + "type", "LINE_OBJECT");
        properties.setProperty(prefix + "colorHex", object.colorHex());
        properties.setProperty(prefix + "widthPixels", Double.toString(object.widthPixels()));
        writePoints(properties, prefix, object.points());
        writeEquipment(properties, prefix, object);
    }

    private PlannerObject readObject(Properties properties, String prefix) {
        String type = properties.getProperty(prefix + "type", "TENT");
        PlannerObject object;
        if ("POWER_SOURCE".equals(type)) {
            object = readPowerSource(properties, prefix);
        } else if ("MARKER_OBJECT".equals(type)) {
            object = readMarkerObject(properties, prefix);
        } else if ("TEXT_OBJECT".equals(type)) {
            object = readTextObject(properties, prefix);
        } else if ("AREA_OBJECT".equals(type)) {
            object = readAreaObject(properties, prefix);
        } else if ("LINE_OBJECT".equals(type)) {
            object = readLineObject(properties, prefix);
        } else if ("CUSTOM_OBJECT".equals(type)) {
            object = readCustomObject(properties, prefix);
        } else {
            object = readTent(properties, prefix);
        }

        object.setGroupName(properties.getProperty(prefix + "groupName", ""));
        object.setNotes(properties.getProperty(prefix + "notes", ""));
        object.setLocked(Boolean.parseBoolean(properties.getProperty(prefix + "locked", "false")));
        object.setShowMapLabel(Boolean.parseBoolean(properties.getProperty(prefix + "showMapLabel", "true")));
        if (Boolean.parseBoolean(properties.getProperty(prefix + "customMapLabelPosition", "false"))) {
            object.setMapLabelOffset(new Position(
                    doubleValue(properties, prefix + "mapLabelOffsetX", 0),
                    doubleValue(properties, prefix + "mapLabelOffsetY", 0)
            ));
        }
        if (object instanceof PowerConnectable connectable) {
            connectable.setPowerConnectionOffset(new Position(
                    doubleValue(properties, prefix + "powerConnectionOffsetX", 0),
                    doubleValue(properties, prefix + "powerConnectionOffsetY", 0)
            ));
        }
        return object;
    }

    private Tent readTent(Properties properties, String prefix) {
        Tent tent = new Tent(
                properties.getProperty(prefix + "id", ""),
                properties.getProperty(prefix + "name", "Telk"),
                readPosition(properties, prefix)
        );
        tent.setSizeMeters(
                doubleValue(properties, prefix + "widthMeters", 3.0),
                doubleValue(properties, prefix + "heightMeters", 3.0)
        );
        tent.setRotationDegrees(doubleValue(properties, prefix + "rotationDegrees", 0));
        tent.setColorHex(properties.getProperty(prefix + "colorHex", "#e74c3c"));
        tent.setOpacity(doubleValue(properties, prefix + "opacity", Tent.DEFAULT_OPACITY));

        readEquipment(properties, prefix, tent);
        return tent;
    }

    private PowerSource readPowerSource(Properties properties, String prefix) {
        PowerSource source = new PowerSource(
                properties.getProperty(prefix + "id", ""),
                properties.getProperty(prefix + "name", "Kapp"),
                readPosition(properties, prefix)
        );

        int outletCount = intValue(properties, prefix + "outlets.count", 0);
        for (int index = 0; index < outletCount; index++) {
            String outletPrefix = prefix + "outlet." + index + ".";
            source.addOutlet(new PowerOutlet(
                    properties.getProperty(outletPrefix + "id", ""),
                    properties.getProperty(outletPrefix + "name", ""),
                    ConnectorType.valueOf(properties.getProperty(outletPrefix + "type", ConnectorType.SCHUKO_230V.name())),
                    intValue(properties, outletPrefix + "capacityWatts", 0)
            ));
        }
        return source;
    }

    private PlannerObject readCustomObject(Properties properties, String prefix) {
        String shapeName = properties.getProperty(prefix + "shape", CustomObjectShape.SQUARE.name());
        if ("TEXT".equals(shapeName)) {
            return readTextObject(properties, prefix);
        }
        CustomObject object = new CustomObject(
                properties.getProperty(prefix + "id", ""),
                properties.getProperty(prefix + "name", "Objekt"),
                readPosition(properties, prefix)
        );
        object.setShape(CustomObjectShape.valueOf(shapeName));
        object.setColorHex(properties.getProperty(prefix + "colorHex", "#9ca3af"));
        object.setOpacity(doubleValue(properties, prefix + "opacity", CustomObject.DEFAULT_OPACITY));
        object.setSizeMeters(
                doubleValue(properties, prefix + "widthMeters", 1.0),
                doubleValue(properties, prefix + "heightMeters", 1.0)
        );
        object.setRotationDegrees(doubleValue(properties, prefix + "rotationDegrees", 0));
        return object;
    }

    private TextObject readTextObject(Properties properties, String prefix) {
        TextObject object = new TextObject(
                properties.getProperty(prefix + "id", ""),
                properties.getProperty(prefix + "name", "Tekst"),
                readPosition(properties, prefix)
        );
        object.setColorHex(properties.getProperty(prefix + "colorHex", "#111827"));
        object.setFontSize(doubleValue(properties, prefix + "fontSize", TextObject.DEFAULT_FONT_SIZE));
        return object;
    }

    private MarkerObject readMarkerObject(Properties properties, String prefix) {
        MarkerObject object = new MarkerObject(
                properties.getProperty(prefix + "id", ""),
                properties.getProperty(prefix + "name", "Marker"),
                readPosition(properties, prefix)
        );
        MarkerType markerType = MarkerType.valueOf(properties.getProperty(prefix + "markerType", MarkerType.WC.name()));
        object.setMarkerType(markerType);
        object.setColorHex(properties.getProperty(prefix + "colorHex", markerType.defaultColorHex()));
        return object;
    }

    private AreaObject readAreaObject(Properties properties, String prefix) {
        AreaObject object = new AreaObject(
                properties.getProperty(prefix + "id", ""),
                properties.getProperty(prefix + "name", "Ala"),
                readPosition(properties, prefix)
        );
        object.setColorHex(properties.getProperty(prefix + "colorHex", "#f59e0b"));
        object.setOpacity(doubleValue(properties, prefix + "opacity", AreaObject.DEFAULT_OPACITY));
        object.setPoints(readPoints(properties, prefix));
        readEquipment(properties, prefix, object);
        return object;
    }

    private LineObject readLineObject(Properties properties, String prefix) {
        LineObject object = new LineObject(
                properties.getProperty(prefix + "id", ""),
                properties.getProperty(prefix + "name", "Joon"),
                readPosition(properties, prefix)
        );
        object.setColorHex(properties.getProperty(prefix + "colorHex", "#0f766e"));
        object.setWidthPixels(doubleValue(properties, prefix + "widthPixels", LineObject.DEFAULT_WIDTH_PIXELS));
        object.setPoints(readPoints(properties, prefix));
        readEquipment(properties, prefix, object);
        return object;
    }

    private void writeEquipment(Properties properties, String prefix, EquipmentContainer container) {
        properties.setProperty(prefix + "equipment.count", Integer.toString(container.equipment().size()));
        for (int index = 0; index < container.equipment().size(); index++) {
            Equipment item = container.equipment().get(index);
            String equipmentPrefix = prefix + "equipment." + index + ".";
            properties.setProperty(equipmentPrefix + "name", item.name());
            properties.setProperty(equipmentPrefix + "requiredWatts", Integer.toString(item.requiredWatts()));
        }
    }

    private void readEquipment(Properties properties, String prefix, EquipmentContainer container) {
        int equipmentCount = intValue(properties, prefix + "equipment.count", 0);
        for (int index = 0; index < equipmentCount; index++) {
            String equipmentPrefix = prefix + "equipment." + index + ".";
            container.addEquipment(new Equipment(
                    properties.getProperty(equipmentPrefix + "name", "Seade"),
                    intValue(properties, equipmentPrefix + "requiredWatts", 0)
            ));
        }
    }

    private Position readPosition(Properties properties, String prefix) {
        return new Position(
                doubleValue(properties, prefix + "x", 0),
                doubleValue(properties, prefix + "y", 0)
        );
    }

    private List<Position> readRoutePoints(Properties properties, String prefix) {
        List<Position> routePoints = new java.util.ArrayList<>();
        int routePointCount = intValue(properties, prefix + "routePoints.count", 0);
        for (int index = 0; index < routePointCount; index++) {
            routePoints.add(readPosition(properties, prefix + "routePoint." + index + "."));
        }
        return routePoints;
    }

    private void writePoints(Properties properties, String prefix, List<Position> points) {
        properties.setProperty(prefix + "points.count", Integer.toString(points.size()));
        for (int index = 0; index < points.size(); index++) {
            Position point = points.get(index);
            String pointPrefix = prefix + "point." + index + ".";
            properties.setProperty(pointPrefix + "x", Double.toString(point.x()));
            properties.setProperty(pointPrefix + "y", Double.toString(point.y()));
        }
    }

    private List<Position> readPoints(Properties properties, String prefix) {
        List<Position> points = new java.util.ArrayList<>();
        int pointCount = intValue(properties, prefix + "points.count", 0);
        for (int index = 0; index < pointCount; index++) {
            points.add(readPosition(properties, prefix + "point." + index + "."));
        }
        return points;
    }

    private int intValue(Properties properties, String key, int fallback) {
        return Integer.parseInt(properties.getProperty(key, Integer.toString(fallback)));
    }

    private double doubleValue(Properties properties, String key, double fallback) {
        return Double.parseDouble(properties.getProperty(key, Double.toString(fallback)));
    }

    private boolean booleanValue(Properties properties, String key, boolean fallback) {
        return Boolean.parseBoolean(properties.getProperty(key, Boolean.toString(fallback)));
    }
}
