package ee.matteus.pannukas.gui;

import ee.matteus.pannukas.core.model.ConnectorType;
import ee.matteus.pannukas.core.model.Equipment;
import ee.matteus.pannukas.core.model.EventPlan;
import ee.matteus.pannukas.core.model.PlannerObject;
import ee.matteus.pannukas.core.model.Position;
import ee.matteus.pannukas.core.model.PowerOutlet;
import ee.matteus.pannukas.core.model.PowerSource;
import ee.matteus.pannukas.core.model.Tent;
import ee.matteus.pannukas.core.service.PlanFactory;
import ee.matteus.pannukas.core.service.PlanFileService;
import ee.matteus.pannukas.core.service.PowerSummary;
import ee.matteus.pannukas.core.service.PowerSummaryService;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class PancakePlannerApp extends Application {
    private static final String DEFAULT_MAP_PATH = "classpath:/maps/tavakaart.png";
    private static final String ORTHOPHOTO_MAP_PATH = "classpath:/maps/ortofoto.png";

    private final PlanFactory planFactory = new PlanFactory();
    private final PowerSummaryService powerSummaryService = new PowerSummaryService();
    private final PlanFileService planFileService = new PlanFileService();

    private EventPlan plan;
    private Pane mapPane;
    private ImageView mapImageView;
    private ListView<String> summaryList;
    private Label selectedTypeLabel;
    private TextField nameField;
    private TextField groupField;
    private CheckBox lockedCheckBox;
    private ColorPicker tentColorPicker;
    private ComboBox<PowerSourceChoice> powerSourceComboBox;
    private TextArea notesArea;
    private ListView<String> equipmentList;
    private TextField equipmentNameField;
    private TextField equipmentWattsField;
    private Button addEquipmentButton;
    private Button removeEquipmentButton;
    private Button deleteObjectButton;
    private PlannerObject selectedObject;
    private Stage stage;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        plan = planFactory.createDemoPlan();

        BorderPane root = new BorderPane();
        root.setTop(createToolbar());
        root.setCenter(createContent());

        redrawMap();
        refreshSummary();
        refreshDetails();

        Scene scene = new Scene(root, 1200, 760);
        stage.setTitle("Pannkoogihommiku planeerija");
        stage.setScene(scene);
        stage.show();
    }

    private ToolBar createToolbar() {
        Button addTentButton = new Button("Lisa telk");
        addTentButton.setOnAction(event -> addTent());

        Button addPowerSourceButton = new Button("Lisa kapp");
        addPowerSourceButton.setOnAction(event -> addPowerSource());

        Button saveButton = new Button("Salvesta");
        saveButton.setOnAction(event -> savePlan());

        Button openButton = new Button("Ava");
        openButton.setOnAction(event -> openPlan());

        Button loadMapButton = new Button("Laadi kaart");
        loadMapButton.setOnAction(event -> loadMapImage());

        Button defaultMapButton = new Button("Tavakaart");
        defaultMapButton.setOnAction(event -> setMapImage(DEFAULT_MAP_PATH));

        Button orthophotoButton = new Button("Ortofoto");
        orthophotoButton.setOnAction(event -> setMapImage(ORTHOPHOTO_MAP_PATH));

        return new ToolBar(
                addTentButton,
                addPowerSourceButton,
                saveButton,
                openButton,
                loadMapButton,
                defaultMapButton,
                orthophotoButton
        );
    }

    private SplitPane createContent() {
        mapPane = new Pane();
        mapPane.setMinSize(760, 560);
        mapPane.setStyle("-fx-background-color: #eef1ec;");
        mapImageView = new ImageView();
        mapImageView.setPreserveRatio(true);

        BorderPane sidebar = new BorderPane();
        sidebar.setPadding(new Insets(12));
        sidebar.setTop(createDetailPanel());

        summaryList = new ListView<>();
        sidebar.setCenter(summaryList);

        SplitPane splitPane = new SplitPane(mapPane, sidebar);
        splitPane.setDividerPositions(0.72);
        return splitPane;
    }

    private VBox createDetailPanel() {
        selectedTypeLabel = new Label("Vali kaardilt objekt");
        nameField = new TextField();
        groupField = new TextField();
        lockedCheckBox = new CheckBox("Lukus");
        tentColorPicker = new ColorPicker();
        powerSourceComboBox = new ComboBox<>();
        notesArea = new TextArea();
        notesArea.setPrefRowCount(3);
        equipmentList = new ListView<>();
        equipmentList.setPrefHeight(120);
        equipmentNameField = new TextField();
        equipmentNameField.setPromptText("Seadme nimi");
        equipmentWattsField = new TextField();
        equipmentWattsField.setPromptText("W");
        addEquipmentButton = new Button("Lisa seade");
        addEquipmentButton.setOnAction(event -> addEquipmentToSelectedTent());
        removeEquipmentButton = new Button("Eemalda valitud");
        removeEquipmentButton.setOnAction(event -> removeSelectedEquipment());

        GridPane form = new GridPane();
        form.setHgap(8);
        form.setVgap(8);
        form.addRow(0, new Label("Tüüp"), selectedTypeLabel);
        form.addRow(1, new Label("Nimi"), nameField);
        form.addRow(2, new Label("Grupp"), groupField);
        form.addRow(3, new Label("Lukustus"), lockedCheckBox);
        form.addRow(4, new Label("Telgi värv"), tentColorPicker);
        form.addRow(5, new Label("Vooluallikas"), powerSourceComboBox);
        form.addRow(6, new Label("Märkmed"), notesArea);

        Button applyButton = new Button("Rakenda muudatused");
        applyButton.setOnAction(event -> applyDetails());
        deleteObjectButton = new Button("Kustuta objekt");
        deleteObjectButton.setOnAction(event -> deleteSelectedObject());

        Label summaryTitle = new Label("Voolu kokkuvõte");

        VBox equipmentPanel = new VBox(
                8,
                new Label("Telgi seadmed"),
                equipmentList,
                equipmentNameField,
                equipmentWattsField,
                addEquipmentButton,
                removeEquipmentButton
        );
        VBox detailPanel = new VBox(10, form, applyButton, deleteObjectButton, equipmentPanel, summaryTitle);
        detailPanel.setPadding(new Insets(0, 0, 12, 0));
        return detailPanel;
    }

    private void addTent() {
        Tent tent = new Tent(planFactory.newId(), "Uus telk", new Position(120, 120));
        tent.setGroupName("Määramata");
        plan.addObject(tent);
        selectObject(tent);
        refreshSummary();
    }

    private void addPowerSource() {
        PowerSource source = new PowerSource(planFactory.newId(), "Uus kapp", new Position(160, 160));
        source.addOutlet(new PowerOutlet(planFactory.newId(), ConnectorType.SCHUKO_230V, 11000));
        plan.addObject(source);
        selectObject(source);
        refreshSummary();
    }

    private void redrawMap() {
        mapPane.getChildren().clear();
        addMapImage();
        for (PlannerObject object : plan.objects()) {
            if (object instanceof Tent tent) {
                drawTent(tent);
            } else if (object instanceof PowerSource source) {
                drawPowerSource(source);
            }
        }
    }

    private void addMapImage() {
        Image image = loadImage(plan.mapImagePath());
        if (image == null) {
            return;
        }

        mapImageView.setImage(image);
        mapImageView.setFitWidth(image.getWidth());
        mapPane.getChildren().add(mapImageView);
    }

    private Image loadImage(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return null;
        }

        try {
            if (imagePath.startsWith("classpath:")) {
                String resourcePath = imagePath.substring("classpath:".length());
                try (InputStream inputStream = getClass().getResourceAsStream(resourcePath)) {
                    return inputStream == null ? null : new Image(inputStream);
                }
            }

            File imageFile = new File(imagePath);
            return imageFile.exists() ? new Image(imageFile.toURI().toString()) : null;
        } catch (RuntimeException | IOException exception) {
            return null;
        }
    }

    private void drawTent(Tent tent) {
        Rectangle rectangle = new Rectangle(tent.position().x(), tent.position().y(), 72, 72);
        rectangle.setArcWidth(4);
        rectangle.setArcHeight(4);
        rectangle.setFill(Color.web(tent.colorHex()));
        rectangle.setStroke(Color.web("#222222"));
        rectangle.setStrokeWidth(isSelected(tent) ? 4 : 1);
        makeSelectable(rectangle, tent);
        makeDraggable(rectangle, tent);

        Label label = new Label(tent.name());
        label.setLayoutX(tent.position().x());
        label.setLayoutY(tent.position().y() - 24);
        makeSelectable(label, tent);

        mapPane.getChildren().addAll(rectangle, label);
    }

    private void drawPowerSource(PowerSource source) {
        Circle circle = new Circle(source.position().x(), source.position().y(), 12);
        circle.setFill(Color.web("#2563eb"));
        circle.setStroke(Color.web("#111827"));
        circle.setStrokeWidth(isSelected(source) ? 4 : 1);
        makeSelectable(circle, source);
        makeDraggable(circle, source);

        Label label = new Label(source.name());
        label.setLayoutX(source.position().x() + 16);
        label.setLayoutY(source.position().y() - 12);
        makeSelectable(label, source);

        mapPane.getChildren().addAll(circle, label);
    }

    private void makeSelectable(Node node, PlannerObject object) {
        node.setOnMouseClicked(event -> {
            selectObject(object);
            event.consume();
        });
    }

    private void makeDraggable(Node node, PlannerObject object) {
        final Delta dragDelta = new Delta();
        node.setOnMousePressed(event -> {
            selectObject(object);
            dragDelta.x = event.getSceneX() - object.position().x();
            dragDelta.y = event.getSceneY() - object.position().y();
        });
        node.setOnMouseDragged(event -> {
            if (object.locked()) {
                return;
            }
            object.moveTo(object.position().moveTo(event.getSceneX() - dragDelta.x, event.getSceneY() - dragDelta.y));
            redrawMap();
            refreshSummary();
        });
    }

    private void selectObject(PlannerObject object) {
        selectedObject = object;
        refreshDetails();
        redrawMap();
    }

    private boolean isSelected(PlannerObject object) {
        return selectedObject != null && selectedObject.id().equals(object.id());
    }

    private void refreshDetails() {
        boolean hasSelection = selectedObject != null;
        boolean tentSelected = selectedObject instanceof Tent;
        nameField.setDisable(!hasSelection);
        groupField.setDisable(!hasSelection);
        notesArea.setDisable(!hasSelection);
        lockedCheckBox.setDisable(!hasSelection);
        deleteObjectButton.setDisable(!hasSelection);
        tentColorPicker.setDisable(!tentSelected);
        powerSourceComboBox.setDisable(!tentSelected);
        equipmentList.setDisable(!tentSelected);
        equipmentNameField.setDisable(!tentSelected);
        equipmentWattsField.setDisable(!tentSelected);
        addEquipmentButton.setDisable(!tentSelected);
        removeEquipmentButton.setDisable(!tentSelected);

        if (!hasSelection) {
            selectedTypeLabel.setText("Vali kaardilt objekt");
            nameField.clear();
            groupField.clear();
            notesArea.clear();
            lockedCheckBox.setSelected(false);
            tentColorPicker.setValue(Color.web("#e74c3c"));
            refreshPowerSourceChoices();
            refreshEquipmentList();
            return;
        }

        selectedTypeLabel.setText(objectTypeName(selectedObject));
        nameField.setText(selectedObject.name());
        groupField.setText(selectedObject.groupName());
        notesArea.setText(selectedObject.notes());
        lockedCheckBox.setSelected(selectedObject.locked());
        if (selectedObject instanceof Tent tent) {
            tentColorPicker.setValue(Color.web(tent.colorHex()));
        } else {
            tentColorPicker.setValue(Color.web("#2563eb"));
        }
        refreshPowerSourceChoices();
        refreshEquipmentList();
    }

    private String objectTypeName(PlannerObject object) {
        if (object instanceof Tent) {
            return "Telk";
        }
        if (object instanceof PowerSource) {
            return "Elektrikapp";
        }
        return "Objekt";
    }

    private void applyDetails() {
        if (selectedObject == null) {
            return;
        }

        selectedObject.rename(nameField.getText());
        selectedObject.setGroupName(groupField.getText());
        selectedObject.setNotes(notesArea.getText());
        selectedObject.setLocked(lockedCheckBox.isSelected());
        if (selectedObject instanceof Tent tent) {
            tent.setColorHex(toHex(tentColorPicker.getValue()));
            applySelectedPowerSource(tent);
        }
        redrawMap();
        refreshSummary();
    }

    private void deleteSelectedObject() {
        if (selectedObject == null) {
            return;
        }

        plan.removeObject(selectedObject.id());
        selectedObject = null;
        redrawMap();
        refreshSummary();
        refreshDetails();
    }

    private void refreshPowerSourceChoices() {
        powerSourceComboBox.getItems().clear();
        powerSourceComboBox.getItems().add(PowerSourceChoice.none());
        for (PowerSource source : plan.powerSources()) {
            powerSourceComboBox.getItems().add(new PowerSourceChoice(source.id(), source.name()));
        }

        powerSourceComboBox.getSelectionModel().selectFirst();
        if (!(selectedObject instanceof Tent tent)) {
            return;
        }

        plan.findPowerConnectionForConsumer(tent.id())
                .flatMap(connection -> powerSourceComboBox.getItems().stream()
                        .filter(choice -> choice.sourceId().equals(connection.sourceId()))
                        .findFirst())
                .ifPresent(choice -> powerSourceComboBox.getSelectionModel().select(choice));
    }

    private void applySelectedPowerSource(Tent tent) {
        PowerSourceChoice selectedSource = powerSourceComboBox.getSelectionModel().getSelectedItem();
        if (selectedSource == null || selectedSource.isNone()) {
            plan.disconnectPower(tent.id());
            return;
        }

        plan.connectToPower(selectedSource.sourceId(), tent.id(), ConnectorType.SCHUKO_230V);
    }

    private void addEquipmentToSelectedTent() {
        if (!(selectedObject instanceof Tent tent)) {
            return;
        }

        String name = equipmentNameField.getText().trim();
        if (name.isBlank()) {
            showError("Seadet ei lisatud", "Sisesta seadme nimi.");
            return;
        }

        int watts;
        try {
            watts = Integer.parseInt(equipmentWattsField.getText().trim());
        } catch (NumberFormatException exception) {
            showError("Seadet ei lisatud", "Sisesta võimsus täisarvuna vattides.");
            return;
        }

        try {
            tent.addEquipment(new Equipment(name, watts));
        } catch (IllegalArgumentException exception) {
            showError("Seadet ei lisatud", exception.getMessage());
            return;
        }

        equipmentNameField.clear();
        equipmentWattsField.clear();
        refreshEquipmentList();
        refreshSummary();
    }

    private void removeSelectedEquipment() {
        if (!(selectedObject instanceof Tent tent)) {
            return;
        }

        int selectedIndex = equipmentList.getSelectionModel().getSelectedIndex();
        if (selectedIndex < 0) {
            return;
        }

        tent.removeEquipment(selectedIndex);
        refreshEquipmentList();
        refreshSummary();
    }

    private void refreshEquipmentList() {
        equipmentList.getItems().clear();
        if (!(selectedObject instanceof Tent tent)) {
            return;
        }

        for (Equipment item : tent.equipment()) {
            equipmentList.getItems().add("%s - %d W".formatted(item.name(), item.requiredWatts()));
        }
    }

    private String toHex(Color color) {
        int red = (int) Math.round(color.getRed() * 255);
        int green = (int) Math.round(color.getGreen() * 255);
        int blue = (int) Math.round(color.getBlue() * 255);
        return "#%02x%02x%02x".formatted(red, green, blue);
    }

    private void refreshSummary() {
        summaryList.getItems().clear();
        for (PowerSummary summary : powerSummaryService.summaries(plan)) {
            summaryList.getItems().add("%s: %d W kasutusel, %d W alles".formatted(
                    summary.sourceName(),
                    summary.usedWatts(),
                    summary.remainingWatts()
            ));
            addConnectedConsumers(summary.sourceId());
        }
    }

    private void addConnectedConsumers(String sourceId) {
        plan.powerSources().stream()
                .filter(source -> source.id().equals(sourceId))
                .findFirst()
                .ifPresent(source -> plan.powerConnections().stream()
                        .filter(connection -> connection.sourceId().equals(source.id()))
                        .map(connection -> plan.findObject(connection.consumerId()))
                        .flatMap(optional -> optional.stream())
                        .filter(Tent.class::isInstance)
                        .map(Tent.class::cast)
                        .forEach(tent -> summaryList.getItems().add("  - %s: %d W".formatted(
                                tent.name(),
                                tent.requiredWatts()
                        ))));
    }

    private void savePlan() {
        FileChooser fileChooser = createPlanFileChooser();
        File file = fileChooser.showSaveDialog(stage);
        if (file == null) {
            return;
        }

        try {
            planFileService.save(plan, file.toPath());
        } catch (IOException exception) {
            showError("Salvestamine ebaõnnestus", exception.getMessage());
        }
    }

    private void loadMapImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Vali kaardipilt");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Pildifail", "*.png", "*.jpg", "*.jpeg"));
        File file = fileChooser.showOpenDialog(stage);
        if (file == null) {
            return;
        }

        setMapImage(file.getAbsolutePath());
    }

    private void setMapImage(String imagePath) {
        plan.setMapImagePath(imagePath);
        redrawMap();
    }

    private void openPlan() {
        FileChooser fileChooser = createPlanFileChooser();
        File file = fileChooser.showOpenDialog(stage);
        if (file == null) {
            return;
        }

        try {
            plan = planFileService.load(file.toPath());
            selectedObject = null;
            redrawMap();
            refreshSummary();
            refreshDetails();
        } catch (IOException | RuntimeException exception) {
            showError("Faili avamine ebaõnnestus", exception.getMessage());
        }
    }

    private FileChooser createPlanFileChooser() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Pannkoogihommiku plaan");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Plaanifail", "*.pplan"));
        return fileChooser;
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message == null || message.isBlank() ? "Tundmatu viga." : message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }

    private record PowerSourceChoice(String sourceId, String name) {
        private static PowerSourceChoice none() {
            return new PowerSourceChoice("", "Määramata");
        }

        private boolean isNone() {
            return sourceId.isBlank();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static class Delta {
        private double x;
        private double y;
    }
}
