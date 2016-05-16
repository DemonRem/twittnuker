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

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;

import de.vanita5.twittnuker.R;
import de.vanita5.twittnuker.model.ParcelableUser;
import de.vanita5.twittnuker.util.AsyncTwitterWrapper;

public class CreateUserBlockDialogFragment extends BaseDialogFragment implements DialogInterface.OnClickListener {

    public static final String FRAGMENT_TAG = "create_user_block";

    @Override
    public void onClick(final DialogInterface dialog, final int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                final ParcelableUser user = getUser();
                final AsyncTwitterWrapper twitter = mTwitterWrapper;
                if (user == null || twitter == null) return;
                twitter.createBlockAsync(user.account_key, user.key);
                break;
            default:
                break;
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        final FragmentActivity activity = getActivity();
        final Context context = activity;
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        final ParcelableUser user = getUser();
        if (user != null) {
            final boolean nameFirst = mPreferences.getBoolean(KEY_NAME_FIRST);
            final String displayName = mUserColorNameManager.getDisplayName(user, nameFirst);
            builder.setTitle(getString(R.string.block_user, displayName));
            builder.setMessage(getString(R.string.block_user_confirm_message, displayName));
        }
        builder.setPositiveButton(android.R.string.ok, this);
        builder.setNegativeButton(android.R.string.cancel, null);
        return builder.create();
    }

    private ParcelableUser getUser() {
        final Bundle args = getArguments();
        if (!args.containsKey(EXTRA_USER)) return null;
        return args.getParcelable(EXTRA_USER);
    }

    public static CreateUserBlockDialogFragment show(final FragmentManager fm, final ParcelableUser user) {
        final Bundle args = new Bundle();
        args.putParcelable(EXTRA_USER, user);
        final CreateUserBlockDialogFragment f = new CreateUserBlockDialogFragment();
        f.setArguments(args);
        f.show(fm, FRAGMENT_TAG);
        return f;
    }
}