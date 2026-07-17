package ee.matteus.pannukas.gui;

import ee.matteus.pannukas.core.model.ConnectorType;
import ee.matteus.pannukas.core.model.CustomObject;
import ee.matteus.pannukas.core.model.CustomObjectShape;
import ee.matteus.pannukas.core.model.Equipment;
import ee.matteus.pannukas.core.model.EventPlan;
import ee.matteus.pannukas.core.model.MarkerObject;
import ee.matteus.pannukas.core.model.MarkerType;
import ee.matteus.pannukas.core.model.PlannerObject;
import ee.matteus.pannukas.core.model.Position;
import ee.matteus.pannukas.core.model.PowerConnection;
import ee.matteus.pannukas.core.model.PowerConsumer;
import ee.matteus.pannukas.core.model.PowerOutlet;
import ee.matteus.pannukas.core.model.PowerSource;
import ee.matteus.pannukas.core.model.TextObject;
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
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Polyline;
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
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PancakePlannerApp extends Application {
    private static final String DEFAULT_MAP_PATH = "classpath:/maps/tavakaart.png";
    private static final String ORTHOPHOTO_MAP_PATH = "classpath:/maps/ortofoto.png";
    private static final String SELECTED_OBJECT_SECTION = "selectedObject";
    private static final String SUMMARY_SECTION = "summary";
    private static final String EQUIPMENT_SECTION = "equipment";
    private static final String OUTLET_SECTION = "outlet";
    private static final String GROUP_FILTER_SECTION = "groupFilter";
    private static final Pattern CABLE_LENGTH_PATTERN = Pattern.compile("\\d+(?:[,.]\\d+)?");
    private static final Comparator<CableSummaryRow> CABLE_SUMMARY_ROW_COMPARATOR = Comparator
            .comparing((CableSummaryRow row) -> row.connection().connectorType())
            .thenComparing(row -> row.tent().name(), String.CASE_INSENSITIVE_ORDER)
            .thenComparing(row -> row.source().name(), String.CASE_INSENSITIVE_ORDER);
    private static final double MIN_MAP_WIDTH = 760.0;
    private static final double MIN_MAP_HEIGHT = 560.0;
    private static final double MAP_CLICK_DRAG_TOLERANCE_PX = 6.0;

    private final PlanFactory planFactory = new PlanFactory();
    private final PowerSummaryService powerSummaryService = new PowerSummaryService();
    private final PlanFileService planFileService = new PlanFileService();

    private EventPlan plan;
    private Pane mapPane;
    private Pane mapContentPane;
    private Scale mapScale;
    private ImageView mapImageView;
    private File currentPlanFile;
    private File lastUsedDirectory;
    private double zoomLevel = 1.0;
    private double mapWidth = MIN_MAP_WIDTH;
    private double mapHeight = MIN_MAP_HEIGHT;
    private boolean measuringActive;
    private boolean addingCablePoint;
    private boolean mapDraggedSincePress;
    private double mapPressSceneX;
    private double mapPressSceneY;
    private Position measurementStart;
    private final List<Node> measurementNodes = new ArrayList<>();
    private final List<MeasurementView> measurements = new ArrayList<>();
    private final Set<String> visibleGroups = new HashSet<>();
    private final Map<String, Boolean> sidebarSectionStates = new HashMap<>();
    private Set<String> knownGroups = new HashSet<>();
    private ListView<String> summaryList;
    private CheckBox showPowerSummaryCheckBox;
    private CheckBox showCableSummaryCheckBox;
    private CheckBox showGroupSummaryCheckBox;
    private Label mapToolStatusLabel;
    private Label planTitleLabel;
    private Label saveStatusLabel;
    private VBox groupFilterPanel;
    private TitledPane equipmentSection;
    private TitledPane outletSection;
    private TitledPane groupFilterSection;
    private TextField planNameField;
    private TextField pixelsPerMeterField;
    private Label selectedTypeLabel;
    private TextField nameField;
    private TextField groupField;
    private CheckBox lockedCheckBox;
    private TextField tentWidthField;
    private TextField tentHeightField;
    private TextField tentRotationField;
    private ColorPicker tentColorPicker;
    private ComboBox<CustomObjectShape> customObjectShapeComboBox;
    private ColorPicker customObjectColorPicker;
    private ColorPicker textObjectColorPicker;
    private ComboBox<MarkerType> markerTypeComboBox;
    private ColorPicker markerColorPicker;
    private Label customObjectWidthLabel;
    private Label customObjectHeightLabel;
    private TextField customObjectWidthField;
    private TextField customObjectHeightField;
    private Label customObjectRotationLabel;
    private TextField customObjectRotationField;
    private ComboBox<PowerSourceChoice> powerSourceComboBox;
    private ComboBox<ConnectorType> connectionTypeComboBox;
    private ComboBox<OutletChoice> connectionOutletComboBox;
    private TextField cableLengthNotesField;
    private TextField cableNotesField;
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
    private VBox customObjectPanel;
    private VBox textObjectPanel;
    private VBox markerPanel;
    private VBox tentPanel;
    private VBox powerConnectionPanel;
    private VBox equipmentPanel;
    private VBox outletPanel;
    private Button deleteObjectButton;
    private Button choosePowerSourceButton;
    private ToggleButton measureButton;
    private ToggleButton addCablePointButton;
    private Button clearCableRouteButton;
    private ToggleButton showCablesButton;
    private ToggleButton showCableLabelsButton;
    private ToggleButton show230VCablesButton;
    private ToggleButton show16ACablesButton;
    private ToggleButton show32ACablesButton;
    private ToggleButton show63ACablesButton;
    private ComboBox<PlacementType> placementTypeComboBox;
    private Button addPlacementButton;
    private PlannerObject selectedObject;
    private Tent pendingPowerSourceTent;
    private String pendingPlacementName;
    private String pendingPlacementGroupName;
    private String pendingPlacementColorHex;
    private Double pendingPlacementWidthMeters;
    private Double pendingPlacementHeightMeters;
    private CustomObjectShape pendingPlacementShape;
    private boolean pendingTentPlacement;
    private boolean pendingPowerSourcePlacement;
    private boolean pendingCustomObjectPlacement;
    private boolean pendingTextObjectPlacement;
    private boolean pendingMarkerPlacement;
    private MarkerType pendingPlacementMarkerType;
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

        placementTypeComboBox = new ComboBox<>();
        placementTypeComboBox.getItems().addAll(PlacementType.values());
        placementTypeComboBox.getSelectionModel().select(PlacementType.TENT);
        placementTypeComboBox.setPrefWidth(120);

        addPlacementButton = new Button("Lisa");
        addPlacementButton.setTooltip(new Tooltip("Vali tüüp ja vajuta kaardile, kuhu objekt lisada"));
        addPlacementButton.setOnAction(event -> toggleSelectedPlacement());

        Button saveButton = new Button("Salvesta");
        saveButton.setOnAction(event -> savePlan());

        Button saveAsButton = new Button("Salvesta kui");
        saveAsButton.setOnAction(event -> savePlanAs());

        Button exportSummaryButton = new Button("Ekspordi");
        exportSummaryButton.setOnAction(event -> exportSummary());

        Button openButton = new Button("Ava");
        openButton.setOnAction(event -> openPlan());

        Button planSettingsButton = new Button("Plaani andmed");
        planSettingsButton.setOnAction(event -> showPlanSettingsDialog());

        Button zoomInButton = new Button("+");
        zoomInButton.setOnAction(event -> changeZoom(1.2));

        Button zoomOutButton = new Button("-");
        zoomOutButton.setOnAction(event -> changeZoom(1 / 1.2));

        Button resetZoomButton = new Button("100%");
        resetZoomButton.setOnAction(event -> setZoom(1.0));

        showCablesButton = new ToggleButton("Kaablid");
        showCablesButton.setSelected(true);
        showCablesButton.setTooltip(new Tooltip("Näitab või peidab kaardil voolukaablid"));
        showCablesButton.setOnAction(event -> redrawMap());

        showCableLabelsButton = new ToggleButton("Sildid");
        showCableLabelsButton.setSelected(true);
        showCableLabelsButton.setTooltip(new Tooltip("Näitab või peidab kaablite tekstisildid"));
        showCableLabelsButton.setOnAction(event -> redrawMap());

        show230VCablesButton = cableTypeToggle("230V", ConnectorType.SCHUKO_230V);
        show16ACablesButton = cableTypeToggle("16A", ConnectorType.INDUSTRIAL_16A);
        show32ACablesButton = cableTypeToggle("32A", ConnectorType.INDUSTRIAL_32A);
        show63ACablesButton = cableTypeToggle("63A", ConnectorType.INDUSTRIAL_63A);

        measureButton = new ToggleButton("Mõõdulint");
        measureButton.setTooltip(new Tooltip("Mõõda kaardil vahemaid"));
        measureButton.setOnAction(event -> setMeasuringActive(measureButton.isSelected()));

        Button clearMeasurementsButton = new Button("Puhasta mõõdulint");
        clearMeasurementsButton.setTooltip(new Tooltip("Eemaldab mõõdulindi jooned kaardilt"));
        clearMeasurementsButton.setOnAction(event -> clearMeasurements());

        addCablePointButton = new ToggleButton("Kaabli punkt");
        addCablePointButton.setTooltip(new Tooltip("Lisa valitud telgi voolukaablile vahepunkt"));
        addCablePointButton.setOnAction(event -> setAddingCablePoint(addCablePointButton.isSelected()));

        clearCableRouteButton = new Button("Puhasta trajektoor");
        clearCableRouteButton.setTooltip(new Tooltip("Eemaldab valitud telgi voolukaabli vahepunktid"));
        clearCableRouteButton.setOnAction(event -> clearSelectedCableRoute());

        mapToolStatusLabel = new Label();
        mapToolStatusLabel.setStyle("-fx-text-fill: #374151;");
        updateMapToolStatus();

        saveStatusLabel = new Label("Salvestatud");
        saveStatusLabel.setStyle("-fx-text-fill: #166534; -fx-font-weight: bold;");
        planTitleLabel = new Label();
        planTitleLabel.setStyle("-fx-font-weight: bold;");
        updatePlanTitleLabel();

        return new ToolBar(
                newPlanButton,
                saveButton,
                saveAsButton,
                openButton,
                exportSummaryButton,
                planSettingsButton,
                new Separator(),
                new Label("Lisa"),
                placementTypeComboBox,
                addPlacementButton,
                new Separator(),
                zoomInButton,
                zoomOutButton,
                resetZoomButton,
                showCablesButton,
                showCableLabelsButton,
                show230VCablesButton,
                show16ACablesButton,
                show32ACablesButton,
                show63ACablesButton,
                measureButton,
                clearMeasurementsButton,
                addCablePointButton,
                clearCableRouteButton,
                new Separator(),
                mapToolStatusLabel,
                new Separator(),
                planTitleLabel,
                saveStatusLabel
        );
    }

    private ToggleButton cableTypeToggle(String text, ConnectorType connectorType) {
        ToggleButton button = new ToggleButton(text);
        button.setSelected(true);
        button.setTooltip(new Tooltip("Näitab või peidab kaardil %s kaablid".formatted(shortCableTypeName(connectorType))));
        button.setOnAction(event -> redrawMap());
        return button;
    }

    private void updateMapDragState(double sceneX, double sceneY) {
        if (mapDraggedSincePress) {
            return;
        }
        double deltaX = sceneX - mapPressSceneX;
        double deltaY = sceneY - mapPressSceneY;
        mapDraggedSincePress = deltaX * deltaX + deltaY * deltaY
                > MAP_CLICK_DRAG_TOLERANCE_PX * MAP_CLICK_DRAG_TOLERANCE_PX;
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
        mapPane.setOnMousePressed(event -> {
            mapDraggedSincePress = false;
            mapPressSceneX = event.getSceneX();
            mapPressSceneY = event.getSceneY();
        });
        mapPane.setOnMouseDragged(event -> updateMapDragState(event.getSceneX(), event.getSceneY()));
        mapPane.setOnMouseClicked(event -> {
            if (pendingTentPlacement && !mapDraggedSincePress) {
                placeTent(new Position(event.getX(), event.getY()));
                return;
            }
            if (pendingPowerSourcePlacement && !mapDraggedSincePress) {
                placePowerSource(new Position(event.getX(), event.getY()));
                return;
            }
            if (pendingCustomObjectPlacement && !mapDraggedSincePress) {
                placeCustomObject(new Position(event.getX(), event.getY()));
                return;
            }
            if (pendingTextObjectPlacement && !mapDraggedSincePress) {
                placeTextObject(new Position(event.getX(), event.getY()));
                return;
            }
            if (pendingMarkerPlacement && !mapDraggedSincePress) {
                placeMarkerObject(new Position(event.getX(), event.getY()));
                return;
            }
            if (addingCablePoint && !mapDraggedSincePress) {
                addCableRoutePoint(new Position(event.getX(), event.getY()));
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
        sidebar.getChildren().add(collapsibleSection(SELECTED_OBJECT_SECTION, "Valitud objekt", createDetailPanel(), true));

        showPowerSummaryCheckBox = new CheckBox("Vool");
        showPowerSummaryCheckBox.setSelected(true);
        showPowerSummaryCheckBox.setOnAction(event -> refreshSummary());
        showCableSummaryCheckBox = new CheckBox("Kaablid");
        showCableSummaryCheckBox.setSelected(true);
        showCableSummaryCheckBox.setOnAction(event -> refreshSummary());
        showGroupSummaryCheckBox = new CheckBox("Grupid");
        showGroupSummaryCheckBox.setSelected(true);
        showGroupSummaryCheckBox.setOnAction(event -> refreshSummary());
        HBox summaryFilters = new HBox(10, showPowerSummaryCheckBox, showCableSummaryCheckBox, showGroupSummaryCheckBox);
        VBox cableLegend = new VBox(
                4,
                cableLegendRow(ConnectorType.SCHUKO_230V),
                cableLegendRow(ConnectorType.INDUSTRIAL_16A),
                cableLegendRow(ConnectorType.INDUSTRIAL_32A),
                cableLegendRow(ConnectorType.INDUSTRIAL_63A)
        );

        summaryList = new ListView<>();
        summaryList.setMinHeight(180);
        summaryList.setPrefHeight(260);
        summaryList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setStyle(!empty && item != null && item.contains("ÜLEKOORMUS")
                        ? "-fx-text-fill: #b91c1c; -fx-font-weight: bold;"
                        : "");
            }
        });
        sidebar.getChildren().add(collapsibleSection(SUMMARY_SECTION, "Voolu kokkuvõte", new VBox(8, summaryFilters, cableLegend, summaryList), true));
        groupFilterPanel = new VBox(6);
        groupFilterPanel.setPadding(new Insets(12, 0, 0, 0));
        groupFilterSection = collapsibleSection(GROUP_FILTER_SECTION, "Grupi filtrid", groupFilterPanel, false);
        sidebar.getChildren().add(groupFilterSection);

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
        pixelsPerMeterField = new TextField(formatMeters(plan.pixelsPerMeter()));
        pixelsPerMeterField.setPromptText("px/m");
        Button applyScaleButton = new Button("Rakenda mõõtkava");
        applyScaleButton.setOnAction(event -> applyPixelsPerMeter());

        GridPane planForm = new GridPane();
        planForm.setHgap(8);
        planForm.setVgap(8);
        planForm.addRow(0, new Label("Plaani nimi"), planNameField);
        planForm.addRow(1, new Label(""), applyPlanNameButton);
        planForm.addRow(2, new Label("Piksleid meetri kohta"), pixelsPerMeterField);
        planForm.addRow(3, new Label(""), applyScaleButton);

        selectedTypeLabel = new Label("Vali kaardilt objekt");
        nameField = new TextField();
        groupField = new TextField();
        lockedCheckBox = new CheckBox("Lukus");
        lockedCheckBox.setOnAction(event -> updateSelectedLock());
        tentWidthField = new TextField();
        tentHeightField = new TextField();
        tentRotationField = new TextField();
        tentColorPicker = new ColorPicker();
        customObjectShapeComboBox = new ComboBox<>();
        customObjectShapeComboBox.getItems().addAll(CustomObjectShape.values());
        customObjectShapeComboBox.setConverter(customObjectShapeConverter());
        customObjectShapeComboBox.getSelectionModel().select(CustomObjectShape.SQUARE);
        customObjectColorPicker = new ColorPicker();
        textObjectColorPicker = new ColorPicker();
        customObjectWidthLabel = new Label("Objekti laius m");
        customObjectHeightLabel = new Label("Objekti pikkus m");
        customObjectWidthField = new TextField();
        customObjectHeightField = new TextField();
        customObjectRotationLabel = new Label("Objekti pööre °");
        customObjectRotationField = new TextField();
        customObjectShapeComboBox.setOnAction(event -> updateCustomObjectSizeFields());
        powerSourceComboBox = new ComboBox<>();
        powerSourceComboBox.setOnAction(event -> refreshConnectionTypeChoices(null));
        connectionTypeComboBox = new ComboBox<>();
        connectionTypeComboBox.setConverter(connectorTypeConverter());
        connectionTypeComboBox.setOnAction(event -> refreshConnectionOutletChoices(null));
        connectionOutletComboBox = new ComboBox<>();
        cableLengthNotesField = new TextField();
        cableLengthNotesField.setPromptText("nt 20 + 10 + 10");
        cableLengthNotesField.setOnAction(event -> autoApplyCableLengthNotes());
        cableLengthNotesField.focusedProperty().addListener((observable, wasFocused, isFocused) -> {
            if (wasFocused && !isFocused) {
                autoApplyCableLengthNotes();
            }
        });
        cableNotesField = new TextField();
        cableNotesField.setPromptText("Kaabli märkmed");
        cableNotesField.setOnAction(event -> autoApplyCableNotes());
        cableNotesField.focusedProperty().addListener((observable, wasFocused, isFocused) -> {
            if (wasFocused && !isFocused) {
                autoApplyCableNotes();
            }
        });
        notesArea = new TextArea();
        notesArea.setPrefRowCount(3);
        notesArea.focusedProperty().addListener((observable, wasFocused, isFocused) -> {
            if (wasFocused && !isFocused) {
                autoApplyNotes();
            }
        });
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
        outletNameField.setPromptText("Väljundi nimi");
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

        GridPane baseForm = detailGrid();
        baseForm.addRow(0, new Label("Tüüp"), selectedTypeLabel);
        baseForm.addRow(1, new Label("Nimi"), nameField);
        baseForm.addRow(2, new Label("Grupp"), groupField);
        baseForm.addRow(3, new Label("Lukustus"), lockedCheckBox);

        GridPane customObjectForm = detailGrid();
        customObjectForm.addRow(0, new Label("Kuju"), customObjectShapeComboBox);
        customObjectForm.addRow(1, new Label("Värv"), customObjectColorPicker);
        customObjectForm.addRow(2, customObjectWidthLabel, customObjectWidthField);
        customObjectForm.addRow(3, customObjectHeightLabel, customObjectHeightField);
        customObjectForm.addRow(4, customObjectRotationLabel, customObjectRotationField);
        customObjectPanel = new VBox(8, sectionLabel("Objekt"), customObjectForm);

        GridPane textObjectForm = detailGrid();
        textObjectForm.addRow(0, new Label("Värv"), textObjectColorPicker);
        textObjectPanel = new VBox(8, sectionLabel("Tekst"), textObjectForm);

        markerTypeComboBox = new ComboBox<>();
        markerTypeComboBox.getItems().addAll(MarkerType.values());
        markerTypeComboBox.setConverter(markerTypeConverter());
        markerTypeComboBox.getSelectionModel().select(MarkerType.WC);
        markerColorPicker = new ColorPicker();
        GridPane markerForm = detailGrid();
        markerForm.addRow(0, new Label("Tüüp"), markerTypeComboBox);
        markerForm.addRow(1, new Label("Värv"), markerColorPicker);
        markerPanel = new VBox(8, sectionLabel("Marker"), markerForm);

        GridPane tentForm = detailGrid();
        tentForm.addRow(0, new Label("Laius m"), tentWidthField);
        tentForm.addRow(1, new Label("Pikkus m"), tentHeightField);
        tentForm.addRow(2, new Label("Pööre °"), tentRotationField);
        tentForm.addRow(3, new Label("Värv"), tentColorPicker);
        tentPanel = new VBox(8, sectionLabel("Telk"), tentForm);

        GridPane powerConnectionForm = detailGrid();
        powerConnectionForm.addRow(0, new Label("Vooluallikas"), powerSourceComboBox);
        powerConnectionForm.addRow(1, new Label("Ühenduse tüüp"), connectionTypeComboBox);
        powerConnectionForm.addRow(2, new Label("Väljund"), connectionOutletComboBox);
        powerConnectionForm.addRow(3, new Label("Kaabli tükid"), cableLengthNotesField);
        powerConnectionForm.addRow(4, new Label("Kaabli märkmed"), cableNotesField);
        powerConnectionPanel = new VBox(8, sectionLabel("Vool"), powerConnectionForm);

        GridPane notesForm = detailGrid();
        notesForm.addRow(0, new Label("Märkmed"), notesArea);

        Button applyButton = new Button("Rakenda muudatused");
        applyButton.setOnAction(event -> applyDetails());
        choosePowerSourceButton = new Button("Vali kapp kaardilt");
        choosePowerSourceButton.setOnAction(event -> startPowerSourceSelectionFromMap());
        deleteObjectButton = new Button("Kustuta objekt");
        deleteObjectButton.setOnAction(event -> deleteSelectedObject());

        equipmentPanel = new VBox(
                8,
                equipmentList,
                equipmentNameField,
                equipmentWattsField,
                addEquipmentButton,
                removeEquipmentButton
        );
        equipmentSection = collapsibleSection(EQUIPMENT_SECTION, "Telgi seadmed", equipmentPanel, false);
        outletPanel = new VBox(
                8,
                outletList,
                outletNameField,
                outletTypeComboBox,
                outletCapacityWattsField,
                addOutletButton,
                updateOutletButton,
                removeOutletButton
        );
        outletSection = collapsibleSection(OUTLET_SECTION, "Kapi väljundid", outletPanel, false);
        VBox detailPanel = new VBox(
                10,
                baseForm,
                customObjectPanel,
                textObjectPanel,
                markerPanel,
                tentPanel,
                powerConnectionPanel,
                equipmentSection,
                outletSection,
                new VBox(8, sectionLabel("Märkmed"), notesForm),
                applyButton,
                choosePowerSourceButton,
                deleteObjectButton
        );
        detailPanel.setPadding(new Insets(0, 0, 12, 0));
        return detailPanel;
    }

    private Label sectionLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-weight: bold; -fx-padding: 8 0 0 0;");
        return label;
    }

    private GridPane detailGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        return grid;
    }

    private TitledPane collapsibleSection(String stateKey, String title, Node content, boolean expandedByDefault) {
        TitledPane pane = new TitledPane(title, content);
        pane.setExpanded(sidebarSectionStates.getOrDefault(stateKey, expandedByDefault));
        pane.setCollapsible(true);
        pane.expandedProperty().addListener((observable, oldValue, newValue) ->
                sidebarSectionStates.put(stateKey, newValue)
        );
        return pane;
    }

    private void toggleSelectedPlacement() {
        if (isPlacementPending()) {
            cancelPlacement();
            return;
        }

        PlacementType selectedType = placementTypeComboBox.getSelectionModel().getSelectedItem();
        if (selectedType == null) {
            selectedType = PlacementType.TENT;
        }

        PlacementDetails placementDetails = askPlacementDetails(selectedType);
        if (placementDetails == null) {
            return;
        }
        pendingPlacementName = placementDetails.name();
        pendingPlacementGroupName = placementDetails.groupName();
        pendingPlacementColorHex = placementDetails.colorHex();
        pendingPlacementWidthMeters = placementDetails.widthMeters();
        pendingPlacementHeightMeters = placementDetails.heightMeters();
        pendingPlacementShape = placementDetails.shape();
        pendingPlacementMarkerType = placementDetails.markerType();

        switch (selectedType) {
            case TENT -> addTent();
            case POWER_SOURCE -> addPowerSource();
            case CUSTOM_OBJECT -> addCustomObject();
            case TEXT_OBJECT -> addTextObject();
            case MARKER_OBJECT -> addMarkerObject();
        }
    }

    private PlacementDetails askPlacementDetails(PlacementType placementType) {
        TextField nameField = new TextField(placementType.defaultName());
        ComboBox<String> groupComboBox = new ComboBox<>();
        groupComboBox.setEditable(true);
        groupComboBox.getItems().addAll(existingGroupNames());
        groupComboBox.getSelectionModel().select("Määramata");
        ColorPicker colorPicker = new ColorPicker(Color.web(placementType.defaultColorHex()));
        TextField tentWidthField = new TextField("3");
        TextField tentHeightField = new TextField("3");
        ComboBox<CustomObjectShape> shapeComboBox = new ComboBox<>();
        shapeComboBox.getItems().addAll(CustomObjectShape.values());
        shapeComboBox.setConverter(customObjectShapeConverter());
        shapeComboBox.getSelectionModel().select(CustomObjectShape.SQUARE);
        Label objectWidthLabel = new Label("Laius m");
        Label objectHeightLabel = new Label("Pikkus m");
        TextField objectWidthField = new TextField("1");
        TextField objectHeightField = new TextField("1");
        ComboBox<MarkerType> markerTypeComboBox = new ComboBox<>();
        markerTypeComboBox.getItems().addAll(MarkerType.values());
        markerTypeComboBox.setConverter(markerTypeConverter());
        markerTypeComboBox.getSelectionModel().select(MarkerType.WC);
        boolean[] markerNameEdited = {false};
        if (placementType == PlacementType.MARKER_OBJECT) {
            nameField.setText(MarkerType.WC.displayName());
            nameField.textProperty().addListener((observable, oldValue, newValue) -> {
                MarkerType selectedMarkerType = markerTypeComboBox.getSelectionModel().getSelectedItem();
                if (selectedMarkerType != null && !newValue.equals(selectedMarkerType.displayName())) {
                    markerNameEdited[0] = true;
                }
            });
            markerTypeComboBox.setOnAction(event -> {
                if (!markerNameEdited[0]) {
                    MarkerType selectedMarkerType = markerTypeComboBox.getSelectionModel().getSelectedItem();
                    nameField.setText(selectedMarkerType == null ? MarkerType.WC.displayName() : selectedMarkerType.displayName());
                }
            });
        }
        shapeComboBox.setOnAction(event -> updatePlacementObjectSizeFields(
                shapeComboBox,
                objectWidthLabel,
                objectWidthField,
                objectHeightLabel,
                objectHeightField
        ));
        updatePlacementObjectSizeFields(shapeComboBox, objectWidthLabel, objectWidthField, objectHeightLabel, objectHeightField);
        GridPane form = detailGrid();
        form.addRow(0, new Label("Nimi"), nameField);
        form.addRow(1, new Label("Grupp"), groupComboBox);
        if (placementType == PlacementType.TENT) {
            form.addRow(2, new Label("Laius m"), tentWidthField);
            form.addRow(3, new Label("Pikkus m"), tentHeightField);
        } else if (placementType == PlacementType.CUSTOM_OBJECT) {
            form.addRow(2, new Label("Kuju"), shapeComboBox);
            form.addRow(3, objectWidthLabel, objectWidthField);
            form.addRow(4, objectHeightLabel, objectHeightField);
        } else if (placementType == PlacementType.MARKER_OBJECT) {
            form.addRow(2, new Label("Marker"), markerTypeComboBox);
        }
        if (placementType.hasConfigurableColor()) {
            int colorRow = switch (placementType) {
                case TENT -> 4;
                case CUSTOM_OBJECT -> 5;
                case MARKER_OBJECT -> 3;
                case POWER_SOURCE, TEXT_OBJECT -> 2;
            };
            form.addRow(colorRow, new Label("Värv"), colorPicker);
        }

        Alert dialog = new Alert(Alert.AlertType.CONFIRMATION);
        dialog.setTitle("Lisa objekt");
        dialog.setHeaderText("Sisesta lisatava objekti andmed");
        dialog.getDialogPane().setContent(form);

        if (dialog.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return null;
        }

        String groupName = groupComboBox.getEditor().getText().trim();
        if (groupName.isBlank()) {
            groupName = "Määramata";
        }
        double widthMeters = placementType == PlacementType.TENT ? 3.0 : 1.0;
        double heightMeters = placementType == PlacementType.TENT ? 3.0 : 1.0;
        CustomObjectShape shape = CustomObjectShape.SQUARE;
        MarkerType markerType = MarkerType.WC;
        String name = nameField.getText().trim();
        if (placementType == PlacementType.TENT) {
            try {
                widthMeters = Double.parseDouble(tentWidthField.getText().trim().replace(',', '.'));
                heightMeters = Double.parseDouble(tentHeightField.getText().trim().replace(',', '.'));
                if (widthMeters <= 0 || heightMeters <= 0) {
                    throw new IllegalArgumentException("Telgi mõõdud peavad olema positiivsed.");
                }
            } catch (NumberFormatException exception) {
                showError("Objekti ei lisatud", "Sisesta telgi laius ja pikkus arvuna meetrites.");
                return null;
            } catch (IllegalArgumentException exception) {
                showError("Objekti ei lisatud", exception.getMessage());
                return null;
            }
        } else if (placementType == PlacementType.CUSTOM_OBJECT) {
            shape = shapeComboBox.getSelectionModel().getSelectedItem();
            if (shape == null) {
                shape = CustomObjectShape.SQUARE;
            }
            try {
                widthMeters = Double.parseDouble(objectWidthField.getText().trim().replace(',', '.'));
                heightMeters = shape == CustomObjectShape.CIRCLE
                        ? widthMeters
                        : Double.parseDouble(objectHeightField.getText().trim().replace(',', '.'));
                if (widthMeters <= 0 || heightMeters <= 0) {
                    throw new IllegalArgumentException("Objekti mõõdud peavad olema positiivsed.");
                }
            } catch (NumberFormatException exception) {
                if (shape == CustomObjectShape.CIRCLE) {
                    showError("Objekti ei lisatud", "Sisesta objekti läbimõõt arvuna meetrites.");
                } else {
                    showError("Objekti ei lisatud", "Sisesta objekti laius ja pikkus arvuna meetrites.");
                }
                return null;
            } catch (IllegalArgumentException exception) {
                showError("Objekti ei lisatud", exception.getMessage());
                return null;
            }
        }
        if (placementType == PlacementType.MARKER_OBJECT) {
            markerType = markerTypeComboBox.getSelectionModel().getSelectedItem();
            if (markerType == null) {
                markerType = MarkerType.WC;
            }
        }
        if (name.isBlank()) {
            name = placementType == PlacementType.MARKER_OBJECT
                    ? markerType.displayName()
                    : placementType.defaultName();
        }
        return new PlacementDetails(name, groupName, toHex(colorPicker.getValue()), widthMeters, heightMeters, shape, markerType);
    }

    private void updatePlacementObjectSizeFields(
            ComboBox<CustomObjectShape> shapeComboBox,
            Label objectWidthLabel,
            TextField objectWidthField,
            Label objectHeightLabel,
            TextField objectHeightField
    ) {
        CustomObjectShape selectedShape = shapeComboBox.getSelectionModel().getSelectedItem();
        boolean circleSelected = selectedShape == CustomObjectShape.CIRCLE;
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

    private List<String> existingGroupNames() {
        Set<String> groupNames = new HashSet<>(knownGroups);
        if (plan != null) {
            for (PlannerObject object : plan.objects()) {
                groupNames.add(groupNameForFilter(object));
            }
        }
        groupNames.add("Määramata");
        return groupNames.stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private void addTent() {
        pendingTentPlacement = !pendingTentPlacement;
        pendingPowerSourcePlacement = false;
        pendingCustomObjectPlacement = false;
        pendingTextObjectPlacement = false;
        pendingMarkerPlacement = false;
        pendingPowerSourceTent = null;
        refreshPlacementButtons();
        updateMapToolStatus();
    }

    private void placeTent(Position position) {
        Tent tent = new Tent(planFactory.newId(), placementNameOrDefault(PlacementType.TENT), position);
        tent.setGroupName(placementGroupNameOrDefault());
        tent.setColorHex(placementColorHexOrDefault(PlacementType.TENT));
        tent.setSizeMeters(pendingPlacementWidthMetersOrDefault(), pendingPlacementHeightMetersOrDefault());
        plan.addObject(tent);
        clearPendingPlacementDetails();
        pendingTentPlacement = false;
        refreshPlacementButtons();
        updateMapToolStatus();
        refreshGroupFilters();
        selectObject(tent);
        refreshSummary();
        markDirty();
    }

    private void addPowerSource() {
        pendingPowerSourcePlacement = !pendingPowerSourcePlacement;
        pendingTentPlacement = false;
        pendingCustomObjectPlacement = false;
        pendingTextObjectPlacement = false;
        pendingMarkerPlacement = false;
        pendingPowerSourceTent = null;
        refreshPlacementButtons();
        updateMapToolStatus();
    }

    private void placePowerSource(Position position) {
        PowerSource source = new PowerSource(planFactory.newId(), placementNameOrDefault(PlacementType.POWER_SOURCE), position);
        source.addOutlet(new PowerOutlet(
                planFactory.newId(),
                ConnectorType.SCHUKO_230V,
                ConnectorType.SCHUKO_230V.defaultCapacityWatts()
        ));
        source.setGroupName(placementGroupNameOrDefault());
        plan.addObject(source);
        clearPendingPlacementDetails();
        pendingPowerSourcePlacement = false;
        refreshPlacementButtons();
        updateMapToolStatus();
        refreshGroupFilters();
        selectObject(source);
        refreshSummary();
        markDirty();
    }

    private void addCustomObject() {
        pendingCustomObjectPlacement = !pendingCustomObjectPlacement;
        pendingTentPlacement = false;
        pendingPowerSourcePlacement = false;
        pendingTextObjectPlacement = false;
        pendingMarkerPlacement = false;
        pendingPowerSourceTent = null;
        refreshPlacementButtons();
        updateMapToolStatus();
    }

    private void placeCustomObject(Position position) {
        CustomObject object = new CustomObject(planFactory.newId(), placementNameOrDefault(PlacementType.CUSTOM_OBJECT), position);
        object.setGroupName(placementGroupNameOrDefault());
        object.setColorHex(placementColorHexOrDefault(PlacementType.CUSTOM_OBJECT));
        object.setShape(placementShapeOrDefault());
        object.setSizeMeters(pendingCustomObjectWidthMetersOrDefault(), pendingCustomObjectHeightMetersOrDefault());
        plan.addObject(object);
        clearPendingPlacementDetails();
        pendingCustomObjectPlacement = false;
        pendingMarkerPlacement = false;
        refreshPlacementButtons();
        updateMapToolStatus();
        refreshGroupFilters();
        selectObject(object);
        refreshSummary();
        markDirty();
    }

    private void addTextObject() {
        pendingTextObjectPlacement = !pendingTextObjectPlacement;
        pendingTentPlacement = false;
        pendingPowerSourcePlacement = false;
        pendingCustomObjectPlacement = false;
        pendingPowerSourceTent = null;
        refreshPlacementButtons();
        updateMapToolStatus();
    }

    private void placeTextObject(Position position) {
        TextObject object = new TextObject(planFactory.newId(), placementNameOrDefault(PlacementType.TEXT_OBJECT), position);
        object.setGroupName(placementGroupNameOrDefault());
        object.setColorHex(placementColorHexOrDefault(PlacementType.TEXT_OBJECT));
        plan.addObject(object);
        clearPendingPlacementDetails();
        pendingTextObjectPlacement = false;
        refreshPlacementButtons();
        updateMapToolStatus();
        refreshGroupFilters();
        selectObject(object);
        refreshSummary();
        markDirty();
    }

    private void addMarkerObject() {
        pendingMarkerPlacement = !pendingMarkerPlacement;
        pendingTentPlacement = false;
        pendingPowerSourcePlacement = false;
        pendingCustomObjectPlacement = false;
        pendingTextObjectPlacement = false;
        pendingPowerSourceTent = null;
        refreshPlacementButtons();
        updateMapToolStatus();
    }

    private void placeMarkerObject(Position position) {
        MarkerObject object = new MarkerObject(planFactory.newId(), placementNameOrDefault(PlacementType.MARKER_OBJECT), position);
        object.setGroupName(placementGroupNameOrDefault());
        object.setColorHex(placementColorHexOrDefault(PlacementType.MARKER_OBJECT));
        object.setMarkerType(placementMarkerTypeOrDefault());
        plan.addObject(object);
        clearPendingPlacementDetails();
        pendingMarkerPlacement = false;
        refreshPlacementButtons();
        updateMapToolStatus();
        refreshGroupFilters();
        selectObject(object);
        refreshSummary();
        markDirty();
    }

    private String placementNameOrDefault(PlacementType placementType) {
        if (pendingPlacementName == null || pendingPlacementName.isBlank()) {
            return placementType.defaultName();
        }
        return pendingPlacementName;
    }

    private String placementGroupNameOrDefault() {
        if (pendingPlacementGroupName == null || pendingPlacementGroupName.isBlank()) {
            return "Määramata";
        }
        return pendingPlacementGroupName;
    }

    private String placementColorHexOrDefault(PlacementType placementType) {
        if (pendingPlacementColorHex == null || pendingPlacementColorHex.isBlank()) {
            return placementType.defaultColorHex();
        }
        return pendingPlacementColorHex;
    }

    private double pendingPlacementWidthMetersOrDefault() {
        return pendingPlacementWidthMeters == null ? 3.0 : pendingPlacementWidthMeters;
    }

    private double pendingPlacementHeightMetersOrDefault() {
        return pendingPlacementHeightMeters == null ? 3.0 : pendingPlacementHeightMeters;
    }

    private double pendingCustomObjectWidthMetersOrDefault() {
        return pendingPlacementWidthMeters == null ? 1.0 : pendingPlacementWidthMeters;
    }

    private double pendingCustomObjectHeightMetersOrDefault() {
        return pendingPlacementHeightMeters == null ? 1.0 : pendingPlacementHeightMeters;
    }

    private CustomObjectShape placementShapeOrDefault() {
        return pendingPlacementShape == null ? CustomObjectShape.SQUARE : pendingPlacementShape;
    }

    private MarkerType placementMarkerTypeOrDefault() {
        return pendingPlacementMarkerType == null ? MarkerType.WC : pendingPlacementMarkerType;
    }

    private void clearPendingPlacementDetails() {
        pendingPlacementName = null;
        pendingPlacementGroupName = null;
        pendingPlacementColorHex = null;
        pendingPlacementWidthMeters = null;
        pendingPlacementHeightMeters = null;
        pendingPlacementShape = null;
        pendingPlacementMarkerType = null;
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

    private void applyPixelsPerMeter() {
        try {
            double pixelsPerMeter = Double.parseDouble(pixelsPerMeterField.getText().trim().replace(',', '.'));
            plan.setPixelsPerMeter(pixelsPerMeter);
            pixelsPerMeterField.setText(formatMeters(plan.pixelsPerMeter()));
            refreshMeasurementLabels();
            redrawMap();
            refreshSummary();
            markDirty();
        } catch (NumberFormatException exception) {
            showError("Mõõtkava ei muudetud", "Sisesta pikslite arv meetri kohta arvuna.");
            pixelsPerMeterField.setText(formatMeters(plan.pixelsPerMeter()));
        } catch (IllegalArgumentException exception) {
            showError("Mõõtkava ei muudetud", exception.getMessage());
            pixelsPerMeterField.setText(formatMeters(plan.pixelsPerMeter()));
        }
    }

    private void showPlanSettingsDialog() {
        TextField dialogPlanNameField = new TextField(plan.name());
        TextField dialogPixelsPerMeterField = new TextField(formatMeters(plan.pixelsPerMeter()));
        dialogPixelsPerMeterField.setPromptText("px/m");
        Label mapLabel = new Label(plan.mapImagePath().isBlank() ? "Kaarti pole valitud" : plan.mapImagePath());

        final String[] selectedMapPath = {plan.mapImagePath()};
        Button defaultMapButton = new Button("Tavakaart");
        defaultMapButton.setOnAction(event -> {
            selectedMapPath[0] = DEFAULT_MAP_PATH;
            mapLabel.setText(selectedMapPath[0]);
        });
        Button orthophotoButton = new Button("Ortofoto");
        orthophotoButton.setOnAction(event -> {
            selectedMapPath[0] = ORTHOPHOTO_MAP_PATH;
            mapLabel.setText(selectedMapPath[0]);
        });
        Button loadMapButton = new Button("Laadi kaart");
        loadMapButton.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Vali kaart");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Pildifailid", "*.png", "*.jpg", "*.jpeg"));
            applyInitialDirectory(fileChooser);
            File file = fileChooser.showOpenDialog(stage);
            if (file != null) {
                selectedMapPath[0] = file.getAbsolutePath();
                mapLabel.setText(selectedMapPath[0]);
                rememberDirectory(file);
            }
        });
        Button setScaleFromMeasurementButton = new Button("Määra mõõdulindi järgi");
        setScaleFromMeasurementButton.setTooltip(new Tooltip("Arvutab piksleid meetri kohta viimase mõõdulindi joone põhjal"));
        setScaleFromMeasurementButton.setOnAction(event -> {
            if (setScaleFromLastMeasurement()) {
                dialogPixelsPerMeterField.setText(formatMeters(plan.pixelsPerMeter()));
            }
        });

        GridPane form = detailGrid();
        form.addRow(0, new Label("Plaani nimi"), dialogPlanNameField);
        form.addRow(1, new Label("Piksleid meetri kohta"), new HBox(8, dialogPixelsPerMeterField, setScaleFromMeasurementButton));
        form.addRow(2, new Label("Kaart"), new HBox(8, defaultMapButton, orthophotoButton, loadMapButton));
        form.addRow(3, new Label("Valitud kaart"), mapLabel);

        Alert dialog = new Alert(Alert.AlertType.CONFIRMATION);
        dialog.setTitle("Plaani andmed");
        dialog.setHeaderText("Muuda plaani andmeid");
        dialog.getDialogPane().setContent(form);
        dialog.showAndWait().ifPresent(buttonType -> {
            if (buttonType != ButtonType.OK) {
                return;
            }
            applyPlanSettings(dialogPlanNameField.getText(), dialogPixelsPerMeterField.getText(), selectedMapPath[0]);
        });
    }

    private void applyPlanSettings(String planName, String pixelsPerMeterText, String mapImagePath) {
        String trimmedPlanName = planName == null ? "" : planName.trim();
        if (trimmedPlanName.isBlank()) {
            showError("Plaani andmeid ei muudetud", "Sisesta plaani nimi.");
            return;
        }

        try {
            double pixelsPerMeter = Double.parseDouble(pixelsPerMeterText.trim().replace(',', '.'));
            plan.rename(trimmedPlanName);
            plan.setPixelsPerMeter(pixelsPerMeter);
            plan.setMapImagePath(mapImagePath);
            if (planNameField != null) {
                planNameField.setText(plan.name());
            }
            if (pixelsPerMeterField != null) {
                pixelsPerMeterField.setText(formatMeters(plan.pixelsPerMeter()));
            }
            refreshMeasurementLabels();
            redrawMap();
            refreshSummary();
            markDirty();
        } catch (NumberFormatException exception) {
            showError("Plaani andmeid ei muudetud", "Sisesta pikslite arv meetri kohta arvuna.");
        } catch (IllegalArgumentException exception) {
            showError("Plaani andmeid ei muudetud", exception.getMessage());
        }
    }

    private void newPlan() {
        if (!confirmDiscardUnsavedChanges()) {
            return;
        }

        plan = planFactory.createEmptyPlan();
        currentPlanFile = null;
        resetPlanViewState();
    }

    private void resetPlanViewState() {
        selectedObject = null;
        pendingPowerSourceTent = null;
        clearPendingPlacementDetails();
        pendingTentPlacement = false;
        pendingPowerSourcePlacement = false;
        pendingCustomObjectPlacement = false;
        pendingTextObjectPlacement = false;
        measuringActive = false;
        addingCablePoint = false;
        if (measureButton != null) {
            measureButton.setSelected(false);
        }
        if (addCablePointButton != null) {
            addCablePointButton.setSelected(false);
        }
        measurementStart = null;
        measurementNodes.clear();
        measurements.clear();
        visibleGroups.clear();
        knownGroups.clear();
        if (planNameField != null) {
            planNameField.setText(plan.name());
        }
        if (pixelsPerMeterField != null) {
            pixelsPerMeterField.setText(formatMeters(plan.pixelsPerMeter()));
        }
        refreshPlacementButtons();
        updateMapToolStatus();
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
        String fileName = currentPlanFile == null ? "" : " - " + currentPlanFile.getName();
        stage.setTitle("%sPannkoogihommiku planeerija%s".formatted(unsavedChanges ? "* " : "", fileName));
        updatePlanTitleLabel();
        updateSaveStatusLabel();
    }

    private void updatePlanTitleLabel() {
        if (planTitleLabel == null || plan == null) {
            return;
        }
        planTitleLabel.setText(plan.name());
    }

    private void updateSaveStatusLabel() {
        if (saveStatusLabel == null) {
            return;
        }
        saveStatusLabel.setText(unsavedChanges ? "Salvestamata muudatused" : "Salvestatud");
        saveStatusLabel.setStyle(unsavedChanges
                ? "-fx-text-fill: #b45309; -fx-font-weight: bold;"
                : "-fx-text-fill: #166534; -fx-font-weight: bold;");
    }

    private void updateMapToolStatus() {
        if (mapToolStatusLabel == null) {
            return;
        }
        if (pendingPowerSourceTent != null) {
            mapToolStatusLabel.setText("Vali kaardilt elektrikapp");
            return;
        }
        if (pendingTentPlacement) {
            mapToolStatusLabel.setText("Paiguta telk kaardile");
            return;
        }
        if (pendingPowerSourcePlacement) {
            mapToolStatusLabel.setText("Paiguta elektrikapp kaardile");
            return;
        }
        if (pendingCustomObjectPlacement) {
            mapToolStatusLabel.setText("Paiguta objekt kaardile");
            return;
        }
        if (pendingTextObjectPlacement) {
            mapToolStatusLabel.setText("Paiguta tekst kaardile");
            return;
        }
        if (addingCablePoint) {
            mapToolStatusLabel.setText("Lisa kaabli punkt kaardile");
            return;
        }
        if (measuringActive) {
            mapToolStatusLabel.setText("Mõõdulint aktiivne");
            return;
        }
        mapToolStatusLabel.setText("Vali tööriist või objekt");
    }

    private boolean confirmDiscardUnsavedChanges() {
        if (!unsavedChanges) {
            return true;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Salvestamata muudatused");
        alert.setHeaderText("Plaanis on salvestamata muudatusi");
        alert.setContentText("Kas soovid enne jätkamist plaani salvestada?");
        ButtonType saveButton = new ButtonType("Salvesta");
        ButtonType discardButton = new ButtonType("Ära salvesta");
        alert.getButtonTypes().setAll(saveButton, discardButton, ButtonType.CANCEL);

        ButtonType choice = alert.showAndWait().orElse(ButtonType.CANCEL);
        if (choice == saveButton) {
            return savePlan();
        }
        return choice == discardButton;
    }

    private void redrawMap() {
        mapPane.getChildren().clear();
        addMapImage();
        if (showCables()) {
            drawPowerConnections();
        }
        for (PlannerObject object : plan.objects()) {
            if (!isGroupVisible(object)) {
                continue;
            }
            if (object instanceof Tent tent) {
                drawTent(tent);
            } else if (object instanceof PowerSource source) {
                drawPowerSource(source);
            } else if (object instanceof TextObject textObject) {
                drawTextObject(textObject);
            } else if (object instanceof MarkerObject markerObject) {
                drawMarkerObject(markerObject);
            } else if (object instanceof CustomObject customObject) {
                drawCustomObject(customObject);
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

    private boolean showCables() {
        return showCablesButton == null || showCablesButton.isSelected();
    }

    private boolean showCableLabels() {
        return showCableLabelsButton == null || showCableLabelsButton.isSelected();
    }

    private boolean showCableType(ConnectorType connectorType) {
        return switch (connectorType) {
            case SCHUKO_230V -> show230VCablesButton == null || show230VCablesButton.isSelected();
            case INDUSTRIAL_16A -> show16ACablesButton == null || show16ACablesButton.isSelected();
            case INDUSTRIAL_32A -> show32ACablesButton == null || show32ACablesButton.isSelected();
            case INDUSTRIAL_63A -> show63ACablesButton == null || show63ACablesButton.isSelected();
        };
    }

    private HBox cableLegendRow(ConnectorType connectorType) {
        Line sample = new Line(0, 0, 34, 0);
        sample.setStroke(cableColor(connectorType));
        sample.setStrokeWidth(cableWidth(connectorType));
        if (connectorType == ConnectorType.SCHUKO_230V) {
            sample.getStrokeDashArray().addAll(8.0, 6.0);
        }

        Label label = new Label(shortCableTypeName(connectorType));
        HBox row = new HBox(8, sample, label);
        row.setStyle("-fx-alignment: center-left;");
        return row;
    }

    private void drawPowerConnections() {
        for (Tent tent : plan.tents()) {
            if (!isGroupVisible(tent)) {
                continue;
            }
            plan.findPowerConnectionForConsumer(tent.id())
                    .filter(connection -> showCableType(connection.connectorType()))
                    .flatMap(connection -> plan.findObject(connection.sourceId())
                            .filter(PowerSource.class::isInstance)
                            .map(PowerSource.class::cast)
                            .filter(this::isGroupVisible)
                            .map(source -> new PowerCableView(tent, source, connection)))
                    .ifPresent(this::drawPowerConnection);
        }
    }

    private void drawPowerConnection(PowerCableView cable) {
        List<Position> path = cablePath(cable);
        Color cableColor = cableColor(cable.connection().connectorType());
        boolean selectedCable = isSelected(cable.tent());
        double strokeWidth = cableWidth(cable.connection().connectorType()) + (selectedCable ? 2.0 : 0.0);

        Polyline line = createCablePolyline(path);
        line.setStroke(cableColor);
        line.setStrokeWidth(strokeWidth);
        line.setOpacity(selectedCable ? 1.0 : 0.85);
        line.setMouseTransparent(true);
        if (cable.connection().connectorType() == ConnectorType.SCHUKO_230V) {
            line.getStrokeDashArray().addAll(8.0, 6.0);
        }

        Polyline highlightLine = createCablePolyline(path);
        highlightLine.setStroke(Color.web("#111827"));
        highlightLine.setStrokeWidth(strokeWidth + 4.0);
        highlightLine.setOpacity(selectedCable ? 0.28 : 0);
        highlightLine.setMouseTransparent(true);
        if (cable.connection().connectorType() == ConnectorType.SCHUKO_230V) {
            highlightLine.getStrokeDashArray().addAll(8.0, 6.0);
        }

        Polyline hitLine = createCablePolyline(path);
        hitLine.setStroke(Color.TRANSPARENT);
        hitLine.setStrokeWidth(Math.max(12.0, strokeWidth + 8.0));
        makeCableSelectable(hitLine, cable);

        mapPane.getChildren().addAll(highlightLine, line, hitLine);
        Label distanceLabel = null;
        if (showCableLabels()) {
            Position labelPosition = pathMidpoint(path);
            distanceLabel = new Label(cableMapLabel(cable.connection(), cableLengthMeters(path)));
            distanceLabel.setStyle("-fx-background-color: rgba(255,255,255,%s); -fx-padding: 2 5 2 5; -fx-border-color: %s; -fx-font-weight: %s;".formatted(
                    selectedCable ? "0.96" : "0.88",
                    toHex(selectedCable ? Color.web("#111827") : cableColor),
                    selectedCable ? "bold" : "normal"
            ));
            distanceLabel.setLayoutX(labelPosition.x() + 6);
            distanceLabel.setLayoutY(labelPosition.y() + 6);
            makeCableSelectable(distanceLabel, cable.tent());
            mapPane.getChildren().add(distanceLabel);
        }
        if (selectedCable) {
            for (int index = 0; index < cable.connection().routePoints().size(); index++) {
                Position routePoint = cable.connection().routePoints().get(index);
                Circle marker = new Circle(routePoint.x(), routePoint.y(), 4);
                marker.setFill(Color.WHITE);
                marker.setStroke(cableColor);
                marker.setStrokeWidth(2);
                Tooltip.install(marker, new Tooltip("Lohista punkti muutmiseks, paremklõps eemaldab punkti"));
                makeCableSelectable(marker, cable.tent());
                makeCableRoutePointDraggable(marker, cable, index, line, highlightLine, hitLine, distanceLabel);
                mapPane.getChildren().add(marker);
            }
        }
    }

    private List<Position> cablePath(PowerCableView cable) {
        List<Position> path = new ArrayList<>();
        path.add(objectCenter(cable.source()));
        path.addAll(cable.connection().routePoints());
        path.add(objectCenter(cable.tent()));
        return path;
    }

    private List<Position> cablePath(Tent tent, PowerSource source, PowerConnection connection) {
        return cablePath(new PowerCableView(tent, source, connection));
    }

    private Polyline createCablePolyline(List<Position> path) {
        Polyline polyline = new Polyline();
        for (Position point : path) {
            polyline.getPoints().addAll(point.x(), point.y());
        }
        return polyline;
    }

    private void makeCableSelectable(Node node, Tent tent) {
        makeCableSelectable(node, new PowerCableView(tent, null, null));
    }

    private void makeCableSelectable(Node node, PowerCableView cable) {
        node.setOnMouseClicked(event -> {
            Tent tent = cable.tent();
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
            if (pendingCustomObjectPlacement) {
                Point2D mapPoint = mapPane.sceneToLocal(event.getSceneX(), event.getSceneY());
                placeCustomObject(new Position(mapPoint.getX(), mapPoint.getY()));
                event.consume();
                return;
            }
            if (pendingTextObjectPlacement) {
                Point2D mapPoint = mapPane.sceneToLocal(event.getSceneX(), event.getSceneY());
                placeTextObject(new Position(mapPoint.getX(), mapPoint.getY()));
                event.consume();
                return;
            }
            if (pendingMarkerPlacement) {
                Point2D mapPoint = mapPane.sceneToLocal(event.getSceneX(), event.getSceneY());
                placeMarkerObject(new Position(mapPoint.getX(), mapPoint.getY()));
                event.consume();
                return;
            }
            if (addingCablePoint) {
                Point2D mapPoint = mapPane.sceneToLocal(event.getSceneX(), event.getSceneY());
                Position point = new Position(mapPoint.getX(), mapPoint.getY());
                if (cable.source() == null || cable.connection() == null) {
                    addCableRoutePoint(point);
                } else {
                    if (!isSelected(tent)) {
                        selectObject(tent);
                        event.consume();
                        return;
                    }
                    insertCableRoutePoint(cable, point);
                }
                event.consume();
                return;
            }
            if (measuringActive) {
                Point2D mapPoint = mapPane.sceneToLocal(event.getSceneX(), event.getSceneY());
                handleMeasureClick(new Position(mapPoint.getX(), mapPoint.getY()));
                event.consume();
                return;
            }
            selectObject(tent);
            event.consume();
        });
    }

    private void makeCableRoutePointDraggable(
            Circle marker,
            PowerCableView cable,
            int routePointIndex,
            Polyline line,
            Polyline highlightLine,
            Polyline hitLine,
            Label distanceLabel
    ) {
        final boolean[] dragged = {false};
        marker.setOnMousePressed(event -> {
            if (measuringActive || addingCablePoint) {
                event.consume();
                return;
            }
            dragged[0] = false;
            event.consume();
        });
        marker.setOnMouseDragged(event -> {
            if (measuringActive || addingCablePoint) {
                event.consume();
                return;
            }
            PowerConnection connection = plan.findPowerConnectionForConsumer(cable.tent().id()).orElse(null);
            if (connection == null || routePointIndex < 0 || routePointIndex >= connection.routePoints().size()) {
                event.consume();
                return;
            }

            Point2D mapPoint = mapPane.sceneToLocal(event.getSceneX(), event.getSceneY());
            List<Position> routePoints = new ArrayList<>(connection.routePoints());
            routePoints.set(routePointIndex, new Position(mapPoint.getX(), mapPoint.getY()));
            plan.updateCableRoutePoints(cable.tent().id(), routePoints);
            marker.setCenterX(mapPoint.getX());
            marker.setCenterY(mapPoint.getY());
            updateCablePolylineRoutePoint(line, routePointIndex, mapPoint);
            updateCablePolylineRoutePoint(highlightLine, routePointIndex, mapPoint);
            updateCablePolylineRoutePoint(hitLine, routePointIndex, mapPoint);
            updateCableLabel(distanceLabel, cable, routePoints);
            dragged[0] = true;
            event.consume();
        });
        marker.setOnMouseReleased(event -> {
            if (dragged[0]) {
                redrawMap();
                refreshSummary();
                markDirty();
            }
            event.consume();
        });
        marker.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                removeCableRoutePoint(cable, routePointIndex);
                event.consume();
                return;
            }
            if (!dragged[0]) {
                selectObject(cable.tent());
            }
            event.consume();
        });
    }

    private void removeCableRoutePoint(PowerCableView cable, int routePointIndex) {
        PowerConnection connection = plan.findPowerConnectionForConsumer(cable.tent().id()).orElse(null);
        if (connection == null || routePointIndex < 0 || routePointIndex >= connection.routePoints().size()) {
            return;
        }

        List<Position> routePoints = new ArrayList<>(connection.routePoints());
        routePoints.remove(routePointIndex);
        plan.updateCableRoutePoints(cable.tent().id(), routePoints);
        redrawMap();
        refreshSummary();
        markDirty();
    }

    private void updateCablePolylineRoutePoint(Polyline polyline, int routePointIndex, Point2D mapPoint) {
        int pointIndex = routePointIndex + 1;
        int xCoordinateIndex = pointIndex * 2;
        int yCoordinateIndex = xCoordinateIndex + 1;
        if (yCoordinateIndex >= polyline.getPoints().size()) {
            return;
        }
        polyline.getPoints().set(xCoordinateIndex, mapPoint.getX());
        polyline.getPoints().set(yCoordinateIndex, mapPoint.getY());
    }

    private void updateCableLabel(Label distanceLabel, PowerCableView cable, List<Position> routePoints) {
        if (distanceLabel == null) {
            return;
        }
        List<Position> path = new ArrayList<>();
        path.add(objectCenter(cable.source()));
        path.addAll(routePoints);
        path.add(objectCenter(cable.tent()));

        Position labelPosition = pathMidpoint(path);
        distanceLabel.setText(cableMapLabel(cable.connection(), cableLengthMeters(path)));
        distanceLabel.setLayoutX(labelPosition.x() + 6);
        distanceLabel.setLayoutY(labelPosition.y() + 6);
    }

    private Color cableColor(ConnectorType connectorType) {
        return switch (connectorType) {
            case SCHUKO_230V -> Color.web("#2563eb");
            case INDUSTRIAL_16A -> Color.web("#16a34a");
            case INDUSTRIAL_32A -> Color.web("#ea580c");
            case INDUSTRIAL_63A -> Color.web("#7c3aed");
        };
    }

    private String shortCableTypeName(ConnectorType connectorType) {
        return switch (connectorType) {
            case SCHUKO_230V -> "230V";
            case INDUSTRIAL_16A -> "16A";
            case INDUSTRIAL_32A -> "32A";
            case INDUSTRIAL_63A -> "63A";
        };
    }

    private String cableMapLabel(PowerConnection connection, double lengthMeters) {
        String baseLabel = "%s · %.1f m".formatted(shortCableTypeName(connection.connectorType()), lengthMeters);
        return connection.cableLengthNotes().isBlank()
                ? baseLabel
                : "%s · %s".formatted(baseLabel, connection.cableLengthNotes());
    }

    private double cableWidth(ConnectorType connectorType) {
        return switch (connectorType) {
            case SCHUKO_230V -> 2.0;
            case INDUSTRIAL_16A -> 2.8;
            case INDUSTRIAL_32A -> 3.6;
            case INDUSTRIAL_63A -> 4.4;
        };
    }

    private record PowerCableView(Tent tent, PowerSource source, PowerConnection connection) {
    }

    private Position objectCenter(PlannerObject object) {
        if (object instanceof Tent tent) {
            double widthPixels = metersToPixels(tent.widthMeters());
            double heightPixels = metersToPixels(tent.heightMeters());
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
        double widthPixels = metersToPixels(tent.widthMeters());
        double heightPixels = metersToPixels(tent.heightMeters());
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

    private void drawCustomObject(CustomObject object) {
        javafx.scene.shape.Shape shape;
        double widthPixels = metersToPixels(object.widthMeters());
        double heightPixels = metersToPixels(object.heightMeters());
        if (object.shape() == CustomObjectShape.CIRCLE) {
            shape = new Circle(object.position().x(), object.position().y(), widthPixels / 2);
        } else {
            Rectangle rectangle = new Rectangle(
                    object.position().x() - widthPixels / 2,
                    object.position().y() - heightPixels / 2,
                    widthPixels,
                    heightPixels
            );
            rectangle.setArcWidth(4);
            rectangle.setArcHeight(4);
            rectangle.setRotate(object.rotationDegrees());
            shape = rectangle;
        }
        shape.setFill(Color.web(object.colorHex()));
        shape.setStroke(Color.web("#111827"));
        shape.setStrokeWidth(isSelected(object) ? 4 : 1);
        applyLockedStroke(shape, object);
        makeSelectable(shape, object);
        makeDraggable(shape, object);

        Label label = new Label(mapLabel(object));
        label.setLayoutX(object.position().x() + 16);
        label.setLayoutY(object.position().y() - 12);
        makeSelectable(label, object);

        mapPane.getChildren().addAll(shape, label);
    }

    private void drawTextObject(TextObject object) {
        Label textLabel = new Label(object.name());
        textLabel.setLayoutX(object.position().x());
        textLabel.setLayoutY(object.position().y());
        textLabel.setTextFill(Color.web(object.colorHex()));
        textLabel.setStyle(isSelected(object)
                ? "-fx-background-color: rgba(255,255,255,0.85); -fx-border-color: #111827; -fx-border-width: 2; -fx-padding: 3 6 3 6; -fx-font-weight: bold;"
                : "-fx-background-color: rgba(255,255,255,0.7); -fx-padding: 3 6 3 6; -fx-font-weight: bold;");
        makeSelectable(textLabel, object);
        makeDraggable(textLabel, object);
        mapPane.getChildren().add(textLabel);
    }

    private void drawMarkerObject(MarkerObject object) {
        Pane markerIcon = createMarkerIcon(object);
        markerIcon.setLayoutX(object.position().x());
        markerIcon.setLayoutY(object.position().y());
        markerIcon.setStyle("-fx-background-color: %s; -fx-background-radius: 6; -fx-border-radius: 6;%s".formatted(
                object.colorHex(),
                isSelected(object) ? " -fx-border-color: #111827; -fx-border-width: 2;" : " -fx-border-color: #111827; -fx-border-width: 1;"
        ));
        makeSelectable(markerIcon, object);
        makeDraggable(markerIcon, object);

        Label label = new Label(object.name());
        label.setLayoutX(object.position().x() + 34);
        label.setLayoutY(object.position().y() + 4);
        makeSelectable(label, object);

        mapPane.getChildren().addAll(markerIcon, label);
    }

    private Pane createMarkerIcon(MarkerObject object) {
        Pane icon = new Pane();
        icon.setMinSize(28, 28);
        icon.setPrefSize(28, 28);
        icon.setMaxSize(28, 28);
        icon.getChildren().addAll(switch (object.markerType()) {
            case WC -> wcIcon();
            case SECURITY -> securityIcon();
            case INFO -> infoIcon();
            case START_FINISH -> startFinishIcon();
        });
        return icon;
    }

    private List<Node> wcIcon() {
        Circle leftHead = new Circle(10, 8, 3, Color.WHITE);
        Circle rightHead = new Circle(18, 8, 3, Color.WHITE);
        Rectangle leftBody = new Rectangle(7, 12, 6, 10);
        Rectangle rightBody = new Rectangle(15, 12, 6, 10);
        leftBody.setFill(Color.WHITE);
        rightBody.setFill(Color.WHITE);
        return List.of(leftHead, rightHead, leftBody, rightBody);
    }

    private List<Node> securityIcon() {
        Polygon shield = new Polygon(
                14.0, 5.0,
                22.0, 8.0,
                20.0, 17.0,
                14.0, 23.0,
                8.0, 17.0,
                6.0, 8.0
        );
        shield.setFill(Color.WHITE);
        return List.of(shield);
    }

    private List<Node> infoIcon() {
        Circle circle = new Circle(14, 14, 9, Color.WHITE);
        Label label = new Label("i");
        label.setTextFill(Color.web("#111827"));
        label.setStyle("-fx-font-weight: bold; -fx-font-size: 16;");
        label.setLayoutX(12);
        label.setLayoutY(4);
        return List.of(circle, label);
    }

    private List<Node> startFinishIcon() {
        Line pole = new Line(9, 6, 9, 23);
        pole.setStroke(Color.WHITE);
        pole.setStrokeWidth(2);
        Polygon flag = new Polygon(
                10.0, 6.0,
                22.0, 9.0,
                10.0, 13.0
        );
        flag.setFill(Color.WHITE);
        return List.of(pole, flag);
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
            if (pendingCustomObjectPlacement) {
                Point2D mapPoint = mapPane.sceneToLocal(event.getSceneX(), event.getSceneY());
                placeCustomObject(new Position(mapPoint.getX(), mapPoint.getY()));
                event.consume();
                return;
            }
            if (pendingTextObjectPlacement) {
                Point2D mapPoint = mapPane.sceneToLocal(event.getSceneX(), event.getSceneY());
                placeTextObject(new Position(mapPoint.getX(), mapPoint.getY()));
                event.consume();
                return;
            }
            if (pendingMarkerPlacement) {
                Point2D mapPoint = mapPane.sceneToLocal(event.getSceneX(), event.getSceneY());
                placeMarkerObject(new Position(mapPoint.getX(), mapPoint.getY()));
                event.consume();
                return;
            }
            if (addingCablePoint) {
                Point2D mapPoint = mapPane.sceneToLocal(event.getSceneX(), event.getSceneY());
                addCableRoutePoint(new Position(mapPoint.getX(), mapPoint.getY()));
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
            if (measuringActive || addingCablePoint) {
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
            if (measuringActive || addingCablePoint) {
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
        if (groupFilterSection != null) {
            setSectionVisible(groupFilterSection, !currentGroups.isEmpty());
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
        boolean customObjectSelected = selectedObject instanceof CustomObject;
        boolean textObjectSelected = selectedObject instanceof TextObject;
        boolean markerSelected = selectedObject instanceof MarkerObject;
        nameField.setDisable(!hasSelection);
        groupField.setDisable(!hasSelection);
        notesArea.setDisable(!hasSelection);
        lockedCheckBox.setDisable(!hasSelection);
        boolean lockedSelection = selectedObject != null && selectedObject.locked();
        deleteObjectButton.setDisable(!hasSelection || lockedSelection);
        deleteObjectButton.setTooltip(lockedSelection
                ? new Tooltip("Lukustatud objekti kustutamiseks eemalda enne lukustus")
                : null);
        customObjectShapeComboBox.setDisable(!customObjectSelected);
        customObjectColorPicker.setDisable(!customObjectSelected);
        textObjectColorPicker.setDisable(!textObjectSelected);
        markerTypeComboBox.setDisable(!markerSelected);
        markerColorPicker.setDisable(!markerSelected);
        tentWidthField.setDisable(!tentSelected);
        tentHeightField.setDisable(!tentSelected);
        tentRotationField.setDisable(!tentSelected);
        tentColorPicker.setDisable(!tentSelected);
        powerSourceComboBox.setDisable(!tentSelected);
        connectionTypeComboBox.setDisable(!tentSelected);
        connectionOutletComboBox.setDisable(!tentSelected);
        cableLengthNotesField.setDisable(!tentSelected);
        cableNotesField.setDisable(!tentSelected);
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
        boolean tentHasPowerConnection = selectedObject instanceof Tent tent
                && plan.findPowerConnectionForConsumer(tent.id()).isPresent();
        if (addCablePointButton != null) {
            addCablePointButton.setDisable(!tentHasPowerConnection);
            addCablePointButton.setText(addingCablePoint ? "Tähista kaabli punkt" : "Kaabli punkt");
            if (!tentHasPowerConnection) {
                addCablePointButton.setSelected(false);
                addingCablePoint = false;
                updateMapToolStatus();
            }
        }
        if (clearCableRouteButton != null) {
            clearCableRouteButton.setDisable(!tentHasPowerConnection);
        }
        boolean selectingPowerSourceForThisTent = tentSelected
                && pendingPowerSourceTent != null
                && pendingPowerSourceTent.id().equals(selectedObject.id());
        choosePowerSourceButton.setText(selectingPowerSourceForThisTent
                ? "Tühista kapi valik"
                : "Vali kapp kaardilt");
        setSectionVisible(customObjectPanel, customObjectSelected);
        setSectionVisible(textObjectPanel, textObjectSelected);
        setSectionVisible(markerPanel, markerSelected);
        setSectionVisible(tentPanel, tentSelected);
        setSectionVisible(powerConnectionPanel, tentSelected);
        setSectionVisible(equipmentSection, tentSelected);
        setSectionVisible(outletSection, powerSourceSelected);
        setSectionVisible(choosePowerSourceButton, tentSelected);
        setSectionVisible(deleteObjectButton, hasSelection);

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
            customObjectShapeComboBox.getSelectionModel().select(CustomObjectShape.SQUARE);
            customObjectColorPicker.setValue(Color.web("#9ca3af"));
            textObjectColorPicker.setValue(Color.web("#111827"));
            markerTypeComboBox.getSelectionModel().select(MarkerType.WC);
            markerColorPicker.setValue(Color.web("#0f766e"));
            customObjectWidthField.clear();
            customObjectHeightField.clear();
            customObjectRotationField.clear();
            cableLengthNotesField.clear();
            cableNotesField.clear();
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
            customObjectShapeComboBox.getSelectionModel().select(CustomObjectShape.SQUARE);
            customObjectColorPicker.setValue(Color.web("#9ca3af"));
            textObjectColorPicker.setValue(Color.web("#111827"));
            markerTypeComboBox.getSelectionModel().select(MarkerType.WC);
            markerColorPicker.setValue(Color.web("#0f766e"));
            customObjectWidthField.clear();
            customObjectHeightField.clear();
            customObjectRotationField.clear();
            cableLengthNotesField.setText(plan.findPowerConnectionForConsumer(tent.id())
                    .map(PowerConnection::cableLengthNotes)
                    .orElse(""));
            cableNotesField.setText(plan.findPowerConnectionForConsumer(tent.id())
                    .map(PowerConnection::cableNotes)
                    .orElse(""));
        } else if (selectedObject instanceof CustomObject customObject) {
            tentWidthField.clear();
            tentHeightField.clear();
            tentRotationField.clear();
            tentColorPicker.setValue(Color.web("#2563eb"));
            customObjectShapeComboBox.getSelectionModel().select(customObject.shape());
            customObjectColorPicker.setValue(Color.web(customObject.colorHex()));
            customObjectWidthField.setText(formatMeters(customObject.widthMeters()));
            customObjectHeightField.setText(formatMeters(customObject.heightMeters()));
            customObjectRotationField.setText(formatDegrees(customObject.rotationDegrees()));
            cableLengthNotesField.clear();
            cableNotesField.clear();
        } else if (selectedObject instanceof TextObject textObject) {
            tentWidthField.clear();
            tentHeightField.clear();
            tentRotationField.clear();
            tentColorPicker.setValue(Color.web("#2563eb"));
            customObjectShapeComboBox.getSelectionModel().select(CustomObjectShape.SQUARE);
            customObjectColorPicker.setValue(Color.web("#9ca3af"));
            textObjectColorPicker.setValue(Color.web(textObject.colorHex()));
            markerTypeComboBox.getSelectionModel().select(MarkerType.WC);
            markerColorPicker.setValue(Color.web("#0f766e"));
            customObjectWidthField.clear();
            customObjectHeightField.clear();
            customObjectRotationField.clear();
            cableLengthNotesField.clear();
            cableNotesField.clear();
        } else if (selectedObject instanceof MarkerObject markerObject) {
            tentWidthField.clear();
            tentHeightField.clear();
            tentRotationField.clear();
            tentColorPicker.setValue(Color.web("#2563eb"));
            customObjectShapeComboBox.getSelectionModel().select(CustomObjectShape.SQUARE);
            customObjectColorPicker.setValue(Color.web("#9ca3af"));
            textObjectColorPicker.setValue(Color.web("#111827"));
            markerTypeComboBox.getSelectionModel().select(markerObject.markerType());
            markerColorPicker.setValue(Color.web(markerObject.colorHex()));
            customObjectWidthField.clear();
            customObjectHeightField.clear();
            customObjectRotationField.clear();
            cableLengthNotesField.clear();
            cableNotesField.clear();
        } else {
            tentWidthField.clear();
            tentHeightField.clear();
            tentRotationField.clear();
            tentColorPicker.setValue(Color.web("#2563eb"));
            customObjectShapeComboBox.getSelectionModel().select(CustomObjectShape.SQUARE);
            customObjectColorPicker.setValue(Color.web("#9ca3af"));
            textObjectColorPicker.setValue(Color.web("#111827"));
            markerTypeComboBox.getSelectionModel().select(MarkerType.WC);
            markerColorPicker.setValue(Color.web("#0f766e"));
            customObjectWidthField.clear();
            customObjectHeightField.clear();
            customObjectRotationField.clear();
            cableLengthNotesField.clear();
            cableNotesField.clear();
        }
        refreshPowerSourceChoices();
        refreshEquipmentList();
        refreshOutletList();
        updateCustomObjectSizeFields();
    }

    private void updateCustomObjectSizeFields() {
        boolean customObjectSelected = selectedObject instanceof CustomObject;
        CustomObjectShape selectedShape = customObjectShapeComboBox.getSelectionModel().getSelectedItem();
        boolean circleSelected = selectedShape == CustomObjectShape.CIRCLE;
        customObjectWidthLabel.setText(circleSelected ? "Objekti läbimõõt m" : "Objekti laius m");
        customObjectWidthLabel.setVisible(true);
        customObjectWidthLabel.setManaged(true);
        customObjectWidthField.setVisible(true);
        customObjectWidthField.setManaged(true);
        customObjectHeightLabel.setVisible(!circleSelected);
        customObjectHeightLabel.setManaged(!circleSelected);
        customObjectHeightField.setVisible(!circleSelected);
        customObjectHeightField.setManaged(!circleSelected);
        customObjectWidthField.setDisable(!customObjectSelected);
        customObjectHeightField.setDisable(!customObjectSelected || circleSelected);
        customObjectRotationLabel.setVisible(!circleSelected);
        customObjectRotationLabel.setManaged(!circleSelected);
        customObjectRotationField.setVisible(!circleSelected);
        customObjectRotationField.setManaged(!circleSelected);
        customObjectRotationField.setDisable(!customObjectSelected || circleSelected);
        if (customObjectSelected && !circleSelected && customObjectRotationField.getText().isBlank()) {
            customObjectRotationField.setText("0");
        }
    }

    private void setSectionVisible(Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    private void startPowerSourceSelectionFromMap() {
        if (!(selectedObject instanceof Tent tent)) {
            return;
        }
        pendingTentPlacement = false;
        pendingPowerSourcePlacement = false;
        pendingCustomObjectPlacement = false;
        pendingTextObjectPlacement = false;
        clearPendingPlacementDetails();
        refreshPlacementButtons();
        updateMapToolStatus();
        if (pendingPowerSourceTent != null && pendingPowerSourceTent.id().equals(tent.id())) {
            pendingPowerSourceTent = null;
            updateMapToolStatus();
            refreshDetails();
            return;
        }
        pendingPowerSourceTent = tent;
        updateMapToolStatus();
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
        updateMapToolStatus();
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

    private void autoApplyNotes() {
        if (selectedObject == null) {
            return;
        }
        String notes = notesArea.getText();
        if (selectedObject.notes().equals(notes)) {
            return;
        }
        selectedObject.setNotes(notes);
        markDirty();
    }

    private void autoApplyCableNotes() {
        if (!(selectedObject instanceof Tent tent)) {
            return;
        }
        String cableNotes = cableNotesField.getText();
        PowerConnection connection = plan.findPowerConnectionForConsumer(tent.id()).orElse(null);
        if (connection == null || connection.cableNotes().equals(cableNotes.trim())) {
            return;
        }
        plan.updateCableNotes(tent.id(), cableNotes);
        cableNotesField.setText(plan.findPowerConnectionForConsumer(tent.id())
                .map(PowerConnection::cableNotes)
                .orElse(""));
        redrawMap();
        refreshSummary();
        markDirty();
    }

    private void autoApplyCableLengthNotes() {
        if (!(selectedObject instanceof Tent tent)) {
            return;
        }
        String cableLengthNotes = cableLengthNotesField.getText();
        PowerConnection connection = plan.findPowerConnectionForConsumer(tent.id()).orElse(null);
        if (connection == null || connection.cableLengthNotes().equals(cableLengthNotes.trim())) {
            return;
        }
        plan.updateCableLengthNotes(tent.id(), cableLengthNotes);
        cableLengthNotesField.setText(plan.findPowerConnectionForConsumer(tent.id())
                .map(PowerConnection::cableLengthNotes)
                .orElse(""));
        redrawMap();
        refreshSummary();
        markDirty();
    }

    private String objectTypeName(PlannerObject object) {
        if (object instanceof Tent) {
            return "Telk";
        }
        if (object instanceof PowerSource) {
            return "Elektrikapp";
        }
        if (object instanceof TextObject) {
            return "Tekst";
        }
        if (object instanceof MarkerObject) {
            return "Marker";
        }
        if (object instanceof CustomObject) {
            return "Objekt";
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
        } else if (selectedObject instanceof CustomObject customObject) {
            if (!applyCustomObjectSize(customObject)) {
                return;
            }
            if (!applyCustomObjectRotation(customObject)) {
                return;
            }
            CustomObjectShape selectedShape = customObjectShapeComboBox.getSelectionModel().getSelectedItem();
            customObject.setShape(selectedShape);
            customObject.setColorHex(toHex(customObjectColorPicker.getValue()));
        } else if (selectedObject instanceof TextObject textObject) {
            textObject.setColorHex(toHex(textObjectColorPicker.getValue()));
        } else if (selectedObject instanceof MarkerObject markerObject) {
            MarkerType selectedMarkerType = markerTypeComboBox.getSelectionModel().getSelectedItem();
            markerObject.setMarkerType(selectedMarkerType);
            markerObject.setColorHex(toHex(markerColorPicker.getValue()));
        }
        setAddingCablePoint(false);
        if (addCablePointButton != null) {
            addCablePointButton.setSelected(false);
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
        if (selectedObject.locked()) {
            showError("Objekti ei kustutatud", "Eemalda enne lukustus ja proovi uuesti.");
            return;
        }
        if (!confirmDeleteSelectedObject()) {
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

    private boolean confirmDeleteSelectedObject() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Kustuta objekt");
        alert.setHeaderText("Kas kustutada \"%s\"?".formatted(selectedObject.name()));
        alert.setContentText(deleteConfirmationText(selectedObject));
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    private String deleteConfirmationText(PlannerObject object) {
        List<String> warnings = new ArrayList<>();
        if (object instanceof Tent tent) {
            if (!tent.equipment().isEmpty()) {
                warnings.add("Telgi seadmed kustutatakse samuti.");
            }
            if (plan.findPowerConnectionForConsumer(tent.id()).isPresent()) {
                warnings.add("Telgi vooluühendus ja kaabli trajektoor kustutatakse samuti.");
            }
        }
        if (object instanceof PowerSource source) {
            int connectionCount = (int) plan.powerConnections().stream()
                    .filter(connection -> connection.sourceId().equals(source.id()))
                    .count();
            if (connectionCount > 0) {
                warnings.add("%d selle kapiga seotud vooluühendus(t) kustutatakse samuti.".formatted(connectionCount));
            }
        }
        if (warnings.isEmpty()) {
            return "Seda tegevust ei saa tagasi võtta.";
        }
        return "%s%n%nSeda tegevust ei saa tagasi võtta.".formatted(String.join(System.lineSeparator(), warnings));
    }

    private void setMeasuringActive(boolean measuringActive) {
        this.measuringActive = measuringActive;
        if (measuringActive) {
            addingCablePoint = false;
            if (addCablePointButton != null) {
                addCablePointButton.setSelected(false);
            }
            pendingTentPlacement = false;
            pendingPowerSourcePlacement = false;
            pendingCustomObjectPlacement = false;
            pendingTextObjectPlacement = false;
            clearPendingPlacementDetails();
            refreshPlacementButtons();
        }
        measurementStart = null;
        updateMapToolStatus();
    }

    private void setAddingCablePoint(boolean addingCablePoint) {
        this.addingCablePoint = addingCablePoint;
        if (addingCablePoint) {
            measuringActive = false;
            if (measureButton != null) {
                measureButton.setSelected(false);
            }
            pendingTentPlacement = false;
            pendingPowerSourcePlacement = false;
            pendingCustomObjectPlacement = false;
            pendingTextObjectPlacement = false;
            clearPendingPlacementDetails();
            pendingPowerSourceTent = null;
            refreshPlacementButtons();
        }
        measurementStart = null;
        refreshDetails();
        updateMapToolStatus();
    }

    private void addCableRoutePoint(Position point) {
        if (!(selectedObject instanceof Tent tent)) {
            showError("Kaabli punkti ei lisatud", "Vali enne telk, mille voolukaablile punkt lisada.");
            setAddingCablePoint(false);
            if (addCablePointButton != null) {
                addCablePointButton.setSelected(false);
            }
            return;
        }
        if (plan.findPowerConnectionForConsumer(tent.id()).isEmpty()) {
            showError("Kaabli punkti ei lisatud", "Valitud telgil ei ole veel vooluühendust.");
            setAddingCablePoint(false);
            if (addCablePointButton != null) {
                addCablePointButton.setSelected(false);
            }
            return;
        }
        plan.addCableRoutePoint(tent.id(), point);
        redrawMap();
        refreshSummary();
        markDirty();
    }

    private void insertCableRoutePoint(PowerCableView cable, Position point) {
        PowerConnection connection = plan.findPowerConnectionForConsumer(cable.tent().id()).orElse(null);
        if (connection == null) {
            return;
        }

        int insertionIndex = closestCableSegmentIndex(cablePath(cable.tent(), cable.source(), connection), point);
        plan.insertCableRoutePoint(cable.tent().id(), insertionIndex, point);
        selectedObject = cable.tent();
        redrawMap();
        refreshSummary();
        markDirty();
    }

    private int closestCableSegmentIndex(List<Position> path, Position point) {
        int closestSegmentIndex = 0;
        double closestDistance = Double.MAX_VALUE;
        for (int index = 1; index < path.size(); index++) {
            double distance = distanceToSegmentSquared(point, path.get(index - 1), path.get(index));
            if (distance < closestDistance) {
                closestDistance = distance;
                closestSegmentIndex = index - 1;
            }
        }
        return closestSegmentIndex;
    }

    private double distanceToSegmentSquared(Position point, Position segmentStart, Position segmentEnd) {
        double segmentX = segmentEnd.x() - segmentStart.x();
        double segmentY = segmentEnd.y() - segmentStart.y();
        double lengthSquared = segmentX * segmentX + segmentY * segmentY;
        if (lengthSquared == 0) {
            double deltaX = point.x() - segmentStart.x();
            double deltaY = point.y() - segmentStart.y();
            return deltaX * deltaX + deltaY * deltaY;
        }

        double ratio = ((point.x() - segmentStart.x()) * segmentX + (point.y() - segmentStart.y()) * segmentY) / lengthSquared;
        double clampedRatio = Math.max(0, Math.min(1, ratio));
        double projectionX = segmentStart.x() + clampedRatio * segmentX;
        double projectionY = segmentStart.y() + clampedRatio * segmentY;
        double deltaX = point.x() - projectionX;
        double deltaY = point.y() - projectionY;
        return deltaX * deltaX + deltaY * deltaY;
    }

    private void clearSelectedCableRoute() {
        if (!(selectedObject instanceof Tent tent)) {
            return;
        }
        if (plan.findPowerConnectionForConsumer(tent.id()).isEmpty()) {
            return;
        }
        plan.clearCableRoutePoints(tent.id());
        redrawMap();
        refreshSummary();
        markDirty();
    }

    private void refreshPlacementButtons() {
        boolean placementPending = isPlacementPending();
        if (placementTypeComboBox != null) {
            placementTypeComboBox.setDisable(placementPending);
            if (pendingTentPlacement) {
                placementTypeComboBox.getSelectionModel().select(PlacementType.TENT);
            } else if (pendingPowerSourcePlacement) {
                placementTypeComboBox.getSelectionModel().select(PlacementType.POWER_SOURCE);
            } else if (pendingCustomObjectPlacement) {
                placementTypeComboBox.getSelectionModel().select(PlacementType.CUSTOM_OBJECT);
            } else if (pendingTextObjectPlacement) {
                placementTypeComboBox.getSelectionModel().select(PlacementType.TEXT_OBJECT);
            }
        }
        if (addPlacementButton != null) {
            addPlacementButton.setText(placementPending ? "Tühista" : "Lisa");
        }
    }

    private boolean isPlacementPending() {
        return pendingTentPlacement || pendingPowerSourcePlacement || pendingCustomObjectPlacement || pendingTextObjectPlacement;
    }

    private void cancelPlacement() {
        pendingTentPlacement = false;
        pendingPowerSourcePlacement = false;
        pendingCustomObjectPlacement = false;
        pendingTextObjectPlacement = false;
        pendingPowerSourceTent = null;
        clearPendingPlacementDetails();
        refreshPlacementButtons();
        updateMapToolStatus();
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
        measurements.add(new MeasurementView(measurementStart, end, distanceLabel));
        mapPane.getChildren().addAll(line, endMarker, distanceLabel);
        measurementStart = null;
    }

    private void refreshMeasurementLabels() {
        for (MeasurementView measurement : measurements) {
            measurement.distanceLabel().setText("%.2f m".formatted(distanceMeters(measurement.start(), measurement.end())));
        }
    }

    private boolean setScaleFromLastMeasurement() {
        if (measurements.isEmpty()) {
            showError("Mõõtkava ei muudetud", "Tee enne mõõdulindiga üks mõõtmine.");
            return false;
        }

        MeasurementView measurement = measurements.getLast();
        TextInputDialog dialog = new TextInputDialog("%.2f".formatted(distanceMeters(measurement.start(), measurement.end())));
        dialog.setTitle("Määra mõõtkava");
        dialog.setHeaderText("Sisesta viimase mõõdulindi joone tegelik pikkus meetrites");
        dialog.setContentText("Tegelik pikkus m:");
        String value = dialog.showAndWait().orElse(null);
        if (value == null) {
            return false;
        }

        try {
            double realLengthMeters = Double.parseDouble(value.trim().replace(',', '.'));
            if (realLengthMeters <= 0) {
                throw new IllegalArgumentException("Tegelik pikkus peab olema positiivne.");
            }
            double pixelLength = distancePixels(measurement.start(), measurement.end());
            if (pixelLength <= 0) {
                showError("Mõõtkava ei muudetud", "Mõõdulindi pikkus peab olema suurem kui 0.");
                return false;
            }
            plan.setPixelsPerMeter(pixelLength / realLengthMeters);
            if (pixelsPerMeterField != null) {
                pixelsPerMeterField.setText(formatMeters(plan.pixelsPerMeter()));
            }
            refreshMeasurementLabels();
            redrawMap();
            refreshSummary();
            markDirty();
            return true;
        } catch (NumberFormatException exception) {
            showError("Mõõtkava ei muudetud", "Sisesta tegelik pikkus arvuna meetrites.");
        } catch (IllegalArgumentException exception) {
            showError("Mõõtkava ei muudetud", exception.getMessage());
        }
        return false;
    }

    private Circle createMeasurementMarker(Position point) {
        Circle marker = new Circle(point.x(), point.y(), 4);
        marker.setFill(Color.web("#111827"));
        marker.setStroke(Color.WHITE);
        return marker;
    }

    private double distanceMeters(Position start, Position end) {
        return distancePixels(start, end) / pixelsPerMeter();
    }

    private double distancePixels(Position start, Position end) {
        double deltaX = end.x() - start.x();
        double deltaY = end.y() - start.y();
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY);
    }

    private double metersToPixels(double meters) {
        return meters * pixelsPerMeter();
    }

    private double pixelsPerMeter() {
        return plan == null ? EventPlan.DEFAULT_PIXELS_PER_METER : plan.pixelsPerMeter();
    }

    private double cableLengthMeters(List<Position> path) {
        double lengthMeters = 0.0;
        for (int index = 1; index < path.size(); index++) {
            lengthMeters += distanceMeters(path.get(index - 1), path.get(index));
        }
        return lengthMeters;
    }

    private Position pathMidpoint(List<Position> path) {
        if (path.isEmpty()) {
            return new Position(0, 0);
        }
        if (path.size() == 1) {
            return path.getFirst();
        }

        double halfLengthMeters = cableLengthMeters(path) / 2;
        double walkedMeters = 0.0;
        for (int index = 1; index < path.size(); index++) {
            Position start = path.get(index - 1);
            Position end = path.get(index);
            double segmentLengthMeters = distanceMeters(start, end);
            if (walkedMeters + segmentLengthMeters >= halfLengthMeters) {
                double ratio = segmentLengthMeters == 0 ? 0 : (halfLengthMeters - walkedMeters) / segmentLengthMeters;
                return new Position(
                        start.x() + (end.x() - start.x()) * ratio,
                        start.y() + (end.y() - start.y()) * ratio
                );
            }
            walkedMeters += segmentLengthMeters;
        }
        return path.getLast();
    }

    private void clearMeasurements() {
        measurementNodes.clear();
        measurements.clear();
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

    private boolean applyCustomObjectSize(CustomObject object) {
        try {
            CustomObjectShape selectedShape = customObjectShapeComboBox.getSelectionModel().getSelectedItem();
            double widthMeters = Double.parseDouble(customObjectWidthField.getText().trim().replace(',', '.'));
            if (selectedShape == CustomObjectShape.CIRCLE) {
                object.setSizeMeters(widthMeters, widthMeters);
                return true;
            }
            double heightMeters = Double.parseDouble(customObjectHeightField.getText().trim().replace(',', '.'));
            object.setSizeMeters(widthMeters, heightMeters);
            return true;
        } catch (NumberFormatException exception) {
            if (customObjectShapeComboBox.getSelectionModel().getSelectedItem() == CustomObjectShape.CIRCLE) {
                showError("Mõõte ei muudetud", "Sisesta objekti läbimõõt arvuna meetrites.");
            } else {
                showError("Mõõte ei muudetud", "Sisesta objekti laius ja pikkus arvuna meetrites.");
            }
            return false;
        } catch (IllegalArgumentException exception) {
            showError("Mõõte ei muudetud", exception.getMessage());
            return false;
        }
    }

    private boolean applyCustomObjectRotation(CustomObject object) {
        CustomObjectShape selectedShape = customObjectShapeComboBox.getSelectionModel().getSelectedItem();
        if (selectedShape == CustomObjectShape.CIRCLE) {
            object.setRotationDegrees(0);
            return true;
        }
        try {
            double rotationDegrees = Double.parseDouble(customObjectRotationField.getText().trim().replace(',', '.'));
            object.setRotationDegrees(rotationDegrees);
            return true;
        } catch (NumberFormatException exception) {
            showError("Pööret ei muudetud", "Sisesta objekti pööre arvuna kraadides.");
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
            return "ÜLEKOORMUS %d W".formatted(Math.abs(remainingWatts));
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
                selectedConnectionOutletId(),
                cableNotesField.getText(),
                cableLengthNotesField.getText()
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
        alert.setTitle("Eemalda väljund");
        alert.setHeaderText("See väljund on kasutusel");
        String tentRows = connectedTents.stream()
                .map(tent -> "- " + tent.name())
                .reduce("", (rows, row) -> rows + row + System.lineSeparator());
        alert.setContentText("%s kustutamisel eemaldatakse nende telkide vooluühendused:%n%n%s".formatted(
                outlet.name().isBlank() ? outlet.type().displayName() : outlet.name(),
                tentRows
        ));
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    private boolean confirmOutletTypeChange(PowerOutlet outlet, ConnectorType selectedType, List<Tent> connectedTents) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Muuda väljundi tüüpi");
        alert.setHeaderText("See väljund on kasutusel");
        String tentRows = connectedTents.stream()
                .map(tent -> "- " + tent.name())
                .reduce("", (rows, row) -> rows + row + System.lineSeparator());
        alert.setContentText("%s tüüp muutub: %s -> %s.%n%nNende telkide ühenduse tüüp muutub samuti:%n%n%s".formatted(
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

    private StringConverter<CustomObjectShape> customObjectShapeConverter() {
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

    private StringConverter<MarkerType> markerTypeConverter() {
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
        if (showPowerSummary()) {
            for (PowerSummary summary : powerSummaryService.summaries(plan)) {
                summaryList.getItems().add("%s: %d W kasutusel, %s".formatted(
                        summary.sourceName(),
                        summary.usedWatts(),
                        remainingWattsText(summary.remainingWatts())
                ));
                addConnectedConsumers(summary.sourceId());
            }
        }
        if (showCableSummary()) {
            addCableSummary();
        }
        if (showGroupSummary()) {
            addGroupSummary();
        }
    }

    private boolean showPowerSummary() {
        return showPowerSummaryCheckBox == null || showPowerSummaryCheckBox.isSelected();
    }

    private boolean showCableSummary() {
        return showCableSummaryCheckBox == null || showCableSummaryCheckBox.isSelected();
    }

    private boolean showGroupSummary() {
        return showGroupSummaryCheckBox == null || showGroupSummaryCheckBox.isSelected();
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

        List<CableSummaryRow> cableRows = new ArrayList<>();
        double totalLengthMeters = 0.0;
        double totalNotedLengthMeters = 0.0;
        boolean hasNotedLength = false;
        Map<ConnectorType, CableTypeSummary> summariesByType = new EnumMap<>(ConnectorType.class);

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

            double lengthMeters = cableLengthMeters(cablePath(tent, source, connection));
            totalLengthMeters += lengthMeters;
            OptionalDouble notedLengthMeters = notedCableLengthMeters(connection);
            CableTypeSummary typeSummary = summariesByType.computeIfAbsent(
                    connection.connectorType(),
                    ignored -> new CableTypeSummary()
            );
            typeSummary.addMapLength(lengthMeters);
            if (notedLengthMeters.isPresent()) {
                totalNotedLengthMeters += notedLengthMeters.getAsDouble();
                typeSummary.addNotedLength(notedLengthMeters.getAsDouble());
                typeSummary.addPieces(cableLengthPieces(connection));
                hasNotedLength = true;
            }
            cableRows.add(new CableSummaryRow(tent, source, connection, lengthMeters, notedLengthMeters));
        }

        if (cableRows.isEmpty()) {
            return;
        }

        addSummarySpacerIfNeeded();
        summaryList.getItems().add("Kaablid");
        summaryList.getItems().addAll(cableRows.stream()
                .sorted(CABLE_SUMMARY_ROW_COMPARATOR)
                .map(this::cableSummaryRow)
                .toList());
        if (hasNotedLength) {
            summaryList.getItems().add("Kokku: %.1f m märgitud, %.1f m kaardil".formatted(totalNotedLengthMeters, totalLengthMeters));
        } else {
            summaryList.getItems().add("Kokku: %.1f m".formatted(totalLengthMeters));
        }
        addCableTypeSummaryRows(summaryList.getItems(), summariesByType);
    }

    private void addGroupSummary() {
        if (plan.objects().isEmpty()) {
            return;
        }

        addSummarySpacerIfNeeded();
        summaryList.getItems().add("Grupid");
        for (Map.Entry<String, List<PlannerObject>> entry : objectsByGroup().entrySet()) {
            summaryList.getItems().add(entry.getKey());
            for (PlannerObject object : entry.getValue()) {
                summaryList.getItems().add("  - %s (%s)".formatted(object.name(), objectTypeName(object)));
            }
        }
    }

    private void addSummarySpacerIfNeeded() {
        if (!summaryList.getItems().isEmpty()) {
            summaryList.getItems().add("");
        }
    }

    private Map<String, List<PlannerObject>> objectsByGroup() {
        Map<String, List<PlannerObject>> objectsByGroup = new TreeMap<>();
        for (PlannerObject object : plan.objects()) {
            String groupName = object.groupName().isBlank() ? "Määramata" : object.groupName();
            objectsByGroup.computeIfAbsent(groupName, ignored -> new ArrayList<>()).add(object);
        }
        return objectsByGroup;
    }

    private boolean savePlan() {
        if (currentPlanFile == null) {
            return savePlanAs();
        }

        return savePlanToFile(currentPlanFile);
    }

    private boolean savePlanAs() {
        FileChooser fileChooser = createPlanFileChooser();
        applyInitialDirectory(fileChooser);
        if (currentPlanFile != null) {
            fileChooser.setInitialFileName(currentPlanFile.getName());
        }
        File file = fileChooser.showSaveDialog(stage);
        if (file == null) {
            return false;
        }

        return savePlanToFile(file);
    }

    private boolean savePlanToFile(File file) {
        try {
            planFileService.save(plan, file.toPath());
            currentPlanFile = file;
            rememberDirectory(file);
            markClean();
            return true;
        } catch (IOException exception) {
            showError("Salvestamine ebaõnnestus", exception.getMessage());
            return false;
        }
    }

    private void exportSummary() {
        refreshSummary();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Ekspordi kokkuvõte");
        applyInitialDirectory(fileChooser);
        fileChooser.setInitialFileName(exportSummaryFileName());
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Tekstifail", "*.txt"));
        File file = fileChooser.showSaveDialog(stage);
        if (file == null) {
            return;
        }

        try {
            Files.writeString(file.toPath(), summaryText(), StandardCharsets.UTF_8);
            rememberDirectory(file);
        } catch (IOException exception) {
            showError("Eksportimine ebaõnnestus", exception.getMessage());
        }
    }

    private String summaryText() {
        String lineSeparator = System.lineSeparator();
        StringBuilder builder = new StringBuilder();
        builder.append(plan.name()).append(lineSeparator);
        builder.append(lineSeparator);

        if (showPowerSummary()) {
            builder.append("Voolu kokkuvõte pesade kaupa").append(lineSeparator);
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
                    builder.append("  Väljundeid pole").append(lineSeparator);
                }

                for (int index = 0; index < source.outlets().size(); index++) {
                    PowerOutlet outlet = source.outlets().get(index);
                    appendOutletReport(builder, source, outlet, index, lineSeparator);
                }
                builder.append(lineSeparator);
            }
            appendUnconnectedTentsReport(builder, lineSeparator);
        }

        if (showCableSummary()) {
            appendCableReport(builder, lineSeparator);
        }

        if (showGroupSummary()) {
            appendGroupReport(builder, lineSeparator);
        }
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

        builder.append("Ühendamata telgid").append(lineSeparator);
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
        double totalNotedLengthMeters = 0.0;
        boolean hasNotedLength = false;
        List<CableSummaryRow> cableRows = new ArrayList<>();
        Map<ConnectorType, CableTypeSummary> summariesByType = new EnumMap<>(ConnectorType.class);
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

            double lengthMeters = cableLengthMeters(cablePath(tent, source, connection));
            totalLengthMeters += lengthMeters;
            OptionalDouble notedLengthMeters = notedCableLengthMeters(connection);
            CableTypeSummary typeSummary = summariesByType.computeIfAbsent(
                    connection.connectorType(),
                    ignored -> new CableTypeSummary()
            );
            typeSummary.addMapLength(lengthMeters);
            if (notedLengthMeters.isPresent()) {
                totalNotedLengthMeters += notedLengthMeters.getAsDouble();
                typeSummary.addNotedLength(notedLengthMeters.getAsDouble());
                typeSummary.addPieces(cableLengthPieces(connection));
                hasNotedLength = true;
            }
            cableRows.add(new CableSummaryRow(tent, source, connection, lengthMeters, notedLengthMeters));
        }

        if (cableRows.isEmpty()) {
            return;
        }

        builder.append("Kaablid").append(lineSeparator);
        cableRows.stream()
                .sorted(CABLE_SUMMARY_ROW_COMPARATOR)
                .map(this::cableSummaryRow)
                .forEach(row -> builder.append(row).append(lineSeparator));
        if (hasNotedLength) {
            builder.append("Kokku: %.1f m märgitud, %.1f m kaardil".formatted(totalNotedLengthMeters, totalLengthMeters)).append(lineSeparator);
        } else {
            builder.append("Kokku: %.1f m".formatted(totalLengthMeters)).append(lineSeparator);
        }
        for (String row : cableTypeSummaryRows(summariesByType)) {
            builder.append(row).append(lineSeparator);
        }
    }

    private String cableNotesText(PowerConnection connection) {
        return connection.cableNotes().isBlank() ? "" : " [%s]".formatted(connection.cableNotes());
    }

    private void addCableTypeSummaryRows(List<String> targetRows, Map<ConnectorType, CableTypeSummary> summariesByType) {
        targetRows.addAll(cableTypeSummaryRows(summariesByType));
    }

    private List<String> cableTypeSummaryRows(Map<ConnectorType, CableTypeSummary> summariesByType) {
        List<String> rows = new ArrayList<>();
        if (!summariesByType.isEmpty()) {
            rows.add("Tüübi kaupa:");
        }
        for (ConnectorType connectorType : ConnectorType.values()) {
            CableTypeSummary summary = summariesByType.get(connectorType);
            if (summary == null) {
                continue;
            }
            rows.add(cableTypeSummaryRow(connectorType, summary));
            if (summary.hasPieces()) {
                rows.add("    tükid: %s".formatted(cablePieceCountText(summary.pieceCounts())));
            }
        }
        return rows;
    }

    private String cableTypeSummaryRow(ConnectorType connectorType, CableTypeSummary summary) {
        if (summary.hasNotedLength()) {
            return "  %s: %.1f m märgitud, %.1f m kaardil".formatted(
                    shortCableTypeName(connectorType),
                    summary.notedLengthMeters(),
                    summary.mapLengthMeters()
            );
        }
        return "  %s: %.1f m kaardil".formatted(shortCableTypeName(connectorType), summary.mapLengthMeters());
    }

    private String cableSummaryRow(CableSummaryRow row) {
        String lengthText = row.notedLengthMeters().isPresent()
                ? "%.1f m kaardil, %.1f m märgitud".formatted(row.mapLengthMeters(), row.notedLengthMeters().getAsDouble())
                : "%.1f m".formatted(row.mapLengthMeters());
        return "  - %s -> %s (%s): %s%s".formatted(
                row.tent().name(),
                row.source().name(),
                row.connection().connectorType().displayName(),
                lengthText,
                cableNotesText(row.connection()) + cableNoteWarningText(row.connection())
        );
    }

    private record CableSummaryRow(
            Tent tent,
            PowerSource source,
            PowerConnection connection,
            double mapLengthMeters,
            OptionalDouble notedLengthMeters
    ) {
    }

    private record MeasurementView(Position start, Position end, Label distanceLabel) {
    }

    private String cableNoteWarningText(PowerConnection connection) {
        return cableNoteNeedsReview(connection) ? " (tükid kontrollida)" : "";
    }

    private boolean cableNoteNeedsReview(PowerConnection connection) {
        String notes = connection.cableLengthNotes();
        if (notes.isBlank() || !notes.contains("+")) {
            return false;
        }

        for (String part : notes.split("\\+")) {
            if (!part.isBlank() && !CABLE_LENGTH_PATTERN.matcher(part).find()) {
                return true;
            }
        }
        return false;
    }

    private OptionalDouble notedCableLengthMeters(PowerConnection connection) {
        List<Double> pieces = cableLengthPieces(connection);
        if (pieces.isEmpty()) {
            return OptionalDouble.empty();
        }

        double totalLengthMeters = 0.0;
        for (double piece : pieces) {
            totalLengthMeters += piece;
        }
        return OptionalDouble.of(totalLengthMeters);
    }

    private List<Double> cableLengthPieces(PowerConnection connection) {
        List<Double> pieces = new ArrayList<>();
        Matcher matcher = CABLE_LENGTH_PATTERN.matcher(connection.cableLengthNotes());
        while (matcher.find()) {
            pieces.add(Double.parseDouble(matcher.group().replace(',', '.')));
        }
        return pieces;
    }

    private String cablePieceCountText(Map<Double, Integer> pieceCounts) {
        List<String> rows = new ArrayList<>();
        for (Map.Entry<Double, Integer> entry : pieceCounts.entrySet()) {
            rows.add("%s m x %d".formatted(formatCablePieceLength(entry.getKey()), entry.getValue()));
        }
        return String.join(", ", rows);
    }

    private String formatCablePieceLength(double lengthMeters) {
        if (Math.abs(lengthMeters - Math.rint(lengthMeters)) < 0.0001) {
            return Integer.toString((int) Math.rint(lengthMeters));
        }
        return "%.1f".formatted(lengthMeters);
    }

    private static class CableTypeSummary {
        private double mapLengthMeters;
        private double notedLengthMeters;
        private boolean hasNotedLength;
        private final Map<Double, Integer> pieceCounts = new TreeMap<>();

        void addMapLength(double lengthMeters) {
            mapLengthMeters += lengthMeters;
        }

        void addNotedLength(double lengthMeters) {
            notedLengthMeters += lengthMeters;
            hasNotedLength = true;
        }

        void addPieces(List<Double> pieces) {
            for (double piece : pieces) {
                pieceCounts.merge(piece, 1, Integer::sum);
            }
        }

        double mapLengthMeters() {
            return mapLengthMeters;
        }

        double notedLengthMeters() {
            return notedLengthMeters;
        }

        boolean hasNotedLength() {
            return hasNotedLength;
        }

        boolean hasPieces() {
            return !pieceCounts.isEmpty();
        }

        Map<Double, Integer> pieceCounts() {
            return pieceCounts;
        }
    }

    private void appendGroupReport(StringBuilder builder, String lineSeparator) {
        if (plan.objects().isEmpty()) {
            return;
        }

        builder.append("Grupid").append(lineSeparator);
        for (Map.Entry<String, List<PlannerObject>> entry : objectsByGroup().entrySet()) {
            builder.append(entry.getKey()).append(lineSeparator);
            for (PlannerObject object : entry.getValue()) {
                builder.append("  - ")
                        .append(object.name())
                        .append(" (")
                        .append(objectTypeName(object))
                        .append(")")
                        .append(lineSeparator);
            }
        }
        builder.append(lineSeparator);
    }

    private void loadMapImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Vali kaardipilt");
        applyInitialDirectory(fileChooser);
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Pildifail", "*.png", "*.jpg", "*.jpeg"));
        File file = fileChooser.showOpenDialog(stage);
        if (file == null) {
            return;
        }

        rememberDirectory(file);
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
        applyInitialDirectory(fileChooser);
        File file = fileChooser.showOpenDialog(stage);
        if (file == null) {
            return;
        }

        try {
            plan = planFileService.load(file.toPath());
            currentPlanFile = file;
            rememberDirectory(file);
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

    private void applyInitialDirectory(FileChooser fileChooser) {
        File directory = lastUsedDirectory;
        if (directory == null && currentPlanFile != null) {
            directory = currentPlanFile.getParentFile();
        }
        if (directory != null && directory.isDirectory()) {
            fileChooser.setInitialDirectory(directory);
        }
    }

    private void rememberDirectory(File file) {
        if (file != null && file.getParentFile() != null && file.getParentFile().isDirectory()) {
            lastUsedDirectory = file.getParentFile();
        }
    }

    private String exportSummaryFileName() {
        String baseName = currentPlanFile == null
                ? plan.name()
                : currentPlanFile.getName().replaceFirst("\\.pplan$", "");
        String safeName = baseName.trim()
                .replaceAll("[\\\\/:*?\"<>|]+", "-")
                .replaceAll("\\s+", "-")
                .toLowerCase();
        if (safeName.isBlank()) {
            safeName = "pannkoogihommiku-kokkuvõte";
        }
        return safeName + "-kokkuvõte.txt";
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

    private record PlacementDetails(
            String name,
            String groupName,
            String colorHex,
            double widthMeters,
            double heightMeters,
            CustomObjectShape shape,
            MarkerType markerType
    ) {
    }

    private enum PlacementType {
        TENT("Telk", "Uus telk", "#e74c3c", true),
        POWER_SOURCE("Elektrikapp", "Uus kapp", "#2563eb", false),
        CUSTOM_OBJECT("Objekt", "Uus objekt", "#9ca3af", true),
        TEXT_OBJECT("Tekst", "Uus tekst", "#111827", true),
        MARKER_OBJECT("Marker", "Uus marker", "#0f766e", true);

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

        private String defaultName() {
            return defaultName;
        }

        private String defaultColorHex() {
            return defaultColorHex;
        }

        private boolean hasConfigurableColor() {
            return configurableColor;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private static class Delta {
        private double x;
        private double y;
    }
}
