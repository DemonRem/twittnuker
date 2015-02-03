/*
 * Twittnuker - Twitter client for Android
 *
 * Copyright (C) 2013-2015 vanita5 <mail@vanita5.de>
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
import android.database.Cursor;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import de.vanita5.twittnuker.R;
import de.vanita5.twittnuker.adapter.MessageEntriesAdapter;
import de.vanita5.twittnuker.provider.TwidereDataStore.DirectMessages.ConversationEntries;
import de.vanita5.twittnuker.util.ImageLoaderWrapper;
import de.vanita5.twittnuker.view.ShortTimeView;

import static de.vanita5.twittnuker.util.HtmlEscapeHelper.toPlainText;

public class MessageEntryViewHolder extends ViewHolder implements OnClickListener {

	public final ImageView profileImageView;
	public final TextView nameView, screenNameView, textView;
	public final ShortTimeView timeView;
    private final MessageEntriesAdapter adapter;
	private float text_size;
	private boolean account_color_enabled;

	public MessageEntryViewHolder(final MessageEntriesAdapter adapter, final View itemView) {
		super(itemView);
		this.adapter = adapter;
		final Context context = itemView.getContext();
		profileImageView = (ImageView) itemView.findViewById(R.id.profile_image);
		nameView = (TextView) itemView.findViewById(R.id.name);
		screenNameView = (TextView) itemView.findViewById(R.id.screen_name);
		textView = (TextView) itemView.findViewById(R.id.text);
		timeView = (ShortTimeView) itemView.findViewById(R.id.time);

		itemView.setOnClickListener(this);
	}

	public void displayMessage(Cursor cursor) {
		final Context context = adapter.getContext();
		final ImageLoaderWrapper loader = adapter.getImageLoader();

		final long accountId = cursor.getLong(ConversationEntries.IDX_ACCOUNT_ID);
		final long conversationId = cursor.getLong(ConversationEntries.IDX_CONVERSATION_ID);
		final long timestamp = cursor.getLong(ConversationEntries.IDX_MESSAGE_TIMESTAMP);
		final boolean isOutgoing = cursor.getInt(ConversationEntries.IDX_IS_OUTGOING) == 1;

		final String name = cursor.getString(ConversationEntries.IDX_NAME);
		final String screenName = cursor.getString(ConversationEntries.IDX_SCREEN_NAME);

		nameView.setText(name);
		screenNameView.setText("@" + screenName);
		textView.setText(toPlainText(cursor.getString(ConversationEntries.IDX_TEXT)));
		timeView.setTime(timestamp);
		setIsOutgoing(isOutgoing);

		final String profileImage = cursor.getString(ConversationEntries.IDX_PROFILE_IMAGE_URL);
		loader.displayProfileImage(profileImageView, profileImage);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.profile_image: {
				break;
			}
			default: {
				if (v == itemView) {
					adapter.onMessageClick(getPosition());
				}
				break;
			}
		}
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
		timeView.setCompoundDrawablesWithIntrinsicBounds(0, 0, is_outgoing ? R.drawable.ic_indicator_sent : 0, 0);
	}

	public void setTextSize(final float text_size) {
		if (this.text_size == text_size) return;
		this.text_size = text_size;
		textView.setTextSize(text_size);
		nameView.setTextSize(text_size);
	}

	public void setUserColor(final int color) {
//        content.drawStart(color);
	}
}