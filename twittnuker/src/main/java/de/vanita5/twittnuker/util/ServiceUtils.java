/*
 * Twittnuker - Twitter client for Android
 *
 * Copyright (C) 2013-2017 vanita5 <mail@vanit.as>
 *
 * This program incorporates a modified version of Twidere.
 * Copyright (C) 2012-2017 Mariotaku Lee <mariotaku.lee@gmail.com>
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

import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;
import android.util.Log;

import java.util.HashMap;

import static de.vanita5.twittnuker.TwittnukerConstants.LOGTAG;

public final class ServiceUtils {

    private static HashMap<Context, ServiceUtils.ServiceBinder> sConnectionMap = new HashMap<>();

    private ServiceUtils() {
    }

    public static ServiceToken bindToService(final Context context, final Intent intent,
                                             final ServiceConnection callback) {

        final ContextWrapper cw = new ContextWrapper(context);
        final ComponentName cn = cw.startService(intent);
        if (cn != null) {
            final ServiceUtils.ServiceBinder sb = new ServiceBinder(callback);
            if (cw.bindService(intent, sb, 0)) {
                sConnectionMap.put(cw, sb);
                return new ServiceToken(cw);
            }
        }
        DebugLog.w(LOGTAG, "Failed to bind to service", null);
        return null;
    }

    public static void unbindFromService(final ServiceToken token) {
        final ServiceBinder serviceBinder = sConnectionMap.get(token.wrappedContext);
        if (serviceBinder == null) return;
        token.wrappedContext.unbindService(serviceBinder);
    }

    public static class ServiceToken {

        private final ContextWrapper wrappedContext;

        ServiceToken(final ContextWrapper context) {
            wrappedContext = context;
        }
    }

    static class ServiceBinder implements ServiceConnection {

        private final ServiceConnection mCallback;

        public ServiceBinder(final ServiceConnection callback) {
            mCallback = callback;
        }

        @Override
        public void onServiceConnected(final ComponentName className, final android.os.IBinder service) {
            if (mCallback != null) {
                mCallback.onServiceConnected(className, service);
            }
        }

        @Override
        public void onServiceDisconnected(final ComponentName className) {
            if (mCallback != null) {
                mCallback.onServiceDisconnected(className);
            }
        }
    }
}