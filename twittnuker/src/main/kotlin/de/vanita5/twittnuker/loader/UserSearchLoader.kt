/*
 *  Twittnuker - Twitter client for Android
 *
 *  Copyright (C) 2013-2017 vanita5 <mail@vanit.as>
 *
 *  This program incorporates a modified version of Twidere.
 *  Copyright (C) 2012-2017 Mariotaku Lee <mariotaku.lee@gmail.com>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.vanita5.twittnuker.loader

import android.content.Context
import de.vanita5.twittnuker.library.MicroBlog
import de.vanita5.twittnuker.library.MicroBlogException
import de.vanita5.twittnuker.library.mastodon.Mastodon
import de.vanita5.twittnuker.library.twitter.model.Paging
import de.vanita5.twittnuker.annotation.AccountType
import de.vanita5.twittnuker.extension.model.api.mastodon.toParcelable
import de.vanita5.twittnuker.extension.model.api.toParcelable
import de.vanita5.twittnuker.extension.model.newMicroBlogInstance
import de.vanita5.twittnuker.model.AccountDetails
import de.vanita5.twittnuker.model.ParcelableUser
import de.vanita5.twittnuker.model.UserKey

open class UserSearchLoader(
        context: Context,
        accountKey: UserKey?,
        val query: String,
        val page: Int,
        data: List<ParcelableUser>?,
        fromUser: Boolean
) : AbsRequestUsersLoader(context, accountKey, data, fromUser) {

    @Throws(MicroBlogException::class)
    override fun getUsers(details: AccountDetails): List<ParcelableUser> {
        val paging = Paging()
        paging.page(page)
        when (details.type) {
            AccountType.MASTODON -> {
                val mastodon = details.newMicroBlogInstance(context, Mastodon::class.java)
                return mastodon.searchAccounts(query, paging).map {
                    it.toParcelable(details.key)
                }
            }
            AccountType.FANFOU -> {
                val microBlog = details.newMicroBlogInstance(context, MicroBlog::class.java)
                return microBlog.searchFanfouUsers(query, paging).map {
                    it.toParcelable(details.key, details.type, profileImageSize = profileImageSize)
            }
        }
            else -> {
                val microBlog = details.newMicroBlogInstance(context, MicroBlog::class.java)
                return microBlog.searchUsers(query, paging).map {
                    it.toParcelable(details.key, details.type, profileImageSize = profileImageSize)
                }
            }
        }
    }

}