package ee.matteus.pannukas.core.model;

public record PowerConnection(String sourceId, String consumerId, ConnectorType connectorType, String outletId) {
    public PowerConnection(String sourceId, String consumerId, ConnectorType connectorType) {
        this(sourceId, consumerId, connectorType, "");
    }
}
