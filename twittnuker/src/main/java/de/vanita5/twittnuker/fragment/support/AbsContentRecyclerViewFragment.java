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

package de.vanita5.twittnuker.fragment.support;

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import de.vanita5.twittnuker.R;
import de.vanita5.twittnuker.activity.iface.IControlBarActivity;
import de.vanita5.twittnuker.adapter.LoadMoreSupportAdapter;
import de.vanita5.twittnuker.fragment.iface.RefreshScrollTopInterface;
import de.vanita5.twittnuker.util.ContentListScrollListener;
import de.vanita5.twittnuker.util.SimpleDrawerCallback;
import de.vanita5.twittnuker.util.ThemeUtils;
import de.vanita5.twittnuker.util.TwidereColorUtils;
import de.vanita5.twittnuker.util.Utils;
import de.vanita5.twittnuker.view.HeaderDrawerLayout;
import de.vanita5.twittnuker.view.iface.IExtendedView;
import de.vanita5.twittnuker.view.themed.AccentSwipeRefreshLayout;

public abstract class AbsContentRecyclerViewFragment<A extends LoadMoreSupportAdapter, L extends RecyclerView.LayoutManager>
        extends BaseSupportFragment implements SwipeRefreshLayout.OnRefreshListener,
        HeaderDrawerLayout.DrawerCallback, RefreshScrollTopInterface, IControlBarActivity.ControlBarOffsetListener,
        ContentListScrollListener.ContentListSupport, IControlBarActivity.ControlBarShowHideHelper.ControlBarAnimationListener {

    private View mProgressContainer;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;
    private View mErrorContainer;

    private L mLayoutManager;
    @SuppressWarnings("NullableProblems")
    @NonNull
    private A mAdapter;

    // Callbacks and listeners
    private SimpleDrawerCallback mDrawerCallback;
    private ContentListScrollListener mScrollListener;

    // Data fields
    private Rect mSystemWindowsInsets = new Rect();

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
    public void onControlBarOffsetChanged(IControlBarActivity activity, float offset) {
        updateRefreshProgressOffset();
    }

    @Override
    public void onRefresh() {
        triggerRefresh();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        updateRefreshProgressOffset();
    }

    @Override
    public void scrollBy(float dy) {
        mDrawerCallback.scrollBy(dy);
    }

    @Override
    public boolean scrollToStart() {
        scrollToPositionWithOffset(0, 0);
        mRecyclerView.stopScroll();
        setControlVisible(true);
        return true;
    }

    protected abstract void scrollToPositionWithOffset(int position, int offset);

    @Override
    public void onControlBarVisibleAnimationFinish(boolean visible) {
        updateRefreshProgressOffset();
    }

    @Override
    public void setControlVisible(boolean visible) {
        final FragmentActivity activity = getActivity();
        if (activity instanceof IControlBarActivity) {
            //TODO hide only if top > actionBar.height
            final L manager = getLayoutManager();
            if (manager.getChildCount() == 0) return;
            final View firstView = manager.getChildAt(0);
            final IControlBarActivity controlBarActivity = (IControlBarActivity) activity;
            if (manager.getPosition(firstView) != 0) {
                controlBarActivity.setControlBarVisibleAnimate(visible, this);
                return;
            }
            final int top = firstView.getTop();
            controlBarActivity.setControlBarVisibleAnimate(visible || top > 0, this);
        }
    }

    @Override
    public boolean shouldLayoutHeaderBottom() {
        return mDrawerCallback.shouldLayoutHeaderBottom();
    }

    @Override
    public void topChanged(int offset) {
        mDrawerCallback.topChanged(offset);
    }

    @NonNull
    @Override
    public A getAdapter() {
        return mAdapter;
    }

    @Override
    public abstract boolean isRefreshing();

    public L getLayoutManager() {
        return mLayoutManager;
    }

    public void setRefreshing(final boolean refreshing) {
        final boolean currentRefreshing = mSwipeRefreshLayout.isRefreshing();
        if (!currentRefreshing) {
            updateRefreshProgressOffset();
        }
        if (refreshing == currentRefreshing) return;
        final boolean layoutRefreshing = refreshing && !mAdapter.isLoadMoreIndicatorVisible();
        mSwipeRefreshLayout.setRefreshing(layoutRefreshing);
    }

    @Override
    public void onLoadMoreContents(boolean fromStart) {
        setLoadMoreIndicatorVisible(true);
        setRefreshEnabled(false);
    }

    public final RecyclerView getRecyclerView() {
        return mRecyclerView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof IControlBarActivity) {
            ((IControlBarActivity) context).registerControlBarOffsetListener(this);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_content_recyclerview, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mDrawerCallback = new SimpleDrawerCallback(mRecyclerView);

        final View view = getView();
        assert view != null;
        final Context context = view.getContext();
        final boolean compact = Utils.isCompactCards(context);
        final int backgroundColor = ThemeUtils.getThemeBackgroundColor(context);
        final int colorRes = TwidereColorUtils.getContrastYIQ(backgroundColor,
                R.color.bg_refresh_progress_color_light, R.color.bg_refresh_progress_color_dark);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.setProgressBackgroundColorSchemeResource(colorRes);
        mAdapter = onCreateAdapter(context, compact);
        mLayoutManager = onCreateLayoutManager(context);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setHasFixedSize(true);
        if (mSwipeRefreshLayout instanceof AccentSwipeRefreshLayout) {
            ((AccentSwipeRefreshLayout) mSwipeRefreshLayout).setTouchInterceptor(new IExtendedView.TouchInterceptor() {
                @Override
                public boolean dispatchTouchEvent(View view, MotionEvent event) {
                    return false;
                }

                @Override
                public boolean onInterceptTouchEvent(View view, MotionEvent event) {
                    if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                        updateRefreshProgressOffset();
                    }
                    return false;
                }

                @Override
                public boolean onTouchEvent(View view, MotionEvent event) {
                    return false;
                }

            });
        }
        setupRecyclerView(context, compact);
        mRecyclerView.setAdapter(mAdapter);

        mScrollListener = new ContentListScrollListener(this);
        mScrollListener.setTouchSlop(ViewConfiguration.get(context).getScaledTouchSlop());
    }

    protected abstract void setupRecyclerView(Context context, boolean compact);

    @NonNull
    protected abstract L onCreateLayoutManager(Context context);

    @Override
    public void onStart() {
        super.onStart();
        mRecyclerView.addOnScrollListener(mScrollListener);
    }

    @Override
    public void onStop() {
        mRecyclerView.removeOnScrollListener(mScrollListener);
        super.onStop();
    }

    @Override
    public void onBaseViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onBaseViewCreated(view, savedInstanceState);
        mProgressContainer = view.findViewById(R.id.progress_container);
        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_layout);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        mErrorContainer = view.findViewById(R.id.error_container);
    }

    @Override
    public void onDetach() {
        final FragmentActivity activity = getActivity();
        if (activity instanceof IControlBarActivity) {
            ((IControlBarActivity) activity).unregisterControlBarOffsetListener(this);
        }
        super.onDetach();
    }

    @NonNull
    protected Rect getExtraContentPadding() {
        return new Rect();
    }

    @Override
    protected void fitSystemWindows(Rect insets) {
        final Rect extraPadding = getExtraContentPadding();
        mRecyclerView.setPadding(insets.left + extraPadding.left, insets.top + extraPadding.top,
                insets.right + extraPadding.right, insets.bottom + extraPadding.bottom);
        mErrorContainer.setPadding(insets.left, insets.top, insets.right, insets.bottom);
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

    @Override
    public boolean triggerRefresh() {
        return false;
    }

    @NonNull
    protected abstract A onCreateAdapter(Context context, boolean compact);

    protected final void showContent() {
        mErrorContainer.setVisibility(View.GONE);
        mProgressContainer.setVisibility(View.GONE);
        mSwipeRefreshLayout.setVisibility(View.VISIBLE);
    }

    protected final void showProgress() {
        mErrorContainer.setVisibility(View.GONE);
        mProgressContainer.setVisibility(View.VISIBLE);
        mSwipeRefreshLayout.setVisibility(View.GONE);
    }

    protected final void showError(int icon, CharSequence text) {
        mErrorContainer.setVisibility(View.VISIBLE);
        mProgressContainer.setVisibility(View.GONE);
        mSwipeRefreshLayout.setVisibility(View.GONE);
        final ImageView errorIconView = (ImageView) mErrorContainer.findViewById(R.id.error_icon);
        final TextView errorTextView = (TextView) mErrorContainer.findViewById(R.id.error_text);
        errorIconView.setImageResource(icon);
        errorTextView.setText(text);
    }

    protected final void showEmpty(int icon, CharSequence text) {
        mErrorContainer.setVisibility(View.VISIBLE);
        mProgressContainer.setVisibility(View.GONE);
        mSwipeRefreshLayout.setVisibility(View.VISIBLE);
        final ImageView errorIconView = (ImageView) mErrorContainer.findViewById(R.id.error_icon);
        final TextView errorTextView = (TextView) mErrorContainer.findViewById(R.id.error_text);
        errorIconView.setImageResource(icon);
        errorTextView.setText(text);
    }

    protected void updateRefreshProgressOffset() {
        final FragmentActivity activity = getActivity();
        final Rect insets = this.mSystemWindowsInsets;
        final SwipeRefreshLayout layout = this.mSwipeRefreshLayout;
        if (!(activity instanceof IControlBarActivity) || insets.top == 0 || layout == null
                || isRefreshing()) {
            return;
        }
        final int progressCircleDiameter = layout.getProgressCircleDiameter();
        if (progressCircleDiameter == 0) return;
        final float density = getResources().getDisplayMetrics().density;
        final IControlBarActivity control = (IControlBarActivity) activity;
        final int controlBarOffsetPixels = Math.round(control.getControlBarHeight() * (1 - control.getControlBarOffset()));
        final int swipeStart = (insets.top - controlBarOffsetPixels) - progressCircleDiameter;
        // 64: SwipeRefreshLayout.DEFAULT_CIRCLE_TARGET
        final int swipeDistance = Math.round(64 * density);
        layout.setProgressViewOffset(false, swipeStart, swipeStart + swipeDistance);
    }
}