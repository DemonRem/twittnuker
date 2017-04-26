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

package de.vanita5.twittnuker.extension.model.api.mastodon

import org.mariotaku.ktextension.mapToArray
import de.vanita5.microblog.library.mastodon.model.Notification
import de.vanita5.microblog.library.twitter.model.Activity
import de.vanita5.twittnuker.model.*

fun Notification.toParcelable(details: AccountDetails): ParcelableActivity {
    return toParcelable(details.key).apply {
        account_color = details.color
    }
}

fun Notification.toParcelable(accountKey: UserKey): ParcelableActivity {
    val result = ParcelableActivity()
    result.account_key = accountKey
    result.timestamp = createdAt.time
    result.min_position = id
    result.max_position = id
    result.min_sort_position = result.timestamp
    result.max_sort_position = result.timestamp
    result.sources = toSources(accountKey)
    when (type) {
        Notification.Type.MENTION -> {
            result.action = Activity.Action.MENTION
            result.target_object_statuses = toStatuses(accountKey)
        }
        Notification.Type.REBLOG -> {
            result.action = Activity.Action.RETWEET
            result.target_object_statuses = toStatuses(accountKey)
        }
        Notification.Type.FAVOURITE -> {
            result.action = Activity.Action.FAVORITE
            result.target_statuses = toStatuses(accountKey)
        }
        Notification.Type.FOLLOW -> {
            result.action = Activity.Action.FOLLOW
        }
        else -> {
            result.action = type
        }
    }
    result.source_keys = result.sources?.mapToArray { it.key }
    return result
}

private fun Notification.toSources(accountKey: UserKey): Array<ParcelableUser>? {
    val account = this.account ?: return null
    return arrayOf(account.toParcelable(accountKey))
}

private fun Notification.toStatuses(accountKey: UserKey): Array<ParcelableStatus>? {
    val status = this.status ?: return null
    return arrayOf(status.toParcelable(accountKey))
}