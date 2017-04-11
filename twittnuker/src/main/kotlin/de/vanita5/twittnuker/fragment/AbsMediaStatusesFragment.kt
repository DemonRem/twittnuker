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

import android.content.Context
import android.os.Bundle
import android.support.v4.app.LoaderManager.LoaderCallbacks
import android.support.v4.app.hasRunningLoadersSafe
import android.support.v4.content.Loader
import android.support.v7.widget.StaggeredGridLayoutManager
import com.bumptech.glide.Glide
import de.vanita5.twittnuker.adapter.StaggeredGridParcelableStatusesAdapter
import de.vanita5.twittnuker.adapter.iface.ILoadMoreSupportAdapter
import de.vanita5.twittnuker.constant.IntentConstants.EXTRA_FROM_USER
import de.vanita5.twittnuker.extension.reachingEnd
import de.vanita5.twittnuker.extension.reachingStart
import de.vanita5.twittnuker.loader.MicroBlogAPIStatusesLoader
import de.vanita5.twittnuker.loader.iface.IExtendedLoader
import de.vanita5.twittnuker.model.ParcelableStatus
import de.vanita5.twittnuker.util.IntentUtils
import de.vanita5.twittnuker.view.HeaderDrawerLayout.DrawerCallback
import de.vanita5.twittnuker.view.holder.iface.IStatusViewHolder

abstract class AbsMediaStatusesFragment : AbsContentRecyclerViewFragment<StaggeredGridParcelableStatusesAdapter,
        StaggeredGridLayoutManager>(), LoaderCallbacks<List<ParcelableStatus>?>, DrawerCallback,
        IStatusViewHolder.StatusClickListener {

    override final var refreshing: Boolean
        get() {
            if (context == null || isDetached) return false
            return loaderManager.hasRunningLoadersSafe()
        }
        set(value) {
            super.refreshing = value
        }

    override final val reachingEnd: Boolean
        get() = layoutManager.reachingEnd

    override final val reachingStart: Boolean
        get() = layoutManager.reachingStart

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        adapter.statusClickListener = this
        val loaderArgs = Bundle(arguments)
        loaderArgs.putBoolean(EXTRA_FROM_USER, true)
        loaderManager.initLoader(0, loaderArgs, this)
        showProgress()
    }

    override final fun onCreateLayoutManager(context: Context): StaggeredGridLayoutManager {
        return StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
    }

    override final fun scrollToPositionWithOffset(position: Int, offset: Int) {
        layoutManager.scrollToPositionWithOffset(position, offset)
    }

    override final fun onCreateAdapter(context: Context): StaggeredGridParcelableStatusesAdapter {
        return StaggeredGridParcelableStatusesAdapter(context, Glide.with(this))
    }

    override fun onCreateLoader(id: Int, args: Bundle): Loader<List<ParcelableStatus>?> {
        val fromUser = args.getBoolean(EXTRA_FROM_USER)
        args.remove(EXTRA_FROM_USER)
        return onCreateStatusesLoader(activity, args, fromUser)
    }

    override final fun onLoadFinished(loader: Loader<List<ParcelableStatus>?>, data: List<ParcelableStatus>?) {
        val changed = adapter.setData(data)
        if ((loader as IExtendedLoader).fromUser) {
            adapter.loadMoreSupportedPosition = if (hasMoreData(loader, data, changed)) {
                ILoadMoreSupportAdapter.END
            } else {
                ILoadMoreSupportAdapter.NONE
            }
        }
        loader.fromUser = false
        refreshing = false
        showContent()
        setLoadMoreIndicatorPosition(ILoadMoreSupportAdapter.NONE)
    }

    override final fun onLoaderReset(loader: Loader<List<ParcelableStatus>?>) {
        adapter.setData(null)
    }

    override final fun onLoadMoreContents(position: Long) {
        // Only supports load from end
        if (ILoadMoreSupportAdapter.END != position) return
        super.onLoadMoreContents(position)
        // Get last raw status
        val startIdx = adapter.statusStartIndex
        if (startIdx < 0) return
        val statusCount = adapter.getStatusCount(true)
        if (statusCount <= 0) return
        val status = adapter.getStatus(startIdx + statusCount - 1, true)
        val maxId = status.id
        getStatuses(maxId, null)
    }

    override final fun onStatusClick(holder: IStatusViewHolder, position: Int) {
        val status = adapter.getStatus(position)
        IntentUtils.openStatus(context, status, null)
    }


    override final fun onQuotedStatusClick(holder: IStatusViewHolder, position: Int) {
        val status = adapter.getStatus(position)
        IntentUtils.openStatus(context, status.account_key, status.quoted_id)
    }

    protected open fun hasMoreData(loader: Loader<List<ParcelableStatus>?>,
            data: List<ParcelableStatus>?, changed: Boolean): Boolean {
        if (loader !is MicroBlogAPIStatusesLoader) return false
        val maxId = loader.maxId?.takeIf(String::isNotEmpty)
        val sinceId = loader.sinceId?.takeIf(String::isNotEmpty)
        if (sinceId == null && maxId != null) {
            if (data != null && !data.isEmpty()) {
                return changed
            }
        }
        return true
    }

    protected abstract fun onCreateStatusesLoader(context: Context, args: Bundle,
                                                  fromUser: Boolean): Loader<List<ParcelableStatus>?>

    protected abstract fun getStatuses(maxId: String?, sinceId: String?): Int

}