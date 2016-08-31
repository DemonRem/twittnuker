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

package de.vanita5.twittnuker.adapter

import android.content.Context
import android.support.v4.text.BidiFormatter
import android.support.v7.widget.RecyclerView

import de.vanita5.twittnuker.util.*
import de.vanita5.twittnuker.util.dagger.GeneralComponentHelper

import javax.inject.Inject

abstract class BaseRecyclerViewAdapter<VH : RecyclerView.ViewHolder>(
        val context: Context
) : RecyclerView.Adapter<VH>() {
    @Inject
    lateinit var twitterWrapper: AsyncTwitterWrapper
    @Inject
    lateinit var readStateManager: ReadStateManager
    @Inject
    lateinit var mediaLoader: MediaLoaderWrapper
    @Inject
    lateinit var multiSelectManager: MultiSelectManager
    @Inject
    lateinit var userColorNameManager: UserColorNameManager
    @Inject
    lateinit var preferences: SharedPreferencesWrapper
    @Inject
    lateinit var bidiFormatter: BidiFormatter

    init {
        //noinspection unchecked
        GeneralComponentHelper.build(context).inject(this as BaseRecyclerViewAdapter<RecyclerView.ViewHolder>)
    }

    fun findPositionByItemId(itemId: Long): Int {
        var i = 0
        val j = itemCount
        while (i < j) {
            if (getItemId(i) == itemId) return i
            i++
        }
        return RecyclerView.NO_POSITION
    }
}