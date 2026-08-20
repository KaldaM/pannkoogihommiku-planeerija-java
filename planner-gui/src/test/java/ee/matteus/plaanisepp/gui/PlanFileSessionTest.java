package ee.matteus.plaanisepp.gui;

import ee.matteus.plaanisepp.core.model.EventPlan;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlanFileSessionTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void saveAndLoadTrackCurrentFileAndDirectory() throws IOException {
        PlanFileSession session = new PlanFileSession();
        EventPlan plan = new EventPlan("Testplaan");
        File file = temporaryDirectory.resolve("test.pplan").toFile();

        session.save(plan, file);

        assertEquals(file, session.currentFile());
        assertEquals(temporaryDirectory.toFile(), session.initialDirectory());
        assertEquals("Testplaan", session.load(file).name());
        assertEquals(file, session.currentFile());
    }

    @Test
    void clearingCurrentFileKeepsLastUsedDirectory() throws IOException {
        PlanFileSession session = new PlanFileSession();
        File file = temporaryDirectory.resolve("test.pplan").toFile();
        session.save(new EventPlan("Testplaan"), file);

        session.clearCurrentFile();

        assertNull(session.currentFile());
        assertEquals(temporaryDirectory.toFile(), session.initialDirectory());
    }

    @Test
    void failedLoadDoesNotReplaceCurrentFile() throws IOException {
        PlanFileSession session = new PlanFileSession();
        File savedFile = temporaryDirectory.resolve("saved.pplan").toFile();
        session.save(new EventPlan("Testplaan"), savedFile);
        File invalidFile = temporaryDirectory.resolve("invalid.pplan").toFile();
        Files.writeString(invalidFile.toPath(), "formatVersion=999");

        assertThrows(IOException.class, () -> session.load(invalidFile));

        assertEquals(savedFile, session.currentFile());
    }
}
