package ee.matteus.pannukas.gui;

import java.io.File;

final class ExportFileNames {
    private static final String DEFAULT_BASE_NAME = "pannkoogihommik";

    private ExportFileNames() {
    }

    static File ensurePngExtension(File file) {
        return ensureExtension(file, ".png");
    }

    static File ensurePdfExtension(File file) {
        return ensureExtension(file, ".pdf");
    }

    static File ensureTxtExtension(File file) {
        return ensureExtension(file, ".txt");
    }

    static String mapImageFileName(String planName, File currentPlanFile) {
        return safeExportBaseName(planName, currentPlanFile) + "-kaart.png";
    }

    static String pdfFileName(String planName, File currentPlanFile) {
        return safeExportBaseName(planName, currentPlanFile) + "-plaan.pdf";
    }

    static String summaryFileName(String planName, File currentPlanFile) {
        return safeExportBaseName(planName, currentPlanFile) + "-kokkuvõte.txt";
    }

    private static File ensureExtension(File file, String extension) {
        if (file.getName().toLowerCase().endsWith(extension)) {
            return file;
        }
        return new File(file.getParentFile(), file.getName() + extension);
    }

    private static String safeExportBaseName(String planName, File currentPlanFile) {
        String baseName = currentPlanFile == null
                ? planName
                : currentPlanFile.getName().replaceFirst("\\.pplan$", "");
        String safeName = baseName.trim()
                .replaceAll("[\\\\/:*?\"<>|]+", "-")
                .replaceAll("\\s+", "-")
                .toLowerCase();
        if (safeName.isBlank()) {
            return DEFAULT_BASE_NAME;
        }
        return safeName;
    }
}
