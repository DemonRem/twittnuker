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

import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.vanita5.twittnuker.R;
import de.vanita5.twittnuker.activity.iface.IThemedActivity;
import de.vanita5.twittnuker.adapter.support.SupportTabsAdapter;
import de.vanita5.twittnuker.fragment.BaseFiltersFragment.FilteredKeywordsFragment;
import de.vanita5.twittnuker.fragment.BaseFiltersFragment.FilteredLinksFragment;
import de.vanita5.twittnuker.fragment.BaseFiltersFragment.FilteredSourcesFragment;
import de.vanita5.twittnuker.fragment.BaseFiltersFragment.FilteredUsersFragment;
import de.vanita5.twittnuker.fragment.iface.IBaseFragment;
import de.vanita5.twittnuker.fragment.iface.RefreshScrollTopInterface;
import de.vanita5.twittnuker.fragment.iface.SupportFragmentCallback;
import de.vanita5.twittnuker.graphic.EmptyDrawable;
import de.vanita5.twittnuker.util.ThemeUtils;
import de.vanita5.twittnuker.view.TabPagerIndicator;

public class FiltersFragment extends BaseSupportFragment implements RefreshScrollTopInterface,
		SupportFragmentCallback, IBaseFragment.SystemWindowsInsetsCallback {

    private SupportTabsAdapter mPagerAdapter;

	private TabPagerIndicator mPagerIndicator;
	private ViewPager mViewPager;
    private View mPagerOverlay;

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_content_pages, container, false);
	}

	@Override
	public void onActivityCreated(final Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		final FragmentActivity activity = getActivity();
		mPagerAdapter = new SupportTabsAdapter(activity, getChildFragmentManager(), null, 1);
		mViewPager.setAdapter(mPagerAdapter);
		mViewPager.setOffscreenPageLimit(2);
		mPagerIndicator.setViewPager(mViewPager);
		mPagerIndicator.setTabDisplayOption(TabPagerIndicator.LABEL);


		mPagerAdapter.addTab(FilteredUsersFragment.class, null, getString(R.string.users), null, 0, null);
		mPagerAdapter.addTab(FilteredKeywordsFragment.class, null, getString(R.string.keywords), null, 1, null);
		mPagerAdapter.addTab(FilteredSourcesFragment.class, null, getString(R.string.sources), null, 2, null);
		mPagerAdapter.addTab(FilteredLinksFragment.class, null, getString(R.string.links), null, 3, null);

        ThemeUtils.initPagerIndicatorAsActionBarTab(activity, mPagerIndicator, mPagerOverlay);
		ThemeUtils.setCompatToolbarOverlay(activity, new EmptyDrawable());
		ThemeUtils.setCompatContentViewOverlay(activity, new EmptyDrawable());
		ThemeUtils.setWindowOverlayViewOverlay(activity, new EmptyDrawable());

        if (activity instanceof IThemedActivity) {
            final String backgroundOption = ((IThemedActivity) activity).getCurrentThemeBackgroundOption();
            final boolean isTransparent = ThemeUtils.isTransparentBackground(backgroundOption);
            final int actionBarAlpha = isTransparent ? ThemeUtils.getActionBarAlpha(ThemeUtils.getUserThemeBackgroundAlpha(activity)) : 0xFF;
            mPagerIndicator.setAlpha(actionBarAlpha / 255f);
        }
	}

	@Override
	public void onBaseViewCreated(View view, Bundle savedInstanceState) {
		super.onBaseViewCreated(view, savedInstanceState);
		mViewPager = (ViewPager) view.findViewById(R.id.view_pager);
		mPagerIndicator = (TabPagerIndicator) view.findViewById(R.id.view_pager_tabs);
        mPagerOverlay = view.findViewById(R.id.pager_window_overlay);
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
	public boolean getSystemWindowsInsets(Rect insets) {
		return false;
	}

	@Override
	public boolean triggerRefresh(int position) {
		return false;
	}
}