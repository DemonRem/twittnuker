/*
 * Twittnuker - Twitter client for Android
 *
 * Copyright (C) 2013-2015 vanita5 <mail@vanita5.de>
 *
 * This program incorporates a modified version of Twidere.
 * Copyright (C) 2012-2015 Mariotaku Lee <mariotaku.lee@gmail.com>
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

package de.vanita5.twittnuker.fragment.support;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.support.v7.widget.FixedLinearLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.PopupMenu.OnMenuItemClickListener;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.OnScrollListener;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import de.vanita5.twittnuker.R;
import de.vanita5.twittnuker.activity.iface.IControlBarActivity;
import de.vanita5.twittnuker.activity.iface.IControlBarActivity.ControlBarOffsetListener;
import de.vanita5.twittnuker.activity.support.BaseActionBarActivity;
import de.vanita5.twittnuker.adapter.AbsStatusesAdapter;
import de.vanita5.twittnuker.adapter.AbsStatusesAdapter.StatusAdapterListener;
import de.vanita5.twittnuker.adapter.decorator.DividerItemDecoration;
import de.vanita5.twittnuker.app.TwittnukerApplication;
import de.vanita5.twittnuker.fragment.iface.RefreshScrollTopInterface;
import de.vanita5.twittnuker.loader.iface.IExtendedLoader;
import de.vanita5.twittnuker.model.ParcelableMedia;
import de.vanita5.twittnuker.model.ParcelableStatus;
import de.vanita5.twittnuker.util.AsyncTwitterWrapper;
import de.vanita5.twittnuker.util.ColorUtils;
import de.vanita5.twittnuker.util.ContentListScrollListener;
import de.vanita5.twittnuker.util.ContentListScrollListener.ContentListSupport;
import de.vanita5.twittnuker.util.KeyboardShortcutsHandler;
import de.vanita5.twittnuker.util.KeyboardShortcutsHandler.ShortcutCallback;
import de.vanita5.twittnuker.util.ReadStateManager;
import de.vanita5.twittnuker.util.RecyclerViewUtils;
import de.vanita5.twittnuker.util.SimpleDrawerCallback;
import de.vanita5.twittnuker.util.ThemeUtils;
import de.vanita5.twittnuker.util.Utils;
import de.vanita5.twittnuker.util.message.StatusListChangedEvent;
import de.vanita5.twittnuker.view.HeaderDrawerLayout.DrawerCallback;
import de.vanita5.twittnuker.view.holder.GapViewHolder;
import de.vanita5.twittnuker.view.holder.StatusViewHolder;

import static de.vanita5.twittnuker.util.Utils.setMenuForStatus;

public abstract class AbsStatusesFragment<Data> extends BaseSupportFragment implements LoaderCallbacks<Data>,
        OnRefreshListener, DrawerCallback, RefreshScrollTopInterface, StatusAdapterListener,
        ControlBarOffsetListener, ContentListSupport, ShortcutCallback {

    private final Object mStatusesBusCallback;
    private AbsStatusesAdapter<Data> mAdapter;
    private LinearLayoutManager mLayoutManager;
    private SharedPreferences mPreferences;
    private View mProgressContainer;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;
    private SimpleDrawerCallback mDrawerCallback;
    private Rect mSystemWindowsInsets = new Rect();
    private int mControlBarOffsetPixels;
    private PopupMenu mPopupMenu;
    private ReadStateManager mReadStateManager;
    private KeyboardShortcutsHandler mKeyboardShortcutsHandler;
    private ParcelableStatus mSelectedStatus;

    private int mPositionBackup;

    private OnMenuItemClickListener mOnStatusMenuItemClickListener = new OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            final ParcelableStatus status = mSelectedStatus;
            if (status == null) return false;
            if (item.getItemId() == MENU_SHARE) {
                final Intent shareIntent = Utils.createStatusShareIntent(getActivity(), status);
                startActivity(Intent.createChooser(shareIntent, getString(R.string.share_status)));
                return true;
            }
            return Utils.handleMenuItemClick(getActivity(), AbsStatusesFragment.this,
                    getFragmentManager(), getTwitterWrapper(), status, item);
        }
    };

    protected AbsStatusesFragment() {
        mStatusesBusCallback = createMessageBusCallback();
    }

	@Override
	public boolean canScroll(float dy) {
		return mDrawerCallback.canScroll(dy);
	}

	@Override
    public void cancelTouch() {
        mDrawerCallback.cancelTouch();
    }

    @Override
    public void fling(float velocity) {
        mDrawerCallback.fling(velocity);
    }

    @Override
	public boolean isScrollContent(float x, float y) {
		return mDrawerCallback.isScrollContent(x, y);
	}

	@Override
    public boolean handleKeyboardShortcutRepeat(int keyCode, int repeatCount, @NonNull KeyEvent event) {
        if (!KeyboardShortcutsHandler.isValidForHotkey(keyCode, event)) return false;
        String action = mKeyboardShortcutsHandler.getKeyAction("navigation", keyCode, event);
        final LinearLayoutManager layoutManager = mLayoutManager;
        final RecyclerView recyclerView = mRecyclerView;
        final View focusedChild = RecyclerViewUtils.findRecyclerViewChild(recyclerView, layoutManager.getFocusedChild());
        final int position;
        if (focusedChild != null) {
            position = recyclerView.getChildLayoutPosition(focusedChild);
        } else if (layoutManager.findFirstVisibleItemPosition() == 0) {
            position = -1;
        } else {
            final int itemCount = mAdapter.getItemCount();
            if (layoutManager.findLastVisibleItemPosition() == itemCount - 1) {
                position = itemCount;
            } else {
                position = mPositionBackup;
            }
        }
        mPositionBackup = position;
        if (action != null) {
            switch (action) {
                case "navigation.previous": {
                    RecyclerViewUtils.focusNavigate(recyclerView, layoutManager, position, -1);
                    return true;
                }
                case "navigation.next": {
                    RecyclerViewUtils.focusNavigate(recyclerView, layoutManager, position, 1);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean handleKeyboardShortcutSingle(int keyCode, @NonNull KeyEvent event) {
        if (!KeyboardShortcutsHandler.isValidForHotkey(keyCode, event)) return false;
        String action = mKeyboardShortcutsHandler.getKeyAction("navigation", keyCode, event);
        if ("navigation.refresh".equals(action)) {
            triggerRefresh();
            return true;
        }
        final View focusedChild = RecyclerViewUtils.findRecyclerViewChild(mRecyclerView, mLayoutManager.getFocusedChild());
        final int position;
        if (focusedChild != null && focusedChild.getParent() == mRecyclerView) {
            position = mRecyclerView.getChildLayoutPosition(focusedChild);
        } else {
            return false;
        }
        if (position == -1) return false;
        final ParcelableStatus status = mAdapter.getStatus(position);
        if (status == null) return false;
        if (action == null) {
            action = mKeyboardShortcutsHandler.getKeyAction("status", keyCode, event);
        }
        if (action == null) return false;
        switch (action) {
            case "status.reply": {
                final Intent intent = new Intent(INTENT_ACTION_REPLY);
                intent.putExtra(EXTRA_STATUS, status);
                startActivity(intent);
                return true;
            }
            case "status.retweet": {
                RetweetQuoteDialogFragment.show(getFragmentManager(), status);
                return true;
            }
            case "status.favorite": {
                final AsyncTwitterWrapper twitter = getTwitterWrapper();
                if (status.is_favorite) {
                    twitter.destroyFavoriteAsync(status);
                } else {
                    twitter.createFavoriteAsync(status);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public void scrollBy(float dy) {
        mDrawerCallback.scrollBy(dy);
	}

	@Override
    public boolean shouldLayoutHeaderBottom() {
        return mDrawerCallback.shouldLayoutHeaderBottom();
    }

    @Override
	public void topChanged(int offset) {
		mDrawerCallback.topChanged(offset);
	}

    public SharedPreferences getSharedPreferences() {
        if (mPreferences != null) return mPreferences;
        return mPreferences = getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    public abstract int getStatuses(long[] accountIds, long[] maxIds, long[] sinceIds);

    public abstract boolean isRefreshing();

    public AbsStatusesAdapter<Data> getAdapter() {
        return mAdapter;
    }

    public void setControlVisible(boolean visible) {
        final FragmentActivity activity = getActivity();
        if (activity instanceof BaseActionBarActivity) {
            ((BaseActionBarActivity) activity).setControlBarVisibleAnimate(visible);
        }
    }

    public void setRefreshing(boolean refreshing) {
        if (refreshing == mSwipeRefreshLayout.isRefreshing()) return;
//        if (!refreshing) updateRefreshProgressOffset();
        mSwipeRefreshLayout.setRefreshing(refreshing && !mAdapter.isLoadMoreIndicatorVisible());
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof IControlBarActivity) {
            ((IControlBarActivity) activity).registerControlBarOffsetListener(this);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_recycler_view, container, false);
    }

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
        mReadStateManager = getReadStateManager();
        final FragmentActivity activity = getActivity();
        final TwittnukerApplication application = TwittnukerApplication.getInstance(activity);
        mKeyboardShortcutsHandler = application.getKeyboardShortcutsHandler();
		final View view = getView();
		if (view == null) throw new AssertionError();
		final Context context = view.getContext();
		final boolean compact = Utils.isCompactCards(context);
		mDrawerCallback = new SimpleDrawerCallback(mRecyclerView);
		mSwipeRefreshLayout.setOnRefreshListener(this);
		mSwipeRefreshLayout.setColorSchemeColors(ThemeUtils.getUserAccentColor(context));
        final int backgroundColor = ThemeUtils.getThemeBackgroundColor(context);
        final int colorRes = ColorUtils.getContrastYIQ(backgroundColor,
				R.color.bg_refresh_progress_color_light, R.color.bg_refresh_progress_color_dark);
        mSwipeRefreshLayout.setProgressBackgroundColorSchemeResource(colorRes);
		mAdapter = onCreateAdapter(context, compact);
        mLayoutManager = new FixedLinearLayoutManager(context);
        mLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setHasFixedSize(true);
		if (compact) {
            mRecyclerView.addItemDecoration(new DividerItemDecoration(context, mLayoutManager.getOrientation()));
		}
		mRecyclerView.setAdapter(mAdapter);

        final ContentListScrollListener scrollListener = new ContentListScrollListener(this);
        scrollListener.setTouchSlop(ViewConfiguration.get(context).getScaledTouchSlop());
        scrollListener.setOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    saveReadPosition();
                }
            }
        });
        mRecyclerView.setOnScrollListener(scrollListener);
        mAdapter.setListener(this);
        final Bundle loaderArgs = new Bundle(getArguments());
        loaderArgs.putBoolean(EXTRA_FROM_USER, true);
        getLoaderManager().initLoader(0, loaderArgs, this);
		setListShown(false);
	}

    @Override
    public void onLoadMoreContents() {
        setLoadMoreIndicatorVisible(true);
        setRefreshEnabled(false);
    }

    public void setLoadMoreIndicatorVisible(boolean visible) {
        mAdapter.setLoadMoreIndicatorVisible(visible);
    }

    @Override
    public void onStart() {
        super.onStart();
        final Bus bus = TwittnukerApplication.getInstance(getActivity()).getMessageBus();
        bus.register(mStatusesBusCallback);
    }

    @Override
    public void onStop() {
        final Bus bus = TwittnukerApplication.getInstance(getActivity()).getMessageBus();
        bus.unregister(mStatusesBusCallback);
        super.onStop();
	}

    @Override
    public void onDestroyView() {
        if (mPopupMenu != null) {
            mPopupMenu.dismiss();
        }
        super.onDestroyView();
    }

    @Override
    public void onBaseViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onBaseViewCreated(view, savedInstanceState);
        mProgressContainer = view.findViewById(R.id.progress_container);
        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_layout);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
    }

    @Override
    public void onDetach() {
        final FragmentActivity activity = getActivity();
        if (activity instanceof IControlBarActivity) {
            ((IControlBarActivity) activity).unregisterControlBarOffsetListener(this);
        }
        super.onDetach();
    }

    @Override
    protected void fitSystemWindows(Rect insets) {
        super.fitSystemWindows(insets);
        mRecyclerView.setPadding(insets.left, insets.top, insets.right, insets.bottom);
        mProgressContainer.setPadding(insets.left, insets.top, insets.right, insets.bottom);
        mSystemWindowsInsets.set(insets);
        updateRefreshProgressOffset();
    }

    @Override
    public void onControlBarOffsetChanged(IControlBarActivity activity, float offset) {
        mControlBarOffsetPixels = Math.round(activity.getControlBarHeight() * (1 - offset));
        updateRefreshProgressOffset();
    }

    @Override
    public final Loader<Data> onCreateLoader(int id, Bundle args) {
        final boolean fromUser = args.getBoolean(EXTRA_FROM_USER);
        args.remove(EXTRA_FROM_USER);
        return onCreateStatusesLoader(getActivity(), args, fromUser);
    }

    @Override
    public final void onLoadFinished(Loader<Data> loader, Data data) {
        final SharedPreferences preferences = getSharedPreferences();
        final boolean rememberPosition = preferences.getBoolean(KEY_REMEMBER_POSITION, false);
        final boolean readFromBottom = preferences.getBoolean(KEY_READ_FROM_BOTTOM, false);
        final long lastReadId;
        final int lastVisiblePos, lastVisibleTop;
        final String tag = getCurrentReadPositionTag();
        if (readFromBottom) {
            lastVisiblePos = mLayoutManager.findLastVisibleItemPosition();
        } else {
            lastVisiblePos = mLayoutManager.findFirstVisibleItemPosition();
        }
        if (lastVisiblePos != RecyclerView.NO_POSITION) {
            lastReadId = mAdapter.getStatusId(lastVisiblePos);
            final View positionView = mLayoutManager.findViewByPosition(lastVisiblePos);
            lastVisibleTop = positionView != null ? positionView.getTop() : 0;
        } else if (rememberPosition && tag != null) {
            lastReadId = mReadStateManager.getPosition(tag);
            lastVisibleTop = 0;
        } else {
            lastReadId = -1;
            lastVisibleTop = 0;
        }
        mAdapter.setData(data);
        if (!(loader instanceof IExtendedLoader) || ((IExtendedLoader) loader).isFromUser()) {
            mAdapter.setLoadMoreSupported(hasMoreData(data));
            setRefreshEnabled(true);
            int pos = -1;
            for (int i = 0, j = mAdapter.getItemCount(); i < j; i++) {
                if (lastReadId != -1 && lastReadId == mAdapter.getStatusId(i)) {
                    pos = i;
                    break;
                }
            }
            if (pos != -1 && mAdapter.isStatus(pos) && (readFromBottom || lastVisiblePos != 0)) {
                mLayoutManager.scrollToPositionWithOffset(pos, lastVisibleTop);
            }
        }
        if (loader instanceof IExtendedLoader) {
            ((IExtendedLoader) loader).setFromUser(false);
        }
        setListShown(true);
        onLoadingFinished();
    }

    protected abstract void onLoadingFinished();

    public void setRefreshEnabled(boolean enabled) {
        mSwipeRefreshLayout.setEnabled(enabled);
    }

    @Override
    public void onLoaderReset(Loader<Data> loader) {
        if (loader instanceof IExtendedLoader) {
            ((IExtendedLoader) loader).setFromUser(false);
        }
    }

    public abstract Loader<Data> onCreateStatusesLoader(final Context context, final Bundle args,
                                                        final boolean fromUser);

    @Override
    public void onGapClick(GapViewHolder holder, int position) {
        final ParcelableStatus status = mAdapter.getStatus(position);
        final long sinceId = position + 1 < mAdapter.getStatusesCount() ? mAdapter.getStatus(position + 1).id : -1;
        final long[] accountIds = {status.account_id};
        final long[] maxIds = {status.id};
        final long[] sinceIds = {sinceId};
        getStatuses(accountIds, maxIds, sinceIds);
    }

	@Override
    public void onStatusActionClick(StatusViewHolder holder, int id, int position) {
        final ParcelableStatus status = mAdapter.getStatus(position);
        if (status == null) return;
        final FragmentActivity activity = getActivity();
        switch (id) {
            case R.id.reply_count: {
                final Intent intent = new Intent(INTENT_ACTION_REPLY);
                intent.setPackage(activity.getPackageName());
                intent.putExtra(EXTRA_STATUS, status);
                activity.startActivity(intent);
                break;
            }
            case R.id.retweet_count: {
                RetweetQuoteDialogFragment.show(getFragmentManager(), status);
                break;
            }
            case R.id.favorite_count: {
                final AsyncTwitterWrapper twitter = getTwitterWrapper();
                if (twitter == null) return;
                if (status.is_favorite) {
                    twitter.destroyFavoriteAsync(status);
                } else {
                    twitter.createFavoriteAsync(status);
                }
                break;
            }
        }
    }

    @Override
    public void onStatusClick(StatusViewHolder holder, int position) {
        Utils.openStatus(getActivity(), mAdapter.getStatus(position), null);
    }

    @Override
    public void onStatusMenuClick(StatusViewHolder holder, View menuView, int position) {
        if (mPopupMenu != null) {
            mPopupMenu.dismiss();
        }
        final PopupMenu popupMenu = new PopupMenu(mAdapter.getContext(), menuView,
                Gravity.NO_GRAVITY, R.attr.actionOverflowMenuStyle, 0);
        popupMenu.setOnMenuItemClickListener(mOnStatusMenuItemClickListener);
        popupMenu.inflate(R.menu.action_status);
        final ParcelableStatus status = mAdapter.getStatus(position);
        setMenuForStatus(mAdapter.getContext(), popupMenu.getMenu(), status);
        popupMenu.show();
        mPopupMenu = popupMenu;
        mSelectedStatus = status;
    }

    @Override
    public void onRefresh() {
        triggerRefresh();
    }

    @Override
    public boolean scrollToStart() {
        saveReadPosition();
        mLayoutManager.scrollToPositionWithOffset(0, 0);
        setControlVisible(true);
        return true;
    }

    protected Object createMessageBusCallback() {
        return new StatusesBusCallback();
    }

    protected abstract long[] getAccountIds();

	protected Data getAdapterData() {
		return mAdapter.getData();
	}

    protected void setAdapterData(Data data) {
		mAdapter.setData(data);
	}

    protected String getReadPositionTag() {
        return null;
    }

    protected abstract boolean hasMoreData(Data data);

    protected abstract AbsStatusesAdapter<Data> onCreateAdapter(Context context, boolean compact);

    protected void saveReadPosition() {
        final String readPositionTag = getReadPositionTagWithAccounts();
        if (readPositionTag == null) return;
        final int position = mLayoutManager.findFirstVisibleItemPosition();
        if (position == RecyclerView.NO_POSITION) return;
        final ParcelableStatus status = mAdapter.getStatus(position);
        if (status == null) return;
        mReadStateManager.setPosition(readPositionTag, status.id);
        mReadStateManager.setPosition(getCurrentReadPositionTag(), status.id, true);
    }

    private String getCurrentReadPositionTag() {
        final String tag = getReadPositionTagWithAccounts();
        if (tag == null) return null;
        return tag + "_current";
    }

    private String getReadPositionTagWithAccounts() {
        return Utils.getReadPositionTagWithAccounts(getReadPositionTag(), getAccountIds());
    }

    private void setListShown(boolean shown) {
        mProgressContainer.setVisibility(shown ? View.GONE : View.VISIBLE);
        mSwipeRefreshLayout.setVisibility(shown ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onMediaClick(StatusViewHolder holder, ParcelableMedia media, int position) {
        final ParcelableStatus status = mAdapter.getStatus(position);
        if (status == null) return;
        Utils.openMedia(getActivity(), status, media);
    }

    private void updateRefreshProgressOffset() {
        if (mSystemWindowsInsets.top == 0 || mSwipeRefreshLayout == null || isRefreshing()) return;
        final float density = getResources().getDisplayMetrics().density;
        final int progressCircleDiameter = mSwipeRefreshLayout.getProgressCircleDiameter();
        final int swipeStart = (mSystemWindowsInsets.top - mControlBarOffsetPixels) - progressCircleDiameter;
        // 64: SwipeRefreshLayout.DEFAULT_CIRCLE_TARGET
        final int swipeDistance = Math.round(64 * density);
        mSwipeRefreshLayout.setProgressViewOffset(false, swipeStart, swipeStart + swipeDistance);
    }

    protected final class StatusesBusCallback {

        protected StatusesBusCallback() {
        }

        @Subscribe
        public void notifyStatusListChanged(StatusListChangedEvent event) {
            mAdapter.notifyDataSetChanged();
        }

	}
}