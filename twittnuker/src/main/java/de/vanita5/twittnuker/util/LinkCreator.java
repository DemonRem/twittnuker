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

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import de.vanita5.twittnuker.Constants;
import de.vanita5.twittnuker.model.UserKey;
import de.vanita5.twittnuker.model.ParcelableStatus;

public class LinkCreator implements Constants {

    private static final String AUTHORITY_TWITTER = "twitter.com";
    private static final String AUTHORITY_FANFOU = "fanfou.com";

    public static Uri getTwitterStatusLink(String screenName, String statusId) {
        Uri.Builder builder = new Uri.Builder();
        builder.scheme(SCHEME_HTTPS);
        builder.authority(AUTHORITY_TWITTER);
        builder.appendPath(screenName);
        builder.appendPath("status");
        builder.appendPath(statusId);
        return builder.build();
    }

    public static Uri getTwidereStatusLink(UserKey accountKey, @NonNull String statusId) {
        final Uri.Builder builder = new Uri.Builder();
        builder.scheme(SCHEME_TWITTNUKER);
        builder.authority(AUTHORITY_STATUS);
        if (accountKey != null) {
            builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_KEY, accountKey.toString());
        }
        builder.appendQueryParameter(QUERY_PARAM_STATUS_ID, statusId);
        return builder.build();
    }

    public static Uri getTwidereUserLink(@Nullable UserKey accountKey, String userId, String screenName) {
        final Uri.Builder builder = new Uri.Builder();
        builder.scheme(SCHEME_TWITTNUKER);
        builder.authority(AUTHORITY_USER);
        if (accountKey != null) {
            builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_KEY, accountKey.toString());
        }
        if (userId != null) {
            builder.appendQueryParameter(QUERY_PARAM_USER_ID, userId);
        }
        if (screenName != null) {
            builder.appendQueryParameter(QUERY_PARAM_SCREEN_NAME, screenName);
        }
        return builder.build();
    }

    public static Uri getTwitterUserListLink(String userScreenName, String listName) {
        Uri.Builder builder = new Uri.Builder();
        builder.scheme(SCHEME_HTTPS);
        builder.authority(AUTHORITY_TWITTER);
        builder.appendPath(userScreenName);
        builder.appendPath(listName);
        return builder.build();
    }

    public static Uri getTwitterUserLink(String screenName) {
        Uri.Builder builder = new Uri.Builder();
        builder.scheme(SCHEME_HTTPS);
        builder.authority(AUTHORITY_TWITTER);
        builder.appendPath(screenName);
        return builder.build();
    }

    public static Uri getStatusWebLink(ParcelableStatus status) {
        if (status.extras != null && !TextUtils.isEmpty(status.extras.external_url)) {
            return Uri.parse(status.extras.external_url);
        }
        if (USER_TYPE_FANFOU_COM.equals(status.account_key.getHost())) {
            return getFanfouStatusLink(status.id);
        }
        return getTwitterStatusLink(status.user_screen_name, status.id);
    }

    private static Uri getFanfouStatusLink(String id) {
        Uri.Builder builder = new Uri.Builder();
        builder.scheme(SCHEME_HTTP);
        builder.authority(AUTHORITY_FANFOU);
        builder.appendPath("statuses");
        builder.appendPath(id);
        return builder.build();
    }
}