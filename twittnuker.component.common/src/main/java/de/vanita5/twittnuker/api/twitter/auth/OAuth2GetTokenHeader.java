/*
 * Twittnuker - Twitter client for Android
 *
 * Copyright (C) 2013-2015 vanita5 <mail@vanit.as>
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

package de.vanita5.twittnuker.api.twitter.auth;

import android.util.Base64;

import org.mariotaku.restfu.http.HeaderValue;

public class OAuth2GetTokenHeader implements HeaderValue {

    private final OAuthToken token;

    public OAuth2GetTokenHeader(OAuthToken token) {
        this.token = token;
    }

    @Override
    public String toHeaderValue() {
        return "Basic " + Base64.encodeToString((token.getOauthToken() + ":"
                + token.getOauthTokenSecret()).getBytes(), Base64.NO_WRAP);
    }
}