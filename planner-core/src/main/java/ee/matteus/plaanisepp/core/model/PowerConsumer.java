package ee.matteus.plaanisepp.core.model;

public interface PowerConsumer {
    String id();

    String name();

    int requiredWatts();
}
