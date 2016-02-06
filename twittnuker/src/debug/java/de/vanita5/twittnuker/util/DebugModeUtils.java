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

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import com.squareup.leakcanary.AndroidExcludedRefs;
import com.squareup.leakcanary.DisplayLeakService;
import com.squareup.leakcanary.ExcludedRefs;
import com.squareup.leakcanary.LeakCanary;
import com.squareup.leakcanary.RefWatcher;
import com.squareup.leakcanary.ServiceHeapDumpListener;

import de.vanita5.twittnuker.activity.support.ComposeActivity;
import okhttp3.OkHttpClient;

public class DebugModeUtils {

    private static RefWatcher sRefWatcher;

    public static void initForHttpClient(final OkHttpClient.Builder client) {
    }

    public static void initForApplication(final Application application) {
        ExcludedRefs.Builder excludedRefsBuilder = AndroidExcludedRefs.createAppDefaults();
        LeakCanary.enableDisplayLeakActivity(application);
        ServiceHeapDumpListener heapDumpListener = new ServiceHeapDumpListener(application, DisplayLeakService.class);
        final RefWatcher refWatcher = LeakCanary.androidWatcher(application, heapDumpListener, excludedRefsBuilder.build());
        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

            }

            @Override
            public void onActivityStarted(Activity activity) {

            }

            @Override
            public void onActivityResumed(Activity activity) {

            }

            @Override
            public void onActivityPaused(Activity activity) {

            }

            @Override
            public void onActivityStopped(Activity activity) {

            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

            }

            @Override
            public void onActivityDestroyed(Activity activity) {
                // Ignore memory leak caused by LocationManager
                if (activity.getClass() == ComposeActivity.class) return;
                refWatcher.watch(activity);
            }
        });
        sRefWatcher = refWatcher;
    }

    public static void watchReferenceLeak(final Object object) {
        if (sRefWatcher == null) return;
        sRefWatcher.watch(object);
    }
}