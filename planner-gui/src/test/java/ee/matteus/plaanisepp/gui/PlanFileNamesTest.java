package ee.matteus.plaanisepp.gui;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class PlanFileNamesTest {
    @Test
    void addsPlanExtensionWhenItIsMissing() {
        File selectedFile = new File("plaan");

        assertEquals("plaan.pplan", PlanFileNames.ensurePlanExtension(selectedFile).getName());
    }

    @Test
    void replacesZipExtensionWithPlanExtension() {
        File selectedFile = new File("plaan.zip");

        assertEquals("plaan.pplan", PlanFileNames.ensurePlanExtension(selectedFile).getName());
    }

    @Test
    void keepsExistingPlanExtensionCaseInsensitively() {
        File selectedFile = new File("plaan.PPLAN");

        assertSame(selectedFile, PlanFileNames.ensurePlanExtension(selectedFile));
    }
}
