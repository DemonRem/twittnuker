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

package de.vanita5.twittnuker.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.support.v4.net.ConnectivityManagerCompat;
import android.text.TextUtils;

import com.nostra13.universalimageloader.cache.disc.DiskCache;
import com.nostra13.universalimageloader.core.ImageLoader;

import de.vanita5.twittnuker.Constants;

import java.io.File;

public class ImagePreloader implements Constants {

    public static final String LOGTAG = "ImagePreloader";

    private final Context mContext;
    private final SharedPreferences mPreferences;
    private final DiskCache mDiskCache;
    private final ImageLoader mImageLoader;
    private final ConnectivityManager mConnectivityManager;

    public ImagePreloader(final Context context, final ImageLoader loader) {
        mContext = context;
        mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        mPreferences = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        mImageLoader = loader;
        mDiskCache = loader.getDiskCache();
    }

    public File getCachedImageFile(final String url) {
        if (url == null) return null;
        final File cache = mDiskCache.get(url);
        if (ImageValidator.isValid(ImageValidator.checkImageValidity(cache)))
            return cache;
        else {
            preloadImage(url);
        }
        return null;
    }

    public void preloadImage(final String url) {
        if (TextUtils.isEmpty(url)) return;
        if (ConnectivityManagerCompat.isActiveNetworkMetered(mConnectivityManager)
                && mPreferences.getBoolean(KEY_PRELOAD_WIFI_ONLY, true)) return;
        mImageLoader.loadImage(url, null);
    }

}