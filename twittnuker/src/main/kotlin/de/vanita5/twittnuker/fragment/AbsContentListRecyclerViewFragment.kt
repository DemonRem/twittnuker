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

package de.vanita5.twittnuker.fragment

import android.content.Context
import android.support.v7.widget.FixedLinearLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView

import de.vanita5.twittnuker.adapter.LoadMoreSupportAdapter
import de.vanita5.twittnuker.adapter.decorator.DividerItemDecoration
import de.vanita5.twittnuker.adapter.iface.ILoadMoreSupportAdapter
import de.vanita5.twittnuker.adapter.iface.ILoadMoreSupportAdapter.IndicatorPosition

abstract class AbsContentListRecyclerViewFragment<A : LoadMoreSupportAdapter<RecyclerView.ViewHolder>>
    : AbsContentRecyclerViewFragment<A, LinearLayoutManager>() {

    override fun createItemDecoration(context: Context,
                                      recyclerView: RecyclerView,
                                      layoutManager: LinearLayoutManager): RecyclerView.ItemDecoration? {
        return DividerItemDecoration(context, layoutManager.orientation)
    }

    override fun setLoadMoreIndicatorPosition(@IndicatorPosition position: Long) {
        val decor = itemDecoration
        if (decor is DividerItemDecoration) {
            decor.setDecorationStart(if (position and ILoadMoreSupportAdapter.START != 0L) 1 else 0)
            decor.setDecorationEndOffset(if (position and ILoadMoreSupportAdapter.END != 0L) 1 else 0)
        }
        super.setLoadMoreIndicatorPosition(position)
    }

    override fun scrollToPositionWithOffset(position: Int, offset: Int) {
        layoutManager.scrollToPositionWithOffset(0, 0)
    }

    override fun onCreateLayoutManager(context: Context): LinearLayoutManager {
        return FixedLinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
    }

    override val reachingEnd: Boolean
        get() {
            val lm = layoutManager
            return lm.findLastCompletelyVisibleItemPosition() >= lm.itemCount - 1
        }

    override val reachingStart: Boolean
        get() {
            val lm = layoutManager
            return lm.findFirstCompletelyVisibleItemPosition() <= 0
        }

}