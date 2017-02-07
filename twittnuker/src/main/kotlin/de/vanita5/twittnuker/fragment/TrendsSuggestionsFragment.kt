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
import android.database.Cursor
import android.os.Bundle
import android.support.v4.app.LoaderManager.LoaderCallbacks
import android.support.v4.content.CursorLoader
import android.support.v4.content.Loader
import android.view.View
import android.widget.AdapterView
import android.widget.ListView
import com.squareup.otto.Subscribe
import de.vanita5.twittnuker.R
import kotlinx.android.synthetic.main.fragment_content_listview.*
import org.mariotaku.sqliteqb.library.*
import de.vanita5.twittnuker.adapter.TrendsAdapter
import de.vanita5.twittnuker.constant.IntentConstants.EXTRA_EXTRAS
import de.vanita5.twittnuker.model.UserKey
import de.vanita5.twittnuker.model.message.TrendsRefreshedEvent
import de.vanita5.twittnuker.model.tab.extra.TrendsTabExtras
import de.vanita5.twittnuker.provider.TwidereDataStore.CachedTrends
import de.vanita5.twittnuker.util.DataStoreUtils.getTableNameByUri
import de.vanita5.twittnuker.util.IntentUtils.openTweetSearch
import de.vanita5.twittnuker.util.Utils

class TrendsSuggestionsFragment : AbsContentListViewFragment<TrendsAdapter>(), LoaderCallbacks<Cursor>, AdapterView.OnItemClickListener {

    private val accountKey: UserKey? get() = Utils.getAccountKeys(context, arguments)?.firstOrNull()
            ?: Utils.getDefaultAccountKey(context)
    private val tabExtras: TrendsTabExtras? get() = arguments.getParcelable(EXTRA_EXTRAS)

    private val woeId: Int get() {
        val id = tabExtras?.woeId ?: return 1
        return if (id > 0) id else 1
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        listView.onItemClickListener = this
        loaderManager.initLoader(0, null, this)
        showProgress()
    }

    override fun onCreateAdapter(context: Context): TrendsAdapter {
        return TrendsAdapter(activity)
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        val uri = CachedTrends.Local.CONTENT_URI
        val table = getTableNameByUri(uri)!!
        val timestampQuery = SQLQueryBuilder.select(Columns.Column(CachedTrends.TIMESTAMP))
                .from(Table(table))
                .orderBy(OrderBy(CachedTrends.TIMESTAMP, false))
                .limit(1)
                .build()
        val where = Expression.and(Expression.equalsArgs(CachedTrends.ACCOUNT_KEY),
                Expression.equalsArgs(CachedTrends.WOEID),
                Expression.equals(Columns.Column(CachedTrends.TIMESTAMP), timestampQuery)).sql
        val whereArgs = arrayOf(accountKey?.toString() ?: "", woeId.toString())
        return CursorLoader(activity, uri, CachedTrends.COLUMNS, where, whereArgs, CachedTrends.TREND_ORDER)
    }

    override fun onItemClick(view: AdapterView<*>, child: View, position: Int, id: Long) {
        if (multiSelectManager.isActive) return
        val trend: String?
        if (view is ListView) {
            trend = adapter.getItem(position - view.headerViewsCount)
        } else {
            trend = adapter.getItem(position)

        }
        if (trend == null) return
        openTweetSearch(activity, accountKey, trend)
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        adapter.swapCursor(null)
    }

    override fun onLoadFinished(loader: Loader<Cursor>, cursor: Cursor) {
        adapter.swapCursor(cursor)
        if (adapter.isEmpty) {
            showEmpty(R.drawable.ic_info_refresh, getString(R.string.swipe_down_to_refresh))
        } else {
            showContent()
        }
    }

    override fun onRefresh() {
        if (refreshing) return
        val accountKey = this.accountKey ?: return
        twitterWrapper.getLocalTrendsAsync(accountKey, woeId)
    }

    override var refreshing: Boolean
        get() = false
        set(value) {
            super.refreshing = value
        }

    override fun onStart() {
        super.onStart()
        loaderManager.restartLoader(0, null, this)
        bus.register(this)
    }

    override fun onStop() {
        bus.unregister(this)
        super.onStop()
    }

    @Subscribe
    fun onTrendsRefreshedEvent(event: TrendsRefreshedEvent) {
        refreshing = false
    }

}