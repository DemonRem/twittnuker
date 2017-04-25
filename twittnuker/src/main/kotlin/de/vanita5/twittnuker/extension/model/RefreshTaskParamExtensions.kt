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

package de.vanita5.twittnuker.extension.model

import de.vanita5.twittnuker.model.RefreshTaskParam
import de.vanita5.twittnuker.model.pagination.SinceMaxPagination

val RefreshTaskParam.hasMaxIds: Boolean
    get() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

fun RefreshTaskParam.getMaxId(index: Int): String? {
    return (pagination?.get(index) as? SinceMaxPagination)?.maxId
}

fun RefreshTaskParam.getSinceId(index: Int): String? {
    return (pagination?.get(index) as? SinceMaxPagination)?.sinceId
}

fun RefreshTaskParam.getMaxSortId(index: Int): Long {
    return (pagination?.get(index) as? SinceMaxPagination)?.maxSortId ?: -1
}

fun RefreshTaskParam.getSinceSortId(index: Int): Long {
    return (pagination?.get(index) as? SinceMaxPagination)?.sinceSortId ?: -1
}