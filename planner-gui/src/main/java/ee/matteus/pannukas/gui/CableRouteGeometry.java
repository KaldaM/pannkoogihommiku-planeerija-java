package ee.matteus.pannukas.gui;

import ee.matteus.pannukas.core.model.Position;

import java.util.List;

final class CableRouteGeometry {
    private CableRouteGeometry() {
    }

    static int closestSegmentIndex(List<Position> path, Position point) {
        int closestSegmentIndex = 0;
        double closestDistance = Double.MAX_VALUE;
        for (int index = 1; index < path.size(); index++) {
            double distance = distanceToSegmentSquared(point, path.get(index - 1), path.get(index));
            if (distance < closestDistance) {
                closestDistance = distance;
                closestSegmentIndex = index - 1;
            }
        }
        return closestSegmentIndex;
    }

    private static double distanceToSegmentSquared(Position point, Position segmentStart, Position segmentEnd) {
        double segmentX = segmentEnd.x() - segmentStart.x();
        double segmentY = segmentEnd.y() - segmentStart.y();
        double lengthSquared = segmentX * segmentX + segmentY * segmentY;
        if (lengthSquared == 0) {
            double deltaX = point.x() - segmentStart.x();
            double deltaY = point.y() - segmentStart.y();
            return deltaX * deltaX + deltaY * deltaY;
        }

        double ratio = ((point.x() - segmentStart.x()) * segmentX + (point.y() - segmentStart.y()) * segmentY) / lengthSquared;
        double clampedRatio = Math.max(0, Math.min(1, ratio));
        double projectionX = segmentStart.x() + clampedRatio * segmentX;
        double projectionY = segmentStart.y() + clampedRatio * segmentY;
        double deltaX = point.x() - projectionX;
        double deltaY = point.y() - projectionY;
        return deltaX * deltaX + deltaY * deltaY;
    }
}
