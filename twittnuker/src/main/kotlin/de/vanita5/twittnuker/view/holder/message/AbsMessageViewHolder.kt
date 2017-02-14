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

import android.os.Build
import android.support.v4.view.GravityCompat
import android.support.v7.widget.RecyclerView
import android.text.format.DateUtils
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import de.vanita5.twittnuker.adapter.MessagesConversationAdapter
import de.vanita5.twittnuker.extension.model.timestamp
import de.vanita5.twittnuker.model.ParcelableMessage


abstract class AbsMessageViewHolder(itemView: View, val adapter: MessagesConversationAdapter) : RecyclerView.ViewHolder(itemView) {

    protected abstract val date: TextView
    protected abstract val messageContent: View

    open fun display(message: ParcelableMessage, showDate: Boolean) {
        setMessageContentGravity(messageContent, message.is_outgoing)
        if (showDate) {
            date.visibility = View.VISIBLE
            date.text = DateUtils.getRelativeTimeSpanString(message.timestamp, System.currentTimeMillis(),
                    DateUtils.DAY_IN_MILLIS, DateUtils.FORMAT_SHOW_DATE)
        } else {
            date.visibility = View.GONE
        }
    }

    open fun setMessageContentGravity(view: View, outgoing: Boolean) {
        val lp = view.layoutParams
        when (lp) {
            is FrameLayout.LayoutParams -> {
                lp.gravity = if (outgoing) GravityCompat.END else GravityCompat.START
            }
            is LinearLayout.LayoutParams -> {
                lp.gravity = if (outgoing) GravityCompat.END else GravityCompat.START
            }
            is RelativeLayout.LayoutParams -> {
                val endRule = if (outgoing) 1 else 0
                val startRule = if (outgoing) 0 else 1
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    lp.addRule(RelativeLayout.ALIGN_PARENT_START, startRule)
                    lp.addRule(RelativeLayout.ALIGN_PARENT_END, endRule)
                } else {
                    lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT, startRule)
                    lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, endRule)
                }
            }
        }
    }

}