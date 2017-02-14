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

package de.vanita5.twittnuker.provider;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteFullException;
import android.database.sqlite.SQLiteOpenHelper;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.InboxStyle;
import android.support.v4.text.BidiFormatter;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.squareup.otto.Bus;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.mariotaku.sqliteqb.library.Columns.Column;
import org.mariotaku.sqliteqb.library.Expression;
import org.mariotaku.sqliteqb.library.OrderBy;
import org.mariotaku.sqliteqb.library.RawItemArray;
import org.mariotaku.sqliteqb.library.SQLConstants;
import org.mariotaku.sqliteqb.library.SQLFunctions;
import org.mariotaku.sqliteqb.library.query.SQLSelectQuery;
import de.vanita5.twittnuker.BuildConfig;
import de.vanita5.twittnuker.Constants;
import de.vanita5.twittnuker.R;
import de.vanita5.twittnuker.TwittnukerConstants;
import de.vanita5.twittnuker.activity.HomeActivity;
import de.vanita5.twittnuker.annotation.CustomTabType;
import de.vanita5.twittnuker.annotation.NotificationType;
import de.vanita5.twittnuker.annotation.ReadPositionTag;
import de.vanita5.twittnuker.library.twitter.model.Activity;
import de.vanita5.twittnuker.app.TwittnukerApplication;
import de.vanita5.twittnuker.model.AccountPreferences;
import de.vanita5.twittnuker.model.ActivityTitleSummaryMessage;
import de.vanita5.twittnuker.model.Draft;
import de.vanita5.twittnuker.model.DraftCursorIndices;
import de.vanita5.twittnuker.model.NotificationContent;
import de.vanita5.twittnuker.model.ParcelableActivity;
import de.vanita5.twittnuker.model.ParcelableActivityCursorIndices;
import de.vanita5.twittnuker.model.ParcelableDirectMessage;
import de.vanita5.twittnuker.model.ParcelableStatus;
import de.vanita5.twittnuker.model.ParcelableStatusCursorIndices;
import de.vanita5.twittnuker.model.ParcelableUser;
import de.vanita5.twittnuker.model.StringLongPair;
import de.vanita5.twittnuker.model.UnreadItem;
import de.vanita5.twittnuker.model.UserKey;
import de.vanita5.twittnuker.model.event.UnreadCountUpdatedEvent;
import de.vanita5.twittnuker.model.util.ParcelableActivityUtils;
import de.vanita5.twittnuker.provider.TwidereDataStore.Activities;
import de.vanita5.twittnuker.provider.TwidereDataStore.CachedHashtags;
import de.vanita5.twittnuker.provider.TwidereDataStore.CachedRelationships;
import de.vanita5.twittnuker.provider.TwidereDataStore.CachedStatuses;
import de.vanita5.twittnuker.provider.TwidereDataStore.CachedUsers;
import de.vanita5.twittnuker.provider.TwidereDataStore.DNS;
import de.vanita5.twittnuker.provider.TwidereDataStore.Drafts;
import de.vanita5.twittnuker.provider.TwidereDataStore.Notifications;
import de.vanita5.twittnuker.provider.TwidereDataStore.Preferences;
import de.vanita5.twittnuker.provider.TwidereDataStore.SavedSearches;
import de.vanita5.twittnuker.provider.TwidereDataStore.SearchHistory;
import de.vanita5.twittnuker.provider.TwidereDataStore.Statuses;
import de.vanita5.twittnuker.provider.TwidereDataStore.Suggestions;
import de.vanita5.twittnuker.provider.TwidereDataStore.UnreadCounts;
import de.vanita5.twittnuker.receiver.NotificationReceiver;
import de.vanita5.twittnuker.service.LengthyOperationsService;
import de.vanita5.twittnuker.util.ActivityTracker;
import de.vanita5.twittnuker.util.AsyncTwitterWrapper;
import de.vanita5.twittnuker.util.DataStoreFunctionsKt;
import de.vanita5.twittnuker.util.DataStoreUtils;
import de.vanita5.twittnuker.util.DebugLog;
import de.vanita5.twittnuker.util.InternalTwitterContentUtils;
import de.vanita5.twittnuker.util.NotificationManagerWrapper;
import de.vanita5.twittnuker.util.ParseUtils;
import de.vanita5.twittnuker.util.NotificationHelper;
import de.vanita5.twittnuker.util.ReadStateManager;
import de.vanita5.twittnuker.util.SQLiteDatabaseWrapper;
import de.vanita5.twittnuker.util.SQLiteDatabaseWrapper.LazyLoadCallback;
import de.vanita5.twittnuker.util.SharedPreferencesWrapper;
import de.vanita5.twittnuker.util.TwidereQueryBuilder.CachedUsersQueryBuilder;
import de.vanita5.twittnuker.util.UriExtraUtils;
import de.vanita5.twittnuker.util.UserColorNameManager;
import de.vanita5.twittnuker.util.Utils;
import de.vanita5.twittnuker.util.dagger.GeneralComponentHelper;
import de.vanita5.twittnuker.util.net.TwidereDns;

import org.oshkimaadziig.george.androidutils.SpanFormatter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

public final class TwidereDataProvider extends ContentProvider implements Constants,
        OnSharedPreferenceChangeListener, LazyLoadCallback {

    public static final String TAG_OLDEST_MESSAGES = "oldest_messages";
    private static final Pattern PATTERN_SCREEN_NAME = Pattern.compile("(?i)[@\uFF20]?([a-z0-9_]{1,20})");
    @Inject
    @NonNull
    ReadStateManager readStateManager;
    @Inject
    @NonNull
    AsyncTwitterWrapper twitterWrapper;
    @Inject
    @NonNull
    ImageLoader mediaLoader;
    @Inject
    @NonNull
    NotificationManagerWrapper notificationManager;
    @Inject
    @NonNull
    SharedPreferencesWrapper preferences;
    @Inject
    @NonNull
    TwidereDns dns;
    @Inject
    @NonNull
    Bus bus;
    @Inject
    @NonNull
    UserColorNameManager userColorNameManager;
    @Inject
    @NonNull
    BidiFormatter bidiFormatter;
    @Inject
    @NonNull
    ActivityTracker activityTracker;

    private Handler mHandler;
    private NotificationHelper notificationHelper;
    private ContentResolver mContentResolver;
    private SQLiteDatabaseWrapper databaseWrapper;
    private Executor mBackgroundExecutor;
    private boolean nameFirst;
    private boolean useStarForLikes;

    private static PendingIntent getMarkReadDeleteIntent(Context context, @NotificationType String type,
                                                         @Nullable UserKey accountKey, long position,
                                                         boolean extraUserFollowing) {
        return getMarkReadDeleteIntent(context, type, accountKey, position, -1, -1, extraUserFollowing);
    }

    private static PendingIntent getMarkReadDeleteIntent(Context context, @NotificationType String type,
                                                         @Nullable UserKey accountKey, long position,
                                                         long extraId, long extraUserId,
                                                         boolean extraUserFollowing) {
        // Setup delete intent
        final Intent intent = new Intent(context, NotificationReceiver.class);
        intent.setAction(BROADCAST_NOTIFICATION_DELETED);
        final Uri.Builder linkBuilder = new Uri.Builder();
        linkBuilder.scheme(SCHEME_TWITTNUKER);
        linkBuilder.authority(AUTHORITY_INTERACTIONS);
        linkBuilder.appendPath(type);
        if (accountKey != null) {
            linkBuilder.appendQueryParameter(QUERY_PARAM_ACCOUNT_KEY, accountKey.toString());
        }
        linkBuilder.appendQueryParameter(QUERY_PARAM_READ_POSITION, String.valueOf(position));
        linkBuilder.appendQueryParameter(QUERY_PARAM_TIMESTAMP, String.valueOf(System.currentTimeMillis()));
        linkBuilder.appendQueryParameter(QUERY_PARAM_NOTIFICATION_TYPE, type);

        UriExtraUtils.addExtra(linkBuilder, "item_id", extraId);
        UriExtraUtils.addExtra(linkBuilder, "item_user_id", extraUserId);
        UriExtraUtils.addExtra(linkBuilder, "item_user_following", extraUserFollowing);
        intent.setData(linkBuilder.build());
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    private static PendingIntent getMarkReadDeleteIntent(Context context, @NotificationType String notificationType,
                                                         @Nullable UserKey accountKey, StringLongPair[] positions) {
        // Setup delete intent
        final Intent intent = new Intent(context, NotificationReceiver.class);
        final Uri.Builder linkBuilder = new Uri.Builder();
        linkBuilder.scheme(SCHEME_TWITTNUKER);
        linkBuilder.authority(AUTHORITY_INTERACTIONS);
        linkBuilder.appendPath(notificationType);
        if (accountKey != null) {
            linkBuilder.appendQueryParameter(QUERY_PARAM_ACCOUNT_KEY, accountKey.toString());
        }
        linkBuilder.appendQueryParameter(QUERY_PARAM_READ_POSITIONS, StringLongPair.toString(positions));
        linkBuilder.appendQueryParameter(QUERY_PARAM_TIMESTAMP, String.valueOf(System.currentTimeMillis()));
        linkBuilder.appendQueryParameter(QUERY_PARAM_NOTIFICATION_TYPE, notificationType);
        intent.setData(linkBuilder.build());
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    private static Cursor getPreferencesCursor(final SharedPreferencesWrapper preferences, final String key) {
        final MatrixCursor c = new MatrixCursor(Preferences.MATRIX_COLUMNS);
        final Map<String, Object> map = new HashMap<>();
        final Map<String, ?> all = preferences.getAll();
        if (key == null) {
            map.putAll(all);
        } else {
            map.put(key, all.get(key));
        }
        for (final Map.Entry<String, ?> item : map.entrySet()) {
            final Object value = item.getValue();
            final int type = getPreferenceType(value);
            c.addRow(new Object[]{item.getKey(), ParseUtils.parseString(value), type});
        }
        return c;
    }

    private static int getPreferenceType(final Object object) {
        if (object == null)
            return Preferences.TYPE_NULL;
        else if (object instanceof Boolean)
            return Preferences.TYPE_BOOLEAN;
        else if (object instanceof Integer)
            return Preferences.TYPE_INTEGER;
        else if (object instanceof Long)
            return Preferences.TYPE_LONG;
        else if (object instanceof Float)
            return Preferences.TYPE_FLOAT;
        else if (object instanceof String) return Preferences.TYPE_STRING;
        return Preferences.TYPE_INVALID;
    }

    private static int getUnreadCount(final List<UnreadItem> set, final long... accountIds) {
        if (set == null || set.isEmpty()) return 0;
        int count = 0;
        for (final UnreadItem item : set.toArray(new UnreadItem[set.size()])) {
            if (item != null && ArrayUtils.contains(accountIds, item.account_id)) {
                count++;
            }
        }
        return count;
    }

    private static int getConflictAlgorithm(final int tableId) {
        switch (tableId) {
            case TABLE_ID_CACHED_HASHTAGS:
            case TABLE_ID_CACHED_STATUSES:
            case TABLE_ID_CACHED_USERS:
            case TABLE_ID_CACHED_RELATIONSHIPS:
            case TABLE_ID_SEARCH_HISTORY:
            case TABLE_ID_MESSAGES:
            case TABLE_ID_MESSAGES_CONVERSATIONS:
                return SQLiteDatabase.CONFLICT_REPLACE;
            case TABLE_ID_FILTERED_USERS:
            case TABLE_ID_FILTERED_KEYWORDS:
            case TABLE_ID_FILTERED_SOURCES:
            case TABLE_ID_FILTERED_LINKS:
                return SQLiteDatabase.CONFLICT_IGNORE;
        }
        return SQLiteDatabase.CONFLICT_NONE;
    }

    @Override
    public int bulkInsert(@NonNull final Uri uri, @NonNull final ContentValues[] valuesArray) {
        try {
            return bulkInsertInternal(uri, valuesArray);
        } catch (final SQLException e) {
            if (handleSQLException(e)) {
                try {
                    return bulkInsertInternal(uri, valuesArray);
                } catch (SQLException e1) {
                    throw new IllegalStateException(e1);
                }
            }
            throw new IllegalStateException(e);
        }
    }

    private boolean handleSQLException(SQLException e) {
        try {
            if (e instanceof SQLiteFullException) {
                // Drop cached databases
                databaseWrapper.delete(CachedUsers.TABLE_NAME, null, null);
                databaseWrapper.delete(CachedStatuses.TABLE_NAME, null, null);
                databaseWrapper.delete(CachedHashtags.TABLE_NAME, null, null);
                databaseWrapper.execSQL("VACUUM");
                return true;
            }
        } catch (SQLException ee) {
            throw new IllegalStateException(ee);
        }
        throw new IllegalStateException(e);
    }

    private int bulkInsertInternal(@NonNull Uri uri, @NonNull ContentValues[] valuesArray) {
        final int tableId = DataStoreUtils.getTableId(uri);
        final String table = DataStoreUtils.getTableNameById(tableId);
        int result = 0;
        final long[] newIds = new long[valuesArray.length];
        if (table != null && valuesArray.length > 0) {
            databaseWrapper.beginTransaction();
            if (tableId == TABLE_ID_CACHED_USERS) {
                for (final ContentValues values : valuesArray) {
                    final Expression where = Expression.equalsArgs(CachedUsers.USER_KEY);
                    databaseWrapper.update(table, values, where.getSQL(), new String[]{
                            values.getAsString(CachedUsers.USER_KEY)});
                    newIds[result++] = databaseWrapper.insertWithOnConflict(table, null,
                            values, SQLiteDatabase.CONFLICT_REPLACE);
                }
            } else if (tableId == TABLE_ID_SEARCH_HISTORY) {
                for (final ContentValues values : valuesArray) {
                    values.put(SearchHistory.RECENT_QUERY, System.currentTimeMillis());
                    final Expression where = Expression.equalsArgs(SearchHistory.QUERY);
                    final String[] args = {values.getAsString(SearchHistory.QUERY)};
                    databaseWrapper.update(table, values, where.getSQL(), args);
                    newIds[result++] = databaseWrapper.insertWithOnConflict(table, null,
                            values, SQLiteDatabase.CONFLICT_IGNORE);
                }
            } else {
                final int conflictAlgorithm = getConflictAlgorithm(tableId);
                if (conflictAlgorithm != SQLiteDatabase.CONFLICT_NONE) {
                    for (final ContentValues values : valuesArray) {
                        newIds[result++] = databaseWrapper.insertWithOnConflict(table, null,
                                values, conflictAlgorithm);
                    }
                } else {
                    for (final ContentValues values : valuesArray) {
                        newIds[result++] = databaseWrapper.insert(table, null, values);
                    }
                }
            }
            databaseWrapper.setTransactionSuccessful();
            databaseWrapper.endTransaction();
        }
        if (result > 0) {
            onDatabaseUpdated(tableId, uri);
        }
        onNewItemsInserted(uri, tableId, valuesArray);
        return result;
    }

    @Override
    public int delete(@NonNull final Uri uri, final String selection, final String[] selectionArgs) {
        try {
            return deleteInternal(uri, selection, selectionArgs);
        } catch (final SQLException e) {
            if (handleSQLException(e)) {
                try {
                    return deleteInternal(uri, selection, selectionArgs);
                } catch (SQLException e1) {
                    throw new IllegalStateException(e1);
                }
            }
            throw new IllegalStateException(e);
        }
    }

    private int deleteInternal(@NonNull Uri uri, String selection, String[] selectionArgs) {
        final int tableId = DataStoreUtils.getTableId(uri);
        final String table = DataStoreUtils.getTableNameById(tableId);
        switch (tableId) {
            case VIRTUAL_TABLE_ID_NOTIFICATIONS: {
                final List<String> segments = uri.getPathSegments();
                if (segments.size() == 1) {
                    clearNotification();
                } else if (segments.size() == 2) {
                    final int notificationType = NumberUtils.toInt(segments.get(1), -1);
                    clearNotification(notificationType, null);
                } else if (segments.size() == 3) {
                    final int notificationType = NumberUtils.toInt(segments.get(1), -1);
                    final UserKey accountKey = UserKey.valueOf(segments.get(2));
                    clearNotification(notificationType, accountKey);
                }
                return 1;
            }
            case VIRTUAL_TABLE_ID_UNREAD_COUNTS: {
                return 0;
            }
        }
        if (table == null) return 0;
        final int result = databaseWrapper.delete(table, selection, selectionArgs);
        if (result > 0) {
            onDatabaseUpdated(tableId, uri);
        }
        return result;
    }

    @Override
    public String getType(@NonNull final Uri uri) {
        return null;
    }

    @Override
    public Uri insert(@NonNull final Uri uri, final ContentValues values) {
        try {
            return insertInternal(uri, values);
        } catch (final SQLException e) {
            if (handleSQLException(e)) {
                try {
                    return insertInternal(uri, values);
                } catch (SQLException e1) {
                    throw new IllegalStateException(e1);
                }
            }
            throw new IllegalStateException(e);
        }
    }

    private Uri insertInternal(@NonNull Uri uri, ContentValues values) {
        final int tableId = DataStoreUtils.getTableId(uri);
        final String table = DataStoreUtils.getTableNameById(tableId);
        final long rowId;
        switch (tableId) {
            case TABLE_ID_CACHED_USERS: {
                final Expression where = Expression.equalsArgs(CachedUsers.USER_KEY);
                final String[] whereArgs = {values.getAsString(CachedUsers.USER_KEY)};
                databaseWrapper.update(table, values, where.getSQL(), whereArgs);
                rowId = databaseWrapper.insertWithOnConflict(table, null, values,
                        SQLiteDatabase.CONFLICT_IGNORE);
                break;
            }
            case TABLE_ID_SEARCH_HISTORY: {
                values.put(SearchHistory.RECENT_QUERY, System.currentTimeMillis());
                final Expression where = Expression.equalsArgs(SearchHistory.QUERY);
                final String[] args = {values.getAsString(SearchHistory.QUERY)};
                databaseWrapper.update(table, values, where.getSQL(), args);
                rowId = databaseWrapper.insertWithOnConflict(table, null, values,
                        SQLiteDatabase.CONFLICT_IGNORE);
                break;
            }
            case TABLE_ID_CACHED_RELATIONSHIPS: {
                final String accountKey = values.getAsString(CachedRelationships.ACCOUNT_KEY);
                final String userId = values.getAsString(CachedRelationships.USER_KEY);
                final Expression where = Expression.and(
                        Expression.equalsArgs(CachedRelationships.ACCOUNT_KEY),
                        Expression.equalsArgs(CachedRelationships.USER_KEY)
                );
                final String[] whereArgs = {accountKey, userId};
                if (databaseWrapper.update(table, values, where.getSQL(), whereArgs) > 0) {
                    final String[] projection = {CachedRelationships._ID};
                    final Cursor c = databaseWrapper.query(table, projection, where.getSQL(), null,
                            null, null, null);
                    if (c.moveToFirst()) {
                        rowId = c.getLong(0);
                    } else {
                        rowId = 0;
                    }
                    c.close();
                } else {
                    rowId = databaseWrapper.insertWithOnConflict(table, null, values,
                            SQLiteDatabase.CONFLICT_IGNORE);
                }
                break;
            }
            case VIRTUAL_TABLE_ID_DRAFTS_NOTIFICATIONS: {
                rowId = showDraftNotification(values);
                break;
            }
            default: {
                final int conflictAlgorithm = getConflictAlgorithm(tableId);
                if (conflictAlgorithm != SQLiteDatabase.CONFLICT_NONE) {
                    rowId = databaseWrapper.insertWithOnConflict(table, null, values,
                            conflictAlgorithm);
            } else if (table != null) {
                    rowId = databaseWrapper.insert(table, null, values);
            } else {
                return null;
            }
                break;
            }
        }
        onDatabaseUpdated(tableId, uri);
        onNewItemsInserted(uri, tableId, values);
        return Uri.withAppendedPath(uri, String.valueOf(rowId));
    }

    private long showDraftNotification(ContentValues values) {
        final Context context = getContext();
        if (values == null || context == null) return -1;
        final Long draftId = values.getAsLong(BaseColumns._ID);
        if (draftId == null) return -1;
        final Expression where = Expression.equals(Drafts._ID, draftId);
        final Cursor c = getContentResolver().query(Drafts.CONTENT_URI, Drafts.COLUMNS, where.getSQL(), null, null);
        if (c == null) return -1;
        final DraftCursorIndices i = new DraftCursorIndices(c);
        final Draft item;
        try {
            if (!c.moveToFirst()) return -1;
            item = i.newObject(c);
        } catch (IOException e) {
            return -1;
        } finally {
            c.close();
        }
        final String title = context.getString(R.string.status_not_updated);
        final String message = context.getString(R.string.status_not_updated_summary);
        final Intent intent = new Intent();
        intent.setPackage(BuildConfig.APPLICATION_ID);
        final Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.scheme(SCHEME_TWITTNUKER);
        uriBuilder.authority(AUTHORITY_DRAFTS);
        intent.setData(uriBuilder.build());
        final NotificationCompat.Builder nb = new NotificationCompat.Builder(context);
        nb.setTicker(message);
        nb.setContentTitle(title);
        nb.setContentText(item.text);
        nb.setAutoCancel(true);
        nb.setWhen(System.currentTimeMillis());
        nb.setSmallIcon(R.drawable.ic_stat_draft);
        final Intent discardIntent = new Intent(context, LengthyOperationsService.class);
        discardIntent.setAction(INTENT_ACTION_DISCARD_DRAFT);
        final Uri draftUri = Uri.withAppendedPath(Drafts.CONTENT_URI, String.valueOf(draftId));
        discardIntent.setData(draftUri);
        nb.addAction(R.drawable.ic_action_delete, context.getString(R.string.discard), PendingIntent.getService(context, 0,
                discardIntent, PendingIntent.FLAG_ONE_SHOT));

        final Intent sendIntent = new Intent(context, LengthyOperationsService.class);
        sendIntent.setAction(INTENT_ACTION_SEND_DRAFT);
        sendIntent.setData(draftUri);
        nb.addAction(R.drawable.ic_action_send, context.getString(R.string.action_send),
                PendingIntent.getService(context, 0, sendIntent, PendingIntent.FLAG_ONE_SHOT));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        nb.setContentIntent(PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_ONE_SHOT));
        notificationManager.notify(draftUri.toString(), NOTIFICATION_ID_DRAFTS,
                nb.build());
        return draftId;
    }

    @Override
    public boolean onCreate() {
        final Context context = getContext();
        assert context != null;
        GeneralComponentHelper.build(context).inject(this);
        mHandler = new Handler(Looper.getMainLooper());
        databaseWrapper = new SQLiteDatabaseWrapper(this);
        notificationHelper = new NotificationHelper(context);
        preferences.registerOnSharedPreferenceChangeListener(this);
        mBackgroundExecutor = Executors.newSingleThreadExecutor();
        updatePreferences();
        // final GetWritableDatabaseTask task = new
        // GetWritableDatabaseTask(context, helper, mDatabaseWrapper);
        // task.executeTask();
        return true;
    }

    @Override
    public SQLiteDatabase onCreateSQLiteDatabase() {
        final TwittnukerApplication app = TwittnukerApplication.Companion.getInstance(getContext());
        final SQLiteOpenHelper helper = app.getSqLiteOpenHelper();
        return helper.getWritableDatabase();
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences preferences, final String key) {
        updatePreferences();
    }

    @Override
    public ParcelFileDescriptor openFile(@NonNull final Uri uri, @NonNull final String mode) throws FileNotFoundException {
        final int table_id = DataStoreUtils.getTableId(uri);
        switch (table_id) {
            case VIRTUAL_TABLE_ID_CACHED_IMAGES: {
                return getCachedImageFd(uri.getQueryParameter(QUERY_PARAM_URL));
            }
            case VIRTUAL_TABLE_ID_CACHE_FILES: {
                return getCacheFileFd(uri.getLastPathSegment());
            }
        }
        return null;
    }

    @Override
    public Cursor query(@NonNull final Uri uri, final String[] projection, final String selection, final String[] selectionArgs,
                        final String sortOrder) {
        try {
            final int tableId = DataStoreUtils.getTableId(uri);
            final String table = DataStoreUtils.getTableNameById(tableId);
            switch (tableId) {
                case VIRTUAL_TABLE_ID_DATABASE_PREPARE: {
                    databaseWrapper.prepare();
                    return new MatrixCursor(projection != null ? projection : new String[0]);
                }
                case VIRTUAL_TABLE_ID_ALL_PREFERENCES: {
                    return getPreferencesCursor(preferences, null);
                }
                case VIRTUAL_TABLE_ID_PREFERENCES: {
                    return getPreferencesCursor(preferences, uri.getLastPathSegment());
                }
                case VIRTUAL_TABLE_ID_DNS: {
                    return getDNSCursor(uri.getLastPathSegment());
                }
                case VIRTUAL_TABLE_ID_NOTIFICATIONS: {
                    final List<String> segments = uri.getPathSegments();
                    if (segments.size() == 2) {
                        final int def = -1;
                        return getNotificationsCursor(NumberUtils.toInt(segments.get(1), def));
                    } else
                        return getNotificationsCursor();
                }
                case VIRTUAL_TABLE_ID_UNREAD_COUNTS: {
                    final List<String> segments = uri.getPathSegments();
                    if (segments.size() == 2) {
                        final int def = -1;
                        return getUnreadCountsCursor(NumberUtils.toInt(segments.get(1), def));
                    } else
                        return getUnreadCountsCursor();
                }
                case VIRTUAL_TABLE_ID_UNREAD_COUNTS_BY_TYPE: {
                    final List<String> segments = uri.getPathSegments();
                    if (segments.size() != 3) return null;
                    return getUnreadCountsCursorByType(segments.get(2));
                }
                case VIRTUAL_TABLE_ID_CACHED_USERS_WITH_RELATIONSHIP: {
                    final UserKey accountKey = UserKey.valueOf(uri.getLastPathSegment());
                    final Pair<SQLSelectQuery, String[]> query = CachedUsersQueryBuilder.withRelationship(projection,
                            selection, selectionArgs, sortOrder, accountKey);
                    final Cursor c = databaseWrapper.rawQuery(query.first.getSQL(), query.second);
                    setNotificationUri(c, CachedUsers.CONTENT_URI);
                    return c;
                }
                case VIRTUAL_TABLE_ID_CACHED_USERS_WITH_SCORE: {
                    final UserKey accountKey = UserKey.valueOf(uri.getLastPathSegment());
                    final Pair<SQLSelectQuery, String[]> query = CachedUsersQueryBuilder.withScore(projection,
                            selection, selectionArgs, sortOrder, accountKey, 0);
                    final Cursor c = databaseWrapper.rawQuery(query.first.getSQL(), query.second);
                    setNotificationUri(c, CachedUsers.CONTENT_URI);
                    return c;
                }
                case VIRTUAL_TABLE_ID_DRAFTS_UNSENT: {
                    final AsyncTwitterWrapper twitter = twitterWrapper;
                    final RawItemArray sendingIds = new RawItemArray(twitter.getSendingDraftIds());
                    final Expression where;
                    if (selection != null) {
                        where = Expression.and(new Expression(selection),
                                Expression.notIn(new Column(Drafts._ID), sendingIds));
                    } else {
                        where = Expression.and(Expression.notIn(new Column(Drafts._ID), sendingIds));
                    }
                    final Cursor c = databaseWrapper.query(Drafts.TABLE_NAME, projection,
                            where.getSQL(), selectionArgs, null, null, sortOrder);
                    setNotificationUri(c, Utils.getNotificationUri(tableId, uri));
                    return c;
                }
                case VIRTUAL_TABLE_ID_SUGGESTIONS_AUTO_COMPLETE: {
                    return getAutoCompleteSuggestionsCursor(uri);
                }
                case VIRTUAL_TABLE_ID_SUGGESTIONS_SEARCH: {
                    return getSearchSuggestionCursor(uri);
                }
                case VIRTUAL_TABLE_ID_NULL: {
                    return null;
                }
                case VIRTUAL_TABLE_ID_EMPTY: {
                    return new MatrixCursor(projection != null ? projection : new String[0]);
                }
                case VIRTUAL_TABLE_ID_RAW_QUERY: {
                    if (projection != null || selection != null || sortOrder != null) {
                        throw new IllegalArgumentException();
                    }
                    return databaseWrapper.rawQuery(uri.getLastPathSegment(), selectionArgs);
                }
            }
            if (table == null) return null;
            final Cursor c = databaseWrapper.query(table, projection, selection, selectionArgs,
                    null, null, sortOrder);
            setNotificationUri(c, Utils.getNotificationUri(tableId, uri));
            return c;
        } catch (final SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private Cursor getSearchSuggestionCursor(Uri uri) {
        final String query = uri.getQueryParameter(QUERY_PARAM_QUERY);
        final String paramAccountKey = uri.getQueryParameter(QUERY_PARAM_ACCOUNT_KEY);
        final UserKey accountKey = paramAccountKey != null ? UserKey.valueOf(paramAccountKey) : null;
        if (query == null || accountKey == null) return null;
        final boolean emptyQuery = TextUtils.isEmpty(query);
        final String queryEscaped = query.replace("_", "^_");
        final Cursor[] cursors;
        final String[] historyProjection = {
                new Column(SearchHistory._ID, Suggestions.Search._ID).getSQL(),
                new Column("'" + Suggestions.Search.TYPE_SEARCH_HISTORY + "'", Suggestions.Search.TYPE).getSQL(),
                new Column(SearchHistory.QUERY, Suggestions.Search.TITLE).getSQL(),
                new Column(SQLConstants.NULL, Suggestions.Search.SUMMARY).getSQL(),
                new Column(SQLConstants.NULL, Suggestions.Search.ICON).getSQL(),
                new Column("0", Suggestions.Search.EXTRA_ID).getSQL(),
                new Column(SQLConstants.NULL, Suggestions.Search.EXTRA).getSQL(),
                new Column(SearchHistory.QUERY, Suggestions.Search.VALUE).getSQL(),
        };
        final Expression historySelection = Expression.likeRaw(new Column(SearchHistory.QUERY), "?||'%'", "^");
        @SuppressLint("Recycle") final Cursor historyCursor = databaseWrapper.query(true,
                SearchHistory.TABLE_NAME, historyProjection, historySelection.getSQL(),
                new String[]{queryEscaped}, null, null, SearchHistory.DEFAULT_SORT_ORDER,
                TextUtils.isEmpty(query) ? "3" : "2");
        if (emptyQuery) {
            final String[] savedSearchesProjection = {
                    new Column(SavedSearches._ID, Suggestions.Search._ID).getSQL(),
                    new Column("'" + Suggestions.Search.TYPE_SAVED_SEARCH + "'", Suggestions.Search.TYPE).getSQL(),
                    new Column(SavedSearches.QUERY, Suggestions.Search.TITLE).getSQL(),
                    new Column(SQLConstants.NULL, Suggestions.Search.SUMMARY).getSQL(),
                    new Column(SQLConstants.NULL, Suggestions.Search.ICON).getSQL(),
                    new Column("0", Suggestions.Search.EXTRA_ID).getSQL(),
                    new Column(SQLConstants.NULL, Suggestions.Search.EXTRA).getSQL(),
                    new Column(SavedSearches.QUERY, Suggestions.Search.VALUE).getSQL()
            };
            final Expression savedSearchesWhere = Expression.equalsArgs(SavedSearches.ACCOUNT_KEY);
            final String[] whereArgs = {accountKey.toString()};
            @SuppressLint("Recycle") final Cursor savedSearchesCursor = databaseWrapper.query(true,
                    SavedSearches.TABLE_NAME, savedSearchesProjection, savedSearchesWhere.getSQL(),
                    whereArgs, null, null, SavedSearches.DEFAULT_SORT_ORDER, null);
            cursors = new Cursor[2];
            cursors[1] = savedSearchesCursor;
        } else {
            final String[] usersProjection = {
                    new Column(CachedUsers._ID, Suggestions.Search._ID).getSQL(),
                    new Column("'" + Suggestions.Search.TYPE_USER + "'", Suggestions.Search.TYPE).getSQL(),
                    new Column(CachedUsers.NAME, Suggestions.Search.TITLE).getSQL(),
                    new Column(CachedUsers.SCREEN_NAME, Suggestions.Search.SUMMARY).getSQL(),
                    new Column(CachedUsers.PROFILE_IMAGE_URL, Suggestions.Search.ICON).getSQL(),
                    new Column(CachedUsers.USER_KEY, Suggestions.Search.EXTRA_ID).getSQL(),
                    new Column(SQLConstants.NULL, Suggestions.Search.EXTRA).getSQL(),
                    new Column(CachedUsers.SCREEN_NAME, Suggestions.Search.VALUE).getSQL(),
            };
            String queryTrimmed = queryEscaped.startsWith("@") ? queryEscaped.substring(1) : queryEscaped;
            final Expression usersSelection = Expression.or(
                    Expression.likeRaw(new Column(CachedUsers.SCREEN_NAME), "?||'%'", "^"),
                    Expression.likeRaw(new Column(CachedUsers.NAME), "?||'%'", "^"));
            final String[] selectionArgs = new String[]{queryTrimmed, queryTrimmed};
            final String[] order = {CachedUsers.LAST_SEEN, CachedUsers.SCORE, CachedUsers.SCREEN_NAME,
                    CachedUsers.NAME};
            final boolean[] ascending = {false, false, true, true};
            final OrderBy orderBy = new OrderBy(order, ascending);

            final Pair<SQLSelectQuery, String[]> usersQuery = CachedUsersQueryBuilder.withScore(usersProjection,
                    usersSelection.getSQL(), selectionArgs, orderBy.getSQL(), accountKey, 0);
            @SuppressLint("Recycle") final Cursor usersCursor = databaseWrapper.rawQuery(usersQuery.first.getSQL(), usersQuery.second);
            final Expression exactUserSelection = Expression.or(Expression.likeRaw(new Column(CachedUsers.SCREEN_NAME), "?", "^"));
            final Cursor exactUserCursor = databaseWrapper.query(CachedUsers.TABLE_NAME,
                    new String[]{SQLFunctions.COUNT()}, exactUserSelection.getSQL(),
                    new String[]{queryTrimmed}, null, null, null, "1");
            final boolean hasName = exactUserCursor.moveToPosition(0) && exactUserCursor.getInt(0) > 0;
            exactUserCursor.close();
            final MatrixCursor screenNameCursor = new MatrixCursor(Suggestions.Search.COLUMNS);
            if (!hasName) {
                final Matcher m = PATTERN_SCREEN_NAME.matcher(query);
                if (m.matches()) {
                    final String screenName = m.group(1);
                    screenNameCursor.addRow(new Object[]{0, Suggestions.Search.TYPE_SCREEN_NAME,
                            screenName, null, null, 0, null, screenName});
                }
            }
            cursors = new Cursor[3];
            cursors[1] = screenNameCursor;
            cursors[2] = usersCursor;
        }
        cursors[0] = historyCursor;
        return new MergeCursor(cursors);
    }

    private Cursor getAutoCompleteSuggestionsCursor(@NonNull Uri uri) {
        final String query = uri.getQueryParameter(QUERY_PARAM_QUERY);
        final String type = uri.getQueryParameter(QUERY_PARAM_TYPE);
        final String accountKey = uri.getQueryParameter(QUERY_PARAM_ACCOUNT_KEY);
        if (query == null || type == null) return null;
        final String queryEscaped = query.replace("_", "^_");
        if (Suggestions.AutoComplete.TYPE_USERS.equals(type)) {
            final Expression where = Expression.or(Expression.likeRaw(new Column(CachedUsers.SCREEN_NAME), "?||'%'", "^"),
                    Expression.likeRaw(new Column(CachedUsers.NAME), "?||'%'", "^"));
            final String[] whereArgs = {queryEscaped, queryEscaped};
            final String[] mappedProjection = {
                    new Column(CachedUsers._ID, Suggestions._ID).getSQL(),
                    new Column("'" + Suggestions.AutoComplete.TYPE_USERS + "'", Suggestions.TYPE).getSQL(),
                    new Column(CachedUsers.NAME, Suggestions.TITLE).getSQL(),
                    new Column(CachedUsers.SCREEN_NAME, Suggestions.SUMMARY).getSQL(),
                    new Column(CachedUsers.USER_KEY, Suggestions.EXTRA_ID).getSQL(),
                    new Column(CachedUsers.PROFILE_IMAGE_URL, Suggestions.ICON).getSQL(),
                    new Column(CachedUsers.SCREEN_NAME, Suggestions.VALUE).getSQL(),
            };
            final String[] orderBy = {CachedUsers.SCORE, CachedUsers.LAST_SEEN, CachedUsers.SCREEN_NAME,
                    CachedUsers.NAME};
            final boolean[] ascending = {false, false, true, true};
            return query(Uri.withAppendedPath(CachedUsers.CONTENT_URI_WITH_SCORE, accountKey),
                    mappedProjection, where.getSQL(), whereArgs, new OrderBy(orderBy, ascending).getSQL());
        } else if (Suggestions.AutoComplete.TYPE_HASHTAGS.equals(type)) {
            final Expression where = Expression.likeRaw(new Column(CachedHashtags.NAME), "?||'%'", "^");
            final String[] whereArgs = new String[]{queryEscaped};
            final String[] mappedProjection = {
                    new Column(CachedHashtags._ID, Suggestions._ID).getSQL(),
                    new Column("'" + Suggestions.AutoComplete.TYPE_HASHTAGS + "'", Suggestions.TYPE).getSQL(),
                    new Column(CachedHashtags.NAME, Suggestions.TITLE).getSQL(),
                    new Column("NULL", Suggestions.SUMMARY).getSQL(),
                    new Column("0", Suggestions.EXTRA_ID).getSQL(),
                    new Column("NULL", Suggestions.ICON).getSQL(),
                    new Column(CachedHashtags.NAME, Suggestions.VALUE).getSQL(),
            };
            return query(CachedHashtags.CONTENT_URI, mappedProjection, where.getSQL(),
                    whereArgs, null);
        }
        return null;
    }

    @Override
    public int update(@NonNull final Uri uri, final ContentValues values, final String selection, final String[] selectionArgs) {
        try {
            return updateInternal(uri, values, selection, selectionArgs);
        } catch (final SQLException e) {
            if (handleSQLException(e)) {
                try {
                    return updateInternal(uri, values, selection, selectionArgs);
                } catch (SQLException e1) {
                    throw new IllegalStateException(e1);
                }
            }
            throw new IllegalStateException(e);
        }
    }

    private int updateInternal(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final int tableId = DataStoreUtils.getTableId(uri);
        final String table = DataStoreUtils.getTableNameById(tableId);
        int result = 0;
        if (table != null) {
            result = databaseWrapper.update(table, values, selection, selectionArgs);
        }
        if (result > 0) {
            onDatabaseUpdated(tableId, uri);
        }
        return result;
    }

    private void clearNotification() {
        notificationManager.cancelAll();
    }

    private void clearNotification(final int notificationType, final UserKey accountId) {
        notificationManager.cancelById(Utils.getNotificationId(notificationType, accountId));
    }

    /**
     * //TODO
     * <p>
     * Creates notifications for mentions and DMs
     *
     * @param pref
     * @param type
     * @param build
     */
    private void createNotifications(final AccountPreferences pref, final String type,
                                     final Object o_status, final boolean build) {
        if (preferences.getBoolean(KEY_ENABLE_PUSH_NOTIFICATIONS, false)) return;
        NotificationContent notification = null;

        if (o_status instanceof ParcelableStatus) {
            ParcelableStatus status = (ParcelableStatus) o_status;
            notification = new NotificationContent();
            notification.setAccountKey(status.account_key);
            notification.setObjectId(String.valueOf(status.id));
            notification.setObjectUserKey(status.user_key);
            notification.setFromUser(status.user_screen_name);
            notification.setType(type);
            notification.setMessage(status.text_unescaped);
            notification.setTimestamp(status.timestamp);
            notification.setProfileImageUrl(status.user_profile_image_url);
            notification.setOriginalStatus(status);
        } else if (o_status instanceof ParcelableDirectMessage) {
            ParcelableDirectMessage dm = (ParcelableDirectMessage) o_status;
            notification = new NotificationContent();
            notification.setAccountKey(dm.account_key);
            notification.setObjectId(String.valueOf(dm.id));
            notification.setObjectUserKey(new UserKey(dm.sender_id, TwittnukerConstants.USER_TYPE_TWITTER_COM));
            notification.setFromUser(dm.sender_screen_name);
            notification.setType(type);
            notification.setMessage(dm.text_unescaped);
            notification.setTimestamp(dm.timestamp);
            notification.setProfileImageUrl(dm.sender_profile_image_url);
            notification.setOriginalMessage(dm);
        }
        if (notification != null) {
            notificationHelper.cachePushNotification(notification);
        }
        if (build) notificationHelper.buildNotificationByType(notification, pref, false);
    }

    private ParcelFileDescriptor getCachedImageFd(final String url) throws FileNotFoundException {
        if (BuildConfig.DEBUG) {
            Log.d(LOGTAG, String.format("getCachedImageFd(%s)", url));
        }
        final File file = mediaLoader.getDiskCache().get(url);
        if (file == null) return null;
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    private ParcelFileDescriptor getCacheFileFd(final String name) throws FileNotFoundException {
        if (name == null) return null;
        final Context context = getContext();
        assert context != null;
        final File cacheDir = context.getCacheDir();
        final File file = new File(cacheDir, name);
        if (!file.exists()) return null;
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    private ContentResolver getContentResolver() {
        if (mContentResolver != null) return mContentResolver;
        final Context context = getContext();
        assert context != null;
        return mContentResolver = context.getContentResolver();
    }

    private Cursor getDNSCursor(final String host) {
        final MatrixCursor c = new MatrixCursor(DNS.MATRIX_COLUMNS);
        try {
            final List<InetAddress> addresses = dns.lookup(host);
            for (InetAddress address : addresses) {
                c.addRow(new String[]{host, address.getHostAddress()});
            }
        } catch (final IOException ignore) {
            DebugLog.w(LOGTAG, null, ignore);
        }
        return c;
    }

    private Cursor getNotificationsCursor() {
        final MatrixCursor c = new MatrixCursor(Notifications.MATRIX_COLUMNS);
        return c;
    }

    private Cursor getNotificationsCursor(final int id) {
        final MatrixCursor c = new MatrixCursor(Notifications.MATRIX_COLUMNS);
        return c;
    }

    private Cursor getUnreadCountsCursor() {
        final MatrixCursor c = new MatrixCursor(UnreadCounts.MATRIX_COLUMNS);
        return c;
    }

    private Cursor getUnreadCountsCursor(final int position) {
        final MatrixCursor c = new MatrixCursor(UnreadCounts.MATRIX_COLUMNS);

        return c;
    }

    private Cursor getUnreadCountsCursorByType(final String type) {
        final MatrixCursor c = new MatrixCursor(UnreadCounts.MATRIX_COLUMNS);
        return c;
    }

    private boolean isNotificationAudible() {
        return !activityTracker.isHomeActivityStarted();
    }

    private void notifyContentObserver(@NonNull final Uri uri) {
        if (!uri.getBooleanQueryParameter(QUERY_PARAM_NOTIFY, true)) return;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                final ContentResolver cr = getContentResolver();
                if (cr == null) return;
                cr.notifyChange(uri, null);
            }
        });
    }

    private void notifyUnreadCountChanged(final int position) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                bus.post(new UnreadCountUpdatedEvent(position));
            }
        });
        notifyContentObserver(UnreadCounts.CONTENT_URI);
    }

    private void onDatabaseUpdated(final int tableId, final Uri uri) {
        if (uri == null) return;
        notifyContentObserver(Utils.getNotificationUri(tableId, uri));
    }

    private void onNewItemsInserted(final Uri uri, final int tableId, final ContentValues values) {
        onNewItemsInserted(uri, tableId, new ContentValues[]{values});
    }

    private void onNewItemsInserted(final Uri uri, final int tableId, final ContentValues[] valuesArray) {
        final Context context = getContext();
        if (uri == null || valuesArray == null || valuesArray.length == 0 || context == null)
            return;
        switch (tableId) {
            case TABLE_ID_STATUSES: {
                mBackgroundExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
//                        final AccountPreferences[] prefs = AccountPreferences.getNotificationEnabledPreferences(context,
//                                DataStoreUtils.getAccountKeys(context));
//                        for (final AccountPreferences pref : prefs) {
//                            if (!pref.isHomeTimelineNotificationEnabled()) continue;
//                            final long positionTag = getPositionTag(CustomTabType.HOME_TIMELINE, pref.getAccountKey());
//                            showTimelineNotification(pref, positionTag);
//                        }
                        notifyUnreadCountChanged(NOTIFICATION_ID_HOME_TIMELINE);
                    }
                });
                break;
            }
            case TABLE_ID_ACTIVITIES_ABOUT_ME: {
                mBackgroundExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        final AccountPreferences[] prefs = AccountPreferences.getNotificationEnabledPreferences(context,
                                DataStoreUtils.getAccountKeys(context));
                        final boolean combined = preferences.getBoolean(KEY_COMBINED_NOTIFICATIONS);
                        for (final AccountPreferences pref : prefs) {
                            if (!pref.isInteractionsNotificationEnabled()) continue;
                            showInteractionsNotification(pref, getPositionTag(ReadPositionTag.ACTIVITIES_ABOUT_ME,
                                    pref.getAccountKey()), combined);
                        }
                        notifyUnreadCountChanged(NOTIFICATION_ID_INTERACTIONS_TIMELINE);
                    }
                });
                break;
            }
            case TABLE_ID_MESSAGES: {
                final AccountPreferences[] prefs = AccountPreferences.getNotificationEnabledPreferences(context,
                        DataStoreUtils.getAccountKeys(context));
                for (final AccountPreferences pref : prefs) {
                    if (!pref.isDirectMessagesNotificationEnabled()) continue;
                    final StringLongPair[] pairs = readStateManager.getPositionPairs(CustomTabType.DIRECT_MESSAGES);
                    // TODO show messages notifications
                }
                notifyUnreadCountChanged(NOTIFICATION_ID_DIRECT_MESSAGES);
                break;
            }
            case TABLE_ID_DRAFTS: {
                break;
            }
        }
    }

    private long getPositionTag(String tag, UserKey accountKey) {
        final long position = readStateManager.getPosition(Utils.getReadPositionTagWithAccount(tag,
                accountKey));
        if (position != -1) return position;
        return readStateManager.getPosition(tag);
    }

    private void showTimelineNotification(SharedPreferences preferences, AccountPreferences pref, long position) {
        if (preferences.getBoolean(KEY_ENABLE_PUSH_NOTIFICATIONS, false)
                && preferences.getBoolean(GCM_TOKEN_SENT, false)) return;
        final UserKey accountKey = pref.getAccountKey();
        final Context context = getContext();
        if (context == null) return;
        final Resources resources = context.getResources();
        final NotificationManagerWrapper nm = notificationManager;
        final Expression selection = Expression.and(Expression.equalsArgs(Statuses.ACCOUNT_KEY),
                Expression.greaterThan(Statuses.POSITION_KEY, position));
        final String filteredSelection = DataStoreFunctionsKt.buildStatusFilterWhereClause(preferences,
                Statuses.TABLE_NAME, selection).getSQL();
        final String[] selectionArgs = {accountKey.toString()};
        final String[] userProjection = {Statuses.USER_KEY, Statuses.USER_NAME, Statuses.USER_SCREEN_NAME};
        final String[] statusProjection = {Statuses.POSITION_KEY};
        final Cursor statusCursor = databaseWrapper.query(Statuses.TABLE_NAME, statusProjection,
                filteredSelection, selectionArgs, null, null, Statuses.DEFAULT_SORT_ORDER);
        final Cursor userCursor = databaseWrapper.query(Statuses.TABLE_NAME, userProjection,
                filteredSelection, selectionArgs, Statuses.USER_KEY, null, Statuses.DEFAULT_SORT_ORDER);
        //noinspection TryFinallyCanBeTryWithResources
        try {
            final int usersCount = userCursor.getCount();
            final int statusesCount = statusCursor.getCount();
            if (statusesCount == 0 || usersCount == 0) return;
            final ParcelableStatusCursorIndices statusIndices = new ParcelableStatusCursorIndices(statusCursor),
                    userIndices = new ParcelableStatusCursorIndices(userCursor);
            final long positionKey = statusCursor.moveToFirst() ? statusCursor.getLong(statusIndices.position_key) : -1;
            final String notificationTitle = resources.getQuantityString(R.plurals.N_new_statuses,
                    statusesCount, statusesCount);
            final String notificationContent;
            userCursor.moveToFirst();
            final String displayName = userColorNameManager.getDisplayName(userCursor.getString(userIndices.user_key),
                    userCursor.getString(userIndices.user_name), userCursor.getString(userIndices.user_screen_name),
                    nameFirst);
            if (usersCount == 1) {
                notificationContent = context.getString(R.string.from_name, displayName);
            } else if (usersCount == 2) {
                userCursor.moveToPosition(1);
                final String othersName = userColorNameManager.getDisplayName(userCursor.getString(userIndices.user_key),
                        userCursor.getString(userIndices.user_name), userCursor.getString(userIndices.user_screen_name),
                        nameFirst);
                notificationContent = resources.getString(R.string.from_name_and_name, displayName, othersName);
            } else {
                notificationContent = resources.getString(R.string.from_name_and_N_others, displayName, usersCount - 1);
            }

            // Setup notification
            final NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
            builder.setAutoCancel(true);
            builder.setSmallIcon(R.drawable.ic_stat_twitter);
            builder.setTicker(notificationTitle);
            builder.setContentTitle(notificationTitle);
            builder.setContentText(notificationContent);
            builder.setCategory(NotificationCompat.CATEGORY_SOCIAL);
            builder.setContentIntent(getContentIntent(context, CustomTabType.HOME_TIMELINE,
                    NotificationType.HOME_TIMELINE, accountKey, positionKey));
            builder.setDeleteIntent(getMarkReadDeleteIntent(context, NotificationType.HOME_TIMELINE,
                    accountKey, positionKey, false));
            builder.setNumber(statusesCount);
            builder.setCategory(NotificationCompat.CATEGORY_SOCIAL);
            applyNotificationPreferences(builder, pref, pref.getHomeTimelineNotificationType());
            try {
                nm.notify("home_" + accountKey, Utils.getNotificationId(NOTIFICATION_ID_HOME_TIMELINE, accountKey), builder.build());
                Utils.sendPebbleNotification(context, null, notificationContent);
            } catch (SecurityException e) {
                // Silently ignore
            }
        } finally {
            statusCursor.close();
            userCursor.close();
        }
    }

    private void showInteractionsNotification(AccountPreferences pref, long position, boolean combined) {
        if (preferences.getBoolean(KEY_ENABLE_PUSH_NOTIFICATIONS, false)
                && preferences.getBoolean(GCM_TOKEN_SENT, false)) return;
        final Context context = getContext();
        if (context == null) return;
        final SQLiteDatabase db = databaseWrapper.getSQLiteDatabase();
        final UserKey accountKey = pref.getAccountKey();
        final String where = Expression.and(
                Expression.equalsArgs(Activities.ACCOUNT_KEY),
                Expression.greaterThanArgs(Activities.POSITION_KEY)
        ).getSQL();
        final String[] whereArgs = {accountKey.toString(), String.valueOf(position)};
        Cursor c = query(Activities.AboutMe.CONTENT_URI, Activities.COLUMNS, where, whereArgs,
                new OrderBy(Activities.TIMESTAMP, false).getSQL());
        if (c == null) return;
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        final StringBuilder pebbleNotificationStringBuilder = new StringBuilder();
        try {
            final int count = c.getCount();
            if (count == 0) return;
            builder.setSmallIcon(R.drawable.ic_stat_mention);
            builder.setCategory(NotificationCompat.CATEGORY_SOCIAL);
            applyNotificationPreferences(builder, pref, pref.getMentionsNotificationType());

            final Resources resources = context.getResources();
            final String accountName = DataStoreUtils.getAccountDisplayName(context, accountKey, nameFirst);
            builder.setContentText(accountName);
            final InboxStyle style = new InboxStyle();
            builder.setStyle(style);
            builder.setAutoCancel(true);
            style.setSummaryText(accountName);
            final ParcelableActivityCursorIndices ci = new ParcelableActivityCursorIndices(c);
            int messageLines = 0;

            long timestamp = -1;
            c.moveToPosition(-1);
            while (c.moveToNext()) {
                if (messageLines == 5) {
                    style.addLine(resources.getString(R.string.and_N_more, count - c.getPosition()));
                    pebbleNotificationStringBuilder.append(resources.getString(R.string.and_N_more, count - c.getPosition()));
                    break;
                }
                final ParcelableActivity activity = ci.newObject(c);
                if (pref.isNotificationMentionsOnly() && !ArrayUtils.contains(Activity.Action.MENTION_ACTIONS,
                        activity.action)) {
                    continue;
                }
                if (activity.status_id != null && InternalTwitterContentUtils.isFiltered(db,
                        activity.status_user_key, activity.status_text_plain,
                        activity.status_quote_text_plain, activity.status_spans,
                        activity.status_quote_spans, activity.status_source,
                        activity.status_quote_source, activity.status_retweeted_by_user_key,
                        activity.status_quoted_user_key)) {
                    continue;
                }
                final UserKey[] filteredUserIds = DataStoreUtils.getFilteredUserIds(context);
                if (timestamp == -1) {
                    timestamp = activity.timestamp;
                }
                ParcelableActivityUtils.INSTANCE.initAfterFilteredSourceIds(activity, filteredUserIds,
                        pref.isNotificationFollowingOnly());
                final ParcelableUser[] sources = ParcelableActivityUtils.INSTANCE.getAfterFilteredSources(activity);
                if (ArrayUtils.isEmpty(sources)) continue;
                final ActivityTitleSummaryMessage message = ActivityTitleSummaryMessage.get(context,
                        userColorNameManager, activity, sources,
                        0, useStarForLikes, nameFirst);
                if (message != null) {
                    final CharSequence summary = message.getSummary();
                    if (TextUtils.isEmpty(summary)) {
                        style.addLine(message.getTitle());
                        pebbleNotificationStringBuilder.append(message.getTitle());
                        pebbleNotificationStringBuilder.append("\n");
                    } else {
                        style.addLine(SpanFormatter.format(resources.getString(R.string.title_summary_line_format),
                                message.getTitle(), summary));
                        pebbleNotificationStringBuilder.append(message.getTitle());
                        pebbleNotificationStringBuilder.append(": ");
                        pebbleNotificationStringBuilder.append(message.getSummary());
                        pebbleNotificationStringBuilder.append("\n");
                    }
                    messageLines++;
                }
            }
            if (messageLines == 0) return;
            final int displayCount = messageLines + count - c.getPosition();
            final String title = resources.getQuantityString(R.plurals.N_new_interactions,
                    displayCount, displayCount);
            builder.setContentTitle(title);
            style.setBigContentTitle(title);
            builder.setNumber(displayCount);
            builder.setContentIntent(getContentIntent(context, CustomTabType.NOTIFICATIONS_TIMELINE,
                    NotificationType.INTERACTIONS, accountKey, timestamp));
            if (timestamp != -1) {
                builder.setDeleteIntent(getMarkReadDeleteIntent(context,
                        NotificationType.INTERACTIONS, accountKey, timestamp, false));
            }
        } catch (IOException e) {
            return;
        } finally {
            c.close();
        }
        final int notificationId = Utils.getNotificationId(NOTIFICATION_ID_INTERACTIONS_TIMELINE,
                accountKey);
        notificationManager.notify("interactions", notificationId, builder.build());

        Utils.sendPebbleNotification(context, context.getResources().getString(R.string.interactions), pebbleNotificationStringBuilder.toString());

    }

    private PendingIntent getContentIntent(final Context context, @CustomTabType final String type,
                                           @NotificationType final String notificationType,
                                           @Nullable final UserKey accountKey, final long readPosition) {
        // Setup click intent
        final Intent homeIntent = new Intent(context, HomeActivity.class);
        final Uri.Builder homeLinkBuilder = new Uri.Builder();
        homeLinkBuilder.scheme(SCHEME_TWITTNUKER);
        homeLinkBuilder.authority(type);
        if (accountKey != null)
            homeLinkBuilder.appendQueryParameter(QUERY_PARAM_ACCOUNT_KEY, accountKey.toString());
        homeLinkBuilder.appendQueryParameter(QUERY_PARAM_FROM_NOTIFICATION, String.valueOf(true));
        homeLinkBuilder.appendQueryParameter(QUERY_PARAM_TIMESTAMP, String.valueOf(System.currentTimeMillis()));
        homeLinkBuilder.appendQueryParameter(QUERY_PARAM_NOTIFICATION_TYPE, notificationType);
        if (readPosition > 0) {
            homeLinkBuilder.appendQueryParameter(QUERY_PARAM_READ_POSITION, String.valueOf(readPosition));
        }
        homeIntent.setData(homeLinkBuilder.build());
        homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return PendingIntent.getActivity(context, 0, homeIntent, 0);
    }

    private void applyNotificationPreferences(NotificationCompat.Builder builder, AccountPreferences pref, int defaultFlags) {
        int notificationDefaults = 0;
        if (AccountPreferences.isNotificationHasLight(defaultFlags)) {
            notificationDefaults |= NotificationCompat.DEFAULT_LIGHTS;
        }
        if (isNotificationAudible()) {
            if (AccountPreferences.isNotificationHasVibration(defaultFlags)) {
                notificationDefaults |= NotificationCompat.DEFAULT_VIBRATE;
            } else {
                notificationDefaults &= ~NotificationCompat.DEFAULT_VIBRATE;
            }
            if (AccountPreferences.isNotificationHasRingtone(defaultFlags)) {
                builder.setSound(pref.getNotificationRingtone(), AudioManager.STREAM_NOTIFICATION);
            }
        } else {
            notificationDefaults &= ~(NotificationCompat.DEFAULT_VIBRATE | NotificationCompat.DEFAULT_SOUND);
        }
        builder.setColor(pref.getNotificationLightColor());
        builder.setDefaults(notificationDefaults);
        builder.setOnlyAlertOnce(true);
    }

    private void setNotificationUri(final Cursor c, final Uri uri) {
        final ContentResolver cr = getContentResolver();
        if (cr == null || c == null || uri == null) return;
        c.setNotificationUri(cr, uri);
    }

    private void updatePreferences() {
        nameFirst = preferences.getBoolean(KEY_NAME_FIRST, false);
        useStarForLikes = preferences.getBoolean(KEY_I_WANT_MY_STARS_BACK, false);
    }

}