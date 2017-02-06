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

package de.vanita5.twittnuker.preference.sync

import android.content.Context
import android.support.v7.preference.SwitchPreferenceCompat
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import de.vanita5.twittnuker.R
import de.vanita5.twittnuker.util.Utils
import de.vanita5.twittnuker.util.dagger.GeneralComponentHelper
import de.vanita5.twittnuker.util.sync.SyncPreferences
import javax.inject.Inject


class SyncItemPreference(
        context: Context,
        attrs: AttributeSet
) : SwitchPreferenceCompat(context, attrs) {
    @Inject
    protected lateinit var syncPreferences: SyncPreferences
    val syncType: String

    init {
        GeneralComponentHelper.build(context).inject(this)
        val a = context.obtainStyledAttributes(attrs, R.styleable.SyncItemPreference)
        syncType = a.getString(R.styleable.SyncItemPreference_syncType)
        key = SyncPreferences.getSyncEnabledKey(syncType)
        a.recycle()

    }

    override fun syncSummaryView(view: View?) {
        if (view is TextView) {
            view.visibility = View.VISIBLE
            val lastSynced = syncPreferences.getLastSynced(syncType)
            if (lastSynced > 0) {
                view.text = context.getString(R.string.message_sync_last_synced_time,
                        Utils.formatToLongTimeString(context, lastSynced))
            } else {
                view.text = null
            }
        }
    }
}