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

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.squareup.otto.Bus;

import de.vanita5.twittnuker.R;
import de.vanita5.twittnuker.adapter.AbsActivitiesAdapter;
import de.vanita5.twittnuker.adapter.AbsActivitiesAdapter.ActivityAdapterListener;
import de.vanita5.twittnuker.app.TwittnukerApplication;
import de.vanita5.twittnuker.fragment.iface.RefreshScrollTopInterface;
import de.vanita5.twittnuker.model.ParcelableActivity;
import de.vanita5.twittnuker.util.AsyncTwitterWrapper;
import de.vanita5.twittnuker.view.HeaderDrawerLayout.DrawerCallback;
import de.vanita5.twittnuker.view.holder.GapViewHolder;

public abstract class AbsActivitiesFragment<Data> extends AbsContentRecyclerViewFragment<AbsActivitiesAdapter<Data>>
        implements LoaderCallbacks<Data>, OnRefreshListener, DrawerCallback, RefreshScrollTopInterface,
        ActivityAdapterListener {

	private final Object mStatusesBusCallback;

	protected AbsActivitiesFragment() {
		mStatusesBusCallback = createMessageBusCallback();
	}

	private SharedPreferences mPreferences;


		@Override
    public void onGapClick(GapViewHolder holder, int position) {
        final ParcelableActivity activity = getAdapter().getActivity(position);
        final long[] accountIds = {activity.account_id};
        final long[] maxIds = {activity.min_position};
        getActivities(accountIds, maxIds, null);
    }


	public SharedPreferences getSharedPreferences() {
		if (mPreferences != null) return mPreferences;
		return mPreferences = getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
	}

    public abstract int getActivities(long[] accountIds, long[] maxIds, long[] sinceIds);

	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_content_recyclerview, container, false);
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		final View view = getView();
		if (view == null) throw new AssertionError();
        getAdapter().setListener(this);
		getLoaderManager().initLoader(0, getArguments(), this);
        showProgress();
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
        getAdapter().setData(data);
        showContent();
	}

	@Override
	public void onLoaderReset(Loader<Data> loader) {
	}

	@Override
	public void onRefresh() {
		triggerRefresh();
	}


	@Override
	public boolean scrollToStart() {
        final boolean result = super.scrollToStart();
        if (result) {
			final AsyncTwitterWrapper twitter = getTwitterWrapper();
			final int tabPosition = getTabPosition();
			if (twitter != null && tabPosition != -1) {
				twitter.clearUnreadCountAsync(tabPosition);
			}
        }
		return true;
	}

	protected abstract long[] getAccountIds();

	protected Data getAdapterData() {
        return getAdapter().getData();
	}

	protected void setAdapterData(Data data) {
        getAdapter().setData(data);
	}

	protected Object createMessageBusCallback() {
		return new StatusesBusCallback();
	}

	protected final class StatusesBusCallback {

		protected StatusesBusCallback() {
		}


	}
}