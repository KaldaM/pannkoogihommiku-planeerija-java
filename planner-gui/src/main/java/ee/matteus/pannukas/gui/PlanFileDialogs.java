package ee.matteus.pannukas.gui;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.Optional;

final class PlanFileDialogs {
    private PlanFileDialogs() {
    }

    static Optional<File> choosePlanToOpen(Stage owner, File initialDirectory) {
        FileChooser fileChooser = createPlanFileChooser(initialDirectory);
        return Optional.ofNullable(fileChooser.showOpenDialog(owner));
    }

    static Optional<File> choosePlanToSave(Stage owner, File initialDirectory, File currentPlanFile) {
        FileChooser fileChooser = createPlanFileChooser(initialDirectory);
        if (currentPlanFile != null) {
            fileChooser.setInitialFileName(currentPlanFile.getName());
        }
        return Optional.ofNullable(fileChooser.showSaveDialog(owner))
                .map(PlanFileNames::ensurePlanExtension);
    }

    static UnsavedChangesChoice confirmUnsavedChanges() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Salvestamata muudatused");
        alert.setHeaderText("Plaanis on salvestamata muudatusi");
        alert.setContentText("Kas soovid enne jätkamist plaani salvestada?");
        ButtonType saveButton = new ButtonType("Salvesta");
        ButtonType discardButton = new ButtonType("Ära salvesta");
        alert.getButtonTypes().setAll(saveButton, discardButton, ButtonType.CANCEL);

        ButtonType choice = alert.showAndWait().orElse(ButtonType.CANCEL);
        if (choice == saveButton) {
            return UnsavedChangesChoice.SAVE;
        }
        if (choice == discardButton) {
            return UnsavedChangesChoice.DISCARD;
        }
        return UnsavedChangesChoice.CANCEL;
    }

    private static FileChooser createPlanFileChooser(File initialDirectory) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Plaanisepa plaan");
        if (initialDirectory != null && initialDirectory.isDirectory()) {
            fileChooser.setInitialDirectory(initialDirectory);
        }
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Plaanifail", "*.pplan"));
        return fileChooser;
    }

    enum UnsavedChangesChoice {
        SAVE,
        DISCARD,
        CANCEL
    }
}
