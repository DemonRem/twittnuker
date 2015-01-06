/*
 * Twittnuker - Twitter client for Android
 *
 * Copyright (C) 2013-2015 vanita5 <mail@vanita5.de>
 *
 * This program incorporates a modified version of Twidere.
 * Copyright (C) 2012-2014 Mariotaku Lee <mariotaku.lee@gmail.com>
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

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.OnScrollListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.squareup.otto.Bus;

import de.vanita5.twittnuker.R;
import de.vanita5.twittnuker.adapter.AbsActivitiesAdapter;
import de.vanita5.twittnuker.adapter.decorator.DividerItemDecoration;
import de.vanita5.twittnuker.app.TwittnukerApplication;
import de.vanita5.twittnuker.fragment.iface.RefreshScrollTopInterface;
import de.vanita5.twittnuker.util.AsyncTwitterWrapper;
import de.vanita5.twittnuker.util.SimpleDrawerCallback;
import de.vanita5.twittnuker.util.ThemeUtils;
import de.vanita5.twittnuker.util.Utils;
import de.vanita5.twittnuker.view.HeaderDrawerLayout.DrawerCallback;

public abstract class AbsActivitiesFragment<Data> extends BaseSupportFragment implements LoaderCallbacks<Data>,
		OnRefreshListener, DrawerCallback, RefreshScrollTopInterface {


	private final Object mStatusesBusCallback;

	protected AbsActivitiesFragment() {
		mStatusesBusCallback = createMessageBusCallback();
	}

	private View mContentView;
	private SharedPreferences mPreferences;
	private View mProgressContainer;
	private SwipeRefreshLayout mSwipeRefreshLayout;
	private RecyclerView mRecyclerView;
	private AbsActivitiesAdapter<Data> mAdapter;
	private SimpleDrawerCallback mDrawerCallback;
	private OnScrollListener mOnScrollListener = new OnScrollListener() {

        private int mScrollState;

		@Override
		public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
			super.onScrollStateChanged(recyclerView, newState);
            mScrollState = newState;
		}

		@Override
		public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
			final LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
			if (isRefreshing()) return;
            if (mAdapter.hasLoadMoreIndicator() && mScrollState != RecyclerView.SCROLL_STATE_IDLE
					&& layoutManager.findLastVisibleItemPosition() == mAdapter.getItemCount() - 1) {
				onLoadMoreStatuses();
			}
		}
	};

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

	public AbsActivitiesAdapter<Data> getAdapter() {
		return mAdapter;
	}

	public SharedPreferences getSharedPreferences() {
		if (mPreferences != null) return mPreferences;
		return mPreferences = getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
	}

	public abstract int getStatuses(long[] accountIds, long[] maxIds, long[] sinceIds);

	public boolean isRefreshing() {
		return mSwipeRefreshLayout.isRefreshing();
	}

	public void setRefreshing(boolean refreshing) {
		mSwipeRefreshLayout.setRefreshing(refreshing);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_recycler_view, container, false);
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		final View view = getView();
		if (view == null) throw new AssertionError();
		final Context context = view.getContext();
		final boolean compact = Utils.isCompactCards(context);
		mDrawerCallback = new SimpleDrawerCallback(mRecyclerView);
		mSwipeRefreshLayout.setOnRefreshListener(this);
		mSwipeRefreshLayout.setColorSchemeColors(ThemeUtils.getUserAccentColor(context));
		mAdapter = onCreateAdapter(context, compact);
		mAdapter.setLoadMoreIndicatorEnabled(true);
		final LinearLayoutManager layoutManager = new LinearLayoutManager(context);
		layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
		mRecyclerView.setLayoutManager(layoutManager);
		mRecyclerView.addItemDecoration(new DividerItemDecoration(context, layoutManager.getOrientation()));
		mRecyclerView.setAdapter(mAdapter);
		mRecyclerView.setOnScrollListener(mOnScrollListener);
		getLoaderManager().initLoader(0, getArguments(), this);
		setListShown(false);
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
	public void onLoadFinished(Loader<Data> loader, Data data) {
		setRefreshing(false);
		mAdapter.setData(data);
		setListShown(true);
	}

	@Override
	public void onLoaderReset(Loader<Data> loader) {
	}

	@Override
	public void onRefresh() {
		triggerRefresh();
	}


	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		mContentView = view.findViewById(R.id.fragment_content);
		mProgressContainer = view.findViewById(R.id.progress_container);
		mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_layout);
		mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
		super.onViewCreated(view, savedInstanceState);
	}

	@Override
	protected void fitSystemWindows(Rect insets) {
		super.fitSystemWindows(insets);
		mContentView.setPadding(insets.left, insets.top, insets.right, insets.bottom);
	}

	@Override
	public boolean scrollToStart() {
		final AsyncTwitterWrapper twitter = getTwitterWrapper();
		final int tabPosition = getTabPosition();
		if (twitter != null && tabPosition != -1) {
			twitter.clearUnreadCountAsync(tabPosition);
		}
        mRecyclerView.smoothScrollToPosition(0);
		return true;
	}

	protected abstract long[] getAccountIds();

	protected Data getAdapterData() {
		return mAdapter.getData();
	}

	protected void setAdapterData(Data data) {
		mAdapter.setData(data);
	}

	protected Object createMessageBusCallback() {
		return new StatusesBusCallback();
	}

	protected abstract AbsActivitiesAdapter<Data> onCreateAdapter(Context context, boolean compact);

	protected abstract void onLoadMoreStatuses();

	private void setListShown(boolean shown) {
		mProgressContainer.setVisibility(shown ? View.GONE : View.VISIBLE);
		mSwipeRefreshLayout.setVisibility(shown ? View.VISIBLE : View.GONE);
	}


	protected final class StatusesBusCallback {

		protected StatusesBusCallback() {
		}


	}
}