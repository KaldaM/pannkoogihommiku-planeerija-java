package ee.matteus.pannukas.core.service;

import ee.matteus.pannukas.core.model.ConnectorType;
import ee.matteus.pannukas.core.model.Equipment;
import ee.matteus.pannukas.core.model.EventPlan;
import ee.matteus.pannukas.core.model.PlannerObject;
import ee.matteus.pannukas.core.model.Position;
import ee.matteus.pannukas.core.model.PowerConnection;
import ee.matteus.pannukas.core.model.PowerOutlet;
import ee.matteus.pannukas.core.model.PowerSource;
import ee.matteus.pannukas.core.model.Tent;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

public class PlanFileService {
    public void save(EventPlan plan, Path file) throws IOException {
        Properties properties = new Properties();
        properties.setProperty("format", "pannukas-plan-v1");
        properties.setProperty("plan.name", plan.name());
        properties.setProperty("plan.mapImagePath", plan.mapImagePath());
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
        }

        try (OutputStream output = Files.newOutputStream(file)) {
            properties.store(output, "Pannkoogihommiku planeerija");
        }
    }

    public EventPlan load(Path file) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(file)) {
            properties.load(input);
        }

        EventPlan plan = new EventPlan(properties.getProperty("plan.name", "Pannkoogihommik"));
        plan.setMapImagePath(properties.getProperty("plan.mapImagePath", ""));

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
                    ConnectorType.valueOf(properties.getProperty(prefix + "connectorType", ConnectorType.SCHUKO_230V.name()))
            );
        }

        return plan;
    }

    private void writeObject(Properties properties, String prefix, PlannerObject object) {
        properties.setProperty(prefix + "id", object.id());
        properties.setProperty(prefix + "name", object.name());
        properties.setProperty(prefix + "x", Double.toString(object.position().x()));
        properties.setProperty(prefix + "y", Double.toString(object.position().y()));
        properties.setProperty(prefix + "locked", Boolean.toString(object.locked()));
        properties.setProperty(prefix + "groupName", object.groupName());
        properties.setProperty(prefix + "notes", object.notes());

        if (object instanceof Tent tent) {
            writeTent(properties, prefix, tent);
        } else if (object instanceof PowerSource source) {
            writePowerSource(properties, prefix, source);
        }
    }

    private void writeTent(Properties properties, String prefix, Tent tent) {
        properties.setProperty(prefix + "type", "TENT");
        properties.setProperty(prefix + "widthMeters", Double.toString(tent.widthMeters()));
        properties.setProperty(prefix + "heightMeters", Double.toString(tent.heightMeters()));
        properties.setProperty(prefix + "rotationDegrees", Double.toString(tent.rotationDegrees()));
        properties.setProperty(prefix + "colorHex", tent.colorHex());
        properties.setProperty(prefix + "equipment.count", Integer.toString(tent.equipment().size()));
        for (int index = 0; index < tent.equipment().size(); index++) {
            Equipment item = tent.equipment().get(index);
            String equipmentPrefix = prefix + "equipment." + index + ".";
            properties.setProperty(equipmentPrefix + "name", item.name());
            properties.setProperty(equipmentPrefix + "requiredWatts", Integer.toString(item.requiredWatts()));
        }
    }

    private void writePowerSource(Properties properties, String prefix, PowerSource source) {
        properties.setProperty(prefix + "type", "POWER_SOURCE");
        properties.setProperty(prefix + "outlets.count", Integer.toString(source.outlets().size()));
        for (int index = 0; index < source.outlets().size(); index++) {
            PowerOutlet outlet = source.outlets().get(index);
            String outletPrefix = prefix + "outlet." + index + ".";
            properties.setProperty(outletPrefix + "id", outlet.id());
            properties.setProperty(outletPrefix + "type", outlet.type().name());
            properties.setProperty(outletPrefix + "capacityWatts", Integer.toString(outlet.capacityWatts()));
        }
    }

    private PlannerObject readObject(Properties properties, String prefix) {
        String type = properties.getProperty(prefix + "type", "TENT");
        PlannerObject object;
        if ("POWER_SOURCE".equals(type)) {
            object = readPowerSource(properties, prefix);
        } else {
            object = readTent(properties, prefix);
        }

        object.setGroupName(properties.getProperty(prefix + "groupName", ""));
        object.setNotes(properties.getProperty(prefix + "notes", ""));
        object.setLocked(Boolean.parseBoolean(properties.getProperty(prefix + "locked", "false")));
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

        int equipmentCount = intValue(properties, prefix + "equipment.count", 0);
        for (int index = 0; index < equipmentCount; index++) {
            String equipmentPrefix = prefix + "equipment." + index + ".";
            tent.addEquipment(new Equipment(
                    properties.getProperty(equipmentPrefix + "name", "Seade"),
                    intValue(properties, equipmentPrefix + "requiredWatts", 0)
            ));
        }
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
                    ConnectorType.valueOf(properties.getProperty(outletPrefix + "type", ConnectorType.SCHUKO_230V.name())),
                    intValue(properties, outletPrefix + "capacityWatts", 0)
            ));
        }
        return source;
    }

    private Position readPosition(Properties properties, String prefix) {
        return new Position(
                doubleValue(properties, prefix + "x", 0),
                doubleValue(properties, prefix + "y", 0)
        );
    }

    private int intValue(Properties properties, String key, int fallback) {
        return Integer.parseInt(properties.getProperty(key, Integer.toString(fallback)));
    }

    private double doubleValue(Properties properties, String key, double fallback) {
        return Double.parseDouble(properties.getProperty(key, Double.toString(fallback)));
    }
}
