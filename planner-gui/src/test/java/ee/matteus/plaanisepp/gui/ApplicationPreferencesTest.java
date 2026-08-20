package ee.matteus.plaanisepp.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApplicationPreferencesTest {
    @Test
    void keepsLegacyPreferencesNodeAfterPackageRename() {
        assertEquals("/ee/matteus/pannukas/gui", ApplicationPreferences.open().absolutePath());
    }
}
