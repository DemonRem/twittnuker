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

package de.vanita5.twittnuker.api.twitter;

import org.mariotaku.simplerestapi.http.BodyType;
import org.mariotaku.simplerestapi.method.POST;
import org.mariotaku.simplerestapi.param.Body;
import org.mariotaku.simplerestapi.param.Extra;
import org.mariotaku.simplerestapi.param.Form;

import de.vanita5.twittnuker.api.twitter.auth.OAuthToken;

/**
 * Created by mariotaku on 15/2/4.
 */
public interface TwitterOAuth {

	@Body(BodyType.FORM)
	@POST("/oauth/request_token")
	OAuthToken getRequestToken(@Form("oauth_callback") String oauthCallback);

	@Body(BodyType.FORM)
	@POST("/oauth/access_token")
	OAuthToken getAccessToken(@Form("x_auth_username") String xauthUsername,
							  @Form("x_auth_password") String xauthPassword,
							  @Form("x_auth_mode") XAuthMode xauthMode);


	@Body(BodyType.FORM)
	@POST("/oauth/access_token")
	OAuthToken getAccessToken(@Extra({"oauth_token", "oauth_token_secret"}) OAuthToken requestToken, @Form("oauth_verifier") String oauthVerifier);

	enum XAuthMode {
		CLIENT("client_auth");

		@Override
		public String toString() {
			return mode;
		}

		private final String mode;

		XAuthMode(String mode) {
			this.mode = mode;
		}
	}
}