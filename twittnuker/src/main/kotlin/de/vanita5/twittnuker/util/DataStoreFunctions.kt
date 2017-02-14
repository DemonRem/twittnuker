package de.vanita5.twittnuker.util

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.support.annotation.WorkerThread
import android.support.v4.util.LongSparseArray
import org.mariotaku.kpreferences.get
import org.mariotaku.ktextension.useCursor
import org.mariotaku.sqliteqb.library.*
import de.vanita5.twittnuker.constant.filterPossibilitySensitiveStatusesKey
import de.vanita5.twittnuker.constant.filterUnavailableQuoteStatusesKey
import de.vanita5.twittnuker.model.*
import de.vanita5.twittnuker.model.ParcelableStatus.FilterFlags
import de.vanita5.twittnuker.provider.TwidereDataStore.*
import de.vanita5.twittnuker.util.DataStoreUtils.ACTIVITIES_URIS
import java.io.IOException

fun buildStatusFilterWhereClause(preferences: SharedPreferences,
                                 table: String,
                                 extraSelection: Expression?): Expression {
    val filteredUsersQuery = SQLQueryBuilder
            .select(Columns.Column(Table(Filters.Users.TABLE_NAME), Filters.Users.USER_KEY))
            .from(Tables(Filters.Users.TABLE_NAME))
            .build()
    val filteredUsersWhere = Expression.or(
            Expression.`in`(Columns.Column(Table(table), Statuses.USER_KEY), filteredUsersQuery),
            Expression.`in`(Columns.Column(Table(table), Statuses.RETWEETED_BY_USER_KEY), filteredUsersQuery),
            Expression.`in`(Columns.Column(Table(table), Statuses.QUOTED_USER_KEY), filteredUsersQuery)
    )
    val filteredIdsQueryBuilder = SQLQueryBuilder
            .select(Columns.Column(Table(table), Statuses._ID))
            .from(Tables(table))
            .where(filteredUsersWhere)
            .union()
            .select(Columns(Columns.Column(Table(table), Statuses._ID)))
            .from(Tables(table, Filters.Sources.TABLE_NAME))
            .where(Expression.or(
                    Expression.likeRaw(Columns.Column(Table(table), Statuses.SOURCE),
                            "'%>'||" + Filters.Sources.TABLE_NAME + "." + Filters.Sources.VALUE + "||'</a>%'"),
                    Expression.likeRaw(Columns.Column(Table(table), Statuses.QUOTED_SOURCE),
                            "'%>'||" + Filters.Sources.TABLE_NAME + "." + Filters.Sources.VALUE + "||'</a>%'")
            ))
            .union()
            .select(Columns(Columns.Column(Table(table), Statuses._ID)))
            .from(Tables(table, Filters.Keywords.TABLE_NAME))
            .where(Expression.or(
                    Expression.likeRaw(Columns.Column(Table(table), Statuses.TEXT_PLAIN),
                            "'%'||" + Filters.Keywords.TABLE_NAME + "." + Filters.Keywords.VALUE + "||'%'"),
                    Expression.likeRaw(Columns.Column(Table(table), Statuses.QUOTED_TEXT_PLAIN),
                            "'%'||" + Filters.Keywords.TABLE_NAME + "." + Filters.Keywords.VALUE + "||'%'")
            ))
            .union()
            .select(Columns(Columns.Column(Table(table), Statuses._ID)))
            .from(Tables(table, Filters.Links.TABLE_NAME))
            .where(Expression.or(
                    Expression.likeRaw(Columns.Column(Table(table), Statuses.SPANS),
                            "'%'||" + Filters.Links.TABLE_NAME + "." + Filters.Links.VALUE + "||'%'"),
                    Expression.likeRaw(Columns.Column(Table(table), Statuses.QUOTED_SPANS),
                            "'%'||" + Filters.Links.TABLE_NAME + "." + Filters.Links.VALUE + "||'%'")
            ))
    var filterFlags: Long = 0
    if (preferences[filterUnavailableQuoteStatusesKey]) {
        filterFlags = filterFlags or FilterFlags.QUOTE_NOT_AVAILABLE
    }
    if (preferences[filterPossibilitySensitiveStatusesKey]) {
        filterFlags = filterFlags or FilterFlags.POSSIBILITY_SENSITIVE
    }

    val filterExpression = Expression.or(
            Expression.and(
                    Expression("(" + Statuses.FILTER_FLAGS + " & " + filterFlags + ") == 0"),
                    Expression.notIn(Columns.Column(Table(table), Statuses._ID), filteredIdsQueryBuilder.build())
            ),
            Expression.equals(Columns.Column(Table(table), Statuses.IS_GAP), 1)
    )
    if (extraSelection != null) {
        return Expression.and(filterExpression, extraSelection)
    }
    return filterExpression
}

@SuppressLint("Recycle")
fun deleteDrafts(context: Context, draftIds: LongArray): Int {
    val where = Expression.inArgs(Drafts._ID, draftIds.size).sql
    val whereArgs = draftIds.map(Long::toString).toTypedArray()

    context.contentResolver.query(Drafts.CONTENT_URI, Drafts.COLUMNS, where, whereArgs,
            null).useCursor { cursor ->
        val indices = DraftCursorIndices(cursor)
        cursor.moveToFirst()
        while (!cursor.isAfterLast) {
            val draft = indices.newObject(cursor)
            draft.media?.forEach { item ->
                Utils.deleteMedia(context, Uri.parse(item.uri))
            }
            cursor.moveToNext()
        }
    }
    return context.contentResolver.delete(Drafts.CONTENT_URI, where, whereArgs)
}

fun deleteAccountData(resolver: ContentResolver, accountKey: UserKey) {
    val where = Expression.equalsArgs(AccountSupportColumns.ACCOUNT_KEY).sql
    val whereArgs = arrayOf(accountKey.toString())
    // Also delete tweets related to the account we previously
    // deleted.
    resolver.delete(Statuses.CONTENT_URI, where, whereArgs)
    resolver.delete(Mentions.CONTENT_URI, where, whereArgs)
    resolver.delete(Activities.AboutMe.CONTENT_URI, where, whereArgs)
    resolver.delete(Messages.CONTENT_URI, where, whereArgs)
    resolver.delete(Messages.Conversations.CONTENT_URI, where, whereArgs)
}


fun deleteActivityStatus(cr: ContentResolver, accountKey: UserKey,
                         statusId: String, result: ParcelableStatus?) {

    val host = accountKey.host
    val deleteWhere: String
    val updateWhere: String
    val deleteWhereArgs: Array<String>
    val updateWhereArgs: Array<String>
    if (host != null) {
        deleteWhere = Expression.and(
                Expression.likeRaw(Columns.Column(Activities.ACCOUNT_KEY), "'%@'||?"),
                Expression.or(
                        Expression.equalsArgs(Activities.STATUS_ID),
                        Expression.equalsArgs(Activities.STATUS_RETWEET_ID)
                )).sql
        deleteWhereArgs = arrayOf(host, statusId, statusId)
        updateWhere = Expression.and(
                Expression.likeRaw(Columns.Column(Activities.ACCOUNT_KEY), "'%@'||?"),
                Expression.equalsArgs(Activities.STATUS_MY_RETWEET_ID)
        ).sql
        updateWhereArgs = arrayOf(host, statusId)
    } else {
        deleteWhere = Expression.or(
                Expression.equalsArgs(Activities.STATUS_ID),
                Expression.equalsArgs(Activities.STATUS_RETWEET_ID)
        ).sql
        deleteWhereArgs = arrayOf(statusId, statusId)
        updateWhere = Expression.equalsArgs(Activities.STATUS_MY_RETWEET_ID).sql
        updateWhereArgs = arrayOf(statusId)
    }
    for (uri in ACTIVITIES_URIS) {
        cr.delete(uri, deleteWhere, deleteWhereArgs)
        updateActivity(cr, uri, updateWhere, updateWhereArgs) { activity ->
            activity.status_my_retweet_id = null
            arrayOf(activity.target_statuses, activity.target_object_statuses).filterNotNull().forEach {
                for (status in it) {
                    if (statusId == status.id || statusId == status.retweet_id || statusId == status.my_retweet_id) {
                        status.my_retweet_id = null
                        if (result != null) {
                            status.reply_count = result.reply_count
                            status.retweet_count = result.retweet_count - 1
                            status.favorite_count = result.favorite_count
                        }
                    }
                }
            }
        }
    }
}

fun updateActivityStatus(resolver: ContentResolver,
                         accountKey: UserKey,
                         statusId: String,
                         action: (ParcelableActivity) -> Unit) {
    val activityWhere = Expression.and(
            Expression.equalsArgs(Activities.ACCOUNT_KEY),
            Expression.or(
                    Expression.equalsArgs(Activities.STATUS_ID),
                    Expression.equalsArgs(Activities.STATUS_RETWEET_ID)
            )
    ).sql
    val activityWhereArgs = arrayOf(accountKey.toString(), statusId, statusId)
    for (uri in ACTIVITIES_URIS) {
        updateActivity(resolver, uri, activityWhere, activityWhereArgs, action)
    }
}


@WorkerThread
fun updateActivity(cr: ContentResolver, uri: Uri,
                   where: String?, whereArgs: Array<String>?,
                   action: (ParcelableActivity) -> Unit) {
    val c = cr.query(uri, Activities.COLUMNS, where, whereArgs, null) ?: return
    val values = LongSparseArray<ContentValues>()
    try {
        val ci = ParcelableActivityCursorIndices(c)
        c.moveToFirst()
        while (!c.isAfterLast) {
            val activity = ci.newObject(c)
            action(activity)
            values.put(activity._id, ParcelableActivityValuesCreator.create(activity))
            c.moveToNext()
        }
    } catch (e: IOException) {
        return
    } finally {
        c.close()
    }
    val updateWhere = Expression.equalsArgs(Activities._ID).sql
    val updateWhereArgs = arrayOfNulls<String>(1)
    for (i in 0 until values.size()) {
        updateWhereArgs[0] = values.keyAt(i).toString()
        cr.update(uri, values.valueAt(i), updateWhere, updateWhereArgs)
    }
}