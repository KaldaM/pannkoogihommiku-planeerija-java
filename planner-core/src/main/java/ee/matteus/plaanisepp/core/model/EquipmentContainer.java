package ee.matteus.plaanisepp.core.model;

import java.util.List;

public interface EquipmentContainer extends PowerConnectable {
    List<Equipment> equipment();

    void addEquipment(Equipment item);

    void removeEquipment(int index);

    @Override
    default int requiredWatts() {
        return equipment().stream().mapToInt(Equipment::requiredWatts).sum();
    }
}
