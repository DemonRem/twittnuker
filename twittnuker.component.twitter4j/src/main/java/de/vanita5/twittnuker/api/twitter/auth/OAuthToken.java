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

package de.vanita5.twittnuker.api.twitter.auth;

import org.mariotaku.simplerestapi.Utils;
import org.mariotaku.simplerestapi.http.KeyValuePair;
import org.mariotaku.simplerestapi.http.ValueMap;

import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by mariotaku on 15/2/4.
 */
public class OAuthToken implements ValueMap {

    private String screenName;
    private long userId;

    private String oauthToken, oauthTokenSecret;

    public String getScreenName() {
        return screenName;
	}

    public long getUserId() {
        return userId;
    }

	public String getOauthTokenSecret() {
		return oauthTokenSecret;
	}

	public String getOauthToken() {
		return oauthToken;
	}

	public OAuthToken(String oauthToken, String oauthTokenSecret) {
		this.oauthToken = oauthToken;
		this.oauthTokenSecret = oauthTokenSecret;
	}

	public OAuthToken(String body, Charset charset) throws ParseException {
		List<KeyValuePair> params = new ArrayList<>();
		Utils.parseGetParameters(body, params, charset.name());
		for (KeyValuePair param : params) {
			switch (param.getKey()) {
				case "oauth_token": {
					oauthToken = param.getValue();
					break;
				}
				case "oauth_token_secret": {
					oauthTokenSecret = param.getValue();
					break;
				}
                case "user_id": {
                    userId = Long.parseLong(param.getValue());
                    break;
                }
                case "screen_name": {
                    screenName = param.getValue();
                    break;
                }
			}
		}
		if (oauthToken == null || oauthTokenSecret == null) {
			throw new ParseException("Unable to parse request token", -1);
		}
	}

	@Override
	public boolean has(String key) {
		return "oauth_token".equals(key) || "oauth_token_secret".equals(key);
	}

    @Override
    public String toString() {
        return "OAuthToken{" +
                "screenName='" + screenName + '\'' +
                ", userId=" + userId +
                ", oauthToken='" + oauthToken + '\'' +
                ", oauthTokenSecret='" + oauthTokenSecret + '\'' +
                '}';
    }

	@Override
	public String get(String key) {
		if ("oauth_token".equals(key)) {
			return oauthToken;
		} else if ("oauth_token_secret".equals(key)) {
			return oauthTokenSecret;
		}
		return null;
	}
}