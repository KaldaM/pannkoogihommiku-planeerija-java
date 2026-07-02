package ee.matteus.pannukas.gui;

import ee.matteus.pannukas.core.model.ConnectorType;
import ee.matteus.pannukas.core.model.Equipment;
import ee.matteus.pannukas.core.model.EventPlan;
import ee.matteus.pannukas.core.model.PlannerObject;
import ee.matteus.pannukas.core.model.Position;
import ee.matteus.pannukas.core.model.PowerConnection;
import ee.matteus.pannukas.core.model.PowerConsumer;
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
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
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
import javafx.util.StringConverter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final Set<String> visibleGroups = new HashSet<>();
    private Set<String> knownGroups = new HashSet<>();
    private ListView<String> summaryList;
    private VBox groupFilterPanel;
    private TextField planNameField;
    private Label selectedTypeLabel;
    private TextField nameField;
    private TextField groupField;
    private CheckBox lockedCheckBox;
    private TextField tentWidthField;
    private TextField tentHeightField;
    private TextField tentRotationField;
    private ColorPicker tentColorPicker;
    private ComboBox<PowerSourceChoice> powerSourceComboBox;
    private ComboBox<ConnectorType> connectionTypeComboBox;
    private ComboBox<OutletChoice> connectionOutletComboBox;
    private TextArea notesArea;
    private ListView<String> equipmentList;
    private TextField equipmentNameField;
    private TextField equipmentWattsField;
    private Button addEquipmentButton;
    private Button removeEquipmentButton;
    private ListView<String> outletList;
    private TextField outletNameField;
    private ComboBox<ConnectorType> outletTypeComboBox;
    private TextField outletCapacityWattsField;
    private Button addOutletButton;
    private Button updateOutletButton;
    private Button removeOutletButton;
    private Button deleteObjectButton;
    private Button choosePowerSourceButton;
    private ToggleButton measureButton;
    private Button addTentButton;
    private Button addPowerSourceButton;
    private PlannerObject selectedObject;
    private Tent pendingPowerSourceTent;
    private boolean pendingTentPlacement;
    private boolean pendingPowerSourcePlacement;
    private boolean unsavedChanges;
    private Stage stage;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        plan = planFactory.createEmptyPlan();

        BorderPane root = new BorderPane();
        root.setTop(createToolbar());
        root.setCenter(createContent());

        refreshGroupFilters();
        redrawMap();
        refreshSummary();
        refreshDetails();

        Scene scene = new Scene(root, 1200, 760);
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.setOnCloseRequest(event -> {
            if (!confirmDiscardUnsavedChanges()) {
                event.consume();
            }
        });
        markClean();
        stage.show();
    }

    private ToolBar createToolbar() {
        Button newPlanButton = new Button("Uus plaan");
        newPlanButton.setOnAction(event -> newPlan());

        addTentButton = new Button("Lisa telk");
        addTentButton.setOnAction(event -> addTent());

        addPowerSourceButton = new Button("Lisa kapp");
        addPowerSourceButton.setOnAction(event -> addPowerSource());

        Button saveButton = new Button("Salvesta");
        saveButton.setOnAction(event -> savePlan());

        Button exportSummaryButton = new Button("Ekspordi kokkuvõte");
        exportSummaryButton.setOnAction(event -> exportSummary());

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

        measureButton = new ToggleButton("Mõõdulint");
        measureButton.setOnAction(event -> setMeasuringActive(measureButton.isSelected()));

        Button clearMeasurementsButton = new Button("Tühjenda mõõdud");
        clearMeasurementsButton.setOnAction(event -> clearMeasurements());

        return new ToolBar(
                newPlanButton,
                addTentButton,
                addPowerSourceButton,
                saveButton,
                exportSummaryButton,
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
            if (pendingTentPlacement && !mapDraggedSincePress) {
                placeTent(new Position(event.getX(), event.getY()));
                return;
            }
            if (pendingPowerSourcePlacement && !mapDraggedSincePress) {
                placePowerSource(new Position(event.getX(), event.getY()));
                return;
            }
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

        VBox sidebar = new VBox(10);
        sidebar.setPadding(new Insets(12));
        sidebar.getChildren().add(createDetailPanel());

        summaryList = new ListView<>();
        summaryList.setMinHeight(180);
        summaryList.setPrefHeight(260);
        summaryList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setStyle(!empty && item != null && item.contains("ULEKOORMUS")
                        ? "-fx-text-fill: #b91c1c; -fx-font-weight: bold;"
                        : "");
            }
        });
        sidebar.getChildren().add(summaryList);
        groupFilterPanel = new VBox(6);
        groupFilterPanel.setPadding(new Insets(12, 0, 0, 0));
        sidebar.getChildren().add(groupFilterPanel);

        ScrollPane sidebarScrollPane = new ScrollPane(sidebar);
        sidebarScrollPane.setFitToWidth(true);

        SplitPane splitPane = new SplitPane(mapScrollPane, sidebarScrollPane);
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
        planNameField = new TextField(plan.name());
        Button applyPlanNameButton = new Button("Rakenda nimi");
        applyPlanNameButton.setOnAction(event -> applyPlanName());

        GridPane planForm = new GridPane();
        planForm.setHgap(8);
        planForm.setVgap(8);
        planForm.addRow(0, new Label("Plaani nimi"), planNameField);
        planForm.addRow(1, new Label(""), applyPlanNameButton);

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
        powerSourceComboBox.setOnAction(event -> refreshConnectionTypeChoices(null));
        connectionTypeComboBox = new ComboBox<>();
        connectionTypeComboBox.setConverter(connectorTypeConverter());
        connectionTypeComboBox.setOnAction(event -> refreshConnectionOutletChoices(null));
        connectionOutletComboBox = new ComboBox<>();
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
        outletList = new ListView<>();
        outletList.setPrefHeight(100);
        outletList.getSelectionModel().selectedIndexProperty()
                .addListener((observable, oldValue, newValue) -> loadSelectedOutletDetails());
        outletNameField = new TextField();
        outletNameField.setPromptText("Valjundi nimi");
        outletTypeComboBox = new ComboBox<>();
        outletTypeComboBox.getItems().addAll(ConnectorType.values());
        outletTypeComboBox.setConverter(connectorTypeConverter());
        outletTypeComboBox.getSelectionModel().select(ConnectorType.SCHUKO_230V);
        outletTypeComboBox.setOnAction(event -> updateDefaultOutletCapacity());
        outletCapacityWattsField = new TextField();
        outletCapacityWattsField.setPromptText("W");
        updateDefaultOutletCapacity();
        addOutletButton = new Button("Lisa väljund");
        addOutletButton.setOnAction(event -> addOutletToSelectedPowerSource());
        updateOutletButton = new Button("Muuda valitud väljundit");
        updateOutletButton.setOnAction(event -> updateSelectedOutlet());
        removeOutletButton = new Button("Eemalda valitud");
        removeOutletButton.setOnAction(event -> removeSelectedOutlet());

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
        form.addRow(9, new Label("Yhenduse tyyp"), connectionTypeComboBox);
        form.addRow(11, new Label("Valjund"), connectionOutletComboBox);
        form.addRow(10, new Label("Märkmed"), notesArea);

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
        VBox outletPanel = new VBox(
                8,
                new Label("Kapi väljundid"),
                outletList,
                outletNameField,
                outletTypeComboBox,
                outletCapacityWattsField,
                addOutletButton,
                updateOutletButton,
                removeOutletButton
        );
        VBox detailPanel = new VBox(10, planForm, form, applyButton, choosePowerSourceButton, deleteObjectButton, equipmentPanel, outletPanel, summaryTitle);
        detailPanel.setPadding(new Insets(0, 0, 12, 0));
        return detailPanel;
    }

    private void addTent() {
        pendingTentPlacement = !pendingTentPlacement;
        pendingPowerSourcePlacement = false;
        pendingPowerSourceTent = null;
        refreshPlacementButtons();
    }

    private void placeTent(Position position) {
        Tent tent = new Tent(planFactory.newId(), "Uus telk", position);
        tent.setGroupName("Määramata");
        plan.addObject(tent);
        pendingTentPlacement = false;
        refreshPlacementButtons();
        refreshGroupFilters();
        selectObject(tent);
        refreshSummary();
        markDirty();
    }

    private void addPowerSource() {
        pendingPowerSourcePlacement = !pendingPowerSourcePlacement;
        pendingTentPlacement = false;
        pendingPowerSourceTent = null;
        refreshPlacementButtons();
    }

    private void placePowerSource(Position position) {
        PowerSource source = new PowerSource(planFactory.newId(), "Uus kapp", position);
        source.addOutlet(new PowerOutlet(
                planFactory.newId(),
                ConnectorType.SCHUKO_230V,
                ConnectorType.SCHUKO_230V.defaultCapacityWatts()
        ));
        plan.addObject(source);
        pendingPowerSourcePlacement = false;
        refreshPlacementButtons();
        refreshGroupFilters();
        selectObject(source);
        refreshSummary();
        markDirty();
    }

    private void applyPlanName() {
        String planName = planNameField.getText().trim();
        if (planName.isBlank()) {
            showError("Plaani nime ei muudetud", "Sisesta plaani nimi.");
            planNameField.setText(plan.name());
            return;
        }

        plan.rename(planName);
        refreshSummary();
        markDirty();
    }

    private void newPlan() {
        if (!confirmDiscardUnsavedChanges()) {
            return;
        }

        plan = planFactory.createEmptyPlan();
        resetPlanViewState();
    }

    private void resetPlanViewState() {
        selectedObject = null;
        pendingPowerSourceTent = null;
        pendingTentPlacement = false;
        pendingPowerSourcePlacement = false;
        measuringActive = false;
        if (measureButton != null) {
            measureButton.setSelected(false);
        }
        measurementStart = null;
        measurementNodes.clear();
        visibleGroups.clear();
        knownGroups.clear();
        planNameField.setText(plan.name());
        refreshPlacementButtons();
        refreshGroupFilters();
        redrawMap();
        refreshSummary();
        refreshDetails();
        markClean();
    }

    private void markDirty() {
        unsavedChanges = true;
        updateWindowTitle();
    }

    private void markClean() {
        unsavedChanges = false;
        updateWindowTitle();
    }

    private void updateWindowTitle() {
        if (stage == null) {
            return;
        }
        stage.setTitle("%sPannkoogihommiku planeerija".formatted(unsavedChanges ? "* " : ""));
    }

    private boolean confirmDiscardUnsavedChanges() {
        if (!unsavedChanges) {
            return true;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Salvestamata muudatused");
        alert.setHeaderText("Plaanis on salvestamata muudatusi");
        alert.setContentText("Kui jatkad, lahevad viimased salvestamata muudatused kaotsi.");
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    private void redrawMap() {
        mapPane.getChildren().clear();
        addMapImage();
        drawPowerConnections();
        for (PlannerObject object : plan.objects()) {
            if (!isGroupVisible(object)) {
                continue;
            }
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
            if (!isGroupVisible(tent)) {
                continue;
            }
            plan.findPowerConnectionForConsumer(tent.id())
                    .flatMap(connection -> plan.findObject(connection.sourceId()))
                    .filter(PowerSource.class::isInstance)
                    .map(PowerSource.class::cast)
                    .filter(this::isGroupVisible)
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
        applyLockedStroke(rectangle, tent);
        makeSelectable(rectangle, tent);
        makeDraggable(rectangle, tent);

        Label label = new Label(mapLabel(tent));
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
        applyLockedStroke(circle, source);
        makeSelectable(circle, source);
        makeDraggable(circle, source);

        Label label = new Label(mapLabel(source));
        label.setLayoutX(source.position().x() + 16);
        label.setLayoutY(source.position().y() - 12);
        makeSelectable(label, source);

        mapPane.getChildren().addAll(circle, label);
    }

    private void makeSelectable(Node node, PlannerObject object) {
        node.setOnMouseClicked(event -> {
            if (pendingTentPlacement) {
                Point2D mapPoint = mapPane.sceneToLocal(event.getSceneX(), event.getSceneY());
                placeTent(new Position(mapPoint.getX(), mapPoint.getY()));
                event.consume();
                return;
            }
            if (pendingPowerSourcePlacement) {
                Point2D mapPoint = mapPane.sceneToLocal(event.getSceneX(), event.getSceneY());
                placePowerSource(new Position(mapPoint.getX(), mapPoint.getY()));
                event.consume();
                return;
            }
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
            markDirty();
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

    private String mapLabel(PlannerObject object) {
        if (object.groupName().isBlank()) {
            return object.name();
        }
        return "%s [%s]".formatted(object.name(), object.groupName());
    }

    private boolean isGroupVisible(PlannerObject object) {
        return knownGroups.isEmpty() || visibleGroups.contains(groupNameForFilter(object));
    }

    private String groupNameForFilter(PlannerObject object) {
        return object.groupName().isBlank() ? "Määramata" : object.groupName();
    }

    private void refreshGroupFilters() {
        if (groupFilterPanel == null || plan == null) {
            return;
        }

        Set<String> currentGroups = new HashSet<>();
        for (PlannerObject object : plan.objects()) {
            currentGroups.add(groupNameForFilter(object));
        }

        Set<String> hiddenGroups = new HashSet<>(plan.hiddenGroups());
        hiddenGroups.retainAll(currentGroups);
        plan.clearHiddenGroups();
        for (String hiddenGroup : hiddenGroups) {
            plan.setGroupHidden(hiddenGroup, true);
        }

        visibleGroups.retainAll(currentGroups);
        for (String groupName : currentGroups) {
            if (hiddenGroups.contains(groupName)) {
                visibleGroups.remove(groupName);
            } else if (!knownGroups.contains(groupName)) {
                visibleGroups.add(groupName);
            }
        }
        knownGroups = currentGroups;

        groupFilterPanel.getChildren().clear();
        groupFilterPanel.getChildren().add(new Label("Grupi filtrid"));
        Map<String, String> sortedGroups = new TreeMap<>();
        for (String currentGroup : currentGroups) {
            sortedGroups.put(currentGroup, currentGroup);
        }
        for (String groupName : sortedGroups.values()) {
            CheckBox groupCheckBox = new CheckBox(groupName);
            groupCheckBox.setSelected(visibleGroups.contains(groupName));
            groupCheckBox.setOnAction(event -> {
                if (groupCheckBox.isSelected()) {
                    visibleGroups.add(groupName);
                    plan.setGroupHidden(groupName, false);
                } else {
                    visibleGroups.remove(groupName);
                    plan.setGroupHidden(groupName, true);
                }
                redrawMap();
                markDirty();
            });
            groupFilterPanel.getChildren().add(groupCheckBox);
        }
    }

    private void applyLockedStroke(javafx.scene.shape.Shape shape, PlannerObject object) {
        shape.getStrokeDashArray().clear();
        if (object.locked()) {
            shape.getStrokeDashArray().addAll(8.0, 5.0);
        }
    }

    private void refreshDetails() {
        boolean hasSelection = selectedObject != null;
        boolean tentSelected = selectedObject instanceof Tent;
        boolean powerSourceSelected = selectedObject instanceof PowerSource;
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
        connectionTypeComboBox.setDisable(!tentSelected);
        connectionOutletComboBox.setDisable(!tentSelected);
        equipmentList.setDisable(!tentSelected);
        equipmentNameField.setDisable(!tentSelected);
        equipmentWattsField.setDisable(!tentSelected);
        addEquipmentButton.setDisable(!tentSelected);
        removeEquipmentButton.setDisable(!tentSelected);
        outletList.setDisable(!powerSourceSelected);
        outletNameField.setDisable(!powerSourceSelected);
        outletTypeComboBox.setDisable(!powerSourceSelected);
        outletCapacityWattsField.setDisable(!powerSourceSelected);
        addOutletButton.setDisable(!powerSourceSelected);
        boolean outletSelected = powerSourceSelected && outletList.getSelectionModel().getSelectedIndex() >= 0;
        updateOutletButton.setDisable(!outletSelected);
        removeOutletButton.setDisable(!outletSelected);
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
            refreshOutletList();
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
        refreshOutletList();
    }

    private void startPowerSourceSelectionFromMap() {
        if (!(selectedObject instanceof Tent tent)) {
            return;
        }
        pendingTentPlacement = false;
        pendingPowerSourcePlacement = false;
        refreshPlacementButtons();
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
        if (plan.connectToPower(source.id(), tent.id(), selectedConnectionType()).isEmpty()) {
            showError("Vooluallikat ei valitud", "Valitud kapis ei ole selle ühenduse jaoks sobivat väljundit.");
            return;
        }
        pendingPowerSourceTent = null;
        selectedObject = tent;
        refreshDetails();
        redrawMap();
        refreshSummary();
        markDirty();
    }

    private void updateSelectedLock() {
        if (selectedObject == null) {
            return;
        }
        selectedObject.setLocked(lockedCheckBox.isSelected());
        redrawMap();
        markDirty();
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
            if (!applySelectedPowerSource(tent)) {
                return;
            }
        }
        refreshGroupFilters();
        redrawMap();
        refreshSummary();
        markDirty();
    }

    private void deleteSelectedObject() {
        if (selectedObject == null) {
            return;
        }

        plan.removeObject(selectedObject.id());
        selectedObject = null;
        pendingPowerSourceTent = null;
        refreshGroupFilters();
        redrawMap();
        refreshSummary();
        refreshDetails();
        markDirty();
    }

    private void setMeasuringActive(boolean measuringActive) {
        this.measuringActive = measuringActive;
        if (measuringActive) {
            pendingTentPlacement = false;
            pendingPowerSourcePlacement = false;
            refreshPlacementButtons();
        }
        measurementStart = null;
    }

    private void refreshPlacementButtons() {
        if (addTentButton != null) {
            addTentButton.setText(pendingTentPlacement ? "Tühista telk" : "Lisa telk");
        }
        if (addPowerSourceButton != null) {
            addPowerSourceButton.setText(pendingPowerSourcePlacement ? "Tühista kapp" : "Lisa kapp");
        }
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
        refreshConnectionTypeChoices(ConnectorType.SCHUKO_230V);
        if (!(selectedObject instanceof Tent tent)) {
            return;
        }

        plan.findPowerConnectionForConsumer(tent.id()).ifPresent(connection -> {
            powerSourceComboBox.getItems().stream()
                    .filter(choice -> choice.sourceId().equals(connection.sourceId()))
                    .findFirst()
                    .ifPresent(choice -> powerSourceComboBox.getSelectionModel().select(choice));
            refreshConnectionTypeChoices(connection.connectorType());
            refreshConnectionOutletChoices(connection.outletId());
        });
    }

    private void refreshConnectionTypeChoices(ConnectorType preferredType) {
        ConnectorType currentType = preferredType != null
                ? preferredType
                : connectionTypeComboBox.getSelectionModel().getSelectedItem();
        connectionTypeComboBox.getItems().clear();

        PowerSource selectedSource = selectedPowerSource();
        if (selectedSource == null) {
            connectionTypeComboBox.getItems().addAll(ConnectorType.values());
        } else {
            for (PowerOutlet outlet : selectedSource.outlets()) {
                if (!connectionTypeComboBox.getItems().contains(outlet.type())) {
                    connectionTypeComboBox.getItems().add(outlet.type());
                }
            }
        }

        if (connectionTypeComboBox.getItems().isEmpty()) {
            return;
        }
        if (currentType != null && connectionTypeComboBox.getItems().contains(currentType)) {
            connectionTypeComboBox.getSelectionModel().select(currentType);
        } else {
            connectionTypeComboBox.getSelectionModel().selectFirst();
        }
        refreshConnectionOutletChoices(null);
    }

    private void refreshConnectionOutletChoices(String preferredOutletId) {
        String currentOutletId = preferredOutletId != null
                ? preferredOutletId
                : selectedConnectionOutletId();
        connectionOutletComboBox.getItems().clear();

        PowerSource selectedSource = selectedPowerSource();
        ConnectorType selectedType = selectedConnectionType();
        if (selectedSource == null || selectedType == null) {
            return;
        }

        int matchingIndex = 1;
        for (PowerOutlet outlet : selectedSource.outlets()) {
            if (outlet.type() != selectedType) {
                continue;
            }
            connectionOutletComboBox.getItems().add(new OutletChoice(
                    outlet.id(),
                    outletLabel(outlet, matchingIndex)
            ));
            matchingIndex++;
        }

        if (connectionOutletComboBox.getItems().isEmpty()) {
            return;
        }
        connectionOutletComboBox.getItems().stream()
                .filter(choice -> choice.outletId().equals(currentOutletId))
                .findFirst()
                .ifPresentOrElse(
                        choice -> connectionOutletComboBox.getSelectionModel().select(choice),
                        () -> connectionOutletComboBox.getSelectionModel().selectFirst()
                );
    }

    private String outletLabel(PowerOutlet outlet, int matchingIndex) {
        int usedWatts = usedWatts(outlet.id());
        return "%s - %d W kasutusel, %s".formatted(
                outletDisplayName(outlet, matchingIndex),
                usedWatts,
                remainingWattsText(outlet.capacityWatts() - usedWatts)
        );
    }

    private String outletDisplayName(PowerOutlet outlet, int matchingIndex) {
        String automaticName = "%s %d".formatted(outlet.type().displayName(), matchingIndex);
        return outlet.name().isBlank()
                ? automaticName
                : "%s (%s)".formatted(outlet.name(), automaticName);
    }

    private String remainingWattsText(int remainingWatts) {
        if (remainingWatts < 0) {
            return "ULEKOORMUS %d W".formatted(Math.abs(remainingWatts));
        }
        return "%d W alles".formatted(remainingWatts);
    }

    private int outletTypeIndex(PowerSource source, PowerOutlet targetOutlet, int targetIndex) {
        int matchingIndex = 0;
        for (int index = 0; index <= targetIndex; index++) {
            PowerOutlet outlet = source.outlets().get(index);
            if (outlet.type() == targetOutlet.type()) {
                matchingIndex++;
            }
        }
        return matchingIndex;
    }

    private PowerSource selectedPowerSource() {
        PowerSourceChoice selectedSource = powerSourceComboBox.getSelectionModel().getSelectedItem();
        if (selectedSource == null || selectedSource.isNone()) {
            return null;
        }
        return plan.findObject(selectedSource.sourceId())
                .filter(PowerSource.class::isInstance)
                .map(PowerSource.class::cast)
                .orElse(null);
    }

    private boolean applySelectedPowerSource(Tent tent) {
        PowerSourceChoice selectedSource = powerSourceComboBox.getSelectionModel().getSelectedItem();
        if (selectedSource == null || selectedSource.isNone()) {
            plan.disconnectPower(tent.id());
            return true;
        }

        if (plan.connectToPower(
                selectedSource.sourceId(),
                tent.id(),
                selectedConnectionType(),
                selectedConnectionOutletId()
        ).isEmpty()) {
            showError("Vooluallikat ei rakendatud", "Valitud kapis ei ole selle ühenduse jaoks sobivat väljundit.");
            return false;
        }
        return true;
    }

    private ConnectorType selectedConnectionType() {
        ConnectorType selectedType = connectionTypeComboBox.getSelectionModel().getSelectedItem();
        return selectedType == null ? ConnectorType.SCHUKO_230V : selectedType;
    }

    private String selectedConnectionOutletId() {
        OutletChoice selectedOutlet = connectionOutletComboBox.getSelectionModel().getSelectedItem();
        return selectedOutlet == null ? "" : selectedOutlet.outletId();
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
        markDirty();
    }

    private void removeSelectedEquipment() {
        if (!(selectedObject instanceof Tent tent)) {
            return;
        }

        int selectedIndex = equipmentList.getSelectionModel().getSelectedIndex();
        if (selectedIndex < 0 || selectedIndex >= tent.equipment().size()) {
            return;
        }

        tent.removeEquipment(selectedIndex);
        refreshEquipmentList();
        refreshSummary();
        markDirty();
    }

    private void addOutletToSelectedPowerSource() {
        if (!(selectedObject instanceof PowerSource source)) {
            return;
        }

        ConnectorType selectedType = outletTypeComboBox.getSelectionModel().getSelectedItem();
        if (selectedType == null) {
            showError("Väljundit ei lisatud", "Vali väljundi tüüp.");
            return;
        }

        int capacityWatts;
        try {
            capacityWatts = Integer.parseInt(outletCapacityWattsField.getText().trim());
        } catch (NumberFormatException exception) {
            showError("Väljundit ei lisatud", "Sisesta võimsus täisarvuna vattides.");
            return;
        }

        if (capacityWatts <= 0) {
            showError("Väljundit ei lisatud", "Võimsus peab olema positiivne.");
            return;
        }

        source.addOutlet(new PowerOutlet(planFactory.newId(), outletNameField.getText(), selectedType, capacityWatts));
        outletNameField.clear();
        updateDefaultOutletCapacity();
        refreshOutletList();
        refreshSummary();
        markDirty();
    }

    private void updateSelectedOutlet() {
        PowerOutlet outlet = selectedOutlet();
        if (outlet == null) {
            return;
        }

        ConnectorType selectedType = outletTypeComboBox.getSelectionModel().getSelectedItem();
        if (selectedType == null) {
            showError("Väljundit ei muudetud", "Vali väljundi tüüp.");
            return;
        }

        int capacityWatts;
        try {
            capacityWatts = Integer.parseInt(outletCapacityWattsField.getText().trim());
        } catch (NumberFormatException exception) {
            showError("Väljundit ei muudetud", "Sisesta võimsus täisarvuna vattides.");
            return;
        }

        if (capacityWatts <= 0) {
            showError("Väljundit ei muudetud", "Võimsus peab olema positiivne.");
            return;
        }

        List<Tent> connectedTents = connectedTents(outlet.id());
        if (outlet.type() != selectedType
                && !connectedTents.isEmpty()
                && !confirmOutletTypeChange(outlet, selectedType, connectedTents)) {
            return;
        }

        outlet.rename(outletNameField.getText());
        outlet.setType(selectedType);
        outlet.setCapacityWatts(capacityWatts);
        plan.updateConnectorTypeForOutlet(outlet.id(), selectedType);
        refreshAfterOutletChange(outlet.id());
        markDirty();
    }

    private void removeSelectedOutlet() {
        if (!(selectedObject instanceof PowerSource source)) {
            return;
        }

        int selectedIndex = outletList.getSelectionModel().getSelectedIndex();
        if (selectedIndex < 0 || selectedIndex >= source.outlets().size()) {
            return;
        }

        PowerOutlet outlet = source.outlets().get(selectedIndex);
        List<Tent> connectedTents = connectedTents(outlet.id());
        if (!connectedTents.isEmpty() && !confirmRemoveConnectedOutlet(outlet, connectedTents)) {
            return;
        }

        plan.disconnectPowerFromOutlet(outlet.id());
        source.removeOutlet(selectedIndex);
        outletNameField.clear();
        refreshAfterOutletChange("");
        markDirty();
    }

    private void refreshAfterOutletChange(String preferredOutletId) {
        refreshOutletList();
        selectOutletInList(preferredOutletId);
        refreshPowerSourceChoices();
        refreshConnectionOutletChoices(preferredOutletId);
        redrawMap();
        refreshSummary();
        refreshOutletActionButtons();
    }

    private void selectOutletInList(String outletId) {
        if (outletId == null || outletId.isBlank() || !(selectedObject instanceof PowerSource source)) {
            return;
        }
        for (int index = 0; index < source.outlets().size(); index++) {
            if (source.outlets().get(index).id().equals(outletId)) {
                outletList.getSelectionModel().select(index);
                return;
            }
        }
    }

    private List<Tent> connectedTents(String outletId) {
        return plan.powerConnections().stream()
                .filter(connection -> connection.outletId().equals(outletId))
                .map(connection -> plan.findObject(connection.consumerId()))
                .flatMap(optional -> optional.stream())
                .filter(Tent.class::isInstance)
                .map(Tent.class::cast)
                .toList();
    }

    private boolean confirmRemoveConnectedOutlet(PowerOutlet outlet, List<Tent> connectedTents) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Eemalda valjund");
        alert.setHeaderText("See valjund on kasutusel");
        String tentRows = connectedTents.stream()
                .map(tent -> "- " + tent.name())
                .reduce("", (rows, row) -> rows + row + System.lineSeparator());
        alert.setContentText("%s kustutamisel eemaldatakse nende telkide vooluyhendused:%n%n%s".formatted(
                outlet.name().isBlank() ? outlet.type().displayName() : outlet.name(),
                tentRows
        ));
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    private boolean confirmOutletTypeChange(PowerOutlet outlet, ConnectorType selectedType, List<Tent> connectedTents) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Muuda valjundi tyypi");
        alert.setHeaderText("See valjund on kasutusel");
        String tentRows = connectedTents.stream()
                .map(tent -> "- " + tent.name())
                .reduce("", (rows, row) -> rows + row + System.lineSeparator());
        alert.setContentText("%s tyyp muutub: %s -> %s.%n%nNende telkide yhenduse tyyp muutub samuti:%n%n%s".formatted(
                outlet.name().isBlank() ? outlet.type().displayName() : outlet.name(),
                outlet.type().displayName(),
                selectedType.displayName(),
                tentRows
        ));
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    private void loadSelectedOutletDetails() {
        PowerOutlet outlet = selectedOutlet();
        if (outlet == null) {
            return;
        }
        outletNameField.setText(outlet.name());
        outletTypeComboBox.getSelectionModel().select(outlet.type());
        outletCapacityWattsField.setText(Integer.toString(outlet.capacityWatts()));
        refreshOutletActionButtons();
    }

    private PowerOutlet selectedOutlet() {
        if (!(selectedObject instanceof PowerSource source)) {
            return null;
        }
        int selectedIndex = outletList.getSelectionModel().getSelectedIndex();
        if (selectedIndex < 0 || selectedIndex >= source.outlets().size()) {
            return null;
        }
        return source.outlets().get(selectedIndex);
    }

    private void refreshOutletActionButtons() {
        boolean outletSelected = selectedOutlet() != null;
        updateOutletButton.setDisable(!outletSelected);
        removeOutletButton.setDisable(!outletSelected);
    }

    private void updateDefaultOutletCapacity() {
        if (outletCapacityWattsField == null || outletTypeComboBox == null) {
            return;
        }
        ConnectorType selectedType = outletTypeComboBox.getSelectionModel().getSelectedItem();
        if (selectedType != null) {
            outletCapacityWattsField.setText(Integer.toString(selectedType.defaultCapacityWatts()));
        }
    }

    private StringConverter<ConnectorType> connectorTypeConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(ConnectorType type) {
                return type == null ? "" : type.displayName();
            }

            @Override
            public ConnectorType fromString(String text) {
                for (ConnectorType type : ConnectorType.values()) {
                    if (type.displayName().equals(text) || type.name().equals(text)) {
                        return type;
                    }
                }
                return ConnectorType.SCHUKO_230V;
            }
        };
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

    private void refreshOutletList() {
        outletList.getItems().clear();
        if (!(selectedObject instanceof PowerSource source)) {
            outletNameField.clear();
            refreshOutletActionButtons();
            return;
        }

        for (int index = 0; index < source.outlets().size(); index++) {
            PowerOutlet outlet = source.outlets().get(index);
            outletList.getItems().add("%s - %d W".formatted(outletLabel(outlet, outletTypeIndex(source, outlet, index)), outlet.capacityWatts()));
        }
        refreshOutletActionButtons();
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
            summaryList.getItems().add("%s: %d W kasutusel, %s".formatted(
                    summary.sourceName(),
                    summary.usedWatts(),
                    remainingWattsText(summary.remainingWatts())
            ));
            addConnectedConsumers(summary.sourceId());
        }
        addCableSummary();
        addGroupSummary();
    }

    private void addConnectedConsumers(String sourceId) {
        PowerSource source = plan.findObject(sourceId)
                .filter(PowerSource.class::isInstance)
                .map(PowerSource.class::cast)
                .orElse(null);
        if (source == null) {
            return;
        }

        for (int index = 0; index < source.outlets().size(); index++) {
            PowerOutlet outlet = source.outlets().get(index);
            int usedWatts = usedWatts(outlet.id());
            summaryList.getItems().add("  %s: %d W kasutusel, %s".formatted(
                    outletDisplayName(outlet, outletTypeIndex(source, outlet, index)),
                    usedWatts,
                    remainingWattsText(outlet.capacityWatts() - usedWatts)
            ));
            addConnectedConsumers(sourceId, outlet.id(), "    ");
        }
        addConnectedConsumers(sourceId, "", "  ");
    }

    private void addConnectedConsumers(String sourceId, String outletId, String rowPrefix) {
        for (PowerConnection connection : plan.powerConnections()) {
            if (!connection.sourceId().equals(sourceId)) {
                continue;
            }
            if (!connection.outletId().equals(outletId)) {
                continue;
            }
            plan.findObject(connection.consumerId())
                    .filter(Tent.class::isInstance)
                    .map(Tent.class::cast)
                    .ifPresent(tent -> summaryList.getItems().add("%s- %s: %d W (%s)".formatted(
                            rowPrefix,
                            tent.name(),
                            tent.requiredWatts(),
                            connection.connectorType().displayName()
                    )));
        }
    }

    private int usedWatts(String outletId) {
        return plan.powerConnections().stream()
                .filter(connection -> connection.outletId().equals(outletId))
                .map(connection -> plan.findObject(connection.consumerId()))
                .flatMap(optional -> optional.stream())
                .filter(PowerConsumer.class::isInstance)
                .map(PowerConsumer.class::cast)
                .mapToInt(PowerConsumer::requiredWatts)
                .sum();
    }

    private void addCableSummary() {
        if (plan.powerConnections().isEmpty()) {
            return;
        }

        List<String> cableRows = new ArrayList<>();
        double totalLengthMeters = 0.0;

        for (Tent tent : plan.tents()) {
            PowerConnection connection = plan.findPowerConnectionForConsumer(tent.id()).orElse(null);
            if (connection == null) {
                continue;
            }

            PowerSource source = plan.findObject(connection.sourceId())
                    .filter(PowerSource.class::isInstance)
                    .map(PowerSource.class::cast)
                    .orElse(null);
            if (source == null) {
                continue;
            }

            double lengthMeters = distanceMeters(objectCenter(tent), objectCenter(source));
            totalLengthMeters += lengthMeters;
            cableRows.add("  - %s -> %s (%s): %.1f m".formatted(
                    tent.name(),
                    source.name(),
                    connection.connectorType().displayName(),
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
            markClean();
        } catch (IOException exception) {
            showError("Salvestamine ebaõnnestus", exception.getMessage());
        }
    }

    private void exportSummary() {
        refreshSummary();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Ekspordi kokkuvõte");
        fileChooser.setInitialFileName("pannkoogihommiku-kokkuvote.txt");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Tekstifail", "*.txt"));
        File file = fileChooser.showSaveDialog(stage);
        if (file == null) {
            return;
        }

        try {
            Files.writeString(file.toPath(), summaryText(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            showError("Eksportimine ebaõnnestus", exception.getMessage());
        }
    }

    private String summaryText() {
        String lineSeparator = System.lineSeparator();
        StringBuilder builder = new StringBuilder();
        builder.append(plan.name()).append(lineSeparator);
        builder.append("Voolu kokkuvote pesade kaupa").append(lineSeparator);
        builder.append(lineSeparator);

        for (PowerSource source : plan.powerSources()) {
            int sourceUsedWatts = powerSummaryService.summaries(plan).stream()
                    .filter(summary -> summary.sourceId().equals(source.id()))
                    .findFirst()
                    .map(PowerSummary::usedWatts)
                    .orElse(0);
            builder.append(source.name())
                    .append(": ")
                    .append(sourceUsedWatts)
                    .append(" W kasutusel, ")
                    .append(remainingWattsText(source.totalCapacityWatts() - sourceUsedWatts))
                    .append(lineSeparator);

            if (source.outlets().isEmpty()) {
                builder.append("  Valjundeid pole").append(lineSeparator);
            }

            for (int index = 0; index < source.outlets().size(); index++) {
                PowerOutlet outlet = source.outlets().get(index);
                appendOutletReport(builder, source, outlet, index, lineSeparator);
            }
            builder.append(lineSeparator);
        }

        appendUnconnectedTentsReport(builder, lineSeparator);
        appendCableReport(builder, lineSeparator);
        return builder.toString();
    }

    private void appendOutletReport(StringBuilder builder, PowerSource source, PowerOutlet outlet, int index, String lineSeparator) {
        int usedWatts = usedWatts(outlet.id());
        builder.append("  ")
                .append(outletDisplayName(outlet, outletTypeIndex(source, outlet, index)))
                .append(": ")
                .append(outlet.capacityWatts())
                .append(" W mahutavus, ")
                .append(usedWatts)
                .append(" W kasutusel, ")
                .append(remainingWattsText(outlet.capacityWatts() - usedWatts))
                .append(lineSeparator);

        List<Tent> tents = connectedTents(source.id(), outlet.id());
        if (tents.isEmpty()) {
            builder.append("    Tarbijaid pole").append(lineSeparator);
            return;
        }

        for (Tent tent : tents) {
            builder.append("    - ")
                    .append(tent.name())
                    .append(": ")
                    .append(tent.requiredWatts())
                    .append(" W");
            if (!tent.groupName().isBlank()) {
                builder.append(" (").append(tent.groupName()).append(")");
            }
            builder.append(lineSeparator);
            for (Equipment equipment : tent.equipment()) {
                builder.append("      * ")
                        .append(equipment.name())
                        .append(": ")
                        .append(equipment.requiredWatts())
                        .append(" W")
                        .append(lineSeparator);
            }
        }
    }

    private List<Tent> connectedTents(String sourceId, String outletId) {
        return plan.powerConnections().stream()
                .filter(connection -> connection.sourceId().equals(sourceId))
                .filter(connection -> connection.outletId().equals(outletId))
                .map(connection -> plan.findObject(connection.consumerId()))
                .flatMap(optional -> optional.stream())
                .filter(Tent.class::isInstance)
                .map(Tent.class::cast)
                .toList();
    }

    private void appendUnconnectedTentsReport(StringBuilder builder, String lineSeparator) {
        List<Tent> unconnectedTents = plan.tents().stream()
                .filter(tent -> plan.findPowerConnectionForConsumer(tent.id()).isEmpty())
                .toList();
        if (unconnectedTents.isEmpty()) {
            return;
        }

        builder.append("Yhendamata telgid").append(lineSeparator);
        for (Tent tent : unconnectedTents) {
            builder.append("  - ")
                    .append(tent.name())
                    .append(": ")
                    .append(tent.requiredWatts())
                    .append(" W")
                    .append(lineSeparator);
        }
        builder.append(lineSeparator);
    }

    private void appendCableReport(StringBuilder builder, String lineSeparator) {
        if (plan.powerConnections().isEmpty()) {
            return;
        }

        double totalLengthMeters = 0.0;
        List<String> cableRows = new ArrayList<>();
        for (Tent tent : plan.tents()) {
            PowerConnection connection = plan.findPowerConnectionForConsumer(tent.id()).orElse(null);
            if (connection == null) {
                continue;
            }
            PowerSource source = plan.findObject(connection.sourceId())
                    .filter(PowerSource.class::isInstance)
                    .map(PowerSource.class::cast)
                    .orElse(null);
            if (source == null) {
                continue;
            }

            double lengthMeters = distanceMeters(objectCenter(tent), objectCenter(source));
            totalLengthMeters += lengthMeters;
            cableRows.add("  - %s -> %s (%s): %.1f m".formatted(
                    tent.name(),
                    source.name(),
                    connection.connectorType().displayName(),
                    lengthMeters
            ));
        }

        if (cableRows.isEmpty()) {
            return;
        }

        builder.append("Kaablid").append(lineSeparator);
        for (String row : cableRows) {
            builder.append(row).append(lineSeparator);
        }
        builder.append("Kokku: %.1f m".formatted(totalLengthMeters)).append(lineSeparator);
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
        markDirty();
    }

    private void openPlan() {
        if (!confirmDiscardUnsavedChanges()) {
            return;
        }

        FileChooser fileChooser = createPlanFileChooser();
        File file = fileChooser.showOpenDialog(stage);
        if (file == null) {
            return;
        }

        try {
            plan = planFileService.load(file.toPath());
            resetPlanViewState();
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

    private record OutletChoice(String outletId, String name) {
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
