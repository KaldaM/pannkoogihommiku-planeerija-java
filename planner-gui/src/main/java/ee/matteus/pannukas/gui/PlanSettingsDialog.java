package ee.matteus.pannukas.gui;

import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

final class PlanSettingsDialog {
    private PlanSettingsDialog() {
    }

    static Optional<Settings> show(
            Stage owner,
            Settings initialSettings,
            File initialDirectory,
            double minimumFontSize,
            double maximumFontSize,
            Supplier<Optional<String>> scaleFromMeasurement,
            Consumer<File> selectedMapFileHandler
    ) {
        TextField planNameField = new TextField(initialSettings.planName());
        TextField pixelsPerMeterField = new TextField(initialSettings.pixelsPerMeterText());
        pixelsPerMeterField.setPromptText("px/m");
        Slider objectLabelFontSizeSlider = createPixelSlider(
                minimumFontSize,
                maximumFontSize,
                initialSettings.objectLabelFontSize()
        );
        Slider cableLabelFontSizeSlider = createPixelSlider(
                minimumFontSize,
                maximumFontSize,
                initialSettings.cableLabelFontSize()
        );
        Label mapLabel = new Label(mapLabelText(initialSettings.mapImagePath()));

        String[] selectedMapPath = {initialSettings.mapImagePath()};
        Button defaultMapButton = new Button("Tavakaart");
        defaultMapButton.setOnAction(event -> selectMap(initialSettings.defaultMapPath(), selectedMapPath, mapLabel));
        Button orthophotoButton = new Button("Ortofoto");
        orthophotoButton.setOnAction(event -> selectMap(initialSettings.orthophotoMapPath(), selectedMapPath, mapLabel));
        Button loadMapButton = new Button("Laadi kaart");
        loadMapButton.setOnAction(event -> chooseMapFile(
                owner,
                initialDirectory,
                selectedMapPath,
                mapLabel,
                selectedMapFileHandler
        ));
        Button setScaleFromMeasurementButton = new Button("Määra mõõdulindi järgi");
        setScaleFromMeasurementButton.setTooltip(new Tooltip(
                "Arvutab piksleid meetri kohta viimase mõõdulindi joone põhjal"
        ));
        setScaleFromMeasurementButton.setOnAction(event ->
                scaleFromMeasurement.get().ifPresent(pixelsPerMeterField::setText)
        );

        GridPane form = createForm();
        form.addRow(0, new Label("Plaani nimi"), planNameField);
        form.addRow(1, new Label("Piksleid meetri kohta"), new HBox(
                8,
                pixelsPerMeterField,
                setScaleFromMeasurementButton
        ));
        form.addRow(2, new Label("Objektisildi suurus"), pixelControl(objectLabelFontSizeSlider));
        form.addRow(3, new Label("Kaablisildi suurus"), pixelControl(cableLabelFontSizeSlider));
        form.addRow(4, new Label("Kaart"), new HBox(8, defaultMapButton, orthophotoButton, loadMapButton));
        form.addRow(5, new Label("Valitud kaart"), mapLabel);

        Alert dialog = new Alert(Alert.AlertType.CONFIRMATION);
        dialog.setTitle("Plaani andmed");
        dialog.setHeaderText("Muuda plaani andmeid");
        dialog.getDialogPane().setContent(form);
        return dialog.showAndWait()
                .filter(ButtonType.OK::equals)
                .map(buttonType -> new Settings(
                        planNameField.getText(),
                        pixelsPerMeterField.getText(),
                        objectLabelFontSizeSlider.getValue(),
                        cableLabelFontSizeSlider.getValue(),
                        selectedMapPath[0],
                        initialSettings.defaultMapPath(),
                        initialSettings.orthophotoMapPath()
                ));
    }

    private static void chooseMapFile(
            Stage owner,
            File initialDirectory,
            String[] selectedMapPath,
            Label mapLabel,
            Consumer<File> selectedMapFileHandler
    ) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Vali kaart");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "Pildifailid",
                "*.png",
                "*.jpg",
                "*.jpeg"
        ));
        if (initialDirectory != null && initialDirectory.isDirectory()) {
            fileChooser.setInitialDirectory(initialDirectory);
        }
        File file = fileChooser.showOpenDialog(owner);
        if (file != null) {
            selectMap(file.getAbsolutePath(), selectedMapPath, mapLabel);
            selectedMapFileHandler.accept(file);
        }
    }

    private static void selectMap(String mapPath, String[] selectedMapPath, Label mapLabel) {
        selectedMapPath[0] = mapPath;
        mapLabel.setText(mapLabelText(mapPath));
    }

    private static String mapLabelText(String mapPath) {
        return mapPath == null || mapPath.isBlank() ? "Kaarti pole valitud" : mapPath;
    }

    private static GridPane createForm() {
        GridPane form = new GridPane();
        form.setHgap(8);
        form.setVgap(8);
        return form;
    }

    private static Slider createPixelSlider(double min, double max, double initialValue) {
        Slider slider = new Slider(min, max, initialValue);
        slider.setMajorTickUnit(1);
        slider.setMinorTickCount(0);
        slider.setBlockIncrement(1);
        slider.setSnapToTicks(true);
        slider.setPrefWidth(210);
        return slider;
    }

    private static HBox pixelControl(Slider slider) {
        Label valueLabel = new Label(pixelValueText(slider.getValue()));
        valueLabel.setMinWidth(46);
        slider.valueProperty().addListener((observable, oldValue, newValue) ->
                valueLabel.setText(pixelValueText(newValue.doubleValue()))
        );
        HBox control = new HBox(8, slider, valueLabel);
        control.setAlignment(Pos.CENTER_LEFT);
        return control;
    }

    private static String pixelValueText(double value) {
        return "%.0f px".formatted(value);
    }

    record Settings(
            String planName,
            String pixelsPerMeterText,
            double objectLabelFontSize,
            double cableLabelFontSize,
            String mapImagePath,
            String defaultMapPath,
            String orthophotoMapPath
    ) {
    }
}
