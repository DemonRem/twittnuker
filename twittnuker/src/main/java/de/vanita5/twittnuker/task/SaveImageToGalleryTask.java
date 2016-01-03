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

package de.vanita5.twittnuker.task;

import android.app.Activity;
import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.widget.Toast;

import de.vanita5.twittnuker.R;
import de.vanita5.twittnuker.provider.CacheProvider;

import java.io.File;

public class SaveImageToGalleryTask extends ProgressSaveFileTask {

    public SaveImageToGalleryTask(@NonNull Activity activity, @NonNull Uri source, @NonNull File destination) {
        super(activity, source, destination, new CacheProvider.CacheFileTypeCallback(activity));
    }

    public static SaveFileTask create(final Activity activity, final Uri source) {
        final File pubDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        final File saveDir = new File(pubDir, "Twittnuker");
        return new SaveImageToGalleryTask(activity, source, saveDir);
    }

    protected void onFileSaved(File savedFile, String mimeType) {
        final Context context = getContext();
        if (context == null) return;
        if (savedFile != null && savedFile.exists()) {
            MediaScannerConnection.scanFile(context, new String[]{savedFile.getPath()},
                    new String[]{mimeType}, null);
            Toast.makeText(context, R.string.saved_to_gallery, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, R.string.error_occurred, Toast.LENGTH_SHORT).show();
        }
    }

}