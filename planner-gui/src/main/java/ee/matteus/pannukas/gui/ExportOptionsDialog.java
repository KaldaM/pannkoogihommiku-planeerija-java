package ee.matteus.pannukas.gui;

import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

import java.util.Optional;

final class ExportOptionsDialog {
    private ExportOptionsDialog() {
    }

    static Optional<MapImageExportScope> chooseMapImageExportScope() {
        ChoiceDialog<MapImageExportScope> dialog = new ChoiceDialog<>(
                MapImageExportScope.FULL_MAP,
                MapImageExportScope.FULL_MAP,
                MapImageExportScope.CURRENT_VIEW
        );
        dialog.setTitle("Ekspordi kaart pildina");
        dialog.setHeaderText("Vali eksporditav ala");
        dialog.setContentText("Ala");
        return dialog.showAndWait();
    }

    static Optional<ReportExportScope> chooseReportExportScope() {
        ChoiceDialog<ReportExportScope> dialog = new ChoiceDialog<>(
                ReportExportScope.COMPACT,
                ReportExportScope.COMPACT,
                ReportExportScope.FULL
        );
        dialog.setTitle("Ekspordi raport");
        dialog.setHeaderText("Vali raporti detailsus");
        dialog.setContentText("Raport");
        return dialog.showAndWait();
    }

    static Optional<PdfExportOptions> choosePdfExportOptions() {
        ComboBox<MapImageExportScope> mapScopeComboBox = new ComboBox<>();
        mapScopeComboBox.getItems().addAll(MapImageExportScope.values());
        mapScopeComboBox.getSelectionModel().select(MapImageExportScope.FULL_MAP);

        ComboBox<ReportExportScope> reportScopeComboBox = new ComboBox<>();
        reportScopeComboBox.getItems().addAll(ReportExportScope.values());
        reportScopeComboBox.getSelectionModel().select(ReportExportScope.COMPACT);

        GridPane form = formGrid();
        form.addRow(0, new Label("Kaardi ala"), mapScopeComboBox);
        form.addRow(1, new Label("Raport"), reportScopeComboBox);

        Alert dialog = new Alert(Alert.AlertType.CONFIRMATION);
        dialog.setTitle("Ekspordi PDF");
        dialog.setHeaderText("Vali PDF ekspordi seaded");
        dialog.getDialogPane().setContent(form);

        return dialog.showAndWait()
                .filter(buttonType -> buttonType == ButtonType.OK)
                .map(buttonType -> new PdfExportOptions(
                        mapScopeComboBox.getSelectionModel().getSelectedItem(),
                        reportScopeComboBox.getSelectionModel().getSelectedItem()
                ));
    }

    private static GridPane formGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(8, 0, 0, 0));
        return grid;
    }
}
