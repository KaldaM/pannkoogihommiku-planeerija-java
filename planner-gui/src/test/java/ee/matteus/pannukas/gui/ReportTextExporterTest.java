package ee.matteus.pannukas.gui;

import ee.matteus.pannukas.core.model.AreaObject;
import ee.matteus.pannukas.core.model.ConnectorType;
import ee.matteus.pannukas.core.model.Equipment;
import ee.matteus.pannukas.core.model.EventPlan;
import ee.matteus.pannukas.core.model.LineObject;
import ee.matteus.pannukas.core.model.Position;
import ee.matteus.pannukas.core.model.PowerOutlet;
import ee.matteus.pannukas.core.model.PowerSource;
import ee.matteus.pannukas.core.service.PowerSummaryService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportTextExporterTest {
    @Test
    void includesConnectedAreaAndLineInPowerAndCableReports() {
        EventPlan plan = new EventPlan("Test");
        PowerSource source = new PowerSource("source", "Kapp", new Position(0, 0));
        source.addOutlet(new PowerOutlet("outlet", ConnectorType.SCHUKO_230V, 11000));
        AreaObject area = new AreaObject("area", "Lava", new Position(20, 20));
        area.addEquipment(new Equipment("Valgusti", 500));
        LineObject line = new LineObject("line", "Valguskett", new Position(40, 40));
        line.addEquipment(new Equipment("Lambid", 300));
        plan.addObject(source);
        plan.addObject(area);
        plan.addObject(line);
        plan.connectToPower(source.id(), area.id(), ConnectorType.SCHUKO_230V, "outlet");
        plan.connectToPower(source.id(), line.id(), ConnectorType.SCHUKO_230V, "outlet");

        String report = new ReportTextExporter(new PowerSummaryService()).export(
                plan,
                ReportExportScope.FULL,
                true,
                true,
                false
        );

        assertTrue(report.contains("Lava: 500 W"));
        assertTrue(report.contains("* Valgusti: 500 W"));
        assertTrue(report.contains("Valguskett: 300 W"));
        assertTrue(report.contains("* Lambid: 300 W"));
        assertTrue(report.contains("Lava -> Kapp"));
        assertTrue(report.contains("Valguskett -> Kapp"));
    }
}
