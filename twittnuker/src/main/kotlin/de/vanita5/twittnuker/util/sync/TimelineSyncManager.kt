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

package de.vanita5.twittnuker.util.sync

import android.content.Context
import android.support.annotation.UiThread
import android.support.annotation.WorkerThread
import android.support.v4.util.ArrayMap
import de.vanita5.twittnuker.annotation.ReadPositionTag
import java.io.IOException
import java.util.*


abstract class TimelineSyncManager(val context: Context) {

    private val stagedCommits = ArrayMap<TimelineKey, Long>()
    private val cachedPositions = ArrayMap<TimelineKey, Long>()

    fun setPosition(@ReadPositionTag positionTag: String, currentTag: String?, positionKey: Long) {
        stagedCommits[TimelineKey(positionTag, currentTag)] = positionKey
    }

    fun commit() {
        val data = stagedCommits.map { (key, value) ->
            PositionData(key.positionTag, key.currentTag, value)
        }.toTypedArray()
        stagedCommits.clear()
        performSync(data)
    }


    fun blockingGetPosition(@ReadPositionTag positionTag: String, currentTag: String?): Long {
        val position = fetchPosition(positionTag, currentTag)
        synchronized(cachedPositions) {
            cachedPositions[TimelineKey(positionTag, currentTag)] = position
        }
        return position
    }

    fun peekPosition(@ReadPositionTag positionTag: String, currentTag: String?): Long {
        synchronized(cachedPositions) {
            return cachedPositions[TimelineKey(positionTag, currentTag)] ?: -1
        }
    }


    @UiThread
    protected abstract fun performSync(data: Array<PositionData>)

    @WorkerThread
    @Throws(IOException::class)
    protected abstract fun fetchPosition(@ReadPositionTag positionTag: String, currentTag: String?): Long

    data class TimelineKey(val positionTag: String, val currentTag: String?)
    data class PositionData(val positionTag: String, val currentTag: String?, val positionKey: Long)

    abstract class Factory {
        protected var manager: TimelineSyncManager? = null

        fun get(): TimelineSyncManager? = manager

        fun setup(context: Context) {
            manager = create(context)
        }

        protected abstract fun create(context: Context): TimelineSyncManager?

    }

    object DummyFactory : Factory() {
        override fun create(context: Context) = null
    }

    companion object {
        fun newFactory(): Factory = ServiceLoader.load(Factory::class.java).firstOrNull() ?: DummyFactory
    }


}