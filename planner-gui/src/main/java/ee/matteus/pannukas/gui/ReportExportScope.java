package ee.matteus.pannukas.gui;

enum ReportExportScope {
    COMPACT("Lühike raport"),
    FULL("Täielik raport");

    private final String label;

    ReportExportScope(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
