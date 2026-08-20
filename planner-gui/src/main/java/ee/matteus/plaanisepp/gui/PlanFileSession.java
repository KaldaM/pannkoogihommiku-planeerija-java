package ee.matteus.plaanisepp.gui;

import ee.matteus.plaanisepp.core.model.EventPlan;
import ee.matteus.plaanisepp.core.service.PlanFileService;

import java.io.File;
import java.io.IOException;
final class PlanFileSession {
    private final PlanFileService planFileService = new PlanFileService();
    private File currentFile;
    private File lastUsedDirectory;

    EventPlan load(File file) throws IOException {
        EventPlan loadedPlan = planFileService.load(file.toPath());
        currentFile = file;
        rememberDirectory(file);
        return loadedPlan;
    }

    void save(EventPlan plan, File file) throws IOException {
        planFileService.save(plan, file.toPath());
        currentFile = file;
        rememberDirectory(file);
    }

    File currentFile() {
        return currentFile;
    }

    void clearCurrentFile() {
        currentFile = null;
    }

    File initialDirectory() {
        if (lastUsedDirectory != null && lastUsedDirectory.isDirectory()) {
            return lastUsedDirectory;
        }
        if (currentFile != null
                && currentFile.getParentFile() != null
                && currentFile.getParentFile().isDirectory()) {
            return currentFile.getParentFile();
        }
        return null;
    }

    void rememberDirectory(File file) {
        if (file != null && file.getParentFile() != null && file.getParentFile().isDirectory()) {
            lastUsedDirectory = file.getParentFile();
        }
    }
}
