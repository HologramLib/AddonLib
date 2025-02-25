/*
 * AddonLib - An addon management library for Minecraft plugins.
 * Copyright (c) 2025. Maxim.jsx
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Source: <https://github.com/HologramLib/AddonLib>
 * contact@maximjsx.com
 */

import com.maximjsx.addonlib.config.AddonConfig;
import com.maximjsx.addonlib.model.AddonEntry;
import com.maximjsx.addonlib.model.AddonRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConfigTest {

    @TempDir
    File tempDir;

    private AddonConfig config;

    @BeforeEach
    void setUp() {
        config = new AddonConfig(tempDir);
    }

    @Test
    void testConfigFileCreation() {
        File configFile = new File(tempDir, "addons.json");
        assertTrue(configFile.exists(), "Config file should be created");
    }

    @Test
    void testSetAddonEnabled() {
        String addonName = "test-addon";

        config.setAddonEnabled(addonName, true);

        assertTrue(config.getAddonEntries().containsKey(addonName), "Addon should be added to entries");
        assertTrue(config.getAddonEntries().get(addonName).isEnabled(), "Addon should be enabled");
    }

    @Test
    void testSaveAndLoadAddonEntry() {
        String addonName = "test-addon";
        AddonEntry entry = new AddonEntry();
        entry.setEnabled(true);
        entry.setInstalledVersion("1.0.0");
        entry.setDescription("Test Description");
        config.saveAddonEntry(addonName, entry);

        AddonConfig newConfig = new AddonConfig(tempDir);

        assertTrue(newConfig.getAddonEntries().containsKey(addonName), "Addon should be loaded");
        AddonEntry loadedEntry = newConfig.getAddonEntries().get(addonName);
        assertEquals(entry.isEnabled(), loadedEntry.isEnabled());
        assertEquals(entry.getInstalledVersion(), loadedEntry.getInstalledVersion());
        assertEquals(entry.getDescription(), loadedEntry.getDescription());
    }

    @Test
    void testMergeNewAddons() {
        Map<String, AddonRegistry.AddonInfo> registryAddons = new HashMap<>();
        AddonRegistry.AddonInfo info = new AddonRegistry.AddonInfo();
        info.setDescription("Test Description");
        registryAddons.put("test-addon", info);

        config.mergeNewAddons(registryAddons);

        assertTrue(config.getAddonEntries().containsKey("test-addon"), "New addon should be added");
        AddonEntry entry = config.getAddonEntries().get("test-addon");
        assertFalse(entry.isEnabled(), "New addons should be disabled by default");
        assertEquals("Test Description", entry.getDescription(), "Description should be set");
    }
}
