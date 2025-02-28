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
import com.maximjsx.addonlib.model.Registry;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

@Data
@Accessors(chain = true)
public class AddonConfig {
    private final File configFile;
    private ConfigData configData;
    private final Map<String, AddonEntry> addonEntries = new HashMap<>();
    private final String[] enabledByDefault;
    private boolean autoUpgrade;

    @Data
    @Accessors(chain = true)
    private static class ConfigData {
        private Map<String, AddonEntry> addons = new HashMap<>();
        private Settings settings = new Settings();
    }

    @Data
    @Accessors(chain = true)
    private static class Settings {
        private boolean autoUpgrade = false;
    }

    /**
     * Creates an AddonConfig with no addons enabled by default
     * @param dataFolder folder to store configuration
     */
    public AddonConfig(File dataFolder) {
        this(dataFolder, null);
    }

    /**
     * Creates an AddonConfig with specified addons enabled by default
     * @param dataFolder folder to store configuration
     * @param enabledAddons array of addon names to enable by default (if null, none will be enabled)
     */
    public AddonConfig(File dataFolder, String[] enabledAddons) {
        ensureDirectoryExists(dataFolder);
        this.configFile = new File(dataFolder, "addons.json");
        this.enabledByDefault = enabledAddons != null ? enabledAddons : new String[0];
        loadConfig();
    }

    /**
     * Creates directory if it doesn't exist
     * @param directory directory to create
     */
    private void ensureDirectoryExists(File directory) {
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    /**
     * Loads configuration from file, creating default if needed
     */
    public void loadConfig() {
        boolean isNewConfig = !configFile.exists();

        if (isNewConfig) {
            createDefaultConfig();
        }

        try (FileReader reader = new FileReader(configFile)) {
            Gson gson = new Gson();
            configData = gson.fromJson(reader, ConfigData.class);

            Optional.ofNullable(configData.addons)
                    .ifPresent(addonEntries::putAll);

            if (isNewConfig && enabledByDefault.length > 0) {
                Arrays.stream(enabledByDefault)
                        .forEach(addonName -> {
                            AddonEntry entry = addonEntries.computeIfAbsent(addonName, k -> new AddonEntry());
                            entry.setEnabled(true);
                        });
                saveConfig();
            }

            autoUpgrade = configData.settings.autoUpgrade;
        } catch (IOException exception) {
            exception.printStackTrace();
            createDefaultConfig();
        }
    }

    /**
     * Creates default configuration
     */
    private void createDefaultConfig() {
        configData = new ConfigData();
        configData.settings.autoUpgrade = false;
        saveConfig();
    }

    /**
     * Saves current configuration to disk
     */
    public void saveConfig() {
        try {
            ensureDirectoryExists(configFile.getParentFile());
            ensureFileExists(configFile);

            try (FileWriter writer = new FileWriter(configFile)) {
                configData.addons = addonEntries;
                configData.settings.autoUpgrade = autoUpgrade;

                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                gson.toJson(configData, writer);
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    /**
     * Ensures file exists, creating it if needed
     * @param file file to check/create
     * @throws IOException if file creation fails
     */
    private void ensureFileExists(File file) throws IOException {
        if (!file.exists()) {
            Files.createFile(file.toPath());
        }
    }

    /**
     * Sets an addon's enabled status
     * @param addonName name of the addon
     * @param enabled whether the addon should be enabled
     */
    public void setAddonEnabled(String addonName, boolean enabled) {
        updateAddon(addonName, entry -> entry.setEnabled(enabled));
    }

    /**
     * Updates an addon entry and saves the configuration
     * @param addonName name of the addon to update
     * @param entry new entry data
     */
    public void saveAddonEntry(String addonName, AddonEntry entry) {
        addonEntries.put(addonName, entry);
        saveConfig();
    }

    /**
     * Updates an addon using a consumer function
     * @param addonName name of the addon to update
     * @param updater function to apply to the addon entry
     */
    private void updateAddon(String addonName, Consumer<AddonEntry> updater) {
        AddonEntry entry = addonEntries.computeIfAbsent(addonName, k -> new AddonEntry());
        updater.accept(entry);
        saveConfig();
    }

    /**
     * Adds new addons from registry to configuration if they don't already exist
     * @param registryAddons map of addons from registry
     */
    public void mergeNewAddons(Map<String, Registry.AddonInfo> registryAddons) {
        registryAddons.forEach((addonName, addonInfo) -> {
            if (!addonEntries.containsKey(addonName)) {
                AddonEntry newEntry = new AddonEntry()
                        .setEnabled(false)
                        .setDescription(addonInfo.getDescription());
                saveAddonEntry(addonName, newEntry);
            }
        });
    }
}