package ee.matteus.plaanisepp.core.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class EventPlan {
    public static final double DEFAULT_PIXELS_PER_METER = 24.0;
    public static final double DEFAULT_OBJECT_LABEL_FONT_SIZE = 12.0;
    public static final double DEFAULT_CABLE_LABEL_FONT_SIZE = 12.0;

    private String name;
    private String mapImagePath;
    private String packagedMapImageEntry = "";
    private byte[] packagedMapImage = new byte[0];
    private double pixelsPerMeter = DEFAULT_PIXELS_PER_METER;
    private double objectLabelFontSize = DEFAULT_OBJECT_LABEL_FONT_SIZE;
    private double cableLabelFontSize = DEFAULT_CABLE_LABEL_FONT_SIZE;
    private final List<PlannerObject> objects = new ArrayList<>();
    private final List<PowerConnection> powerConnections = new ArrayList<>();
    private final Set<String> hiddenGroups = new HashSet<>();
    private boolean showCables = true;
    private boolean showCableLabels = true;
    private boolean show230VCables = true;
    private boolean show16ACables = true;
    private boolean show32ACables = true;
    private boolean show63ACables = true;
    private boolean showObjectLabels = true;
    private boolean showTents = true;
    private boolean showPowerSources = true;
    private boolean showCustomObjects = true;
    private boolean showTextObjects = true;
    private boolean showMarkerObjects = true;
    private boolean showAreaObjects = true;
    private boolean showLineObjects = true;

    public EventPlan(String name) {
        this.name = name;
        this.mapImagePath = "";
    }

    public String name() {
        return name;
    }

    public void rename(String name) {
        this.name = name;
    }

    public String mapImagePath() {
        return mapImagePath;
    }

    public void setMapImagePath(String mapImagePath) {
        this.mapImagePath = mapImagePath == null ? "" : mapImagePath;
        clearPackagedMapImage();
    }

    public boolean hasPackagedMapImage() {
        return packagedMapImage.length > 0;
    }

    public String packagedMapImageEntry() {
        return packagedMapImageEntry;
    }

    public byte[] packagedMapImage() {
        return packagedMapImage.clone();
    }

    public void setPackagedMapImage(String entryName, byte[] imageData) {
        if (entryName == null || entryName.isBlank()) {
            throw new IllegalArgumentException("Pakitud kaardipildi kirje nimi ei tohi olla tühi.");
        }
        if (imageData == null || imageData.length == 0) {
            throw new IllegalArgumentException("Pakitud kaardipilt ei tohi olla tühi.");
        }
        packagedMapImageEntry = entryName;
        packagedMapImage = imageData.clone();
        mapImagePath = "package:/" + entryName;
    }

    public void clearPackagedMapImage() {
        packagedMapImageEntry = "";
        packagedMapImage = new byte[0];
    }

    public double pixelsPerMeter() {
        return pixelsPerMeter;
    }

    public void setPixelsPerMeter(double pixelsPerMeter) {
        if (pixelsPerMeter <= 0) {
            throw new IllegalArgumentException("Pikslit meetri kohta peab olema positiivne.");
        }
        this.pixelsPerMeter = pixelsPerMeter;
    }

    public double objectLabelFontSize() {
        return objectLabelFontSize;
    }

    public void setObjectLabelFontSize(double objectLabelFontSize) {
        if (objectLabelFontSize <= 0) {
            throw new IllegalArgumentException("Objektisildi teksti suurus peab olema positiivne.");
        }
        this.objectLabelFontSize = objectLabelFontSize;
    }

    public double cableLabelFontSize() {
        return cableLabelFontSize;
    }

    public void setCableLabelFontSize(double cableLabelFontSize) {
        if (cableLabelFontSize <= 0) {
            throw new IllegalArgumentException("Kaablisildi teksti suurus peab olema positiivne.");
        }
        this.cableLabelFontSize = cableLabelFontSize;
    }

    public void addObject(PlannerObject object) {
        objects.add(object);
    }

    public void removeObject(String objectId) {
        objects.removeIf(object -> object.id().equals(objectId));
        removePowerConnections(connection ->
                connection.sourceId().equals(objectId) || connection.consumerId().equals(objectId));
    }

    public List<PlannerObject> objects() {
        return Collections.unmodifiableList(objects);
    }

    public List<PowerSource> powerSources() {
        return objects.stream()
                .filter(PowerSource.class::isInstance)
                .map(PowerSource.class::cast)
                .toList();
    }

    public List<Tent> tents() {
        return objects.stream()
                .filter(Tent.class::isInstance)
                .map(Tent.class::cast)
                .toList();
    }

    public List<PowerConsumer> powerConsumers() {
        return objects.stream()
                .filter(PowerConsumer.class::isInstance)
                .map(PowerConsumer.class::cast)
                .toList();
    }

    public List<AreaObject> areaObjects() {
        return objects.stream()
                .filter(AreaObject.class::isInstance)
                .map(AreaObject.class::cast)
                .toList();
    }

    public List<LineObject> lineObjects() {
        return objects.stream()
                .filter(LineObject.class::isInstance)
                .map(LineObject.class::cast)
                .toList();
    }

    public Optional<PlannerObject> findObject(String id) {
        return objects.stream().filter(object -> object.id().equals(id)).findFirst();
    }

    public boolean showCables() {
        return showCables;
    }

    public void setShowCables(boolean showCables) {
        this.showCables = showCables;
    }

    public boolean showCableLabels() {
        return showCableLabels;
    }

    public void setShowCableLabels(boolean showCableLabels) {
        this.showCableLabels = showCableLabels;
    }

    public boolean showCableType(ConnectorType connectorType) {
        return switch (connectorType) {
            case SCHUKO_230V -> show230VCables;
            case INDUSTRIAL_16A -> show16ACables;
            case INDUSTRIAL_32A -> show32ACables;
            case INDUSTRIAL_63A -> show63ACables;
        };
    }

    public void setShowCableType(ConnectorType connectorType, boolean visible) {
        switch (connectorType) {
            case SCHUKO_230V -> show230VCables = visible;
            case INDUSTRIAL_16A -> show16ACables = visible;
            case INDUSTRIAL_32A -> show32ACables = visible;
            case INDUSTRIAL_63A -> show63ACables = visible;
        }
    }

    public boolean showObjectLabels() {
        return showObjectLabels;
    }

    public void setShowObjectLabels(boolean showObjectLabels) {
        this.showObjectLabels = showObjectLabels;
    }

    public boolean showTents() {
        return showTents;
    }

    public void setShowTents(boolean showTents) {
        this.showTents = showTents;
    }

    public boolean showPowerSources() {
        return showPowerSources;
    }

    public void setShowPowerSources(boolean showPowerSources) {
        this.showPowerSources = showPowerSources;
    }

    public boolean showCustomObjects() {
        return showCustomObjects;
    }

    public void setShowCustomObjects(boolean showCustomObjects) {
        this.showCustomObjects = showCustomObjects;
    }

    public boolean showTextObjects() {
        return showTextObjects;
    }

    public void setShowTextObjects(boolean showTextObjects) {
        this.showTextObjects = showTextObjects;
    }

    public boolean showMarkerObjects() {
        return showMarkerObjects;
    }

    public void setShowMarkerObjects(boolean showMarkerObjects) {
        this.showMarkerObjects = showMarkerObjects;
    }

    public boolean showAreaObjects() {
        return showAreaObjects;
    }

    public void setShowAreaObjects(boolean showAreaObjects) {
        this.showAreaObjects = showAreaObjects;
    }

    public boolean showLineObjects() {
        return showLineObjects;
    }

    public void setShowLineObjects(boolean showLineObjects) {
        this.showLineObjects = showLineObjects;
    }

    public Optional<PowerConnection> connectToPower(String sourceId, String consumerId, ConnectorType connectorType) {
        return connectToPower(sourceId, consumerId, connectorType, "");
    }

    public Optional<PowerConnection> connectToPower(String sourceId, String consumerId, ConnectorType connectorType, String outletId) {
        return connectToPower(sourceId, consumerId, connectorType, outletId, existingCableNotes(consumerId));
    }

    public Optional<PowerConnection> connectToPower(String sourceId, String consumerId, ConnectorType connectorType, String outletId, String cableNotes) {
        return connectToPower(sourceId, consumerId, connectorType, outletId, cableNotes, existingCableLengthNotes(consumerId));
    }

    public Optional<PowerConnection> connectToPower(
            String sourceId,
            String consumerId,
            ConnectorType connectorType,
            String outletId,
            String cableNotes,
            String cableLengthNotes
    ) {
        return connectToPower(sourceId, consumerId, connectorType, outletId, cableNotes, cableLengthNotes, "");
    }

    public Optional<PowerConnection> connectToPower(
            String sourceId,
            String consumerId,
            ConnectorType connectorType,
            String outletId,
            String cableNotes,
            String cableLengthNotes,
            String connectionId
    ) {
        PowerSource source = findObject(sourceId)
                .filter(PowerSource.class::isInstance)
                .map(PowerSource.class::cast)
                .orElse(null);
        if (source == null) {
            return Optional.empty();
        }

        PowerConsumer consumer = findObject(consumerId)
                .filter(PowerConsumer.class::isInstance)
                .map(PowerConsumer.class::cast)
                .orElse(null);
        if (consumer == null) {
            return Optional.empty();
        }

        ConnectorType selectedType = connectorType == null ? ConnectorType.SCHUKO_230V : connectorType;
        Optional<PowerOutlet> selectedOutlet = selectOutlet(source, consumerId, selectedType, outletId);
        if (selectedOutlet.isEmpty()) {
            return Optional.empty();
        }

        List<Position> existingRoutePoints = existingCableRoutePoints(consumerId);
        PowerConnection existingConnection = findPowerConnectionForConsumer(consumerId).orElse(null);
        String selectedConnectionId = connectionId == null || connectionId.isBlank()
                ? existingConnection == null ? "" : existingConnection.id()
                : connectionId;
        powerConnections.removeIf(connection -> connection.consumerId().equals(consumerId));
        PowerConnection connection = new PowerConnection(
                selectedConnectionId,
                sourceId,
                consumerId,
                selectedType,
                selectedOutlet.get().id(),
                cableNotes,
                cableLengthNotes,
                existingRoutePoints,
                existingConnection != null && existingConnection.customCableLabelPosition(),
                existingConnection == null ? new Position(0, 0) : existingConnection.cableLabelOffset()
        );
        powerConnections.add(connection);
        return Optional.of(connection);
    }

    public void updateCableNotes(String consumerId, String cableNotes) {
        for (int index = 0; index < powerConnections.size(); index++) {
            PowerConnection connection = powerConnections.get(index);
            if (connection.consumerId().equals(consumerId)) {
                powerConnections.set(index, new PowerConnection(
                        connection.id(),
                        connection.sourceId(),
                        connection.consumerId(),
                        connection.connectorType(),
                        connection.outletId(),
                        cableNotes,
                        connection.cableLengthNotes(),
                        connection.routePoints(),
                        connection.customCableLabelPosition(),
                        connection.cableLabelOffset()
                ));
                return;
            }
        }
    }

    public void updateCableLengthNotes(String consumerId, String cableLengthNotes) {
        for (int index = 0; index < powerConnections.size(); index++) {
            PowerConnection connection = powerConnections.get(index);
            if (connection.consumerId().equals(consumerId)) {
                powerConnections.set(index, new PowerConnection(
                        connection.id(),
                        connection.sourceId(),
                        connection.consumerId(),
                        connection.connectorType(),
                        connection.outletId(),
                        connection.cableNotes(),
                        cableLengthNotes,
                        connection.routePoints(),
                        connection.customCableLabelPosition(),
                        connection.cableLabelOffset()
                ));
                return;
            }
        }
    }

    private String existingCableNotes(String consumerId) {
        return findPowerConnectionForConsumer(consumerId)
                .map(PowerConnection::cableNotes)
                .orElse("");
    }

    private String existingCableLengthNotes(String consumerId) {
        return findPowerConnectionForConsumer(consumerId)
                .map(PowerConnection::cableLengthNotes)
                .orElse("");
    }

    private List<Position> existingCableRoutePoints(String consumerId) {
        return findPowerConnectionForConsumer(consumerId)
                .map(PowerConnection::routePoints)
                .orElse(List.of());
    }

    private Optional<PowerOutlet> selectOutlet(PowerSource source, String consumerId, ConnectorType connectorType, String outletId) {
        if (outletId != null && !outletId.isBlank()) {
            Optional<PowerOutlet> requestedOutlet = source.outlets().stream()
                    .filter(outlet -> outlet.id().equals(outletId))
                    .filter(outlet -> outlet.type() == connectorType)
                    .findFirst();
            if (requestedOutlet.isPresent()) {
                return requestedOutlet;
            }
        }

        List<PowerOutlet> matchingOutlets = source.outlets().stream()
                .filter(outlet -> outlet.type() == connectorType)
                .toList();
        if (matchingOutlets.isEmpty()) {
            return Optional.empty();
        }

        int requiredWatts = findObject(consumerId)
                .filter(PowerConsumer.class::isInstance)
                .map(PowerConsumer.class::cast)
                .map(PowerConsumer::requiredWatts)
                .orElse(0);
        return matchingOutlets.stream()
                .filter(outlet -> outlet.capacityWatts() - usedWatts(source.id(), outlet.id(), consumerId) >= requiredWatts)
                .findFirst()
                .or(() -> matchingOutlets.stream().findFirst());
    }

    private int usedWatts(String sourceId, String outletId, String ignoredConsumerId) {
        return powerConnections.stream()
                .filter(connection -> connection.sourceId().equals(sourceId))
                .filter(connection -> connection.outletId().equals(outletId))
                .filter(connection -> !connection.consumerId().equals(ignoredConsumerId))
                .map(connection -> findObject(connection.consumerId()))
                .flatMap(Optional::stream)
                .filter(PowerConsumer.class::isInstance)
                .map(PowerConsumer.class::cast)
                .mapToInt(PowerConsumer::requiredWatts)
                .sum();
    }

    public void disconnectPower(String consumerId) {
        removePowerConnections(connection -> connection.consumerId().equals(consumerId));
    }

    public void disconnectPowerFromOutlet(String outletId) {
        removePowerConnections(connection -> connection.outletId().equals(outletId));
    }

    public EquipmentPowerAssignmentResult assignEquipmentToPowerConnection(
            String containerId,
            String equipmentId,
            String connectionId
    ) {
        EquipmentContainer container = findEquipmentContainer(containerId).orElse(null);
        if (container == null) {
            return EquipmentPowerAssignmentResult.CONTAINER_NOT_FOUND;
        }

        Equipment equipment = container.equipment().stream()
                .filter(item -> item.id().equals(equipmentId))
                .findFirst()
                .orElse(null);
        if (equipment == null) {
            return EquipmentPowerAssignmentResult.EQUIPMENT_NOT_FOUND;
        }

        PowerConnection connection = powerConnections.stream()
                .filter(item -> item.id().equals(connectionId))
                .findFirst()
                .orElse(null);
        if (connection == null) {
            return EquipmentPowerAssignmentResult.CONNECTION_NOT_FOUND;
        }
        if (!connection.consumerId().equals(containerId)) {
            return EquipmentPowerAssignmentResult.CONNECTION_BELONGS_TO_ANOTHER_CONSUMER;
        }

        equipment.assignPowerConnection(connection.id());
        return EquipmentPowerAssignmentResult.SUCCESS;
    }

    public EquipmentPowerAssignmentResult useDefaultPowerForEquipment(String containerId, String equipmentId) {
        EquipmentContainer container = findEquipmentContainer(containerId).orElse(null);
        if (container == null) {
            return EquipmentPowerAssignmentResult.CONTAINER_NOT_FOUND;
        }

        Equipment equipment = container.equipment().stream()
                .filter(item -> item.id().equals(equipmentId))
                .findFirst()
                .orElse(null);
        if (equipment == null) {
            return EquipmentPowerAssignmentResult.EQUIPMENT_NOT_FOUND;
        }

        equipment.useDefaultPower();
        return EquipmentPowerAssignmentResult.SUCCESS;
    }

    private Optional<EquipmentContainer> findEquipmentContainer(String containerId) {
        return findObject(containerId)
                .filter(EquipmentContainer.class::isInstance)
                .map(EquipmentContainer.class::cast);
    }

    private void removePowerConnections(Predicate<PowerConnection> predicate) {
        Set<String> removedConnectionIds = powerConnections.stream()
                .filter(predicate)
                .map(PowerConnection::id)
                .collect(Collectors.toSet());
        powerConnections.removeIf(predicate);
        if (removedConnectionIds.isEmpty()) {
            return;
        }
        objects.stream()
                .filter(EquipmentContainer.class::isInstance)
                .map(EquipmentContainer.class::cast)
                .flatMap(container -> container.equipment().stream())
                .filter(equipment -> removedConnectionIds.contains(equipment.powerConnectionId()))
                .forEach(Equipment::useDefaultPower);
    }

    public void updateConnectorTypeForOutlet(String outletId, ConnectorType connectorType) {
        ConnectorType selectedType = connectorType == null ? ConnectorType.SCHUKO_230V : connectorType;
        for (int index = 0; index < powerConnections.size(); index++) {
            PowerConnection connection = powerConnections.get(index);
            if (connection.outletId().equals(outletId)) {
                powerConnections.set(index, new PowerConnection(
                        connection.id(),
                        connection.sourceId(),
                        connection.consumerId(),
                        selectedType,
                        connection.outletId(),
                        connection.cableNotes(),
                        connection.cableLengthNotes(),
                        connection.routePoints(),
                        connection.customCableLabelPosition(),
                        connection.cableLabelOffset()
                ));
            }
        }
    }

    public void addCableRoutePoint(String consumerId, Position point) {
        insertCableRoutePoint(consumerId, -1, point);
    }

    public void insertCableRoutePoint(String consumerId, int routePointIndex, Position point) {
        if (point == null) {
            return;
        }
        for (int index = 0; index < powerConnections.size(); index++) {
            PowerConnection connection = powerConnections.get(index);
            if (connection.consumerId().equals(consumerId)) {
                List<Position> routePoints = new ArrayList<>(connection.routePoints());
                if (routePointIndex < 0 || routePointIndex > routePoints.size()) {
                    routePoints.add(point);
                } else {
                    routePoints.add(routePointIndex, point);
                }
                powerConnections.set(index, new PowerConnection(
                        connection.id(),
                        connection.sourceId(),
                        connection.consumerId(),
                        connection.connectorType(),
                        connection.outletId(),
                        connection.cableNotes(),
                        connection.cableLengthNotes(),
                        routePoints,
                        connection.customCableLabelPosition(),
                        connection.cableLabelOffset()
                ));
                return;
            }
        }
    }

    public void updateCableRoutePoints(String consumerId, List<Position> routePoints) {
        for (int index = 0; index < powerConnections.size(); index++) {
            PowerConnection connection = powerConnections.get(index);
            if (connection.consumerId().equals(consumerId)) {
                powerConnections.set(index, new PowerConnection(
                        connection.id(),
                        connection.sourceId(),
                        connection.consumerId(),
                        connection.connectorType(),
                        connection.outletId(),
                        connection.cableNotes(),
                        connection.cableLengthNotes(),
                        routePoints,
                        connection.customCableLabelPosition(),
                        connection.cableLabelOffset()
                ));
                return;
            }
        }
    }

    public void updateCableLabelOffset(String consumerId, Position offset) {
        for (int index = 0; index < powerConnections.size(); index++) {
            PowerConnection connection = powerConnections.get(index);
            if (connection.consumerId().equals(consumerId)) {
                powerConnections.set(index, new PowerConnection(
                        connection.id(),
                        connection.sourceId(),
                        connection.consumerId(),
                        connection.connectorType(),
                        connection.outletId(),
                        connection.cableNotes(),
                        connection.cableLengthNotes(),
                        connection.routePoints(),
                        true,
                        offset
                ));
                return;
            }
        }
    }

    public void resetCableLabelOffset(String consumerId) {
        for (int index = 0; index < powerConnections.size(); index++) {
            PowerConnection connection = powerConnections.get(index);
            if (connection.consumerId().equals(consumerId)) {
                powerConnections.set(index, new PowerConnection(
                        connection.id(),
                        connection.sourceId(),
                        connection.consumerId(),
                        connection.connectorType(),
                        connection.outletId(),
                        connection.cableNotes(),
                        connection.cableLengthNotes(),
                        connection.routePoints(),
                        false,
                        new Position(0, 0)
                ));
                return;
            }
        }
    }

    public void clearCableRoutePoints(String consumerId) {
        updateCableRoutePoints(consumerId, List.of());
    }

    public Optional<PowerConnection> findPowerConnectionForConsumer(String consumerId) {
        return powerConnections.stream()
                .filter(connection -> connection.consumerId().equals(consumerId))
                .findFirst();
    }

    public List<PowerConnection> powerConnections() {
        return Collections.unmodifiableList(powerConnections);
    }

    public Set<String> hiddenGroups() {
        return Collections.unmodifiableSet(hiddenGroups);
    }

    public void setGroupHidden(String groupName, boolean hidden) {
        if (groupName == null || groupName.isBlank()) {
            return;
        }
        if (hidden) {
            hiddenGroups.add(groupName);
        } else {
            hiddenGroups.remove(groupName);
        }
    }

    public void clearHiddenGroups() {
        hiddenGroups.clear();
    }
}
