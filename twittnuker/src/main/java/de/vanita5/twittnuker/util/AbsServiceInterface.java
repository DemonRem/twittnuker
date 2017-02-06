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
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.support.annotation.Nullable;

import de.vanita5.twittnuker.constant.IntentConstants;
import de.vanita5.twittnuker.util.ServiceUtils.ServiceToken;

public abstract class AbsServiceInterface<I extends IInterface> implements IInterface {

    private final Context mContext;
    private final String mShortenerName;
    @Nullable
    private final Bundle mMetaData;
    private I mIInterface;

    private ServiceToken mToken;
    private boolean mDisconnected;

    private final ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(final ComponentName service, final IBinder obj) {
            mIInterface = AbsServiceInterface.this.onServiceConnected(service, obj);
            mDisconnected = false;
        }

        @Override
        public void onServiceDisconnected(final ComponentName service) {
            mIInterface = null;
            mDisconnected = true;
        }
    };

    protected abstract I onServiceConnected(ComponentName service, IBinder obj);

    protected AbsServiceInterface(final Context context, final String componentName, @Nullable final Bundle metaData) {
        mDisconnected = true;
        mContext = context;
        mShortenerName = componentName;
        mMetaData = metaData;
    }

    public final I getInterface() {
        return mIInterface;
    }

    @Override
    public final IBinder asBinder() {
        // Useless here
        return mIInterface.asBinder();
    }

    public final void unbindService() {
        if (mIInterface == null || mToken == null) return;
        ServiceUtils.unbindFromService(mToken);
    }

    public final boolean waitForService() {
        if (mIInterface != null || mToken != null) return true;
        final Intent intent = new Intent(IntentConstants.INTENT_ACTION_EXTENSION_SHORTEN_STATUS);
        final ComponentName component = ComponentName.unflattenFromString(mShortenerName);
        intent.setComponent(component);
        mDisconnected = true;
        mToken = ServiceUtils.bindToService(mContext, intent, mConnection);
        if (mToken == null) return false;
        mDisconnected = false;
        while (mIInterface == null && !mDisconnected) {
            try {
                Thread.sleep(50L);
            } catch (InterruptedException e) {
                // Ignore
                return false;
            }
        }
        return true;
    }

    public final void checkService(CheckServiceAction action) throws CheckServiceException {
        action.check(mMetaData);
    }

    public interface CheckServiceAction {
        void check(@Nullable Bundle metaData) throws CheckServiceException;
    }

    public static class CheckServiceException extends Exception {

    }
}