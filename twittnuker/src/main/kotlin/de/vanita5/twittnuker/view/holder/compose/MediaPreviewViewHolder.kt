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

package de.vanita5.twittnuker.view.holder.compose

import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import com.bumptech.glide.load.resource.drawable.GlideDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import de.vanita5.twittnuker.R
import de.vanita5.twittnuker.adapter.MediaPreviewAdapter
import de.vanita5.twittnuker.model.ParcelableMedia
import de.vanita5.twittnuker.model.ParcelableMediaUpdate
import java.lang.Exception

class MediaPreviewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnLongClickListener, View.OnClickListener {

    private val imageView = itemView.findViewById(R.id.image) as ImageView
    private val videoIndicatorView = itemView.findViewById(R.id.videoIndicator)
    private val loadProgress = itemView.findViewById(R.id.loadProgress)
    private val removeView = itemView.findViewById(R.id.remove)
    private val editView = itemView.findViewById(R.id.edit)

    private val requestListener = object : RequestListener<String, GlideDrawable> {
        override fun onException(e: Exception?, model: String?, target: Target<GlideDrawable>?,
                isFirstResource: Boolean): Boolean {
            loadProgress.visibility = View.GONE
            return false
        }

        override fun onResourceReady(resource: GlideDrawable?, model: String?,
                target: Target<GlideDrawable>?, isFromMemoryCache: Boolean,
                isFirstResource: Boolean): Boolean {
            loadProgress.visibility = View.GONE
            return false
        }

    }

    var adapter: MediaPreviewAdapter? = null

    init {
        itemView.setOnLongClickListener(this)
        itemView.setOnClickListener(this)
        removeView.setOnClickListener(this)
        editView.setOnClickListener(this)
    }

    fun displayMedia(adapter: MediaPreviewAdapter, media: ParcelableMediaUpdate) {
        loadProgress.visibility = View.VISIBLE
        adapter.requestManager.load(media.uri).listener(requestListener).into(imageView)
        videoIndicatorView.visibility = if (media.type == ParcelableMedia.Type.VIDEO) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    override fun onLongClick(v: View): Boolean {
        adapter?.listener?.onStartDrag(this)
        return false
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.remove -> {
                val adapter = this.adapter ?: return
                if (layoutPosition >= 0 && layoutPosition < adapter.itemCount) {
                    adapter.listener?.onRemoveClick(layoutPosition, this)
                }
            }
            R.id.edit -> {
                adapter?.listener?.onEditClick(layoutPosition, this)
            }
        }
    }
}