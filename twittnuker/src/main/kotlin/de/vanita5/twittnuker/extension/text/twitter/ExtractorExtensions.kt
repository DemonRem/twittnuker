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

package de.vanita5.twittnuker.extension.text.twitter

import com.twitter.Extractor
import de.vanita5.twittnuker.model.ParcelableStatus
import de.vanita5.twittnuker.model.ParcelableUserMention

fun Extractor.extractMentionsAndNonMentionStartIndex(text: String): MentionsAndNonMentionStartIndex {
    var nextExpectedPos = 0
    val mentions = extractMentionedScreennamesWithIndices(text)
    @Suppress("LoopToCallChain")
    for (entity in mentions) {
        if (entity.start != nextExpectedPos) break
        nextExpectedPos = (entity.end..text.indices.endInclusive).firstOrNull {
            !text[it].isWhitespace()
        } ?: text.indices.endInclusive + 1
    }
    return MentionsAndNonMentionStartIndex(mentions, nextExpectedPos)
}

fun Extractor.extractReplyTextAndMentions(text: String, inReplyTo: ParcelableStatus): ReplyTextAndMentions {
    // First extract mentions and 'real text' start index
    val (textMentions, index) = extractMentionsAndNonMentionStartIndex(text)

    val replyMentions = run {
        val mentions = inReplyTo.mentions?.toMutableList() ?: mutableListOf()
        if (inReplyTo.is_retweet) {
            mentions.add(ParcelableUserMention().also {
                it.key = inReplyTo.retweeted_by_user_key
                it.name = inReplyTo.retweeted_by_user_name
                it.screen_name = inReplyTo.retweeted_by_user_screen_name
            })
        }
        return@run mentions
    }

    // Find mentions that `inReplyTo` doesn't have and add to `extraMentions` list

    val extraMentions = textMentions.filter { entity ->
        if (entity.value.equals(inReplyTo.user_screen_name, ignoreCase = true)) {
            return@filter false
        }
        return@filter replyMentions.none { mention ->
            entity.value.equals(mention.screen_name, ignoreCase = true)
        }
    }
    // Find removed mentions from `inReplyTo` and add to `excludedMentions` list
    val excludedMentions = replyMentions.filter { mention ->
        return@filter textMentions.none { entity ->
            entity.value.equals(mention.screen_name, ignoreCase = true)
        }
    }
    // Find reply text contains mention to `inReplyTo.user`
    val mentioningUser = textMentions.any {
        it.value.equals(inReplyTo.user_screen_name, ignoreCase = true)
    }
    if (!mentioningUser) {
        /*
         * remember to process status without mentioning user of `inReplyTo`
         * then this status should be treated at a mention referring to `inReplyTo`, all other mentions
         * counts.
         */
        return ReplyTextAndMentions(text, emptyList(), emptyList(), mentioningUser)
    }
    val overrideText = run {
        val sb = StringBuilder()
        extraMentions.forEach { entity ->
            if (entity.start >= index) return@forEach
            sb.append('@')
            sb.append(entity.value)
            sb.append(' ')
        }
        sb.append(text, index, text.length)
        return@run sb.toString()
    }
    return ReplyTextAndMentions(overrideText, extraMentions, excludedMentions, mentioningUser)
}

data class MentionsAndNonMentionStartIndex(val mentions: List<Extractor.Entity>, val index: Int)

data class ReplyTextAndMentions(
        val replyText: String,
        val extraMentions: List<Extractor.Entity>,
        val excludedMentions: List<ParcelableUserMention>,
        val replyToOriginalUser: Boolean
)