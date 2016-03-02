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
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;

import de.vanita5.twittnuker.adapter.ParcelableUserListsAdapter;
import de.vanita5.twittnuker.adapter.iface.ILoadMoreSupportAdapter.IndicatorPosition;
import de.vanita5.twittnuker.model.ParcelableUserList;

import java.util.List;

public abstract class ParcelableUserListsFragment extends AbsUserListsFragment<List<ParcelableUserList>> {

    @Override
    public boolean isRefreshing() {
        if (getContext() == null || isDetached()) return false;
        final LoaderManager lm = getLoaderManager();
        return lm.hasRunningLoaders();
    }

    @NonNull
    @Override
    protected final ParcelableUserListsAdapter onCreateAdapter(Context context, boolean compact) {
        return new ParcelableUserListsAdapter(context, compact);
    }

    protected long getAccountId() {
        final Bundle args = getArguments();
        return args != null ? args.getLong(EXTRA_ACCOUNT_ID, -1) : -1;
    }

    @Override
    protected boolean hasMoreData(List<ParcelableUserList> data) {
        return data == null || !data.isEmpty();
    }

    @Override
    public void onLoadFinished(Loader<List<ParcelableUserList>> loader, List<ParcelableUserList> data) {
        super.onLoadFinished(loader, data);
        setRefreshEnabled(true);
        setRefreshing(false);
        setLoadMoreIndicatorPosition(IndicatorPosition.NONE);
    }

    @Override
    public void onLoadMoreContents(@IndicatorPosition int position) {
        // Only supports load from end, skip START flag
        if ((position & IndicatorPosition.START) != 0) return;
        super.onLoadMoreContents(position);
        if (position == 0) return;
        final Bundle loaderArgs = new Bundle(getArguments());
        loaderArgs.putBoolean(EXTRA_FROM_USER, true);
        loaderArgs.putLong(EXTRA_NEXT_CURSOR, getNextCursor());
        getLoaderManager().restartLoader(0, loaderArgs, this);
    }

    protected void removeUsers(long... ids) {
        //TODO remove from adapter
    }

}