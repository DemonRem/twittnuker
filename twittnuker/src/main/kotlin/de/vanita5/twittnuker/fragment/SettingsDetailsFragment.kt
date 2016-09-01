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

package de.vanita5.twittnuker.fragment

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.support.v7.preference.PreferenceScreen
import de.vanita5.twittnuker.Constants.*
import de.vanita5.twittnuker.activity.SettingsActivity
import de.vanita5.twittnuker.util.Utils

class SettingsDetailsFragment : BasePreferenceFragment(), OnSharedPreferenceChangeListener {


    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String) {
        val preferenceManager = preferenceManager
        preferenceManager.sharedPreferencesName = SHARED_PREFERENCES_NAME
        val defaultScreen = preferenceScreen
        val preferenceScreen: PreferenceScreen
        if (defaultScreen != null) {
            defaultScreen.removeAll()
            preferenceScreen = defaultScreen
        } else {
            preferenceScreen = preferenceManager.createPreferenceScreen(activity)
        }
        setPreferenceScreen(preferenceScreen)

        val args = arguments
        val rawResId = args.get(EXTRA_RESID)
        val resId: Int
        if (rawResId is Int) {
            resId = rawResId
        } else if (rawResId is String) {
            resId = Utils.getResId(activity, rawResId)
        } else {
            resId = 0
        }
        if (resId != 0) {
            addPreferencesFromResource(resId)
        }

    }

    override fun onStart() {
        super.onStart()
        val preferences = preferenceManager.sharedPreferences
        preferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onStop() {
        val preferences = preferenceManager.sharedPreferences
        preferences.unregisterOnSharedPreferenceChangeListener(this)
        super.onStop()
    }

    override fun onSharedPreferenceChanged(preferences: SharedPreferences, key: String) {
        val preference = findPreference(key) ?: return
        val extras = preference.extras
        if (extras != null) {
            val activity = activity
            if (extras.containsKey(EXTRA_NOTIFY_CHANGE)) {
                SettingsActivity.setShouldNotifyChange(activity)
            }
            if (extras.containsKey(EXTRA_RESTART_ACTIVITY)) {
                activity.recreate()
            }
        }
    }


}