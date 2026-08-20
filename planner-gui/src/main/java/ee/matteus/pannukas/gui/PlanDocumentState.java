package ee.matteus.pannukas.gui;

import java.io.File;

final class PlanDocumentState {
    private static final String APPLICATION_TITLE = "Plaanisepp";

    private boolean unsavedChanges;

    void markDirty() {
        unsavedChanges = true;
    }

    void markClean() {
        unsavedChanges = false;
    }

    boolean hasUnsavedChanges() {
        return unsavedChanges;
    }

    String windowTitle(File currentFile) {
        String unsavedPrefix = unsavedChanges ? "* " : "";
        String fileName = currentFile == null ? "" : " - " + currentFile.getName();
        return unsavedPrefix + APPLICATION_TITLE + fileName;
    }

    String saveStatusText() {
        return unsavedChanges ? "Salvestamata muudatused" : "Salvestatud";
    }
}
