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

package de.vanita5.twittnuker.util

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.database.Cursor
import android.media.AudioManager
import android.net.Uri
import android.support.v4.app.NotificationCompat
import android.text.TextUtils
import org.apache.commons.lang3.ArrayUtils
import org.mariotaku.kpreferences.get
import de.vanita5.twittnuker.library.twitter.model.Activity
import org.mariotaku.sqliteqb.library.*
import de.vanita5.twittnuker.R
import de.vanita5.twittnuker.TwittnukerConstants.*
import de.vanita5.twittnuker.activity.HomeActivity
import de.vanita5.twittnuker.annotation.CustomTabType
import de.vanita5.twittnuker.annotation.NotificationType
import de.vanita5.twittnuker.constant.IntentConstants
import de.vanita5.twittnuker.constant.iWantMyStarsBackKey
import de.vanita5.twittnuker.constant.nameFirstKey
import de.vanita5.twittnuker.extension.rawQuery
import de.vanita5.twittnuker.model.*
import de.vanita5.twittnuker.model.util.ParcelableActivityUtils
import de.vanita5.twittnuker.provider.TwidereDataStore
import de.vanita5.twittnuker.provider.TwidereDataStore.Activities
import de.vanita5.twittnuker.provider.TwidereDataStore.Statuses
import de.vanita5.twittnuker.receiver.NotificationReceiver
import de.vanita5.twittnuker.util.database.FilterQueryBuilder
import org.oshkimaadziig.george.androidutils.SpanFormatter
import java.io.IOException

class ContentNotificationManager(
        val context: Context,
        val activityTracker: ActivityTracker,
        val userColorNameManager: UserColorNameManager,
        val notificationManager: NotificationManagerWrapper,
        val preferences: SharedPreferences
) {

    private var nameFirst: Boolean = false
    private var useStarForLikes: Boolean = false

    fun showInteractions(pref: AccountPreferences, position: Long) {
        val cr = context.contentResolver
        val accountKey = pref.accountKey
        val where = Expression.and(
                Expression.equalsArgs(Activities.ACCOUNT_KEY),
                Expression.greaterThanArgs(Activities.POSITION_KEY)
        ).sql
        val whereArgs = arrayOf(accountKey.toString(), position.toString())
        val c = cr.query(Activities.AboutMe.CONTENT_URI, Activities.COLUMNS, where, whereArgs,
                OrderBy(Activities.TIMESTAMP, false).sql) ?: return
        val builder = NotificationCompat.Builder(context)
        val pebbleNotificationStringBuilder = StringBuilder()
        try {
            val count = c.count
            if (count == 0) return
            builder.setSmallIcon(R.drawable.ic_stat_notification)
            builder.setCategory(NotificationCompat.CATEGORY_SOCIAL)
            applyNotificationPreferences(builder, pref, pref.mentionsNotificationType)

            val resources = context.resources
            val accountName = DataStoreUtils.getAccountDisplayName(context, accountKey, nameFirst)
            builder.setContentText(accountName)
            val style = NotificationCompat.InboxStyle()
            builder.setStyle(style)
            builder.setAutoCancel(true)
            style.setSummaryText(accountName)
            val ci = ParcelableActivityCursorIndices(c)
            var messageLines = 0

            var timestamp: Long = -1
            c.moveToPosition(-1)
            while (c.moveToNext()) {
                if (messageLines == 5) {
                    style.addLine(resources.getString(R.string.and_N_more, count - c.position))
                    pebbleNotificationStringBuilder.append(resources.getString(R.string.and_N_more, count - c.position))
                    break
                }
                val activity = ci.newObject(c)
                if (pref.isNotificationMentionsOnly && !ArrayUtils.contains(Activity.Action.MENTION_ACTIONS,
                        activity.action)) {
                    continue
                }
                if (activity.status_id != null && FilterQueryBuilder.isFiltered(cr,
                        activity.status_user_key, activity.status_text_plain,
                        activity.status_quote_text_plain, activity.status_spans,
                        activity.status_quote_spans, activity.status_source,
                        activity.status_quote_source, activity.status_retweeted_by_user_key,
                        activity.status_quoted_user_key)) {
                    continue
                }
                val filteredUserIds = DataStoreUtils.getFilteredUserIds(context)
                if (timestamp == -1L) {
                    timestamp = activity.timestamp
                }
                ParcelableActivityUtils.initAfterFilteredSourceIds(activity, filteredUserIds,
                        pref.isNotificationFollowingOnly)
                val sources = ParcelableActivityUtils.getAfterFilteredSources(activity)
                if (ArrayUtils.isEmpty(sources)) continue
                val message = ActivityTitleSummaryMessage.get(context,
                        userColorNameManager, activity, sources,
                        0, useStarForLikes, nameFirst)
                if (message != null) {
                    val summary = message.summary
                    if (TextUtils.isEmpty(summary)) {
                        style.addLine(message.title)
                        pebbleNotificationStringBuilder.append(message.title)
                        pebbleNotificationStringBuilder.append("\n")
                    } else {
                        style.addLine(SpanFormatter.format(resources.getString(R.string.title_summary_line_format),
                                message.title, summary))
                        pebbleNotificationStringBuilder.append(message.title)
                        pebbleNotificationStringBuilder.append(": ")
                        pebbleNotificationStringBuilder.append(message.summary)
                        pebbleNotificationStringBuilder.append("\n")
                    }
                    messageLines++
                }
            }
            if (messageLines == 0) return
            val displayCount = messageLines + count - c.position
            val title = resources.getQuantityString(R.plurals.N_new_interactions,
                    displayCount, displayCount)
            builder.setContentTitle(title)
            style.setBigContentTitle(title)
            builder.setNumber(displayCount)
            builder.setContentIntent(getContentIntent(context, CustomTabType.NOTIFICATIONS_TIMELINE,
                    NotificationType.INTERACTIONS, accountKey, timestamp))
            if (timestamp != -1L) {
                builder.setDeleteIntent(getMarkReadDeleteIntent(context,
                        NotificationType.INTERACTIONS, accountKey, timestamp, false))
            }
        } catch (e: IOException) {
            return
        } finally {
            c.close()
        }
        val notificationId = Utils.getNotificationId(NOTIFICATION_ID_INTERACTIONS_TIMELINE, accountKey)
        notificationManager.notify("interactions", notificationId, builder.build())

        Utils.sendPebbleNotification(context, context.resources.getString(R.string.interactions), pebbleNotificationStringBuilder.toString())

    }


    private fun applyNotificationPreferences(builder: NotificationCompat.Builder, pref: AccountPreferences, defaultFlags: Int) {
        var notificationDefaults = 0
        if (AccountPreferences.isNotificationHasLight(defaultFlags)) {
            notificationDefaults = notificationDefaults or NotificationCompat.DEFAULT_LIGHTS
        }
        if (isNotificationAudible()) {
            if (AccountPreferences.isNotificationHasVibration(defaultFlags)) {
                notificationDefaults = notificationDefaults or NotificationCompat.DEFAULT_VIBRATE
            } else {
                notificationDefaults = notificationDefaults and NotificationCompat.DEFAULT_VIBRATE.inv()
            }
            if (AccountPreferences.isNotificationHasRingtone(defaultFlags)) {
                builder.setSound(pref.notificationRingtone, AudioManager.STREAM_NOTIFICATION)
            }
        } else {
            notificationDefaults = notificationDefaults and (NotificationCompat.DEFAULT_VIBRATE or NotificationCompat.DEFAULT_SOUND).inv()
        }
        builder.color = pref.notificationLightColor
        builder.setDefaults(notificationDefaults)
        builder.setOnlyAlertOnce(true)
    }

    private fun isNotificationAudible(): Boolean {
        return !activityTracker.isHomeActivityStarted
    }

    fun showTimeline(pref: AccountPreferences, position: Long) {
        val accountKey = pref.accountKey
        val resources = context.resources
        val nm = notificationManager
        val selection = Expression.and(Expression.equalsArgs(Statuses.ACCOUNT_KEY),
                Expression.greaterThan(Statuses.POSITION_KEY, position))
        val filteredSelection = buildStatusFilterWhereClause(preferences,
                Statuses.TABLE_NAME, selection)
        val selectionArgs = arrayOf(accountKey.toString())
        val userProjection = arrayOf(Statuses.USER_KEY, Statuses.USER_NAME, Statuses.USER_SCREEN_NAME)
        val statusProjection = arrayOf(Statuses.POSITION_KEY)
        val statusCursor = context.contentResolver.query(Statuses.CONTENT_URI, statusProjection,
                filteredSelection.sql, selectionArgs, Statuses.DEFAULT_SORT_ORDER)

        val userCursor = context.contentResolver.rawQuery(SQLQueryBuilder.select(Columns(*userProjection))
                .from(Table(Statuses.TABLE_NAME))
                .where(filteredSelection)
                .groupBy(Columns.Column(Statuses.USER_KEY))
                .orderBy(OrderBy(Statuses.DEFAULT_SORT_ORDER)).buildSQL(), selectionArgs)

        try {
            val usersCount = userCursor.count
            val statusesCount = statusCursor.count
            if (statusesCount == 0 || usersCount == 0) return
            val statusIndices = ParcelableStatusCursorIndices(statusCursor)
            val userIndices = ParcelableStatusCursorIndices(userCursor)
            val positionKey = if (statusCursor.moveToFirst()) statusCursor.getLong(statusIndices.position_key) else -1L
            val notificationTitle = resources.getQuantityString(R.plurals.N_new_statuses,
                    statusesCount, statusesCount)
            val notificationContent: String
            userCursor.moveToFirst()
            val displayName = userColorNameManager.getDisplayName(userCursor.getString(userIndices.user_key),
                    userCursor.getString(userIndices.user_name), userCursor.getString(userIndices.user_screen_name),
                    nameFirst)
            if (usersCount == 1) {
                notificationContent = context.getString(R.string.from_name, displayName)
            } else if (usersCount == 2) {
                userCursor.moveToPosition(1)
                val othersName = userColorNameManager.getDisplayName(userCursor.getString(userIndices.user_key),
                        userCursor.getString(userIndices.user_name), userCursor.getString(userIndices.user_screen_name),
                        nameFirst)
                notificationContent = resources.getString(R.string.from_name_and_name, displayName, othersName)
            } else {
                notificationContent = resources.getString(R.string.from_name_and_N_others, displayName, usersCount - 1)
            }

            // Setup notification
            val builder = NotificationCompat.Builder(context)
            builder.setAutoCancel(true)
            builder.setSmallIcon(R.drawable.ic_stat_twitter)
            builder.setTicker(notificationTitle)
            builder.setContentTitle(notificationTitle)
            builder.setContentText(notificationContent)
            builder.setCategory(NotificationCompat.CATEGORY_SOCIAL)
            builder.setContentIntent(getContentIntent(context, CustomTabType.HOME_TIMELINE,
                    NotificationType.HOME_TIMELINE, accountKey, positionKey))
            builder.setDeleteIntent(getMarkReadDeleteIntent(context, NotificationType.HOME_TIMELINE,
                    accountKey, positionKey, false))
            builder.setNumber(statusesCount)
            builder.setCategory(NotificationCompat.CATEGORY_SOCIAL)
            applyNotificationPreferences(builder, pref, pref.homeTimelineNotificationType)
            try {
                nm.notify("home_" + accountKey, Utils.getNotificationId(NOTIFICATION_ID_HOME_TIMELINE, accountKey), builder.build())
                Utils.sendPebbleNotification(context, null, notificationContent)
            } catch (e: SecurityException) {
                // Silently ignore
            }

        } finally {
            statusCursor.close()
            userCursor.close()
        }
    }


    private fun getContentIntent(context: Context, @CustomTabType type: String,
                                 @NotificationType notificationType: String, accountKey: UserKey?, readPosition: Long): PendingIntent {
        // Setup click intent
        val homeIntent = Intent(context, HomeActivity::class.java)
        val homeLinkBuilder = Uri.Builder()
        homeLinkBuilder.scheme(SCHEME_TWITTNUKER)
        homeLinkBuilder.authority(type)
        if (accountKey != null)
            homeLinkBuilder.appendQueryParameter(QUERY_PARAM_ACCOUNT_KEY, accountKey.toString())
        homeLinkBuilder.appendQueryParameter(QUERY_PARAM_FROM_NOTIFICATION, true.toString())
        homeLinkBuilder.appendQueryParameter(QUERY_PARAM_TIMESTAMP, System.currentTimeMillis().toString())
        homeLinkBuilder.appendQueryParameter(QUERY_PARAM_NOTIFICATION_TYPE, notificationType)
        if (readPosition > 0) {
            homeLinkBuilder.appendQueryParameter(QUERY_PARAM_READ_POSITION, readPosition.toString())
        }
        homeIntent.data = homeLinkBuilder.build()
        homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        return PendingIntent.getActivity(context, 0, homeIntent, 0)
    }


    fun setNotificationUri(c: Cursor?, uri: Uri?) {
        val cr = context.contentResolver
        if (cr == null || c == null || uri == null) return
        c.setNotificationUri(cr, uri)
    }

    private fun updatePreferences() {
        nameFirst = preferences[nameFirstKey]
        useStarForLikes = preferences[iWantMyStarsBackKey]
    }


    private fun getMarkReadDeleteIntent(context: Context, @NotificationType notificationType: String,
                                        accountKey: UserKey?, positions: Array<StringLongPair>): PendingIntent {
        // Setup delete intent
        val intent = Intent(context, NotificationReceiver::class.java)
        val linkBuilder = Uri.Builder()
        linkBuilder.scheme(SCHEME_TWITTNUKER)
        linkBuilder.authority(AUTHORITY_INTERACTIONS)
        linkBuilder.appendPath(notificationType)
        if (accountKey != null) {
            linkBuilder.appendQueryParameter(QUERY_PARAM_ACCOUNT_KEY, accountKey.toString())
        }
        linkBuilder.appendQueryParameter(QUERY_PARAM_READ_POSITIONS, StringLongPair.toString(positions))
        linkBuilder.appendQueryParameter(QUERY_PARAM_TIMESTAMP, System.currentTimeMillis().toString())
        linkBuilder.appendQueryParameter(QUERY_PARAM_NOTIFICATION_TYPE, notificationType)
        intent.data = linkBuilder.build()
        return PendingIntent.getBroadcast(context, 0, intent, 0)
    }


    private fun getMarkReadDeleteIntent(context: Context, @NotificationType type: String,
                                        accountKey: UserKey?, position: Long,
                                        extraUserFollowing: Boolean): PendingIntent {
        return getMarkReadDeleteIntent(context, type, accountKey, position, -1, -1, extraUserFollowing)
    }

    private fun getMarkReadDeleteIntent(context: Context, @NotificationType type: String,
                                        accountKey: UserKey?, position: Long,
                                        extraId: Long, extraUserId: Long,
                                        extraUserFollowing: Boolean): PendingIntent {
        // Setup delete intent
        val intent = Intent(context, NotificationReceiver::class.java)
        intent.action = IntentConstants.BROADCAST_NOTIFICATION_DELETED
        val linkBuilder = Uri.Builder()
        linkBuilder.scheme(SCHEME_TWITTNUKER)
        linkBuilder.authority(AUTHORITY_INTERACTIONS)
        linkBuilder.appendPath(type)
        if (accountKey != null) {
            linkBuilder.appendQueryParameter(QUERY_PARAM_ACCOUNT_KEY, accountKey.toString())
        }
        linkBuilder.appendQueryParameter(QUERY_PARAM_READ_POSITION, position.toString())
        linkBuilder.appendQueryParameter(QUERY_PARAM_TIMESTAMP, System.currentTimeMillis().toString())
        linkBuilder.appendQueryParameter(QUERY_PARAM_NOTIFICATION_TYPE, type)

        UriExtraUtils.addExtra(linkBuilder, "item_id", extraId)
        UriExtraUtils.addExtra(linkBuilder, "item_user_id", extraUserId)
        UriExtraUtils.addExtra(linkBuilder, "item_user_following", extraUserFollowing)
        intent.data = linkBuilder.build()
        return PendingIntent.getBroadcast(context, 0, intent, 0)
    }
}