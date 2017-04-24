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

package de.vanita5.twittnuker.loader.statuses

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.support.annotation.WorkerThread
import de.vanita5.twittnuker.library.MicroBlog
import de.vanita5.twittnuker.library.MicroBlogException
import de.vanita5.twittnuker.library.mastodon.Mastodon
import de.vanita5.twittnuker.library.twitter.model.Paging
import de.vanita5.twittnuker.library.twitter.model.ResponseList
import de.vanita5.twittnuker.library.twitter.model.Status
import de.vanita5.twittnuker.annotation.AccountType
import de.vanita5.twittnuker.extension.model.api.mastodon.toParcelable
import de.vanita5.twittnuker.extension.model.api.toParcelable
import de.vanita5.twittnuker.extension.model.newMicroBlogInstance
import de.vanita5.twittnuker.model.AccountDetails
import de.vanita5.twittnuker.model.ParcelableStatus
import de.vanita5.twittnuker.model.UserKey
import de.vanita5.twittnuker.util.InternalTwitterContentUtils

class UserFavoritesLoader(
        context: Context,
        accountKey: UserKey?,
        private val userKey: UserKey?,
        private val screenName: String?,
        sinceId: String?,
        maxId: String?,
        page: Int,
        data: List<ParcelableStatus>?,
        savedStatusesArgs: Array<String>?,
        tabPosition: Int,
        fromUser: Boolean,
        loadingMore: Boolean
) : AbsRequestStatusesLoader(context, accountKey, sinceId, maxId, page, data, savedStatusesArgs,
        tabPosition, fromUser, loadingMore) {

    @Throws(MicroBlogException::class)
    override fun getStatuses(account: AccountDetails, paging: Paging): List<ParcelableStatus> {
        when (account.type) {
            AccountType.MASTODON -> {
                return getMastodonStatuses(account, paging)
            }
        }
        return getMicroBlogStatuses(account, paging).map {
            it.toParcelable(account.key, account.type, profileImageSize)
        }
    }

    @WorkerThread
    override fun shouldFilterStatus(database: SQLiteDatabase, status: ParcelableStatus): Boolean {
        return InternalTwitterContentUtils.isFiltered(database, status, false)
    }

    override fun processPaging(details: AccountDetails, loadItemLimit: Int, paging: Paging) {
        when (details.type) {
            AccountType.FANFOU -> {
                paging.setCount(loadItemLimit)
                if (page > 0) {
                    paging.page(page)
                }
            }
            else -> {
                super.processPaging(details, loadItemLimit, paging)
            }
        }
    }

    private fun getMicroBlogStatuses(account: AccountDetails, paging: Paging): ResponseList<Status> {
        val microBlog = account.newMicroBlogInstance(context, MicroBlog::class.java)
        if (userKey != null) {
            return microBlog.getFavorites(userKey.id, paging)
        } else if (screenName != null) {
            return microBlog.getFavoritesByScreenName(screenName, paging)
        }
        throw MicroBlogException("Null user")
    }

    private fun getMastodonStatuses(account: AccountDetails, paging: Paging): List<ParcelableStatus> {
        if (userKey != null && userKey != account.key) {
            throw MicroBlogException("Only current account favorites is supported")
        }
        if (screenName != null && !screenName.equals(account.user?.screen_name, ignoreCase = true)) {
            throw MicroBlogException("Only current account favorites is supported")
        }
        val mastodon = account.newMicroBlogInstance(context, Mastodon::class.java)
        return mastodon.getFavourites(paging).map { it.toParcelable(account.key) }
    }
}