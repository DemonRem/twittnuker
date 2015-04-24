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

package de.vanita5.twittnuker.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Looper;
import android.webkit.WebSettings;
import android.webkit.WebView;

import java.lang.reflect.Constructor;

public class UserAgentUtils {
	// You may uncomment next line if using Android Annotations library, otherwise just be sure to run it in on the UI thread
	public static String getDefaultUserAgentString(Context context) {
		if (Looper.myLooper() != Looper.getMainLooper()) throw new IllegalStateException();
        try {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
				return NewApiWrapper.getDefaultUserAgent(context);
			}
            final Constructor<WebSettings> constructor = WebSettings.class.getDeclaredConstructor(Context.class, WebView.class);
			constructor.setAccessible(true);
			try {
				WebSettings settings = constructor.newInstance(context, null);
				return settings.getUserAgentString();
			} finally {
				constructor.setAccessible(false);
			}
		} catch (Exception e) {
			final WebView webView = new WebView(context);
			try {
				return webView.getSettings().getUserAgentString();
			} finally {
				webView.destroy();
			}
		}
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
	static class NewApiWrapper {
		static String getDefaultUserAgent(Context context) {
			return WebSettings.getDefaultUserAgent(context);
		}
	}
}