package ee.matteus.pannukas.core.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EquipmentContainerTest {
    @Test
    void tentAreaAndLineShareEquipmentBehavior() {
        List<EquipmentContainer> containers = List.of(
                new Tent("tent", "Telk", new Position(0, 0)),
                new AreaObject("area", "Ala", new Position(0, 0)),
                new LineObject("line", "Joon", new Position(0, 0))
        );

        for (EquipmentContainer container : containers) {
            container.addEquipment(new Equipment("Valgusti", 500));
            container.addEquipment(new Equipment("Soojendi", 1500));

            assertEquals(2, container.equipment().size());
            assertEquals(2000, container.requiredWatts());

            container.removeEquipment(0);

            assertEquals(1, container.equipment().size());
            assertEquals(1500, container.requiredWatts());
        }
    }

    @Test
    void equipmentListCannotBeChangedWithoutContainerMethods() {
        EquipmentContainer container = new AreaObject("area", "Ala", new Position(0, 0));

        assertThrows(
                UnsupportedOperationException.class,
                () -> container.equipment().add(new Equipment("Valgusti", 500))
        );
    }
}
