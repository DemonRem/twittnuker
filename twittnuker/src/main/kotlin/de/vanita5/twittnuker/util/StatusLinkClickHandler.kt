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

package de.vanita5.twittnuker.util

import android.content.Context
import de.vanita5.twittnuker.Constants
import de.vanita5.twittnuker.constant.SharedPreferenceConstants.KEY_NEW_DOCUMENT_API
import de.vanita5.twittnuker.model.ParcelableMedia
import de.vanita5.twittnuker.model.ParcelableStatus
import de.vanita5.twittnuker.model.UserKey

open class StatusLinkClickHandler(context: Context, manager: MultiSelectManager, preferences: SharedPreferencesWrapper) : OnLinkClickHandler(context, manager, preferences), Constants {

    var status: ParcelableStatus? = null

    override fun openMedia(accountKey: UserKey, extraId: Long, sensitive: Boolean,
                           link: String, start: Int, end: Int) {
        val status = status
        val current = findByLink(status!!.media, link)
        if (current == null || current.open_browser) {
            openLink(link)
        } else {
            IntentUtils.openMedia(context, status, current, null,
                    preferences.getBoolean(KEY_NEW_DOCUMENT_API))
        }
    }

    companion object {

        fun findByLink(media: Array<ParcelableMedia>?, link: String?): ParcelableMedia? {
            if (link == null || media == null) return null
            return media.firstOrNull {
                link == it.media_url || link == it.url || link == it.page_url || link == it.preview_url
            }
        }
    }
}