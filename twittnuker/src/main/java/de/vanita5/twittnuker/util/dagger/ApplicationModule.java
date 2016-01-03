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

package de.vanita5.twittnuker.util.dagger;

import android.content.Context;
import android.os.Looper;
import android.support.annotation.NonNull;

import com.nostra13.universalimageloader.cache.disc.DiskCache;
import com.nostra13.universalimageloader.cache.disc.impl.ext.LruDiskCache;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.nostra13.universalimageloader.core.download.ImageDownloader;
import com.nostra13.universalimageloader.utils.L;
import com.squareup.okhttp.Dns;
import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;

import org.mariotaku.restfu.http.RestHttpClient;
import de.vanita5.twittnuker.BuildConfig;
import de.vanita5.twittnuker.Constants;
import de.vanita5.twittnuker.app.TwittnukerApplication;
import de.vanita5.twittnuker.constant.SharedPreferenceConstants;
import de.vanita5.twittnuker.util.ActivityTracker;
import de.vanita5.twittnuker.util.AsyncTaskManager;
import de.vanita5.twittnuker.util.AsyncTwitterWrapper;
import de.vanita5.twittnuker.util.ExternalThemeManager;
import de.vanita5.twittnuker.util.KeyboardShortcutsHandler;
import de.vanita5.twittnuker.util.MediaLoaderWrapper;
import de.vanita5.twittnuker.util.MultiSelectManager;
import de.vanita5.twittnuker.util.NotificationManagerWrapper;
import de.vanita5.twittnuker.util.ReadStateManager;
import de.vanita5.twittnuker.util.SharedPreferencesWrapper;
import de.vanita5.twittnuker.util.TwidereMathUtils;
import de.vanita5.twittnuker.util.TwitterAPIFactory;
import de.vanita5.twittnuker.util.UserColorNameManager;
import de.vanita5.twittnuker.util.Utils;
import de.vanita5.twittnuker.util.imageloader.ReadOnlyDiskLRUNameCache;
import de.vanita5.twittnuker.util.imageloader.TwidereImageDownloader;
import de.vanita5.twittnuker.util.imageloader.URLFileNameGenerator;
import de.vanita5.twittnuker.util.net.TwidereDns;

import java.io.File;
import java.io.IOException;

import javax.inject.Singleton;

import cz.fhucho.android.util.SimpleDiskCache;
import dagger.Module;
import dagger.Provides;

import static de.vanita5.twittnuker.util.Utils.getInternalCacheDir;

@Module
public class ApplicationModule implements Constants {

    private final TwittnukerApplication application;

    public ApplicationModule(TwittnukerApplication application) {
        if (Thread.currentThread() != Looper.getMainLooper().getThread()) {
            throw new RuntimeException("Module must be created inside main thread");
        }
        this.application = application;
    }

    static ApplicationModule get(@NonNull Context context) {
        return TwittnukerApplication.getInstance(context).getApplicationModule();
    }

    @Provides
    @Singleton
    public KeyboardShortcutsHandler keyboardShortcutsHandler() {
        return new KeyboardShortcutsHandler(application);
    }

    @Provides
    @Singleton
    public ExternalThemeManager externalThemeManager(SharedPreferencesWrapper preferences) {
        return new ExternalThemeManager(application, preferences);
    }

    @Provides
    @Singleton
    public NotificationManagerWrapper notificationManagerWrapper() {
        return new NotificationManagerWrapper(application);
    }

    @Provides
    @Singleton
    public SharedPreferencesWrapper sharedPreferences() {
        return SharedPreferencesWrapper.getInstance(application, Constants.SHARED_PREFERENCES_NAME,
                Context.MODE_PRIVATE, SharedPreferenceConstants.class);
    }

    @Provides
    @Singleton
    public UserColorNameManager userColorNameManager() {
        return new UserColorNameManager(application);
    }

    @Provides
    @Singleton
    public MultiSelectManager multiSelectManager() {
        return new MultiSelectManager();
    }

    @Provides
    @Singleton
    public RestHttpClient restHttpClient() {
        return TwitterAPIFactory.getDefaultHttpClient(application);
    }

    @Provides
    @Singleton
    public Bus bus() {
        return new Bus(ThreadEnforcer.MAIN);
    }

    @Provides
    @Singleton
    public AsyncTaskManager asyncTaskManager() {
        return new AsyncTaskManager();
    }

    @Provides
    @Singleton
    public ImageLoader imageLoader(ImageDownloader downloader, SharedPreferencesWrapper preferences) {
        final ImageLoader loader = ImageLoader.getInstance();
        final ImageLoaderConfiguration.Builder cb = new ImageLoaderConfiguration.Builder(application);
        cb.threadPriority(Thread.NORM_PRIORITY - 2);
        cb.denyCacheImageMultipleSizesInMemory();
        cb.tasksProcessingOrder(QueueProcessingType.LIFO);
        // cb.memoryCache(new ImageMemoryCache(40));
        cb.diskCache(createDiskCache("images", preferences));
        cb.imageDownloader(downloader);
        L.writeDebugLogs(BuildConfig.DEBUG);
        loader.init(cb.build());
        return loader;
    }

    @Provides
    @Singleton
    public ImageDownloader imageDownloader(SharedPreferencesWrapper preferences, RestHttpClient client) {
        return new TwidereImageDownloader(application, preferences, client);
    }

    @Provides
    @Singleton
    public ActivityTracker activityTracker() {
        return new ActivityTracker();
    }

    @Provides
    @Singleton
    public AsyncTwitterWrapper asyncTwitterWrapper(UserColorNameManager userColorNameManager,
                                                   ReadStateManager readStateManager,
                                                   Bus bus, SharedPreferencesWrapper preferences,
                                                   AsyncTaskManager asyncTaskManager) {
        return new AsyncTwitterWrapper(application, userColorNameManager, readStateManager, bus,
                preferences, asyncTaskManager);
    }

    @Provides
    @Singleton
    public ReadStateManager readStateManager() {
        return new ReadStateManager(application);
    }

    @Provides
    @Singleton
    public MediaLoaderWrapper mediaLoaderWrapper(ImageLoader loader) {
        return new MediaLoaderWrapper(loader);
    }

    @Provides
    @Singleton
    public Dns dns() {
        return new TwidereDns(application);
    }

    @Provides
    @Singleton
    public SimpleDiskCache providesSimpleDiskCache() {
        final File cacheDir = new File(application.getCacheDir(), "files");
        final File reserveCacheDir = new File(application.getExternalCacheDir(), "files");
        return openCache(cacheDir, reserveCacheDir);
    }

    private SimpleDiskCache openCache(File cacheDir, File reserveCacheDir) {
        try {
            return SimpleDiskCache.open(cacheDir, BuildConfig.VERSION_CODE, 512 * 1024 * 1024);
        } catch (IOException e) {
            if (reserveCacheDir == null) throw new RuntimeException(e);
            return openCache(reserveCacheDir, null);
        }
    }


    private DiskCache createDiskCache(final String dirName, SharedPreferencesWrapper preferences) {
        final File cacheDir = Utils.getExternalCacheDir(application, dirName);
        final File fallbackCacheDir = getInternalCacheDir(application, dirName);
        final URLFileNameGenerator fileNameGenerator = new URLFileNameGenerator();
        final int cacheSize = TwidereMathUtils.clamp(preferences.getInt(KEY_CACHE_SIZE_LIMIT, 300), 100, 500);
        try {
            final int cacheMaxSizeBytes = cacheSize * 1024 * 1024;
            if (cacheDir != null)
                return new LruDiskCache(cacheDir, fallbackCacheDir, fileNameGenerator, cacheMaxSizeBytes, 0);
            return new LruDiskCache(fallbackCacheDir, null, fileNameGenerator, cacheMaxSizeBytes, 0);
        } catch (IOException e) {
            return new ReadOnlyDiskLRUNameCache(cacheDir, fallbackCacheDir, fileNameGenerator);
        }
    }

    public void reloadConnectivitySettings() {
//        imageDownloader.reloadConnectivitySettings();
//        if (restHttpClient instanceof OkHttpRestClient) {
//             OkHttpClient okHttpClient = ((OkHttpRestClient) restHttpClient).getClient();
//            TwitterAPIFactory.updateHttpClientConfiguration(application, sharedPreferences, okHttpClient);
//        }
    }

    public void onLowMemory() {
    }
}