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
import com.maximjsx.addonlib.util.VersionUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class VersionUtilsTest {

    @ParameterizedTest
    @CsvSource({
            "1.0.0, 1.0.0, 0",
            "1.0.0, 1.0.1, -1",
            "1.0.1, 1.0.0, 1",
            "1.1.0, 1.0.0, 1",
            "2.0.0, 1.0.0, 1",
            "1.0, 1.0.0, 0",
            "1, 1.0.0, 0"
    })
    void compareVersions(String v1, String v2, int expected) {
        assertEquals(expected, Integer.signum(VersionUtils.compareVersions(v1, v2)),
                "Comparing " + v1 + " to " + v2);
    }

    @ParameterizedTest
    @CsvSource({
            "1.0.0, 1.0.0, true",
            "1.0.0, 1.0.1, false",
            "1.0.1, 1.0.0, true",
            "1.1.0, 1.0.0, true",
            "2.0.0, 1.0.0, true",
            "1.0.0, 2.0.0, false"
    })
    void isVersionCompatible(String current, String required, boolean expected) {
        assertEquals(expected, VersionUtils.isVersionCompatible(required, current),
                "Checking if " + current + " is compatible with " + required);
    }
}
