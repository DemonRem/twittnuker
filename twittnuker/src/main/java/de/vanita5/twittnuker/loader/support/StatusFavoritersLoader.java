/*
 * Twittnuker - Twitter client for Android
 *
 * Copyright (C) 2013-2015 vanita5 <mail@vanita5.de>
 *
 * This program incorporates a modified version of Twidere.
 * Copyright (C) 2012-2015 Mariotaku Lee <mariotaku.lee@gmail.com>
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

import de.vanita5.twittnuker.model.ParcelableUser;

import java.util.List;

import de.vanita5.twittnuker.api.twitter.model.IDs;
import de.vanita5.twittnuker.api.twitter.model.Paging;
import de.vanita5.twittnuker.api.twitter.Twitter;
import de.vanita5.twittnuker.api.twitter.TwitterException;

public class StatusFavoritersLoader extends IDsUsersLoader {

	private final long mStatusId;

	public StatusFavoritersLoader(final Context context, final long accountId, final long statusId,
                                  final long cursor, final List<ParcelableUser> data, boolean fromUser) {
		super(context, accountId, cursor, data, fromUser);
		mStatusId = statusId;
	}

	@Override
    protected IDs getIDs(final Twitter twitter, final Paging paging) throws TwitterException {
		if (twitter == null) return null;
		if (mStatusId > 0) return twitter.getStatusActivitySummary(mStatusId).getFavoriters();
		return null;
	}

}