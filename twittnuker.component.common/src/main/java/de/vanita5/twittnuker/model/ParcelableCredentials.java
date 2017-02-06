/*
 * Twittnuker - Twitter client for Android
 *
 * Copyright (C) 2013-2017 vanita5 <mail@vanit.as>
 *
 * This program incorporates a modified version of Twidere.
 * Copyright (C) 2012-2017 Mariotaku Lee <mariotaku.lee@gmail.com>
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

package de.vanita5.twittnuker.model;

import android.support.annotation.Nullable;

import org.mariotaku.library.objectcursor.annotation.CursorField;
import org.mariotaku.library.objectcursor.annotation.CursorObject;
import de.vanita5.twittnuker.annotation.AuthTypeInt;
import de.vanita5.twittnuker.provider.TwidereDataStore.Accounts;

@CursorObject
@Deprecated
public class ParcelableCredentials extends ParcelableAccount {


    @CursorField(Accounts.AUTH_TYPE)
    @AuthTypeInt
    public int auth_type;

    @CursorField(Accounts.CONSUMER_KEY)
    public String consumer_key;

    @CursorField(Accounts.CONSUMER_SECRET)
    public String consumer_secret;


    @CursorField(Accounts.BASIC_AUTH_USERNAME)
    public String basic_auth_username;


    @CursorField(Accounts.BASIC_AUTH_PASSWORD)
    public String basic_auth_password;


    @CursorField(Accounts.OAUTH_TOKEN)
    public String oauth_token;


    @CursorField(Accounts.OAUTH_TOKEN_SECRET)
    public String oauth_token_secret;


    @CursorField(Accounts.API_URL_FORMAT)
    @Nullable
    public String api_url_format;


    @CursorField(Accounts.SAME_OAUTH_SIGNING_URL)
    public boolean same_oauth_signing_url;


    @CursorField(Accounts.NO_VERSION_SUFFIX)
    public boolean no_version_suffix;


    @CursorField(Accounts.ACCOUNT_EXTRAS)
    public String account_extras;

    @CursorField(Accounts.SORT_POSITION)
    public String sort_position;

    ParcelableCredentials() {
    }

    @Override
    public String toString() {
        return "ParcelableCredentials{" +
                "account_extras='" + account_extras + '\'' +
                ", auth_type=" + auth_type +
                ", consumer_key='" + consumer_key + '\'' +
                ", consumer_secret='" + consumer_secret + '\'' +
                ", basic_auth_username='" + basic_auth_username + '\'' +
                ", basic_auth_password='" + basic_auth_password + '\'' +
                ", oauth_token='" + oauth_token + '\'' +
                ", oauth_token_secret='" + oauth_token_secret + '\'' +
                ", api_url_format='" + api_url_format + '\'' +
                ", same_oauth_signing_url=" + same_oauth_signing_url +
                ", no_version_suffix=" + no_version_suffix +
                ", sort_position='" + sort_position + '\'' +
                "} " + super.toString();
    }


}
