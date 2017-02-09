package de.vanita5.twittnuker.util

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import org.mariotaku.kpreferences.get
import org.mariotaku.ktextension.useCursor
import org.mariotaku.sqliteqb.library.*
import de.vanita5.twittnuker.constant.filterPossibilitySensitiveStatusesKey
import de.vanita5.twittnuker.constant.filterUnavailableQuoteStatusesKey
import de.vanita5.twittnuker.model.DraftCursorIndices
import de.vanita5.twittnuker.model.ParcelableStatus.FilterFlags
import de.vanita5.twittnuker.model.UserKey
import de.vanita5.twittnuker.provider.TwidereDataStore.*

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
    resolver.delete(DirectMessages.Inbox.CONTENT_URI, where, whereArgs)
    resolver.delete(DirectMessages.Outbox.CONTENT_URI, where, whereArgs)
}