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

package de.vanita5.twittnuker.preference

import android.content.Context
import android.content.SharedPreferences
import android.support.v7.preference.Preference
import android.text.TextUtils
import android.util.AttributeSet

import org.mariotaku.abstask.library.AbstractTask
import org.mariotaku.abstask.library.TaskStarter

import de.vanita5.twittnuker.R
import de.vanita5.twittnuker.TwittnukerConstants.*
import de.vanita5.twittnuker.constant.SharedPreferenceConstants
import de.vanita5.twittnuker.push.PushBackendServer

class PushNotificationStatusPreference @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = R.attr.preferenceStyle
) : Preference(context, attrs, defStyle) {

    private val preferences: SharedPreferences = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)

    init {
        setTitle(R.string.push_status_title)

        if (preferences.getBoolean(SharedPreferenceConstants.GCM_TOKEN_SENT, false)) {
            setSummary(R.string.push_status_connected)
        } else {
            setSummary(R.string.push_status_disconnected)
        }
    }

    override fun onClick() {
        setSummary(R.string.push_status_disconnecting)
        super.onClick()

        val task = object : AbstractTask<Any?, Unit, PushNotificationStatusPreference>() {

            override fun afterExecute(pushNotificationStatusPreference: PushNotificationStatusPreference?, result: Unit?) {
                pushNotificationStatusPreference!!.setSummary(R.string.push_status_disconnected)
            }

            override fun doLongOperation(param: Any?) {
                val currentToken = preferences.getString(SharedPreferenceConstants.GCM_CURRENT_TOKEN, null)

                if (!TextUtils.isEmpty(currentToken)) {
                    val backend = PushBackendServer(context)
                    backend.remove(currentToken)
                    preferences.edit().putBoolean(SharedPreferenceConstants.GCM_TOKEN_SENT, false).apply()
                    preferences.edit().putString(SharedPreferenceConstants.GCM_CURRENT_TOKEN, null).apply()
                }
            }
        }
        task.callback = this
        TaskStarter.execute(task)
    }
}
