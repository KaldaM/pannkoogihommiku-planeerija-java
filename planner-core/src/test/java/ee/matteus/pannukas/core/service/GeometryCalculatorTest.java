package ee.matteus.pannukas.core.service;

import ee.matteus.pannukas.core.model.CustomObject;
import ee.matteus.pannukas.core.model.CustomObjectShape;
import ee.matteus.pannukas.core.model.Position;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GeometryCalculatorTest {
    private static final double TOLERANCE = 0.0001;

    @Test
    void calculatesPolylineLengthUsingMapScale() {
        List<Position> points = List.of(
                new Position(0, 0),
                new Position(30, 40),
                new Position(60, 40)
        );

        assertEquals(8.0, GeometryCalculator.lineLengthMeters(points, 10.0), TOLERANCE);
    }

    @Test
    void calculatesPolygonAreaAndPerimeterUsingMapScale() {
        List<Position> points = List.of(
                new Position(0, 0),
                new Position(40, 0),
                new Position(40, 30),
                new Position(0, 30)
        );

        assertEquals(12.0, GeometryCalculator.polygonAreaSquareMeters(points, 10.0), TOLERANCE);
        assertEquals(14.0, GeometryCalculator.polygonPerimeterMeters(points, 10.0), TOLERANCE);
    }

    @Test
    void calculatesRectangleAreaAndPerimeter() {
        CustomObject rectangle = new CustomObject("rectangle", "Ristkülik", new Position(0, 0));
        rectangle.setSizeMeters(4.0, 3.0);

        assertEquals(12.0, GeometryCalculator.customObjectAreaSquareMeters(rectangle), TOLERANCE);
        assertEquals(14.0, GeometryCalculator.customObjectPerimeterMeters(rectangle), TOLERANCE);
    }

    @Test
    void calculatesCircleAreaAndCircumferenceFromDiameter() {
        CustomObject circle = new CustomObject("circle", "Ring", new Position(0, 0));
        circle.setShape(CustomObjectShape.CIRCLE);
        circle.setSizeMeters(4.0, 4.0);

        assertEquals(4.0 * Math.PI, GeometryCalculator.customObjectAreaSquareMeters(circle), TOLERANCE);
        assertEquals(4.0 * Math.PI, GeometryCalculator.customObjectPerimeterMeters(circle), TOLERANCE);
    }
}
