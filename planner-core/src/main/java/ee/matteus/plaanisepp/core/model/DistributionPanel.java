package ee.matteus.plaanisepp.core.model;

public class DistributionPanel extends PowerSource implements PowerConnectable {
    private Position powerConnectionOffset = new Position(0, 0);

    public DistributionPanel(String id, String name, Position position) {
        super(id, name, position);
    }

    @Override
    public int requiredWatts() {
        return 0;
    }

    @Override
    public Position powerConnectionOffset() {
        return powerConnectionOffset;
    }

    @Override
    public void setPowerConnectionOffset(Position offset) {
        powerConnectionOffset = offset == null ? new Position(0, 0) : offset;
    }
}
