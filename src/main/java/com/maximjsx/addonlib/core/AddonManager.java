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
import java.net.URI;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class AddonManager {
    private final String primaryRegistryUrl;
    private final String backupRegistryUrl;

    private final Logger logger;
    private final AddonConfig config;
    private Registry registry;
    private final File pluginsFolder;
    private final String currentVersion;

    /**
     * Creates a new AddonManager
     * @param logger Logger for addon operations
     * @param folder The plugins folder
     * @param version Current plugin version
     * @param config Addon configuration
     * @param primaryUrl Primary registry URL
     * @param backupUrl Backup registry URL
     */
    public AddonManager(Logger logger, File folder, String version, AddonConfig config,
                        String primaryUrl, String backupUrl) {
        this.primaryRegistryUrl = Objects.requireNonNull(primaryUrl, "Primary URL cannot be null");
        this.backupRegistryUrl = Objects.requireNonNull(backupUrl, "Backup URL cannot be null");
        this.logger = Objects.requireNonNull(logger, "Logger cannot be null");
        this.pluginsFolder = Objects.requireNonNull(folder.getParentFile(), "Plugins folder cannot be null");
        this.currentVersion = Objects.requireNonNull(version, "Version cannot be null");
        this.config = Objects.requireNonNull(config, "Configuration cannot be null");
    }

    /**
     * Loads the addon registry from primary or backup URL
     */
    public void loadRegistry() {
        try {
            String registryJson = Optional.ofNullable(fetchUrl(primaryRegistryUrl))
                    .orElse(fetchUrl(backupRegistryUrl));

            if (registryJson != null) {
                registry = new Gson().fromJson(registryJson, Registry.class);

                if (registry != null) {
                    config.mergeNewAddons(registry.getAddons());
                }
            }
        } catch (Exception e) {
            this.logger.warning("Failed to load addon registry: " + e.getMessage());
        }
    }

    /**
     * Checks for addon updates and installs or updates enabled addons
     * @param upgrade whether to upgrade addons to newer versions if available
     */
    public void checkAndUpdateAddons(boolean upgrade) {
        loadRegistry();

        if (registry == null) {
            this.logger.warning("Cannot check addons - registry not loaded");
            return;
        }

        registry.getAddons().forEach((addonName, addonInfo) ->
                processRegistryAddon(addonName, addonInfo, upgrade));

        config.getAddonEntries().forEach((addonName, configEntry) -> {
            if (configEntry.isEnabled() && !registry.getAddons().containsKey(addonName)) {
                this.logger.warning("Addon " + addonName + " no longer exists in registry. Disabling.");
                configEntry.setEnabled(false);
                config.saveAddonEntry(addonName, configEntry);
                removeAddonJar(addonName, configEntry.getInstalledVersion());
            }
        });

        cleanupAddonJars();

        /* Install missing jar files */
        config.getAddonEntries().forEach((addonName, configEntry) -> {
            if (configEntry.isEnabled() && configEntry.getInstalledVersion() != null) {
                File addonFile = getAddonJarFile(addonName, configEntry.getInstalledVersion());

                if (!addonFile.exists()) {
                    this.logger.info("Addon JAR missing: " + addonName + ". Downloading...");
                    installAddon(addonName, configEntry.getInstalledVersion());
                }
            }
        });
    }

    /**
     * Process a single addon from the registry
     * @param addonName Name of the addon
     * @param addonInfo Addon information from registry
     * @param upgrade Whether to upgrade addons to newer versions if available
     */
    private void processRegistryAddon(String addonName, Registry.AddonInfo addonInfo, boolean upgrade) {
        final AddonEntry configEntry = config.getAddonEntries().get(addonName);
        if (configEntry == null) {
            AddonEntry newEntry = new AddonEntry()
                    .setEnabled(false)
                    .setDescription(addonInfo.getDescription());
            config.saveAddonEntry(addonName, newEntry);
            return;
        }

        if (!configEntry.isEnabled()) return;

        String latestVersion = findLatestCompatibleVersion(addonInfo.getVersions());
        if (latestVersion == null) {
            configEntry.setEnabled(false);
            config.saveAddonEntry(addonName, configEntry);
            this.logger.warning("Disabled incompatible addon: " + addonName);
            removeAddonJar(addonName, configEntry.getInstalledVersion());
            return;
        }

        if (configEntry.getInstalledVersion() != null) {
            boolean currentVersionCompatible = addonInfo.getVersions().entrySet().stream()
                    .anyMatch(entry -> entry.getKey().equals(configEntry.getInstalledVersion()) &&
                            VersionUtils.isVersionCompatible(entry.getValue(), currentVersion));

            if (!currentVersionCompatible) {
                this.logger.warning("Current version of " + addonName + " (" +
                        configEntry.getInstalledVersion() + ") is no longer compatible. Updating to " + latestVersion);
                configEntry.setInstalledVersion(latestVersion);
                config.saveAddonEntry(addonName, configEntry);
            }
            else if (upgrade &&
                    !latestVersion.equals(configEntry.getInstalledVersion()) &&
                    VersionUtils.compareVersions(latestVersion, configEntry.getInstalledVersion()) > 0) {
                this.logger.info("Upgrading " + addonName + " from " +
                        configEntry.getInstalledVersion() + " to " + latestVersion);
                configEntry.setInstalledVersion(latestVersion);
                config.saveAddonEntry(addonName, configEntry);
            }
        } else {
            configEntry.setInstalledVersion(latestVersion);
            config.saveAddonEntry(addonName, configEntry);
        }

        if (addonInfo.getDescription() != null &&
                !addonInfo.getDescription().equals(configEntry.getDescription())) {
            configEntry.setDescription(addonInfo.getDescription());
            config.saveAddonEntry(addonName, configEntry);
        }
    }

    /**
     * Cleans up outdated and disabled addon JAR files
     */
    private void cleanupAddonJars() {
        /* Remove disabled addons */
        config.getAddonEntries().forEach((addonName, configEntry) -> {
            if (!configEntry.isEnabled() && configEntry.getInstalledVersion() != null) {
                removeAddonJar(addonName, configEntry.getInstalledVersion());
            }
        });

        /* Remove incorrect version JARs */
        try (Stream<Path> files = Files.list(pluginsFolder.toPath())) {
            files.map(Path::getFileName)
                    .map(Path::toString)
                    .filter(name -> name.endsWith(".jar"))
                    .forEach(fileName -> {
                        for (String addonName : config.getAddonEntries().keySet()) {
                            if (fileName.startsWith(addonName + "-")) {
                                AddonEntry entry = config.getAddonEntries().get(addonName);

                                if (!entry.isEnabled() || entry.getInstalledVersion() == null) {
                                    continue;
                                }

                                String correctVersion = entry.getInstalledVersion();
                                String expectedFileName = addonName + "-" + correctVersion + ".jar";

                                if (!fileName.equals(expectedFileName)) {
                                    Path incorrectJar = pluginsFolder.toPath().resolve(fileName);
                                    try {
                                        Files.delete(incorrectJar);
                                        this.logger.info("Removed incorrect version JAR: " + fileName);
                                    } catch (IOException e) {
                                        this.logger.warning("Failed to remove incorrect version JAR: " + fileName);
                                    }
                                }
                            }
                        }
                    });
        } catch (IOException e) {
            this.logger.warning("Error while cleaning up addon JARs: " + e.getMessage());
        }
    }

    /**
     * Removes an addon JAR file
     * @param addonName addon name
     * @param version addon version
     */
    private void removeAddonJar(String addonName, String version) {
        if (version == null) return;

        File jar = getAddonJarFile(addonName, version);
        if (jar.exists()) {
            try {
                Files.delete(jar.toPath());
                this.logger.info("Removed addon JAR: " + jar.getName());
            } catch (IOException e) {
                this.logger.warning("Failed to remove addon JAR: " + jar.getName());
            }
        }
    }

    /**
     * Gets File object for an addon JAR
     * @param addonName addon name
     * @param version addon version
     * @return File object for the JAR
     */
    private File getAddonJarFile(String addonName, String version) {
        return new File(pluginsFolder, addonName + "-" + version + ".jar");
    }

    /**
     * Finds the latest version compatible with current plugin version
     * @param versions map of addon versions to required plugin versions
     * @return latest compatible version, or null if none found
     */
    private String findLatestCompatibleVersion(Map<String, String> versions) {
        return versions.entrySet().stream()
                .filter(e -> VersionUtils.isVersionCompatible(e.getValue(), currentVersion))
                .map(Map.Entry::getKey)
                .max(VersionUtils::compareVersions)
                .orElse(null);
    }

    /**
     * Installs an addon from the registry
     * @param addonName addon name
     * @param version version to install
     */
    private void installAddon(String addonName, String version) {
        String downloadUrl = String.format("%s%s/releases/download/%s/%s-%s.jar",
                registry.getBaseURL(), addonName, version, addonName, version);

        File addonFile = getAddonJarFile(addonName, version);

        try {
            downloadFile(downloadUrl, addonFile);
            this.logger.success("Successfully installed " + addonName + " v" + version);
        } catch (IOException e) {
            this.logger.warning("Failed to install " + addonName + ": " + e.getMessage());
        }
    }

    /**
     * Downloads a file from a URL to a local file
     * @param urlStr URL to download from
     * @param file destination file
     * @throws IOException if download fails
     */
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

    /**
     * Fetches content from a URL
     * @param urlStr URL to fetch
     * @return content as String, or null if failed
     */
    private String fetchUrl(String urlStr) {
        try {
            URL url = new URI(urlStr).toURL();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(url.openStream()))) {
                StringBuilder buffer = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line);
                }
                return buffer.toString();
            }
        } catch (Exception e) {
            // failure - will try backup URL
            return null;
        }
    }
}