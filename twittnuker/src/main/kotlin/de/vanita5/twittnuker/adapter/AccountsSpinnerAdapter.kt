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
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.list_item_simple_user.view.*
import de.vanita5.twittnuker.R
import de.vanita5.twittnuker.model.AccountDetails
import de.vanita5.twittnuker.model.UserKey

class AccountsSpinnerAdapter(
        context: Context,
        itemViewResource: Int = R.layout.list_item_simple_user
) : BaseArrayAdapter<AccountDetails>(context, itemViewResource) {

    private var dummyItemText: String? = null

    constructor(context: Context, accounts: Collection<AccountDetails>) : this(context) {
        addAll(accounts)
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).hashCode().toLong()
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getDropDownView(position, convertView, parent)
        bindView(view, getItem(position))
        return view
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)
        bindView(view, getItem(position))
        return view
    }

    private fun bindView(view: View, item: AccountDetails) {
        val text1 = view.name
        val text2 = view.screenName
        val icon = view.profileImage
        if (!item.dummy) {
            text1?.visibility = View.VISIBLE
            text1?.text = item.user.name
            text2?.visibility = View.VISIBLE
            @SuppressLint("SetTextI18n")
            text2?.text = "@${item.user.screen_name}"
            if (icon != null) {
                if (profileImageEnabled) {
                    icon.visibility = View.VISIBLE
                    icon.style = profileImageStyle
                    mediaLoader.displayProfileImage(icon, item.user)
                } else {
                    icon.visibility = View.GONE
                    mediaLoader.cancelDisplayTask(icon)
                }
            }
        } else {
            text1?.visibility = View.VISIBLE
            text1?.text = dummyItemText
            text2?.visibility = View.GONE
            icon?.visibility = View.GONE
        }
    }


    fun setDummyItemText(textRes: Int) {
        setDummyItemText(context.getString(textRes))
    }

    fun setDummyItemText(text: String) {
        dummyItemText = text
        notifyDataSetChanged()
    }

    fun findPositionByKey(key: UserKey): Int {
        return (0 until count).indexOfFirst { key == getItem(it).key }
    }

}