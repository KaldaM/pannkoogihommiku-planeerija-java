package ee.matteus.plaanisepp.core.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PowerConnectableTest {
    @Test
    void tentAreaAndLineKeepRelativeConnectionOffsetWhenMoved() {
        List<PlannerObject> objects = List.of(
                new Tent("tent", "Telk", new Position(10, 20)),
                new AreaObject("area", "Ala", new Position(10, 20)),
                new LineObject("line", "Joon", new Position(10, 20))
        );

        for (PlannerObject object : objects) {
            PowerConnectable connectable = (PowerConnectable) object;
            connectable.setPowerConnectionOffset(new Position(12, -8));

            object.moveTo(new Position(100, 200));

            assertEquals(new Position(12, -8), connectable.powerConnectionOffset());
            connectable.resetPowerConnectionOffset();
            assertEquals(new Position(0, 0), connectable.powerConnectionOffset());
        }
    }
}
