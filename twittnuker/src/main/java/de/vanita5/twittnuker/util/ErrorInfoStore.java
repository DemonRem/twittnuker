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
import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import de.vanita5.twittnuker.R;
import de.vanita5.twittnuker.model.UserKey;


public class ErrorInfoStore {

    public static final String KEY_DIRECT_MESSAGES = "direct_messages";
    public static final String KEY_INTERACTIONS = "interactions";
    public static final String KEY_HOME_TIMELINE = "home_timeline";
    public static final String KEY_ACTIVITIES_BY_FRIENDS = "activities_by_friends";

    public static final int CODE_NO_DM_PERMISSION = 1;
    public static final int CODE_NO_ACCESS_FOR_CREDENTIALS = 2;
    public static final int CODE_NETWORK_ERROR = 3;
    public static final int CODE_TIMESTAMP_ERROR = 4;

    private final SharedPreferences mPreferences;

    public ErrorInfoStore(Application application) {
        mPreferences = application.getSharedPreferences("error_info", Context.MODE_PRIVATE);
    }

    public int get(String key) {
        return mPreferences.getInt(key, 0);
    }

    public int get(String key, String extraId) {
        return get(key + "_" + extraId);
    }

    public int get(String key, UserKey extraId) {
        final String host = extraId.getHost();
        if (host == null) {
            return get(key, extraId.getId());
        } else {
            return get(key + "_" + extraId.getId() + "_" + host);
        }
    }

    public void set(String key, int code) {
        mPreferences.edit().putInt(key, code).apply();
    }

    public void set(String key, String extraId, int code) {
        set(key + "_" + extraId, code);
    }

    public void set(String key, UserKey extraId, int code) {
        final String host = extraId.getHost();
        if (host == null) {
            set(key, extraId.getId(), code);
        } else {
            set(key + "_" + extraId.getId() + "_" + host, code);
        }
    }

    @Nullable
    public static DisplayErrorInfo getErrorInfo(@NonNull Context context, int code) {
        switch (code) {
            case CODE_NO_DM_PERMISSION: {
                return new DisplayErrorInfo(code, R.drawable.ic_info_error_generic,
                        context.getString(R.string.error_no_dm_permission));
            }
            case CODE_NO_ACCESS_FOR_CREDENTIALS: {
                return new DisplayErrorInfo(code, R.drawable.ic_info_error_generic,
                        context.getString(R.string.error_no_access_for_credentials));
            }
            case CODE_NETWORK_ERROR: {
                return new DisplayErrorInfo(code, R.drawable.ic_info_error_generic,
                        context.getString(R.string.message_toast_network_error));
            }
            case CODE_TIMESTAMP_ERROR: {
                return new DisplayErrorInfo(code, R.drawable.ic_info_error_generic,
                        context.getString(R.string.error_info_oauth_timestamp_error));
            }
        }
        return null;
    }

    public void remove(String key, String extraId) {
        remove(key + "_" + extraId);
    }

    public void remove(String key, UserKey extraId) {
        final String host = extraId.getHost();
        if (host == null) {
            remove(key, extraId.getId());
        } else {
            remove(key + "_" + extraId.getId() + "_" + host);
        }
    }

    public void remove(String key) {
        mPreferences.edit().remove(key).apply();
    }

    public static class DisplayErrorInfo {
        int code;
        @DrawableRes
        int icon;
        String message;

        public DisplayErrorInfo(int code, @DrawableRes int icon, String message) {
            this.code = code;
            this.icon = icon;
            this.message = message;
        }

        public int getCode() {
            return code;
        }

        @DrawableRes
        public int getIcon() {
            return icon;
        }

        public String getMessage() {
            return message;
        }
    }
}