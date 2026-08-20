package ee.matteus.plaanisepp.gui;

import ee.matteus.plaanisepp.core.model.AreaObject;
import ee.matteus.plaanisepp.core.model.ConnectorType;
import ee.matteus.plaanisepp.core.model.LineObject;
import ee.matteus.plaanisepp.core.model.Position;
import ee.matteus.plaanisepp.core.model.PowerConnection;
import ee.matteus.plaanisepp.core.model.PowerSource;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CablePathHelperTest {
    @Test
    void usesAreaBoundsCenterAsDefaultConsumerEndpoint() {
        AreaObject area = new AreaObject("area", "Ala", new Position(10, 20));
        area.setPoints(List.of(
                new Position(10, 20),
                new Position(50, 20),
                new Position(50, 60),
                new Position(10, 60)
        ));

        List<Position> path = CablePathHelper.cablePath(area, source(), connection(area.id()), 10.0);

        assertEquals(new Position(30, 40), path.getLast());
    }

    @Test
    void usesLineBoundsCenterAsDefaultConsumerEndpoint() {
        LineObject line = new LineObject("line", "Joon", new Position(20, 30));
        line.setPoints(List.of(
                new Position(20, 30),
                new Position(80, 50),
                new Position(60, 90)
        ));

        List<Position> path = CablePathHelper.cablePath(line, source(), connection(line.id()), 10.0);

        assertEquals(new Position(50, 60), path.getLast());
    }

    @Test
    void appliesRelativePowerConnectionOffsetToConsumerEndpoint() {
        AreaObject area = new AreaObject("area", "Ala", new Position(10, 20));
        area.setPoints(List.of(
                new Position(10, 20),
                new Position(50, 60)
        ));
        area.setPowerConnectionOffset(new Position(12, -8));

        List<Position> path = CablePathHelper.cablePath(area, source(), connection(area.id()), 10.0);

        assertEquals(new Position(42, 32), path.getLast());
    }

    private PowerSource source() {
        return new PowerSource("source", "Kapp", new Position(0, 0));
    }

    private PowerConnection connection(String consumerId) {
        return new PowerConnection("source", consumerId, ConnectorType.SCHUKO_230V);
    }
}
