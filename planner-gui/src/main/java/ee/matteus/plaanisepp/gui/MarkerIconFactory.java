package ee.matteus.plaanisepp.gui;

import ee.matteus.plaanisepp.core.model.MarkerType;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;

import java.util.List;

final class MarkerIconFactory {
    private static final double ICON_SIZE = 28;

    private MarkerIconFactory() {
    }

    static Pane create(MarkerType markerType) {
        Pane icon = new Pane();
        icon.setMinSize(ICON_SIZE, ICON_SIZE);
        icon.setPrefSize(ICON_SIZE, ICON_SIZE);
        icon.setMaxSize(ICON_SIZE, ICON_SIZE);
        icon.getChildren().addAll(switch (markerType) {
            case WC -> wcIcon();
            case SECURITY -> securityIcon();
            case INFO -> infoIcon();
            case START_FINISH -> startFinishIcon();
            case SAUNA -> saunaIcon();
            case MEMBER -> memberIcon();
        });
        return icon;
    }

    private static List<Node> wcIcon() {
        Circle leftHead = new Circle(10, 8, 3, Color.WHITE);
        Circle rightHead = new Circle(18, 8, 3, Color.WHITE);
        Rectangle leftBody = new Rectangle(7, 12, 6, 10);
        Rectangle rightBody = new Rectangle(15, 12, 6, 10);
        leftBody.setFill(Color.WHITE);
        rightBody.setFill(Color.WHITE);
        return List.of(leftHead, rightHead, leftBody, rightBody);
    }

    private static List<Node> securityIcon() {
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

    private static List<Node> infoIcon() {
        Circle circle = new Circle(14, 14, 9, Color.WHITE);
        Label label = new Label("i");
        label.setTextFill(Color.web("#111827"));
        label.setStyle("-fx-font-weight: bold; -fx-font-size: 16;");
        label.setLayoutX(12);
        label.setLayoutY(4);
        return List.of(circle, label);
    }

    private static List<Node> startFinishIcon() {
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

    private static List<Node> saunaIcon() {
        Polygon roof = new Polygon(
                6.0, 14.0,
                14.0, 7.0,
                22.0, 14.0
        );
        roof.setFill(Color.WHITE);
        Rectangle house = new Rectangle(8, 14, 12, 8);
        house.setFill(Color.WHITE);
        Line steamOne = new Line(10, 5, 10, 10);
        Line steamTwo = new Line(14, 4, 14, 9);
        Line steamThree = new Line(18, 5, 18, 10);
        for (Line steam : List.of(steamOne, steamTwo, steamThree)) {
            steam.setStroke(Color.WHITE);
            steam.setStrokeWidth(1.5);
        }
        return List.of(steamOne, steamTwo, steamThree, roof, house);
    }

    private static List<Node> memberIcon() {
        Circle head = new Circle(14, 8, 4, Color.WHITE);
        Rectangle body = new Rectangle(9, 14, 10, 9);
        body.setArcWidth(6);
        body.setArcHeight(6);
        body.setFill(Color.WHITE);
        return List.of(head, body);
    }
}
