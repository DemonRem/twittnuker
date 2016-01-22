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

import java.io.File;

public interface VideoLoadingListener {

    void onVideoLoadingCancelled(String uri, VideoLoadingListener listener);

    void onVideoLoadingComplete(String uri, VideoLoadingListener listener, File file);

    void onVideoLoadingFailed(String uri, VideoLoadingListener listener, Exception e);

    void onVideoLoadingProgressUpdate(String uri, VideoLoadingListener listener, int current, int total);

    void onVideoLoadingStarted(String uri, VideoLoadingListener listener);
}