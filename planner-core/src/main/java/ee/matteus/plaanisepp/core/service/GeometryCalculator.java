package ee.matteus.plaanisepp.core.service;

import ee.matteus.plaanisepp.core.model.CustomObject;
import ee.matteus.plaanisepp.core.model.CustomObjectShape;
import ee.matteus.plaanisepp.core.model.Position;

import java.util.List;

public final class GeometryCalculator {
    private GeometryCalculator() {
    }

    public static double lineLengthMeters(List<Position> points, double pixelsPerMeter) {
        requirePositiveScale(pixelsPerMeter);
        double lengthPixels = 0.0;
        for (int index = 1; index < points.size(); index++) {
            lengthPixels += distance(points.get(index - 1), points.get(index));
        }
        return lengthPixels / pixelsPerMeter;
    }

    public static double polygonPerimeterMeters(List<Position> points, double pixelsPerMeter) {
        requirePositiveScale(pixelsPerMeter);
        if (points.size() < 2) {
            return 0.0;
        }
        double perimeterPixels = lineLengthMeters(points, 1.0);
        perimeterPixels += distance(points.getLast(), points.getFirst());
        return perimeterPixels / pixelsPerMeter;
    }

    public static double polygonAreaSquareMeters(List<Position> points, double pixelsPerMeter) {
        requirePositiveScale(pixelsPerMeter);
        if (points.size() < 3) {
            return 0.0;
        }
        double doubledAreaPixels = 0.0;
        for (int index = 0; index < points.size(); index++) {
            Position current = points.get(index);
            Position next = points.get((index + 1) % points.size());
            doubledAreaPixels += current.x() * next.y() - next.x() * current.y();
        }
        return Math.abs(doubledAreaPixels) / 2.0 / Math.pow(pixelsPerMeter, 2);
    }

    public static double customObjectAreaSquareMeters(CustomObject object) {
        if (object.shape() == CustomObjectShape.CIRCLE) {
            double radiusMeters = object.widthMeters() / 2.0;
            return Math.PI * radiusMeters * radiusMeters;
        }
        return object.widthMeters() * object.heightMeters();
    }

    public static double customObjectPerimeterMeters(CustomObject object) {
        if (object.shape() == CustomObjectShape.CIRCLE) {
            return Math.PI * object.widthMeters();
        }
        return 2.0 * (object.widthMeters() + object.heightMeters());
    }

    private static double distance(Position start, Position end) {
        return Math.hypot(end.x() - start.x(), end.y() - start.y());
    }

    private static void requirePositiveScale(double pixelsPerMeter) {
        if (pixelsPerMeter <= 0) {
            throw new IllegalArgumentException("Pikslit meetri kohta peab olema positiivne.");
        }
    }
}
