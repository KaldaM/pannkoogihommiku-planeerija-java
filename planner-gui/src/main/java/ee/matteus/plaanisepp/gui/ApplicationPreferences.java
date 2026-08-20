package ee.matteus.plaanisepp.gui;

import java.util.prefs.Preferences;

final class ApplicationPreferences {
    static final String LEGACY_NODE_PATH = "/ee/matteus/pannukas/gui";

    private ApplicationPreferences() {
    }

    static Preferences open() {
        return Preferences.userRoot().node(LEGACY_NODE_PATH);
    }
}
