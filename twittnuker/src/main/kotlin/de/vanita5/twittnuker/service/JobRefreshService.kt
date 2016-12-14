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
import org.mariotaku.abstask.library.AbstractTask
import org.mariotaku.abstask.library.TaskStarter
import org.mariotaku.kpreferences.KPreferences
import de.vanita5.twittnuker.constant.IntentConstants.*
import de.vanita5.twittnuker.constant.stopAutoRefreshWhenBatteryLowKey
import de.vanita5.twittnuker.model.AccountPreferences
import de.vanita5.twittnuker.provider.TwidereDataStore.*
import de.vanita5.twittnuker.task.GetActivitiesAboutMeTask
import de.vanita5.twittnuker.task.GetHomeTimelineTask
import de.vanita5.twittnuker.task.GetReceivedDirectMessagesTask
import de.vanita5.twittnuker.util.DataStoreUtils
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

        val task = createJobTask(params) ?: return false
        task.callback = {
            this.jobFinished(params, true)
        }
        TaskStarter.execute(task)
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        return false
    }

    internal fun createJobTask(params: JobParameters): AbstractTask<*, *, () -> Unit>? {
        when (params.extras?.getString(EXTRA_ACTION)) {
            BROADCAST_REFRESH_HOME_TIMELINE -> {
                val task = GetHomeTimelineTask(this)
                task.params = RefreshService.AutoRefreshTaskParam(this, AccountPreferences::isAutoRefreshHomeTimelineEnabled) { accountKeys ->
                    DataStoreUtils.getNewestStatusIds(this, Statuses.CONTENT_URI, accountKeys)
                }
                return task
            }
            BROADCAST_REFRESH_NOTIFICATIONS -> {
                val task = GetActivitiesAboutMeTask(this)
                task.params = RefreshService.AutoRefreshTaskParam(this, AccountPreferences::isAutoRefreshMentionsEnabled) { accountKeys ->
                    DataStoreUtils.getNewestActivityMaxPositions(this, Activities.AboutMe.CONTENT_URI, accountKeys)
                }
                return task
            }
            BROADCAST_REFRESH_DIRECT_MESSAGES -> {
                val task = GetReceivedDirectMessagesTask(this)
                task.params = RefreshService.AutoRefreshTaskParam(this, AccountPreferences::isAutoRefreshDirectMessagesEnabled) { accountKeys ->
                    DataStoreUtils.getNewestMessageIds(this, DirectMessages.Inbox.CONTENT_URI, accountKeys)
                }
                return task
            }
        }
        return null
    }
}