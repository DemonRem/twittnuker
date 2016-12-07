/*
 * Twittnuker - Twitter client for Android
 *
 * Copyright (C) 2013-2016 vanita5 <mail@vanit.as>
 *
 * This program incorporates a modified version of Twidere.
 * Copyright (C) 2012-2016 Mariotaku Lee <mariotaku.lee@gmail.com>
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

package de.vanita5.twittnuker.model.util

import android.text.Spannable
import android.text.Spanned
import android.text.style.URLSpan
import de.vanita5.twittnuker.library.twitter.model.Status
import de.vanita5.twittnuker.TwittnukerConstants.USER_TYPE_FANFOU_COM
import de.vanita5.twittnuker.model.*
import de.vanita5.twittnuker.util.HtmlSpanBuilder
import de.vanita5.twittnuker.util.InternalTwitterContentUtils
import de.vanita5.twittnuker.util.TwitterContentUtils
import de.vanita5.twittnuker.util.UserColorNameManager
import java.util.*

object ParcelableStatusUtils {

    fun makeOriginalStatus(status: ParcelableStatus) {
        if (!status.is_retweet) return
        status.id = status.retweet_id
        status.retweeted_by_user_key = null
        status.retweeted_by_user_name = null
        status.retweeted_by_user_screen_name = null
        status.retweeted_by_user_profile_image = null
        status.retweet_timestamp = -1
        status.retweet_id = null
    }

    fun fromStatus(orig: Status, accountKey: UserKey,
                   isGap: Boolean): ParcelableStatus {
        val result = ParcelableStatus()
        result.is_gap = isGap
        result.account_key = accountKey
        result.id = orig.id
        result.sort_id = orig.sortId
        result.timestamp = getTime(orig.createdAt)

        result.extras = ParcelableStatus.Extras()
        result.extras.external_url = orig.inferExternalUrl()
        result.extras.support_entities = orig.entities != null
        result.extras.statusnet_conversation_id = orig.statusnetConversationId
        result.is_pinned_status = orig.user.pinnedTweetIds?.contains(orig.id) ?: false

        val retweetedStatus = orig.retweetedStatus
        result.is_retweet = orig.isRetweet
        result.retweeted = orig.wasRetweeted()
        val status: Status
        if (retweetedStatus != null) {
            status = retweetedStatus
            val retweetUser = orig.user
            result.retweet_id = retweetedStatus.id
            result.retweet_timestamp = getTime(retweetedStatus.createdAt)
            result.retweeted_by_user_key = UserKeyUtils.fromUser(retweetUser)
            result.retweeted_by_user_name = retweetUser.name
            result.retweeted_by_user_screen_name = retweetUser.screenName
            result.retweeted_by_user_profile_image = TwitterContentUtils.getProfileImageUrl(retweetUser)

            result.extras.retweeted_external_url = retweetedStatus.inferExternalUrl()
        } else {
            status = orig
        }

        val quoted = status.quotedStatus
        result.is_quote = status.isQuoteStatus
        result.quoted_id = status.quotedStatusId
        if (quoted != null) {
            val quotedUser = quoted.user
            result.quoted_id = quoted.id
            result.extras.quoted_external_url = quoted.inferExternalUrl()

            val quotedText = quoted.htmlText
            // Twitter will escape <> to &lt;&gt;, so if a status contains those symbols unescaped
            // We should treat this as an html
            if (isHtml(quotedText)) {
                val html = HtmlSpanBuilder.fromHtml(quotedText, quoted.extendedText)
                result.quoted_text_unescaped = html.toString()
                result.quoted_text_plain = result.quoted_text_unescaped
                result.quoted_spans = getSpanItems(html)
            } else {
                val textWithIndices = InternalTwitterContentUtils.formatStatusTextWithIndices(quoted)
                result.quoted_text_plain = InternalTwitterContentUtils.unescapeTwitterStatusText(quotedText)
                result.quoted_text_unescaped = textWithIndices.text
                result.quoted_spans = textWithIndices.spans
                result.extras.quoted_display_text_range = textWithIndices.range
            }

            result.quoted_timestamp = quoted.createdAt.time
            result.quoted_source = quoted.source
            result.quoted_media = ParcelableMediaUtils.fromStatus(quoted)

            result.quoted_user_key = UserKeyUtils.fromUser(quotedUser)
            result.quoted_user_name = quotedUser.name
            result.quoted_user_screen_name = quotedUser.screenName
            result.quoted_user_profile_image = TwitterContentUtils.getProfileImageUrl(quotedUser)
            result.quoted_user_is_protected = quotedUser.isProtected
            result.quoted_user_is_verified = quotedUser.isVerified
        }

        result.reply_count = status.replyCount
        result.retweet_count = status.retweetCount
        result.favorite_count = status.favoriteCount

        result.in_reply_to_name = getInReplyToName(status)
        result.in_reply_to_screen_name = status.inReplyToScreenName
        result.in_reply_to_status_id = status.inReplyToStatusId
        result.in_reply_to_user_id = getInReplyToUserId(status, accountKey)

        val user = status.user
        result.user_key = UserKeyUtils.fromUser(user)
        result.user_name = user.name
        result.user_screen_name = user.screenName
        result.user_profile_image_url = TwitterContentUtils.getProfileImageUrl(user)
        result.user_is_protected = user.isProtected
        result.user_is_verified = user.isVerified
        result.user_is_following = user.isFollowing
        result.extras.user_profile_image_url_profile_size = user.profileImageUrlProfileSize
        result.extras.user_statusnet_profile_url = user.statusnetProfileUrl
        if (result.extras.user_profile_image_url_profile_size == null) {
            result.extras.user_profile_image_url_profile_size = user.profileImageUrlLarge
        }
        val text = status.htmlText
        // Twitter will escape <> to &lt;&gt;, so if a status contains those symbols unescaped
        // We should treat this as an html
        if (isHtml(text)) {
            val html = HtmlSpanBuilder.fromHtml(text, status.extendedText)
            result.text_unescaped = html.toString()
            result.text_plain = result.text_unescaped
            result.spans = getSpanItems(html)
        } else {
            val textWithIndices = InternalTwitterContentUtils.formatStatusTextWithIndices(status)
            result.text_unescaped = textWithIndices.text
            result.text_plain = InternalTwitterContentUtils.unescapeTwitterStatusText(text)
            result.spans = textWithIndices.spans
            result.extras.display_text_range = textWithIndices.range
        }
        result.media = ParcelableMediaUtils.fromStatus(status)
        result.source = status.source
        result.location = getLocation(status)
        result.is_favorite = status.isFavorited
        if (result.account_key.maybeEquals(result.retweeted_by_user_key)) {
            result.my_retweet_id = result.id
        } else {
            result.my_retweet_id = status.currentUserRetweet
        }
        result.is_possibly_sensitive = status.isPossiblySensitive
        result.mentions = ParcelableUserMentionUtils.fromUserMentionEntities(user,
                status.userMentionEntities)
        result.card = ParcelableCardEntityUtils.fromCardEntity(status.card, accountKey)
        result.place_full_name = getPlaceFullName(status)
        result.card_name = if (result.card != null) result.card!!.name else null
        result.lang = status.lang
        return result
    }

    private fun getSpanItems(html: CharSequence): Array<SpanItem>? {
        if (html !is Spanned) return null
        val spans = html.getSpans(0, html.length, URLSpan::class.java)
        return Array(spans.size) { idx ->
            SpanItem.from(html, spans[idx])
        }
    }

    private fun isHtml(text: String): Boolean {
        return text.contains("<") && text.contains(">")
    }

    private fun isFanfouStatus(accountKey: UserKey): Boolean {
        return USER_TYPE_FANFOU_COM == accountKey.host
    }

    private fun getInReplyToUserId(status: Status, accountKey: UserKey): UserKey? {
        val inReplyToUserId = status.inReplyToUserId ?: return null
        val entities = status.userMentionEntities
        if (entities != null) {
            if (entities.any { inReplyToUserId == it.id }) {
                return UserKey(inReplyToUserId, accountKey.host)
            }
        }
        val attentions = status.attentions
        if (attentions != null) {
            attentions.firstOrNull { inReplyToUserId == it.id }?.let {
                val host = UserKeyUtils.getUserHost(it.ostatusUri,
                        accountKey.host)
                return UserKey(inReplyToUserId, host)
            }
        }
        return UserKey(inReplyToUserId, accountKey.host)
    }

    fun fromStatuses(statuses: Array<Status>?, accountKey: UserKey): Array<ParcelableStatus>? {
        if (statuses == null) return null
        return Array(statuses.size) { i ->
            fromStatus(statuses[i], accountKey, false)
        }
    }

    private fun getPlaceFullName(status: Status): String? {
        val place = status.place
        if (place != null) return place.fullName
        val location = status.location
        if (ParcelableLocation.valueOf(location) == null) {
            return location
        }
        return null
    }

    private fun getLocation(status: Status): ParcelableLocation? {
        val geoLocation = status.geoLocation
        if (geoLocation != null) {
            return ParcelableLocationUtils.fromGeoLocation(geoLocation)
        }
        val locationString = status.location
        val location = ParcelableLocation.valueOf(locationString)
        if (location != null) {
            return location
        }
        return null
    }

    private fun getTime(date: Date?): Long {
        return date?.time ?: 0
    }

    fun getInReplyToName(status: Status): String? {
        val inReplyToUserId = status.inReplyToUserId
        status.userMentionEntities?.firstOrNull { inReplyToUserId == it.id }?.let {
            return it.name
        }
        status.attentions?.firstOrNull { inReplyToUserId == it.id }?.let {
            return it.fullName
        }
        return status.inReplyToScreenName
    }

    fun applySpans(text: Spannable, spans: Array<SpanItem>?) {
        spans?.forEach { span ->
            text.setSpan(URLSpan(span.link), span.start, span.end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    fun updateExtraInformation(status: ParcelableStatus, details: AccountDetails, manager: UserColorNameManager) {
        status.account_color = details.color
        status.user_color = manager.getUserColor(status.user_key)

        if (status.quoted_user_key != null) {
            status.quoted_user_color = manager.getUserColor(status.quoted_user_key!!)
        }
        if (status.retweeted_by_user_key != null) {
            status.retweet_user_color = manager.getUserColor(status.retweeted_by_user_key!!)
        }

        if (status.in_reply_to_user_id != null) {
        }
    }

    fun Status.inferExternalUrl(): String? {
        if (externalUrl != null) {
            return externalUrl
        }
        if (uri != null) {
            val r = Regex("tag:([\\w\\d\\.]+),(\\d{4}\\-\\d{2}\\-\\d{2}):noticeId=(\\d+):objectType=(\\w+)")
            r.matchEntire(uri)?.let { result: MatchResult ->
                return "https://%s/notice/%s".format(Locale.ROOT, result.groups[1]?.value, result.groups[3]?.value)
            }
        }
        return null
    }
}
