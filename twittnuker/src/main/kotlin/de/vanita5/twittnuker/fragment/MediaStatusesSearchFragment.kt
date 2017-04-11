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

package de.vanita5.twittnuker.fragment

import android.content.Context
import android.os.Bundle
import android.support.v4.content.Loader
import de.vanita5.twittnuker.TwittnukerConstants.*
import de.vanita5.twittnuker.loader.MediaStatusesSearchLoader
import de.vanita5.twittnuker.loader.TweetSearchLoader
import de.vanita5.twittnuker.model.ParcelableStatus
import de.vanita5.twittnuker.util.Utils

class MediaStatusesSearchFragment : AbsMediaStatusesFragment() {

    override fun onCreateStatusesLoader(context: Context, args: Bundle, fromUser: Boolean):
            Loader<List<ParcelableStatus>?> {
        refreshing = true
        val accountKey = Utils.getAccountKey(context, args)
        val maxId = args.getString(EXTRA_MAX_ID)
        val sinceId = args.getString(EXTRA_SINCE_ID)
        val page = args.getInt(EXTRA_PAGE, -1)
        val query = args.getString(EXTRA_QUERY)
        val tabPosition = args.getInt(EXTRA_TAB_POSITION, -1)
        val makeGap = args.getBoolean(EXTRA_MAKE_GAP, true)
        val loadingMore = args.getBoolean(EXTRA_LOADING_MORE, false)
        return TweetSearchLoader(activity, accountKey, query, sinceId, maxId, page, adapter.getData(),
                null, tabPosition, fromUser, makeGap, loadingMore)
    }

    override fun hasMoreData(loader: Loader<List<ParcelableStatus>?>, data: List<ParcelableStatus>?,
                             changed: Boolean): Boolean {
        if (loader !is MediaStatusesSearchLoader) return false
        val maxId = loader.maxId?.takeIf(String::isNotEmpty)
        val sinceId = loader.sinceId?.takeIf(String::isNotEmpty)
        if (sinceId != null && maxId != null) {
            if (data != null && !data.isEmpty()) {
                return changed
            }
        }
        return false
    }

    override fun getStatuses(maxId: String?, sinceId: String?): Int {
        if (context == null) return -1
        val args = Bundle(arguments)
        args.putBoolean(EXTRA_MAKE_GAP, false)
        args.putString(EXTRA_MAX_ID, maxId)
        args.putString(EXTRA_SINCE_ID, sinceId)
        args.putBoolean(EXTRA_FROM_USER, true)
        loaderManager.restartLoader(0, args, this)
        return 0
    }

}