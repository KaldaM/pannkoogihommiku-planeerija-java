package ee.matteus.pannukas.core.model;

public record PowerConnection(String sourceId, String consumerId, ConnectorType connectorType, String outletId, String cableNotes) {
    public PowerConnection(String sourceId, String consumerId, ConnectorType connectorType) {
        this(sourceId, consumerId, connectorType, "");
    }

    public PowerConnection(String sourceId, String consumerId, ConnectorType connectorType, String outletId) {
        this(sourceId, consumerId, connectorType, outletId, "");
    }

    public PowerConnection {
        cableNotes = cableNotes == null ? "" : cableNotes.trim();
    }
}
