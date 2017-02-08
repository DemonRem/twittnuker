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

package de.vanita5.twittnuker.service

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.job.JobParameters
import android.app.job.JobService
import android.os.Build
import org.mariotaku.kpreferences.KPreferences
import de.vanita5.twittnuker.annotation.AutoRefreshType
import de.vanita5.twittnuker.constant.autoRefreshCompatibilityModeKey
import de.vanita5.twittnuker.util.TaskServiceRunner
import de.vanita5.twittnuker.util.dagger.GeneralComponentHelper
import de.vanita5.twittnuker.util.support.JobServiceSupport
import javax.inject.Inject

@SuppressLint("Registered")
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class JobTaskService : JobService() {

    @Inject
    internal lateinit var taskServiceRunner: TaskServiceRunner
    @Inject
    internal lateinit var kPreferences: KPreferences

    override fun onCreate() {
        super.onCreate()
        GeneralComponentHelper.build(this).inject(this)
    }

    override fun onStartJob(params: JobParameters): Boolean {
        if (kPreferences[autoRefreshCompatibilityModeKey]) return false
        val action = getTaskAction(params.jobId) ?: return false
        return taskServiceRunner.runTask(action) {
            this.jobFinished(params, false)
        }
    }

    override fun onStopJob(params: JobParameters): Boolean {
        try {
            if (JobServiceSupport.handleStopJob(params, false)) {
                JobServiceSupport.removeCallback(params)
            }
        } catch (e: Exception) {
            // Swallow any possible exceptions
        }
        return false
    }

    companion object {
        // DON'T CHANGE JOB ID ONCE CREATED!
        const val JOB_ID_REFRESH_HOME_TIMELINE = 1
        const val JOB_ID_REFRESH_NOTIFICATIONS = 2
        const val JOB_ID_REFRESH_DIRECT_MESSAGES = 3
        const val JOB_ID_REFRESH_FILTERS_SUBSCRIPTIONS = 19
        const val JOB_ID_SYNC_DRAFTS = 21
        const val JOB_ID_SYNC_FILTERS = 22
        const val JOB_ID_SYNC_USER_COLORS = 24

        val JOB_IDS_REFRESH = intArrayOf(JOB_ID_REFRESH_HOME_TIMELINE, JOB_ID_REFRESH_NOTIFICATIONS,
                JOB_ID_REFRESH_DIRECT_MESSAGES)
        val JOB_IDS_SYNC = intArrayOf(JOB_ID_SYNC_DRAFTS, JOB_ID_SYNC_FILTERS,
                JOB_ID_SYNC_USER_COLORS)

        fun getRefreshJobId(@AutoRefreshType type: String): Int = when (type) {
            AutoRefreshType.HOME_TIMELINE -> JOB_ID_REFRESH_HOME_TIMELINE
            AutoRefreshType.INTERACTIONS_TIMELINE -> JOB_ID_REFRESH_NOTIFICATIONS
            AutoRefreshType.DIRECT_MESSAGES -> JOB_ID_REFRESH_DIRECT_MESSAGES
            else -> 0
        }

        @TaskServiceRunner.Action
        fun getTaskAction(jobId: Int): String? = when (jobId) {
            JOB_ID_REFRESH_HOME_TIMELINE -> TaskServiceRunner.ACTION_REFRESH_HOME_TIMELINE
            JOB_ID_REFRESH_NOTIFICATIONS -> TaskServiceRunner.ACTION_REFRESH_NOTIFICATIONS
            JOB_ID_REFRESH_DIRECT_MESSAGES -> TaskServiceRunner.ACTION_REFRESH_DIRECT_MESSAGES
            JOB_ID_REFRESH_FILTERS_SUBSCRIPTIONS -> TaskServiceRunner.ACTION_REFRESH_FILTERS_SUBSCRIPTIONS
            JOB_ID_SYNC_DRAFTS -> TaskServiceRunner.ACTION_SYNC_DRAFTS
            JOB_ID_SYNC_FILTERS -> TaskServiceRunner.ACTION_SYNC_FILTERS
            JOB_ID_SYNC_USER_COLORS -> TaskServiceRunner.ACTION_SYNC_USER_COLORS
            else -> null
        }
    }
}