package ee.matteus.pannukas.gui;

import ee.matteus.pannukas.core.model.AreaObject;
import ee.matteus.pannukas.core.model.LineObject;
import ee.matteus.pannukas.core.model.PlannerObject;
import ee.matteus.pannukas.core.model.Position;
import ee.matteus.pannukas.core.model.PowerConnection;
import ee.matteus.pannukas.core.model.PowerSource;
import ee.matteus.pannukas.core.model.Tent;

import java.util.ArrayList;
import java.util.List;

final class CablePathHelper {
    private CablePathHelper() {
    }

    static List<Position> cablePath(PlannerObject consumer, PowerSource source, PowerConnection connection, double pixelsPerMeter) {
        List<Position> path = new ArrayList<>();
        path.add(objectCenter(source, pixelsPerMeter));
        path.addAll(connection.routePoints());
        path.add(objectCenter(consumer, pixelsPerMeter));
        return path;
    }

    static List<Position> cablePath(
            PlannerObject consumer,
            PowerSource source,
            PowerConnection connection,
            List<Position> routePoints,
            double pixelsPerMeter
    ) {
        List<Position> path = new ArrayList<>();
        path.add(objectCenter(source, pixelsPerMeter));
        path.addAll(routePoints);
        path.add(objectCenter(consumer, pixelsPerMeter));
        return path;
    }

    static Position objectCenter(PlannerObject object, double pixelsPerMeter) {
        if (object instanceof Tent tent) {
            double widthPixels = metersToPixels(tent.widthMeters(), pixelsPerMeter);
            double heightPixels = metersToPixels(tent.heightMeters(), pixelsPerMeter);
            return new Position(
                    tent.position().x() + widthPixels / 2,
                    tent.position().y() + heightPixels / 2
            );
        }
        if (object instanceof AreaObject area) {
            return pointsCenter(area.points(), area.position());
        }
        if (object instanceof LineObject line) {
            return pointsCenter(line.points(), line.position());
        }
        return object.position();
    }

    private static Position pointsCenter(List<Position> points, Position fallback) {
        if (points == null || points.isEmpty()) {
            return fallback;
        }
        double minX = points.stream().mapToDouble(Position::x).min().orElse(fallback.x());
        double maxX = points.stream().mapToDouble(Position::x).max().orElse(fallback.x());
        double minY = points.stream().mapToDouble(Position::y).min().orElse(fallback.y());
        double maxY = points.stream().mapToDouble(Position::y).max().orElse(fallback.y());
        return new Position((minX + maxX) / 2.0, (minY + maxY) / 2.0);
    }

    private static double metersToPixels(double meters, double pixelsPerMeter) {
        return meters * pixelsPerMeter;
    }
}
