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

package de.vanita5.twittnuker.util

import android.content.Context
import de.vanita5.twittnuker.library.mastodon.model.RegisteredApplication
import de.vanita5.twittnuker.TwittnukerConstants.ETAG_MASTODON_APPS_PREFERENCES_NAME
import java.io.IOException


class MastodonApplicationRegistry(context: Context) {
    private val preferences = context.getSharedPreferences(ETAG_MASTODON_APPS_PREFERENCES_NAME,
            Context.MODE_PRIVATE)

    operator fun get(host: String): RegisteredApplication? {
        val json = preferences.getString(host, null) ?: return null
        try {
            return JsonSerializer.parse(json, RegisteredApplication::class.java)
        } catch (e: IOException) {
            return null
        }
    }

    operator fun set(host: String, app: RegisteredApplication?): Boolean {
        val editor = preferences.edit()
        if (app != null) {
            editor.putString(host, JsonSerializer.serialize(app, RegisteredApplication::class.java))
        } else {
            return false
        }
        editor.apply()
        return true
    }
}