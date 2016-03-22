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

package de.vanita5.twittnuker.fragment;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.Loader;

import com.squareup.otto.Subscribe;

import org.mariotaku.sqliteqb.library.ArgsArray;
import org.mariotaku.sqliteqb.library.Columns.Column;
import org.mariotaku.sqliteqb.library.Expression;

import de.vanita5.twittnuker.R;
import de.vanita5.twittnuker.activity.HomeActivity;
import de.vanita5.twittnuker.adapter.ListParcelableStatusesAdapter;
import de.vanita5.twittnuker.adapter.ParcelableStatusesAdapter;
import de.vanita5.twittnuker.adapter.iface.ILoadMoreSupportAdapter.IndicatorPosition;
import de.vanita5.twittnuker.loader.ExtendedObjectCursorLoader;
import de.vanita5.twittnuker.model.ParcelableStatus;
import de.vanita5.twittnuker.model.ParcelableStatusCursorIndices;
import de.vanita5.twittnuker.model.SimpleRefreshTaskParam;
import de.vanita5.twittnuker.model.UserKey;
import de.vanita5.twittnuker.model.message.AccountChangedEvent;
import de.vanita5.twittnuker.model.message.FavoriteTaskEvent;
import de.vanita5.twittnuker.model.message.GetStatusesTaskEvent;
import de.vanita5.twittnuker.model.message.StatusDestroyedEvent;
import de.vanita5.twittnuker.model.message.StatusListChangedEvent;
import de.vanita5.twittnuker.model.message.StatusRetweetedEvent;
import de.vanita5.twittnuker.provider.TwidereDataStore.Accounts;
import de.vanita5.twittnuker.provider.TwidereDataStore.Filters;
import de.vanita5.twittnuker.provider.TwidereDataStore.Statuses;
import de.vanita5.twittnuker.util.DataStoreUtils;
import de.vanita5.twittnuker.util.ErrorInfoStore;
import de.vanita5.twittnuker.util.TwidereArrayUtils;
import de.vanita5.twittnuker.util.Utils;

import java.util.List;

import static de.vanita5.twittnuker.util.DataStoreUtils.buildStatusFilterWhereClause;
import static de.vanita5.twittnuker.util.DataStoreUtils.getTableNameByUri;

public abstract class CursorStatusesFragment extends AbsStatusesFragment {

    @Override
    protected void onLoadingFinished() {
        final UserKey[] accountKeys = getAccountKeys();
        final ParcelableStatusesAdapter adapter = getAdapter();
        if (adapter.getItemCount() > 0) {
            showContent();
        } else if (accountKeys.length > 0) {
            final ErrorInfoStore.DisplayErrorInfo errorInfo = ErrorInfoStore.getErrorInfo(getContext(),
                    mErrorInfoStore.get(getErrorInfoKey(), accountKeys[0]));
            if (errorInfo != null) {
                showEmpty(errorInfo.getIcon(), errorInfo.getMessage());
            } else {
                showEmpty(R.drawable.ic_info_refresh, getString(R.string.swipe_down_to_refresh));
            }
        } else {
            showError(R.drawable.ic_info_accounts, getString(R.string.no_account_selected));
        }
    }

    private ContentObserver mContentObserver;

    @NonNull
    protected abstract String getErrorInfoKey();

    public abstract Uri getContentUri();

    @Override
    protected Loader<List<ParcelableStatus>> onCreateStatusesLoader(final Context context,
                                                                    final Bundle args,
                                                                    final boolean fromUser) {
        final Uri uri = getContentUri();
        final String table = getTableNameByUri(uri);
        final String sortOrder = Statuses.DEFAULT_SORT_ORDER;
        final UserKey[] accountKeys = getAccountKeys();
        final Expression accountWhere = Expression.in(new Column(Statuses.ACCOUNT_KEY),
                new ArgsArray(accountKeys.length));
        final Expression filterWhere = getFiltersWhere(table), where;
        if (filterWhere != null) {
            where = Expression.and(accountWhere, filterWhere);
        } else {
            where = accountWhere;
        }
        final String selection = processWhere(where).getSQL();
        final ParcelableStatusesAdapter adapter = getAdapter();
        adapter.setShowAccountsColor(accountKeys.length > 1);
        final String[] projection = Statuses.COLUMNS;
        final String[] selectionArgs = TwidereArrayUtils.toStringArray(accountKeys, 0,
                accountKeys.length);
        return new ExtendedObjectCursorLoader<>(context, ParcelableStatusCursorIndices.class, uri,
                projection, selection, selectionArgs, sortOrder, fromUser);
    }

    @Override
    protected Object createMessageBusCallback() {
        return new CursorStatusesBusCallback();
    }


    protected class CursorStatusesBusCallback {

        @Subscribe
        public void notifyGetStatusesTaskChanged(GetStatusesTaskEvent event) {
            if (!event.uri.equals(getContentUri())) return;
            setRefreshing(event.running);
            if (!event.running) {
                setLoadMoreIndicatorPosition(IndicatorPosition.NONE);
                setRefreshEnabled(true);
                onLoadingFinished();
            }
        }

        @Subscribe
        public void notifyFavoriteTask(FavoriteTaskEvent event) {
            if (event.isSucceeded()) {
                final ParcelableStatus status = event.getStatus();
                final List<ParcelableStatus> data = getAdapterData();
                if (status == null || data == null || data.isEmpty()) return;
                final ParcelableStatusesAdapter adapter = getAdapter();
                final int firstVisiblePosition = getLayoutManager().findFirstVisibleItemPosition();
                final int lastVisiblePosition = getLayoutManager().findLastVisibleItemPosition();
                final int startIndex = adapter.getStatusStartIndex();
                for (int i = firstVisiblePosition, j = lastVisiblePosition + 1; i < j; i++) {
                    if (status.account_key.equals(adapter.getAccountKey(i)) &&
                            status.id.equals(adapter.getStatusId(i))) {
                        data.set(i - startIndex, status);
                        return;
                    }
                }
            }
        }


        @Subscribe
        public void notifyStatusDestroyed(StatusDestroyedEvent event) {
        }

        @Subscribe
        public void notifyStatusListChanged(StatusListChangedEvent event) {
            getAdapter().notifyDataSetChanged();
        }

        @Subscribe
        public void notifyStatusRetweeted(StatusRetweetedEvent event) {
        }

        @Subscribe
        public void notifyAccountChanged(AccountChangedEvent event) {

        }

    }

    @Override
    protected UserKey[] getAccountKeys() {
        final Bundle args = getArguments();
        final UserKey[] accountKeys = Utils.getAccountKeys(getContext(), args);
        if (accountKeys != null) {
            return accountKeys;
        }
        final FragmentActivity activity = getActivity();
        if (activity instanceof HomeActivity) {
            return ((HomeActivity) activity).getActivatedAccountKeys();
        }
        return DataStoreUtils.getActivatedAccountKeys(getActivity());
    }

    @Override
    public void onStart() {
        super.onStart();
        final ContentResolver cr = getContentResolver();
        mContentObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                reloadStatuses();
            }
        };
        cr.registerContentObserver(Accounts.CONTENT_URI, true, mContentObserver);
        cr.registerContentObserver(Filters.CONTENT_URI, true, mContentObserver);
        updateRefreshState();
        reloadStatuses();
    }

    protected void reloadStatuses() {
        if (getContext() == null || isDetached()) return;
        final Bundle args = new Bundle(), fragmentArgs = getArguments();
        if (fragmentArgs != null) {
            args.putAll(fragmentArgs);
            args.putBoolean(EXTRA_FROM_USER, true);
        }
        getLoaderManager().restartLoader(0, args, this);
    }

    @Override
    public void onStop() {
        final ContentResolver cr = getContentResolver();
        cr.unregisterContentObserver(mContentObserver);
        super.onStop();
    }

    @Override
    protected boolean hasMoreData(final List<ParcelableStatus> cursor) {
        return cursor != null && cursor.size() != 0;
    }

    @NonNull
    @Override
    protected ListParcelableStatusesAdapter onCreateAdapter(final Context context, final boolean compact) {
        return new ListParcelableStatusesAdapter(context, compact);
    }

    @Override
    public void onLoaderReset(Loader<List<ParcelableStatus>> loader) {
        getAdapter().setData(null);
    }

    @Override
    public void onLoadMoreContents(@IndicatorPosition int position) {
        // Only supports load from end, skip START flag
        if ((position & IndicatorPosition.START) != 0) return;
        super.onLoadMoreContents(position);
        if (position == 0) return;
        getStatuses(new SimpleRefreshTaskParam() {
            @NonNull
            @Override
            public UserKey[] getAccountKeysWorker() {
                return CursorStatusesFragment.this.getAccountKeys();
            }

            @Nullable
            @Override
            public String[] getMaxIds() {
                return getOldestStatusIds(getAccountKeys());
            }

            @Nullable
            @Override
            public long[] getMaxSortIds() {
                return DataStoreUtils.getOldestStatusSortIds(getContext(), getContentUri(),
                        getAccountKeys());
            }

            @Override
            public boolean hasMaxIds() {
                return true;
            }

            @Override
            public boolean shouldAbort() {
                return getContext() == null;
            }
        });
    }

    @Override
    public boolean triggerRefresh() {
        super.triggerRefresh();
        getStatuses(new SimpleRefreshTaskParam() {
            @NonNull
            @Override
            public UserKey[] getAccountKeysWorker() {
                return CursorStatusesFragment.this.getAccountKeys();
            }

            @Override
            public boolean hasMaxIds() {
                return false;
            }

            @Nullable
            @Override
            public String[] getSinceIds() {
                return getNewestStatusIds(getAccountKeys());
            }

            @Nullable
            @Override
            public long[] getSinceSortIds() {
                return DataStoreUtils.getNewestStatusSortIds(getContext(), getContentUri(),
                        getAccountKeys());
            }

            @Override
            public boolean shouldAbort() {
                return getContext() == null;
            }
        });
        return true;
    }

    protected Expression getFiltersWhere(String table) {
        if (!isFilterEnabled()) return null;
        return buildStatusFilterWhereClause(table, null);
    }

    protected String[] getNewestStatusIds(UserKey[] accountKeys) {
        final Context context = getContext();
        if (context == null) return null;
        return DataStoreUtils.getNewestStatusIds(context, getContentUri(), accountKeys);
    }

    protected abstract int getNotificationType();

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            for (UserKey accountId : getAccountKeys()) {
                mTwitterWrapper.clearNotificationAsync(getNotificationType(), accountId);
            }
        }
    }

    protected String[] getOldestStatusIds(UserKey[] accountKeys) {
        final Context context = getContext();
        if (context == null) return null;
        return DataStoreUtils.getOldestStatusIds(context, getContentUri(), accountKeys);
    }

    protected abstract boolean isFilterEnabled();

    protected Expression processWhere(final Expression where) {
        return where;
    }

    protected abstract void updateRefreshState();

}