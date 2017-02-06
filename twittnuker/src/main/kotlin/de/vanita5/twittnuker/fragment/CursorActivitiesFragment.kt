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

package de.vanita5.twittnuker.fragment

import android.accounts.AccountManager
import android.accounts.OnAccountsUpdateListener
import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.support.v4.content.Loader
import com.squareup.otto.Subscribe
import org.mariotaku.ktextension.addOnAccountsUpdatedListenerSafe
import org.mariotaku.ktextension.removeOnAccountsUpdatedListenerSafe
import org.mariotaku.library.objectcursor.ObjectCursor
import org.mariotaku.sqliteqb.library.ArgsArray
import org.mariotaku.sqliteqb.library.Columns.Column
import org.mariotaku.sqliteqb.library.Expression
import de.vanita5.twittnuker.R
import de.vanita5.twittnuker.adapter.iface.ILoadMoreSupportAdapter
import de.vanita5.twittnuker.adapter.iface.ILoadMoreSupportAdapter.IndicatorPosition
import de.vanita5.twittnuker.constant.IntentConstants.EXTRA_FROM_USER
import de.vanita5.twittnuker.loader.ExtendedObjectCursorLoader
import de.vanita5.twittnuker.model.*
import de.vanita5.twittnuker.model.message.*
import de.vanita5.twittnuker.provider.TwidereDataStore.Activities
import de.vanita5.twittnuker.provider.TwidereDataStore.Filters
import de.vanita5.twittnuker.util.DataStoreUtils
import de.vanita5.twittnuker.util.DataStoreUtils.getTableNameByUri
import de.vanita5.twittnuker.util.ErrorInfoStore
import de.vanita5.twittnuker.util.Utils

abstract class CursorActivitiesFragment : AbsActivitiesFragment() {

    override fun onLoadingFinished() {
        val accountKeys = accountKeys
        if (adapter.itemCount > 0) {
            showContent()
        } else if (accountKeys.isNotEmpty()) {
            val errorInfo = ErrorInfoStore.getErrorInfo(context,
                    errorInfoStore[errorInfoKey, accountKeys[0]])
            if (errorInfo != null) {
                showEmpty(errorInfo.icon, errorInfo.message)
            } else {
                showEmpty(R.drawable.ic_info_refresh, getString(R.string.swipe_down_to_refresh))
            }
        } else {
            showError(R.drawable.ic_info_accounts, getString(R.string.message_toast_no_account_selected))
        }
    }

    protected abstract val errorInfoKey: String

    private var contentObserver: ContentObserver? = null
    private val accountListener: OnAccountsUpdateListener = OnAccountsUpdateListener { accounts ->
        reloadActivities()
    }

    abstract val contentUri: Uri

    override fun onCreateActivitiesLoader(context: Context,
                                          args: Bundle,
                                          fromUser: Boolean): Loader<List<ParcelableActivity>> {
        val uri = contentUri
        val table = getTableNameByUri(uri)
        val sortOrder = sortOrder
        val accountKeys = accountKeys
        val accountWhere = Expression.`in`(Column(Activities.ACCOUNT_KEY),
                ArgsArray(accountKeys.size))
        val filterWhere = getFiltersWhere(table)
        val where: Expression
        if (filterWhere != null) {
            where = Expression.and(accountWhere, filterWhere)
        } else {
            where = accountWhere
        }

        val accountSelectionArgs = Array(accountKeys.size) {
            accountKeys[it].toString()
        }
        val expression = processWhere(where, accountSelectionArgs)
        val selection = expression.sql
        adapter.showAccountsColor = accountKeys.size > 1
        val projection = Activities.COLUMNS
        return CursorActivitiesLoader(context, uri, projection, selection, expression.parameters,
                sortOrder, fromUser)
    }

    override fun createMessageBusCallback(): Any {
        return CursorActivitiesBusCallback()
    }

    override val accountKeys: Array<UserKey>
        get() = Utils.getAccountKeys(context, arguments) ?: DataStoreUtils.getActivatedAccountKeys(context)

    override fun onStart() {
        super.onStart()
        if (contentObserver == null) {
            contentObserver = object : ContentObserver(Handler()) {
                override fun onChange(selfChange: Boolean) {
                    reloadActivities()
                }
            }
            context.contentResolver.registerContentObserver(Filters.CONTENT_URI, true, contentObserver)
        }
        AccountManager.get(context).addOnAccountsUpdatedListenerSafe(accountListener, updateImmediately = false)
        updateRefreshState()
        reloadActivities()
    }

    override fun onStop() {
        if (contentObserver != null) {
            context.contentResolver.unregisterContentObserver(contentObserver)
            contentObserver = null
        }
        AccountManager.get(context).removeOnAccountsUpdatedListenerSafe(accountListener)
        super.onStop()
    }

    protected fun reloadActivities() {
        if (activity == null || isDetached) return
        val args = Bundle()
        val fragmentArgs = arguments
        if (fragmentArgs != null) {
            args.putAll(fragmentArgs)
            args.putBoolean(EXTRA_FROM_USER, true)
        }
        loaderManager.restartLoader(0, args, this)
    }

    override fun hasMoreData(data: List<ParcelableActivity>?): Boolean {
        return data?.size != 0
    }

    override fun onLoaderReset(loader: Loader<List<ParcelableActivity>>) {
        adapter.setData(null)
    }

    override fun onLoadMoreContents(@IndicatorPosition position: Long) {
        // Only supports load from end, skip START flag
        if (position and ILoadMoreSupportAdapter.START !== 0L || refreshing) return
        super.onLoadMoreContents(position)
        if (position == 0L) return
        getActivities(object : SimpleRefreshTaskParam() {
            override fun getAccountKeysWorker(): Array<UserKey> {
                return this@CursorActivitiesFragment.accountKeys
            }

            override val maxIds: Array<String?>?
                get() = getOldestActivityIds(accountKeys)

            override val maxSortIds: LongArray?
                get() {
                    val context = context ?: return null
                    return DataStoreUtils.getOldestActivityMaxSortPositions(context,
                            contentUri, accountKeys)
                }

            override val hasMaxIds: Boolean
                get() = true

            override val shouldAbort: Boolean
                get() = context == null
        })
    }

    override fun triggerRefresh(): Boolean {
        super.triggerRefresh()
        getActivities(object : SimpleRefreshTaskParam() {
            override fun getAccountKeysWorker(): Array<UserKey> {
                return this@CursorActivitiesFragment.accountKeys
            }

            override val sinceIds: Array<String?>?
                get() = getNewestActivityIds(accountKeys)

            override val sinceSortIds: LongArray?
                get() {
                    val context = context ?: return null
                    return DataStoreUtils.getNewestActivityMaxSortPositions(context,
                            contentUri, accountKeys)
                }

            override val hasSinceIds: Boolean
                get() = true

            override val shouldAbort: Boolean
                get() = context == null
        })
        return true
    }

    protected fun getFiltersWhere(table: String): Expression? {
        if (!isFilterEnabled) return null
        return DataStoreUtils.buildActivityFilterWhereClause(table, null)
    }

    protected fun getNewestActivityIds(accountKeys: Array<UserKey>): Array<String?>? {
        val context = context ?: return null
        return DataStoreUtils.getNewestActivityMaxPositions(context, contentUri, accountKeys)
    }

    protected abstract val notificationType: Int

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        val context = context
        if (context != null && isVisibleToUser) {
            val accountKeys = accountKeys
            for (accountKey in accountKeys) {
                twitterWrapper.clearNotificationAsync(notificationType, accountKey)
            }
        }
    }

    protected fun getOldestActivityIds(accountKeys: Array<UserKey>): Array<String?>? {
        val context = context ?: return null
        return DataStoreUtils.getOldestActivityMaxPositions(context, contentUri, accountKeys)
    }

    protected abstract val isFilterEnabled: Boolean

    protected open fun processWhere(where: Expression, whereArgs: Array<String>): ParameterizedExpression {
        return ParameterizedExpression(where, whereArgs)
    }

    protected abstract fun updateRefreshState()

    private val sortOrder: String
        get() = Activities.DEFAULT_SORT_ORDER


    private fun updateFavoritedStatus(status: ParcelableStatus) {
        activity ?: return
        replaceStatusStates(status)
    }


    fun replaceStatusStates(result: ParcelableStatus?) {
        if (result == null) return
        val lm = layoutManager
        val rangeStart = Math.max(adapter.activityStartIndex, lm.findFirstVisibleItemPosition())
        val rangeEnd = Math.min(lm.findLastVisibleItemPosition(), adapter.activityStartIndex + adapter.activityCount - 1)
        loop@ for (i in rangeStart..rangeEnd) {
            val activity = adapter.getActivity(i)
            if (result.account_key == activity!!.account_key && result.id == activity.status_id) {
                if (result.id != activity.status_id) {
                    continue@loop
                }
                val statusesMatrix = arrayOf(activity.target_statuses, activity.target_object_statuses)
                statusesMatrix.filterNotNull().forEach { statuses ->
                    for (status in statuses) {
                        if (result.id == status.id || result.id == status.retweet_id
                                || result.id == status.my_retweet_id) {
                            status.is_favorite = result.is_favorite
                            status.reply_count = result.reply_count
                            status.retweet_count = result.retweet_count
                            status.favorite_count = result.favorite_count
                        }
                    }
                }
            }
        }
        adapter.notifyItemRangeChanged(rangeStart, rangeEnd)
    }

    protected inner class CursorActivitiesBusCallback {

        @Subscribe
        fun notifyGetStatusesTaskChanged(event: GetActivitiesTaskEvent) {
            if (event.uri != contentUri) return
            refreshing = event.running
            if (!event.running) {
                setLoadMoreIndicatorPosition(ILoadMoreSupportAdapter.NONE)
                refreshEnabled = true
                onLoadingFinished()
            }
        }

        @Subscribe
        fun notifyFavoriteTask(event: FavoriteTaskEvent) {
            if (event.isSucceeded) {
                updateFavoritedStatus(event.status!!)
            }
        }

        @Subscribe
        fun notifyStatusDestroyed(event: StatusDestroyedEvent) {
        }

        @Subscribe
        fun notifyStatusListChanged(event: StatusListChangedEvent) {
            adapter.notifyDataSetChanged()
        }

        @Subscribe
        fun notifyStatusRetweeted(event: StatusRetweetedEvent) {
        }

        @Subscribe
        fun notifyAccountChanged(event: AccountChangedEvent) {

        }

    }

    class CursorActivitiesLoader(context: Context, uri: Uri, projection: Array<String>,
                                 selection: String, selectionArgs: Array<String>,
                                 sortOrder: String, fromUser: Boolean) : ExtendedObjectCursorLoader<ParcelableActivity>(context, ParcelableActivityCursorIndices::class.java, uri, projection, selection, selectionArgs, sortOrder, fromUser) {

        override fun createObjectCursor(cursor: Cursor, indices: ObjectCursor.CursorIndices<ParcelableActivity>): ObjectCursor<ParcelableActivity> {
            val filteredUserIds = DataStoreUtils.getFilteredUserIds(context)
            return ActivityCursor(cursor, indices, filteredUserIds)
        }

        class ActivityCursor(cursor: Cursor, indies: ObjectCursor.CursorIndices<ParcelableActivity>,
                             val filteredUserIds: Array<UserKey>) : ObjectCursor<ParcelableActivity>(cursor, indies)
    }
}