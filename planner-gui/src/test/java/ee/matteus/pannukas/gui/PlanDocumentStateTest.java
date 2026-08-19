package ee.matteus.pannukas.gui;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlanDocumentStateTest {
    @Test
    void newDocumentIsClean() {
        PlanDocumentState state = new PlanDocumentState();

        assertFalse(state.hasUnsavedChanges());
        assertEquals("Pannkoogihommiku planeerija", state.windowTitle(null));
        assertEquals("Salvestatud", state.saveStatusText());
    }

    @Test
    void dirtyDocumentIncludesMarkerAndFileNameInTitle() {
        PlanDocumentState state = new PlanDocumentState();

        state.markDirty();

        assertTrue(state.hasUnsavedChanges());
        assertEquals(
                "* Pannkoogihommiku planeerija - test.pplan",
                state.windowTitle(new File("test.pplan"))
        );
        assertEquals("Salvestamata muudatused", state.saveStatusText());
    }

    @Test
    void markingDocumentCleanRemovesUnsavedState() {
        PlanDocumentState state = new PlanDocumentState();
        state.markDirty();

        state.markClean();

        assertFalse(state.hasUnsavedChanges());
        assertEquals("Pannkoogihommiku planeerija - test.pplan", state.windowTitle(new File("test.pplan")));
        assertEquals("Salvestatud", state.saveStatusText());
    }
}
