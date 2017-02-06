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

import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.SimpleArrayMap;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.apache.commons.collections.primitives.ArrayIntList;
import org.apache.commons.collections.primitives.ArrayLongList;
import org.apache.commons.collections.primitives.IntList;
import org.apache.commons.collections.primitives.LongList;
import org.mariotaku.abstask.library.AbstractTask;
import org.mariotaku.abstask.library.TaskStarter;
import de.vanita5.twittnuker.library.MicroBlog;
import de.vanita5.twittnuker.library.MicroBlogException;
import de.vanita5.twittnuker.library.twitter.http.HttpResponseCode;
import de.vanita5.twittnuker.library.twitter.model.DirectMessage;
import de.vanita5.twittnuker.library.twitter.model.ErrorInfo;
import de.vanita5.twittnuker.library.twitter.model.FriendshipUpdate;
import de.vanita5.twittnuker.library.twitter.model.Relationship;
import de.vanita5.twittnuker.library.twitter.model.SavedSearch;
import de.vanita5.twittnuker.library.twitter.model.User;
import de.vanita5.twittnuker.library.twitter.model.UserList;
import de.vanita5.twittnuker.library.twitter.model.UserListUpdate;
import org.mariotaku.sqliteqb.library.Expression;
import de.vanita5.twittnuker.BuildConfig;
import de.vanita5.twittnuker.R;
import de.vanita5.twittnuker.annotation.AccountType;
import de.vanita5.twittnuker.extension.model.AccountDetailsExtensionsKt;
import de.vanita5.twittnuker.model.AccountDetails;
import de.vanita5.twittnuker.model.ListResponse;
import de.vanita5.twittnuker.model.ParcelableActivity;
import de.vanita5.twittnuker.model.ParcelableStatus;
import de.vanita5.twittnuker.model.ParcelableUser;
import de.vanita5.twittnuker.model.ParcelableUserList;
import de.vanita5.twittnuker.model.RefreshTaskParam;
import de.vanita5.twittnuker.model.Response;
import de.vanita5.twittnuker.model.SimpleRefreshTaskParam;
import de.vanita5.twittnuker.model.SingleResponse;
import de.vanita5.twittnuker.model.UserKey;
import de.vanita5.twittnuker.model.message.FavoriteTaskEvent;
import de.vanita5.twittnuker.model.message.FriendshipUpdatedEvent;
import de.vanita5.twittnuker.model.message.GetMessagesTaskEvent;
import de.vanita5.twittnuker.model.message.GetStatusesTaskEvent;
import de.vanita5.twittnuker.model.message.SavedSearchDestroyedEvent;
import de.vanita5.twittnuker.model.message.StatusListChangedEvent;
import de.vanita5.twittnuker.model.message.StatusRetweetedEvent;
import de.vanita5.twittnuker.model.message.UserListCreatedEvent;
import de.vanita5.twittnuker.model.message.UserListDestroyedEvent;
import de.vanita5.twittnuker.model.message.UserListMembersChangedEvent;
import de.vanita5.twittnuker.model.message.UserListSubscriptionEvent;
import de.vanita5.twittnuker.model.message.UserListUpdatedEvent;
import de.vanita5.twittnuker.model.message.UsersBlockedEvent;
import de.vanita5.twittnuker.model.util.AccountUtils;
import de.vanita5.twittnuker.model.util.ParcelableStatusUtils;
import de.vanita5.twittnuker.model.util.ParcelableUserListUtils;
import de.vanita5.twittnuker.provider.TwidereDataStore.AccountSupportColumns;
import de.vanita5.twittnuker.provider.TwidereDataStore.Activities;
import de.vanita5.twittnuker.provider.TwidereDataStore.CachedRelationships;
import de.vanita5.twittnuker.provider.TwidereDataStore.DirectMessages;
import de.vanita5.twittnuker.provider.TwidereDataStore.DirectMessages.Inbox;
import de.vanita5.twittnuker.provider.TwidereDataStore.DirectMessages.Outbox;
import de.vanita5.twittnuker.provider.TwidereDataStore.Drafts;
import de.vanita5.twittnuker.provider.TwidereDataStore.Statuses;
import de.vanita5.twittnuker.service.LengthyOperationsService;
import de.vanita5.twittnuker.task.AcceptFriendshipTask;
import de.vanita5.twittnuker.task.AddUserListMembersTask;
import de.vanita5.twittnuker.task.CreateFriendshipTask;
import de.vanita5.twittnuker.task.CreateUserBlockTask;
import de.vanita5.twittnuker.task.CreateUserMuteTask;
import de.vanita5.twittnuker.task.DenyFriendshipTask;
import de.vanita5.twittnuker.task.DestroyFriendshipTask;
import de.vanita5.twittnuker.task.DestroyStatusTask;
import de.vanita5.twittnuker.task.DestroyUserBlockTask;
import de.vanita5.twittnuker.task.DestroyUserMuteTask;
import de.vanita5.twittnuker.task.GetActivitiesAboutMeTask;
import de.vanita5.twittnuker.task.GetHomeTimelineTask;
import de.vanita5.twittnuker.task.GetLocalTrendsTask;
import de.vanita5.twittnuker.task.GetReceivedDirectMessagesTask;
import de.vanita5.twittnuker.task.GetSavedSearchesTask;
import de.vanita5.twittnuker.task.GetSentDirectMessagesTask;
import de.vanita5.twittnuker.task.ManagedAsyncTask;
import de.vanita5.twittnuker.task.ReportSpamAndBlockTask;
import de.vanita5.twittnuker.task.twitter.GetActivitiesTask;
import de.vanita5.twittnuker.util.collection.CompactHashSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AsyncTwitterWrapper extends TwitterWrapper {

    private final Context context;
    private final ContentResolver resolver;

    private final AsyncTaskManager asyncTaskManager;
    private final SharedPreferencesWrapper preferences;
    private final Bus bus;

    private IntList mCreatingFavoriteIds = new ArrayIntList();
    private IntList mDestroyingFavoriteIds = new ArrayIntList();
    private IntList mCreatingRetweetIds = new ArrayIntList();
    public IntList destroyingStatusIds = new ArrayIntList();
    private IntList updatingRelationshipIds = new ArrayIntList();

    private final LongList sendingDraftIds = new ArrayLongList();

    private final Set<Uri> getMessageTasks = new CompactHashSet<>();
    private final Set<Uri> getStatusTasks = new CompactHashSet<>();

    public AsyncTwitterWrapper(Context context, Bus bus, SharedPreferencesWrapper preferences,
                               AsyncTaskManager asyncTaskManager) {
        this.context = context;
        resolver = context.getContentResolver();
        this.bus = bus;
        this.preferences = preferences;
        this.asyncTaskManager = asyncTaskManager;
        bus.register(new Object() {
            @Subscribe
            public void onGetDirectMessagesTaskEvent(GetMessagesTaskEvent event) {
                if (event.running) {
                    getMessageTasks.add(event.uri);
                } else {
                    getMessageTasks.remove(event.uri);
                }
            }

            @Subscribe
            public void onGetStatusesTaskEvent(GetStatusesTaskEvent event) {
                if (event.running) {
                    getStatusTasks.add(event.uri);
                } else {
                    getStatusTasks.remove(event.uri);
                }
            }
        });
    }

    public void acceptFriendshipAsync(final UserKey accountKey, final UserKey userKey) {
        final AcceptFriendshipTask task = new AcceptFriendshipTask(context);
        task.setup(accountKey, userKey);
        TaskStarter.execute(task);
    }

    public void addSendingDraftId(long id) {
        synchronized (sendingDraftIds) {
            sendingDraftIds.add(id);
            resolver.notifyChange(Drafts.CONTENT_URI_UNSENT, null);
        }
    }

    public int addUserListMembersAsync(final UserKey accountKey, final String listId, @NonNull final ParcelableUser... users) {
        final AddUserListMembersTask task = new AddUserListMembersTask(context, accountKey, listId, users);
        return asyncTaskManager.add(task, true);
    }

    public int cancelRetweetAsync(UserKey accountKey, String statusId, String myRetweetId) {
        if (myRetweetId != null)
            return destroyStatusAsync(accountKey, myRetweetId);
        else if (statusId != null)
            return destroyStatusAsync(accountKey, statusId);
        return -1;
    }

    public void clearNotificationAsync(final int notificationType) {
        clearNotificationAsync(notificationType, null);
    }

    public void clearNotificationAsync(final int notificationId, @Nullable final UserKey accountKey) {
        final ClearNotificationTask task = new ClearNotificationTask(notificationId, accountKey);
        AsyncTaskUtils.executeTask(task);
    }

    public void clearUnreadCountAsync(final int position) {
        final ClearUnreadCountTask task = new ClearUnreadCountTask(position);
        AsyncTaskUtils.executeTask(task);
    }

    public void createBlockAsync(final UserKey accountKey, final UserKey userKey, boolean filterEverywhere) {
        final CreateUserBlockTask task = new CreateUserBlockTask(context, filterEverywhere);
        task.setup(accountKey, userKey);
        TaskStarter.execute(task);
    }

    public int createFavoriteAsync(final UserKey accountKey, final String statusId) {
        final CreateFavoriteTask task = new CreateFavoriteTask(context, accountKey, statusId);
        return asyncTaskManager.add(task, true);
    }

    public void createFriendshipAsync(final UserKey accountKey, final UserKey userKey) {
        final CreateFriendshipTask task = new CreateFriendshipTask(context);
        task.setup(accountKey, userKey);
        TaskStarter.execute(task);
    }

    public int createMultiBlockAsync(final UserKey accountKey, final String[] userIds) {
        final CreateMultiBlockTask task = new CreateMultiBlockTask(accountKey, userIds);
        return asyncTaskManager.add(task, true);
    }

    public void createMuteAsync(final UserKey accountKey, final UserKey userKey, boolean filterEverywhere) {
        final CreateUserMuteTask task = new CreateUserMuteTask(context, filterEverywhere);
        task.setup(accountKey, userKey);
        TaskStarter.execute(task);
    }

    public int createSavedSearchAsync(final UserKey accountKey, final String query) {
        final CreateSavedSearchTask task = new CreateSavedSearchTask(accountKey, query);
        return asyncTaskManager.add(task, true);
    }

    public int createUserListAsync(final UserKey accountKey, final String listName, final boolean isPublic,
                                   final String description) {
        final CreateUserListTask task = new CreateUserListTask(context, accountKey, listName, isPublic,
                description);
        return asyncTaskManager.add(task, true);
    }

    public int createUserListSubscriptionAsync(final UserKey accountKey, final String listId) {
        final CreateUserListSubscriptionTask task = new CreateUserListSubscriptionTask(accountKey, listId);
        return asyncTaskManager.add(task, true);
    }

    public int deleteUserListMembersAsync(final UserKey accountKey, final String listId, final ParcelableUser... users) {
        final DeleteUserListMembersTask task = new DeleteUserListMembersTask(accountKey, listId, users);
        return asyncTaskManager.add(task, true);
    }

    public void denyFriendshipAsync(final UserKey accountKey, final UserKey userKey) {
        final DenyFriendshipTask task = new DenyFriendshipTask(context);
        task.setup(accountKey, userKey);
        TaskStarter.execute(task);
    }

    public void destroyBlockAsync(final UserKey accountKey, final UserKey userKey) {
        final DestroyUserBlockTask task = new DestroyUserBlockTask(context);
        task.setup(accountKey, userKey);
        TaskStarter.execute(task);
    }

    public int destroyDirectMessageAsync(final UserKey accountKey, final String messageId) {
        final DestroyDirectMessageTask task = new DestroyDirectMessageTask(accountKey, messageId);
        return asyncTaskManager.add(task, true);
    }

    public int destroyMessageConversationAsync(final UserKey accountKey, final String userId) {
        final DestroyMessageConversationTask task = new DestroyMessageConversationTask(accountKey, userId);
        return asyncTaskManager.add(task, true);
    }

    public int destroyFavoriteAsync(final UserKey accountKey, final String statusId) {
        final DestroyFavoriteTask task = new DestroyFavoriteTask(accountKey, statusId);
        return asyncTaskManager.add(task, true);
    }

    public void destroyFriendshipAsync(final UserKey accountKey, final UserKey userKey) {
        final DestroyFriendshipTask task = new DestroyFriendshipTask(context);
        task.setup(accountKey, userKey);
        TaskStarter.execute(task);
    }

    public void destroyMuteAsync(final UserKey accountKey, final UserKey userKey) {
        final DestroyUserMuteTask task = new DestroyUserMuteTask(context);
        task.setup(accountKey, userKey);
        TaskStarter.execute(task);
    }

    public int destroySavedSearchAsync(final UserKey accountKey, final long searchId) {
        final DestroySavedSearchTask task = new DestroySavedSearchTask(accountKey, searchId);
        return asyncTaskManager.add(task, true);
    }

    public int destroyStatusAsync(final UserKey accountKey, final String statusId) {
        final DestroyStatusTask task = new DestroyStatusTask(context,accountKey, statusId);
        return asyncTaskManager.add(task, true);
    }

    public int destroyUserListAsync(final UserKey accountKey, final String listId) {
        final DestroyUserListTask task = new DestroyUserListTask(context, accountKey, listId);
        return asyncTaskManager.add(task, true);
    }

    public int destroyUserListSubscriptionAsync(final UserKey accountKey, final String listId) {
        final DestroyUserListSubscriptionTask task = new DestroyUserListSubscriptionTask(accountKey, listId);
        return asyncTaskManager.add(task, true);
    }

    public Context getContext() {
        return context;
    }

    public boolean getHomeTimelineAsync(RefreshTaskParam param) {
        final GetHomeTimelineTask task = new GetHomeTimelineTask(getContext());
        task.setParams(param);
        TaskStarter.execute(task);
        return true;
    }

    public void getLocalTrendsAsync(final UserKey accountId, final int woeId) {
        final GetLocalTrendsTask task = new GetLocalTrendsTask(context, accountId, woeId);
        TaskStarter.execute(task);
    }

    public void getReceivedDirectMessagesAsync(RefreshTaskParam param) {
        final GetReceivedDirectMessagesTask task = new GetReceivedDirectMessagesTask(context);
        task.setParams(param);
        TaskStarter.execute(task);
    }

    public void getSentDirectMessagesAsync(RefreshTaskParam param) {
        final GetSentDirectMessagesTask task = new GetSentDirectMessagesTask(context);
        task.setParams(param);
        TaskStarter.execute(task);
    }

    public void getSavedSearchesAsync(UserKey[] accountKeys) {
        final GetSavedSearchesTask task = new GetSavedSearchesTask(context);
        task.setParams(accountKeys);
        TaskStarter.execute(task);
    }

    @NonNull
    public long[] getSendingDraftIds() {
        return sendingDraftIds.toArray();
    }

    public boolean isCreatingFavorite(@Nullable final UserKey accountId, @Nullable final String statusId) {
        return mCreatingFavoriteIds.contains(calculateHashCode(accountId, statusId));
    }

    public boolean isCreatingRetweet(@Nullable final UserKey accountKey, @Nullable final String statusId) {
        return mCreatingRetweetIds.contains(calculateHashCode(accountKey, statusId));
    }

    public boolean isDestroyingFavorite(@Nullable final UserKey accountKey, @Nullable final String statusId) {
        return mDestroyingFavoriteIds.contains(calculateHashCode(accountKey, statusId));
    }

    public boolean isDestroyingStatus(@Nullable final UserKey accountId, @Nullable final String statusId) {
        return destroyingStatusIds.contains(calculateHashCode(accountId, statusId));
    }

    public static int calculateHashCode(@Nullable final UserKey accountId, @Nullable final String statusId) {
        return (accountId == null ? 0 : accountId.hashCode()) ^ (statusId == null ? 0 : statusId.hashCode());
    }

    public boolean isStatusTimelineRefreshing(Uri uri) {
        return getStatusTasks.contains(uri);
    }

    public void refreshAll() {
        refreshAll(new GetAccountKeysClosure() {
            @Override
            public UserKey[] getAccountKeys() {
                return DataStoreUtils.getActivatedAccountKeys(context);
            }
        });
    }

    public boolean refreshAll(final UserKey[] accountKeys) {
        return refreshAll(new GetAccountKeysClosure() {
            @Override
            public UserKey[] getAccountKeys() {
                return accountKeys;
            }
        });
    }

    public boolean refreshAll(final GetAccountKeysClosure closure) {
        getHomeTimelineAsync(new SimpleRefreshTaskParam() {

            @NonNull
            @Override
            public UserKey[] getAccountKeysWorker() {
                return closure.getAccountKeys();
            }

            @Nullable
            @Override
            public String[] getSinceIds() {
                return DataStoreUtils.getNewestStatusIds(context, Statuses.CONTENT_URI,
                        getAccountKeys());
            }
        });
        if (preferences.getBoolean(KEY_HOME_REFRESH_MENTIONS)) {
            getActivitiesAboutMeAsync(new SimpleRefreshTaskParam() {
                @NonNull
                @Override
                public UserKey[] getAccountKeysWorker() {
                    return closure.getAccountKeys();
                }

                @Nullable
                @Override
                public String[] getSinceIds() {
                    return DataStoreUtils.getNewestActivityMaxPositions(context,
                            Activities.AboutMe.CONTENT_URI, getAccountKeys());
                    }
            });
        }
        if (preferences.getBoolean(KEY_HOME_REFRESH_DIRECT_MESSAGES)) {
            getReceivedDirectMessagesAsync(new SimpleRefreshTaskParam() {
                @NonNull
                @Override
                public UserKey[] getAccountKeysWorker() {
                    return closure.getAccountKeys();
                }
            });
            getSentDirectMessagesAsync(new SimpleRefreshTaskParam() {
                @NonNull
                @Override
                public UserKey[] getAccountKeysWorker() {
                    return closure.getAccountKeys();
                }
            });
        }
        if (preferences.getBoolean(KEY_HOME_REFRESH_SAVED_SEARCHES)) {
            getSavedSearchesAsync(closure.getAccountKeys());
        }
        return true;
    }

    public void removeSendingDraftId(long id) {
        synchronized (sendingDraftIds) {
            sendingDraftIds.removeElement(id);
            resolver.notifyChange(Drafts.CONTENT_URI_UNSENT, null);
        }
    }

    public void removeUnreadCountsAsync(final int position, final SimpleArrayMap<UserKey, Set<String>> counts) {
        final RemoveUnreadCountsTask task = new RemoveUnreadCountsTask(position, counts);
        AsyncTaskUtils.executeTask(task);
    }

    public void reportMultiSpam(final UserKey accountKey, final String[] userIds) {
        // TODO implementation
    }

    public void reportSpamAsync(final UserKey accountKey, final UserKey userKey) {
        final ReportSpamAndBlockTask task = new ReportSpamAndBlockTask(context);
        task.setup(accountKey, userKey);
        TaskStarter.execute(task);
    }

    public int retweetStatusAsync(final UserKey accountKey, final String statusId) {
        final RetweetStatusTask task = new RetweetStatusTask(context, accountKey, statusId);
        return asyncTaskManager.add(task, true);
    }

    public int sendDirectMessageAsync(final UserKey accountKey, final String recipientId, final String text,
                                      final String imageUri) {
        final Intent intent = new Intent(context, LengthyOperationsService.class);
        intent.setAction(INTENT_ACTION_SEND_DIRECT_MESSAGE);
        intent.putExtra(EXTRA_ACCOUNT_KEY, accountKey);
        intent.putExtra(EXTRA_RECIPIENT_ID, recipientId);
        intent.putExtra(EXTRA_TEXT, text);
        intent.putExtra(EXTRA_IMAGE_URI, imageUri);
        context.startService(intent);
        return 0;
    }

    public int updateUserListDetails(final UserKey accountKey, final String listId,
                                     final UserListUpdate update) {
        final UpdateUserListDetailsTask task = new UpdateUserListDetailsTask(context, accountKey,
                listId, update);
        return asyncTaskManager.add(task, true);
    }

    public static <T extends Response<?>> Exception getException(List<T> responses) {
        for (T response : responses) {
            if (response.hasException()) return response.getException();
        }
        return null;
    }

    public void updateFriendship(final UserKey accountKey, final UserKey userKey, final FriendshipUpdate update) {
        final Bus bus = this.bus;
        if (bus == null) return;
        TaskStarter.execute(new AbstractTask<Object, SingleResponse<Relationship>, Bus>() {
            @Override
            public SingleResponse<Relationship> doLongOperation(Object param) {
                final MicroBlog microBlog = MicroBlogAPIFactory.getInstance(context, accountKey);
                try {
                    final Relationship relationship = microBlog.updateFriendship(userKey.getId(), update);
                    if (!relationship.isSourceWantRetweetsFromTarget()) {
                        // TODO remove cached retweets
                        final Expression where = Expression.and(
                                Expression.equalsArgs(Statuses.ACCOUNT_KEY),
                                Expression.equalsArgs(Statuses.RETWEETED_BY_USER_KEY)
                        );
                        final String[] selectionArgs = {accountKey.toString(), userKey.toString()};
                        context.getContentResolver().delete(Statuses.CONTENT_URI, where.getSQL(), selectionArgs);
                    }
                    return SingleResponse.Companion.getInstance(relationship);
                } catch (MicroBlogException e) {
                    return SingleResponse.Companion.getInstance(e);
                }
            }

            @Override
            public void afterExecute(Bus handler, SingleResponse<Relationship> result) {
                if (result.hasData()) {
                    handler.post(new FriendshipUpdatedEvent(accountKey, userKey, result.getData()));
                } else if (result.hasException()) {
                    DebugLog.w(LOGTAG, "Unable to update friendship", result.getException());
                }
            }
        }.setCallback(bus));
    }

    public void getActivitiesAboutMeAsync(final RefreshTaskParam param) {
        final GetActivitiesTask task = new GetActivitiesAboutMeTask(getContext());
        task.setParams(param);
        TaskStarter.execute(task);
    }

    public void setActivitiesAboutMeUnreadAsync(final UserKey[] accountKeys, final long cursor) {
        AbstractTask<Object, Object, AsyncTwitterWrapper> task = new AbstractTask<Object, Object, AsyncTwitterWrapper>() {

            @Override
            public Object doLongOperation(Object o) {
                for (UserKey accountId : accountKeys) {
                    MicroBlog microBlog = MicroBlogAPIFactory.getInstance(context, accountId);
                    if (!Utils.isOfficialCredentials(context, accountId)) continue;
                    try {
                        microBlog.setActivitiesAboutMeUnread(cursor);
                    } catch (MicroBlogException e) {
                        DebugLog.w(LOGTAG, null, e);
                    }
                }
                return null;
            }
        };
        TaskStarter.execute(task);
    }

    public void addUpdatingRelationshipId(UserKey accountKey, UserKey userId) {
        updatingRelationshipIds.add(ParcelableUser.calculateHashCode(accountKey, userId));
    }

    public void removeUpdatingRelationshipId(UserKey accountKey, UserKey userId) {
        updatingRelationshipIds.removeElement(ParcelableUser.calculateHashCode(accountKey, userId));
    }

    public boolean isUpdatingRelationship(UserKey accountId, UserKey userId) {
        return updatingRelationshipIds.contains(ParcelableUser.calculateHashCode(accountId, userId));
    }

    final class ClearNotificationTask extends AsyncTask<Object, Object, Integer> {
        private final int notificationType;
        private final UserKey accountKey;

        ClearNotificationTask(final int notificationType, final UserKey accountKey) {
            this.notificationType = notificationType;
            this.accountKey = accountKey;
        }

        @Override
        protected Integer doInBackground(final Object... params) {
            return clearNotification(context, notificationType, accountKey);
        }

    }

    final class ClearUnreadCountTask extends AsyncTask<Object, Object, Integer> {
        private final int position;

        ClearUnreadCountTask(final int position) {
            this.position = position;
        }

        @Override
        protected Integer doInBackground(final Object... params) {
            return clearUnreadCount(context, position);
        }

    }

    class CreateFavoriteTask extends ManagedAsyncTask<Object, Object, SingleResponse<ParcelableStatus>> {

        private final UserKey mAccountKey;
        private final String mStatusId;

        public CreateFavoriteTask(Context context, final UserKey accountKey, final String statusId) {
            super(context);
            this.mAccountKey = accountKey;
            this.mStatusId = statusId;
        }

        @Override
        protected SingleResponse<ParcelableStatus> doInBackground(final Object... params) {
            final ContentResolver resolver = getContext().getContentResolver();
            final AccountDetails details = AccountUtils.getAccountDetails(AccountManager.get(getContext()), mAccountKey, true);
            if (details == null) return SingleResponse.Companion.getInstance();
            final MicroBlog microBlog = AccountDetailsExtensionsKt.newMicroBlogInstance(details,
                    getContext(), MicroBlog.class);
            if (microBlog == null) return SingleResponse.Companion.getInstance();
            try {
                final ParcelableStatus result;
                switch (details.type) {
                    case AccountType.FANFOU: {
                        result = ParcelableStatusUtils.INSTANCE.fromStatus(microBlog.createFanfouFavorite(mStatusId),
                                mAccountKey, false);
                        break;
                    }
                    default: {
                        result = ParcelableStatusUtils.INSTANCE.fromStatus(microBlog.createFavorite(mStatusId),
                                mAccountKey, false);
                    }
                }
                ParcelableStatusUtils.INSTANCE.updateExtraInformation(result, details
                );
                Utils.setLastSeen(getContext(), result.mentions, System.currentTimeMillis());
                final ContentValues values = new ContentValues();
                values.put(Statuses.IS_FAVORITE, true);
                values.put(Statuses.REPLY_COUNT, result.reply_count);
                values.put(Statuses.RETWEET_COUNT, result.retweet_count);
                values.put(Statuses.FAVORITE_COUNT, result.favorite_count);
                final String statusWhere = Expression.and(
                        Expression.equalsArgs(Statuses.ACCOUNT_KEY),
                        Expression.or(
                                Expression.equalsArgs(Statuses.STATUS_ID),
                                Expression.equalsArgs(Statuses.RETWEET_ID)
                        )
                ).getSQL();
                final String[] statusWhereArgs = {mAccountKey.toString(), String.valueOf(mStatusId),
                        String.valueOf(mStatusId)};
                for (final Uri uri : DataStoreUtils.STATUSES_URIS) {
                    resolver.update(uri, values, statusWhere, statusWhereArgs);
                }
                DataStoreUtils.updateActivityStatus(resolver, mAccountKey, mStatusId, new DataStoreUtils.UpdateActivityAction() {
                    @Override
                    public void process(ParcelableActivity activity) {
                        ParcelableStatus[][] statusesMatrix = {activity.target_statuses,
                                activity.target_object_statuses};
                        for (ParcelableStatus[] statusesArray : statusesMatrix) {
                            if (statusesArray == null) continue;
                            for (ParcelableStatus status : statusesArray) {
                                if (!result.id.equals(status.id)) continue;
                                status.is_favorite = true;
                                status.reply_count = result.reply_count;
                                status.retweet_count = result.retweet_count;
                                status.favorite_count = result.favorite_count;
                            }
                        }
                    }
                });
                return SingleResponse.Companion.getInstance(result);
            } catch (final MicroBlogException e) {
                DebugLog.w(LOGTAG, null, e);
                return SingleResponse.Companion.getInstance(e);
            }
        }


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            final int hashCode = calculateHashCode(mAccountKey, mStatusId);
            if (!mCreatingFavoriteIds.contains(hashCode)) {
                mCreatingFavoriteIds.add(hashCode);
            }
            getBus().post(new StatusListChangedEvent());
        }

        @Override
        protected void onPostExecute(final SingleResponse<ParcelableStatus> result) {
            mCreatingFavoriteIds.removeElement(calculateHashCode(mAccountKey, mStatusId));
            final FavoriteTaskEvent taskEvent = new FavoriteTaskEvent(FavoriteTaskEvent.Action.CREATE,
                    mAccountKey, mStatusId);
            taskEvent.setFinished(true);
            if (result.hasData()) {
                final ParcelableStatus status = result.getData();
                taskEvent.setStatus(status);
                taskEvent.setSucceeded(true);
            } else {
                taskEvent.setSucceeded(false);
                Utils.showErrorMessage(getContext(), R.string.action_favoriting, result.getException(), true);
            }
            getBus().post(taskEvent);
            getBus().post(new StatusListChangedEvent());
            super.onPostExecute(result);
        }

    }

    class CreateMultiBlockTask extends ManagedAsyncTask<Object, Object, ListResponse<String>> {

        private final UserKey mAccountKey;
        private final String[] mUserIds;

        public CreateMultiBlockTask(final UserKey accountKey, final String[] userIds) {
            super(context);
            this.mAccountKey = accountKey;
            this.mUserIds = userIds;
        }

        private void deleteCaches(final List<String> list) {
            for (final Uri uri : DataStoreUtils.STATUSES_URIS) {
                // TODO delete caches
                // ContentResolverUtils.bulkDelete(mResolver, uri, Statuses.USER_ID, list,
                // Statuses.ACCOUNT_ID + " = " + mAccountKey, false);
            }
            // I bet you don't want to see these users in your auto complete list.
            //TODO insert to blocked users data
            final ContentValues values = new ContentValues();
            values.put(CachedRelationships.BLOCKING, true);
            values.put(CachedRelationships.FOLLOWING, false);
            values.put(CachedRelationships.FOLLOWED_BY, false);
            final String where = Expression.inArgs(CachedRelationships.USER_KEY, list.size()).getSQL();
            final String[] selectionArgs = list.toArray(new String[list.size()]);
            resolver.update(CachedRelationships.CONTENT_URI, values, where, selectionArgs);
        }

        @Override
        protected ListResponse<String> doInBackground(final Object... params) {
            final List<String> blockedUsers = new ArrayList<>();
            final MicroBlog microBlog = MicroBlogAPIFactory.getInstance(context, mAccountKey);
            if (microBlog != null) {
                for (final String userId : mUserIds) {
                    try {
                        final User user = microBlog.createBlock(userId);
                        blockedUsers.add(user.getId());
                    } catch (final MicroBlogException e) {
                        deleteCaches(blockedUsers);
                        return ListResponse.getListInstance(e);
                    }
                }
            }
            deleteCaches(blockedUsers);
            return ListResponse.getListInstance(blockedUsers);
        }

        @Override
        protected void onPostExecute(final ListResponse<String> result) {
            if (result.hasData()) {
                Utils.showInfoMessage(context, R.string.users_blocked, false);
            } else {
                Utils.showErrorMessage(context, R.string.action_blocking, result.getException(), true);
            }
            AsyncTwitterWrapper.this.bus.post(new UsersBlockedEvent(mAccountKey, mUserIds));
            super.onPostExecute(result);
        }


    }

    class CreateSavedSearchTask extends ManagedAsyncTask<Object, Object, SingleResponse<SavedSearch>> {

        private final UserKey mAccountKey;
        private final String mQuery;

        CreateSavedSearchTask(final UserKey accountKey, final String query) {
            super(context);
            mAccountKey = accountKey;
            mQuery = query;
        }

        @Override
        protected SingleResponse<SavedSearch> doInBackground(final Object... params) {
            final MicroBlog microBlog = MicroBlogAPIFactory.getInstance(context, mAccountKey);
            if (microBlog == null) return null;
            try {
                return SingleResponse.Companion.getInstance(microBlog.createSavedSearch(mQuery));
            } catch (final MicroBlogException e) {
                return SingleResponse.Companion.getInstance(e);
            }
        }

        @Override
        protected void onPostExecute(final SingleResponse<SavedSearch> result) {
            if (result.hasData()) {
                final String message = context.getString(R.string.message_toast_search_name_saved, result.getData().getQuery());
                Utils.showOkMessage(context, message, false);
            } else if (result.hasException()) {
                final Exception exception = result.getException();
                // https://github.com/TwidereProject/Twidere-Android/issues/244
                if (exception instanceof MicroBlogException && ((MicroBlogException) exception).getStatusCode() == 403) {
                    final String desc = context.getString(R.string.saved_searches_already_saved_hint);
                    Utils.showErrorMessage(context, R.string.action_saving_search, desc, false);
                } else {
                    Utils.showErrorMessage(context, R.string.action_saving_search, exception, false);
                }
            }
            super.onPostExecute(result);
        }

    }

    class CreateUserListSubscriptionTask extends ManagedAsyncTask<Object, Object, SingleResponse<ParcelableUserList>> {

        private final UserKey mAccountKey;
        private final String mListId;

        public CreateUserListSubscriptionTask(final UserKey accountKey, final String listId) {
            super(context);
            this.mAccountKey = accountKey;
            this.mListId = listId;
        }

        @Override
        protected SingleResponse<ParcelableUserList> doInBackground(final Object... params) {
            final MicroBlog microBlog = MicroBlogAPIFactory.getInstance(context, mAccountKey);
            if (microBlog == null) return SingleResponse.Companion.getInstance();
            try {
                final UserList userList = microBlog.createUserListSubscription(mListId);
                final ParcelableUserList list = ParcelableUserListUtils.from(userList, mAccountKey);
                return SingleResponse.Companion.getInstance(list);
            } catch (final MicroBlogException e) {
                return SingleResponse.Companion.getInstance(e);
            }
        }

        @Override
        protected void onPostExecute(final SingleResponse<ParcelableUserList> result) {
            final boolean succeed = result.hasData();
            if (succeed) {
                final String message = context.getString(R.string.subscribed_to_list, result.getData().name);
                Utils.showOkMessage(context, message, false);
                getBus().post(new UserListSubscriptionEvent(UserListSubscriptionEvent.Action.SUBSCRIBE,
                        result.getData()));
            } else {
                Utils.showErrorMessage(context, R.string.action_subscribing_to_list, result.getException(), true);
            }
            super.onPostExecute(result);
        }

    }

    static class CreateUserListTask extends ManagedAsyncTask<Object, Object, SingleResponse<ParcelableUserList>> {

        private final UserKey mAccountKey;
        private final String mListName, mDescription;
        private final boolean mIsPublic;

        public CreateUserListTask(Context context, final UserKey accountKey, final String listName,
                                  final boolean isPublic, final String description) {
            super(context);
            this.mAccountKey = accountKey;
            this.mListName = listName;
            this.mDescription = description;
            this.mIsPublic = isPublic;
        }

        @Override
        protected SingleResponse<ParcelableUserList> doInBackground(final Object... params) {
            final MicroBlog microBlog = MicroBlogAPIFactory.getInstance(getContext(), mAccountKey
            );
            if (microBlog == null || mListName == null)
                return SingleResponse.Companion.getInstance();
            try {
                final UserListUpdate userListUpdate = new UserListUpdate();
                userListUpdate.setName(mListName);
                userListUpdate.setMode(mIsPublic ? UserList.Mode.PUBLIC : UserList.Mode.PRIVATE);
                userListUpdate.setDescription(mDescription);
                final UserList list = microBlog.createUserList(userListUpdate);
                return SingleResponse.Companion.getInstance(ParcelableUserListUtils.from(list, mAccountKey));
            } catch (final MicroBlogException e) {
                return SingleResponse.Companion.getInstance(e);
            }
        }

        @Override
        protected void onPostExecute(final SingleResponse<ParcelableUserList> result) {
            final Context context = getContext();
            if (result.hasData()) {
                final ParcelableUserList userList = result.getData();
                final String message = context.getString(R.string.created_list, userList.name);
                Utils.showOkMessage(context, message, false);
                getBus().post(new UserListCreatedEvent(userList));
            } else {
                Utils.showErrorMessage(context, R.string.action_creating_list, result.getException(), true);
            }
            super.onPostExecute(result);
        }

    }

    class DeleteUserListMembersTask extends ManagedAsyncTask<Object, Object, SingleResponse<ParcelableUserList>> {

        private final UserKey mAccountKey;
        private final String mUserListId;
        private final ParcelableUser[] users;

        public DeleteUserListMembersTask(final UserKey accountKey, final String userListId, final ParcelableUser[] users) {
            super(context);
            mAccountKey = accountKey;
            mUserListId = userListId;
            this.users = users;
        }

        @Override
        protected SingleResponse<ParcelableUserList> doInBackground(final Object... params) {
            final MicroBlog microBlog = MicroBlogAPIFactory.getInstance(context, mAccountKey);
            if (microBlog == null) return SingleResponse.Companion.getInstance();
            try {
                final UserKey[] userIds = new UserKey[users.length];
                for (int i = 0, j = users.length; i < j; i++) {
                    userIds[i] = users[i].key;
                }
                final UserList userList = microBlog.deleteUserListMembers(mUserListId, UserKey.getIds(userIds));
                final ParcelableUserList list = ParcelableUserListUtils.from(userList, mAccountKey);
                return SingleResponse.Companion.getInstance(list);
            } catch (final MicroBlogException e) {
                return SingleResponse.Companion.getInstance(e);
            }
        }

        @Override
        protected void onPostExecute(final SingleResponse<ParcelableUserList> result) {
            final boolean succeed = result.hasData();
            final String message;
            if (succeed) {
                if (users.length == 1) {
                    final ParcelableUser user = users[0];
                    final boolean nameFirst = getPreferences().getBoolean(KEY_NAME_FIRST);
                    final String displayName = getUserColorNameManager().getDisplayName(user.key,
                            user.name, user.screen_name, nameFirst);
                    message = context.getString(R.string.deleted_user_from_list, displayName,
                            result.getData().name);
                } else {
                    final Resources res = context.getResources();
                    message = res.getQuantityString(R.plurals.deleted_N_users_from_list, users.length, users.length,
                            result.getData().name);
                }
                getBus().post(new UserListMembersChangedEvent(UserListMembersChangedEvent.Action.REMOVED,
                        result.getData(), users));
                Utils.showInfoMessage(context, message, false);
            } else {
                Utils.showErrorMessage(context, R.string.action_deleting, result.getException(), true);
            }
            super.onPostExecute(result);
        }

    }


    class DestroyDirectMessageTask extends ManagedAsyncTask<Object, Object, SingleResponse<DirectMessage>> {

        private final UserKey mAccountKey;
        private final String mMessageId;

        public DestroyDirectMessageTask(final UserKey accountKey, final String messageId) {
            super(context);
            mAccountKey = accountKey;
            mMessageId = messageId;
        }

        private void deleteMessages() {
            final String where = Expression.and(Expression.equalsArgs(DirectMessages.ACCOUNT_KEY),
                    Expression.equalsArgs(DirectMessages.MESSAGE_ID)).getSQL();
            final String[] whereArgs = new String[]{mAccountKey.toString(), mMessageId};
            resolver.delete(DirectMessages.Inbox.CONTENT_URI, where, whereArgs);
            resolver.delete(DirectMessages.Outbox.CONTENT_URI, where, whereArgs);
        }

        private boolean isMessageNotFound(final Exception e) {
            if (!(e instanceof MicroBlogException)) return false;
            final MicroBlogException te = (MicroBlogException) e;
            return te.getErrorCode() == ErrorInfo.PAGE_NOT_FOUND
                    || te.getStatusCode() == HttpResponseCode.NOT_FOUND;
        }

        @Override
        protected SingleResponse<DirectMessage> doInBackground(final Object... args) {
            final MicroBlog microBlog = MicroBlogAPIFactory.getInstance(context, mAccountKey);
            if (microBlog == null) return SingleResponse.Companion.getInstance();
            try {
                final DirectMessage message = microBlog.destroyDirectMessage(mMessageId);
                deleteMessages();
                return SingleResponse.Companion.getInstance(message);
            } catch (final MicroBlogException e) {
                if (isMessageNotFound(e)) {
                    deleteMessages();
                }
                return SingleResponse.Companion.getInstance(e);
            }
        }


        @Override
        protected void onPostExecute(final SingleResponse<DirectMessage> result) {
            super.onPostExecute(result);
            if (result == null) return;
            if (result.hasData() || isMessageNotFound(result.getException())) {
                Utils.showInfoMessage(context, R.string.message_direct_message_deleted, false);
            } else {
                Utils.showErrorMessage(context, R.string.action_deleting, result.getException(), true);
            }
        }


    }


    class DestroyMessageConversationTask extends ManagedAsyncTask<Object, Object, SingleResponse<Void>> {

        private final String mUserId;
        private final UserKey mAccountKey;

        public DestroyMessageConversationTask(final UserKey accountKey, final String userId) {
            super(context);
            mAccountKey = accountKey;
            mUserId = userId;
        }

        private void deleteMessages(final UserKey accountKey, final String userId) {
            final String[] whereArgs = {accountKey.toString(), userId};
            resolver.delete(DirectMessages.Inbox.CONTENT_URI, Expression.and(
                    Expression.equalsArgs(AccountSupportColumns.ACCOUNT_KEY),
                    Expression.equalsArgs(Inbox.SENDER_ID)
            ).getSQL(), whereArgs);
            resolver.delete(DirectMessages.Outbox.CONTENT_URI, Expression.and(
                    Expression.equalsArgs(AccountSupportColumns.ACCOUNT_KEY),
                    Expression.equalsArgs(Outbox.RECIPIENT_ID)
            ).getSQL(), whereArgs);
        }

        private boolean isMessageNotFound(final Exception e) {
            if (!(e instanceof MicroBlogException)) return false;
            final MicroBlogException te = (MicroBlogException) e;
            return te.getErrorCode() == ErrorInfo.PAGE_NOT_FOUND
                    || te.getStatusCode() == HttpResponseCode.NOT_FOUND;
        }

        @Override
        protected SingleResponse<Void> doInBackground(final Object... args) {
            final MicroBlog microBlog = MicroBlogAPIFactory.getInstance(context, mAccountKey);
            if (microBlog == null) return SingleResponse.Companion.getInstance();
            try {
                microBlog.destroyDirectMessagesConversation(mAccountKey.getId(), mUserId);
                deleteMessages(mAccountKey, mUserId);
                return SingleResponse.Companion.getInstance();
            } catch (final MicroBlogException e) {
                if (isMessageNotFound(e)) {
                    deleteMessages(mAccountKey, mUserId);
                }
                return SingleResponse.Companion.getInstance(e);
            }
        }


        @Override
        protected void onPostExecute(final SingleResponse<Void> result) {
            super.onPostExecute(result);
            if (result == null) return;
            if (result.hasData() || isMessageNotFound(result.getException())) {
                Utils.showInfoMessage(context, R.string.message_direct_message_deleted, false);
            } else {
                Utils.showErrorMessage(context, R.string.action_deleting, result.getException(), true);
            }
        }


    }


    class DestroyFavoriteTask extends ManagedAsyncTask<Object, Object, SingleResponse<ParcelableStatus>> {

        @NonNull
        private final UserKey mAccountKey;
        private final String mStatusId;

        public DestroyFavoriteTask(@NonNull final UserKey accountKey, final String statusId) {
            super(context);
            this.mAccountKey = accountKey;
            this.mStatusId = statusId;
        }

        @Override
        protected SingleResponse<ParcelableStatus> doInBackground(final Object... params) {
            final AccountDetails details = AccountUtils.getAccountDetails(AccountManager.get(context), mAccountKey, true);
            if (details == null) return SingleResponse.Companion.getInstance();
            final MicroBlog microBlog = AccountDetailsExtensionsKt.newMicroBlogInstance(details,
                    getContext(), MicroBlog.class);
            if (microBlog == null) return SingleResponse.Companion.getInstance();
            try {
                final ParcelableStatus result;
                switch (details.type) {
                    case AccountType.FANFOU: {
                        result = ParcelableStatusUtils.INSTANCE.fromStatus(microBlog.destroyFanfouFavorite(mStatusId),
                                mAccountKey, false);
                        break;
                    }
                    default: {
                        result = ParcelableStatusUtils.INSTANCE.fromStatus(microBlog.destroyFavorite(mStatusId),
                                mAccountKey, false);
                    }
                }
                final ContentValues values = new ContentValues();
                values.put(Statuses.IS_FAVORITE, false);
                values.put(Statuses.FAVORITE_COUNT, result.favorite_count - 1);
                values.put(Statuses.RETWEET_COUNT, result.retweet_count);
                values.put(Statuses.REPLY_COUNT, result.reply_count);

                final Expression where = Expression.and(Expression.equalsArgs(Statuses.ACCOUNT_KEY),
                        Expression.or(Expression.equalsArgs(Statuses.STATUS_ID),
                                Expression.equalsArgs(Statuses.RETWEET_ID)));
                final String[] whereArgs = {mAccountKey.toString(), mStatusId, mStatusId};
                for (final Uri uri : DataStoreUtils.STATUSES_URIS) {
                    resolver.update(uri, values, where.getSQL(), whereArgs);
                }

                DataStoreUtils.updateActivityStatus(resolver, mAccountKey, mStatusId, new DataStoreUtils.UpdateActivityAction() {
                    @Override
                    public void process(ParcelableActivity activity) {
                        ParcelableStatus[][] statusesMatrix = {activity.target_statuses,
                                activity.target_object_statuses};
                        for (ParcelableStatus[] statusesArray : statusesMatrix) {
                            if (statusesArray == null) continue;
                            for (ParcelableStatus status : statusesArray) {
                                if (!result.id.equals(status.id)) continue;
                                status.is_favorite = false;
                                status.reply_count = result.reply_count;
                                status.retweet_count = result.retweet_count;
                                status.favorite_count = result.favorite_count - 1;
                            }
                        }
                    }
                });
                return SingleResponse.Companion.getInstance(result);
            } catch (final MicroBlogException e) {
                return SingleResponse.Companion.getInstance(e);
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            final int hashCode = calculateHashCode(mAccountKey, mStatusId);
            if (!mDestroyingFavoriteIds.contains(hashCode)) {
                mDestroyingFavoriteIds.add(hashCode);
            }
            getBus().post(new StatusListChangedEvent());
        }

        @Override
        protected void onPostExecute(final SingleResponse<ParcelableStatus> result) {
            mDestroyingFavoriteIds.removeElement(calculateHashCode(mAccountKey, mStatusId));
            final FavoriteTaskEvent taskEvent = new FavoriteTaskEvent(FavoriteTaskEvent.Action.DESTROY,
                    mAccountKey, mStatusId);
            taskEvent.setFinished(true);
            if (result.hasData()) {
                final ParcelableStatus status = result.getData();
                taskEvent.setStatus(status);
                taskEvent.setSucceeded(true);
                Utils.showInfoMessage(context, R.string.message_toast_status_unfavorited, false);
            } else {
                taskEvent.setSucceeded(false);
                Utils.showErrorMessage(context, R.string.action_unfavoriting, result.getException(), true);
            }
            getBus().post(taskEvent);
            getBus().post(new StatusListChangedEvent());
            super.onPostExecute(result);
        }

    }

    class DestroySavedSearchTask extends ManagedAsyncTask<Object, Object, SingleResponse<SavedSearch>> {

        private final UserKey mAccountKey;
        private final long mSearchId;

        DestroySavedSearchTask(final UserKey accountKey, final long searchId) {
            super(context);
            mAccountKey = accountKey;
            mSearchId = searchId;
        }

        @Override
        protected SingleResponse<SavedSearch> doInBackground(final Object... params) {
            final MicroBlog microBlog = MicroBlogAPIFactory.getInstance(context, mAccountKey);
            if (microBlog == null) return SingleResponse.Companion.getInstance();
            try {
                return SingleResponse.Companion.getInstance(microBlog.destroySavedSearch(mSearchId));
            } catch (final MicroBlogException e) {
                return SingleResponse.Companion.getInstance(e);
            }
        }

        @Override
        protected void onPostExecute(final SingleResponse<SavedSearch> result) {
            if (result.hasData()) {
                final String message = context.getString(R.string.message_toast_search_name_deleted, result.getData().getQuery());
                Utils.showOkMessage(context, message, false);
                getBus().post(new SavedSearchDestroyedEvent(mAccountKey, mSearchId));
            } else {
                Utils.showErrorMessage(context, R.string.action_deleting_search, result.getException(), false);
            }
            super.onPostExecute(result);
        }

    }

    class DestroyUserListSubscriptionTask extends ManagedAsyncTask<Object, Object, SingleResponse<ParcelableUserList>> {

        private final UserKey mAccountKey;
        private final String mListId;

        public DestroyUserListSubscriptionTask(@NonNull final UserKey accountKey, final String listId) {
            super(context);
            mAccountKey = accountKey;
            mListId = listId;
        }

        @Override
        protected SingleResponse<ParcelableUserList> doInBackground(final Object... params) {

            final MicroBlog microBlog = MicroBlogAPIFactory.getInstance(context, mAccountKey);
            if (microBlog == null) return SingleResponse.Companion.getInstance();
                try {
                final UserList userList = microBlog.destroyUserListSubscription(mListId);
                final ParcelableUserList list = ParcelableUserListUtils.from(userList, mAccountKey);
                return SingleResponse.Companion.getInstance(list);
            } catch (final MicroBlogException e) {
                return SingleResponse.Companion.getInstance(e);
            }
        }

        @Override
        protected void onPostExecute(final SingleResponse<ParcelableUserList> result) {
            final boolean succeed = result.hasData();
            if (succeed) {
                final String message = context.getString(R.string.unsubscribed_from_list, result.getData().name);
                Utils.showOkMessage(context, message, false);
                getBus().post(new UserListSubscriptionEvent(UserListSubscriptionEvent.Action.UNSUBSCRIBE,
                        result.getData()));
            } else {
                Utils.showErrorMessage(context, R.string.action_unsubscribing_from_list, result.getException(), true);
            }
            super.onPostExecute(result);
        }

    }

    static class DestroyUserListTask extends ManagedAsyncTask<Object, Object, SingleResponse<ParcelableUserList>> {

        private final UserKey mAccountKey;
        private final String mListId;

        public DestroyUserListTask(Context context, final UserKey accountKey, final String listId) {
            super(context);
            mAccountKey = accountKey;
            mListId = listId;
        }

        @Override
        protected SingleResponse<ParcelableUserList> doInBackground(final Object... params) {
            final MicroBlog microBlog = MicroBlogAPIFactory.getInstance(getContext(), mAccountKey
            );
            if (microBlog == null) return SingleResponse.Companion.getInstance();
            try {
                final UserList userList = microBlog.destroyUserList(mListId);
                final ParcelableUserList list = ParcelableUserListUtils.from(userList, mAccountKey);
                return SingleResponse.Companion.getInstance(list);
            } catch (final MicroBlogException e) {
                return SingleResponse.Companion.getInstance(e);
            }
        }

        @Override
        protected void onPostExecute(final SingleResponse<ParcelableUserList> result) {
            final boolean succeed = result.hasData();
            final Context context = getContext();
            if (succeed) {
                final String message = context.getString(R.string.deleted_list, result.getData().name);
                Utils.showInfoMessage(context, message, false);
                getBus().post(new UserListDestroyedEvent(result.getData()));
            } else {
                Utils.showErrorMessage(context, R.string.action_deleting, result.getException(), true);
            }
            super.onPostExecute(result);
        }

    }

    public SharedPreferencesWrapper getPreferences() {
        return preferences;
    }

    final class RemoveUnreadCountsTask extends AsyncTask<Object, Object, Integer> {
        private final int position;
        private final SimpleArrayMap<UserKey, Set<String>> counts;

        RemoveUnreadCountsTask(final int position, final SimpleArrayMap<UserKey, Set<String>> counts) {
            this.position = position;
            this.counts = counts;
        }

        @Override
        protected Integer doInBackground(final Object... params) {
            return removeUnreadCounts(context, position, counts);
        }

    }

    private class RetweetStatusTask extends ManagedAsyncTask<Object, Object, SingleResponse<ParcelableStatus>> {

        private final UserKey mAccountKey;
        private final String mStatusId;

        RetweetStatusTask(@NonNull Context context, @NonNull final UserKey accountKey, final String statusId) {
            super(context);
            this.mAccountKey = accountKey;
            this.mStatusId = statusId;
        }

        @Override
        protected SingleResponse<ParcelableStatus> doInBackground(final Object... params) {
            final ContentResolver resolver = getContext().getContentResolver();
            final AccountDetails details = AccountUtils.getAccountDetails(AccountManager.get(getContext()),
                    mAccountKey, true);
            if (details == null) return SingleResponse.Companion.getInstance();
            final MicroBlog microBlog = AccountDetailsExtensionsKt.newMicroBlogInstance(details,
                    getContext(), MicroBlog.class);
            if (microBlog == null) {
                return SingleResponse.Companion.getInstance();
            }
            try {
                final ParcelableStatus result = ParcelableStatusUtils.INSTANCE.fromStatus(microBlog.retweetStatus(mStatusId),
                        mAccountKey, false);
                ParcelableStatusUtils.INSTANCE.updateExtraInformation(result, details
                );
                Utils.setLastSeen(getContext(), result.mentions, System.currentTimeMillis());
                final ContentValues values = new ContentValues();
                values.put(Statuses.MY_RETWEET_ID, result.id);
                values.put(Statuses.REPLY_COUNT, result.reply_count);
                values.put(Statuses.RETWEET_COUNT, result.retweet_count);
                values.put(Statuses.FAVORITE_COUNT, result.favorite_count);
                final Expression where = Expression.or(
                        Expression.equalsArgs(Statuses.STATUS_ID),
                        Expression.equalsArgs(Statuses.RETWEET_ID)
                );
                final String[] whereArgs = {mStatusId, mStatusId};
                for (final Uri uri : DataStoreUtils.STATUSES_URIS) {
                    resolver.update(uri, values, where.getSQL(), whereArgs);
                }
                DataStoreUtils.updateActivityStatus(resolver, mAccountKey, mStatusId, new DataStoreUtils.UpdateActivityAction() {
                    @Override
                    public void process(ParcelableActivity activity) {
                        ParcelableStatus[][] statusesMatrix = {activity.target_statuses,
                                activity.target_object_statuses};
                        activity.status_my_retweet_id = result.my_retweet_id;
                        for (ParcelableStatus[] statusesArray : statusesMatrix) {
                            if (statusesArray == null) continue;
                            for (ParcelableStatus status : statusesArray) {
                                if (mStatusId.equals(status.id) || mStatusId.equals(status.retweet_id)
                                        || mStatusId.equals(status.my_retweet_id)) {
                                    status.my_retweet_id = result.id;
                                    status.reply_count = result.reply_count;
                                    status.retweet_count = result.retweet_count;
                                    status.favorite_count = result.favorite_count;
                                }
                            }
                        }
                    }
                });
                return SingleResponse.Companion.getInstance(result);
            } catch (final MicroBlogException e) {
                return SingleResponse.Companion.getInstance(e);
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            final int hashCode = calculateHashCode(mAccountKey, mStatusId);
            if (!mCreatingRetweetIds.contains(hashCode)) {
                mCreatingRetweetIds.add(hashCode);
            }
            getBus().post(new StatusListChangedEvent());
        }

        @Override
        protected void onPostExecute(final SingleResponse<ParcelableStatus> result) {
            mCreatingRetweetIds.removeElement(calculateHashCode(mAccountKey, mStatusId));
            if (result.hasData()) {
                final ParcelableStatus status = result.getData();
                getBus().post(new StatusRetweetedEvent(status));
            } else {
                Utils.showErrorMessage(getContext(), R.string.action_retweeting, result.getException(), true);
            }
            super.onPostExecute(result);
        }

    }


    static class UpdateUserListDetailsTask extends ManagedAsyncTask<Object, Object, SingleResponse<ParcelableUserList>> {

        private final UserKey mAccountKey;
        private final String listId;
        private final UserListUpdate update;
        private Context mContext;

        public UpdateUserListDetailsTask(Context context, final UserKey accountKey,
                                         final String listId, UserListUpdate update) {
            super(context);
            this.mAccountKey = accountKey;
            this.listId = listId;
            this.update = update;
            this.mContext = context;
        }

        @Override
        protected SingleResponse<ParcelableUserList> doInBackground(final Object... params) {

            final MicroBlog microBlog = MicroBlogAPIFactory.getInstance(mContext, mAccountKey);
            if (microBlog != null) {
                try {
                    final UserList list = microBlog.updateUserList(listId, update);
                    return SingleResponse.Companion.getInstance(ParcelableUserListUtils.from(list, mAccountKey));
                } catch (final MicroBlogException e) {
                    return SingleResponse.Companion.getInstance(e);
                }
            }
            return SingleResponse.Companion.getInstance();
        }

        @Override
        protected void onPostExecute(final SingleResponse<ParcelableUserList> result) {
            if (result.hasData()) {
                final String message = mContext.getString(R.string.updated_list_details, result.getData().name);
                Utils.showOkMessage(mContext, message, false);
                getBus().post(new UserListUpdatedEvent(result.getData()));
            } else {
                Utils.showErrorMessage(mContext, R.string.action_updating_details, result.getException(), true);
            }
            super.onPostExecute(result);
        }

    }

    interface GetAccountKeysClosure {
        UserKey[] getAccountKeys();
    }
}