package ee.matteus.pannukas.gui;

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

    static List<Position> cablePath(Tent tent, PowerSource source, PowerConnection connection, double pixelsPerMeter) {
        List<Position> path = new ArrayList<>();
        path.add(objectCenter(source, pixelsPerMeter));
        path.addAll(connection.routePoints());
        path.add(objectCenter(tent, pixelsPerMeter));
        return path;
    }

    static List<Position> cablePath(
            Tent tent,
            PowerSource source,
            PowerConnection connection,
            List<Position> routePoints,
            double pixelsPerMeter
    ) {
        List<Position> path = new ArrayList<>();
        path.add(objectCenter(source, pixelsPerMeter));
        path.addAll(routePoints);
        path.add(objectCenter(tent, pixelsPerMeter));
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
        return object.position();
    }

    private static double metersToPixels(double meters, double pixelsPerMeter) {
        return meters * pixelsPerMeter;
    }
}
