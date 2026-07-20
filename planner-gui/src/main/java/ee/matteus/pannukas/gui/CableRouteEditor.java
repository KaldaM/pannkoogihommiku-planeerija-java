package ee.matteus.pannukas.gui;

import ee.matteus.pannukas.core.model.EventPlan;
import ee.matteus.pannukas.core.model.Position;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class CableRouteEditor {
    private CableRouteEditor() {
    }

    static boolean addPoint(EventPlan plan, String consumerId, Position point) {
        if (plan.findPowerConnectionForConsumer(consumerId).isEmpty()) {
            return false;
        }
        plan.addCableRoutePoint(consumerId, point);
        return true;
    }

    static boolean insertPoint(EventPlan plan, String consumerId, List<Position> path, Position point) {
        if (plan.findPowerConnectionForConsumer(consumerId).isEmpty()) {
            return false;
        }
        int insertionIndex = CableRouteGeometry.closestSegmentIndex(path, point);
        plan.insertCableRoutePoint(consumerId, insertionIndex, point);
        return true;
    }

    static Optional<List<Position>> replacePoint(EventPlan plan, String consumerId, int routePointIndex, Position point) {
        return plan.findPowerConnectionForConsumer(consumerId)
                .filter(connection -> routePointIndex >= 0 && routePointIndex < connection.routePoints().size())
                .map(connection -> {
                    List<Position> routePoints = new ArrayList<>(connection.routePoints());
                    routePoints.set(routePointIndex, point);
                    plan.updateCableRoutePoints(consumerId, routePoints);
                    return routePoints;
                });
    }

    static boolean removePoint(EventPlan plan, String consumerId, int routePointIndex) {
        return plan.findPowerConnectionForConsumer(consumerId)
                .filter(connection -> routePointIndex >= 0 && routePointIndex < connection.routePoints().size())
                .map(connection -> {
                    List<Position> routePoints = new ArrayList<>(connection.routePoints());
                    routePoints.remove(routePointIndex);
                    plan.updateCableRoutePoints(consumerId, routePoints);
                    return true;
                })
                .orElse(false);
    }

    static boolean clearRoute(EventPlan plan, String consumerId) {
        if (plan.findPowerConnectionForConsumer(consumerId).isEmpty()) {
            return false;
        }
        plan.clearCableRoutePoints(consumerId);
        return true;
    }
}
