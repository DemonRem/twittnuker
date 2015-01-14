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

package de.vanita5.twittnuker.preference;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.util.AttributeSet;

import de.vanita5.twittnuker.Constants;
import de.vanita5.twittnuker.R;

public abstract class AsyncTaskPreference extends Preference implements Constants, OnPreferenceClickListener {

	private Task mTask;

	public AsyncTaskPreference(final Context context) {
		this(context, null);
	}

	public AsyncTaskPreference(final Context context, final AttributeSet attrs) {
		this(context, attrs, android.R.attr.preferenceStyle);
	}

	public AsyncTaskPreference(final Context context, final AttributeSet attrs, final int defStyle) {
		super(context, attrs, defStyle);
		setOnPreferenceClickListener(this);
	}

	@Override
	public final boolean onPreferenceClick(final Preference preference) {
		if (mTask == null || mTask.getStatus() != Status.RUNNING) {
			mTask = new Task(this);
			mTask.execute();
		}
		return true;
	}

	protected abstract void doInBackground();

	private static class Task extends AsyncTask<Void, Void, Void> {

		private final AsyncTaskPreference mPreference;
		private final Context mContext;
		private final ProgressDialog mProgress;

		public Task(final AsyncTaskPreference preference) {
			mPreference = preference;
			mContext = preference.getContext();
			mProgress = new ProgressDialog(mContext);
		}

		@Override
		protected Void doInBackground(final Void... args) {
			mPreference.doInBackground();
			return null;
		}

		@Override
		protected void onPostExecute(final Void result) {
			if (mProgress == null) return;
			if (mProgress.isShowing()) {
				mProgress.dismiss();
			}
		}

		@Override
		protected void onPreExecute() {
			if (mProgress == null) return;
			if (mProgress.isShowing()) {
				mProgress.dismiss();
			}
			mProgress.setMessage(mContext.getString(R.string.please_wait));
			mProgress.setCancelable(false);
			mProgress.show();
		}

	}

}
