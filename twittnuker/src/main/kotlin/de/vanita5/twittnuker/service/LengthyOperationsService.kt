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
import android.annotation.SuppressLint
import android.app.Notification
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.BaseColumns
import android.support.annotation.UiThread
import android.support.annotation.WorkerThread
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationCompat.Builder
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import nl.komponents.kovenant.task
import nl.komponents.kovenant.ui.successUi
import org.mariotaku.abstask.library.AbstractTask
import org.mariotaku.abstask.library.ManualTaskStarter
import org.mariotaku.ktextension.configure
import org.mariotaku.ktextension.toLong
import org.mariotaku.ktextension.toTypedArray
import org.mariotaku.ktextension.useCursor
import org.mariotaku.library.objectcursor.ObjectCursor
import de.vanita5.twittnuker.library.MicroBlogException
import de.vanita5.twittnuker.library.twitter.TwitterUpload
import de.vanita5.twittnuker.library.twitter.model.MediaUploadResponse
import de.vanita5.twittnuker.library.twitter.model.MediaUploadResponse.ProcessingInfo
import org.mariotaku.restfu.http.ContentType
import org.mariotaku.restfu.http.mime.Body
import org.mariotaku.restfu.http.mime.SimpleBody
import org.mariotaku.sqliteqb.library.Expression
import de.vanita5.twittnuker.R
import de.vanita5.twittnuker.TwittnukerConstants.*
import de.vanita5.twittnuker.model.*
import de.vanita5.twittnuker.model.draft.SendDirectMessageActionExtras
import de.vanita5.twittnuker.model.draft.StatusObjectExtras
import de.vanita5.twittnuker.model.schedule.ScheduleInfo
import de.vanita5.twittnuker.model.util.AccountUtils
import de.vanita5.twittnuker.model.util.ParcelableStatusUpdateUtils
import de.vanita5.twittnuker.provider.TwidereDataStore.Drafts
import de.vanita5.twittnuker.task.CreateFavoriteTask
import de.vanita5.twittnuker.task.RetweetStatusTask
import de.vanita5.twittnuker.task.twitter.UpdateStatusTask
import de.vanita5.twittnuker.task.twitter.message.SendMessageTask
import de.vanita5.twittnuker.util.NotificationManagerWrapper
import de.vanita5.twittnuker.util.Utils
import de.vanita5.twittnuker.util.deleteDrafts
import de.vanita5.twittnuker.util.io.ContentLengthInputStream.ReadListener
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Intent service for lengthy operations like update status/send DM.
 */
class LengthyOperationsService : BaseIntentService("lengthy_operations") {

    private val handler: Handler by lazy { Handler(Looper.getMainLooper()) }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return Service.START_STICKY
    }

    override fun onHandleIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action ?: return
        when (action) {
            INTENT_ACTION_UPDATE_STATUS -> {
                handleUpdateStatusIntent(intent)
            }
            INTENT_ACTION_SCHEDULE_STATUS -> {
                handleScheduleStatusIntent(intent)
            }
            INTENT_ACTION_SEND_DIRECT_MESSAGE -> {
                handleSendDirectMessageIntent(intent)
            }
            INTENT_ACTION_DISCARD_DRAFT -> {
                handleDiscardDraftIntent(intent)
            }
            INTENT_ACTION_SEND_DRAFT -> {
                handleSendDraftIntent(intent)
            }
        }
    }

    private fun showErrorMessage(actionRes: Int, e: Exception?, longMessage: Boolean) {
        handler.post { Utils.showErrorMessage(this@LengthyOperationsService, actionRes, e, longMessage) }
    }

    private fun showOkMessage(message: Int, longMessage: Boolean) {
        handler.post { Toast.makeText(this@LengthyOperationsService, message, if (longMessage) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show() }
    }

    private fun handleSendDraftIntent(intent: Intent) {
        val uri = intent.data ?: return
        notificationManager.cancel(uri.toString(), NOTIFICATION_ID_DRAFTS)
        val draftId = uri.lastPathSegment.toLong(-1)
        if (draftId == -1L) return
        val where = Expression.equals(Drafts._ID, draftId)
        @SuppressLint("Recycle")
        val draft: Draft = contentResolver.query(Drafts.CONTENT_URI, Drafts.COLUMNS, where.sql, null, null)?.useCursor {
            val i = ObjectCursor.indicesFrom(it, Draft::class.java)
            if (!it.moveToFirst()) return@useCursor null
            return@useCursor i.newObject(it)
        } ?: return

        contentResolver.delete(Drafts.CONTENT_URI, where.sql, null)
        if (TextUtils.isEmpty(draft.action_type)) {
            draft.action_type = Draft.Action.UPDATE_STATUS
        }
        when (draft.action_type) {
            Draft.Action.UPDATE_STATUS_COMPAT_1, Draft.Action.UPDATE_STATUS_COMPAT_2,
            Draft.Action.UPDATE_STATUS, Draft.Action.REPLY, Draft.Action.QUOTE -> {
                updateStatuses(ParcelableStatusUpdateUtils.fromDraftItem(this, draft))
            }
            Draft.Action.SEND_DIRECT_MESSAGE_COMPAT, Draft.Action.SEND_DIRECT_MESSAGE -> {
                val extras = draft.action_extras as? SendDirectMessageActionExtras ?: return
                val message = ParcelableNewMessage().apply {
                    this.account = draft.account_keys?.firstOrNull()?.let { key ->
                        val am = AccountManager.get(this@LengthyOperationsService)
                        return@let AccountUtils.getAccountDetails(am, key, true)
                    }
                    this.text = draft.text
                    this.media = draft.media
                    this.recipient_ids = extras.recipientIds
                    this.conversation_id = extras.conversationId
                }
                sendMessage(message)
            }
            Draft.Action.FAVORITE -> {
                performStatusAction(draft) { accountKey, status ->
                    CreateFavoriteTask(this, accountKey, status)
                }
            }
            Draft.Action.RETWEET -> {
                performStatusAction(draft) { accountKey, status ->
                    RetweetStatusTask(this, accountKey, status)
                }
            }
        }
    }

    @SuppressLint("Recycle")
    private fun handleDiscardDraftIntent(intent: Intent) {
        val data = intent.data ?: return
        task {
            if (deleteDrafts(this, longArrayOf(data.lastPathSegment.toLong(-1))) < 1) {
                throw IOException()
            }
            return@task data
        }.successUi { uri ->
            notificationManager.cancel(uri.toString(), NOTIFICATION_ID_DRAFTS)
        }
    }

    private fun handleSendDirectMessageIntent(intent: Intent) {
        val message = intent.getParcelableExtra<ParcelableNewMessage>(EXTRA_MESSAGE) ?: return
        sendMessage(message)
    }

    private fun sendMessage(message: ParcelableNewMessage) {
        val title = getString(R.string.sending_direct_message)
        val builder = Builder(this)
        builder.setSmallIcon(R.drawable.ic_stat_send)
        builder.setProgress(100, 0, true)
        builder.setTicker(title)
        builder.setContentTitle(title)
        builder.setContentText(message.text)
        builder.setCategory(NotificationCompat.CATEGORY_PROGRESS)
        builder.setOngoing(true)
        val notification = builder.build()
        startForeground(NOTIFICATION_ID_SEND_DIRECT_MESSAGE, notification)
        val task = SendMessageTask(this)
        task.params = message
        invokeBeforeExecute(task)
        val result = ManualTaskStarter.invokeExecute(task)
        invokeAfterExecute(task, result)

        if (result.hasData()) {
            showOkMessage(R.string.message_direct_message_sent, false)
        } else {
            UpdateStatusTask.saveDraft(this, Draft.Action.SEND_DIRECT_MESSAGE) {
                account_keys = arrayOf(message.account.key)
                text = message.text
                media = message.media
                action_extras = SendDirectMessageActionExtras().apply {
                    recipientIds = message.recipient_ids
                    conversationId = message.conversation_id
                }
            }
            showErrorMessage(R.string.action_sending_direct_message, result.exception, true)
        }
        stopForeground(false)
        notificationManager.cancel(NOTIFICATION_ID_SEND_DIRECT_MESSAGE)
    }

    private fun handleUpdateStatusIntent(intent: Intent) {
        val status = intent.getParcelableExtra<ParcelableStatusUpdate>(EXTRA_STATUS)
        val statusParcelables = intent.getParcelableArrayExtra(EXTRA_STATUSES)
        val statuses: Array<ParcelableStatusUpdate>
        if (statusParcelables != null) {
            statuses = statusParcelables.toTypedArray(ParcelableStatusUpdate.CREATOR)
        } else if (status != null) {
            statuses = arrayOf(status)
        } else
            return
        @Draft.Action
        val actionType = intent.getStringExtra(EXTRA_ACTION)
        statuses.forEach { it.draft_action = actionType }
        updateStatuses(*statuses)
    }

    private fun handleScheduleStatusIntent(intent: Intent) {
        val status = intent.getParcelableExtra<ParcelableStatusUpdate>(EXTRA_STATUS)
        val scheduleInfo = intent.getParcelableExtra<ScheduleInfo>(EXTRA_SCHEDULE_INFO)
        @Draft.Action
        val actionType = intent.getStringExtra(EXTRA_ACTION)
        status.draft_action = actionType

    }

    private fun updateStatuses(vararg statuses: ParcelableStatusUpdate) {
        val context = this
        val builder = Builder(context)
        startForeground(NOTIFICATION_ID_UPDATE_STATUS, updateUpdateStatusNotification(context,
                builder, 0, null))
        for (item in statuses) {
            val task = UpdateStatusTask(context, object : UpdateStatusTask.StateCallback {

                @WorkerThread
                override fun onStartUploadingMedia() {
                    startForeground(NOTIFICATION_ID_UPDATE_STATUS, updateUpdateStatusNotification(context,
                            builder, 0, item))
                }

                @WorkerThread
                override fun onUploadingProgressChanged(index: Int, current: Long, total: Long) {
                    val progress = (current * 100 / total).toInt()
                    startForeground(NOTIFICATION_ID_UPDATE_STATUS, updateUpdateStatusNotification(context,
                            builder, progress, item))
                }

                @WorkerThread
                override fun onShorteningStatus() {
                    startForeground(NOTIFICATION_ID_UPDATE_STATUS, updateUpdateStatusNotification(context,
                            builder, 0, item))
                }

                @WorkerThread
                override fun onUpdatingStatus() {
                    startForeground(NOTIFICATION_ID_UPDATE_STATUS, updateUpdateStatusNotification(context,
                            builder, 0, item))
                }

                @UiThread
                override fun afterExecute(result: UpdateStatusTask.UpdateStatusResult) {
                    var failed = false
                    val exception = result.exception
                    val exceptions = result.exceptions
                    if (exception != null) {
                        val cause = exception.cause
                        if (cause is MicroBlogException) {
                            Toast.makeText(context, cause.errors?.firstOrNull()?.message ?: cause.message,
                                    Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, exception.message, Toast.LENGTH_SHORT).show()
                        }
                        failed = true
                        Log.w(LOGTAG, exception)
                    } else for (e in exceptions) {
                        if (e != null) {
                            // Show error
                            var errorMessage = Utils.getErrorMessage(context, e)
                            if (TextUtils.isEmpty(errorMessage)) {
                                errorMessage = context.getString(R.string.status_not_updated)
                            }
                            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                            failed = true
                            break
                        }
                    }
                    if (!failed) {
                        Toast.makeText(context, R.string.message_toast_status_updated, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun beforeExecute() {

                }
            })
            task.callback = this
            task.params = item
            invokeBeforeExecute(task)

            val result = ManualTaskStarter.invokeExecute(task)
            invokeAfterExecute(task, result)

            if (!result.succeed) {
                contentResolver.insert(Drafts.CONTENT_URI_NOTIFICATIONS, configure(ContentValues()) {
                    put(BaseColumns._ID, result.draftId)
                })
            }
        }
        if (preferences.getBoolean(KEY_REFRESH_AFTER_TWEET)) {
            handler.post { twitterWrapper.refreshAll() }
        }
        stopForeground(false)
        notificationManager.cancel(NOTIFICATION_ID_UPDATE_STATUS)
    }


    @Throws(IOException::class, MicroBlogException::class)
    private fun uploadMedia(upload: TwitterUpload, body: Body): MediaUploadResponse {
        val mediaType = body.contentType().contentType
        val length = body.length()
        val stream = body.stream()
        var response = upload.initUploadMedia(mediaType, length, null)
        val segments = if (length == 0L) 0 else (length / BULK_SIZE + 1).toInt()
        for (segmentIndex in 0..segments - 1) {
            val currentBulkSize = Math.min(BULK_SIZE, length - segmentIndex * BULK_SIZE).toInt()
            val bulk = SimpleBody(ContentType.OCTET_STREAM, null, currentBulkSize.toLong(),
                    stream)
            upload.appendUploadMedia(response.id, segmentIndex, bulk)
        }
        response = upload.finalizeUploadMedia(response.id)
        run {
            var info: ProcessingInfo? = response.processingInfo
            while (info != null && shouldWaitForProcess(info)) {
                val checkAfterSecs = info.checkAfterSecs
                if (checkAfterSecs <= 0) {
                    break
                }
                try {
                    Thread.sleep(TimeUnit.SECONDS.toMillis(checkAfterSecs))
                } catch (e: InterruptedException) {
                    break
                }

                response = upload.getUploadMediaStatus(response.id)
                info = response.processingInfo
            }
        }
        val info = response.processingInfo
        if (info != null && ProcessingInfo.State.FAILED == info.state) {
            val exception = MicroBlogException()
            val errorInfo = info.error
            if (errorInfo != null) {
                exception.errors = arrayOf(errorInfo)
            }
            throw exception
        }
        return response
    }

    private fun shouldWaitForProcess(info: ProcessingInfo): Boolean {
        when (info.state) {
            ProcessingInfo.State.PENDING, ProcessingInfo.State.IN_PROGRESS -> return true
            else -> return false
        }
    }

    private fun <T> performStatusAction(draft: Draft, action: (accountKey: UserKey, status: ParcelableStatus) -> AbstractTask<*, T, *>): Boolean {
        val accountKey = draft.account_keys?.firstOrNull() ?: return false
        val status = (draft.action_extras as? StatusObjectExtras)?.status ?: return false
        val task = action(accountKey, status)
        invokeBeforeExecute(task)
        val result = ManualTaskStarter.invokeExecute(task)
        invokeAfterExecute(task, result)
        return true
        }

    private fun invokeBeforeExecute(task: AbstractTask<*, *, *>) {
        handler.post { ManualTaskStarter.invokeBeforeExecute(task) }
    }

    private fun <T> invokeAfterExecute(task: AbstractTask<*, T, *>, result: T) {
        handler.post { ManualTaskStarter.invokeAfterExecute(task, result) }
    }

    internal class MessageMediaUploadListener(private val context: Context, private val manager: NotificationManagerWrapper,
                                              builder: NotificationCompat.Builder, private val message: String) : ReadListener {

        var percent: Int = 0

        private val builder: Builder

        init {
            this.builder = builder
        }

        override fun onRead(length: Long, position: Long) {
            val percent = if (length > 0) (position * 100 / length).toInt() else 0
            if (this.percent != percent) {
                manager.notify(NOTIFICATION_ID_SEND_DIRECT_MESSAGE,
                        updateSendDirectMessageNotification(context, builder, percent, message))
            }
            this.percent = percent
        }
    }

    companion object {
        private val BULK_SIZE = (128 * 1024).toLong() // 128KiB

        private fun updateSendDirectMessageNotification(context: Context,
                                                        builder: NotificationCompat.Builder,
                                                        progress: Int, message: String?): Notification {
            builder.setContentTitle(context.getString(R.string.sending_direct_message))
            if (message != null) {
                builder.setContentText(message)
            }
            builder.setSmallIcon(R.drawable.ic_stat_send)
            builder.setProgress(100, progress, progress >= 100 || progress <= 0)
            builder.setOngoing(true)
            return builder.build()
        }

        private fun updateUpdateStatusNotification(context: Context,
                                                   builder: NotificationCompat.Builder,
                                                   progress: Int,
                                                   status: ParcelableStatusUpdate?): Notification {
            builder.setContentTitle(context.getString(R.string.updating_status_notification))
            if (status != null) {
                builder.setContentText(status.text)
            }
            builder.setSmallIcon(R.drawable.ic_stat_send)
            builder.setProgress(100, progress, progress >= 100 || progress <= 0)
            builder.setOngoing(true)
            return builder.build()
        }

        fun updateStatusesAsync(context: Context, @Draft.Action action: String,
                                vararg statuses: ParcelableStatusUpdate) {
            val intent = Intent(context, LengthyOperationsService::class.java)
            intent.action = INTENT_ACTION_UPDATE_STATUS
            intent.putExtra(EXTRA_STATUSES, statuses)
            intent.putExtra(EXTRA_ACTION, action)
            context.startService(intent)
        }

        fun scheduleStatus(context: Context, @Draft.Action action: String,
                status: ParcelableStatusUpdate, scheduleInfo: ScheduleInfo) {
            val intent = Intent(context, LengthyOperationsService::class.java)
            intent.action = INTENT_ACTION_SCHEDULE_STATUS
            intent.putExtra(EXTRA_STATUS, status)
            intent.putExtra(EXTRA_SCHEDULE_INFO, scheduleInfo)
            intent.putExtra(EXTRA_ACTION, action)
            context.startService(intent)
        }

        fun sendMessageAsync(context: Context, message: ParcelableNewMessage) {
            val intent = Intent(context, LengthyOperationsService::class.java)
            intent.action = INTENT_ACTION_SEND_DIRECT_MESSAGE
            intent.putExtra(EXTRA_MESSAGE, message)
            context.startService(intent)
        }
    }

}