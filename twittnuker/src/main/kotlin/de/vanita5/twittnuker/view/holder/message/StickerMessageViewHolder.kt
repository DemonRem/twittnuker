/*
 * Twittnuker - Twitter client for Android
 *
 * Copyright (C) 2013-2017 vanita5 <mail@vanit.as>
 *
 * This program incorporates a modified version of Twidere.
 * Copyright (C) 2012-2017 Mariotaku Lee <mariotaku.lee@gmail.com>
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

package de.vanita5.twittnuker.view.holder.message

import android.view.View
import android.widget.RelativeLayout
import kotlinx.android.synthetic.main.list_item_message_conversation_sticker.view.*
import de.vanita5.twittnuker.R
import de.vanita5.twittnuker.adapter.MessagesConversationAdapter
import de.vanita5.twittnuker.model.ParcelableMessage
import de.vanita5.twittnuker.model.message.StickerExtras
import de.vanita5.twittnuker.view.FixedTextView
import de.vanita5.twittnuker.view.ProfileImageView


class StickerMessageViewHolder(itemView: View, adapter: MessagesConversationAdapter) : AbsMessageViewHolder(itemView, adapter) {

    override val date: FixedTextView by lazy { itemView.date }
    override val messageContent: RelativeLayout by lazy { itemView.messageContent }
    override val profileImage: ProfileImageView by lazy { itemView.profileImage }
    override val nameTime: FixedTextView by lazy { itemView.nameTime }

    private val stickerIcon by lazy { itemView.stickerIcon }

    override fun display(message: ParcelableMessage, showDate: Boolean) {
        super.display(message, showDate)
        val extras = message.extras as StickerExtras
        adapter.mediaLoader.displayStickerImage(stickerIcon, extras.url)
        stickerIcon.contentDescription = extras.displayName
    }

    companion object {
        const val layoutResource = R.layout.list_item_message_conversation_sticker
    }
}