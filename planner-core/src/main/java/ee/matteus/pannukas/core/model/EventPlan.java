package ee.matteus.pannukas.core.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class EventPlan {
    private String name;
    private String mapImagePath;
    private final List<PlannerObject> objects = new ArrayList<>();
    private final List<PowerConnection> powerConnections = new ArrayList<>();
    private final Set<String> hiddenGroups = new HashSet<>();

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
    }

    public void addObject(PlannerObject object) {
        objects.add(object);
    }

    public void removeObject(String objectId) {
        objects.removeIf(object -> object.id().equals(objectId));
        powerConnections.removeIf(connection ->
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

    public Optional<PlannerObject> findObject(String id) {
        return objects.stream().filter(object -> object.id().equals(id)).findFirst();
    }

    public Optional<PowerConnection> connectToPower(String sourceId, String consumerId, ConnectorType connectorType) {
        return connectToPower(sourceId, consumerId, connectorType, "");
    }

    public Optional<PowerConnection> connectToPower(String sourceId, String consumerId, ConnectorType connectorType, String outletId) {
        PowerSource source = findObject(sourceId)
                .filter(PowerSource.class::isInstance)
                .map(PowerSource.class::cast)
                .orElse(null);
        if (source == null) {
            return Optional.empty();
        }

        ConnectorType selectedType = connectorType == null ? ConnectorType.SCHUKO_230V : connectorType;
        Optional<PowerOutlet> selectedOutlet = selectOutlet(source, consumerId, selectedType, outletId);
        if (selectedOutlet.isEmpty()) {
            return Optional.empty();
        }

        powerConnections.removeIf(connection -> connection.consumerId().equals(consumerId));
        PowerConnection connection = new PowerConnection(sourceId, consumerId, selectedType, selectedOutlet.get().id());
        powerConnections.add(connection);
        return Optional.of(connection);
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
        powerConnections.removeIf(connection -> connection.consumerId().equals(consumerId));
    }

    public void disconnectPowerFromOutlet(String outletId) {
        powerConnections.removeIf(connection -> connection.outletId().equals(outletId));
    }

    public void updateConnectorTypeForOutlet(String outletId, ConnectorType connectorType) {
        ConnectorType selectedType = connectorType == null ? ConnectorType.SCHUKO_230V : connectorType;
        for (int index = 0; index < powerConnections.size(); index++) {
            PowerConnection connection = powerConnections.get(index);
            if (connection.outletId().equals(outletId)) {
                powerConnections.set(index, new PowerConnection(
                        connection.sourceId(),
                        connection.consumerId(),
                        selectedType,
                        connection.outletId()
                ));
            }
        }
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
