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

package de.vanita5.twittnuker.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.Loader;

import de.vanita5.twittnuker.loader.UserListTimelineLoader;
import de.vanita5.twittnuker.model.ParcelableStatus;
import de.vanita5.twittnuker.model.UserKey;
import de.vanita5.twittnuker.util.Utils;

import java.util.List;

public class UserListTimelineFragment extends ParcelableStatusesFragment {

    @Override
    protected Loader<List<ParcelableStatus>> onCreateStatusesLoader(final Context context,
                                                                    final Bundle args,
                                                                    final boolean fromUser) {
        setRefreshing(true);
        if (args == null) return null;
        final long listId = args.getLong(EXTRA_LIST_ID, -1);
        final UserKey accountKey = Utils.getAccountKey(context, args);
        final String maxId = args.getString(EXTRA_MAX_ID);
        final String sinceId = args.getString(EXTRA_SINCE_ID);
        final String userId = args.getString(EXTRA_USER_ID);
        final String screenName = args.getString(EXTRA_SCREEN_NAME);
        final String listName = args.getString(EXTRA_LIST_NAME);
        final int tabPosition = args.getInt(EXTRA_TAB_POSITION, -1);
        final boolean loadingMore = args.getBoolean(EXTRA_LOADING_MORE, false);
        return new UserListTimelineLoader(getActivity(), accountKey, listId, userId, screenName,
                listName, sinceId, maxId, getAdapterData(), getSavedStatusesFileArgs(), tabPosition,
                fromUser, loadingMore);
    }

    @Override
    protected String[] getSavedStatusesFileArgs() {
        final Bundle args = getArguments();
        assert args != null;
        final long listId = args.getLong(EXTRA_LIST_ID, -1);
        final UserKey accountKey = args.getParcelable(EXTRA_ACCOUNT_KEY);
        final String userId = args.getString(EXTRA_USER_ID);
        final String screenName = args.getString(EXTRA_SCREEN_NAME);
        final String listName = args.getString(EXTRA_LIST_NAME);
        return new String[]{AUTHORITY_USER_LIST_TIMELINE, "account" + accountKey, "list_id" + listId,
                "list_name" + listName, "user_id" + userId, "screen_name" + screenName};
    }

    @Override
    protected String getReadPositionTagWithArguments() {
        final Bundle args = getArguments();
        assert args != null;
        final int tabPosition = args.getInt(EXTRA_TAB_POSITION, -1);
        StringBuilder sb = new StringBuilder("user_list_");
        if (tabPosition < 0) return null;
        final long listId = args.getLong(EXTRA_LIST_ID, -1);
        final String listName = args.getString(EXTRA_LIST_NAME);
        if (listId > 0) {
            sb.append(listId);
        } else if (listName != null) {
            final String userId = args.getString(EXTRA_USER_ID);
            final String screenName = args.getString(EXTRA_SCREEN_NAME);
            if (userId != null) {
                sb.append(userId);
            } else if (screenName != null) {
                sb.append(screenName);
            } else {
                return null;
            }
            sb.append('_');
            sb.append(listName);
        } else {
            return null;
        }
        return sb.toString();
    }

}