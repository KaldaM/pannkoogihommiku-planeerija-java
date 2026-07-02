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

    public void connectToPower(String sourceId, String consumerId, ConnectorType connectorType) {
        powerConnections.removeIf(connection -> connection.consumerId().equals(consumerId));
        powerConnections.add(new PowerConnection(sourceId, consumerId, connectorType));
    }

    public void disconnectPower(String consumerId) {
        powerConnections.removeIf(connection -> connection.consumerId().equals(consumerId));
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
