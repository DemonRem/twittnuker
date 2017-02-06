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

import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.ViewHolder
import android.text.*
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnLongClickListener
import android.widget.ImageView
import kotlinx.android.synthetic.main.list_item_status.view.*
import org.mariotaku.ktextension.applyFontFamily
import de.vanita5.twittnuker.Constants
import de.vanita5.twittnuker.R
import de.vanita5.twittnuker.TwittnukerConstants.USER_TYPE_FANFOU_COM
import de.vanita5.twittnuker.adapter.iface.IStatusesAdapter
import de.vanita5.twittnuker.constant.SharedPreferenceConstants.VALUE_LINK_HIGHLIGHT_OPTION_CODE_NONE
import de.vanita5.twittnuker.graphic.like.LikeAnimationDrawable
import de.vanita5.twittnuker.model.ParcelableLocation
import de.vanita5.twittnuker.model.ParcelableMedia
import de.vanita5.twittnuker.model.ParcelableStatus
import de.vanita5.twittnuker.model.UserKey
import de.vanita5.twittnuker.model.util.ParcelableLocationUtils
import de.vanita5.twittnuker.model.util.ParcelableStatusUtils
import de.vanita5.twittnuker.util.*
import de.vanita5.twittnuker.util.HtmlEscapeHelper.toPlainText
import de.vanita5.twittnuker.util.Utils.getUserTypeIconRes
import de.vanita5.twittnuker.view.ProfileImageView
import de.vanita5.twittnuker.view.holder.iface.IStatusViewHolder
import java.lang.ref.WeakReference

class StatusViewHolder(private val adapter: IStatusesAdapter<*>, itemView: View) : ViewHolder(itemView), Constants, IStatusViewHolder {

    override val profileImageView: ProfileImageView by lazy { itemView.profileImage }
    override val profileTypeView: ImageView by lazy { itemView.profileType }

    private val itemContent by lazy { itemView.itemContent }
    private val mediaPreview by lazy { itemView.mediaPreview }
    private val statusContentUpperSpace by lazy { itemView.statusContentUpperSpace }
    private val textView by lazy { itemView.text }
    private val nameView by lazy { itemView.name }
    private val itemMenu by lazy { itemView.itemMenu }
    private val statusInfoLabel by lazy { itemView.statusInfoLabel }
    private val statusInfoIcon by lazy { itemView.statusInfoIcon }
    private val extraTypeView by lazy { itemView.extraType }
    private val quotedNameView by lazy { itemView.quotedName }
    private val timeView by lazy { itemView.time }
    private val replyCountView by lazy { itemView.replyCount }
    private val retweetCountView by lazy { itemView.retweetCount }
    private val quotedView by lazy { itemView.quotedView }
    private val quotedTextView by lazy { itemView.quotedText }
    private val actionButtons by lazy { itemView.actionButtons }
    private val mediaLabel by lazy { itemView.mediaLabel }
    private val quotedMediaLabel by lazy { itemView.quotedMediaLabel }
    private val statusContentLowerSpace by lazy { itemView.statusContentLowerSpace }
    private val quotedMediaPreview by lazy { itemView.quotedMediaPreview }
    private val favoriteIcon by lazy { itemView.favoriteIcon }
    private val retweetIcon by lazy { itemView.retweetIcon }
    private val favoriteCountView by lazy { itemView.favoriteCount }
    private val mediaLabelTextView by lazy { itemView.mediaLabelText }
    private val quotedMediaLabelTextView by lazy { itemView.quotedMediaLabelText }
    private val replyButton by lazy { itemView.reply }
    private val retweetButton by lazy { itemView.retweet }
    private val favoriteButton by lazy { itemView.favorite }

    private val eventListener: EventListener

    private var statusClickListener: IStatusViewHolder.StatusClickListener? = null


    init {
        this.eventListener = EventListener(this)

        if (adapter.mediaPreviewEnabled) {
            View.inflate(mediaPreview.context, R.layout.layout_card_media_preview,
                    itemView.mediaPreview)
            View.inflate(quotedMediaPreview.context, R.layout.layout_card_media_preview,
                    itemView.quotedMediaPreview)
        }

        nameView.applyFontFamily(adapter.lightFont)
        timeView.applyFontFamily(adapter.lightFont)
        textView.applyFontFamily(adapter.lightFont)
        mediaLabelTextView.applyFontFamily(adapter.lightFont)

        quotedNameView.applyFontFamily(adapter.lightFont)
        quotedTextView.applyFontFamily(adapter.lightFont)
        quotedMediaLabelTextView.applyFontFamily(adapter.lightFont)
    }


    fun displaySampleStatus() {
        val profileImageEnabled = adapter.profileImageEnabled
        profileImageView.visibility = if (profileImageEnabled) View.VISIBLE else View.GONE
        statusContentUpperSpace.visibility = View.VISIBLE

        profileImageView.setImageResource(R.drawable.ic_account_logo_twitter)
        nameView.setName(Constants.TWITTNUKER_PREVIEW_NAME)
        nameView.setScreenName("@" + Constants.TWITTNUKER_PREVIEW_SCREEN_NAME)
        nameView.updateText(adapter.bidiFormatter)
        if (adapter.linkHighlightingStyle == VALUE_LINK_HIGHLIGHT_OPTION_CODE_NONE) {
            textView.text = toPlainText(Constants.TWITTNUKER_PREVIEW_TEXT_HTML)
        } else {
            val linkify = adapter.twidereLinkify
            val text = HtmlSpanBuilder.fromHtml(Constants.TWITTNUKER_PREVIEW_TEXT_HTML)
            linkify.applyAllLinks(text, null, -1, false, adapter.linkHighlightingStyle, true)
            textView.text = text
        }
        timeView.time = System.currentTimeMillis()
        val showCardActions = isCardActionsShown
        if (adapter.mediaPreviewEnabled) {
            mediaPreview.visibility = View.VISIBLE
            mediaLabel.visibility = View.GONE
        } else {
            mediaPreview.visibility = View.GONE
            mediaLabel.visibility = View.VISIBLE
        }
        actionButtons.visibility = if (showCardActions) View.VISIBLE else View.GONE
        itemMenu.visibility = if (showCardActions) View.VISIBLE else View.GONE
        statusContentLowerSpace.visibility = if (showCardActions) View.GONE else View.VISIBLE
        quotedMediaPreview.visibility = View.GONE
        quotedMediaLabel.visibility = View.GONE
        mediaPreview.displayMedia(R.drawable.twittnuker_feature_graphic)
        extraTypeView.setImageResource(R.drawable.ic_action_gallery)
    }

    override fun displayStatus(status: ParcelableStatus, displayInReplyTo: Boolean,
                               shouldDisplayExtraType: Boolean) {

        val context = itemView.context
        val loader = adapter.mediaLoader
        val twitter = adapter.twitterWrapper
        val linkify = adapter.twidereLinkify
        val formatter = adapter.bidiFormatter
        val colorNameManager = adapter.userColorNameManager
        val nameFirst = adapter.nameFirst
        val showCardActions = isCardActionsShown

        actionButtons.visibility = if (showCardActions) View.VISIBLE else View.GONE
        itemMenu.visibility = if (showCardActions) View.VISIBLE else View.GONE
        statusContentLowerSpace.visibility = if (showCardActions) View.GONE else View.VISIBLE

        val replyCount = status.reply_count
        val retweetCount: Long
        val favoriteCount: Long

        if (status.is_pinned_status) {
            statusInfoLabel.setText(R.string.pinned_status)
            statusInfoIcon.setImageResource(R.drawable.ic_activity_action_pinned)
            statusInfoLabel.visibility = View.VISIBLE
            statusInfoIcon.visibility = View.VISIBLE

            statusContentUpperSpace.visibility = View.GONE
        } else if (TwitterCardUtils.isPoll(status)) {
            statusInfoLabel.setText(R.string.label_poll)
            statusInfoIcon.setImageResource(R.drawable.ic_activity_action_poll)
            statusInfoLabel.visibility = View.VISIBLE
            statusInfoIcon.visibility = View.VISIBLE

            statusContentUpperSpace.visibility = View.GONE
        } else if (status.retweet_id != null) {
            val retweetedBy = colorNameManager.getDisplayName(status.retweeted_by_user_key!!,
                    status.retweeted_by_user_name, status.retweeted_by_user_screen_name, nameFirst)
            statusInfoLabel.text = context.getString(R.string.name_retweeted, formatter.unicodeWrap(retweetedBy))
            statusInfoIcon.setImageResource(R.drawable.ic_activity_action_retweet)
            statusInfoLabel.visibility = View.VISIBLE
            statusInfoIcon.visibility = View.VISIBLE

            statusContentUpperSpace.visibility = View.GONE
        } else if (status.in_reply_to_status_id != null && status.in_reply_to_user_key != null && displayInReplyTo) {
            val inReplyTo = colorNameManager.getDisplayName(status.in_reply_to_user_key!!,
                    status.in_reply_to_name, status.in_reply_to_screen_name, nameFirst)
            statusInfoLabel.text = context.getString(R.string.in_reply_to_name, formatter.unicodeWrap(inReplyTo))
            statusInfoIcon.setImageResource(R.drawable.ic_activity_action_reply)
            statusInfoLabel.visibility = View.VISIBLE
            statusInfoIcon.visibility = View.VISIBLE

            statusContentUpperSpace.visibility = View.GONE
        } else {
            statusInfoLabel.visibility = View.GONE
            statusInfoIcon.visibility = View.GONE

            statusContentUpperSpace.visibility = View.VISIBLE
        }

        val skipLinksInText = status.extras != null && status.extras.support_entities
        if (status.is_quote) {

            quotedView.visibility = View.VISIBLE

            val quoteContentAvailable = status.quoted_text_plain != null && status.quoted_text_unescaped != null
            val isFanfouStatus = status.account_key.host == USER_TYPE_FANFOU_COM
            if (quoteContentAvailable && !isFanfouStatus) {
                quotedNameView.visibility = View.VISIBLE
                quotedTextView.visibility = View.VISIBLE

                val quoted_user_key = status.quoted_user_key!!
                quotedNameView.setName(
                        status.quoted_user_name)
                quotedNameView.setScreenName("@${status.quoted_user_screen_name}")

                var quotedDisplayEnd = -1
                if (status.extras.quoted_display_text_range != null) {
                    quotedDisplayEnd = status.extras.quoted_display_text_range!![1]
                }
                val quotedText: CharSequence
                if (adapter.linkHighlightingStyle != VALUE_LINK_HIGHLIGHT_OPTION_CODE_NONE) {
                    quotedText = SpannableStringBuilder.valueOf(status.quoted_text_unescaped)
                    ParcelableStatusUtils.applySpans(quotedText as Spannable, status.quoted_spans)
                    linkify.applyAllLinks(quotedText, status.account_key, layoutPosition.toLong(),
                            status.is_possibly_sensitive, adapter.linkHighlightingStyle,
                            skipLinksInText)
                } else {
                    quotedText = status.quoted_text_unescaped
                }
                if (quotedDisplayEnd != -1 && quotedDisplayEnd <= quotedText.length) {
                    quotedTextView.text = quotedText.subSequence(0, quotedDisplayEnd)
                } else {
                    quotedTextView.text = quotedText
                }

                if (quotedTextView.length() == 0) {
                    // No text
                    quotedTextView.visibility = View.GONE
                } else {
                    quotedTextView.visibility = View.VISIBLE
                }

                val quoted_user_color = colorNameManager.getUserColor(quoted_user_key)
                if (quoted_user_color != 0) {
                    quotedView.drawStart(quoted_user_color)
                } else {
                    quotedView.drawStart(ThemeUtils.getColorFromAttribute(context, R.attr.quoteIndicatorBackgroundColor, 0))
                }

                displayQuotedMedia(loader, status)
            } else {
                quotedNameView.visibility = View.GONE
                quotedTextView.visibility = View.VISIBLE

                if (quoteContentAvailable) {
                    displayQuotedMedia(loader, status)
                } else {
                    quotedMediaPreview.visibility = View.GONE
                    quotedMediaLabel.visibility = View.GONE
                }

                val quoteHint = if (!quoteContentAvailable) {
                    // Display 'not available' label
                    context.getString(R.string.label_status_not_available)
                } else {
                    // Display 'original status' label
                    context.getString(R.string.label_original_status)
                }
                quotedTextView.text = SpannableString.valueOf(quoteHint).apply {
                    setSpan(ForegroundColorSpan(ThemeUtils.getColorFromAttribute(context,
                            android.R.attr.textColorTertiary, textView.currentTextColor)), 0,
                            length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }

                quotedView.drawStart(ThemeUtils.getColorFromAttribute(context, R.attr.quoteIndicatorBackgroundColor, 0))
            }

            itemContent.drawStart(colorNameManager.getUserColor(status.user_key))
        } else {
            quotedView.visibility = View.GONE

            val userColor = colorNameManager.getUserColor(status.user_key)

            if (status.is_retweet) {
                val retweetUserColor = colorNameManager.getUserColor(status.retweeted_by_user_key!!)
                if (retweetUserColor == 0) {
                    itemContent.drawStart(userColor)
                } else if (userColor == 0) {
                    itemContent.drawStart(retweetUserColor)
                } else {
                    itemContent.drawStart(retweetUserColor, userColor)
                }
            } else {
                itemContent.drawStart(userColor)
            }
        }

        timeView.time = if (status.is_retweet) {
            status.retweet_timestamp
        } else {
            status.timestamp
        }

        nameView.setName(status.user_name)
        nameView.setScreenName("@${status.user_screen_name}")

        if (adapter.profileImageEnabled) {
            profileImageView.visibility = View.VISIBLE
            loader.displayProfileImage(profileImageView, status)

            profileTypeView.setImageResource(getUserTypeIconRes(status.user_is_verified, status.user_is_protected))
            profileTypeView.visibility = View.VISIBLE
        } else {
            profileImageView.visibility = View.GONE
            loader.cancelDisplayTask(profileImageView)

            profileTypeView.setImageDrawable(null)
            profileTypeView.visibility = View.GONE
        }

        if (adapter.showAccountsColor) {
            itemContent.drawEnd(status.account_color)
        } else {
            itemContent.drawEnd()
        }

        if (status.media?.isNotEmpty() ?: false) {

            if (!adapter.sensitiveContentEnabled && status.is_possibly_sensitive) {
                // Sensitive content, show label instead of media view
                mediaLabel.visibility = View.VISIBLE
                mediaPreview.visibility = View.GONE
            } else if (!adapter.mediaPreviewEnabled) {
                // Media preview disabled, just show label
                mediaLabel.visibility = View.VISIBLE
                mediaPreview.visibility = View.GONE
            } else {
                // Show media
                mediaLabel.visibility = View.GONE
                mediaPreview.visibility = View.VISIBLE

                mediaPreview.displayMedia(status.media, loader, status.account_key, -1, this,
                        adapter.mediaLoadingHandler)
            }
        } else {
            // No media, hide all related views
            mediaLabel.visibility = View.GONE
            mediaPreview.visibility = View.GONE
        }


        var displayEnd = -1
        if (status.extras.display_text_range != null) {
            displayEnd = status.extras.display_text_range!![1]
        }

        val text: CharSequence
        if (adapter.linkHighlightingStyle != VALUE_LINK_HIGHLIGHT_OPTION_CODE_NONE) {
            text = SpannableStringBuilder.valueOf(status.text_unescaped)
            ParcelableStatusUtils.applySpans(text as Spannable, status.spans)
            linkify.applyAllLinks(text, status.account_key, layoutPosition.toLong(),
                    status.is_possibly_sensitive, adapter.linkHighlightingStyle,
                    skipLinksInText)
        } else {
            text = status.text_unescaped
        }

        if (displayEnd != -1 && displayEnd <= text.length) {
            textView.text = text.subSequence(0, displayEnd)
        } else {
            textView.text = text
        }
        if (textView.length() == 0) {
            // No text
            textView.visibility = View.GONE
        } else {
            textView.visibility = View.VISIBLE
        }

        if (replyCount > 0) {
            replyCountView.text = UnitConvertUtils.calculateProperCount(replyCount)
            replyCountView.visibility = View.VISIBLE
        } else {
            replyCountView.text = null
            replyCountView.visibility = View.GONE
        }

        if (twitter.isDestroyingStatus(status.account_key, status.my_retweet_id)) {
            retweetIcon.isActivated = false
            retweetCount = Math.max(0, status.retweet_count - 1)
        } else {
            val creatingRetweet = twitter.isCreatingRetweet(status.account_key, status.id)
            retweetIcon.isActivated = creatingRetweet || status.retweeted ||
                    Utils.isMyRetweet(status.account_key, status.retweeted_by_user_key,
                            status.my_retweet_id)
            retweetCount = status.retweet_count + if (creatingRetweet) 1 else 0
        }

        if (retweetCount > 0) {
            retweetCountView.text = UnitConvertUtils.calculateProperCount(retweetCount)
            retweetCountView.visibility = View.VISIBLE
        } else {
            retweetCountView.text = null
            retweetCountView.visibility = View.GONE
        }
        if (twitter.isDestroyingFavorite(status.account_key, status.id)) {
            favoriteIcon.isActivated = false
            favoriteCount = Math.max(0, status.favorite_count - 1)
        } else {
            val creatingFavorite = twitter.isCreatingFavorite(status.account_key, status.id)
            favoriteIcon.isActivated = creatingFavorite || status.is_favorite
            favoriteCount = status.favorite_count + if (creatingFavorite) 1 else 0
        }
        if (favoriteCount > 0) {
            favoriteCountView.text = UnitConvertUtils.calculateProperCount(favoriteCount)
            favoriteCountView.visibility = View.VISIBLE
        } else {
            favoriteCountView.text = null
            favoriteCountView.visibility = View.GONE
        }
        if (shouldDisplayExtraType) {
            displayExtraTypeIcon(status.card_name, status.media, status.location,
                    status.place_full_name, status.is_possibly_sensitive)
        } else {
            extraTypeView.visibility = View.GONE
        }

        nameView.updateText(formatter)
        quotedNameView.updateText(formatter)

    }

    private fun displayQuotedMedia(loader: MediaLoaderWrapper, status: ParcelableStatus) {
        if (status.quoted_media?.isNotEmpty() ?: false) {

            if (!adapter.sensitiveContentEnabled && status.is_possibly_sensitive) {
                // Sensitive content, show label instead of media view
                quotedMediaPreview.visibility = View.GONE
                quotedMediaLabel.visibility = View.VISIBLE
            } else if (!adapter.mediaPreviewEnabled) {
                // Media preview disabled, just show label
                quotedMediaPreview.visibility = View.GONE
                quotedMediaLabel.visibility = View.VISIBLE
            } else {
                // Show media
                quotedMediaPreview.visibility = View.VISIBLE
                quotedMediaLabel.visibility = View.GONE

                quotedMediaPreview.displayMedia(status.quoted_media, loader, status.account_key, -1,
                        null, null)
            }
        } else {
            // No media, hide all related views
            quotedMediaPreview.visibility = View.GONE
            quotedMediaLabel.visibility = View.GONE
        }
    }

    override fun onMediaClick(view: View, media: ParcelableMedia, accountKey: UserKey, extraId: Long) {
        statusClickListener?.onMediaClick(this, view, media, layoutPosition)
    }


    fun setOnClickListeners() {
        setStatusClickListener(adapter.statusClickListener)
    }

    override fun setStatusClickListener(listener: IStatusViewHolder.StatusClickListener?) {
        statusClickListener = listener
        itemContent.setOnClickListener(eventListener)
        itemContent.setOnLongClickListener(eventListener)

        itemMenu.setOnClickListener(eventListener)
        profileImageView.setOnClickListener(eventListener)
        replyButton.setOnClickListener(eventListener)
        retweetButton.setOnClickListener(eventListener)
        favoriteButton.setOnClickListener(eventListener)

        mediaLabel.setOnClickListener(eventListener)

        quotedView.setOnClickListener(eventListener)
    }


    override fun setTextSize(textSize: Float) {
        nameView.setPrimaryTextSize(textSize)
        quotedNameView.setPrimaryTextSize(textSize)
        textView.textSize = textSize
        quotedTextView.textSize = textSize
        nameView.setSecondaryTextSize(textSize * 0.85f)
        quotedNameView.setSecondaryTextSize(textSize * 0.85f)
        timeView.textSize = textSize * 0.85f
        statusInfoLabel.textSize = textSize * 0.75f

        mediaLabelTextView.textSize = textSize * 0.95f

        replyCountView.textSize = textSize
        retweetCountView.textSize = textSize
        favoriteCountView.textSize = textSize
    }

    fun setupViewOptions() {
        setTextSize(adapter.textSize)
        profileImageView.style = adapter.profileImageStyle

        mediaPreview.setStyle(adapter.mediaPreviewStyle)
        quotedMediaPreview.setStyle(adapter.mediaPreviewStyle)
        //        profileImageView.setStyle(adapter.getProfileImageStyle());

        val nameFirst = adapter.nameFirst
        nameView.setNameFirst(nameFirst)
        quotedNameView.setNameFirst(nameFirst)

        val favIcon: Int
        val favStyle: Int
        val favColor: Int
        val context = itemView.context
        if (adapter.useStarsForLikes) {
            favIcon = R.drawable.ic_action_star
            favStyle = LikeAnimationDrawable.Style.FAVORITE
            favColor = ContextCompat.getColor(context, R.color.highlight_favorite)
        } else {
            favIcon = R.drawable.ic_action_heart
            favStyle = LikeAnimationDrawable.Style.LIKE
            favColor = ContextCompat.getColor(context, R.color.highlight_like)
        }
        val icon = ContextCompat.getDrawable(context, favIcon)
        val drawable = LikeAnimationDrawable(icon,
                favoriteCountView.textColors.defaultColor, favColor, favStyle)
        drawable.mutate()
        favoriteIcon.setImageDrawable(drawable)
        timeView.showAbsoluteTime = adapter.showAbsoluteTime

        favoriteIcon.activatedColor = favColor
    }

    override fun playLikeAnimation(listener: LikeAnimationDrawable.OnLikedListener) {
        var handled = false
        val drawable = favoriteIcon.drawable
        if (drawable is LikeAnimationDrawable) {
            drawable.setOnLikedListener(listener)
            drawable.start()
            handled = true
        }
        if (!handled) {
            listener.onLiked()
        }
    }

    private val isCardActionsShown: Boolean
        get() = adapter.isCardActionsShown(layoutPosition)

    private fun showCardActions() {
        adapter.showCardActions(layoutPosition)
    }

    private fun hideTempCardActions(): Boolean {
        adapter.showCardActions(RecyclerView.NO_POSITION)
        return !adapter.isCardActionsShown(RecyclerView.NO_POSITION)
    }

    private fun displayExtraTypeIcon(cardName: String?, media: Array<ParcelableMedia?>?,
                                     location: ParcelableLocation?, placeFullName: String?,
                                     sensitive: Boolean) {
        if (TwitterCardUtils.CARD_NAME_AUDIO == cardName) {
            extraTypeView.setImageResource(if (sensitive) R.drawable.ic_action_warning else R.drawable.ic_action_music)
            extraTypeView.visibility = View.VISIBLE
        } else if (TwitterCardUtils.CARD_NAME_ANIMATED_GIF == cardName) {
            extraTypeView.setImageResource(if (sensitive) R.drawable.ic_action_warning else R.drawable.ic_action_movie)
            extraTypeView.visibility = View.VISIBLE
        } else if (TwitterCardUtils.CARD_NAME_PLAYER == cardName) {
            extraTypeView.setImageResource(if (sensitive) R.drawable.ic_action_warning else R.drawable.ic_action_play_circle)
            extraTypeView.visibility = View.VISIBLE
        } else if (media?.isNotEmpty() ?: false) {
            if (hasVideo(media)) {
                extraTypeView.setImageResource(if (sensitive) R.drawable.ic_action_warning else R.drawable.ic_action_movie)
            } else {
                extraTypeView.setImageResource(if (sensitive) R.drawable.ic_action_warning else R.drawable.ic_action_gallery)
            }
            extraTypeView.visibility = View.VISIBLE
        } else if (ParcelableLocationUtils.isValidLocation(location) || !TextUtils.isEmpty(placeFullName)) {
            extraTypeView.setImageResource(R.drawable.ic_action_location)
            extraTypeView.visibility = View.VISIBLE
        } else {
            extraTypeView.visibility = View.GONE
        }
    }

    private fun hasVideo(media: Array<ParcelableMedia?>?): Boolean {
        if (media == null) return false
        media.filterNotNull().forEach {
            when (it.type) {
                ParcelableMedia.Type.VIDEO, ParcelableMedia.Type.ANIMATED_GIF, ParcelableMedia.Type.EXTERNAL_PLAYER -> return true
            }
        }
        return false
    }

    internal class EventListener(holder: StatusViewHolder) : OnClickListener, OnLongClickListener {

        val holderRef: WeakReference<StatusViewHolder>

        init {
            this.holderRef = WeakReference(holder)
        }

        override fun onClick(v: View) {
            val holder = holderRef.get() ?: return
            val listener = holder.statusClickListener ?: return
            val position = holder.layoutPosition
            when (v) {
                holder.itemContent -> {
                    listener.onStatusClick(holder, position)
                }
                holder.quotedView -> {
                    listener.onQuotedStatusClick(holder, position)
                }
                holder.itemMenu -> {
                    listener.onItemMenuClick(holder, v, position)
                }
                holder.profileImageView -> {
                    listener.onUserProfileClick(holder, position)
                }
                holder.replyButton -> {
                    listener.onItemActionClick(holder, R.id.reply, position)
                }
                holder.retweetButton -> {
                    listener.onItemActionClick(holder, R.id.retweet, position)
                }
                holder.favoriteButton -> {
                    listener.onItemActionClick(holder, R.id.favorite, position)
                }
                holder.mediaLabel -> {
                    val firstMedia = holder.adapter.getStatus(position)?.media?.firstOrNull() ?: return
                    listener.onMediaClick(holder, v, firstMedia, position)
                }
            }
        }

        override fun onLongClick(v: View): Boolean {
            val holder = holderRef.get() ?: return false
            val listener = holder.statusClickListener ?: return false
            val position = holder.layoutPosition
            when (v) {
                holder.itemContent -> {
                    if (!holder.isCardActionsShown) {
                        holder.showCardActions()
                        return true
                    } else if (holder.hideTempCardActions()) {
                        return true
                    }
                    return listener.onStatusLongClick(holder, position)
                }
            }
            return false
        }
    }

}
