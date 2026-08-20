package ee.matteus.plaanisepp.gui;

import java.io.File;
import java.util.Locale;

final class PlanFileNames {
    private static final String PLAN_EXTENSION = ".pplan";

    private PlanFileNames() {
    }

    static File ensurePlanExtension(File file) {
        String fileName = file.getName();
        String lowerCaseName = fileName.toLowerCase(Locale.ROOT);
        if (lowerCaseName.endsWith(PLAN_EXTENSION)) {
            return file;
        }
        if (lowerCaseName.endsWith(".zip")) {
            fileName = fileName.substring(0, fileName.length() - ".zip".length());
        }
        return new File(file.getParentFile(), fileName + PLAN_EXTENSION);
    }
}
