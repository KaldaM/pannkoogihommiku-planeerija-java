package ee.matteus.pannukas.gui;

enum MapImageExportScope {
    FULL_MAP("Kogu kaart"),
    CURRENT_VIEW("Praegune vaade");

    private final String label;

    MapImageExportScope(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
