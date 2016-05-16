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

package de.vanita5.twittnuker.loader;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import de.vanita5.twittnuker.library.MicroBlog;
import de.vanita5.twittnuker.library.MicroBlogException;
import de.vanita5.twittnuker.library.twitter.model.User;
import de.vanita5.twittnuker.model.ListResponse;
import de.vanita5.twittnuker.model.ParcelableCredentials;
import de.vanita5.twittnuker.model.ParcelableUser;
import de.vanita5.twittnuker.model.UserKey;
import de.vanita5.twittnuker.model.util.ParcelableCredentialsUtils;
import de.vanita5.twittnuker.model.util.ParcelableUserUtils;
import de.vanita5.twittnuker.util.MicroBlogAPIFactory;

import java.util.Collections;
import java.util.List;

public abstract class TwitterAPIUsersLoader extends ParcelableUsersLoader {

    @Nullable
    private final UserKey mAccountKey;

    public TwitterAPIUsersLoader(final Context context, @Nullable final UserKey accountKey,
                                 final List<ParcelableUser> data, boolean fromUser) {
        super(context, data, fromUser);
        mAccountKey = accountKey;
    }

    @Override
    public List<ParcelableUser> loadInBackground() {
        if (mAccountKey == null) {
            return ListResponse.getListInstance(new MicroBlogException("No Account"));
        }
        final ParcelableCredentials credentials = ParcelableCredentialsUtils.getCredentials(getContext(),
                mAccountKey);
        if (credentials == null) {
            return ListResponse.getListInstance(new MicroBlogException("No Account"));
        }
        final MicroBlog twitter = MicroBlogAPIFactory.getTwitterInstance(getContext(), credentials, true,
                true);
        if (twitter == null)
            return ListResponse.getListInstance(new MicroBlogException("No Account"));
        final List<ParcelableUser> data = getData();
        final List<User> users;
        try {
            users = getUsers(twitter, credentials);
        } catch (final MicroBlogException e) {
            Log.w(LOGTAG, e);
            return ListResponse.getListInstance(data);
        }
        int pos = data.size();
        for (final User user : users) {
            if (hasId(user.getId())) {
                continue;
            }
            data.add(ParcelableUserUtils.fromUser(user, mAccountKey, pos));
            pos++;
        }
        Collections.sort(data);
        return ListResponse.getListInstance(data);
    }

    @Nullable
    public final UserKey getAccountId() {
        return mAccountKey;
    }

    @NonNull
    protected abstract List<User> getUsers(@NonNull MicroBlog twitter,
                                           @NonNull ParcelableCredentials credentials) throws MicroBlogException;
}