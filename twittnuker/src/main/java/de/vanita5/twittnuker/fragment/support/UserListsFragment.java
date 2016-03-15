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
import android.support.v4.app.DialogFragment;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.squareup.otto.Subscribe;

import de.vanita5.twittnuker.R;
import de.vanita5.twittnuker.adapter.ParcelableUserListsAdapter;
import de.vanita5.twittnuker.loader.support.UserListsLoader;
import de.vanita5.twittnuker.model.UserKey;
import de.vanita5.twittnuker.model.ParcelableUserList;
import de.vanita5.twittnuker.model.message.UserListDestroyedEvent;
import de.vanita5.twittnuker.util.MenuUtils;
import de.vanita5.twittnuker.util.Utils;

import java.util.List;

public class UserListsFragment extends ParcelableUserListsFragment {

    @Override
    public Loader<List<ParcelableUserList>> onCreateUserListsLoader(final Context context,
                                                                    final Bundle args, final boolean fromUser) {
        final UserKey accountKey = args.getParcelable(EXTRA_ACCOUNT_KEY);
        final String userId = args.getString(EXTRA_USER_ID);
        final String screenName = args.getString(EXTRA_SCREEN_NAME);
        return new UserListsLoader(getActivity(), accountKey, userId, screenName, true, getData());
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        inflater.inflate(R.menu.menu_user_lists_owned, menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.new_user_list: {
                final DialogFragment f = new CreateUserListDialogFragment();
                final Bundle args = new Bundle();
                args.putParcelable(EXTRA_ACCOUNT_KEY, getAccountKey());
                f.setArguments(args);
                f.show(getFragmentManager(), null);
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPrepareOptionsMenu(final Menu menu) {
        final MenuItem item = menu.findItem(R.id.new_user_list);
        final UserKey accountId = getAccountKey();
        if (accountId == null || item == null) return;
        final String userId = getUserId();
        if (TextUtils.equals(accountId.getId(), userId)) {
            MenuUtils.setMenuItemAvailability(menu, R.id.new_user_list, true);
        } else {
            MenuUtils.setMenuItemAvailability(menu, R.id.new_user_list, Utils.isMyAccount(getActivity(), getScreenName()));
        }
    }

    private String getScreenName() {
        return getArguments().getString(EXTRA_SCREEN_NAME);
    }

    private String getUserId() {
        return getArguments().getString(EXTRA_USER_ID);
    }

    @Override
    public void onStart() {
        super.onStart();
        mBus.register(this);
    }

    @Override
    public void onStop() {
        mBus.unregister(this);
        super.onStop();
    }

    @Subscribe
    public void onUserListDestroyed(UserListDestroyedEvent event) {
        removeUserList(event.userList.id);
    }

    private void removeUserList(final long id) {
        final ParcelableUserListsAdapter adapter = getAdapter();
//        final int listsIdx = adapter.findItemPosition(id);
//        if (listsIdx >= 0) {
//            adapter.removeAt(listsIdx);
//        }
    }

}