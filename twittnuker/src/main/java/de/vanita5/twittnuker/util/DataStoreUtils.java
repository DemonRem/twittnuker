/*
 * Twittnuker - Twitter client for Android
 *
 * Copyright (C) 2013-2016 vanita5 <mail@vanit.as>
 *
 * This program incorporates a modified version of Twidere.
 * Copyright (C) 2012-2016 Mariotaku Lee <mariotaku.lee@gmail.com>
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

package de.vanita5.twittnuker.util;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.support.v4.util.LongSparseArray;
import android.text.TextUtils;

import com.bluelinelabs.logansquare.JsonMapper;
import com.bluelinelabs.logansquare.LoganSquare;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import de.vanita5.twittnuker.library.twitter.model.Activity;
import org.mariotaku.sqliteqb.library.ArgsArray;
import org.mariotaku.sqliteqb.library.Columns;
import org.mariotaku.sqliteqb.library.Columns.Column;
import org.mariotaku.sqliteqb.library.Expression;
import org.mariotaku.sqliteqb.library.OrderBy;
import org.mariotaku.sqliteqb.library.SQLFunctions;
import org.mariotaku.sqliteqb.library.SQLQueryBuilder;
import org.mariotaku.sqliteqb.library.Table;
import org.mariotaku.sqliteqb.library.Tables;
import org.mariotaku.sqliteqb.library.query.SQLSelectQuery;
import de.vanita5.twittnuker.Constants;
import de.vanita5.twittnuker.TwittnukerConstants;
import de.vanita5.twittnuker.extension.AccountExtensionsKt;
import de.vanita5.twittnuker.model.ParcelableActivity;
import de.vanita5.twittnuker.model.ParcelableActivityCursorIndices;
import de.vanita5.twittnuker.model.ParcelableActivityValuesCreator;
import de.vanita5.twittnuker.model.ParcelableStatus;
import de.vanita5.twittnuker.model.ParcelableUser;
import de.vanita5.twittnuker.model.UserFollowState;
import de.vanita5.twittnuker.model.UserKey;
import de.vanita5.twittnuker.model.tab.extra.HomeTabExtras;
import de.vanita5.twittnuker.model.tab.extra.InteractionsTabExtras;
import de.vanita5.twittnuker.model.tab.extra.TabExtras;
import de.vanita5.twittnuker.model.util.AccountUtils;
import de.vanita5.twittnuker.provider.TwidereDataStore;
import de.vanita5.twittnuker.provider.TwidereDataStore.AccountSupportColumns;
import de.vanita5.twittnuker.provider.TwidereDataStore.Accounts;
import de.vanita5.twittnuker.provider.TwidereDataStore.Activities;
import de.vanita5.twittnuker.provider.TwidereDataStore.CacheFiles;
import de.vanita5.twittnuker.provider.TwidereDataStore.CachedHashtags;
import de.vanita5.twittnuker.provider.TwidereDataStore.CachedImages;
import de.vanita5.twittnuker.provider.TwidereDataStore.CachedRelationships;
import de.vanita5.twittnuker.provider.TwidereDataStore.CachedStatuses;
import de.vanita5.twittnuker.provider.TwidereDataStore.CachedTrends;
import de.vanita5.twittnuker.provider.TwidereDataStore.CachedUsers;
import de.vanita5.twittnuker.provider.TwidereDataStore.DNS;
import de.vanita5.twittnuker.provider.TwidereDataStore.DirectMessages;
import de.vanita5.twittnuker.provider.TwidereDataStore.Drafts;
import de.vanita5.twittnuker.provider.TwidereDataStore.Filters;
import de.vanita5.twittnuker.provider.TwidereDataStore.Notifications;
import de.vanita5.twittnuker.provider.TwidereDataStore.Preferences;
import de.vanita5.twittnuker.provider.TwidereDataStore.PushNotifications;
import de.vanita5.twittnuker.provider.TwidereDataStore.SavedSearches;
import de.vanita5.twittnuker.provider.TwidereDataStore.SearchHistory;
import de.vanita5.twittnuker.provider.TwidereDataStore.Statuses;
import de.vanita5.twittnuker.provider.TwidereDataStore.Suggestions;
import de.vanita5.twittnuker.provider.TwidereDataStore.Tabs;
import de.vanita5.twittnuker.provider.TwidereDataStore.UnreadCounts;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.text.TextUtils.isEmpty;
import static de.vanita5.twittnuker.provider.TwidereDataStore.ACTIVITIES_URIS;
import static de.vanita5.twittnuker.provider.TwidereDataStore.CACHE_URIS;
import static de.vanita5.twittnuker.provider.TwidereDataStore.DIRECT_MESSAGES_URIS;
import static de.vanita5.twittnuker.provider.TwidereDataStore.STATUSES_URIS;

public class DataStoreUtils implements Constants {
    static final UriMatcher CONTENT_PROVIDER_URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
    static Map<UserKey, String> sAccountScreenNames = new HashMap<>();
    static Map<UserKey, String> sAccountNames = new HashMap<>();

    static {
        CONTENT_PROVIDER_URI_MATCHER.addURI(TwidereDataStore.AUTHORITY, Accounts.CONTENT_PATH,
                TABLE_ID_ACCOUNTS);
        CONTENT_PROVIDER_URI_MATCHER.addURI(TwidereDataStore.AUTHORITY, Statuses.CONTENT_PATH,
                TABLE_ID_STATUSES);
        CONTENT_PROVIDER_URI_MATCHER.addURI(TwidereDataStore.AUTHORITY, Activities.AboutMe.CONTENT_PATH,
                TABLE_ID_ACTIVITIES_ABOUT_ME);
        CONTENT_PROVIDER_URI_MATCHER.addURI(TwidereDataStore.AUTHORITY, Activities.ByFriends.CONTENT_PATH,
                TABLE_ID_ACTIVITIES_BY_FRIENDS);
        CONTENT_PROVIDER_URI_MATCHER.addURI(TwidereDataStore.AUTHORITY, Drafts.CONTENT_PATH,
                TABLE_ID_DRAFTS);
        CONTENT_PROVIDER_URI_MATCHER.addURI(TwidereDataStore.AUTHORITY, CachedUsers.CONTENT_PATH,
                TABLE_ID_CACHED_USERS);
        CONTENT_PROVIDER_URI_MATCHER.addURI(TwidereDataStore.AUTHORITY, Filters.Users.CONTENT_PATH,
                TABLE_ID_FILTERED_USERS);
        CONTENT_PROVIDER_URI_MATCHER.addURI(TwidereDataStore.AUTHORITY, Filters.Keywords.CONTENT_PATH,
                TABLE_ID_FILTERED_KEYWORDS);
        CONTENT_PROVIDER_URI_MATCHER.addURI(TwidereDataStore.AUTHORITY, Filters.Sources.CONTENT_PATH,
                TABLE_ID_FILTERED_SOURCES);
        CONTENT_PROVIDER_URI_MATCHER.addURI(TwidereDataStore.AUTHORITY, Filters.Links.CONTENT_PATH,
                TABLE_ID_FILTERED_LINKS);
        CONTENT_PROVIDER_URI_MATCHER.addURI(TwidereDataStore.AUTHORITY, DirectMessages.CONTENT_PATH,
                TABLE_ID_DIRECT_MESSAGES);
        CONTENT_PROVIDER_URI_MATCHER.addURI(TwidereDataStore.AUTHORITY, DirectMessages.Inbox.CONTENT_PATH,
                TABLE_ID_DIRECT_MESSAGES_INBOX);
        CONTENT_PROVIDER_URI_MATCHER.addURI(TwidereDataStore.AUTHORITY, DirectMessages.Outbox.CONTENT_PATH,
                TABLE_ID_DIRECT_MESSAGES_OUTBOX);
        CONTENT_PROVIDER_URI_MATCHER.addURI(TwidereDataStore.AUTHORITY, DirectMessages.Conversation.CONTENT_PATH + "/*/*",
                TABLE_ID_DIRECT_MESSAGES_CONVERSATION);
        CONTENT_PROVIDER_URI_MATCHER.addURI(TwidereDataStore.AUTHORITY, DirectMessages.Conversation.CONTENT_PATH_SCREEN_NAME + "/*/*",
                TABLE_ID_DIRECT_MESSAGES_CONVERSATION_SCREEN_NAME);
        CONTENT_PROVIDER_URI_MATCHER.addURI(TwidereDataStore.AUTHORITY, DirectMessages.ConversationEntries.CONTENT_PATH,
                TABLE_ID_DIRECT_MESSAGES_CONVERSATIONS_ENTRIES);
        CONTENT_PROVIDER_URI_MATCHER.addURI(TwidereDataStore.AUTHORITY, CachedTrends.Local.CONTENT_PATH,
                TABLE_ID_TRENDS_LOCAL);
        CONTENT_PROVIDER_URI_MATCHER.addURI(TwidereDataStore.AUTHORITY, Tabs.CONTENT_PATH,
                TABLE_ID_TABS);
        CONTENT_PROVIDER_URI_MATCHER.addURI(TwidereDataStore.AUTHORITY, CachedStatuses.CONTENT_PATH,
                TABLE_ID_CACHED_STATUSES);
        CONTENT_PROVIDER_URI_MATCHER.addURI(TwidereDataStore.AUTHORITY, CachedHashtags.CONTENT_PATH,
                TABLE_ID_CACHED_HASHTAGS);
        CONTENT_PROVIDER_URI_MATCHER.addURI(TwidereDataStore.AUTHORITY, CachedRelationships.CONTENT_PATH,
                TABLE_ID_CACHED_RELATIONSHIPS);
        CONTENT_PROVIDER_URI_MATCHER.addURI(TwidereDataStore.AUTHORITY, SavedSearches.CONTENT_PATH,
                TABLE_ID_SAVED_SEARCHES);
        CONTENT_PROVIDER_URI_MATCHER.addURI(TwidereDataStore.AUTHORITY, SearchHistory.CONTENT_PATH,
                TABLE_ID_SEARCH_HISTORY);

        CONTENT_PROVIDER_URI_MATCHER.addURI(TwidereDataStore.AUTHORITY, Notifications.CONTENT_PATH,
                VIRTUAL_TABLE_ID_NOTIFICATIONS);
        CONTENT_PROVIDER_URI_MATCHER.addURI(TwidereDataStore.AUTHORITY, Notifications.CONTENT_PATH + "/#",
                VIRTUAL_TABLE_ID_NOTIFICATIONS);
        CONTENT_PROVIDER_URI_MATCHER.addURI(TwidereDataStore.AUTHORITY, Notifications.CONTENT_PATH + "/#/*",
                VIRTUAL_TABLE_ID_NOTIFICATIONS);
        CONTENT_PROVIDER_URI_MATCHER.addURI(TwidereDataStore.AUTHORITY, DNS.CONTENT_PATH + "/*",
                VIRTUAL_TABLE_ID_DNS);
        CONTENT_PROVIDER_URI_MATCHER.addURI(TwidereDataStore.AUTHORITY, CachedImages.CONTENT_PATH,
                VIRTUAL_TABLE_ID_CACHED_IMAGES);
        CONTENT_PROVIDER_URI_MATCHER.addURI(TwidereDataStore.AUTHORITY, CacheFiles.CONTENT_PATH + "/*",
                VIRTUAL_TABLE_ID_CACHE_FILES);
        CONTENT_PROVIDER_URI_MATCHER.addURI(TwidereDataStore.AUTHORITY, Preferences.CONTENT_PATH,
                VIRTUAL_TABLE_ID_ALL_PREFERENCES);
        CONTENT_PROVIDER_URI_MATCHER.addURI(TwidereDataStore.AUTHORITY, Preferences.CONTENT_PATH + "/*",
                VIRTUAL_TABLE_ID_PREFERENCES);
        CONTENT_PROVIDER_URI_MATCHER.addURI(TwidereDataStore.AUTHORITY, UnreadCounts.CONTENT_PATH,
                VIRTUAL_TABLE_ID_UNREAD_COUNTS);
        CONTENT_PROVIDER_URI_MATCHER.addURI(TwidereDataStore.AUTHORITY, UnreadCounts.CONTENT_PATH + "/#",
                VIRTUAL_TABLE_ID_UNREAD_COUNTS);
        CONTENT_PROVIDER_URI_MATCHER.addURI(TwidereDataStore.AUTHORITY, UnreadCounts.CONTENT_PATH + "/#/#/*",
                VIRTUAL_TABLE_ID_UNREAD_COUNTS);
        CONTENT_PROVIDER_URI_MATCHER.addURI(TwidereDataStore.AUTHORITY, UnreadCounts.ByType.CONTENT_PATH + "/*",
                VIRTUAL_TABLE_ID_UNREAD_COUNTS_BY_TYPE);
        CONTENT_PROVIDER_URI_MATCHER.addURI(TwidereDataStore.AUTHORITY, CachedUsers.CONTENT_PATH_WITH_RELATIONSHIP + "/*",
                VIRTUAL_TABLE_ID_CACHED_USERS_WITH_RELATIONSHIP);
        CONTENT_PROVIDER_URI_MATCHER.addURI(TwidereDataStore.AUTHORITY, CachedUsers.CONTENT_PATH_WITH_SCORE + "/*",
                VIRTUAL_TABLE_ID_CACHED_USERS_WITH_SCORE);
        CONTENT_PROVIDER_URI_MATCHER.addURI(TwidereDataStore.AUTHORITY, Drafts.CONTENT_PATH_UNSENT,
                VIRTUAL_TABLE_ID_DRAFTS_UNSENT);
        CONTENT_PROVIDER_URI_MATCHER.addURI(TwidereDataStore.AUTHORITY, Drafts.CONTENT_PATH_NOTIFICATIONS,
                VIRTUAL_TABLE_ID_DRAFTS_NOTIFICATIONS);
        CONTENT_PROVIDER_URI_MATCHER.addURI(TwidereDataStore.AUTHORITY, PushNotifications.CONTENT_PATH,
                TABLE_ID_PUSH_NOTIFICATIONS);
        CONTENT_PROVIDER_URI_MATCHER.addURI(TwidereDataStore.AUTHORITY, Drafts.CONTENT_PATH_NOTIFICATIONS,
                VIRTUAL_TABLE_ID_DRAFTS_NOTIFICATIONS);
        CONTENT_PROVIDER_URI_MATCHER.addURI(TwidereDataStore.AUTHORITY, Suggestions.AutoComplete.CONTENT_PATH,
                VIRTUAL_TABLE_ID_SUGGESTIONS_AUTO_COMPLETE);
        CONTENT_PROVIDER_URI_MATCHER.addURI(TwidereDataStore.AUTHORITY, Suggestions.Search.CONTENT_PATH,
                VIRTUAL_TABLE_ID_SUGGESTIONS_SEARCH);
        CONTENT_PROVIDER_URI_MATCHER.addURI(TwidereDataStore.AUTHORITY, TwidereDataStore.CONTENT_PATH_DATABASE_PREPARE,
                VIRTUAL_TABLE_ID_DATABASE_PREPARE);
        CONTENT_PROVIDER_URI_MATCHER.addURI(TwidereDataStore.AUTHORITY, TwidereDataStore.CONTENT_PATH_NULL,
                VIRTUAL_TABLE_ID_NULL);
        CONTENT_PROVIDER_URI_MATCHER.addURI(TwidereDataStore.AUTHORITY, TwidereDataStore.CONTENT_PATH_EMPTY,
                VIRTUAL_TABLE_ID_EMPTY);
        CONTENT_PROVIDER_URI_MATCHER.addURI(TwidereDataStore.AUTHORITY, TwidereDataStore.CONTENT_PATH_RAW_QUERY + "/*",
                VIRTUAL_TABLE_ID_RAW_QUERY);
    }

    @NonNull
    public static String[] getNewestMessageIds(@NonNull final Context context, @NonNull final Uri uri,
                                             @NonNull final UserKey[] accountKeys) {
        return getStringFieldArray(context, uri, accountKeys, DirectMessages.ACCOUNT_KEY,
                DirectMessages.MESSAGE_ID, new OrderBy(SQLFunctions.MAX(DirectMessages.MESSAGE_TIMESTAMP)));
    }

    @NonNull
    public static String[] getNewestStatusIds(@NonNull final Context context, @NonNull final Uri uri,
                                            @NonNull final UserKey[] accountKeys) {
        return getStringFieldArray(context, uri, accountKeys, Statuses.ACCOUNT_KEY,
                Statuses.STATUS_ID, new OrderBy(SQLFunctions.MAX(Statuses.STATUS_TIMESTAMP)));
    }


    @NonNull
    public static long[] getNewestStatusSortIds(@NonNull final Context context, @NonNull final Uri uri,
                                                @NonNull final UserKey[] accountKeys) {
        return getLongFieldArray(context, uri, accountKeys, Statuses.ACCOUNT_KEY,
                Statuses.SORT_ID, new OrderBy(SQLFunctions.MAX(Statuses.STATUS_TIMESTAMP)));
    }


    @NonNull
    public static String[] getOldestMessageIds(@NonNull final Context context, @NonNull final Uri uri,
                                             @NonNull final UserKey[] accountKeys) {
        return getStringFieldArray(context, uri, accountKeys, DirectMessages.ACCOUNT_KEY,
                DirectMessages.MESSAGE_ID, new OrderBy(SQLFunctions.MIN(DirectMessages.MESSAGE_TIMESTAMP)));
    }

    @NonNull
    public static String[] getOldestStatusIds(@NonNull final Context context, @NonNull final Uri uri,
                                            @NonNull final UserKey[] accountKeys) {
        return getStringFieldArray(context, uri, accountKeys, Statuses.ACCOUNT_KEY,
                Statuses.STATUS_ID, new OrderBy(SQLFunctions.MIN(Statuses.STATUS_TIMESTAMP)));
    }


    @NonNull
    public static long[] getOldestStatusSortIds(@NonNull final Context context, @NonNull final Uri uri,
                                                @NonNull final UserKey[] accountKeys) {
        return getLongFieldArray(context, uri, accountKeys, Statuses.ACCOUNT_KEY,
                Statuses.SORT_ID, new OrderBy(SQLFunctions.MIN(Statuses.STATUS_TIMESTAMP)));
    }

    @NonNull
    public static String[] getNewestActivityMaxPositions(final Context context, final Uri uri,
                                                       final UserKey[] accountKeys) {
        return getStringFieldArray(context, uri, accountKeys, Activities.ACCOUNT_KEY,
                Activities.MAX_REQUEST_POSITION, new OrderBy(SQLFunctions.MAX(Activities.TIMESTAMP)));
    }

    @NonNull
    public static String[] getOldestActivityMaxPositions(@NonNull final Context context,
                                                       @NonNull final Uri uri,
                                                       @NonNull final UserKey[] accountKeys) {
        return getStringFieldArray(context, uri, accountKeys, Activities.ACCOUNT_KEY,
                Activities.MAX_REQUEST_POSITION, new OrderBy(SQLFunctions.MIN(Activities.TIMESTAMP)));
    }

    @NonNull
    public static long[] getNewestActivityMaxSortPositions(final Context context, final Uri uri,
                                                         final UserKey[] accountKeys) {
        return getLongFieldArray(context, uri, accountKeys, Activities.ACCOUNT_KEY,
                Activities.MAX_SORT_POSITION, new OrderBy(SQLFunctions.MAX(Activities.TIMESTAMP)));
    }

    @NonNull
    public static long[] getOldestActivityMaxSortPositions(@NonNull final Context context,
                                                         @NonNull final Uri uri,
                                                         @NonNull final UserKey[] accountKeys) {
        return getLongFieldArray(context, uri, accountKeys, Activities.ACCOUNT_KEY,
                Activities.MAX_SORT_POSITION, new OrderBy(SQLFunctions.MIN(Activities.TIMESTAMP)));
    }

    public static int getStatusCount(final Context context, final Uri uri, final UserKey accountId) {
        final String where = Expression.equalsArgs(AccountSupportColumns.ACCOUNT_KEY).getSQL();
        final String[] whereArgs = {accountId.toString()};
        return queryCount(context, uri, where, whereArgs);
    }

    public static int getActivitiesCount(@NonNull final Context context, @NonNull final Uri uri,
                                         @NonNull final UserKey accountKey) {
        final String where = Expression.equalsArgs(AccountSupportColumns.ACCOUNT_KEY).getSQL();
        return queryCount(context, uri, where, new String[]{accountKey.toString()});
    }


    @NonNull
    public static UserKey[] getFilteredUserIds(Context context) {
        if (context == null) return new UserKey[0];
        final ContentResolver resolver = context.getContentResolver();
        final String[] projection = {Filters.Users.USER_KEY};
        final Cursor cur = resolver.query(Filters.Users.CONTENT_URI, projection, null, null, null);
        if (cur == null) return new UserKey[0];
        try {
            final UserKey[] ids = new UserKey[cur.getCount()];
            cur.moveToFirst();
            int i = 0;
            while (!cur.isAfterLast()) {
                ids[i] = UserKey.valueOf(cur.getString(0));
                cur.moveToNext();
                i++;
            }
            cur.close();
            return ids;
        } finally {
            cur.close();
        }
    }

    @NonNull
    public static Expression buildStatusFilterWhereClause(@NonNull final String table, final Expression extraSelection) {
        final SQLSelectQuery filteredUsersQuery = SQLQueryBuilder
                .select(new Column(new Table(Filters.Users.TABLE_NAME), Filters.Users.USER_KEY))
                .from(new Tables(Filters.Users.TABLE_NAME))
                .build();
        final Expression filteredUsersWhere = Expression.or(
                Expression.in(new Column(new Table(table), Statuses.USER_KEY), filteredUsersQuery),
                Expression.in(new Column(new Table(table), Statuses.RETWEETED_BY_USER_KEY), filteredUsersQuery),
                Expression.in(new Column(new Table(table), Statuses.QUOTED_USER_KEY), filteredUsersQuery)
        );
        final SQLSelectQuery.Builder filteredIdsQueryBuilder = SQLQueryBuilder
                .select(new Column(new Table(table), Statuses._ID))
                .from(new Tables(table))
                .where(filteredUsersWhere)
                .union()
                .select(new Columns(new Column(new Table(table), Statuses._ID)))
                .from(new Tables(table, Filters.Sources.TABLE_NAME))
                .where(Expression.or(
                        Expression.likeRaw(new Column(new Table(table), Statuses.SOURCE),
                                "'%>'||" + Filters.Sources.TABLE_NAME + "." + Filters.Sources.VALUE + "||'</a>%'"),
                        Expression.likeRaw(new Column(new Table(table), Statuses.QUOTED_SOURCE),
                                "'%>'||" + Filters.Sources.TABLE_NAME + "." + Filters.Sources.VALUE + "||'</a>%'")
                ))
                .union()
                .select(new Columns(new Column(new Table(table), Statuses._ID)))
                .from(new Tables(table, Filters.Keywords.TABLE_NAME))
                .where(Expression.or(
                        Expression.likeRaw(new Column(new Table(table), Statuses.TEXT_PLAIN),
                                "'%'||" + Filters.Keywords.TABLE_NAME + "." + Filters.Keywords.VALUE + "||'%'"),
                        Expression.likeRaw(new Column(new Table(table), Statuses.QUOTED_TEXT_PLAIN),
                                "'%'||" + Filters.Keywords.TABLE_NAME + "." + Filters.Keywords.VALUE + "||'%'")
                ))
                .union()
                .select(new Columns(new Column(new Table(table), Statuses._ID)))
                .from(new Tables(table, Filters.Links.TABLE_NAME))
                .where(Expression.or(
                        Expression.likeRaw(new Column(new Table(table), Statuses.SPANS),
                                "'%'||" + Filters.Links.TABLE_NAME + "." + Filters.Links.VALUE + "||'%'"),
                        Expression.likeRaw(new Column(new Table(table), Statuses.QUOTED_SPANS),
                                "'%'||" + Filters.Links.TABLE_NAME + "." + Filters.Links.VALUE + "||'%'")
                ));
        final Expression filterExpression = Expression.or(
                Expression.notIn(new Column(new Table(table), Statuses._ID), filteredIdsQueryBuilder.build()),
                Expression.equals(new Column(new Table(table), Statuses.IS_GAP), 1)
        );
        if (extraSelection != null) {
            return Expression.and(filterExpression, extraSelection);
        }
        return filterExpression;
    }


    public static String getAccountDisplayName(final Context context, final UserKey accountKey, final boolean nameFirst) {
        final String name;
        if (nameFirst) {
            name = getAccountName(context, accountKey);
        } else {
            name = String.format("@%s", getAccountScreenName(context, accountKey));
        }
        return name;
    }

    public static String getAccountName(@NonNull final Context context, final UserKey accountKey) {
        final String cached = sAccountNames.get(accountKey);
        if (!isEmpty(cached)) return cached;

        AccountManager am = AccountManager.get(context);
        Account account = AccountUtils.findByAccountKey(am, accountKey);
        if (account == null) return null;

        return AccountExtensionsKt.getAccountUser(account, am).name;
    }

    public static String getAccountScreenName(final Context context, final UserKey accountKey) {
        if (context == null) return null;
        final String cached = sAccountScreenNames.get(accountKey);
        if (!isEmpty(cached)) return cached;

        AccountManager am = AccountManager.get(context);
        Account account = AccountUtils.findByAccountKey(am, accountKey);
        if (account == null) return null;

        return AccountExtensionsKt.getAccountUser(account, am).screen_name;
    }

    @NonNull
    public static UserKey[] getActivatedAccountKeys(@NonNull final Context context) {
        AccountManager am = AccountManager.get(context);
        List<UserKey> keys = new ArrayList<>();
        for (Account account : AccountUtils.getAccounts(am)) {
            if (AccountExtensionsKt.isActivated(account, am)) {
                keys.add(AccountExtensionsKt.getAccountKey(account, am));
            }
        }
        return keys.toArray(new UserKey[keys.size()]);
    }

    public static int getStatusesCount(@NonNull final Context context, final Uri uri,
                                       @Nullable final Bundle extraArgs, final long compare,
                                       String compareColumn, boolean greaterThan,
                                       @Nullable UserKey[] accountKeys) {
        if (accountKeys == null) {
            accountKeys = getActivatedAccountKeys(context);
        }

        List<Expression> expressions = new ArrayList<>();
        List<String> expressionArgs = new ArrayList<>();

        expressions.add(Expression.inArgs(new Column(Statuses.ACCOUNT_KEY), accountKeys.length));
        for (UserKey accountKey : accountKeys) {
            expressionArgs.add(accountKey.toString());
        }

        if (greaterThan) {
            expressions.add(Expression.greaterThanArgs(compareColumn));
        } else {
            expressions.add(Expression.lesserThanArgs(compareColumn));
        }
        expressionArgs.add(String.valueOf(compare));

        expressions.add(buildStatusFilterWhereClause(getTableNameByUri(uri), null));

        if (extraArgs != null) {
            Parcelable extras = extraArgs.getParcelable(EXTRA_EXTRAS);
            if (extras instanceof HomeTabExtras) {
                processTabExtras(expressions, expressionArgs, (HomeTabExtras) extras);
            }
        }

        Expression selection = Expression.and(expressions.toArray(new Expression[expressions.size()]));
        return queryCount(context, uri, selection.getSQL(), expressionArgs.toArray(new String[expressionArgs.size()]));
    }

    public static int getActivitiesCount(final Context context, final Uri uri, final long compare,
                                       String compareColumn, boolean greaterThan, UserKey... accountKeys) {
        if (context == null) return 0;
        if (accountKeys == null) {
            accountKeys = getActivatedAccountKeys(context);
        }
        final Expression selection = Expression.and(
                Expression.inArgs(new Column(Activities.ACCOUNT_KEY), accountKeys.length),
                greaterThan ? Expression.greaterThanArgs(compareColumn) : Expression.lesserThanArgs(compareColumn),
                buildActivityFilterWhereClause(getTableNameByUri(uri), null)
        );
        final String[] whereArgs = new String[accountKeys.length + 1];
        for (int i = 0; i < accountKeys.length; i++) {
            whereArgs[i] = accountKeys[i].toString();
        }
        whereArgs[accountKeys.length] = String.valueOf(compare);
        return queryCount(context, uri, selection.getSQL(), whereArgs);
    }

    public static int getActivitiesCount(@NonNull final Context context, final Uri uri,
                                         final Expression extraWhere, final String[] extraWhereArgs,
                                         final long since, String sinceColumn, boolean followingOnly,
                                         @Nullable UserKey[] accountKeys) {
        if (accountKeys == null) {
            accountKeys = getActivatedAccountKeys(context);
        }
        Expression[] expressions;
        if (extraWhere != null) {
            expressions = new Expression[4];
            expressions[3] = extraWhere;
        } else {
            expressions = new Expression[3];
        }
        expressions[0] = Expression.inArgs(new Column(Activities.ACCOUNT_KEY), accountKeys.length);
        expressions[1] = Expression.greaterThanArgs(sinceColumn);
        expressions[2] = buildActivityFilterWhereClause(getTableNameByUri(uri), null);
        final Expression selection = Expression.and(expressions);
        String[] selectionArgs;
        if (extraWhereArgs != null) {
            selectionArgs = new String[accountKeys.length + 1 + extraWhereArgs.length];
            System.arraycopy(extraWhereArgs, 0, selectionArgs, accountKeys.length + 1,
                    extraWhereArgs.length);
        } else {
            selectionArgs = new String[accountKeys.length + 1];
        }
        for (int i = 0; i < accountKeys.length; i++) {
            selectionArgs[i] = accountKeys[i].toString();
        }
        selectionArgs[accountKeys.length] = String.valueOf(since);
        // If followingOnly option is on, we have to iterate over items
        if (followingOnly) {
            final ContentResolver resolver = context.getContentResolver();
            final String[] projection = new String[]{Activities.SOURCES};
            final Cursor cur = resolver.query(uri, projection, selection.getSQL(), selectionArgs, null);
            if (cur == null) return -1;
            try {
                final JsonMapper<UserFollowState> mapper = LoganSquare.mapperFor(UserFollowState.class);
                int total = 0;
                cur.moveToFirst();
                while (!cur.isAfterLast()) {
                    final String string = cur.getString(0);
                    if (TextUtils.isEmpty(string)) continue;
                    boolean hasFollowing = false;
                    try {
                        for (UserFollowState state : mapper.parseList(string)) {
                            if (state.is_following) {
                                hasFollowing = true;
                                break;
                            }
                        }
                    } catch (IOException e) {
                        continue;
                    }
                    if (hasFollowing) {
                        total++;
                    }
                    cur.moveToNext();
                }
                return total;
            } finally {
                cur.close();
            }
        }
        return queryCount(context, uri, selection.getSQL(), selectionArgs);
    }

    public static int getTableId(final Uri uri) {
        if (uri == null) return -1;
        return CONTENT_PROVIDER_URI_MATCHER.match(uri);
    }

    public static String getTableNameById(final int id) {
        switch (id) {
            case TwittnukerConstants.TABLE_ID_ACCOUNTS:
                return Accounts.TABLE_NAME;
            case TwittnukerConstants.TABLE_ID_STATUSES:
                return Statuses.TABLE_NAME;
            case TwittnukerConstants.TABLE_ID_ACTIVITIES_ABOUT_ME:
                return Activities.AboutMe.TABLE_NAME;
            case TwittnukerConstants.TABLE_ID_ACTIVITIES_BY_FRIENDS:
                return Activities.ByFriends.TABLE_NAME;
            case TwittnukerConstants.TABLE_ID_DRAFTS:
                return Drafts.TABLE_NAME;
            case TwittnukerConstants.TABLE_ID_FILTERED_USERS:
                return Filters.Users.TABLE_NAME;
            case TwittnukerConstants.TABLE_ID_FILTERED_KEYWORDS:
                return Filters.Keywords.TABLE_NAME;
            case TwittnukerConstants.TABLE_ID_FILTERED_SOURCES:
                return Filters.Sources.TABLE_NAME;
            case TwittnukerConstants.TABLE_ID_FILTERED_LINKS:
                return Filters.Links.TABLE_NAME;
            case TwittnukerConstants.TABLE_ID_DIRECT_MESSAGES_INBOX:
                return DirectMessages.Inbox.TABLE_NAME;
            case TwittnukerConstants.TABLE_ID_DIRECT_MESSAGES_OUTBOX:
                return DirectMessages.Outbox.TABLE_NAME;
            case TwittnukerConstants.TABLE_ID_DIRECT_MESSAGES:
                return DirectMessages.TABLE_NAME;
            case TwittnukerConstants.TABLE_ID_DIRECT_MESSAGES_CONVERSATIONS_ENTRIES:
                return DirectMessages.ConversationEntries.TABLE_NAME;
            case TwittnukerConstants.TABLE_ID_TRENDS_LOCAL:
                return CachedTrends.Local.TABLE_NAME;
            case TwittnukerConstants.TABLE_ID_TABS:
                return Tabs.TABLE_NAME;
            case TwittnukerConstants.TABLE_ID_PUSH_NOTIFICATIONS:
                return PushNotifications.TABLE_NAME;
            case TwittnukerConstants.TABLE_ID_CACHED_STATUSES:
                return CachedStatuses.TABLE_NAME;
            case TwittnukerConstants.TABLE_ID_CACHED_USERS:
                return CachedUsers.TABLE_NAME;
            case TwittnukerConstants.TABLE_ID_CACHED_HASHTAGS:
                return CachedHashtags.TABLE_NAME;
            case TwittnukerConstants.TABLE_ID_CACHED_RELATIONSHIPS:
                return CachedRelationships.TABLE_NAME;
            case TwittnukerConstants.TABLE_ID_SAVED_SEARCHES:
                return SavedSearches.TABLE_NAME;
            case TwittnukerConstants.TABLE_ID_SEARCH_HISTORY:
                return SearchHistory.TABLE_NAME;
            default:
                return null;
        }
    }

    public static String getTableNameByUri(final Uri uri) {
        if (uri == null) return null;
        return getTableNameById(getTableId(uri));
    }

    @NonNull
    public static Expression buildActivityFilterWhereClause(@NonNull final String table, final Expression extraSelection) {
        final SQLSelectQuery filteredUsersQuery = SQLQueryBuilder
                .select(new Column(new Table(Filters.Users.TABLE_NAME), Filters.Users.USER_KEY))
                .from(new Tables(Filters.Users.TABLE_NAME))
                .build();
        final Expression filteredUsersWhere = Expression.or(
                Expression.in(new Column(new Table(table), Activities.STATUS_USER_KEY), filteredUsersQuery),
                Expression.in(new Column(new Table(table), Activities.STATUS_RETWEETED_BY_USER_KEY), filteredUsersQuery),
                Expression.in(new Column(new Table(table), Activities.STATUS_QUOTED_USER_KEY), filteredUsersQuery)
        );
        final SQLSelectQuery.Builder filteredIdsQueryBuilder = SQLQueryBuilder
                .select(new Column(new Table(table), Activities._ID))
                .from(new Tables(table))
                .where(filteredUsersWhere)
                .union()
                .select(new Columns(new Column(new Table(table), Activities._ID)))
                .from(new Tables(table, Filters.Sources.TABLE_NAME))
                .where(Expression.or(
                        Expression.likeRaw(new Column(new Table(table), Activities.STATUS_SOURCE),
                                "'%>'||" + Filters.Sources.TABLE_NAME + "." + Filters.Sources.VALUE + "||'</a>%'"),
                        Expression.likeRaw(new Column(new Table(table), Activities.STATUS_QUOTE_SOURCE),
                                "'%>'||" + Filters.Sources.TABLE_NAME + "." + Filters.Sources.VALUE + "||'</a>%'")
                ))
                .union()
                .select(new Columns(new Column(new Table(table), Activities._ID)))
                .from(new Tables(table, Filters.Keywords.TABLE_NAME))
                .where(Expression.or(
                        Expression.likeRaw(new Column(new Table(table), Activities.STATUS_TEXT_PLAIN),
                                "'%'||" + Filters.Keywords.TABLE_NAME + "." + Filters.Keywords.VALUE + "||'%'"),
                        Expression.likeRaw(new Column(new Table(table), Activities.STATUS_QUOTE_TEXT_PLAIN),
                                "'%'||" + Filters.Keywords.TABLE_NAME + "." + Filters.Keywords.VALUE + "||'%'")
                ))
                .union()
                .select(new Columns(new Column(new Table(table), Activities._ID)))
                .from(new Tables(table, Filters.Links.TABLE_NAME))
                .where(Expression.or(
                        Expression.likeRaw(new Column(new Table(table), Activities.STATUS_SPANS),
                                "'%'||" + Filters.Links.TABLE_NAME + "." + Filters.Links.VALUE + "||'%'"),
                        Expression.likeRaw(new Column(new Table(table), Activities.STATUS_QUOTE_SPANS),
                                "'%'||" + Filters.Links.TABLE_NAME + "." + Filters.Links.VALUE + "||'%'")
                ));
        final Expression filterExpression = Expression.or(
                Expression.notIn(new Column(new Table(table), Activities._ID), filteredIdsQueryBuilder.build()),
                Expression.equals(new Column(new Table(table), Activities.IS_GAP), 1)
        );
        if (extraSelection != null) {
            return Expression.and(filterExpression, extraSelection);
        }
        return filterExpression;
    }

    @NonNull
    public static int[] getAccountColors(@NonNull final Context context, @NonNull final UserKey[] accountKeys) {
        AccountManager am = AccountManager.get(context);
        final int[] colors = new int[accountKeys.length];
        for (int i = 0; i < accountKeys.length; i++) {
            Account account = AccountUtils.findByAccountKey(am, accountKeys[i]);
            if (account != null) {
                colors[i] = AccountExtensionsKt.getColor(account, am);
            }
        }
        return colors;
    }

    @Nullable
    public static UserKey findAccountKeyByScreenName(@NonNull final Context context, @NonNull final String screenName) {
        AccountManager am = AccountManager.get(context);
        for (Account account : AccountUtils.getAccounts(am)) {
            ParcelableUser user = AccountExtensionsKt.getAccountUser(account, am);
            if (StringUtils.equalsIgnoreCase(screenName, user.screen_name)) {
                return user.key;
            }
        }
        return null;
    }

    @NonNull
    public static UserKey[] getAccountKeys(final Context context) {
        AccountManager am = AccountManager.get(context);
        final Account[] accounts = AccountUtils.getAccounts(am);
        final UserKey[] keys = new UserKey[accounts.length];
        for (int i = 0; i < accounts.length; i++) {
            keys[i] = AccountExtensionsKt.getAccountKey(accounts[i], am);
        }
        return keys;
    }

    @Nullable
    public static UserKey findAccountKey(@NonNull final Context context, @NonNull final String accountId) {
        AccountManager am = AccountManager.get(context);
        for (Account account : AccountUtils.getAccounts(am)) {
            UserKey key = AccountExtensionsKt.getAccountKey(account, am);
            if (accountId.equals(key.getId())) {
                return key;
            }
        }
        return null;
    }

    public static boolean hasAccount(@NonNull final Context context) {
        return AccountUtils.getAccounts(AccountManager.get(context)).length > 0;
    }

    public static synchronized void cleanDatabasesByItemLimit(final Context context) {
        if (context == null) return;
        final ContentResolver resolver = context.getContentResolver();
        final int itemLimit = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).getInt(
                KEY_DATABASE_ITEM_LIMIT, DEFAULT_DATABASE_ITEM_LIMIT);

        for (final UserKey accountKey : getAccountKeys(context)) {
            // Clean statuses.
            for (final Uri uri : STATUSES_URIS) {
                if (CachedStatuses.CONTENT_URI.equals(uri)) {
                    continue;
                }
                final String table = getTableNameByUri(uri);
                final SQLSelectQuery.Builder qb = new SQLSelectQuery.Builder();
                qb.select(new Column(Statuses._ID))
                        .from(new Tables(table))
                        .where(Expression.equalsArgs(Statuses.ACCOUNT_KEY))
                        .orderBy(new OrderBy(Statuses.POSITION_KEY, false))
                        .limit(itemLimit);
                final Expression where = Expression.and(
                        Expression.notIn(new Column(Statuses._ID), qb.build()),
                        Expression.equalsArgs(Statuses.ACCOUNT_KEY)
                );
                final String[] whereArgs = {String.valueOf(accountKey), String.valueOf(accountKey)};
                resolver.delete(uri, where.getSQL(), whereArgs);
            }
            for (final Uri uri : ACTIVITIES_URIS) {
                final String table = getTableNameByUri(uri);
                final SQLSelectQuery.Builder qb = new SQLSelectQuery.Builder();
                qb.select(new Column(Activities._ID))
                        .from(new Tables(table))
                        .where(Expression.equalsArgs(Activities.ACCOUNT_KEY))
                        .orderBy(new OrderBy(Activities.TIMESTAMP, false))
                        .limit(itemLimit);
                final Expression where = Expression.and(
                        Expression.notIn(new Column(Activities._ID), qb.build()),
                        Expression.equalsArgs(Activities.ACCOUNT_KEY)
                );
                final String[] whereArgs = {String.valueOf(accountKey), String.valueOf(accountKey)};
                resolver.delete(uri, where.getSQL(), whereArgs);
            }
            for (final Uri uri : DIRECT_MESSAGES_URIS) {
                final String table = getTableNameByUri(uri);
                final Expression accountWhere = Expression.equalsArgs(DirectMessages.ACCOUNT_KEY);
                final SQLSelectQuery.Builder qb = new SQLSelectQuery.Builder();
                qb.select(new Column(DirectMessages._ID))
                        .from(new Tables(table))
                        .where(accountWhere)
                        .orderBy(new OrderBy(DirectMessages.MESSAGE_ID, false))
                        .limit(itemLimit * 10);
                final Expression where = Expression.and(
                        Expression.notIn(new Column(DirectMessages._ID), qb.build()),
                        Expression.equalsArgs(DirectMessages.ACCOUNT_KEY)
                );
                final String[] whereArgs = {String.valueOf(accountKey), String.valueOf(accountKey)};
                resolver.delete(uri, where.getSQL(), whereArgs);
            }
        }
        // Clean cached values.
        for (final Uri uri : CACHE_URIS) {
            final String table = getTableNameByUri(uri);
            if (table == null) continue;
            final SQLSelectQuery.Builder qb = new SQLSelectQuery.Builder();
            qb.select(new Column(BaseColumns._ID))
                    .from(new Tables(table))
                    .orderBy(new OrderBy(BaseColumns._ID, false))
                    .limit(itemLimit * 20);
            final Expression where = Expression.notIn(new Column(BaseColumns._ID), qb.build());
            resolver.delete(uri, where.getSQL(), null);
        }
    }

    public static void clearAccountName() {
        sAccountScreenNames.clear();
    }

    public static boolean isFilteringUser(Context context, UserKey userKey) {
        return isFilteringUser(context, userKey.toString());
    }

    public static boolean isFilteringUser(Context context, String userKey) {
        final ContentResolver cr = context.getContentResolver();
        final Expression where = Expression.equalsArgs(Filters.Users.USER_KEY);
        final Cursor c = cr.query(Filters.Users.CONTENT_URI, new String[]{SQLFunctions.COUNT()},
                where.getSQL(), new String[]{userKey}, null);
        if (c == null) return false;
        try {
            if (c.moveToFirst()) {
                return c.getLong(0) > 0;
            }
        } finally {
            c.close();
        }
        return false;
    }

    @NonNull
    static String[] getStringFieldArray(@NonNull Context context, @NonNull Uri uri,
                                    @NonNull UserKey[] keys, @NonNull String keyField,
                                    @NonNull String valueField, @Nullable OrderBy sortExpression) {
        return getFieldArray(context, uri, keys, keyField, valueField, sortExpression, new FieldArrayCreator<String[]>() {
            @Override
            public String[] newArray(int size) {
                return new String[size];
            }

            @Override
            public void assign(String[] array, int arrayIdx, Cursor cur, int colIdx) {
                array[arrayIdx] = cur.getString(colIdx);
            }
        });
    }

    @NonNull
    static long[] getLongFieldArray(@NonNull Context context, @NonNull Uri uri,
                                    @NonNull UserKey[] keys, @NonNull String keyField,
                                    @NonNull String valueField, @Nullable OrderBy sortExpression) {
        return getFieldArray(context, uri, keys, keyField, valueField, sortExpression, new FieldArrayCreator<long[]>() {
            @Override
            public long[] newArray(int size) {
                return new long[size];
            }

            @Override
            public void assign(long[] array, int arrayIdx, Cursor cur, int colIdx) {
                array[arrayIdx] = cur.getLong(colIdx);
            }
        });
    }

    @NonNull
    static <T> T getFieldArray(@NonNull Context context, @NonNull Uri uri,
                               @NonNull UserKey[] keys, @NonNull String keyField,
                               @NonNull String valueField, @Nullable OrderBy sortExpression,
                               @NonNull FieldArrayCreator<T> creator) {
        final ContentResolver resolver = context.getContentResolver();
        final T messageIds = creator.newArray(keys.length);
        final String[] selectionArgs = TwidereArrayUtils.toStringArray(keys);
        final SQLSelectQuery.Builder builder = SQLQueryBuilder.select(new Columns(keyField, valueField))
                .from(new Table(getTableNameByUri(uri)))
                .groupBy(new Column(keyField))
                .having(Expression.in(new Column(keyField), new ArgsArray(keys.length)));
        if (sortExpression != null) {
            builder.orderBy(sortExpression);
        }
        final Cursor cur = resolver.query(Uri.withAppendedPath(TwidereDataStore.CONTENT_URI_RAW_QUERY,
                builder.buildSQL()), null, null, selectionArgs, null);
        if (cur == null) return messageIds;
        try {
            while (cur.moveToNext()) {
                final String string = cur.getString(0);
                final UserKey accountKey = string != null ? UserKey.valueOf(string) : null;
                int idx = ArrayUtils.indexOf(keys, accountKey);
                if (idx < 0) continue;
                creator.assign(messageIds, idx, cur, 1);
            }
            return messageIds;
        } finally {
            cur.close();
        }
    }

    public static void deleteStatus(@NonNull ContentResolver cr, @NonNull UserKey accountKey,
                                    @NonNull String statusId, @Nullable ParcelableStatus status) {

        final String host = accountKey.getHost();
        final String deleteWhere, updateWhere;
        final String[] deleteWhereArgs, updateWhereArgs;
        if (host != null) {
            deleteWhere = Expression.and(
                    Expression.likeRaw(new Column(Statuses.ACCOUNT_KEY), "'%@'||?"),
                    Expression.or(
                            Expression.equalsArgs(Statuses.STATUS_ID),
                            Expression.equalsArgs(Statuses.RETWEET_ID)
                    )).getSQL();
            deleteWhereArgs = new String[]{host, statusId, statusId};
            updateWhere = Expression.and(
                    Expression.likeRaw(new Column(Statuses.ACCOUNT_KEY), "'%@'||?"),
                    Expression.equalsArgs(Statuses.MY_RETWEET_ID)
            ).getSQL();
            updateWhereArgs = new String[]{host, statusId};
        } else {
            deleteWhere = Expression.or(
                    Expression.equalsArgs(Statuses.STATUS_ID),
                    Expression.equalsArgs(Statuses.RETWEET_ID)
            ).getSQL();
            deleteWhereArgs = new String[]{statusId, statusId};
            updateWhere = Expression.equalsArgs(Statuses.MY_RETWEET_ID).getSQL();
            updateWhereArgs = new String[]{statusId};
        }
        for (final Uri uri : STATUSES_URIS) {
            cr.delete(uri, deleteWhere, deleteWhereArgs);
            if (status != null) {
                final ContentValues values = new ContentValues();
                values.putNull(Statuses.MY_RETWEET_ID);
                values.put(Statuses.RETWEET_COUNT, status.retweet_count - 1);
                cr.update(uri, values, updateWhere, updateWhereArgs);
            }
        }
    }

    public static void deleteActivityStatus(@NonNull ContentResolver cr, @NonNull UserKey accountKey,
                                            @NonNull final String statusId, @Nullable final ParcelableStatus result) {

        final String host = accountKey.getHost();
        final String deleteWhere, updateWhere;
        final String[] deleteWhereArgs, updateWhereArgs;
        if (host != null) {
            deleteWhere = Expression.and(
                    Expression.likeRaw(new Column(Activities.ACCOUNT_KEY), "'%@'||?"),
                    Expression.or(
                            Expression.equalsArgs(Activities.STATUS_ID),
                            Expression.equalsArgs(Activities.STATUS_RETWEET_ID)
                    )).getSQL();
            deleteWhereArgs = new String[]{host, statusId, statusId};
            updateWhere = Expression.and(
                    Expression.likeRaw(new Column(Activities.ACCOUNT_KEY), "'%@'||?"),
                    Expression.equalsArgs(Activities.STATUS_MY_RETWEET_ID)
            ).getSQL();
            updateWhereArgs = new String[]{host, statusId};
        } else {
            deleteWhere = Expression.or(
                    Expression.equalsArgs(Activities.STATUS_ID),
                    Expression.equalsArgs(Activities.STATUS_RETWEET_ID)
            ).getSQL();
            deleteWhereArgs = new String[]{statusId, statusId};
            updateWhere = Expression.equalsArgs(Activities.STATUS_MY_RETWEET_ID).getSQL();
            updateWhereArgs = new String[]{statusId};
        }
        for (final Uri uri : ACTIVITIES_URIS) {
            cr.delete(uri, deleteWhere, deleteWhereArgs);
            updateActivity(cr, uri, updateWhere, updateWhereArgs, new UpdateActivityAction() {

                @Override
                public void process(ParcelableActivity activity) {
                    activity.status_my_retweet_id = null;
                    ParcelableStatus[][] statusesMatrix = {activity.target_statuses,
                            activity.target_object_statuses};
                    for (ParcelableStatus[] statusesArray : statusesMatrix) {
                        if (statusesArray == null) continue;
                        for (ParcelableStatus status : statusesArray) {
                            if (statusId.equals(status.id) || statusId.equals(status.retweet_id)
                                    || statusId.equals(status.my_retweet_id)) {
                                status.my_retweet_id = null;
                                if (result != null) {
                                    status.reply_count = result.reply_count;
                                    status.retweet_count = result.retweet_count - 1;
                                    status.favorite_count = result.favorite_count;
                                }
                            }
                        }
                    }
                }
            });
        }
    }

    @WorkerThread
    public static void updateActivity(ContentResolver cr, Uri uri, String where, String[] whereArgs,
                                      UpdateActivityAction action) {
        final Cursor c = cr.query(uri, Activities.COLUMNS, where, whereArgs, null);
        if (c == null) return;
        LongSparseArray<ContentValues> values = new LongSparseArray<>();
        try {
            ParcelableActivityCursorIndices ci = new ParcelableActivityCursorIndices(c);
            c.moveToFirst();
            while (!c.isAfterLast()) {
                final ParcelableActivity activity = ci.newObject(c);
                action.process(activity);
                values.put(activity._id, ParcelableActivityValuesCreator.create(activity));
                c.moveToNext();
            }
        } catch (IOException e) {
            return;
        } finally {
            c.close();
        }
        String updateWhere = Expression.equalsArgs(Activities._ID).getSQL();
        String[] updateWhereArgs = new String[1];
        for (int i = 0, j = values.size(); i < j; i++) {
            updateWhereArgs[0] = String.valueOf(values.keyAt(i));
            cr.update(uri, values.valueAt(i), updateWhere, updateWhereArgs);
        }
    }

    static void updateActivityStatus(ContentResolver resolver, UserKey accountKey, String statusId, UpdateActivityAction action) {
        final String activityWhere = Expression.and(
                Expression.equalsArgs(Activities.ACCOUNT_KEY),
                Expression.or(
                        Expression.equalsArgs(Activities.STATUS_ID),
                        Expression.equalsArgs(Activities.STATUS_RETWEET_ID)
                )
        ).getSQL();
        final String[] activityWhereArgs = {accountKey.toString(), statusId, statusId};
        for (final Uri uri : ACTIVITIES_URIS) {
            updateActivity(resolver, uri, activityWhere, activityWhereArgs, action);
        }
    }

    public static void processTabExtras(List<Expression> expressions, List<String> expressionArgs, HomeTabExtras extras) {
        if (extras.isHideRetweets()) {
            expressions.add(Expression.equalsArgs(Statuses.IS_RETWEET));
            expressionArgs.add("0");
        }
        if (extras.isHideQuotes()) {
            expressions.add(Expression.equalsArgs(Statuses.IS_QUOTE));
            expressionArgs.add("0");
        }
        if (extras.isHideReplies()) {
            expressions.add(Expression.isNull(new Column(Statuses.IN_REPLY_TO_STATUS_ID)));
        }
    }

    public static void prepareDatabase(@NonNull Context context) {
        context.getContentResolver().query(TwidereDataStore.CONTENT_URI_DATABASE_PREPARE, null,
                null, null, null);
    }

    interface FieldArrayCreator<T> {
        T newArray(int size);

        void assign(T array, int arrayIdx, Cursor cur, int colIdx);
    }

    public static int queryCount(@NonNull final Context context, @NonNull final Uri uri,
                          @Nullable final String selection, @Nullable final String[] selectionArgs) {
        final ContentResolver resolver = context.getContentResolver();
        final String[] projection = new String[]{SQLFunctions.COUNT()};
        final Cursor cur = resolver.query(uri, projection, selection, selectionArgs, null);
        if (cur == null) return -1;
        try {
            if (cur.moveToFirst()) {
                return cur.getInt(0);
            }
            return -1;
        } finally {
            cur.close();
        }
    }

    public static int getInteractionsCount(@NonNull final Context context, @Nullable final Bundle extraArgs,
                                           final UserKey[] accountIds, final long since,final String sinceColumn) {
        Expression extraWhere = null;
        String[] extraWhereArgs = null;
        boolean followingOnly = false;
        if (extraArgs != null) {
            final TabExtras extras = extraArgs.getParcelable(EXTRA_EXTRAS);
            if (extras instanceof InteractionsTabExtras) {
                InteractionsTabExtras ite = ((InteractionsTabExtras) extras);
                if (ite.isMentionsOnly()) {
                    extraWhere = Expression.inArgs(Activities.ACTION, 3);
                    extraWhereArgs = new String[]{Activity.Action.MENTION,
                            Activity.Action.REPLY, Activity.Action.QUOTE};
                }
                if (ite.isMyFollowingOnly()) {
                    followingOnly = true;
                }
            }
        }
        return getActivitiesCount(context, Activities.AboutMe.CONTENT_URI, extraWhere, extraWhereArgs,
                since, sinceColumn, followingOnly, accountIds);
    }

    public interface UpdateActivityAction {

        void process(ParcelableActivity activity);
    }
}