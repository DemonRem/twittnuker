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
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.Loader;

import de.vanita5.twittnuker.fragment.ParcelableUsersFragment;
import de.vanita5.twittnuker.loader.IntentExtrasUsersLoader;
import de.vanita5.twittnuker.model.ParcelableUser;

import java.util.List;

public class UsersListFragment extends ParcelableUsersFragment {

    @Override
    protected boolean hasMoreData(List<ParcelableUser> data) {
        return false;
    }

    @Override
    public boolean isRefreshing() {
        if (getContext() == null || isDetached()) return false;
        return false;
    }

    @Override
    public void onLoadFinished(Loader<List<ParcelableUser>> loader, List<ParcelableUser> data) {
        super.onLoadFinished(loader, data);
        setRefreshEnabled(false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setRefreshEnabled(false);
    }

    @Override
    public Loader<List<ParcelableUser>> onCreateUsersLoader(final Context context, @NonNull final Bundle args, boolean fromUser) {
        if (args.containsKey(EXTRA_USERS))
            return new IntentExtrasUsersLoader(context, args, getData(), fromUser);
        return null;
    }

}