package ee.matteus.pannukas.core.model;

public interface PowerConsumer {
    String id();

    String name();

    int requiredWatts();
}
