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

package de.vanita5.twittnuker.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.support.v4.widget.SimpleCursorAdapter
import android.text.TextUtils
import android.view.View
import android.widget.TextView
import org.mariotaku.kpreferences.get
import org.mariotaku.sqliteqb.library.Columns
import org.mariotaku.sqliteqb.library.Expression
import org.mariotaku.sqliteqb.library.OrderBy
import de.vanita5.twittnuker.R
import de.vanita5.twittnuker.constant.displayProfileImageKey
import de.vanita5.twittnuker.constant.profileImageStyleKey
import de.vanita5.twittnuker.model.ParcelableUserCursorIndices
import de.vanita5.twittnuker.model.UserKey
import de.vanita5.twittnuker.provider.TwidereDataStore.CachedUsers
import de.vanita5.twittnuker.util.MediaLoaderWrapper
import de.vanita5.twittnuker.util.SharedPreferencesWrapper
import de.vanita5.twittnuker.util.UserColorNameManager
import de.vanita5.twittnuker.util.Utils
import de.vanita5.twittnuker.util.dagger.GeneralComponentHelper
import de.vanita5.twittnuker.view.ProfileImageView

import javax.inject.Inject


class UserAutoCompleteAdapter(val context: Context) : SimpleCursorAdapter(context,
        R.layout.list_item_auto_complete, null, emptyArray(),
        intArrayOf(), 0) {

    @Inject
    lateinit var profileImageLoader: MediaLoaderWrapper
    @Inject
    lateinit var preferences: SharedPreferencesWrapper
    @Inject
    lateinit var userColorNameManager: UserColorNameManager

    private val displayProfileImage: Boolean
    private var profileImageStyle: Int

    private var indices: ParcelableUserCursorIndices? = null

    var accountKey: UserKey? = null

    init {
        GeneralComponentHelper.build(context).inject(this)
        displayProfileImage = preferences[displayProfileImageKey]
        profileImageStyle = preferences[profileImageStyleKey]
    }

    override fun bindView(view: View, context: Context?, cursor: Cursor) {
        val indices = this.indices!!
        val text1 = view.findViewById(android.R.id.text1) as TextView
        val text2 = view.findViewById(android.R.id.text2) as TextView
        val icon = view.findViewById(android.R.id.icon) as ProfileImageView

        icon.style = profileImageStyle

        text1.text = cursor.getString(indices.name)
        @SuppressLint("SetTextI18n")
        text2.text = "@${cursor.getString(indices.screen_name)}"
        if (displayProfileImage) {
            val profileImageUrl = cursor.getString(indices.profile_image_url)
            profileImageLoader.displayProfileImage(icon, profileImageUrl)
        } else {
            //TODO cancel image load
        }

        icon.visibility = if (displayProfileImage) View.VISIBLE else View.GONE
        super.bindView(view, context, cursor)
    }

    fun closeCursor() {
        val cursor = swapCursor(null) ?: return
        if (!cursor.isClosed) {
            cursor.close()
        }
    }

    override fun convertToString(cursor: Cursor?): CharSequence {
        return cursor!!.getString(indices!!.screen_name)
    }

    override fun runQueryOnBackgroundThread(constraint: CharSequence): Cursor? {
        if (TextUtils.isEmpty(constraint)) return null
        val filter = filterQueryProvider
        if (filter != null) return filter.runQuery(constraint)
        val query = constraint.toString()
        val queryEscaped = query.replace("_", "^_")
        val usersSelection = Expression.or(
                Expression.likeRaw(Columns.Column(CachedUsers.SCREEN_NAME), "?||'%'", "^"),
                Expression.likeRaw(Columns.Column(CachedUsers.NAME), "?||'%'", "^"))
        val selectionArgs = arrayOf(queryEscaped, queryEscaped)
        val order = arrayOf(CachedUsers.LAST_SEEN, CachedUsers.SCORE, CachedUsers.SCREEN_NAME, CachedUsers.NAME)
        val ascending = booleanArrayOf(false, false, true, true)
        val orderBy = OrderBy(order, ascending)
        val uri = Uri.withAppendedPath(CachedUsers.CONTENT_URI_WITH_SCORE, accountKey.toString())
        @SuppressLint("Recycle")
        val cursor = context.contentResolver.query(uri, CachedUsers.COLUMNS, usersSelection.sql,
                selectionArgs, orderBy.sql)
        return cursor
    }

    override fun swapCursor(cursor: Cursor?): Cursor? {
        if (cursor != null) {
            indices = ParcelableUserCursorIndices(cursor)
        }
        return super.swapCursor(cursor)
    }

}