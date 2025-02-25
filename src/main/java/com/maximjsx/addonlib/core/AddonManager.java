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

package com.maximjsx.addonlib.core;

import com.google.gson.Gson;
import com.maximjsx.addonlib.config.AddonConfig;
import com.maximjsx.addonlib.model.AddonEntry;
import com.maximjsx.addonlib.model.Registry;
import com.maximjsx.addonlib.util.Logger;
import com.maximjsx.addonlib.util.VersionUtils;

import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;

public class AddonManager {
    private String PRIMARY_REGISTRY_URL = "";
    private String BACKUP_REGISTRY_URL = "";

    private final Logger logger;
    private final AddonConfig config;
    private Registry registry;
    private final File folder;
    private final String currentVersion;

    public AddonManager(Logger logger, File folder, String version, AddonConfig config, String url, String backupUrl) {
        this.PRIMARY_REGISTRY_URL = url;
        this.BACKUP_REGISTRY_URL = backupUrl;
        this.logger = logger;
        this.folder = folder;
        this.currentVersion = version;
        this.config = config;
        this.checkAndUpdateAddons(config.isAutoUpgrade());
    }

    public AddonManager(Logger logger, File folder, String version, AddonConfig config) {
        this(logger, folder, version, config, "https://cdn.maximjsx.com/hologramlib/registry.json", "https://raw.githubusercontent.com/HologramLib/Addons/main/registry.json");
    }

    public void loadRegistry() {
        try {
            String registryJson = fetchUrl(PRIMARY_REGISTRY_URL);
            if (registryJson == null) {
                registryJson = fetchUrl(BACKUP_REGISTRY_URL);
            }
            if (registryJson != null) {
                registry = new Gson().fromJson(registryJson, Registry.class);
            }
            if (registry != null) {
                config.mergeNewAddons(registry.getAddons());
            }
        } catch (Exception e) {
            this.logger.warning("Failed to load addon registry: " + e.getMessage());
        }
    }

    public void checkAndUpdateAddons(boolean upgrade) {
        loadRegistry();
        if (registry == null) {
            this.logger.warning("Cannot check addons - registry not loaded");
            return;
        }

        for (Map.Entry<String, Registry.AddonInfo> entry : registry.getAddons().entrySet()) {
            String addonName = entry.getKey();
            Registry.AddonInfo addonInfo = entry.getValue();
            AddonEntry configEntry = config.getAddonEntries().get(addonName);

            if (configEntry == null || !configEntry.isEnabled()) continue;

            String latestVersion = findLatestCompatibleVersion(addonInfo.getVersions());
            if (latestVersion == null) {
                if (upgrade) {
                    config.getAddonEntries().remove(addonName);
                    this.logger.warning("Removing incompatible addon: " + addonName);
                }
                continue;
            }

            if (upgrade || !latestVersion.equals(configEntry.getInstalledVersion())) {
                installAddon(addonName, latestVersion);
                configEntry.setInstalledVersion(latestVersion);
                configEntry.setDescription(addonInfo.getDescription());
                config.saveAddonEntry(addonName, configEntry);
            }
        }
        removeDisabledAddons();
    }

    private void removeDisabledAddons() {
        for (Map.Entry<String, AddonEntry> entry : config.getAddonEntries().entrySet()) {
            if (!entry.getValue().isEnabled()) {
                File jar = new File(this.folder.getParentFile(),
                        entry.getKey() + "-" + entry.getValue().getInstalledVersion() + ".jar");
                if (jar.exists()) jar.delete();
            }
        }
    }

    private String findLatestCompatibleVersion(Map<String, String> versions) {
        return versions.entrySet().stream()
                .filter(e -> VersionUtils.isVersionCompatible(e.getValue(), currentVersion))
                .map(Map.Entry::getKey)
                .max(VersionUtils::compareVersions)
                .orElse(null);
    }

    private void installAddon(String addonName, String version) {
        String downloadUrl = String.format("%s%s/releases/download/%s/%s-%s.jar",
                registry.getBaseURL(), addonName, version, addonName, version);

        File addonFile = new File(this.folder.getParentFile(), addonName + "-" + version + ".jar");

        try {
            downloadFile(downloadUrl, addonFile);
            this.logger.info("Successfully installed " + addonName + " v" + version);
        } catch (IOException e) {
            this.logger.warning("Failed to install " + addonName + ": " + e.getMessage());
        }
    }

    private String fetchUrl(String urlStr) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new URL(urlStr).openStream()))) {
            StringBuilder buffer = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }
            return buffer.toString();
        } catch (Exception ignore) {}
        return null;
    }

    private void downloadFile(String urlStr, File file) throws IOException {
        File tempFile = new File(file.getParentFile(), file.getName() + ".tmp");
        try (
                ReadableByteChannel readChannel = Channels.newChannel(new URL(urlStr).openStream());
                FileOutputStream fileOS = new FileOutputStream(tempFile);
                FileChannel writeChannel = fileOS.getChannel()
        ) {
            writeChannel.transferFrom(readChannel, 0, Long.MAX_VALUE);
        }
        Files.move(tempFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
}