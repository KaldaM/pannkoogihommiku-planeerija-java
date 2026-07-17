package ee.matteus.pannukas.core.model;

public abstract class PlannerObject {
    private final String id;
    private String name;
    private Position position;
    private boolean locked;
    private String groupName;
    private String notes;
    private boolean showMapLabel;

    protected PlannerObject(String id, String name, Position position) {
        this.id = id;
        this.name = name;
        this.position = position;
        this.groupName = "";
        this.notes = "";
        this.showMapLabel = true;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public void rename(String name) {
        this.name = name;
    }

    public Position position() {
        return position;
    }

    public void moveTo(Position position) {
        if (!locked) {
            this.position = position;
        }
    }

    public boolean locked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public String groupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName == null ? "" : groupName;
    }

    public String notes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes == null ? "" : notes;
    }

    public boolean showMapLabel() {
        return showMapLabel;
    }

    public void setShowMapLabel(boolean showMapLabel) {
        this.showMapLabel = showMapLabel;
    }
}
