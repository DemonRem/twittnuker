/*
 * Twittnuker - Twitter client for Android
 *
 * Copyright (C) 2013-2015 vanita5 <mail@vanita5.de>
 *
 * This program incorporates a modified version of Twidere.
 * Copyright (C) 2012-2015 Mariotaku Lee <mariotaku.lee@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.vanita5.twittnuker.util;

import android.app.Application;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public abstract class BugReporter {

    private static BugReporter sImplementation;

    public static void setImplementation(BugReporter impl) {
        sImplementation = impl;
    }

    public static void log(@Nullable String message, @Nullable Throwable throwable) {
        if (sImplementation == null) return;
        sImplementation.logImpl(message, throwable);
    }

    public static void error(@Nullable String message, @Nullable Throwable throwable) {
        if (sImplementation == null) return;
        sImplementation.errorImpl(message, throwable);
    }

    public static void error(@NonNull String message) {
        error(message, null);
    }

    public static void init(Application application) {
        if (sImplementation == null) return;
        sImplementation.initImpl(application);
    }

    public static void error(Throwable throwable) {
        error(null, throwable);
    }

    public static void logIfFalse(boolean expression, String message) {
        if (!expression) error(message);
    }

    protected abstract void logImpl(@Nullable String message, @Nullable Throwable throwable);

    protected abstract void errorImpl(@Nullable String message, @Nullable Throwable throwable);

    protected abstract void initImpl(Application application);
}