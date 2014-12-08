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

package de.vanita5.twittnuker.fragment.support;

import android.content.Context;
import android.os.Bundle;

import de.vanita5.twittnuker.adapter.ParcelableStatusesAdapter;
import de.vanita5.twittnuker.model.ParcelableStatus;

import java.util.List;

public abstract class ParcelableStatusesFragment extends AbsStatusesFragment<List<ParcelableStatus>> {

	@Override
	protected ParcelableStatusesAdapter onCreateAdapter(final Context context, final boolean compact) {
		return new ParcelableStatusesAdapter(context, compact);
	}

	@Override
	public int getStatuses(long[] accountIds, final long[] maxIds, final long[] sinceIds) {
		final Bundle args = new Bundle(getArguments());
		if (maxIds != null) {
			args.putLong(EXTRA_MAX_ID, maxIds[0]);
		}
		if (sinceIds != null) {
			args.putLong(EXTRA_SINCE_ID, sinceIds[0]);
		}
		getLoaderManager().restartLoader(0, args, this);
		return -1;
	}

}
