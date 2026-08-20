package ee.matteus.plaanisepp.gui;

import ee.matteus.plaanisepp.core.model.Position;
import javafx.geometry.Point2D;
import javafx.scene.shape.Polyline;

import java.util.List;

final class CablePolylineHelper {
    private CablePolylineHelper() {
    }

    static Polyline create(List<Position> path) {
        Polyline polyline = new Polyline();
        for (Position point : path) {
            polyline.getPoints().addAll(point.x(), point.y());
        }
        return polyline;
    }

    static void updateRoutePoint(Polyline polyline, int routePointIndex, Point2D mapPoint) {
        int pointIndex = routePointIndex + 1;
        int xCoordinateIndex = pointIndex * 2;
        int yCoordinateIndex = xCoordinateIndex + 1;
        if (yCoordinateIndex >= polyline.getPoints().size()) {
            return;
        }
        polyline.getPoints().set(xCoordinateIndex, mapPoint.getX());
        polyline.getPoints().set(yCoordinateIndex, mapPoint.getY());
    }

    static void updateLastPoint(Polyline polyline, Point2D mapPoint) {
        int yCoordinateIndex = polyline.getPoints().size() - 1;
        int xCoordinateIndex = yCoordinateIndex - 1;
        if (xCoordinateIndex < 0) {
            return;
        }
        polyline.getPoints().set(xCoordinateIndex, mapPoint.getX());
        polyline.getPoints().set(yCoordinateIndex, mapPoint.getY());
    }
}
