package ee.matteus.pannukas.core.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PowerSource extends PlannerObject {
    private final List<PowerOutlet> outlets = new ArrayList<>();

    public PowerSource(String id, String name, Position position) {
        super(id, name, position);
    }

    public void addOutlet(PowerOutlet outlet) {
        outlets.add(outlet);
    }

    public void removeOutlet(int index) {
        outlets.remove(index);
    }

    public List<PowerOutlet> outlets() {
        return Collections.unmodifiableList(outlets);
    }

    public int totalCapacityWatts() {
        return outlets.stream().mapToInt(PowerOutlet::capacityWatts).sum();
    }
}
