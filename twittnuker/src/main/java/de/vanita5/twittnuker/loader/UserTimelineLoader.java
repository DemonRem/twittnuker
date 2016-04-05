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

package de.vanita5.twittnuker.loader;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;

import de.vanita5.twittnuker.api.twitter.Twitter;
import de.vanita5.twittnuker.api.twitter.TwitterException;
import de.vanita5.twittnuker.api.twitter.model.Paging;
import de.vanita5.twittnuker.api.twitter.model.ResponseList;
import de.vanita5.twittnuker.api.twitter.model.Status;
import de.vanita5.twittnuker.model.ParcelableCredentials;
import de.vanita5.twittnuker.model.ParcelableStatus;
import de.vanita5.twittnuker.model.UserKey;
import de.vanita5.twittnuker.util.InternalTwitterContentUtils;

import java.util.List;

public class UserTimelineLoader extends TwitterAPIStatusesLoader {

    @Nullable
    private final UserKey mUserId;
    @Nullable
    private final String mUserScreenName;

    public UserTimelineLoader(final Context context, @Nullable final UserKey accountId,
                              @Nullable final UserKey userId, @Nullable final String screenName,
                              final String sinceId, final String maxId,
                              final List<ParcelableStatus> data, final String[] savedStatusesArgs,
                              final int tabPosition, boolean fromUser, boolean loadingMore) {
        super(context, accountId, sinceId, maxId, data, savedStatusesArgs, tabPosition, fromUser, loadingMore);
        mUserId = userId;
        mUserScreenName = screenName;
    }

    @NonNull
    @Override
    protected ResponseList<Status> getStatuses(@NonNull final Twitter twitter,
                                               @NonNull ParcelableCredentials credentials,
                                               @NonNull final Paging paging) throws TwitterException {
        if (mUserId != null) {
            return twitter.getUserTimeline(mUserId.getId(), paging);
        } else if (mUserScreenName != null) {
            return twitter.getUserTimelineByScreenName(mUserScreenName, paging);
        } else {
            throw new TwitterException("Invalid user");
        }
    }

    @WorkerThread
    @Override
    protected boolean shouldFilterStatus(final SQLiteDatabase database, final ParcelableStatus status) {
        final UserKey accountId = getAccountKey();
        if (accountId != null && mUserId != null && TextUtils.equals(accountId.getId(), mUserId.getId()))
            return false;
        final UserKey retweetUserId = status.is_retweet ? status.user_key : null;
        return InternalTwitterContentUtils.isFiltered(database, retweetUserId, status.text_plain,
                status.spans, status.source, null, status.quoted_user_key);
    }
}