package ee.matteus.pannukas.gui;

import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.Optional;

final class ExportFileChooser {
    private ExportFileChooser() {
    }

    static Optional<File> chooseSummaryFile(Stage owner, File initialDirectory, String planName, File currentPlanFile) {
        return chooseFile(
                owner,
                initialDirectory,
                "Ekspordi kokkuvõte",
                ExportFileNames.summaryFileName(planName, currentPlanFile),
                new FileChooser.ExtensionFilter("Tekstifail", "*.txt")
        ).map(ExportFileNames::ensureTxtExtension);
    }

    static Optional<File> chooseMapImageFile(Stage owner, File initialDirectory, String planName, File currentPlanFile) {
        return chooseFile(
                owner,
                initialDirectory,
                "Ekspordi kaart pildina",
                ExportFileNames.mapImageFileName(planName, currentPlanFile),
                new FileChooser.ExtensionFilter("PNG pilt", "*.png")
        ).map(ExportFileNames::ensurePngExtension);
    }

    static Optional<File> choosePdfFile(Stage owner, File initialDirectory, String planName, File currentPlanFile) {
        return chooseFile(
                owner,
                initialDirectory,
                "Ekspordi PDF",
                ExportFileNames.pdfFileName(planName, currentPlanFile),
                new FileChooser.ExtensionFilter("PDF fail", "*.pdf")
        ).map(ExportFileNames::ensurePdfExtension);
    }

    private static Optional<File> chooseFile(
            Stage owner,
            File initialDirectory,
            String title,
            String initialFileName,
            FileChooser.ExtensionFilter extensionFilter
    ) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        if (initialDirectory != null && initialDirectory.isDirectory()) {
            fileChooser.setInitialDirectory(initialDirectory);
        }
        fileChooser.setInitialFileName(initialFileName);
        fileChooser.getExtensionFilters().add(extensionFilter);
        return Optional.ofNullable(fileChooser.showSaveDialog(owner));
    }
}
