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

package com.maximjsx.addonlib.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.maximjsx.addonlib.model.AddonEntry;
import com.maximjsx.addonlib.model.AddonRegistry;
import lombok.Data;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Data
public class AddonConfig {
    private final File configFile;
    private ConfigData configData;
    private final Map<String, AddonEntry> addonEntries = new HashMap<>();
    private boolean autoUpgrade;

    @Data
    private static class ConfigData {
        private Map<String, AddonEntry> addons = new HashMap<>();
        private Settings settings = new Settings();
    }

    @Data
    private static class Settings {
        private boolean autoUpgrade = false;
    }

    public AddonConfig(File dataFolder) {
        this.configFile = new File(dataFolder, "addons.json");
        loadConfig();
    }

    public void loadConfig() {
        if (!configFile.exists()) {
            createDefaultConfig();
        }

        try (FileReader reader = new FileReader(configFile)) {
            Gson gson = new Gson();
            configData = gson.fromJson(reader, ConfigData.class);

            if (configData.addons != null) {
                addonEntries.putAll(configData.addons);
            }

            autoUpgrade = configData.settings.autoUpgrade;
        } catch (IOException e) {
            e.printStackTrace();
            createDefaultConfig();
        }
    }

    private void createDefaultConfig() {
        configData = new ConfigData();
        configData.settings.autoUpgrade = false;
        saveConfig();
    }

    public void saveConfig() {
        try (FileWriter writer = new FileWriter(configFile)) {
            configData.addons = addonEntries;
            configData.settings.autoUpgrade = autoUpgrade;

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(configData, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setAddonEnabled(String addonName, boolean enabled) {
        AddonEntry entry = addonEntries.computeIfAbsent(addonName, k -> new AddonEntry());
        entry.setEnabled(enabled);
        saveConfig();
    }

    public void saveAddonEntry(String addonName, AddonEntry entry) {
        addonEntries.put(addonName, entry);
        saveConfig();
    }

    public void mergeNewAddons(Map<String, AddonRegistry.AddonInfo> registryAddons) {
        for (String addonName : registryAddons.keySet()) {
            AddonRegistry.AddonInfo addonInfo = registryAddons.get(addonName);
            if (!addonEntries.containsKey(addonName)) {
                AddonEntry newEntry = new AddonEntry();
                newEntry.setEnabled(false);
                newEntry.setDescription(addonInfo.getDescription());
                addonEntries.put(addonName, newEntry);
                saveAddonEntry(addonName, newEntry);
            }
        }
    }
}