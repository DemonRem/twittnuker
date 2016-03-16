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

package de.vanita5.twittnuker.preference;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore.Audio;
import android.support.annotation.NonNull;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.DialogPreference;
import android.support.v7.preference.PreferenceDialogFragmentCompat;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.util.AttributeSet;

import org.mariotaku.sqliteqb.library.Expression;
import de.vanita5.twittnuker.preference.iface.IDialogPreference;

import static android.text.TextUtils.isEmpty;

public class RingtonePreference extends DialogPreference implements IDialogPreference {

    private final int mRingtoneType;
    private final boolean mShowDefault;
    private final boolean mShowSilent;
    private int mSelectedItem;

    public RingtonePreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        final int[] styleable = {android.R.attr.ringtoneType, android.R.attr.showDefault,
                android.R.attr.showSilent};
        TypedArray a = context.obtainStyledAttributes(attrs, styleable);
        mRingtoneType = a.getInt(0, RingtoneManager.TYPE_RINGTONE);
        mShowDefault = a.getBoolean(1, true);
        mShowSilent = a.getBoolean(2, true);
        a.recycle();
    }

    public int getItem() {
        return mSelectedItem;
    }

    public void setItem(final int selected) {
        mSelectedItem = selected;
    }

    @Override
    public void displayDialog(PreferenceFragmentCompat fragment) {
        RingtonePreferenceDialogFragment df = RingtonePreferenceDialogFragment.newInstance(getKey());
        df.setTargetFragment(fragment, 0);
        df.show(fragment.getFragmentManager(), getKey());
    }

    public static class RingtonePreferenceDialogFragment extends PreferenceDialogFragmentCompat
            implements LoaderManager.LoaderCallbacks<Cursor> {
        private MediaPlayer mMediaPlayer;
        private SimpleCursorAdapter mAdapter;

        public static RingtonePreferenceDialogFragment newInstance(String key) {
            final RingtonePreferenceDialogFragment df = new RingtonePreferenceDialogFragment();
            final Bundle args = new Bundle();
            args.putString(ARG_KEY, key);
            df.setArguments(args);
            return df;
        }

        @Override
        public void onDialogClosed(boolean positive) {
            if (mMediaPlayer != null) {
                if (mMediaPlayer.isPlaying()) {
                    mMediaPlayer.stop();
                }
                mMediaPlayer.release();
                mMediaPlayer = null;
            }
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getContext();
            final TypedArray a = context.obtainStyledAttributes(null, android.support.v7.appcompat.R.styleable.AlertDialog,
                    android.support.v7.appcompat.R.attr.alertDialogStyle, 0);
            @SuppressLint("PrivateResource")
            final int layout = a.getResourceId(android.support.v7.appcompat.R.styleable.AlertDialog_singleChoiceItemLayout, 0);
            a.recycle();
            mAdapter = new SimpleCursorAdapter(context, layout, null, new String[]{Audio.Media.TITLE}, new int[]{android.R.id.text1}, 0);

            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setPositiveButton(android.R.string.ok, this);
            builder.setNegativeButton(android.R.string.cancel, this);
            int checkedItem = -1;
            builder.setSingleChoiceItems(mAdapter, checkedItem, new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (mMediaPlayer != null) {
                        if (mMediaPlayer.isPlaying()) {
                            mMediaPlayer.stop();
                        }
                        mMediaPlayer.release();
                    }
                    final Cursor cursor = mAdapter.getCursor();
                    if (!cursor.moveToPosition(which)) return;
                    mMediaPlayer = new MediaPlayer();
                    mMediaPlayer.setLooping(false);
                    final String ringtone = cursor.getString(cursor.getColumnIndex(Audio.Media.DATA));
                    final Uri def_uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    final Uri uri = isEmpty(ringtone) ? def_uri : Uri.parse(ringtone);
                    try {
                        mMediaPlayer.setDataSource(getContext(), uri);
                        mMediaPlayer.prepare();
                        mMediaPlayer.start();
                    } catch (final Exception e) {
                        // Ignore
                    }
                }
            });
            getLoaderManager().initLoader(0, null, this);
            return builder.create();
        }

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            final String[] cols = new String[]{Audio.Media._ID, Audio.Media.DATA, Audio.Media.TITLE};
            final String selection = Expression.equalsArgs(Audio.Media.IS_NOTIFICATION).getSQL();
            final String[] selectionArgs = {"1"};
            return new CursorLoader(getContext(), Audio.Media.INTERNAL_CONTENT_URI, cols, selection,
                    selectionArgs, Audio.Media.DEFAULT_SORT_ORDER);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            mAdapter.changeCursor(data);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            mAdapter.changeCursor(null);
        }
    }
}