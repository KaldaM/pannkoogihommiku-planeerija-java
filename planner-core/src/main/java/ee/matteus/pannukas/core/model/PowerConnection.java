package ee.matteus.pannukas.core.model;

import java.util.List;

public record PowerConnection(
        String sourceId,
        String consumerId,
        ConnectorType connectorType,
        String outletId,
        String cableNotes,
        String cableLengthNotes,
        List<Position> routePoints,
        boolean customCableLabelPosition,
        Position cableLabelOffset
) {
    public PowerConnection(String sourceId, String consumerId, ConnectorType connectorType) {
        this(sourceId, consumerId, connectorType, "");
    }

    public PowerConnection(String sourceId, String consumerId, ConnectorType connectorType, String outletId) {
        this(sourceId, consumerId, connectorType, outletId, "");
    }

    public PowerConnection(String sourceId, String consumerId, ConnectorType connectorType, String outletId, String cableNotes) {
        this(sourceId, consumerId, connectorType, outletId, cableNotes, "");
    }

    public PowerConnection(
            String sourceId,
            String consumerId,
            ConnectorType connectorType,
            String outletId,
            String cableNotes,
            String cableLengthNotes
    ) {
        this(sourceId, consumerId, connectorType, outletId, cableNotes, cableLengthNotes, List.of());
    }

    public PowerConnection(
            String sourceId,
            String consumerId,
            ConnectorType connectorType,
            String outletId,
            String cableNotes,
            String cableLengthNotes,
            List<Position> routePoints
    ) {
        this(sourceId, consumerId, connectorType, outletId, cableNotes, cableLengthNotes, routePoints, false, new Position(0, 0));
    }

    public PowerConnection {
        cableNotes = cableNotes == null ? "" : cableNotes.trim();
        cableLengthNotes = cableLengthNotes == null ? "" : cableLengthNotes.trim();
        routePoints = routePoints == null ? List.of() : List.copyOf(routePoints);
        cableLabelOffset = cableLabelOffset == null ? new Position(0, 0) : cableLabelOffset;
    }
}
