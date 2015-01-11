/*
 * Twittnuker - Twitter client for Android
 *
 * Copyright (C) 2013-2014 vanita5 <mail@vanita5.de>
 *
 * This program incorporates a modified version of Twidere.
 * Copyright (C) 2012-2014 Mariotaku Lee <mariotaku.lee@gmail.com>
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

package de.vanita5.twittnuker.loader.support;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import de.vanita5.twittnuker.model.ParcelableStatus;

import java.util.List;

import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;

import static de.vanita5.twittnuker.util.Utils.isFiltered;

public class UserListTimelineLoader extends Twitter4JStatusesLoader {

	private final long mUserId;
	private final String mScreenName, mListName;
	private final int mListId;
	private final boolean mFiltersForRts;

    public UserListTimelineLoader(final Context context, final long accountId, final int listId,
                                  final long userId, final String screenName, final String listName,
                                  final long sinceId, final long maxId, final List<ParcelableStatus> data,
                                  final String[] savedStatusesArgs, final int tabPosition, boolean fromUser) {
        super(context, accountId, sinceId, maxId, data, savedStatusesArgs, tabPosition, fromUser);
        mListId = listId;
        mUserId = userId;
        mScreenName = screenName;
        mListName = listName;
		mFiltersForRts = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).getBoolean(
				KEY_FILTERS_FOR_RTS, true);
	}

	@Override
	protected ResponseList<Status> getStatuses(final Twitter twitter, final Paging paging) throws TwitterException {
		if (twitter == null) return null;
		if (mListId > 0)
			return twitter.getUserListStatuses(mListId, paging);
        else if (mListName == null)
            return null;
		else if (mUserId > 0)
			return twitter.getUserListStatuses(mListName.replace(' ', '-'), mUserId, paging);
		else if (mScreenName != null)
			return twitter.getUserListStatuses(mListName.replace(' ', '-'), mScreenName, paging);
		return null;
	}

	@Override
	protected boolean shouldFilterStatus(final SQLiteDatabase database, final ParcelableStatus status) {
		return isFiltered(database, status, mFiltersForRts);
	}

}
