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

package de.vanita5.twittnuker.task;

import android.content.Context;
import android.net.Uri;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

import de.vanita5.twittnuker.activity.iface.IExtendedActivity;
import de.vanita5.twittnuker.fragment.SupportProgressDialogFragment;

import java.io.File;

public abstract class ProgressSaveFileTask extends SaveFileTask {
    private static final String PROGRESS_FRAGMENT_TAG = "progress";

    public ProgressSaveFileTask(Context context, Uri source, File destination, FileInfoCallback getMimeType) {
        super(context, source, destination, getMimeType);
    }

    protected void showProgress() {
        final Context context = getContext();
        if (context == null) return;
        ((IExtendedActivity) context).executeAfterFragmentResumed(new IExtendedActivity.Action() {
            @Override
            public void execute(IExtendedActivity activity) {
                final DialogFragment fragment = new SupportProgressDialogFragment();
                fragment.setCancelable(false);
                fragment.show(((FragmentActivity) activity).getSupportFragmentManager(), PROGRESS_FRAGMENT_TAG);
            }
        });
    }

    protected void dismissProgress() {
        final Context context = getContext();
        if (context == null) return;
        ((IExtendedActivity) context).executeAfterFragmentResumed(new IExtendedActivity.Action() {
            @Override
            public void execute(IExtendedActivity activity) {
                final FragmentManager fm = ((FragmentActivity) activity).getSupportFragmentManager();
                final DialogFragment fragment = (DialogFragment) fm.findFragmentByTag(PROGRESS_FRAGMENT_TAG);
                if (fragment != null) {
                    fragment.dismiss();
                }
            }
        });
    }
}