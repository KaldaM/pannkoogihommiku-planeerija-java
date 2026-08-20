package ee.matteus.plaanisepp.gui;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StartupPlanFileResolverTest {
    @Test
    void returnsEmptyWhenNoFileArgumentWasGiven() {
        assertTrue(StartupPlanFileResolver.resolve(List.of()).isEmpty());
        assertTrue(StartupPlanFileResolver.resolve(List.of(" ")).isEmpty());
    }

    @Test
    void resolvesRelativePlanPathToNormalizedAbsolutePath() {
        Path resolved = StartupPlanFileResolver.resolve(List.of("plans/../test plan.pplan")).orElseThrow();

        assertEquals(Path.of("test plan.pplan").toAbsolutePath().normalize(), resolved);
    }

    @Test
    void usesOnlyTheFirstFileArgument() {
        Path resolved = StartupPlanFileResolver.resolve(List.of("first.pplan", "second.pplan")).orElseThrow();

        assertEquals(Path.of("first.pplan").toAbsolutePath().normalize(), resolved);
    }
}
