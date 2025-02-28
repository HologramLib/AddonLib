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

import java.util.Arrays;
import java.util.Objects;

public class VersionUtils {

    /**
     * Compares two version strings in the format "x.y.z"
     * @param v1 First version string
     * @param v2 Second version string
     * @return positive if v1 > v2, negative if v1 < v2, 0 if equal
     */
    public static int compareVersions(String v1, String v2) {
        Objects.requireNonNull(v1, "VersionUtils#compareVersions(..): First version cannot be null");
        Objects.requireNonNull(v2, "VersionUtils#compareVersions(..): Second version cannot be null");

        int[] parts1 = Arrays.stream(v1.split("\\."))
                .mapToInt(Integer::parseInt)
                .toArray();

        int[] parts2 = Arrays.stream(v2.split("\\."))
                .mapToInt(Integer::parseInt)
                .toArray();

        int length = Math.max(parts1.length, parts2.length);

        for (int i = 0; i < length; i++) {
            int num1 = i < parts1.length ? parts1[i] : 0;
            int num2 = i < parts2.length ? parts2[i] : 0;

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
     * @return true if current version is greater than or equal to required version
     */
    public static boolean isVersionCompatible(String requiredVersion, String currentVersion) {
        return compareVersions(currentVersion, requiredVersion) >= 0;
    }
}