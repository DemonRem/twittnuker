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

package de.vanita5.twittnuker.adapter

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import org.apache.commons.lang3.time.DateUtils
import org.mariotaku.kpreferences.get
import de.vanita5.twittnuker.adapter.iface.IItemCountsAdapter
import de.vanita5.twittnuker.annotation.PreviewStyle
import de.vanita5.twittnuker.constant.linkHighlightOptionKey
import de.vanita5.twittnuker.constant.mediaPreviewStyleKey
import de.vanita5.twittnuker.constant.nameFirstKey
import de.vanita5.twittnuker.extension.model.timestamp
import de.vanita5.twittnuker.model.*
import de.vanita5.twittnuker.model.ParcelableMessage.MessageType
import de.vanita5.twittnuker.util.MediaLoadingHandler
import de.vanita5.twittnuker.util.TwidereLinkify
import de.vanita5.twittnuker.view.holder.message.AbsMessageViewHolder
import de.vanita5.twittnuker.view.holder.message.MessageViewHolder
import de.vanita5.twittnuker.view.holder.message.NoticeSummaryEventViewHolder
import de.vanita5.twittnuker.view.holder.message.StickerMessageViewHolder
import java.util.*

class MessagesConversationAdapter(context: Context) : LoadMoreSupportAdapter<RecyclerView.ViewHolder>(context),
        IItemCountsAdapter {
    private val calendars = Pair(Calendar.getInstance(), Calendar.getInstance())
    override val itemCounts: ItemCounts = ItemCounts(1)

    @PreviewStyle
    val mediaPreviewStyle: Int = preferences[mediaPreviewStyleKey]
    val linkHighlightingStyle: Int = preferences[linkHighlightOptionKey]
    val nameFirst: Boolean = preferences[nameFirstKey]
    val linkify: TwidereLinkify = TwidereLinkify(null)
    val mediaLoadingHandler: MediaLoadingHandler = MediaLoadingHandler()

    var messages: List<ParcelableMessage>? = null
        private set
    var conversation: ParcelableMessageConversation? = null
        private set

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        when (viewType) {
            ITEM_TYPE_TEXT_MESSAGE -> {
                val view = inflater.inflate(MessageViewHolder.layoutResource, parent, false)
                val holder = MessageViewHolder(view, this)
                holder.setup()
                return holder
            }
            ITEM_TYPE_STICKER_MESSAGE -> {
                val view = inflater.inflate(StickerMessageViewHolder.layoutResource, parent, false)
                val holder = StickerMessageViewHolder(view, this)
                holder.setup()
                return holder
            }
            ITEM_TYPE_NOTICE_MESSAGE -> {
                val view = inflater.inflate(NoticeSummaryEventViewHolder.layoutResource, parent, false)
                val holder = NoticeSummaryEventViewHolder(view, this)
                holder.setup()
                return holder
            }
        }
        throw UnsupportedOperationException()
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            ITEM_TYPE_TEXT_MESSAGE, ITEM_TYPE_STICKER_MESSAGE, ITEM_TYPE_NOTICE_MESSAGE -> {
                val message = getMessage(position)!!
                // Display date for oldest item
                var showDate = true
                // ... or if current message is > 1 day newer than previous one
                if (position < itemCounts.getItemStartPosition(ITEM_START_MESSAGE)
                        + itemCounts[ITEM_START_MESSAGE] - 1) {
                    calendars.first.timeInMillis = getMessage(position + 1)!!.timestamp
                    calendars.second.timeInMillis = message.timestamp
                    showDate = !DateUtils.isSameDay(calendars.first, calendars.second)
                }
                (holder as AbsMessageViewHolder).display(message, showDate)
            }
        }

    }

    override fun getItemCount(): Int {
        itemCounts[ITEM_START_MESSAGE] = messages?.size ?: 0
        return itemCounts.itemCount
    }

    override fun getItemViewType(position: Int): Int {
        when (itemCounts.getItemCountIndex(position)) {
            ITEM_START_MESSAGE -> {
                when (getMessage(position)!!.message_type) {
                    MessageType.STICKER -> {
                        return ITEM_TYPE_STICKER_MESSAGE
                    }
                    MessageType.CONVERSATION_CREATE, MessageType.JOIN_CONVERSATION,
                    MessageType.PARTICIPANTS_LEAVE, MessageType.PARTICIPANTS_JOIN,
                    MessageType.CONVERSATION_NAME_UPDATE, MessageType.CONVERSATION_AVATAR_UPDATE -> {
                        return ITEM_TYPE_NOTICE_MESSAGE
                    }
                    else -> return ITEM_TYPE_TEXT_MESSAGE
                }
            }
        }
        throw UnsupportedOperationException()
    }

    fun getMessage(position: Int): ParcelableMessage? {
        return messages?.get(position - itemCounts.getItemStartPosition(0))
    }

    fun findUser(key: UserKey): ParcelableUser? {
        return conversation?.participants?.firstOrNull { it.key == key }
    }

    fun setData(conversation: ParcelableMessageConversation?, messages: List<ParcelableMessage>?) {
        this.conversation = conversation
        this.messages = messages
        notifyDataSetChanged()
    }

    companion object {
        private const val ITEM_START_MESSAGE = 0

        const val ITEM_TYPE_TEXT_MESSAGE = 1
        const val ITEM_TYPE_STICKER_MESSAGE = 2
        const val ITEM_TYPE_NOTICE_MESSAGE = 3
    }


}
