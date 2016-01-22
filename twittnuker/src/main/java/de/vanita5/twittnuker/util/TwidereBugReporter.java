/*
 * Twittnuker - Twitter client for Android
 *
 * Copyright (C) 2013-2016 vanita5 <mail@vanit.as>
 *
 * This program incorporates a modified version of Twidere.
 * Copyright (C) 2012-2016 Mariotaku Lee <mariotaku.lee@gmail.com>
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
import android.support.annotation.Nullable;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

import de.vanita5.twittnuker.BuildConfig;
import de.vanita5.twittnuker.Constants;

import io.fabric.sdk.android.Fabric;

public class TwidereBugReporter extends BugReporter implements Constants {

    @Override
    protected void logImpl(@Nullable String message, @Nullable Throwable throwable) {
        Log.d(LOGTAG, message, throwable);
    }

    @Override
    protected void errorImpl(@Nullable String message, @Nullable Throwable throwable) {
        if (throwable == null && message == null) {
            throw new NullPointerException("Message and Throwable can't be both null");
        }
        if (message != null) {
            if (BuildConfig.DEBUG) {
                Log.w(LOGTAG, message, throwable);
            }

            handleSilentException(new Exception(message, throwable));
            return;
        }
        if (BuildConfig.DEBUG) {
            Log.w(LOGTAG, throwable);
        }
        handleSilentException(throwable);
    }

    private void handleSilentException(final Throwable throwable) {
        Crashlytics.logException(throwable);
    }

    @Override
    protected void initImpl(final Application application) {
        Fabric.with(application, new Crashlytics());
    }

}