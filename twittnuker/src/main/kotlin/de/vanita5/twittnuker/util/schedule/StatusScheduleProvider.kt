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

package de.vanita5.twittnuker.util.schedule

import android.content.Context
import android.content.Intent
import android.support.annotation.WorkerThread
import de.vanita5.twittnuker.model.ParcelableStatusUpdate
import de.vanita5.twittnuker.model.schedule.ScheduleInfo
import de.vanita5.twittnuker.task.twitter.UpdateStatusTask
import de.vanita5.twittnuker.task.twitter.UpdateStatusTask.PendingStatusUpdate
import java.util.*


interface StatusScheduleProvider {

    @WorkerThread
    @Throws(ScheduleException::class)
    fun scheduleStatus(statusUpdate: ParcelableStatusUpdate, pendingUpdate: PendingStatusUpdate,
                       scheduleInfo: ScheduleInfo)

    fun createSetScheduleIntent(): Intent

    fun createSettingsIntent(): Intent?

    fun createManageIntent(): Intent?

    class ScheduleException : UpdateStatusTask.UpdateStatusException {

        constructor() : super()

        constructor(detailMessage: String, throwable: Throwable) : super(detailMessage, throwable)

        constructor(throwable: Throwable) : super(throwable)

        constructor(message: String) : super(message)
    }

    interface Factory {
        fun newInstance(context: Context): StatusScheduleProvider?

        fun parseInfo(json: String): ScheduleInfo?

        companion object {
            val instance: Factory get() = ServiceLoader.load(Factory::class.java)?.firstOrNull() ?: NullFactory

            private object NullFactory : Factory {
                override fun newInstance(context: Context) = null

                override fun parseInfo(json: String): ScheduleInfo? = null

            }
        }
    }
}