package ee.matteus.pannukas.core.service;

import ee.matteus.pannukas.core.model.EventPlan;

import java.util.UUID;

public class PlanFactory {
    public EventPlan createEmptyPlan() {
        EventPlan plan = new EventPlan("Uus plaan");
        plan.setMapImagePath("classpath:/maps/tavakaart.png");
        return plan;
    }

    public String newId() {
        return UUID.randomUUID().toString();
    }
}
