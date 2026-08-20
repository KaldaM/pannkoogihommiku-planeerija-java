package ee.matteus.plaanisepp.gui;

import ee.matteus.plaanisepp.core.model.AreaObject;
import ee.matteus.plaanisepp.core.model.CustomObjectShape;
import ee.matteus.plaanisepp.core.model.LineObject;
import ee.matteus.plaanisepp.core.model.MarkerType;
import ee.matteus.plaanisepp.core.model.TextObject;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.util.StringConverter;

import java.util.List;
import java.util.Optional;

final class PlacementDetailsDialog {
    private PlacementDetailsDialog() {
    }

    static Optional<PlacementDetails> show(
            PlacementType placementType,
            List<String> existingGroupNames,
            double minimumFontSize,
            double maximumFontSize
    ) {
        TextField nameField = new TextField(placementType.defaultName());
        ComboBox<String> groupComboBox = new ComboBox<>();
        groupComboBox.setEditable(true);
        groupComboBox.getItems().addAll(existingGroupNames);
        groupComboBox.getSelectionModel().select("Määramata");
        ColorPicker colorPicker = new ColorPicker(Color.web(placementType.defaultColorHex()));
        TextField tentWidthField = new TextField("3");
        TextField tentHeightField = new TextField("3");
        ComboBox<CustomObjectShape> shapeComboBox = createShapeComboBox();
        Label objectWidthLabel = new Label("Laius m");
        Label objectHeightLabel = new Label("Pikkus m");
        TextField objectWidthField = new TextField("1");
        TextField objectHeightField = new TextField("1");
        Slider opacitySlider = createOpacitySlider(
                placementType == PlacementType.AREA_OBJECT ? AreaObject.DEFAULT_OPACITY * 100.0 : 100.0
        );
        Slider lineWidthSlider = createPixelSlider(1, 50, LineObject.DEFAULT_WIDTH_PIXELS);
        Slider fontSizeSlider = createPixelSlider(
                minimumFontSize,
                maximumFontSize,
                TextObject.DEFAULT_FONT_SIZE
        );
        ComboBox<MarkerType> markerTypeComboBox = createMarkerTypeComboBox();

        configureMarkerDefaults(placementType, nameField, colorPicker, markerTypeComboBox);
        shapeComboBox.setOnAction(event -> updateObjectSizeFields(
                shapeComboBox,
                objectWidthLabel,
                objectWidthField,
                objectHeightLabel,
                objectHeightField
        ));
        updateObjectSizeFields(
                shapeComboBox,
                objectWidthLabel,
                objectWidthField,
                objectHeightLabel,
                objectHeightField
        );

        GridPane form = createForm(
                placementType,
                nameField,
                groupComboBox,
                colorPicker,
                tentWidthField,
                tentHeightField,
                shapeComboBox,
                objectWidthLabel,
                objectWidthField,
                objectHeightLabel,
                objectHeightField,
                opacitySlider,
                lineWidthSlider,
                fontSizeSlider,
                markerTypeComboBox
        );
        Alert dialog = new Alert(Alert.AlertType.CONFIRMATION);
        dialog.setTitle("Lisa objekt");
        dialog.setHeaderText("Sisesta lisatava objekti andmed");
        dialog.getDialogPane().setContent(form);
        if (dialog.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return Optional.empty();
        }

        return createPlacementDetails(
                placementType,
                nameField,
                groupComboBox,
                colorPicker,
                tentWidthField,
                tentHeightField,
                shapeComboBox,
                objectWidthField,
                objectHeightField,
                opacitySlider,
                lineWidthSlider,
                fontSizeSlider,
                markerTypeComboBox
        );
    }

    private static ComboBox<CustomObjectShape> createShapeComboBox() {
        ComboBox<CustomObjectShape> comboBox = new ComboBox<>();
        comboBox.getItems().addAll(CustomObjectShape.values());
        comboBox.setConverter(customObjectShapeConverter());
        comboBox.getSelectionModel().select(CustomObjectShape.SQUARE);
        return comboBox;
    }

    private static ComboBox<MarkerType> createMarkerTypeComboBox() {
        ComboBox<MarkerType> comboBox = new ComboBox<>();
        comboBox.getItems().addAll(MarkerType.values());
        comboBox.setConverter(markerTypeConverter());
        comboBox.getSelectionModel().select(MarkerType.WC);
        return comboBox;
    }

    private static void configureMarkerDefaults(
            PlacementType placementType,
            TextField nameField,
            ColorPicker colorPicker,
            ComboBox<MarkerType> markerTypeComboBox
    ) {
        boolean[] markerNameEdited = {false};
        boolean[] markerColorEdited = {false};
        boolean[] updatingMarkerColor = {false};
        colorPicker.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (placementType == PlacementType.MARKER_OBJECT && !updatingMarkerColor[0]) {
                markerColorEdited[0] = true;
            }
        });
        if (placementType != PlacementType.MARKER_OBJECT) {
            return;
        }

        nameField.setText(MarkerType.WC.displayName());
        updatingMarkerColor[0] = true;
        colorPicker.setValue(Color.web(MarkerType.WC.defaultColorHex()));
        updatingMarkerColor[0] = false;
        nameField.textProperty().addListener((observable, oldValue, newValue) -> {
            MarkerType selectedMarkerType = markerTypeComboBox.getSelectionModel().getSelectedItem();
            if (selectedMarkerType != null && !newValue.equals(selectedMarkerType.displayName())) {
                markerNameEdited[0] = true;
            }
        });
        markerTypeComboBox.setOnAction(event -> {
            MarkerType selectedMarkerType = markerTypeComboBox.getSelectionModel().getSelectedItem();
            if (selectedMarkerType == null) {
                selectedMarkerType = MarkerType.WC;
            }
            if (!markerNameEdited[0]) {
                nameField.setText(selectedMarkerType.displayName());
            }
            if (!markerColorEdited[0]) {
                updatingMarkerColor[0] = true;
                colorPicker.setValue(Color.web(selectedMarkerType.defaultColorHex()));
                updatingMarkerColor[0] = false;
            }
        });
    }

    private static GridPane createForm(
            PlacementType placementType,
            TextField nameField,
            ComboBox<String> groupComboBox,
            ColorPicker colorPicker,
            TextField tentWidthField,
            TextField tentHeightField,
            ComboBox<CustomObjectShape> shapeComboBox,
            Label objectWidthLabel,
            TextField objectWidthField,
            Label objectHeightLabel,
            TextField objectHeightField,
            Slider opacitySlider,
            Slider lineWidthSlider,
            Slider fontSizeSlider,
            ComboBox<MarkerType> markerTypeComboBox
    ) {
        GridPane form = detailGrid();
        form.addRow(0, new Label("Nimi"), nameField);
        form.addRow(1, new Label("Grupp"), groupComboBox);
        if (placementType == PlacementType.TENT) {
            form.addRow(2, new Label("Laius m"), tentWidthField);
            form.addRow(3, new Label("Pikkus m"), tentHeightField);
            form.addRow(4, new Label("Läbipaistvus"), opacityControl(opacitySlider));
        } else if (placementType == PlacementType.CUSTOM_OBJECT) {
            form.addRow(2, new Label("Kuju"), shapeComboBox);
            form.addRow(3, objectWidthLabel, objectWidthField);
            form.addRow(4, objectHeightLabel, objectHeightField);
            form.addRow(5, new Label("Läbipaistvus"), opacityControl(opacitySlider));
        } else if (placementType == PlacementType.MARKER_OBJECT) {
            form.addRow(2, new Label("Marker"), markerTypeComboBox);
        } else if (placementType == PlacementType.AREA_OBJECT) {
            form.addRow(2, new Label("Läbipaistvus"), opacityControl(opacitySlider));
        } else if (placementType == PlacementType.LINE_OBJECT) {
            form.addRow(2, new Label("Paksus"), pixelControl(lineWidthSlider));
        } else if (placementType == PlacementType.TEXT_OBJECT) {
            form.addRow(2, new Label("Suurus"), pixelControl(fontSizeSlider));
        }
        if (placementType.hasConfigurableColor()) {
            form.addRow(colorRow(placementType), new Label("Värv"), colorPicker);
        }
        return form;
    }

    private static int colorRow(PlacementType placementType) {
        return switch (placementType) {
            case TENT -> 5;
            case CUSTOM_OBJECT -> 6;
            case MARKER_OBJECT, AREA_OBJECT, LINE_OBJECT, TEXT_OBJECT -> 3;
            case POWER_SOURCE, DISTRIBUTION_PANEL -> 2;
        };
    }

    private static Optional<PlacementDetails> createPlacementDetails(
            PlacementType placementType,
            TextField nameField,
            ComboBox<String> groupComboBox,
            ColorPicker colorPicker,
            TextField tentWidthField,
            TextField tentHeightField,
            ComboBox<CustomObjectShape> shapeComboBox,
            TextField objectWidthField,
            TextField objectHeightField,
            Slider opacitySlider,
            Slider lineWidthSlider,
            Slider fontSizeSlider,
            ComboBox<MarkerType> markerTypeComboBox
    ) {
        String groupName = groupComboBox.getEditor().getText().trim();
        if (groupName.isBlank()) {
            groupName = "Määramata";
        }
        Dimensions dimensions = readDimensions(
                placementType,
                tentWidthField,
                tentHeightField,
                shapeComboBox,
                objectWidthField,
                objectHeightField
        );
        if (dimensions == null) {
            return Optional.empty();
        }
        MarkerType markerType = selectedMarkerType(placementType, markerTypeComboBox);
        String name = nameField.getText().trim();
        if (name.isBlank()) {
            name = placementType == PlacementType.MARKER_OBJECT
                    ? markerType.displayName()
                    : placementType.defaultName();
        }
        double opacity = placementType == PlacementType.AREA_OBJECT
                || placementType == PlacementType.CUSTOM_OBJECT
                || placementType == PlacementType.TENT
                ? opacitySlider.getValue() / 100.0
                : AreaObject.DEFAULT_OPACITY;
        return Optional.of(new PlacementDetails(
                name,
                groupName,
                toHex(colorPicker.getValue()),
                dimensions.widthMeters(),
                dimensions.heightMeters(),
                opacity,
                lineWidthSlider.getValue(),
                fontSizeSlider.getValue(),
                dimensions.shape(),
                markerType
        ));
    }

    private static Dimensions readDimensions(
            PlacementType placementType,
            TextField tentWidthField,
            TextField tentHeightField,
            ComboBox<CustomObjectShape> shapeComboBox,
            TextField objectWidthField,
            TextField objectHeightField
    ) {
        if (placementType == PlacementType.TENT) {
            return readTentDimensions(tentWidthField, tentHeightField);
        }
        if (placementType == PlacementType.CUSTOM_OBJECT) {
            return readCustomObjectDimensions(shapeComboBox, objectWidthField, objectHeightField);
        }
        return new Dimensions(1.0, 1.0, CustomObjectShape.SQUARE);
    }

    private static Dimensions readTentDimensions(TextField widthField, TextField heightField) {
        try {
            double widthMeters = parseDouble(widthField.getText());
            double heightMeters = parseDouble(heightField.getText());
            if (widthMeters <= 0 || heightMeters <= 0) {
                throw new IllegalArgumentException("Telgi mõõdud peavad olema positiivsed.");
            }
            return new Dimensions(widthMeters, heightMeters, CustomObjectShape.SQUARE);
        } catch (NumberFormatException exception) {
            showError("Objekti ei lisatud", "Sisesta telgi laius ja pikkus arvuna meetrites.");
        } catch (IllegalArgumentException exception) {
            showError("Objekti ei lisatud", exception.getMessage());
        }
        return null;
    }

    private static Dimensions readCustomObjectDimensions(
            ComboBox<CustomObjectShape> shapeComboBox,
            TextField widthField,
            TextField heightField
    ) {
        CustomObjectShape shape = shapeComboBox.getSelectionModel().getSelectedItem();
        if (shape == null) {
            shape = CustomObjectShape.SQUARE;
        }
        try {
            double widthMeters = parseDouble(widthField.getText());
            double heightMeters = shape == CustomObjectShape.CIRCLE
                    ? widthMeters
                    : parseDouble(heightField.getText());
            if (widthMeters <= 0 || heightMeters <= 0) {
                throw new IllegalArgumentException("Objekti mõõdud peavad olema positiivsed.");
            }
            return new Dimensions(widthMeters, heightMeters, shape);
        } catch (NumberFormatException exception) {
            String message = shape == CustomObjectShape.CIRCLE
                    ? "Sisesta objekti läbimõõt arvuna meetrites."
                    : "Sisesta objekti laius ja pikkus arvuna meetrites.";
            showError("Objekti ei lisatud", message);
        } catch (IllegalArgumentException exception) {
            showError("Objekti ei lisatud", exception.getMessage());
        }
        return null;
    }

    private static MarkerType selectedMarkerType(
            PlacementType placementType,
            ComboBox<MarkerType> markerTypeComboBox
    ) {
        if (placementType != PlacementType.MARKER_OBJECT) {
            return MarkerType.WC;
        }
        MarkerType markerType = markerTypeComboBox.getSelectionModel().getSelectedItem();
        return markerType == null ? MarkerType.WC : markerType;
    }

    private static double parseDouble(String value) {
        return Double.parseDouble(value.trim().replace(',', '.'));
    }

    private static void updateObjectSizeFields(
            ComboBox<CustomObjectShape> shapeComboBox,
            Label objectWidthLabel,
            TextField objectWidthField,
            Label objectHeightLabel,
            TextField objectHeightField
    ) {
        boolean circleSelected = shapeComboBox.getSelectionModel().getSelectedItem() == CustomObjectShape.CIRCLE;
        objectWidthLabel.setText(circleSelected ? "Läbimõõt m" : "Laius m");
        objectWidthLabel.setVisible(true);
        objectWidthLabel.setManaged(true);
        objectWidthField.setVisible(true);
        objectWidthField.setManaged(true);
        objectHeightLabel.setVisible(!circleSelected);
        objectHeightLabel.setManaged(!circleSelected);
        objectHeightField.setVisible(!circleSelected);
        objectHeightField.setManaged(!circleSelected);
    }

    private static GridPane detailGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        return grid;
    }

    private static Slider createOpacitySlider(double initialPercentage) {
        Slider slider = new Slider(0, 100, initialPercentage);
        slider.setMajorTickUnit(25);
        slider.setMinorTickCount(4);
        slider.setBlockIncrement(5);
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setSnapToTicks(true);
        slider.setPrefWidth(210);
        return slider;
    }

    private static HBox opacityControl(Slider slider) {
        Label valueLabel = new Label("%.0f%%".formatted(slider.getValue()));
        valueLabel.setMinWidth(42);
        slider.valueProperty().addListener((observable, oldValue, newValue) ->
                valueLabel.setText("%.0f%%".formatted(newValue.doubleValue()))
        );
        HBox control = new HBox(8, slider, valueLabel);
        control.setAlignment(Pos.CENTER_LEFT);
        return control;
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
        Label valueLabel = new Label("%.0f px".formatted(slider.getValue()));
        valueLabel.setMinWidth(46);
        slider.valueProperty().addListener((observable, oldValue, newValue) ->
                valueLabel.setText("%.0f px".formatted(newValue.doubleValue()))
        );
        HBox control = new HBox(8, slider, valueLabel);
        control.setAlignment(Pos.CENTER_LEFT);
        return control;
    }

    private static StringConverter<CustomObjectShape> customObjectShapeConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(CustomObjectShape shape) {
                return shape == null ? "" : shape.displayName();
            }

            @Override
            public CustomObjectShape fromString(String text) {
                for (CustomObjectShape shape : CustomObjectShape.values()) {
                    if (shape.displayName().equals(text) || shape.name().equals(text)) {
                        return shape;
                    }
                }
                return CustomObjectShape.SQUARE;
            }
        };
    }

    private static StringConverter<MarkerType> markerTypeConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(MarkerType markerType) {
                return markerType == null ? "" : markerType.displayName();
            }

            @Override
            public MarkerType fromString(String text) {
                for (MarkerType markerType : MarkerType.values()) {
                    if (markerType.displayName().equals(text)) {
                        return markerType;
                    }
                }
                return MarkerType.WC;
            }
        };
    }

    private static String toHex(Color color) {
        int red = (int) Math.round(color.getRed() * 255);
        int green = (int) Math.round(color.getGreen() * 255);
        int blue = (int) Math.round(color.getBlue() * 255);
        return "#%02x%02x%02x".formatted(red, green, blue);
    }

    private static void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message == null || message.isBlank() ? "Tundmatu viga." : message);
        alert.showAndWait();
    }

    private record Dimensions(double widthMeters, double heightMeters, CustomObjectShape shape) {
    }
}

record PlacementDetails(
        String name,
        String groupName,
        String colorHex,
        double widthMeters,
        double heightMeters,
        double opacity,
        double lineWidthPixels,
        double fontSizePixels,
        CustomObjectShape shape,
        MarkerType markerType
) {
}

enum PlacementType {
    TENT("Telk", "Uus telk", "#e74c3c", true),
    POWER_SOURCE("Elektrikapp", "Uus kapp", "#2563eb", false),
    DISTRIBUTION_PANEL("Alajaotuskilp", "Uus alajaotuskilp", "#2563eb", false),
    CUSTOM_OBJECT("Objekt", "Uus objekt", "#9ca3af", true),
    TEXT_OBJECT("Tekst", "Uus tekst", "#111827", true),
    MARKER_OBJECT("Marker", "Uus marker", MarkerType.WC.defaultColorHex(), true),
    LINE_OBJECT("Joon", "Uus joon", "#0f766e", true),
    AREA_OBJECT("Ala", "Uus ala", "#f59e0b", true);

    private final String label;
    private final String defaultName;
    private final String defaultColorHex;
    private final boolean configurableColor;

    PlacementType(String label, String defaultName, String defaultColorHex, boolean configurableColor) {
        this.label = label;
        this.defaultName = defaultName;
        this.defaultColorHex = defaultColorHex;
        this.configurableColor = configurableColor;
    }

    String defaultName() {
        return defaultName;
    }

    String defaultColorHex() {
        return defaultColorHex;
    }

    boolean hasConfigurableColor() {
        return configurableColor;
    }

    @Override
    public String toString() {
        return label;
    }
}
