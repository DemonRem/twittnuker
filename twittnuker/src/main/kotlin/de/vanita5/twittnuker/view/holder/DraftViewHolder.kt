/*
 *  Twittnuker - Twitter client for Android
 *
 *  Copyright (C) 2013-2017 vanita5 <mail@vanit.as>
 *
 *  This program incorporates a modified version of Twidere.
 *  Copyright (C) 2012-2017 Mariotaku Lee <mariotaku.lee@gmail.com>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.vanita5.twittnuker.view.holder

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.View
import com.bumptech.glide.RequestManager
import kotlinx.android.synthetic.main.list_item_draft.view.*
import org.mariotaku.ktextension.mapToArray
import de.vanita5.twittnuker.R
import de.vanita5.twittnuker.extension.model.getActionName
import de.vanita5.twittnuker.model.Draft
import de.vanita5.twittnuker.model.ParcelableMedia
import de.vanita5.twittnuker.model.draft.StatusObjectActionExtras
import de.vanita5.twittnuker.model.draft.UpdateStatusActionExtras
import de.vanita5.twittnuker.util.DataStoreUtils
import de.vanita5.twittnuker.util.Utils

class DraftViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    internal val contentView = itemView.content
    internal val textView = itemView.text
    internal val timeView = itemView.time
    internal val mediaPreviewContainer = itemView.mediaPreviewContainer

    fun display(context: Context, requestManager: RequestManager, draft: Draft) {
        val accountKeys = draft.account_keys
        val actionType: String = draft.action_type ?: Draft.Action.UPDATE_STATUS
        val actionName = draft.getActionName(context)
        var summaryText: String? = null
        when (actionType) {
            Draft.Action.SEND_DIRECT_MESSAGE, Draft.Action.SEND_DIRECT_MESSAGE_COMPAT,
            Draft.Action.UPDATE_STATUS, Draft.Action.UPDATE_STATUS_COMPAT_1,
            Draft.Action.UPDATE_STATUS_COMPAT_2, Draft.Action.REPLY, Draft.Action.QUOTE -> {
                val media = draft.media?.mapToArray(::ParcelableMedia)
                val extras = draft.action_extras as? UpdateStatusActionExtras
                if (extras != null) {
                    summaryText = extras.editingText
                }
                mediaPreviewContainer.visibility = View.VISIBLE
                mediaPreviewContainer.displayMedia(requestManager = requestManager,
                        media = media)
            }
            Draft.Action.FAVORITE, Draft.Action.RETWEET -> {
                val extras = draft.action_extras as? StatusObjectActionExtras
                if (extras != null) {
                    summaryText = extras.status.text_unescaped
                }
                mediaPreviewContainer.visibility = View.GONE
            }
            else -> {
                mediaPreviewContainer.visibility = View.GONE
            }
        }
        if (accountKeys != null) {
            contentView.drawEnd(*DataStoreUtils.getAccountColors(context, accountKeys))
        } else {
            contentView.drawEnd()
        }
        if (summaryText != null) {
            textView.text = summaryText
        } else if (draft.text.isNullOrEmpty()) {
            textView.setText(R.string.empty_content)
        } else {
            textView.text = draft.text
        }

        if (draft.timestamp > 0) {
            val timeString = Utils.formatSameDayTime(context, draft.timestamp)
            timeView.text = context.getString(R.string.action_name_saved_at_time, actionName, timeString)
        } else {
            timeView.text = actionName
        }
    }

    fun setTextSize(textSize: Float) {
        textView.textSize = textSize
        timeView.textSize = textSize * 0.75f
    }

    companion object {

        const val layoutResource = R.layout.list_item_draft
        const val layoutResourceClickable = R.layout.list_item_draft_clickable

    }

}