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

package de.vanita5.twittnuker.view.holder;

import android.content.Context;
import android.view.View;

import de.vanita5.twittnuker.Constants;

public class ViewListHolder implements Constants {

	public View view;

	public ViewListHolder(final View view) {
		if (view == null) throw new NullPointerException();
		this.view = view;
	}

	public View findViewById(final int id) {
		return view.findViewById(id);
	}

	public Context getContext() {
		return view.getContext();
	}

	protected String getString(final int resId, final Object... formatArgs) {
		return getContext().getString(resId, formatArgs);
	}

}
