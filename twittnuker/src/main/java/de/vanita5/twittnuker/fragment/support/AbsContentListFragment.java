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
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.support.v7.widget.FixedLinearLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import de.vanita5.twittnuker.R;
import de.vanita5.twittnuker.activity.iface.IControlBarActivity;
import de.vanita5.twittnuker.activity.iface.IControlBarActivity.ControlBarOffsetListener;
import de.vanita5.twittnuker.activity.support.BaseActionBarActivity;
import de.vanita5.twittnuker.adapter.decorator.DividerItemDecoration;
import de.vanita5.twittnuker.adapter.iface.IContentCardAdapter;
import de.vanita5.twittnuker.fragment.iface.RefreshScrollTopInterface;
import de.vanita5.twittnuker.util.ColorUtils;
import de.vanita5.twittnuker.util.ContentListScrollListener;
import de.vanita5.twittnuker.util.ContentListScrollListener.ContentListSupport;
import de.vanita5.twittnuker.util.SimpleDrawerCallback;
import de.vanita5.twittnuker.util.ThemeUtils;
import de.vanita5.twittnuker.util.Utils;
import de.vanita5.twittnuker.view.HeaderDrawerLayout.DrawerCallback;

public abstract class AbsContentListFragment<A extends IContentCardAdapter> extends BaseSupportFragment implements OnRefreshListener,
		DrawerCallback, RefreshScrollTopInterface, ControlBarOffsetListener, ContentListSupport {

	private Rect mSystemWindowsInsets = new Rect();
	private int mControlBarOffsetPixels;
	private LinearLayoutManager mLayoutManager;
	private View mProgressContainer;
	private SwipeRefreshLayout mSwipeRefreshLayout;
	private RecyclerView mRecyclerView;
	private SimpleDrawerCallback mDrawerCallback;

	private A mAdapter;
	private ContentListScrollListener mScrollListener;

	public boolean canScroll(float dy) {
		return mDrawerCallback.canScroll(dy);
	}

	public void cancelTouch() {
		mDrawerCallback.cancelTouch();
	}

	public void fling(float velocity) {
		mDrawerCallback.fling(velocity);
	}

	public boolean isScrollContent(float x, float y) {
		return mDrawerCallback.isScrollContent(x, y);
	}

	@Override
	public void onControlBarOffsetChanged(IControlBarActivity activity, float offset) {
		mControlBarOffsetPixels = Math.round(activity.getControlBarHeight() * (1 - offset));
		updateRefreshProgressOffset();
	}

	@Override
	public void onRefresh() {
		triggerRefresh();
	}

	public void scrollBy(float dy) {
		mDrawerCallback.scrollBy(dy);
	}

	@Override
	public boolean scrollToStart() {
		mLayoutManager.scrollToPositionWithOffset(0, 0);
		mRecyclerView.stopScroll();
		setControlVisible(true);
		return true;
	}

	public void setControlVisible(boolean visible) {
		final FragmentActivity activity = getActivity();
		if (activity instanceof BaseActionBarActivity) {
			((BaseActionBarActivity) activity).setControlBarVisibleAnimate(visible);
		}
	}

	public boolean shouldLayoutHeaderBottom() {
		return mDrawerCallback.shouldLayoutHeaderBottom();
	}

	public void topChanged(int offset) {
		mDrawerCallback.topChanged(offset);
	}

	public A getAdapter() {
		return mAdapter;
	}

	public abstract boolean isRefreshing();

	public LinearLayoutManager getLayoutManager() {
		return mLayoutManager;
	}

	public void setRefreshing(boolean refreshing) {
		if (refreshing == mSwipeRefreshLayout.isRefreshing()) return;
//        if (!refreshing) updateRefreshProgressOffset();
		mSwipeRefreshLayout.setRefreshing(refreshing && !mAdapter.isLoadMoreIndicatorVisible());
	}

	public void onLoadMoreContents() {
		setLoadMoreIndicatorVisible(true);
		setRefreshEnabled(false);
	}

	public final RecyclerView getRecyclerView() {
		return mRecyclerView;
	}

	public final ContentListScrollListener getScrollListener() {
		return mScrollListener;
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
		mDrawerCallback = new SimpleDrawerCallback(mRecyclerView);

		final View view = getView();
		if (view == null) throw new AssertionError();
		final Context context = view.getContext();
		final boolean compact = Utils.isCompactCards(context);
		final int backgroundColor = ThemeUtils.getThemeBackgroundColor(context);
		final int colorRes = ColorUtils.getContrastYIQ(backgroundColor,
				R.color.bg_refresh_progress_color_light, R.color.bg_refresh_progress_color_dark);
		mSwipeRefreshLayout.setOnRefreshListener(this);
		mSwipeRefreshLayout.setColorSchemeColors(ThemeUtils.getUserAccentColor(context));
		mSwipeRefreshLayout.setProgressBackgroundColorSchemeResource(colorRes);
		mAdapter = onCreateAdapter(context, compact);
		mLayoutManager = new FixedLinearLayoutManager(context);
		mLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
		mRecyclerView.setLayoutManager(mLayoutManager);
		mRecyclerView.setHasFixedSize(true);
		if (compact) {
			mRecyclerView.addItemDecoration(new DividerItemDecoration(context, mLayoutManager.getOrientation()));
		}
		mRecyclerView.setAdapter((RecyclerView.Adapter) mAdapter);

		mScrollListener = new ContentListScrollListener(this);
		mScrollListener.setTouchSlop(ViewConfiguration.get(context).getScaledTouchSlop());
		mRecyclerView.setOnScrollListener(mScrollListener);
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

	public void setLoadMoreIndicatorVisible(boolean visible) {
		mAdapter.setLoadMoreIndicatorVisible(visible);
	}

	public void setRefreshEnabled(boolean enabled) {
		mSwipeRefreshLayout.setEnabled(enabled);
	}

	@NonNull
	protected abstract A onCreateAdapter(Context context, boolean compact);

	protected final void setListShown(boolean shown) {
		mProgressContainer.setVisibility(shown ? View.GONE : View.VISIBLE);
		mSwipeRefreshLayout.setVisibility(shown ? View.VISIBLE : View.GONE);
	}

	protected void updateRefreshProgressOffset() {
		if (mSystemWindowsInsets.top == 0 || mSwipeRefreshLayout == null || isRefreshing()) return;
		final float density = getResources().getDisplayMetrics().density;
		final int progressCircleDiameter = mSwipeRefreshLayout.getProgressCircleDiameter();
		final int swipeStart = (mSystemWindowsInsets.top - mControlBarOffsetPixels) - progressCircleDiameter;
		// 64: SwipeRefreshLayout.DEFAULT_CIRCLE_TARGET
		final int swipeDistance = Math.round(64 * density);
		mSwipeRefreshLayout.setProgressViewOffset(false, swipeStart, swipeStart + swipeDistance);
	}
}