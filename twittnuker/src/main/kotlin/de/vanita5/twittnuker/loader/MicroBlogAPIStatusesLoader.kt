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

package de.vanita5.twittnuker.loader

import android.accounts.AccountManager
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.support.annotation.WorkerThread
import android.util.Log
import com.bluelinelabs.logansquare.LoganSquare
import com.nostra13.universalimageloader.cache.disc.DiskCache
import org.mariotaku.kpreferences.get
import de.vanita5.twittnuker.library.MicroBlog
import de.vanita5.twittnuker.library.MicroBlogException
import de.vanita5.twittnuker.library.twitter.model.Paging
import de.vanita5.twittnuker.library.twitter.model.Status
import de.vanita5.twittnuker.BuildConfig
import de.vanita5.twittnuker.TwittnukerConstants.*
import de.vanita5.twittnuker.app.TwittnukerApplication
import de.vanita5.twittnuker.constant.loadItemLimitKey
import de.vanita5.twittnuker.extension.model.newMicroBlogInstance
import de.vanita5.twittnuker.model.AccountDetails
import de.vanita5.twittnuker.model.ListResponse
import de.vanita5.twittnuker.model.ParcelableStatus
import de.vanita5.twittnuker.model.UserKey
import de.vanita5.twittnuker.model.util.AccountUtils
import de.vanita5.twittnuker.model.util.ParcelableStatusUtils
import de.vanita5.twittnuker.task.twitter.GetStatusesTask
import de.vanita5.twittnuker.util.*
import de.vanita5.twittnuker.util.dagger.GeneralComponentHelper
import java.io.IOException
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject

abstract class MicroBlogAPIStatusesLoader(
        context: Context,
        val accountKey: UserKey?,
        val sinceId: String?,
        val maxId: String?,
        val page: Int,
        adapterData: List<ParcelableStatus>?,
        private val savedStatusesArgs: Array<String>?,
        tabPosition: Int,
        fromUser: Boolean,
        private val loadingMore: Boolean
) : ParcelableStatusesLoader(context, adapterData, tabPosition, fromUser) {
    // Statuses sorted descending by default
    var comparator: Comparator<ParcelableStatus>? = ParcelableStatus.REVERSE_COMPARATOR
    private val exceptionRef = AtomicReference<MicroBlogException?>()

    var exception: MicroBlogException?
        get() = exceptionRef.get()
        private set(value) {
            exceptionRef.set(value)
        }
    @Inject
    lateinit var fileCache: DiskCache
    @Inject
    lateinit var preferences: SharedPreferencesWrapper
    @Inject
    lateinit var userColorNameManager: UserColorNameManager

    init {
        GeneralComponentHelper.build(context).inject(this)
    }

    @SuppressWarnings("unchecked")
    override fun loadInBackground(): ListResponse<ParcelableStatus> {
        val context = context
        val accountKey = accountKey ?: return ListResponse.getListInstance<ParcelableStatus>(MicroBlogException("No Account"))
        val details = AccountUtils.getAccountDetails(AccountManager.get(context), accountKey, true) ?:
                return ListResponse.getListInstance<ParcelableStatus>(MicroBlogException("No Account"))

        var data: MutableList<ParcelableStatus>? = data
        if (data == null) {
            data = CopyOnWriteArrayList<ParcelableStatus>()
        }
        if (isFirstLoad && tabPosition >= 0) {
            val cached = cachedData
            if (cached != null) {
                data.addAll(cached)
                if (comparator != null) {
                    Collections.sort(data, comparator)
                } else {
                    Collections.sort(data)
                }
                return ListResponse.getListInstance(CopyOnWriteArrayList(data))
            }
        }
        if (!fromUser) return ListResponse.getListInstance(data)
        val microBlog = details.newMicroBlogInstance(context = context, cls = MicroBlog::class.java)
        val statuses: List<Status>
        val noItemsBefore = data.isEmpty()
        val loadItemLimit = preferences.getInt(KEY_LOAD_ITEM_LIMIT, DEFAULT_LOAD_ITEM_LIMIT)
        try {
            val paging = Paging()
            processPaging(details, loadItemLimit, paging)
            statuses = getStatuses(microBlog, details, paging)
        } catch (e: MicroBlogException) {
            // mHandler.post(new ShowErrorRunnable(e));
            exception = e
            if (BuildConfig.DEBUG) {
                Log.w(LOGTAG, e)
            }
            return ListResponse.getListInstance(CopyOnWriteArrayList(data), e)
        }

        val statusIds = arrayOfNulls<String>(statuses.size)
        var minIdx = -1
        var rowsDeleted = 0
        for (i in 0 until statuses.size) {
            val status = statuses[i]
            if (minIdx == -1 || status < statuses[minIdx]) {
                minIdx = i
            }
            statusIds[i] = status.id
            if (deleteStatus(data, status.id)) {
                rowsDeleted++
            }
        }

        // Insert a gap.
        val deletedOldGap = rowsDeleted > 0 && statusIds.contains(maxId)
        val noRowsDeleted = rowsDeleted == 0
        val insertGap = minIdx != -1 && (noRowsDeleted || deletedOldGap) && !noItemsBefore
                && statuses.size >= loadItemLimit && !loadingMore

        if (statuses.isNotEmpty()) {
            val firstSortId = statuses.first().sortId
            val lastSortId = statuses.last().sortId
            // Get id diff of first and last item
            val sortDiff = firstSortId - lastSortId
            for (i in 0 until statuses.size) {
                val status = statuses[i]
                val item = ParcelableStatusUtils.fromStatus(status, accountKey, insertGap && isGapEnabled && minIdx == i)
                item.position_key = GetStatusesTask.getPositionKey(item.timestamp, item.sort_id, lastSortId,
                        sortDiff, i, statuses.size)
                ParcelableStatusUtils.updateExtraInformation(item, details)
                data.add(item)
            }
        }

        val db = TwittnukerApplication.getInstance(context).sqLiteDatabase
        val array = data.toTypedArray()
        val size = array.size
        for (i in (0 until size)) {
            val status = array[i]
            val filtered = shouldFilterStatus(db, status)
            if (filtered) {
                if (!status.is_gap && i != size - 1) {
                    data.remove(status)
                } else {
                    status.is_filtered = true
                }
            }
        }

        if (comparator != null) {
            data.sortWith(comparator!!)
        } else {
            data.sort()
        }
        saveCachedData(data)
        return ListResponse.getListInstance(CopyOnWriteArrayList(data))
    }

    @Throws(MicroBlogException::class)
    protected abstract fun getStatuses(microBlog: MicroBlog,
                                       details: AccountDetails,
                                       paging: Paging): List<Status>

    @WorkerThread
    protected abstract fun shouldFilterStatus(database: SQLiteDatabase, status: ParcelableStatus): Boolean


    override fun onStartLoading() {
        exception = null
        super.onStartLoading()
    }

    protected open fun processPaging(details: AccountDetails, loadItemLimit: Int, paging: Paging) {
        paging.setCount(loadItemLimit)
        if (maxId != null) {
            paging.setMaxId(maxId)
        }
        if (sinceId != null) {
            paging.setSinceId(sinceId)
            if (maxId == null) {
                paging.setLatestResults(true)
            }
        }
    }

    protected open val isGapEnabled: Boolean
        get() = true

    private val cachedData: List<ParcelableStatus>?
        get() {
            val key = serializationKey ?: return null
            val file = fileCache.get(key) ?: return null
            return JsonSerializer.parseList(file, ParcelableStatus::class.java)
        }

    private val serializationKey: String?
        get() {
            if (savedStatusesArgs == null) return null
            return TwidereArrayUtils.toString(savedStatusesArgs, '_', false)
        }

    private fun saveCachedData(data: List<ParcelableStatus>?) {
        val key = serializationKey
        if (key == null || data == null) return
        val databaseItemLimit = preferences[loadItemLimitKey]
        try {
            val statuses = data.subList(0, Math.min(databaseItemLimit, data.size))
            fileCache.save(key, tempFileInputStream(context) { os ->
                LoganSquare.serialize(statuses, os, ParcelableStatus::class.java)
            }) { current, total -> true }
        } catch (e: Exception) {
            // Ignore
            if (e !is IOException) {
                DebugLog.w(LOGTAG, "Error saving cached data", e)
            }
        }

    }

    companion object {

        private val pool = Executors.newSingleThreadExecutor()
    }

}