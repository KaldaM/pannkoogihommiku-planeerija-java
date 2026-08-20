package ee.matteus.plaanisepp.core.service;

import ee.matteus.plaanisepp.core.model.EventPlan;
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
