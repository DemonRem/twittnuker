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

package de.vanita5.twittnuker.view.holder;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import de.vanita5.twittnuker.R;
import de.vanita5.twittnuker.util.Utils;
import de.vanita5.twittnuker.view.ShortTimeView;

public class DirectMessageEntryViewHolder extends ViewListHolder {

	public final ImageView profile_image;
    public final TextView name, screen_name, text;
	public final ShortTimeView time;
	private float text_size;
	private boolean account_color_enabled;
	private final boolean is_rtl;

	public DirectMessageEntryViewHolder(final View view) {
		super(view);
		final Context context = view.getContext();
		profile_image = (ImageView) findViewById(R.id.profile_image);
		name = (TextView) findViewById(R.id.name);
        screen_name = (TextView) findViewById(R.id.screen_name);
		text = (TextView) findViewById(R.id.text);
		time = (ShortTimeView) findViewById(R.id.time);
		is_rtl = Utils.isRTL(context);
	}

	public void setAccountColor(final int color) {
//        content.drawEnd(account_color_enabled ? color : Color.TRANSPARENT);
	}

	public void setAccountColorEnabled(final boolean enabled) {
		if (account_color_enabled == enabled) return;
		account_color_enabled = enabled;
		if (!account_color_enabled) {
//            content.drawEnd(Color.TRANSPARENT);
		}
	}

	public void setIsOutgoing(final boolean is_outgoing) {
		if (is_rtl) {
			time.setCompoundDrawablesWithIntrinsicBounds(is_outgoing ? R.drawable.ic_indicator_sent : 0, 0, 0, 0);
		} else {
			time.setCompoundDrawablesWithIntrinsicBounds(0, 0, is_outgoing ? R.drawable.ic_indicator_sent : 0, 0);
		}
	}

	public void setTextSize(final float text_size) {
		if (this.text_size == text_size) return;
		this.text_size = text_size;
        text.setTextSize(text_size);
        name.setTextSize(text_size);
	}

	public void setUserColor(final int color) {
//        content.drawStart(color);
	}
}
