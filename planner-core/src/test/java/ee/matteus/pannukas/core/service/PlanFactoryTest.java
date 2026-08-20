package ee.matteus.pannukas.core.service;

import ee.matteus.pannukas.core.model.EventPlan;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlanFactoryTest {
    @Test
    void createsBrandNeutralEmptyPlan() {
        EventPlan plan = new PlanFactory().createEmptyPlan();

        assertEquals("Uus plaan", plan.name());
        assertEquals("classpath:/maps/tavakaart.png", plan.mapImagePath());
    }
}
