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

import com.maximjsx.addonlib.config.AddonConfig;
import com.maximjsx.addonlib.util.Logger;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.io.File;

public class AddonLib {

    private AddonManager addonManager;

    @Getter
    private AddonConfig config;

    /**
     * The primary registry URL
     */
    @Getter @Accessors(chain = true)
    private String registry = "https://cdn.maximjsx.com/hologramlib/registry.json";

    /**
     * The primary registry URL
     */
    @Getter @Accessors(chain = true)
    private String backupRegistry = "https://raw.githubusercontent.com/HologramLib/Addons/main/registry.json";

    @Getter @Accessors(chain = true)
    private String[] enabledAddons =  new String[]{};

    private final File folder;

    private final Logger logger;

    private final String version;

    /**
     * @param folder The plugins folder
     * @param version The version of the plugin e.g. 1.7.1 (HologramLib)
     */
    public AddonLib(Logger logger, File folder, String version) {
        this.logger = logger;
        this.folder = folder;
        this.version = version;
    }

    public void init() {
        this.config = new AddonConfig(folder, enabledAddons);
        this.addonManager = new AddonManager(logger, folder, version, config, registry, backupRegistry);
        this.addonManager.loadRegistry();
        this.addonManager.checkAndUpdateAddons(config.isAutoUpgrade());
    }

    /**
     * Reloads the addon registry and checks for updates
     */
    public void reload(boolean upgrade) {
        if(config == null || addonManager == null) {
            logger.error("Failed to reload AddonLib because it was not initialized! Use AddonLib#init()!");
            return;
        }
        this.config.loadConfig();
        this.addonManager.loadRegistry();
        this.addonManager.checkAndUpdateAddons(upgrade);
    }

}