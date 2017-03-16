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

package de.vanita5.twittnuker.service

import android.accounts.AccountManager
import android.accounts.OnAccountsUpdateListener
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.support.v4.app.NotificationCompat
import android.support.v4.net.ConnectivityManagerCompat
import org.apache.commons.lang3.concurrent.BasicThreadFactory
import org.mariotaku.abstask.library.TaskStarter
import org.mariotaku.kpreferences.get
import org.mariotaku.ktextension.addOnAccountsUpdatedListenerSafe
import org.mariotaku.ktextension.removeOnAccountsUpdatedListenerSafe
import org.mariotaku.library.objectcursor.ObjectCursor
import de.vanita5.twittnuker.library.MicroBlogException
import de.vanita5.twittnuker.library.twitter.TwitterUserStream
import de.vanita5.twittnuker.library.twitter.annotation.StreamWith
import de.vanita5.twittnuker.library.twitter.model.Activity
import de.vanita5.twittnuker.library.twitter.model.DirectMessage
import de.vanita5.twittnuker.library.twitter.model.Status
import de.vanita5.twittnuker.R
import de.vanita5.twittnuker.TwittnukerConstants.LOGTAG
import de.vanita5.twittnuker.annotation.AccountType
import de.vanita5.twittnuker.constant.streamingNonMeteredNetworkKey
import de.vanita5.twittnuker.constant.streamingPowerSavingKey
import de.vanita5.twittnuker.extension.model.isOfficial
import de.vanita5.twittnuker.extension.model.isStreamingSupported
import de.vanita5.twittnuker.extension.model.newMicroBlogInstance
import de.vanita5.twittnuker.model.*
import de.vanita5.twittnuker.model.util.AccountUtils
import de.vanita5.twittnuker.model.util.ParcelableActivityUtils
import de.vanita5.twittnuker.model.util.ParcelableStatusUtils
import de.vanita5.twittnuker.provider.TwidereDataStore.Activities
import de.vanita5.twittnuker.provider.TwidereDataStore.Statuses
import de.vanita5.twittnuker.task.twitter.GetActivitiesAboutMeTask
import de.vanita5.twittnuker.task.twitter.message.GetMessagesTask
import de.vanita5.twittnuker.util.DataStoreUtils
import de.vanita5.twittnuker.util.DebugLog
import de.vanita5.twittnuker.util.IntentUtils
import de.vanita5.twittnuker.util.Utils
import de.vanita5.twittnuker.util.dagger.GeneralComponentHelper
import de.vanita5.twittnuker.util.streaming.TwitterTimelineStreamCallback
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class StreamingService : BaseService() {

    internal lateinit var threadPoolExecutor: ExecutorService
    internal lateinit var handler: Handler

    private val submittedTasks = WeakHashMap<UserKey, StreamingRunnable<*>>()

    private val accountChangeObserver = OnAccountsUpdateListener {
        setupStreaming()
    }

    override fun onCreate() {
        super.onCreate()
        GeneralComponentHelper.build(this).inject(this)
        threadPoolExecutor = Executors.newCachedThreadPool(BasicThreadFactory.Builder()
                .namingPattern("twittnuker-streaming-%d")
                .priority(Thread.NORM_PRIORITY - 1).build())
        handler = Handler(Looper.getMainLooper())
        AccountManager.get(this).addOnAccountsUpdatedListenerSafe(accountChangeObserver, updateImmediately = false)
    }

    override fun onDestroy() {
        submittedTasks.forEach { _, future ->
            future.cancel()
        }
        threadPoolExecutor.shutdown()
        submittedTasks.clear()
        removeNotification()
        AccountManager.get(this).removeOnAccountsUpdatedListenerSafe(accountChangeObserver)
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (setupStreaming()) {
            return START_STICKY
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent) = throw UnsupportedOperationException()

    private fun setupStreaming(): Boolean {
        val isNetworkMetered = ConnectivityManagerCompat.isActiveNetworkMetered(connectivityManager)
        if (preferences[streamingNonMeteredNetworkKey] && isNetworkMetered) {
            stopSelf()
            return false
        }
        val isCharging = Utils.isCharging(this)
        if (preferences[streamingPowerSavingKey] && !isCharging) {
            stopSelf()
            return false
        }
        if (updateStreamingInstances()) {
            showNotification()
            return true
        } else {
            stopSelf()
            return false
        }
    }

    private fun updateStreamingInstances(): Boolean {
        val am = AccountManager.get(this)
        val supportedAccounts = AccountUtils.getAllAccountDetails(am, true).filter { it.isStreamingSupported }
        val enabledPrefs = supportedAccounts.map { AccountPreferences(this, it.key) }
        val enabledAccounts = supportedAccounts.filter { account ->
            return@filter enabledPrefs.any {
                account.key == it.accountKey && it.isStreamingEnabled
            }
        }

        if (enabledAccounts.isEmpty()) return false

        // Remove all disabled instances
        submittedTasks.forEach { k, v ->
            if (enabledAccounts.none { k == it.key } && !v.cancelled) {
                v.cancel()
            }
        }
        // Add instances if not running
        enabledAccounts.forEach { account ->
            val existing = submittedTasks[account.key]
            if (existing == null || existing.cancelled) {
                val runnable = account.newStreamingRunnable() ?: return@forEach
                threadPoolExecutor.submit(runnable)
                submittedTasks[account.key] = runnable
            }
        }
        return true
    }

    private fun showNotification() {
        val intent = IntentUtils.settings("streaming")
        val contentIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        val contentTitle = getString(R.string.app_name)
        val contentText = getString(R.string.timeline_streaming_running)
        val builder = NotificationCompat.Builder(this)
        builder.setOngoing(true)
        builder.setSmallIcon(R.drawable.ic_stat_twittnuker)
        builder.setContentTitle(contentTitle)
        builder.setContentText(contentText)
//            builder.setTicker(getString(R.string.streaming_service_running))
        builder.setContentIntent(contentIntent)
        builder.setCategory(NotificationCompat.CATEGORY_STATUS)
        builder.priority = NotificationCompat.PRIORITY_MIN
        startForeground(NOTIFICATION_SERVICE_STARTED, builder.build())
    }

    private fun removeNotification() {
        stopForeground(true)
    }

    private fun buildNotification() {

    }

    private fun AccountDetails.newStreamingRunnable(): StreamingRunnable<*>? {
        when (type) {
            AccountType.TWITTER -> {
                return TwitterStreamingRunnable(this@StreamingService, handler, this)
            }
        }
        return null
    }

    internal abstract class StreamingRunnable<T>(val context: Context, val account: AccountDetails) : Runnable {

        var cancelled: Boolean = false
            private set

        override fun run() {
            val instance = createStreamingInstance()
            while (!cancelled && !Thread.currentThread().isInterrupted) {
                try {
                    instance.beginStreaming()
                } catch (e: MicroBlogException) {
                    DebugLog.w(LOGTAG, msg = "Can't stream for ${account.key}", tr = e)
                }
                Thread.sleep(TimeUnit.MINUTES.toMillis(1))
            }
        }

        fun cancel(): Boolean {
            if (cancelled) return false
            cancelled = true
            onCancelled()
            return true
        }

        abstract fun createStreamingInstance(): T

        abstract fun T.beginStreaming()

        abstract fun onCancelled()
    }

    internal class TwitterStreamingRunnable(context: Context, val handler: Handler, account: AccountDetails) :
            StreamingRunnable<TwitterUserStream>(context, account) {

        private val profileImageSize = context.getString(R.string.profile_image_size)
        private val isOfficial = account.isOfficial(context)

        private var canGetInteractions: Boolean = true
        private var canGetMessages: Boolean = true

        private val interactionsTimeoutRunnable = Runnable {
            canGetInteractions = true
        }

        private val messagesTimeoutRunnable = Runnable {
            canGetMessages = true
        }

        val callback = object : TwitterTimelineStreamCallback(account.key.id) {

            private var lastStatusTimestamps = LongArray(2)

            private var homeInsertGap = false
            private var interactionsInsertGap = false

            override fun onConnected(): Boolean {
                homeInsertGap = true
                interactionsInsertGap = true
                return true
            }

            override fun onHomeTimeline(status: Status): Boolean {
                val parcelableStatus = ParcelableStatusUtils.fromStatus(status, account.key,
                        homeInsertGap, profileImageSize)

                val currentTimeMillis = System.currentTimeMillis()
                if (lastStatusTimestamps[0] >= parcelableStatus.timestamp) {
                    val extraValue = (currentTimeMillis - lastStatusTimestamps[1]).coerceAtMost(499)
                    parcelableStatus.position_key = parcelableStatus.timestamp + extraValue
                } else {
                    parcelableStatus.position_key = parcelableStatus.timestamp
                }
                parcelableStatus.inserted_date = currentTimeMillis

                lastStatusTimestamps[0] = parcelableStatus.position_key
                lastStatusTimestamps[1] = parcelableStatus.inserted_date

                val values = ObjectCursor.valuesCreatorFrom(ParcelableStatus::class.java)
                        .create(parcelableStatus)
                context.contentResolver.insert(Statuses.CONTENT_URI, values)
                homeInsertGap = false
                return true
            }

            override fun onActivityAboutMe(activity: Activity): Boolean {
                if (isOfficial) {
                    // Wait for 30 seconds to avoid rate limit
                    if (canGetInteractions) {
                        getInteractions()
                        canGetInteractions = false
                        handler.postDelayed(interactionsTimeoutRunnable, TimeUnit.SECONDS.toMillis(30))
                    }
                } else {
                    val parcelableActivity = ParcelableActivityUtils.fromActivity(activity,
                            account.key, interactionsInsertGap, profileImageSize)
                    parcelableActivity.position_key = parcelableActivity.timestamp
                    val values = ObjectCursor.valuesCreatorFrom(ParcelableActivity::class.java)
                            .create(parcelableActivity)
                    context.contentResolver.insert(Activities.AboutMe.CONTENT_URI, values)
                    interactionsInsertGap = false
                }
                return true
            }

            override fun onDirectMessage(directMessage: DirectMessage): Boolean {
                if (canGetMessages) {
                    getMessages()
                    canGetMessages = false
                    val timeout = TimeUnit.SECONDS.toMillis(if (isOfficial) 30 else 90)
                    handler.postDelayed(messagesTimeoutRunnable, timeout)
                }
                return true
            }

            override fun onException(ex: Throwable): Boolean {
                DebugLog.w(LOGTAG, msg = "Exception for ${account.key}", tr = ex)
                return true
            }

            private fun getInteractions() {
                val task = GetActivitiesAboutMeTask(context)
                task.params = object : SimpleRefreshTaskParam() {
                    override val accountKeys: Array<UserKey> = arrayOf(account.key)

                    override val sinceIds: Array<String?>?
                        get() = DataStoreUtils.getNewestActivityMaxPositions(context,
                                Activities.AboutMe.CONTENT_URI, arrayOf(account.key))

                    override val sinceSortIds: LongArray?
                        get() = DataStoreUtils.getNewestActivityMaxSortPositions(context,
                                Activities.AboutMe.CONTENT_URI, arrayOf(account.key))

                    override val hasSinceIds: Boolean = true

                }
                TaskStarter.execute(task)
            }

            private fun getMessages() {
                val task = GetMessagesTask(context)
                task.params = object : GetMessagesTask.RefreshMessagesTaskParam(context) {
                    override val accountKeys: Array<UserKey> = arrayOf(account.key)

                    override val hasSinceIds: Boolean = true
                }
                TaskStarter.execute(task)
            }
        }

        override fun createStreamingInstance(): TwitterUserStream {
            return account.newMicroBlogInstance(context, cls = TwitterUserStream::class.java)
        }

        override fun TwitterUserStream.beginStreaming() {
            getUserStream(StreamWith.USER, callback)
        }

        override fun onCancelled() {
            callback.disconnect()
        }

    }

    companion object {

        private val NOTIFICATION_SERVICE_STARTED = 1

    }

}
