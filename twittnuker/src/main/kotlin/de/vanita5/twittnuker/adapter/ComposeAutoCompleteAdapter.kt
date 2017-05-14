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

import android.content.Context
import android.content.SharedPreferences
import android.database.Cursor
import android.graphics.PorterDuff.Mode
import android.support.v4.widget.SimpleCursorAdapter
import android.view.View
import android.widget.TextView
import com.bumptech.glide.RequestManager
import org.mariotaku.kpreferences.get
import org.mariotaku.ktextension.spannable
import de.vanita5.twittnuker.R
import de.vanita5.twittnuker.TwittnukerConstants.*
import de.vanita5.twittnuker.annotation.AccountType
import de.vanita5.twittnuker.constant.displayProfileImageKey
import de.vanita5.twittnuker.constant.profileImageStyleKey
import de.vanita5.twittnuker.extension.appendQueryParameterIgnoreNull
import de.vanita5.twittnuker.extension.loadProfileImage
import de.vanita5.twittnuker.model.AccountDetails
import de.vanita5.twittnuker.model.SuggestionItem
import de.vanita5.twittnuker.model.UserKey
import de.vanita5.twittnuker.provider.TwidereDataStore.Suggestions
import de.vanita5.twittnuker.util.UserColorNameManager
import de.vanita5.twittnuker.util.dagger.GeneralComponent
import de.vanita5.twittnuker.view.ProfileImageView

import javax.inject.Inject

class ComposeAutoCompleteAdapter(context: Context, val requestManager: RequestManager) : SimpleCursorAdapter(context,
        R.layout.list_item_auto_complete, null, emptyArray(), intArrayOf(), 0) {

    @Inject
    lateinit var preferences: SharedPreferences
    @Inject
    lateinit var userColorNameManager: UserColorNameManager

    var account: AccountDetails? = null

    private val displayProfileImage: Boolean
    private val profileImageStyle: Int

    private var indices: SuggestionItem.Indices? = null
    private var token: Char = ' '

    init {
        GeneralComponent.get(context).inject(this)
        displayProfileImage = preferences[displayProfileImageKey]
        profileImageStyle = preferences[profileImageStyleKey]
    }

    override fun bindView(view: View, context: Context, cursor: Cursor) {
        val indices = this.indices!!
        val text1 = view.findViewById(android.R.id.text1) as TextView
        val text2 = view.findViewById(android.R.id.text2) as TextView
        val icon = view.findViewById(android.R.id.icon) as ProfileImageView

        icon.style = profileImageStyle

        if (Suggestions.AutoComplete.TYPE_USERS == cursor.getString(indices.type)) {
            val userKey = UserKey.valueOf(cursor.getString(indices.extra_id))
            text1.spannable =
                    cursor.getString(indices.title)
            val screenName = cursor.getString(indices.summary)
            text2.spannable = "@${getScreenNameOrAcct(screenName, userKey)}"
            if (displayProfileImage) {
                val profileImageUrl = cursor.getString(indices.icon)
                requestManager.loadProfileImage(context, profileImageUrl, profileImageStyle).into(icon)
            } else {
                //TODO cancel image load
            }

            icon.clearColorFilter()
        } else {
            text1.spannable = "#${cursor.getString(indices.title)}"
            text2.setText(R.string.hashtag)

            icon.setImageResource(R.drawable.ic_action_hashtag)
            icon.setColorFilter(text1.currentTextColor, Mode.SRC_ATOP)
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

    override fun convertToString(cursor: Cursor): CharSequence {
        val indices = this.indices!!
        when (cursor.getString(indices.type)) {
            Suggestions.AutoComplete.TYPE_HASHTAGS -> {
                return "#${cursor.getString(indices.value)}"
            }
            Suggestions.AutoComplete.TYPE_USERS -> {
                val screenName = cursor.getString(indices.value)
                val userKey = UserKey.valueOf(cursor.getString(indices.extra_id))
                return "@${getScreenNameOrAcct(screenName, userKey)}"
            }
        }
        return cursor.getString(indices.value)
    }

    override fun runQueryOnBackgroundThread(constraint: CharSequence?): Cursor? {
        if (constraint == null || constraint.isEmpty()) return null
        val token = constraint[0]
        if (getNormalizedSymbol(token) == getNormalizedSymbol(this.token)) {
            val filter = filterQueryProvider
            if (filter != null) return filter.runQuery(constraint)
        }
        this.token = token
        val builder = Suggestions.AutoComplete.CONTENT_URI.buildUpon()
        builder.appendQueryParameter(QUERY_PARAM_QUERY, constraint.subSequence(1, constraint.length).toString())
        when (getNormalizedSymbol(token)) {
            '#' -> {
                builder.appendQueryParameter(QUERY_PARAM_TYPE, Suggestions.AutoComplete.TYPE_HASHTAGS)
            }
            '@' -> {
                builder.appendQueryParameter(QUERY_PARAM_TYPE, Suggestions.AutoComplete.TYPE_USERS)
            }
            else -> {
                return null
            }
        }
        val account = this.account
        if (account != null) {
            builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_KEY, account.key.toString())
            builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_TYPE, account.type)
            if (account.type != AccountType.MASTODON) {
                builder.appendQueryParameterIgnoreNull(QUERY_PARAM_ACCOUNT_HOST, account.key?.host)
            }
        }
        return mContext.contentResolver.query(builder.build(), Suggestions.AutoComplete.COLUMNS,
                null, null, null)
    }

    override fun swapCursor(cursor: Cursor?): Cursor? {
        if (cursor != null) {
            indices = SuggestionItem.Indices(cursor)
        }
        return super.swapCursor(cursor)
    }

    private fun getScreenNameOrAcct(screenName: String, userKey: UserKey): String {
        if (account?.key?.host == userKey.host) return screenName
        return "$screenName@${userKey.host}"
    }

    companion object {

        private fun getNormalizedSymbol(character: Char): Char {
            when (character) {
                '\uff20', '@' -> return '@'
                '\uff03', '#' -> return '#'
            }
            return '\u0000'
        }
    }

}