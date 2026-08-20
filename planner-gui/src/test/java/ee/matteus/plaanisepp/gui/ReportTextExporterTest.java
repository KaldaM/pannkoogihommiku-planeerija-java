package ee.matteus.plaanisepp.gui;

import ee.matteus.plaanisepp.core.model.AreaObject;
import ee.matteus.plaanisepp.core.model.ConnectorType;
import ee.matteus.plaanisepp.core.model.DistributionPanel;
import ee.matteus.plaanisepp.core.model.Equipment;
import ee.matteus.plaanisepp.core.model.EventPlan;
import ee.matteus.plaanisepp.core.model.LineObject;
import ee.matteus.plaanisepp.core.model.Position;
import ee.matteus.plaanisepp.core.model.PowerOutlet;
import ee.matteus.plaanisepp.core.model.PowerSource;
import ee.matteus.plaanisepp.core.service.PowerSummaryService;
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

    @Test
    void includesDistributionPanelDemandInUpstreamOutlet() {
        EventPlan plan = new EventPlan("Test");
        PowerSource mainSource = new PowerSource("source", "Põhikilp", new Position(0, 0));
        mainSource.addOutlet(new PowerOutlet("source-outlet", ConnectorType.INDUSTRIAL_16A, 11000));
        DistributionPanel panel = new DistributionPanel("panel", "Alajaotuskilp", new Position(20, 20));
        panel.addOutlet(new PowerOutlet("panel-outlet", ConnectorType.SCHUKO_230V, 3500));
        AreaObject area = new AreaObject("area", "Telk", new Position(40, 40));
        area.addEquipment(new Equipment("Pliit", 1811));
        plan.addObject(mainSource);
        plan.addObject(panel);
        plan.addObject(area);
        plan.connectToPower(mainSource.id(), panel.id(), ConnectorType.INDUSTRIAL_16A, "source-outlet");
        plan.connectToPower(panel.id(), area.id(), ConnectorType.SCHUKO_230V, "panel-outlet");

        String report = new ReportTextExporter(new PowerSummaryService()).export(
                plan,
                ReportExportScope.FULL,
                true,
                false,
                false
        );

        assertTrue(report.contains("16A tööstusvool 1: 11000 W mahutavus, 1811 W kasutusel"));
        assertTrue(report.contains("- Alajaotuskilp: 1811 W"));
    }
}
