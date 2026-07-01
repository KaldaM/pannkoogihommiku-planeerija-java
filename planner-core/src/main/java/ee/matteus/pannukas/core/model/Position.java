package ee.matteus.pannukas.core.model;

public record Position(double x, double y) {
    public Position moveTo(double newX, double newY) {
        return new Position(newX, newY);
    }
}
