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

package de.vanita5.twittnuker.fragment.support;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.Loader;

import de.vanita5.twittnuker.loader.support.UserTimelineLoader;
import de.vanita5.twittnuker.model.ParcelableStatus;
import de.vanita5.twittnuker.model.UserKey;

import java.util.List;

public class UserTimelineFragment extends ParcelableStatusesFragment {

    @Override
    protected Loader<List<ParcelableStatus>> onCreateStatusesLoader(final Context context,
                                                                    final Bundle args,
                                                                    final boolean fromUser) {
        setRefreshing(true);
        final List<ParcelableStatus> data = getAdapterData();
        final UserKey accountKey = args.getParcelable(EXTRA_ACCOUNT_KEY);
        final long maxId = args.getLong(EXTRA_MAX_ID, -1);
        final long sinceId = args.getLong(EXTRA_SINCE_ID, -1);
        final long userId = args.getLong(EXTRA_USER_ID, -1);
        final String screenName = args.getString(EXTRA_SCREEN_NAME);
        final int tabPosition = args.getInt(EXTRA_TAB_POSITION, -1);
        return new UserTimelineLoader(context, accountKey, userId, screenName, sinceId, maxId, data,
                getSavedStatusesFileArgs(), tabPosition, fromUser);
    }

    @Override
    protected String[] getSavedStatusesFileArgs() {
        final Bundle args = getArguments();
        if (args == null) return null;
        final UserKey accountKey = args.getParcelable(EXTRA_ACCOUNT_KEY);
        final long userId = args.getLong(EXTRA_USER_ID, -1);
        final String screenName = args.getString(EXTRA_SCREEN_NAME);
        return new String[]{AUTHORITY_USER_TIMELINE, "account" + accountKey, "user" + userId + "name" + screenName};
    }

    @Override
    protected String getReadPositionTagWithArguments() {
        final Bundle args = getArguments();
        assert args != null;
        final int tabPosition = args.getInt(EXTRA_TAB_POSITION, -1);
        StringBuilder sb = new StringBuilder("user_timeline_");
        if (tabPosition < 0) return null;

        final long userId = args.getLong(EXTRA_USER_ID, -1);
        final String screenName = args.getString(EXTRA_SCREEN_NAME);
        if (userId > 0) {
            sb.append(userId);
        } else if (screenName != null) {
            sb.append(screenName);
        } else {
            return null;
        }
        return sb.toString();
    }
}