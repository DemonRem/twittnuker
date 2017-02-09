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

package de.vanita5.twittnuker.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.text.TextUtils
import org.mariotaku.ktextension.convert
import org.mariotaku.ktextension.toLong
import de.vanita5.twittnuker.TwittnukerConstants.*
import de.vanita5.twittnuker.annotation.NotificationType
import de.vanita5.twittnuker.annotation.ReadPositionTag
import de.vanita5.twittnuker.constant.IntentConstants.BROADCAST_NOTIFICATION_DELETED
import de.vanita5.twittnuker.model.StringLongPair
import de.vanita5.twittnuker.model.UserKey
import de.vanita5.twittnuker.util.UriExtraUtils
import de.vanita5.twittnuker.util.Utils
import de.vanita5.twittnuker.util.dagger.DependencyHolder

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        when (action) {
            BROADCAST_NOTIFICATION_DELETED -> {
                val uri = intent.data ?: return
                val holder = DependencyHolder.get(context)
                @NotificationType
                val notificationType = uri.getQueryParameter(QUERY_PARAM_NOTIFICATION_TYPE)
                val accountKey = uri.getQueryParameter(QUERY_PARAM_ACCOUNT_KEY)?.convert(UserKey::valueOf)
                val itemId = UriExtraUtils.getExtra(uri, "item_id").toLong(-1)
                val itemUserId = UriExtraUtils.getExtra(uri, "item_user_id").toLong(-1)
                val itemUserFollowing = UriExtraUtils.getExtra(uri, "item_user_following")?.toBoolean() ?: false
                val timestamp = uri.getQueryParameter(QUERY_PARAM_TIMESTAMP)?.toLong() ?: -1
                val manager = holder.readStateManager
                val paramReadPosition = uri.getQueryParameter(QUERY_PARAM_READ_POSITION)
                val paramReadPositions = uri.getQueryParameter(QUERY_PARAM_READ_POSITIONS)
                @ReadPositionTag
                val tag = getPositionTag(notificationType)

                if (tag != null && !TextUtils.isEmpty(paramReadPosition)) {
                    manager.setPosition(Utils.getReadPositionTagWithAccount(tag, accountKey),
                            paramReadPosition.toLong(-1))
                } else if (!TextUtils.isEmpty(paramReadPositions)) {
                    try {
                        val pairs = StringLongPair.valuesOf(paramReadPositions)
                        for (pair in pairs) {
                            manager.setPosition(tag!!, pair.key, pair.value)
                        }
                    } catch (ignore: NumberFormatException) {

                    }

                }
            }
        }
    }

    @ReadPositionTag
    private fun getPositionTag(@NotificationType type: String?): String? {
        if (type == null) return null
        when (type) {
            NotificationType.HOME_TIMELINE -> return ReadPositionTag.HOME_TIMELINE
            NotificationType.INTERACTIONS -> return ReadPositionTag.ACTIVITIES_ABOUT_ME
            NotificationType.DIRECT_MESSAGES -> {
                return ReadPositionTag.DIRECT_MESSAGES
            }
        }
        return null
    }
}