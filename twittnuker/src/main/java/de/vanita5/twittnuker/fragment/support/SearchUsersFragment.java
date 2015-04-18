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
import android.os.Bundle;
import android.support.v4.content.Loader;

import de.vanita5.twittnuker.loader.iface.IExtendedLoader;
import de.vanita5.twittnuker.loader.support.UserSearchLoader;
import de.vanita5.twittnuker.model.ParcelableUser;

import java.util.List;

public class SearchUsersFragment extends ParcelableUsersFragment {

	private int mPage = 1;

	@Override
	public void onActivityCreated(final Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			mPage = savedInstanceState.getInt(EXTRA_PAGE, 1);
		}
		super.onActivityCreated(savedInstanceState);
	}

	@Override
    public Loader<List<ParcelableUser>> onCreateUsersLoader(final Context context, final Bundle args, boolean fromUser) {
        if (args == null) return null;
        final long account_id = args.getLong(EXTRA_ACCOUNT_ID);
        final String query = args.getString(EXTRA_QUERY);
        return new UserSearchLoader(context, account_id, query, mPage, getData(), fromUser);
	}

	@Override
    public void onLoadFinished(final Loader<List<ParcelableUser>> loader, final List<ParcelableUser> data) {
        super.onLoadFinished(loader, data);
        if (loader instanceof IExtendedLoader && ((IExtendedLoader) loader).isFromUser() && data != null) {
			mPage++;
		}
	}

	@Override
    public void onLoadMoreContents() {
        super.onLoadMoreContents();
        final Bundle loaderArgs = new Bundle(getArguments());
        loaderArgs.putBoolean(EXTRA_FROM_USER, true);
        loaderArgs.putLong(EXTRA_PAGE, mPage);
        getLoaderManager().restartLoader(0, loaderArgs, this);
    }

    @Override
	public void onSaveInstanceState(final Bundle outState) {
		outState.putInt(EXTRA_PAGE, mPage);
		super.onSaveInstanceState(outState);
	}

    @Override
    public void onDestroyView() {
        mPage = 1;
        super.onDestroyView();
    }

}