package ee.matteus.plaanisepp.gui;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

final class StartupPlanFileResolver {
    private StartupPlanFileResolver() {
    }

    static Optional<Path> resolve(List<String> arguments) {
        if (arguments == null || arguments.isEmpty()) {
            return Optional.empty();
        }

        String fileName = arguments.getFirst();
        if (fileName == null || fileName.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(Path.of(fileName).toAbsolutePath().normalize());
    }
}
