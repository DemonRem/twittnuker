/*
 *  Twittnuker - Twitter client for Android
 *
 *  Copyright (C) 2013-2016 vanita5 <mail@vanit.as>
 *
 *  This program incorporates a modified version of Twidere.
 *  Copyright (C) 2012-2016 Mariotaku Lee <mariotaku.lee@gmail.com>
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

package de.vanita5.twittnuker.service

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.job.JobParameters
import android.app.job.JobService
import android.os.Build
import android.util.Log
import org.mariotaku.abstask.library.TaskStarter
import org.mariotaku.kpreferences.KPreferences
import de.vanita5.twittnuker.BuildConfig
import de.vanita5.twittnuker.TwittnukerConstants.LOGTAG
import de.vanita5.twittnuker.annotation.AutoRefreshType
import de.vanita5.twittnuker.constant.stopAutoRefreshWhenBatteryLowKey
import de.vanita5.twittnuker.util.Utils
import de.vanita5.twittnuker.util.dagger.GeneralComponentHelper
import javax.inject.Inject

@SuppressLint("Registered")
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class JobRefreshService : JobService() {

    @Inject
    internal lateinit var preferences: KPreferences

    override fun onCreate() {
        super.onCreate()
        GeneralComponentHelper.build(this).inject(this)
    }

    override fun onStartJob(params: JobParameters): Boolean {
        if (!Utils.isBatteryOkay(this) && preferences[stopAutoRefreshWhenBatteryLowKey]) {
            // Low battery, don't refresh
            return false
        }
        if (BuildConfig.DEBUG) {
            Log.d(LOGTAG, "Running job ${params.jobId}")
        }

        val task = run {
            val type = getRefreshType(params.jobId) ?: return@run null
            return@run RefreshService.createJobTask(this, type)
        } ?: return false
        task.callback = {
            this.jobFinished(params, false)
        }
        TaskStarter.execute(task)
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        return false
    }

    companion object {
        const val ID_REFRESH_HOME_TIMELINE = 1
        const val ID_REFRESH_NOTIFICATIONS = 2
        const val ID_REFRESH_DIRECT_MESSAGES = 3

        fun getJobId(@AutoRefreshType type: String): Int = when (type) {
            AutoRefreshType.HOME_TIMELINE -> JobRefreshService.ID_REFRESH_HOME_TIMELINE
            AutoRefreshType.INTERACTIONS_TIMELINE -> JobRefreshService.ID_REFRESH_NOTIFICATIONS
            AutoRefreshType.DIRECT_MESSAGES -> JobRefreshService.ID_REFRESH_DIRECT_MESSAGES
            else -> 0
        }

        fun getRefreshType(jobId: Int): String? = when (jobId) {
            JobRefreshService.ID_REFRESH_HOME_TIMELINE -> AutoRefreshType.HOME_TIMELINE
            JobRefreshService.ID_REFRESH_NOTIFICATIONS -> AutoRefreshType.INTERACTIONS_TIMELINE
            JobRefreshService.ID_REFRESH_DIRECT_MESSAGES -> AutoRefreshType.DIRECT_MESSAGES
            else -> null
        }
    }
}