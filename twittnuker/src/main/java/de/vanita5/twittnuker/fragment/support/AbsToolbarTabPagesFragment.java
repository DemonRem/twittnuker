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
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.vanita5.twittnuker.R;
import de.vanita5.twittnuker.activity.LinkHandlerActivity;
import de.vanita5.twittnuker.activity.LinkHandlerActivity.HideUiOnScroll;
import de.vanita5.twittnuker.activity.iface.IControlBarActivity;
import de.vanita5.twittnuker.activity.iface.IControlBarActivity.ControlBarOffsetListener;
import de.vanita5.twittnuker.adapter.SupportTabsAdapter;
import de.vanita5.twittnuker.fragment.iface.IBaseFragment;
import de.vanita5.twittnuker.fragment.iface.IToolBarSupportFragment;
import de.vanita5.twittnuker.fragment.iface.RefreshScrollTopInterface;
import de.vanita5.twittnuker.fragment.iface.SupportFragmentCallback;
import de.vanita5.twittnuker.util.KeyboardShortcutsHandler;
import de.vanita5.twittnuker.util.KeyboardShortcutsHandler.KeyboardShortcutCallback;
import de.vanita5.twittnuker.view.ExtendedLinearLayout;
import de.vanita5.twittnuker.view.TabPagerIndicator;
import de.vanita5.twittnuker.view.iface.IExtendedView;

public abstract class AbsToolbarTabPagesFragment extends BaseSupportFragment implements
        RefreshScrollTopInterface, SupportFragmentCallback, IBaseFragment.SystemWindowsInsetsCallback,
        ControlBarOffsetListener, HideUiOnScroll, OnPageChangeListener, IToolBarSupportFragment,
        KeyboardShortcutCallback {

    private SupportTabsAdapter mPagerAdapter;
    private TabPagerIndicator mPagerIndicator;
    private ViewPager mViewPager;
    private View mPagerOverlay;
    private Toolbar mToolbar;
    private int mControlBarHeight;
    private ExtendedLinearLayout mToolbarContainer;

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final FragmentActivity activity = getActivity();
        mPagerAdapter = new SupportTabsAdapter(activity, getChildFragmentManager(), null, 1);
        mViewPager.setAdapter(mPagerAdapter);
        mViewPager.setOffscreenPageLimit(2);
        mViewPager.addOnPageChangeListener(this);
        mPagerIndicator.setViewPager(mViewPager);
        mPagerIndicator.setTabDisplayOption(TabPagerIndicator.LABEL);


        addTabs(mPagerAdapter);
        mToolbarContainer.setOnSizeChangedListener(new IExtendedView.OnSizeChangedListener() {
            @Override
            public void onSizeChanged(View view, int w, int h, int oldw, int oldh) {
                final int pageLimit = mViewPager.getOffscreenPageLimit();
                final int currentItem = mViewPager.getCurrentItem();
                final int count = mPagerAdapter.getCount();
                for (int i = 0; i < count; i++) {
                    if (i > currentItem - pageLimit - 1 || i < currentItem + pageLimit) {
                        Object obj = mPagerAdapter.instantiateItem(mViewPager, i);
                        if (obj instanceof IBaseFragment) {
                            ((IBaseFragment) obj).requestFitSystemWindows();
                        }
                    }
                }
            }
        });
    }

    protected abstract void addTabs(SupportTabsAdapter adapter);

    @Override
    public void onDestroy() {
        mViewPager.removeOnPageChangeListener(this);
        super.onDestroy();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof IControlBarActivity) {
            ((IControlBarActivity) context).registerControlBarOffsetListener(this);
        }
    }

    @Override
    public void onDetach() {
        final FragmentActivity activity = getActivity();
        if (activity instanceof IControlBarActivity) {
            ((IControlBarActivity) activity).unregisterControlBarOffsetListener(this);
        }
        super.onDetach();
    }

    @Nullable
    @Override
    public final View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_toolbar_tab_pages, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewPager = (ViewPager) view.findViewById(R.id.view_pager);
        mToolbarContainer = (ExtendedLinearLayout) view.findViewById(R.id.toolbar_container);
        mPagerIndicator = (TabPagerIndicator) view.findViewById(R.id.toolbar_tabs);
        mPagerOverlay = view.findViewById(R.id.pager_window_overlay);
        mToolbar = (Toolbar) view.findViewById(R.id.toolbar);

        final Object host = getHost();
        if (host instanceof AppCompatActivity) {
            ((AppCompatActivity) host).setSupportActionBar(mToolbar);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        final Object o = mPagerAdapter.instantiateItem(mViewPager, mViewPager.getCurrentItem());
        if (o instanceof Fragment) {
            ((Fragment) o).onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public boolean scrollToStart() {
        final Fragment fragment = getCurrentVisibleFragment();
        if (!(fragment instanceof RefreshScrollTopInterface)) return false;
        ((RefreshScrollTopInterface) fragment).scrollToStart();
        return true;
    }

    @Override
    public boolean triggerRefresh() {
        return false;
    }

    @Override
    public Fragment getCurrentVisibleFragment() {
        final int currentItem = mViewPager.getCurrentItem();
        if (currentItem < 0 || currentItem >= mPagerAdapter.getCount()) return null;
        return (Fragment) mPagerAdapter.instantiateItem(mViewPager, currentItem);
    }

    @Override
    public boolean triggerRefresh(int position) {
        return false;
    }

    @Override
    protected void fitSystemWindows(Rect insets) {
    }

    @Override
    public boolean getSystemWindowsInsets(Rect insets) {
        if (mPagerIndicator == null) return false;
        insets.set(0, mToolbarContainer.getHeight(), 0, 0);
        return true;
    }

    @Override
    public void onControlBarOffsetChanged(IControlBarActivity activity, float offset) {
        mControlBarHeight = activity.getControlBarHeight();
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {

    }

    @Override
    public void onPageScrollStateChanged(int state) {
        final FragmentActivity activity = getActivity();
        if (activity instanceof LinkHandlerActivity) {
            ((LinkHandlerActivity) activity).setControlBarVisibleAnimate(true);
        }
    }

    @Override
    public Toolbar getToolbar() {
        return mToolbar;
    }

    @Override
    public float getControlBarOffset() {
        if (mToolbarContainer == null) return 0;
        return 1 + mToolbarContainer.getTranslationY() / getControlBarHeight();
    }

    @Override
    public int getControlBarHeight() {
        if (mToolbar == null) return 0;
        return mToolbar.getMeasuredHeight();
    }

    @Override
    public void setControlBarOffset(float offset) {
        if (mToolbarContainer == null) return;
        mToolbarContainer.setTranslationY((offset - 1) * getControlBarHeight());
    }

    @Override
    public boolean setupWindow(FragmentActivity activity) {
        return false;
    }

    @Override
    public boolean handleKeyboardShortcutSingle(@NonNull KeyboardShortcutsHandler handler, int keyCode, @NonNull KeyEvent event, int metaState) {
        if (handleFragmentKeyboardShortcutSingle(handler, keyCode, event, metaState)) return true;
        final String action = handler.getKeyAction(CONTEXT_TAG_NAVIGATION, keyCode, event, metaState);
        if (action != null) {
            switch (action) {
                case ACTION_NAVIGATION_PREVIOUS_TAB: {
                    final int previous = mViewPager.getCurrentItem() - 1;
                    if (previous >= 0 && previous < mPagerAdapter.getCount()) {
                        mViewPager.setCurrentItem(previous, true);
                    }
                    return true;
                }
                case ACTION_NAVIGATION_NEXT_TAB: {
                    final int next = mViewPager.getCurrentItem() + 1;
                    if (next >= 0 && next < mPagerAdapter.getCount()) {
                        mViewPager.setCurrentItem(next, true);
                    }
                    return true;
                }
            }
        }
        return handler.handleKey(getActivity(), null, keyCode, event, metaState);
    }

    @Override
    public boolean isKeyboardShortcutHandled(@NonNull KeyboardShortcutsHandler handler, int keyCode,
                                             @NonNull KeyEvent event, int metaState) {
        if (isFragmentKeyboardShortcutHandled(handler, keyCode, event, metaState)) return true;
        final String action = handler.getKeyAction(CONTEXT_TAG_NAVIGATION, keyCode, event, metaState);
        return ACTION_NAVIGATION_PREVIOUS_TAB.equals(action) || ACTION_NAVIGATION_NEXT_TAB.equals(action);
    }

    @Override
    public boolean handleKeyboardShortcutRepeat(@NonNull KeyboardShortcutsHandler handler, int keyCode,
                                                int repeatCount, @NonNull KeyEvent event, int metaState) {
        return handleFragmentKeyboardShortcutRepeat(handler, keyCode, repeatCount, event, metaState);
    }

    private Fragment getKeyboardShortcutRecipient() {
        return getCurrentVisibleFragment();
    }

    private boolean handleFragmentKeyboardShortcutRepeat(KeyboardShortcutsHandler handler, int keyCode,
                                                         int repeatCount, @NonNull KeyEvent event, int metaState) {
        final Fragment fragment = getKeyboardShortcutRecipient();
        if (fragment instanceof KeyboardShortcutCallback) {
            return ((KeyboardShortcutCallback) fragment).handleKeyboardShortcutRepeat(handler, keyCode,
                    repeatCount, event, metaState);
        }
        return false;
    }

    private boolean handleFragmentKeyboardShortcutSingle(KeyboardShortcutsHandler handler, int keyCode,
                                                         @NonNull KeyEvent event, int metaState) {
        final Fragment fragment = getKeyboardShortcutRecipient();
        if (fragment instanceof KeyboardShortcutCallback) {
            return ((KeyboardShortcutCallback) fragment).handleKeyboardShortcutSingle(handler, keyCode,
                    event, metaState);
        }
        return false;
    }

    private boolean isFragmentKeyboardShortcutHandled(KeyboardShortcutsHandler handler, int keyCode,
                                                      @NonNull KeyEvent event, int metaState) {
        final Fragment fragment = getKeyboardShortcutRecipient();
        if (fragment instanceof KeyboardShortcutCallback) {
            return ((KeyboardShortcutCallback) fragment).isKeyboardShortcutHandled(handler, keyCode,
                    event, metaState);
        }
        return false;
    }
}