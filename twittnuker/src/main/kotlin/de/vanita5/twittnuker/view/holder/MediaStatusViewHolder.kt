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

package de.vanita5.twittnuker.view.holder

import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import com.commonsware.cwac.layouts.AspectLockedFrameLayout
import kotlinx.android.synthetic.main.adapter_item_media_status.view.*
import de.vanita5.twittnuker.R
import de.vanita5.twittnuker.adapter.iface.IStatusesAdapter
import de.vanita5.twittnuker.extension.loadProfileImage
import de.vanita5.twittnuker.graphic.like.LikeAnimationDrawable
import de.vanita5.twittnuker.model.ParcelableMedia
import de.vanita5.twittnuker.model.ParcelableStatus
import de.vanita5.twittnuker.model.UserKey
import de.vanita5.twittnuker.model.util.ParcelableMediaUtils
import de.vanita5.twittnuker.view.holder.iface.IStatusViewHolder

class MediaStatusViewHolder(private val adapter: IStatusesAdapter<*>, itemView: View) : RecyclerView.ViewHolder(itemView), IStatusViewHolder, View.OnClickListener, View.OnLongClickListener {
    private val aspectRatioSource = SimpleAspectRatioSource().apply {
        setSize(100, 100)
    }

    private val mediaImageContainer = itemView.mediaImageContainer
    private val mediaImageView = itemView.mediaImage
    override val profileImageView: ImageView = itemView.mediaProfileImage
    private val mediaTextView = itemView.mediaText
    private var listener: IStatusViewHolder.StatusClickListener? = null

    override val profileTypeView: ImageView?
        get() = null


    init {
        mediaImageContainer.setAspectRatioSource(aspectRatioSource)
    }

    override fun displayStatus(status: ParcelableStatus, displayInReplyTo: Boolean,
            displayPinned: Boolean) {
        val media = status.media ?: return
        if (media.isEmpty()) return
        val firstMedia = media[0]

        var displayEnd = -1
        if (status.extras.display_text_range != null) {
            displayEnd = status.extras.display_text_range!![1]
        }

        if (displayEnd >= 0) {
            mediaTextView.text = status.text_unescaped.subSequence(0, displayEnd)
        } else {
            mediaTextView.text = status.text_unescaped
        }

        if (firstMedia.width > 0 && firstMedia.height > 0) {
            aspectRatioSource.setSize(firstMedia.width, firstMedia.height)
        } else {
            aspectRatioSource.setSize(100, 100)
        }
        mediaImageContainer.tag = firstMedia
        mediaImageContainer.requestLayout()

        mediaImageView.setHasPlayIcon(ParcelableMediaUtils.hasPlayIcon(firstMedia.type))
        val context = itemView.context
        adapter.requestManager.loadProfileImage(context, status).into(profileImageView)
        // TODO image loaded event and credentials
        adapter.requestManager.load(firstMedia.preview_url).into(mediaImageView)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.itemContent -> {
                listener?.onStatusClick(this, layoutPosition)
            }
        }
    }

    override fun onLongClick(v: View): Boolean {
        return false
    }

    override fun onMediaClick(view: View, media: ParcelableMedia, accountKey: UserKey?, id: Long) {
    }

    override fun setStatusClickListener(listener: IStatusViewHolder.StatusClickListener?) {
        this.listener = listener
        itemView.findViewById(R.id.itemContent).setOnClickListener(this)
    }

    override fun setTextSize(textSize: Float) {

    }

    override fun playLikeAnimation(listener: LikeAnimationDrawable.OnLikedListener) {

    }

    fun setOnClickListeners() {
        setStatusClickListener(adapter.statusClickListener)
    }

    fun setupViewOptions() {
        setTextSize(adapter.textSize)
    }


    private class SimpleAspectRatioSource : AspectLockedFrameLayout.AspectRatioSource {
        private var width: Int = 0
        private var height: Int = 0

        override fun getWidth(): Int {
            return width
        }

        override fun getHeight(): Int {
            return height
        }

        fun setSize(width: Int, height: Int) {
            this.width = width
            this.height = height
        }

    }
}