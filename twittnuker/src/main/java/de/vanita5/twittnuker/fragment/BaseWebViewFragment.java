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

package de.vanita5.twittnuker.fragment;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebViewFragment;

import de.vanita5.twittnuker.Constants;
import de.vanita5.twittnuker.util.support.WebSettingsSupport;
import de.vanita5.twittnuker.util.webkit.DefaultWebViewClient;

@SuppressLint("SetJavaScriptEnabled")
public class BaseWebViewFragment extends WebViewFragment implements Constants {

	@Override
	public void onActivityCreated(final Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		final WebView view = getWebView();
        view.setWebViewClient(createWebViewClient());
		final WebSettings settings = view.getSettings();
		settings.setBuiltInZoomControls(true);
		settings.setJavaScriptEnabled(true);
		WebSettingsSupport.setAllowUniversalAccessFromFileURLs(settings, true);
    }


    protected WebViewClient createWebViewClient() {
        return new DefaultWebViewClient(getActivity());
	}
}
