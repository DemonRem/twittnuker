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

package de.vanita5.twittnuker.task

import android.content.ContentValues
import android.content.Context
import android.widget.Toast
import de.vanita5.microblog.library.MicroBlog
import de.vanita5.microblog.library.MicroBlogException
import de.vanita5.microblog.library.mastodon.Mastodon
import de.vanita5.twittnuker.R
import de.vanita5.twittnuker.annotation.AccountType
import de.vanita5.twittnuker.constant.nameFirstKey
import de.vanita5.twittnuker.extension.model.api.mastodon.toParcelable
import de.vanita5.twittnuker.extension.model.api.toParcelable
import de.vanita5.twittnuker.extension.model.newMicroBlogInstance
import de.vanita5.twittnuker.model.AccountDetails
import de.vanita5.twittnuker.model.ParcelableUser
import de.vanita5.twittnuker.model.event.FriendshipTaskEvent
import de.vanita5.twittnuker.provider.TwidereDataStore.CachedRelationships

class DestroyUserBlockTask(context: Context) : AbsFriendshipOperationTask(context, FriendshipTaskEvent.Action.UNBLOCK) {

    @Throws(MicroBlogException::class)
    override fun perform(details: AccountDetails, args: Arguments): ParcelableUser {
        when (details.type) {
            AccountType.MASTODON -> {
                val mastodon = details.newMicroBlogInstance(context, Mastodon::class.java)
                mastodon.unblockUser(args.userKey.id)
                return mastodon.getAccount(args.userKey.id).toParcelable(details)
            }
            AccountType.FANFOU -> {
                val fanfou = details.newMicroBlogInstance(context, MicroBlog::class.java)
                return fanfou.destroyFanfouBlock(args.userKey.id).toParcelable(details,
                        profileImageSize = profileImageSize)
            }
            else -> {
                val twitter = details.newMicroBlogInstance(context, MicroBlog::class.java)
                return twitter.destroyBlock(args.userKey.id).toParcelable(details,
                        profileImageSize = profileImageSize)
            }
        }
    }

    override fun succeededWorker(details: AccountDetails,
            args: Arguments,
            user: ParcelableUser) {
        val resolver = context.contentResolver
        // I bet you don't want to see this user in your auto complete list.
        val values = ContentValues()
        values.put(CachedRelationships.ACCOUNT_KEY, args.accountKey.toString())
        values.put(CachedRelationships.USER_KEY, args.userKey.toString())
        values.put(CachedRelationships.BLOCKING, false)
        values.put(CachedRelationships.FOLLOWING, false)
        values.put(CachedRelationships.FOLLOWED_BY, false)
        resolver.insert(CachedRelationships.CONTENT_URI, values)
    }

    override fun showSucceededMessage(params: AbsFriendshipOperationTask.Arguments, user: ParcelableUser) {
        val nameFirst = kPreferences[nameFirstKey]
        val message = context.getString(R.string.unblocked_user, manager.getDisplayName(user, nameFirst))
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

    }
}