package ee.matteus.pannukas.core.model;

public interface PowerConnectable extends PowerConsumer {
    Position powerConnectionOffset();

    void setPowerConnectionOffset(Position offset);

    default void resetPowerConnectionOffset() {
        setPowerConnectionOffset(new Position(0, 0));
    }
}
