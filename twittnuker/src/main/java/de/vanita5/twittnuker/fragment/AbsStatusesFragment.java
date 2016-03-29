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

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.OnScrollListener;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.squareup.otto.Subscribe;

import de.vanita5.twittnuker.R;
import de.vanita5.twittnuker.adapter.ParcelableStatusesAdapter;
import de.vanita5.twittnuker.adapter.iface.ILoadMoreSupportAdapter.IndicatorPosition;
import de.vanita5.twittnuker.annotation.ReadPositionTag;
import de.vanita5.twittnuker.graphic.like.LikeAnimationDrawable;
import de.vanita5.twittnuker.loader.iface.IExtendedLoader;
import de.vanita5.twittnuker.model.BaseRefreshTaskParam;
import de.vanita5.twittnuker.model.ParcelableMedia;
import de.vanita5.twittnuker.model.ParcelableStatus;
import de.vanita5.twittnuker.model.RefreshTaskParam;
import de.vanita5.twittnuker.model.UserKey;
import de.vanita5.twittnuker.model.message.StatusListChangedEvent;
import de.vanita5.twittnuker.util.AsyncTwitterWrapper;
import de.vanita5.twittnuker.util.IntentUtils;
import de.vanita5.twittnuker.util.KeyboardShortcutsHandler;
import de.vanita5.twittnuker.util.KeyboardShortcutsHandler.KeyboardShortcutCallback;
import de.vanita5.twittnuker.util.LinkCreator;
import de.vanita5.twittnuker.util.MenuUtils;
import de.vanita5.twittnuker.util.RecyclerViewNavigationHelper;
import de.vanita5.twittnuker.util.RecyclerViewUtils;
import de.vanita5.twittnuker.util.Utils;
import de.vanita5.twittnuker.util.imageloader.PauseRecyclerViewOnScrollListener;
import de.vanita5.twittnuker.view.ExtendedRecyclerView;
import de.vanita5.twittnuker.view.holder.GapViewHolder;
import de.vanita5.twittnuker.view.holder.StatusViewHolder;
import de.vanita5.twittnuker.view.holder.iface.IStatusViewHolder;

import java.util.List;

public abstract class AbsStatusesFragment extends AbsContentListRecyclerViewFragment<ParcelableStatusesAdapter>
        implements LoaderCallbacks<List<ParcelableStatus>>, IStatusViewHolder.StatusClickListener, KeyboardShortcutCallback {

    private final Object mStatusesBusCallback;
    private final OnScrollListener mOnScrollListener = new OnScrollListener() {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                final LinearLayoutManager layoutManager = getLayoutManager();
                saveReadPosition(layoutManager.findFirstVisibleItemPosition());
            }
        }
    };
    private RecyclerViewNavigationHelper mNavigationHelper;
    private OnScrollListener mPauseOnScrollListener;

    protected AbsStatusesFragment() {
        mStatusesBusCallback = createMessageBusCallback();
    }

    public abstract boolean getStatuses(RefreshTaskParam param);

    @Override
    public boolean handleKeyboardShortcutSingle(@NonNull KeyboardShortcutsHandler handler, int keyCode, @NonNull KeyEvent event, int metaState) {
        String action = handler.getKeyAction(CONTEXT_TAG_NAVIGATION, keyCode, event, metaState);
        if (ACTION_NAVIGATION_REFRESH.equals(action)) {
            triggerRefresh();
            return true;
        }
        final RecyclerView recyclerView = getRecyclerView();
        final LinearLayoutManager layoutManager = getLayoutManager();
        if (recyclerView == null || layoutManager == null) return false;
        final View focusedChild = RecyclerViewUtils.findRecyclerViewChild(recyclerView,
                layoutManager.getFocusedChild());
        int position = -1;
        if (focusedChild != null && focusedChild.getParent() == recyclerView) {
            position = recyclerView.getChildLayoutPosition(focusedChild);
        }
        if (position != -1) {
            final ParcelableStatus status = getAdapter().getStatus(position);
            if (status == null) return false;
            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                IntentUtils.openStatus(getActivity(), status, null);
                return true;
            }
            if (action == null) {
                action = handler.getKeyAction(CONTEXT_TAG_STATUS, keyCode, event, metaState);
            }
            if (action == null) return false;
            switch (action) {
                case ACTION_STATUS_REPLY: {
                    final Intent intent = new Intent(INTENT_ACTION_REPLY);
                    intent.putExtra(EXTRA_STATUS, status);
                    startActivity(intent);
                    return true;
                }
                case ACTION_STATUS_RETWEET: {
                    RetweetQuoteDialogFragment.show(getFragmentManager(), status);
                    return true;
                }
                case ACTION_STATUS_FAVORITE: {
                    final AsyncTwitterWrapper twitter = mTwitterWrapper;
                    if (status.is_favorite) {
                        twitter.destroyFavoriteAsync(status.account_key, status.id);
                    } else {
                        final IStatusViewHolder holder = (IStatusViewHolder)
                                recyclerView.findViewHolderForLayoutPosition(position);
                        holder.playLikeAnimation(new DefaultOnLikedListener(twitter, status));
                    }
                    return true;
                }
            }
        }
        return mNavigationHelper.handleKeyboardShortcutSingle(handler, keyCode, event, metaState);
    }

    @Override
    public boolean isKeyboardShortcutHandled(@NonNull KeyboardShortcutsHandler handler, int keyCode, @NonNull KeyEvent event, int metaState) {
        String action = handler.getKeyAction(CONTEXT_TAG_NAVIGATION, keyCode, event, metaState);
        if (ACTION_NAVIGATION_REFRESH.equals(action)) {
            return true;
        }
        if (action == null) {
            action = handler.getKeyAction(CONTEXT_TAG_STATUS, keyCode, event, metaState);
        }
        if (action == null) return false;
        switch (action) {
            case ACTION_STATUS_REPLY:
            case ACTION_STATUS_RETWEET:
            case ACTION_STATUS_FAVORITE:
                return true;
        }
        return mNavigationHelper.isKeyboardShortcutHandled(handler, keyCode, event, metaState);
    }

    @Override
    public boolean handleKeyboardShortcutRepeat(@NonNull KeyboardShortcutsHandler handler, final int keyCode, final int repeatCount,
                                                @NonNull final KeyEvent event, int metaState) {
        return mNavigationHelper.handleKeyboardShortcutRepeat(handler, keyCode, repeatCount, event, metaState);
    }

    @Override
    public final Loader<List<ParcelableStatus>> onCreateLoader(int id, Bundle args) {
        final boolean fromUser = args.getBoolean(EXTRA_FROM_USER);
        args.remove(EXTRA_FROM_USER);
        return onCreateStatusesLoader(getActivity(), args, fromUser);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (isVisibleToUser) {
            saveReadPosition();
        }
    }

    @Override
    public final void onLoadFinished(Loader<List<ParcelableStatus>> loader, List<ParcelableStatus> data) {
        final ParcelableStatusesAdapter adapter = getAdapter();
        final boolean rememberPosition = mPreferences.getBoolean(KEY_REMEMBER_POSITION, false);
        final boolean readFromBottom = mPreferences.getBoolean(KEY_READ_FROM_BOTTOM, true);
        long lastReadPositionKey;
        final int lastVisiblePos, lastVisibleTop;
        final String tag = getCurrentReadPositionTag();
        final LinearLayoutManager layoutManager = getLayoutManager();
        if (readFromBottom) {
            lastVisiblePos = layoutManager.findLastVisibleItemPosition();
        } else {
            lastVisiblePos = layoutManager.findFirstVisibleItemPosition();
        }
        if (lastVisiblePos != RecyclerView.NO_POSITION && lastVisiblePos < adapter.getItemCount()) {
            final int statusStartIndex = adapter.getStatusStartIndex();
            final int statusEndIndex = statusStartIndex + adapter.getStatusCount();
            final int lastItemIndex = Math.min(statusEndIndex, lastVisiblePos);
            lastReadPositionKey = adapter.getStatusPositionKey(lastItemIndex);
            final View positionView = layoutManager.findViewByPosition(lastItemIndex);
            lastVisibleTop = positionView != null ? positionView.getTop() : 0;
        } else if (rememberPosition && tag != null) {
            lastReadPositionKey = mReadStateManager.getPosition(tag);
            lastVisibleTop = 0;
        } else {
            lastReadPositionKey = -1;
            lastVisibleTop = 0;
        }
        adapter.setData(data);
        final int statusStartIndex = adapter.getStatusStartIndex();
        // The last status is statusEndExclusiveIndex - 1
        final int statusEndExclusiveIndex = statusStartIndex + adapter.getStatusCount();
        if (statusEndExclusiveIndex >= 0 && rememberPosition && tag != null) {
            final long lastPositionKey = adapter.getStatusPositionKey(statusEndExclusiveIndex - 1);
            // Status corresponds to last read id was deleted, use last item id instead
            if (lastPositionKey != -1 && lastReadPositionKey > 0 && lastReadPositionKey < lastPositionKey) {
                lastReadPositionKey = lastPositionKey;
            }
        }
        setRefreshEnabled(true);
        if (!(loader instanceof IExtendedLoader) || ((IExtendedLoader) loader).isFromUser()) {
            if (hasMoreData(data)) {
                adapter.setLoadMoreSupportedPosition(IndicatorPosition.END);
                onHasMoreDataChanged(true);
            } else {
                adapter.setLoadMoreSupportedPosition(IndicatorPosition.NONE);
                onHasMoreDataChanged(false);
            }
            int pos = -1;
            for (int i = statusStartIndex; i < statusEndExclusiveIndex; i++) {
                // Assume statuses are descend sorted by id, so break at first status with id
                // lesser equals than read position
                if (lastReadPositionKey != -1 && adapter.getStatusPositionKey(i) <= lastReadPositionKey) {
                    pos = i;
                    break;
                }
            }
            if (pos != -1 && adapter.isStatus(pos) && (readFromBottom || lastVisiblePos != 0)) {
                if (layoutManager.getHeight() == 0) {
                    // RecyclerView has not currently laid out, ignore padding.
                    layoutManager.scrollToPositionWithOffset(pos, lastVisibleTop);
                } else {
                    layoutManager.scrollToPositionWithOffset(pos, lastVisibleTop - layoutManager.getPaddingTop());
                }
            }
        } else {
            onHasMoreDataChanged(false);
        }
        if (loader instanceof IExtendedLoader) {
            ((IExtendedLoader) loader).setFromUser(false);
        }
        onLoadingFinished();
    }

    @Override
    public void onLoaderReset(Loader<List<ParcelableStatus>> loader) {
        if (loader instanceof IExtendedLoader) {
            ((IExtendedLoader) loader).setFromUser(false);
        }
    }

    @Override
    public void onGapClick(GapViewHolder holder, int position) {
        final ParcelableStatusesAdapter adapter = getAdapter();
        final ParcelableStatus status = adapter.getStatus(position);
        if (status == null) return;
        final UserKey[] accountIds = {status.account_key};
        final String[] maxIds = {status.id};
        final long[] maxSortIds = {status.sort_id};
        getStatuses(new BaseRefreshTaskParam(accountIds, maxIds, null, maxSortIds, null));
    }

    @Override
    public void onMediaClick(IStatusViewHolder holder, View view, ParcelableMedia media, int statusPosition) {
        final ParcelableStatusesAdapter adapter = getAdapter();
        final ParcelableStatus status = adapter.getStatus(statusPosition);
        if (status == null) return;
        IntentUtils.openMedia(getActivity(), status, media, null, true);
    }

    protected void saveReadPosition() {
        final LinearLayoutManager layoutManager = getLayoutManager();
        if (layoutManager != null) {
            saveReadPosition(layoutManager.findFirstVisibleItemPosition());
        }
    }

    protected void onHasMoreDataChanged(boolean hasMoreData) {
    }

    @Override
    public void onItemActionClick(RecyclerView.ViewHolder holder, int id, int position) {
        final ParcelableStatusesAdapter adapter = getAdapter();
        final ParcelableStatus status = adapter.getStatus(position);
        if (status == null) return;
        final FragmentActivity activity = getActivity();
        switch (id) {
            case R.id.reply: {
                final Intent intent = new Intent(INTENT_ACTION_REPLY);
                intent.setPackage(activity.getPackageName());
                intent.putExtra(EXTRA_STATUS, status);
                activity.startActivity(intent);
                break;
            }
            case R.id.retweet: {
                RetweetQuoteDialogFragment.show(getFragmentManager(), status);
                break;
            }
            case R.id.favorite: {
                final AsyncTwitterWrapper twitter = mTwitterWrapper;
                if (twitter == null) return;
                if (status.is_favorite) {
                    twitter.destroyFavoriteAsync(status.account_key, status.id);
                } else {
                    ((StatusViewHolder) holder).playLikeAnimation(new DefaultOnLikedListener(twitter,
                            status));
                }
                break;
            }
        }
    }

    @Override
    public void onStatusClick(IStatusViewHolder holder, int position) {
        final ParcelableStatusesAdapter adapter = getAdapter();
        IntentUtils.openStatus(getActivity(), adapter.getStatus(position), null);
    }

    @Override
    public boolean onStatusLongClick(IStatusViewHolder holder, int position) {
        //TODO handle long click event
        return true;
    }

    @Override
    public void onItemMenuClick(RecyclerView.ViewHolder holder, View menuView, int position) {
        if (getActivity() == null) return;
        final LinearLayoutManager lm = getLayoutManager();
        final View view = lm.findViewByPosition(position);
        if (view == null) return;
        getRecyclerView().showContextMenuForChild(view);
    }

    @Override
    public void onUserProfileClick(IStatusViewHolder holder, int position) {
        final ParcelableStatus status = getAdapter().getStatus(position);
        final Intent intent = IntentUtils.userProfile(status.account_key, status.user_key,
                status.user_screen_name, UserFragment.Referral.TIMELINE_STATUS,
                status.extras.user_statusnet_profile_url);
        IntentUtils.applyNewDocument(intent, mPreferences.getBoolean(KEY_NEW_DOCUMENT_API));
        startActivity(intent);
    }

    @Override
    public void onStart() {
        super.onStart();
        final RecyclerView recyclerView = getRecyclerView();
        recyclerView.addOnScrollListener(mOnScrollListener);
        recyclerView.addOnScrollListener(mPauseOnScrollListener);
        mBus.register(mStatusesBusCallback);
    }

    @Override
    public void onStop() {
        mBus.unregister(mStatusesBusCallback);
        final RecyclerView recyclerView = getRecyclerView();
        recyclerView.removeOnScrollListener(mPauseOnScrollListener);
        recyclerView.removeOnScrollListener(mOnScrollListener);
        if (getUserVisibleHint()) {
            saveReadPosition();
        }
        super.onStop();
    }

    @Override
    public void onDestroy() {
        final ParcelableStatusesAdapter adapter = getAdapter();
        adapter.setStatusClickListener(null);
        super.onDestroy();
    }

    @Override
    public final boolean scrollToStart() {
        final boolean result = super.scrollToStart();
        if (result) {
            saveReadPosition(0);
        }
        return result;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final ParcelableStatusesAdapter adapter = getAdapter();
        final RecyclerView recyclerView = getRecyclerView();
        final LinearLayoutManager layoutManager = getLayoutManager();
        adapter.setStatusClickListener(this);
        registerForContextMenu(recyclerView);
        mNavigationHelper = new RecyclerViewNavigationHelper(recyclerView, layoutManager,
                adapter, this);
        mPauseOnScrollListener = new PauseRecyclerViewOnScrollListener(adapter.getMediaLoader().getImageLoader(), false, true);

        final Bundle loaderArgs = new Bundle(getArguments());
        loaderArgs.putBoolean(EXTRA_FROM_USER, true);
        getLoaderManager().initLoader(0, loaderArgs, this);
        showProgress();
    }

    protected Object createMessageBusCallback() {
        return new StatusesBusCallback();
    }

    @NonNull
    protected abstract UserKey[] getAccountKeys();

    protected List<ParcelableStatus> getAdapterData() {
        final ParcelableStatusesAdapter adapter = getAdapter();
        return adapter.getData();
    }

    protected void setAdapterData(List<ParcelableStatus> data) {
        final ParcelableStatusesAdapter adapter = getAdapter();
        adapter.setData(data);
    }

    @ReadPositionTag
    @Nullable
    protected String getReadPositionTag() {
        return null;
    }

    @Nullable
    protected String getReadPositionTagWithArguments() {
        return getReadPositionTag();
    }

    protected abstract boolean hasMoreData(List<ParcelableStatus> data);

    protected abstract Loader<List<ParcelableStatus>> onCreateStatusesLoader(final Context context, final Bundle args,
                                                           final boolean fromUser);

    protected abstract void onLoadingFinished();

    protected final void saveReadPosition(int position) {
        final String readPositionTag = getReadPositionTagWithAccounts();
        if (readPositionTag == null) return;
        if (position == RecyclerView.NO_POSITION) return;
        final ParcelableStatusesAdapter adapter = getAdapter();
        final ParcelableStatus status = adapter.getStatus(position);
        if (status == null) return;
        final long positionKey = status.position_key > 0 ? status.position_key : status.timestamp;
        mReadStateManager.setPosition(readPositionTag, positionKey);
        final UserKey[] accountKeys = getAccountKeys();
        if (accountKeys.length > 1) {
            for (UserKey accountKey : accountKeys) {
                final String tag = Utils.getReadPositionTagWithAccounts(getReadPositionTagWithArguments(),
                        accountKey);
                mReadStateManager.setPosition(tag, positionKey);
            }
        }
        mReadStateManager.setPosition(getCurrentReadPositionTag(), positionKey, true);
    }

    @NonNull
    @Override
    protected Rect getExtraContentPadding() {
        final int paddingVertical = getResources().getDimensionPixelSize(R.dimen.element_spacing_small);
        return new Rect(0, paddingVertical, 0, paddingVertical);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        if (!getUserVisibleHint() || menuInfo == null) return;
        final ParcelableStatusesAdapter adapter = getAdapter();
        final MenuInflater inflater = new MenuInflater(getContext());
        final ExtendedRecyclerView.ContextMenuInfo contextMenuInfo =
                (ExtendedRecyclerView.ContextMenuInfo) menuInfo;
        final ParcelableStatus status = adapter.getStatus(contextMenuInfo.getPosition());
        inflater.inflate(R.menu.action_status, menu);
        MenuUtils.setupForStatus(getContext(), mPreferences, menu, status,
                mTwitterWrapper);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (!getUserVisibleHint()) return false;
        final ExtendedRecyclerView.ContextMenuInfo contextMenuInfo =
                (ExtendedRecyclerView.ContextMenuInfo) item.getMenuInfo();
        final ParcelableStatus status = getAdapter().getStatus(contextMenuInfo.getPosition());
        if (status == null) return false;
        if (item.getItemId() == R.id.share) {
            final Intent shareIntent = Utils.createStatusShareIntent(getActivity(), status);
            final Intent chooser = Intent.createChooser(shareIntent, getString(R.string.share_status));
            Utils.addCopyLinkIntent(getContext(), chooser, LinkCreator.getStatusWebLink(status));
            startActivity(chooser);
            return true;
        }
        return MenuUtils.handleStatusClick(getActivity(), AbsStatusesFragment.this,
                getFragmentManager(), mUserColorNameManager, mTwitterWrapper, status, item);
    }

    private String getCurrentReadPositionTag() {
        final String tag = getReadPositionTagWithAccounts();
        if (tag == null) return null;
        return tag + "_current";
    }

    private String getReadPositionTagWithAccounts() {
        return Utils.getReadPositionTagWithAccounts(getReadPositionTagWithArguments(), getAccountKeys());
    }

    public static final class DefaultOnLikedListener implements LikeAnimationDrawable.OnLikedListener {
        private final ParcelableStatus mStatus;
        private final AsyncTwitterWrapper mTwitter;

        public DefaultOnLikedListener(final AsyncTwitterWrapper twitter, final ParcelableStatus status) {
            mStatus = status;
            mTwitter = twitter;
        }

        @Override
        public boolean onLiked() {
            final ParcelableStatus status = mStatus;
            if (status.is_favorite) return false;
            mTwitter.createFavoriteAsync(status.account_key, status.id);
            return true;
        }
    }

    protected final class StatusesBusCallback {

        protected StatusesBusCallback() {
        }

        @Subscribe
        public void notifyStatusListChanged(StatusListChangedEvent event) {
            final ParcelableStatusesAdapter adapter = getAdapter();
            adapter.notifyDataSetChanged();
        }

    }
}