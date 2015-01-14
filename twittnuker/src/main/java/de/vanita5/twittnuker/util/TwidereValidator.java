/*
 * Twittnuker - Twitter client for Android
 *
 * Copyright (C) 2013-2014 vanita5 <mail@vanita5.de>
 *
 * This program incorporates a modified version of Twidere.
 * Copyright (C) 2012-2014 Mariotaku Lee <mariotaku.lee@gmail.com>
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

package de.vanita5.twittnuker.util;

import android.content.Context;
import android.text.TextUtils;

import com.twitter.Validator;

import de.vanita5.twittnuker.Constants;

public class TwidereValidator implements Constants {

	private final int mMaxTweetLength;
	private final Validator mValidator;

	public TwidereValidator(final Context context) {
		final SharedPreferencesWrapper prefs = SharedPreferencesWrapper.getInstance(context, SHARED_PREFERENCES_NAME,
				Context.MODE_PRIVATE);
		mValidator = new Validator();
		if (prefs != null) {
			final String textLimit = prefs.getString(KEY_STATUS_TEXT_LIMIT, null);
			mMaxTweetLength = ParseUtils.parseInt(textLimit, Validator.MAX_TWEET_LENGTH);
		} else {
			mMaxTweetLength = Validator.MAX_TWEET_LENGTH;
		}
	}

	public int getMaxTweetLength() {
		return mMaxTweetLength;
	}

	public int getTweetLength(final String text) {
		return mValidator.getTweetLength(text);
	}

	public boolean isValidTweet(final String text) {
		return !TextUtils.isEmpty(text) && getTweetLength(text) <= getMaxTweetLength();
	}

}