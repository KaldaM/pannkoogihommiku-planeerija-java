package ee.matteus.pannukas.core.service;

import ee.matteus.pannukas.core.model.ConnectorType;
import ee.matteus.pannukas.core.model.EventPlan;
import ee.matteus.pannukas.core.model.Position;
import ee.matteus.pannukas.core.model.PowerOutlet;
import ee.matteus.pannukas.core.model.PowerSource;
import ee.matteus.pannukas.core.model.Tent;

import java.util.UUID;

public class PlanFactory {
    public EventPlan createDemoPlan() {
        EventPlan plan = new EventPlan("Pannkoogihommik");
        plan.setMapImagePath("classpath:/maps/tavakaart.png");

        PowerSource pvk1 = new PowerSource(newId(), "PVK 1", new Position(180, 320));
        pvk1.addOutlet(new PowerOutlet(newId(), ConnectorType.SCHUKO_230V, 11000));

        PowerSource pvk2 = new PowerSource(newId(), "PVK 2", new Position(420, 220));
        pvk2.addOutlet(new PowerOutlet(newId(), ConnectorType.INDUSTRIAL_32A, 22000));

        Tent pancakes = new Tent(newId(), "Pannkoogitelk", new Position(260, 260));
        pancakes.setGroupName("Toitlustus");
        pancakes.addEquipment(new ee.matteus.pannukas.core.model.Equipment("Pannkoogipann", 2000));
        pancakes.addEquipment(new ee.matteus.pannukas.core.model.Equipment("Veekeetja", 1800));

        plan.addObject(pvk1);
        plan.addObject(pvk2);
        plan.addObject(pancakes);
        plan.connectToPower(pvk1.id(), pancakes.id(), ConnectorType.SCHUKO_230V);

        return plan;
    }

    public String newId() {
        return UUID.randomUUID().toString();
    }
}
