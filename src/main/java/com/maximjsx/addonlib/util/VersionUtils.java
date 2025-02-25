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

package com.maximjsx.addonlib.util;

public class VersionUtils {
    /**
     * Compares two version strings in the format "x.y.z"
     * @return positive if v1 > v2, negative if v1 < v2, 0 if equal
     */
    public static int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");

        int length = Math.max(parts1.length, parts2.length);

        for (int i = 0; i < length; i++) {
            int num1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int num2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;

            if (num1 != num2) {
                return num1 - num2;
            }
        }

        return 0;
    }

    /**
     * Checks if the current plugin version is compatible with the required version
     * @param requiredVersion minimum version required by the addon
     * @param currentVersion current Plugin (e.g. HologramLib) version
     */
    public static boolean isVersionCompatible(String requiredVersion, String currentVersion) {
        return compareVersions(currentVersion, requiredVersion) >= 0;
    }
}