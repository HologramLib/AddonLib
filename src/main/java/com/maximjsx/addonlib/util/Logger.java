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

public interface Logger {
    enum LogLevel {
        ERROR, WARNING, INFO, SUCCESS
    }

    void log(LogLevel level, String message);

    default void error(String message) {
        log(LogLevel.ERROR, message);
    }

    default void warning(String message) {
        log(LogLevel.WARNING, message);
    }

    default void info(String message) {
        log(LogLevel.INFO, message);
    }

    default void success(String message) {
        log(LogLevel.SUCCESS, message);
    }
}