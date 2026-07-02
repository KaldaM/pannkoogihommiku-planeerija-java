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
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class PancakePlannerApp extends Application {
    private static final String DEFAULT_MAP_PATH = "classpath:/maps/tavakaart.png";
    private static final String ORTHOPHOTO_MAP_PATH = "classpath:/maps/ortofoto.png";
    private static final double PIXELS_PER_METER = 24.0;
    private static final double MIN_MAP_WIDTH = 760.0;
    private static final double MIN_MAP_HEIGHT = 560.0;

    private final PlanFactory planFactory = new PlanFactory();
    private final PowerSummaryService powerSummaryService = new PowerSummaryService();
    private final PlanFileService planFileService = new PlanFileService();

    private EventPlan plan;
    private Pane mapPane;
    private Pane mapContentPane;
    private Scale mapScale;
    private ImageView mapImageView;
    private double zoomLevel = 1.0;
    private double mapWidth = MIN_MAP_WIDTH;
    private double mapHeight = MIN_MAP_HEIGHT;
    private boolean measuringActive;
    private boolean mapDraggedSincePress;
    private Position measurementStart;
    private final List<Node> measurementNodes = new ArrayList<>();
    private ListView<String> summaryList;
    private Label selectedTypeLabel;
    private TextField nameField;
    private TextField groupField;
    private CheckBox lockedCheckBox;
    private TextField tentWidthField;
    private TextField tentHeightField;
    private TextField tentRotationField;
    private ColorPicker tentColorPicker;
    private ComboBox<PowerSourceChoice> powerSourceComboBox;
    private TextArea notesArea;
    private ListView<String> equipmentList;
    private TextField equipmentNameField;
    private TextField equipmentWattsField;
    private Button addEquipmentButton;
    private Button removeEquipmentButton;
    private Button deleteObjectButton;
    private Button choosePowerSourceButton;
    private PlannerObject selectedObject;
    private Tent pendingPowerSourceTent;
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
        stage.setMaximized(true);
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

        Button zoomInButton = new Button("+");
        zoomInButton.setOnAction(event -> changeZoom(1.2));

        Button zoomOutButton = new Button("-");
        zoomOutButton.setOnAction(event -> changeZoom(1 / 1.2));

        Button resetZoomButton = new Button("100%");
        resetZoomButton.setOnAction(event -> setZoom(1.0));

        ToggleButton measureButton = new ToggleButton("Mõõdulint");
        measureButton.setOnAction(event -> setMeasuringActive(measureButton.isSelected()));

        Button clearMeasurementsButton = new Button("Tühjenda mõõdud");
        clearMeasurementsButton.setOnAction(event -> clearMeasurements());

        return new ToolBar(
                addTentButton,
                addPowerSourceButton,
                saveButton,
                openButton,
                loadMapButton,
                defaultMapButton,
                orthophotoButton,
                zoomInButton,
                zoomOutButton,
                resetZoomButton,
                measureButton,
                clearMeasurementsButton
        );
    }

    private SplitPane createContent() {
        mapPane = new Pane();
        mapPane.setMinSize(MIN_MAP_WIDTH, MIN_MAP_HEIGHT);
        mapPane.setPrefSize(MIN_MAP_WIDTH, MIN_MAP_HEIGHT);
        mapPane.setStyle("-fx-background-color: #eef1ec;");
        mapScale = new Scale(1.0, 1.0, 0.0, 0.0);
        mapPane.getTransforms().add(mapScale);
        mapImageView = new ImageView();
        mapImageView.setPreserveRatio(true);
        mapPane.setOnMousePressed(event -> mapDraggedSincePress = false);
        mapPane.setOnMouseDragged(event -> mapDraggedSincePress = true);
        mapPane.setOnMouseClicked(event -> {
            if (measuringActive && !mapDraggedSincePress) {
                handleMeasureClick(new Position(event.getX(), event.getY()));
            }
        });
        mapContentPane = new Pane(mapPane);
        updateZoomContentSize();

        ScrollPane mapScrollPane = new ScrollPane(mapContentPane);
        mapScrollPane.setPannable(true);
        mapScrollPane.setFitToWidth(false);
        mapScrollPane.setFitToHeight(false);
        mapScrollPane.setStyle("-fx-background: #eef1ec;");

        BorderPane sidebar = new BorderPane();
        sidebar.setPadding(new Insets(12));
        sidebar.setTop(createDetailPanel());

        summaryList = new ListView<>();
        sidebar.setCenter(summaryList);

        SplitPane splitPane = new SplitPane(mapScrollPane, sidebar);
        splitPane.setDividerPositions(0.72);
        return splitPane;
    }

    private void changeZoom(double factor) {
        setZoom(zoomLevel * factor);
    }

    private void setZoom(double zoomLevel) {
        this.zoomLevel = Math.max(0.25, Math.min(4.0, zoomLevel));
        if (mapScale != null) {
            mapScale.setX(this.zoomLevel);
            mapScale.setY(this.zoomLevel);
        }
        updateZoomContentSize();
    }

    private void updateZoomContentSize() {
        if (mapPane != null) {
            mapPane.setMinSize(mapWidth, mapHeight);
            mapPane.setPrefSize(mapWidth, mapHeight);
        }
        if (mapContentPane != null) {
            mapContentPane.setMinSize(mapWidth * zoomLevel, mapHeight * zoomLevel);
            mapContentPane.setPrefSize(mapWidth * zoomLevel, mapHeight * zoomLevel);
        }
    }

    private VBox createDetailPanel() {
        selectedTypeLabel = new Label("Vali kaardilt objekt");
        nameField = new TextField();
        groupField = new TextField();
        lockedCheckBox = new CheckBox("Lukus");
        lockedCheckBox.setOnAction(event -> updateSelectedLock());
        tentWidthField = new TextField();
        tentHeightField = new TextField();
        tentRotationField = new TextField();
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
        form.addRow(4, new Label("Laius m"), tentWidthField);
        form.addRow(5, new Label("Pikkus m"), tentHeightField);
        form.addRow(6, new Label("Pööre °"), tentRotationField);
        form.addRow(7, new Label("Telgi värv"), tentColorPicker);
        form.addRow(8, new Label("Vooluallikas"), powerSourceComboBox);
        form.addRow(9, new Label("Märkmed"), notesArea);

        Button applyButton = new Button("Rakenda muudatused");
        applyButton.setOnAction(event -> applyDetails());
        choosePowerSourceButton = new Button("Vali kapp kaardilt");
        choosePowerSourceButton.setOnAction(event -> startPowerSourceSelectionFromMap());
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
        VBox detailPanel = new VBox(10, form, applyButton, choosePowerSourceButton, deleteObjectButton, equipmentPanel, summaryTitle);
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
        drawPowerConnections();
        for (PlannerObject object : plan.objects()) {
            if (object instanceof Tent tent) {
                drawTent(tent);
            } else if (object instanceof PowerSource source) {
                drawPowerSource(source);
            }
        }
        mapPane.getChildren().addAll(measurementNodes);
    }

    private void addMapImage() {
        Image image = loadImage(plan.mapImagePath());
        if (image == null) {
            return;
        }

        mapImageView.setImage(image);
        mapImageView.setFitWidth(image.getWidth());
        mapWidth = Math.max(MIN_MAP_WIDTH, image.getWidth());
        mapHeight = Math.max(MIN_MAP_HEIGHT, image.getHeight());
        updateZoomContentSize();
        mapPane.getChildren().add(mapImageView);
    }

    private void drawPowerConnections() {
        for (Tent tent : plan.tents()) {
            plan.findPowerConnectionForConsumer(tent.id())
                    .flatMap(connection -> plan.findObject(connection.sourceId()))
                    .filter(PowerSource.class::isInstance)
                    .map(PowerSource.class::cast)
                    .ifPresent(source -> drawPowerConnection(tent, source));
        }
    }

    private void drawPowerConnection(Tent tent, PowerSource source) {
        Position tentCenter = objectCenter(tent);
        Position sourceCenter = objectCenter(source);

        Line line = new Line(tentCenter.x(), tentCenter.y(), sourceCenter.x(), sourceCenter.y());
        line.setStroke(Color.web("#0f766e"));
        line.setStrokeWidth(2.5);
        line.setOpacity(0.85);
        line.setMouseTransparent(true);

        Label distanceLabel = new Label("%.1f m".formatted(distanceMeters(tentCenter, sourceCenter)));
        distanceLabel.setStyle("-fx-background-color: rgba(255,255,255,0.88); -fx-padding: 2 5 2 5; -fx-border-color: #0f766e;");
        distanceLabel.setLayoutX((tentCenter.x() + sourceCenter.x()) / 2 + 6);
        distanceLabel.setLayoutY((tentCenter.y() + sourceCenter.y()) / 2 + 6);
        distanceLabel.setMouseTransparent(true);

        mapPane.getChildren().addAll(line, distanceLabel);
    }

    private Position objectCenter(PlannerObject object) {
        if (object instanceof Tent tent) {
            double widthPixels = tent.widthMeters() * PIXELS_PER_METER;
            double heightPixels = tent.heightMeters() * PIXELS_PER_METER;
            return new Position(
                    tent.position().x() + widthPixels / 2,
                    tent.position().y() + heightPixels / 2
            );
        }
        return object.position();
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
        double widthPixels = tent.widthMeters() * PIXELS_PER_METER;
        double heightPixels = tent.heightMeters() * PIXELS_PER_METER;
        Position rotationOffset = rotationOffset(widthPixels, heightPixels, tent.rotationDegrees());
        Rectangle rectangle = new Rectangle(
                tent.position().x(),
                tent.position().y(),
                widthPixels,
                heightPixels
        );
        rectangle.setRotate(tent.rotationDegrees());
        rectangle.setTranslateX(rotationOffset.x());
        rectangle.setTranslateY(rotationOffset.y());
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
            if (measuringActive) {
                Point2D mapPoint = mapPane.sceneToLocal(event.getSceneX(), event.getSceneY());
                handleMeasureClick(new Position(mapPoint.getX(), mapPoint.getY()));
                event.consume();
                return;
            }
            if (pendingPowerSourceTent != null && object instanceof PowerSource source) {
                connectPowerSourceFromMap(source);
                event.consume();
                return;
            }
            selectObject(object);
            event.consume();
        });
    }

    private void makeDraggable(Node node, PlannerObject object) {
        final Delta dragDelta = new Delta();
        node.setOnMousePressed(event -> {
            if (measuringActive) {
                return;
            }
            if (pendingPowerSourceTent != null && object instanceof PowerSource source) {
                connectPowerSourceFromMap(source);
                event.consume();
                return;
            }
            selectObject(object);
            Point2D mapPoint = mapPane.sceneToLocal(event.getSceneX(), event.getSceneY());
            dragDelta.x = mapPoint.getX() - object.position().x();
            dragDelta.y = mapPoint.getY() - object.position().y();
            event.consume();
        });
        node.setOnMouseDragged(event -> {
            if (measuringActive) {
                return;
            }
            if (object.locked()) {
                return;
            }
            Point2D mapPoint = mapPane.sceneToLocal(event.getSceneX(), event.getSceneY());
            object.moveTo(object.position().moveTo(mapPoint.getX() - dragDelta.x, mapPoint.getY() - dragDelta.y));
            redrawMap();
            refreshSummary();
            event.consume();
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
        tentWidthField.setDisable(!tentSelected);
        tentHeightField.setDisable(!tentSelected);
        tentRotationField.setDisable(!tentSelected);
        tentColorPicker.setDisable(!tentSelected);
        powerSourceComboBox.setDisable(!tentSelected);
        equipmentList.setDisable(!tentSelected);
        equipmentNameField.setDisable(!tentSelected);
        equipmentWattsField.setDisable(!tentSelected);
        addEquipmentButton.setDisable(!tentSelected);
        removeEquipmentButton.setDisable(!tentSelected);
        choosePowerSourceButton.setDisable(!tentSelected);
        boolean selectingPowerSourceForThisTent = tentSelected
                && pendingPowerSourceTent != null
                && pendingPowerSourceTent.id().equals(selectedObject.id());
        choosePowerSourceButton.setText(selectingPowerSourceForThisTent
                ? "Tühista kapi valik"
                : "Vali kapp kaardilt");

        if (!hasSelection) {
            selectedTypeLabel.setText("Vali kaardilt objekt");
            nameField.clear();
            groupField.clear();
            notesArea.clear();
            lockedCheckBox.setSelected(false);
            tentWidthField.clear();
            tentHeightField.clear();
            tentRotationField.clear();
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
            tentWidthField.setText(formatMeters(tent.widthMeters()));
            tentHeightField.setText(formatMeters(tent.heightMeters()));
            tentRotationField.setText(formatDegrees(tent.rotationDegrees()));
            tentColorPicker.setValue(Color.web(tent.colorHex()));
        } else {
            tentWidthField.clear();
            tentHeightField.clear();
            tentRotationField.clear();
            tentColorPicker.setValue(Color.web("#2563eb"));
        }
        refreshPowerSourceChoices();
        refreshEquipmentList();
    }

    private void startPowerSourceSelectionFromMap() {
        if (!(selectedObject instanceof Tent tent)) {
            return;
        }
        if (pendingPowerSourceTent != null && pendingPowerSourceTent.id().equals(tent.id())) {
            pendingPowerSourceTent = null;
            refreshDetails();
            return;
        }
        pendingPowerSourceTent = tent;
        refreshDetails();
    }

    private void connectPowerSourceFromMap(PowerSource source) {
        Tent tent = pendingPowerSourceTent;
        if (tent == null) {
            return;
        }
        plan.connectToPower(source.id(), tent.id(), ConnectorType.SCHUKO_230V);
        pendingPowerSourceTent = null;
        selectedObject = tent;
        refreshDetails();
        redrawMap();
        refreshSummary();
    }

    private void updateSelectedLock() {
        if (selectedObject == null) {
            return;
        }
        selectedObject.setLocked(lockedCheckBox.isSelected());
        redrawMap();
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
            if (!applyTentSize(tent)) {
                return;
            }
            if (!applyTentRotation(tent)) {
                return;
            }
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
        pendingPowerSourceTent = null;
        redrawMap();
        refreshSummary();
        refreshDetails();
    }

    private void setMeasuringActive(boolean measuringActive) {
        this.measuringActive = measuringActive;
        measurementStart = null;
    }

    private void handleMeasureClick(Position point) {
        if (measurementStart == null) {
            measurementStart = point;
            Circle marker = createMeasurementMarker(point);
            measurementNodes.add(marker);
            mapPane.getChildren().add(marker);
            return;
        }

        Position end = point;
        Line line = new Line(measurementStart.x(), measurementStart.y(), end.x(), end.y());
        line.setStroke(Color.web("#111827"));
        line.setStrokeWidth(2);

        Circle endMarker = createMeasurementMarker(end);
        Label distanceLabel = new Label("%.2f m".formatted(distanceMeters(measurementStart, end)));
        distanceLabel.setStyle("-fx-background-color: white; -fx-padding: 2 4 2 4;");
        distanceLabel.setLayoutX((measurementStart.x() + end.x()) / 2 + 6);
        distanceLabel.setLayoutY((measurementStart.y() + end.y()) / 2 + 6);

        measurementNodes.add(line);
        measurementNodes.add(endMarker);
        measurementNodes.add(distanceLabel);
        mapPane.getChildren().addAll(line, endMarker, distanceLabel);
        measurementStart = null;
    }

    private Circle createMeasurementMarker(Position point) {
        Circle marker = new Circle(point.x(), point.y(), 4);
        marker.setFill(Color.web("#111827"));
        marker.setStroke(Color.WHITE);
        return marker;
    }

    private double distanceMeters(Position start, Position end) {
        double deltaX = end.x() - start.x();
        double deltaY = end.y() - start.y();
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY) / PIXELS_PER_METER;
    }

    private void clearMeasurements() {
        measurementNodes.clear();
        measurementStart = null;
        redrawMap();
    }

    private boolean applyTentSize(Tent tent) {
        try {
            double widthMeters = Double.parseDouble(tentWidthField.getText().trim().replace(',', '.'));
            double heightMeters = Double.parseDouble(tentHeightField.getText().trim().replace(',', '.'));
            tent.setSizeMeters(widthMeters, heightMeters);
            return true;
        } catch (NumberFormatException exception) {
            showError("Mõõte ei muudetud", "Sisesta telgi laius ja pikkus arvuna meetrites.");
            return false;
        } catch (IllegalArgumentException exception) {
            showError("Mõõte ei muudetud", exception.getMessage());
            return false;
        }
    }

    private boolean applyTentRotation(Tent tent) {
        try {
            double rotationDegrees = Double.parseDouble(tentRotationField.getText().trim().replace(',', '.'));
            tent.setRotationDegrees(rotationDegrees);
            return true;
        } catch (NumberFormatException exception) {
            showError("Pööret ei muudetud", "Sisesta telgi pööre arvuna kraadides.");
            return false;
        }
    }

    private String formatMeters(double meters) {
        if (meters == Math.rint(meters)) {
            return "%.0f".formatted(meters);
        }
        return "%.2f".formatted(meters);
    }

    private String formatDegrees(double degrees) {
        if (degrees == Math.rint(degrees)) {
            return "%.0f".formatted(degrees);
        }
        return "%.2f".formatted(degrees);
    }

    private Position rotationOffset(double width, double height, double degrees) {
        double radians = Math.toRadians(degrees);
        double sin = Math.abs(Math.sin(radians));
        double cos = Math.abs(Math.cos(radians));
        double rotatedWidth = width * cos + height * sin;
        double rotatedHeight = width * sin + height * cos;
        return new Position((rotatedWidth - width) / 2, (rotatedHeight - height) / 2);
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
        addCableSummary();
        addGroupSummary();
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

    private void addCableSummary() {
        if (plan.powerConnections().isEmpty()) {
            return;
        }

        List<String> cableRows = new ArrayList<>();
        double totalLengthMeters = 0.0;

        for (Tent tent : plan.tents()) {
            PowerSource source = plan.findPowerConnectionForConsumer(tent.id())
                    .flatMap(connection -> plan.findObject(connection.sourceId()))
                    .filter(PowerSource.class::isInstance)
                    .map(PowerSource.class::cast)
                    .orElse(null);
            if (source == null) {
                continue;
            }

            double lengthMeters = distanceMeters(objectCenter(tent), objectCenter(source));
            totalLengthMeters += lengthMeters;
            cableRows.add("  - %s -> %s: %.1f m".formatted(
                    tent.name(),
                    source.name(),
                    lengthMeters
            ));
        }

        if (cableRows.isEmpty()) {
            return;
        }

        summaryList.getItems().add("");
        summaryList.getItems().add("Kaablid");
        summaryList.getItems().addAll(cableRows);
        summaryList.getItems().add("Kokku: %.1f m".formatted(totalLengthMeters));
    }

    private void addGroupSummary() {
        if (plan.objects().isEmpty()) {
            return;
        }

        Map<String, List<PlannerObject>> objectsByGroup = new TreeMap<>();
        for (PlannerObject object : plan.objects()) {
            String groupName = object.groupName().isBlank() ? "Määramata" : object.groupName();
            objectsByGroup.computeIfAbsent(groupName, ignored -> new ArrayList<>()).add(object);
        }

        summaryList.getItems().add("");
        summaryList.getItems().add("Grupid");
        for (Map.Entry<String, List<PlannerObject>> entry : objectsByGroup.entrySet()) {
            summaryList.getItems().add(entry.getKey());
            for (PlannerObject object : entry.getValue()) {
                summaryList.getItems().add("  - %s (%s)".formatted(object.name(), objectTypeName(object)));
            }
        }
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
            pendingPowerSourceTent = null;
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
