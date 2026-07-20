package ee.matteus.pannukas.gui;

import ee.matteus.pannukas.core.model.ConnectorType;
import ee.matteus.pannukas.core.model.Position;
import ee.matteus.pannukas.core.model.PowerConnection;
import javafx.scene.paint.Color;

import java.util.List;

final class CableDisplayHelper {
    private CableDisplayHelper() {
    }

    static Color color(ConnectorType connectorType) {
        return switch (connectorType) {
            case SCHUKO_230V -> Color.web("#2563eb");
            case INDUSTRIAL_16A -> Color.web("#16a34a");
            case INDUSTRIAL_32A -> Color.web("#ea580c");
            case INDUSTRIAL_63A -> Color.web("#7c3aed");
        };
    }

    static double width(ConnectorType connectorType) {
        return switch (connectorType) {
            case SCHUKO_230V -> 2.0;
            case INDUSTRIAL_16A -> 2.8;
            case INDUSTRIAL_32A -> 3.6;
            case INDUSTRIAL_63A -> 4.4;
        };
    }

    static String shortTypeName(ConnectorType connectorType) {
        return switch (connectorType) {
            case SCHUKO_230V -> "230V";
            case INDUSTRIAL_16A -> "16A";
            case INDUSTRIAL_32A -> "32A";
            case INDUSTRIAL_63A -> "63A";
        };
    }

    static String mapLabel(PowerConnection connection, double lengthMeters) {
        String baseLabel = "%s · %.1f m".formatted(shortTypeName(connection.connectorType()), lengthMeters);
        return connection.cableLengthNotes().isBlank()
                ? baseLabel
                : "%s · %s".formatted(baseLabel, connection.cableLengthNotes());
    }

    static double lengthMeters(List<Position> path, double pixelsPerMeter) {
        double lengthMeters = 0.0;
        for (int index = 1; index < path.size(); index++) {
            lengthMeters += distancePixels(path.get(index - 1), path.get(index)) / pixelsPerMeter;
        }
        return lengthMeters;
    }

    static Position labelPosition(PowerConnection connection, List<Position> path) {
        Position defaultPosition = defaultLabelPosition(path);
        if (connection.customCableLabelPosition()) {
            return new Position(
                    defaultPosition.x() + connection.cableLabelOffset().x(),
                    defaultPosition.y() + connection.cableLabelOffset().y()
            );
        }
        return defaultPosition;
    }

    static Position defaultLabelPosition(List<Position> path) {
        Position midpoint = pathMidpoint(path);
        return new Position(midpoint.x() + 6, midpoint.y() + 6);
    }

    private static Position pathMidpoint(List<Position> path) {
        if (path.isEmpty()) {
            return new Position(0, 0);
        }
        if (path.size() == 1) {
            return path.getFirst();
        }

        double halfLengthPixels = pathLengthPixels(path) / 2;
        double walkedPixels = 0.0;
        for (int index = 1; index < path.size(); index++) {
            Position start = path.get(index - 1);
            Position end = path.get(index);
            double segmentLengthPixels = distancePixels(start, end);
            if (walkedPixels + segmentLengthPixels >= halfLengthPixels) {
                double ratio = segmentLengthPixels == 0 ? 0 : (halfLengthPixels - walkedPixels) / segmentLengthPixels;
                return new Position(
                        start.x() + (end.x() - start.x()) * ratio,
                        start.y() + (end.y() - start.y()) * ratio
                );
            }
            walkedPixels += segmentLengthPixels;
        }
        return path.getLast();
    }

    private static double pathLengthPixels(List<Position> path) {
        double lengthPixels = 0.0;
        for (int index = 1; index < path.size(); index++) {
            lengthPixels += distancePixels(path.get(index - 1), path.get(index));
        }
        return lengthPixels;
    }

    private static double distancePixels(Position start, Position end) {
        double deltaX = end.x() - start.x();
        double deltaY = end.y() - start.y();
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY);
    }
}
