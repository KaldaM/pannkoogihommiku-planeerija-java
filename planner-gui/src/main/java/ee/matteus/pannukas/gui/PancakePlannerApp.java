package ee.matteus.pannukas.gui;

import ee.matteus.pannukas.core.model.EventPlan;
import ee.matteus.pannukas.core.model.PlannerObject;
import ee.matteus.pannukas.core.model.PowerSource;
import ee.matteus.pannukas.core.model.Tent;
import ee.matteus.pannukas.core.service.PlanFileService;
import ee.matteus.pannukas.core.service.PlanFactory;
import ee.matteus.pannukas.core.service.PowerSummary;
import ee.matteus.pannukas.core.service.PowerSummaryService;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;

public class PancakePlannerApp extends Application {
    private final PlanFactory planFactory = new PlanFactory();
    private final PowerSummaryService powerSummaryService = new PowerSummaryService();
    private final PlanFileService planFileService = new PlanFileService();
    private EventPlan plan;
    private Pane mapPane;
    private ListView<String> summaryList;
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

        return new ToolBar(addTentButton, addPowerSourceButton, saveButton, openButton);
    }

    private SplitPane createContent() {
        mapPane = new Pane();
        mapPane.setMinSize(760, 560);
        mapPane.setStyle("-fx-background-color: #eef1ec;");

        BorderPane sidebar = new BorderPane();
        sidebar.setPadding(new Insets(12));
        summaryList = new ListView<>();
        sidebar.setTop(new Label("Voolu kokkuvõte"));
        sidebar.setCenter(summaryList);

        SplitPane splitPane = new SplitPane(mapPane, sidebar);
        splitPane.setDividerPositions(0.72);
        return splitPane;
    }

    private void addTent() {
        Tent tent = new Tent(planFactory.newId(), "Uus telk", new ee.matteus.pannukas.core.model.Position(120, 120));
        tent.setGroupName("Määramata");
        plan.addObject(tent);
        redrawMap();
        refreshSummary();
    }

    private void addPowerSource() {
        PowerSource source = new PowerSource(planFactory.newId(), "Uus kapp", new ee.matteus.pannukas.core.model.Position(160, 160));
        source.addOutlet(new ee.matteus.pannukas.core.model.PowerOutlet(planFactory.newId(), ee.matteus.pannukas.core.model.ConnectorType.SCHUKO_230V, 11000));
        plan.addObject(source);
        redrawMap();
        refreshSummary();
    }

    private void redrawMap() {
        mapPane.getChildren().clear();
        for (PlannerObject object : plan.objects()) {
            if (object instanceof Tent tent) {
                drawTent(tent);
            } else if (object instanceof PowerSource source) {
                drawPowerSource(source);
            }
        }
    }

    private void drawTent(Tent tent) {
        Rectangle rectangle = new Rectangle(tent.position().x(), tent.position().y(), 72, 72);
        rectangle.setArcWidth(4);
        rectangle.setArcHeight(4);
        rectangle.setFill(Color.web(tent.colorHex()));
        rectangle.setStroke(Color.web("#222222"));
        makeDraggable(rectangle, tent);

        Label label = new Label(tent.name());
        label.setLayoutX(tent.position().x());
        label.setLayoutY(tent.position().y() - 24);

        mapPane.getChildren().addAll(rectangle, label);
    }

    private void drawPowerSource(PowerSource source) {
        Circle circle = new Circle(source.position().x(), source.position().y(), 12);
        circle.setFill(Color.web("#2563eb"));
        circle.setStroke(Color.web("#111827"));
        makeDraggable(circle, source);

        Label label = new Label(source.name());
        label.setLayoutX(source.position().x() + 16);
        label.setLayoutY(source.position().y() - 12);

        mapPane.getChildren().addAll(circle, label);
    }

    private void makeDraggable(javafx.scene.Node node, PlannerObject object) {
        final Delta dragDelta = new Delta();
        node.setOnMousePressed(event -> {
            dragDelta.x = event.getSceneX() - object.position().x();
            dragDelta.y = event.getSceneY() - object.position().y();
        });
        node.setOnMouseDragged(event -> {
            object.moveTo(object.position().moveTo(event.getSceneX() - dragDelta.x, event.getSceneY() - dragDelta.y));
            redrawMap();
            refreshSummary();
        });
    }

    private void refreshSummary() {
        summaryList.getItems().clear();
        for (PowerSummary summary : powerSummaryService.summaries(plan)) {
            summaryList.getItems().add("%s: %d W kasutusel, %d W alles".formatted(
                    summary.sourceName(),
                    summary.usedWatts(),
                    summary.remainingWatts()
            ));
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

    private void openPlan() {
        FileChooser fileChooser = createPlanFileChooser();
        File file = fileChooser.showOpenDialog(stage);
        if (file == null) {
            return;
        }

        try {
            plan = planFileService.load(file.toPath());
            redrawMap();
            refreshSummary();
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

    private static class Delta {
        private double x;
        private double y;
    }
}
