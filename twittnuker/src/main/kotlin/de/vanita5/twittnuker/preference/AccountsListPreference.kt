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

package de.vanita5.twittnuker.preference

import android.accounts.AccountManager
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.support.v7.internal.widget.PreferenceImageView
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceManager
import android.support.v7.preference.PreferenceViewHolder
import android.support.v7.widget.SwitchCompat
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import de.vanita5.twittnuker.R
import de.vanita5.twittnuker.TwittnukerConstants.ACCOUNT_PREFERENCES_NAME_PREFIX
import de.vanita5.twittnuker.model.AccountDetails
import de.vanita5.twittnuker.model.util.AccountUtils
import de.vanita5.twittnuker.util.dagger.GeneralComponent
import de.vanita5.twittnuker.util.media.MediaPreloader

import javax.inject.Inject

abstract class AccountsListPreference(context: Context, attrs: AttributeSet? = null) : TintedPreferenceCategory(context, attrs) {

    private val switchKeyAttr: String?
    private val switchDefaultAttr: Boolean

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.AccountsListPreference)
        switchKeyAttr = a.getString(R.styleable.AccountsListPreference_switchKey)
        switchDefaultAttr = a.getBoolean(R.styleable.AccountsListPreference_switchDefault, false)
        a.recycle()
    }

    fun setAccountsData(accounts: Array<AccountDetails>) {
        removeAll()
        for (account in accounts) {
            val preference = AccountItemPreference(context, account, switchKeyAttr, getSwitchDefault())
            setupPreference(preference, account)
            addPreference(preference)
        }
        val preference = Preference(context)
        preference.layoutResource = R.layout.settings_layout_click_to_config
        addPreference(preference)
    }

    override final fun onAttachedToHierarchy(preferenceManager: PreferenceManager) {
        super.onAttachedToHierarchy(preferenceManager)
        if (preferenceCount > 0) return
        setAccountsData(AccountUtils.getAllAccountDetails(AccountManager.get(context), true))
    }

    protected open fun getSwitchDefault(): Boolean {
        return switchDefaultAttr
    }

    protected abstract fun setupPreference(preference: AccountItemPreference, account: AccountDetails)

    class AccountItemPreference(
            context: Context,
            account: AccountDetails,
            private val switchKey: String?,
            private val switchDefault: Boolean
    ) : Preference(context), OnSharedPreferenceChangeListener {
        private val switchPreference: SharedPreferences

        @Inject
        internal lateinit var mediaPreloader: MediaPreloader

        init {
            GeneralComponent.get(context).inject(this)
            val switchPreferenceName = "$ACCOUNT_PREFERENCES_NAME_PREFIX${account.key}"
            switchPreference = context.getSharedPreferences(switchPreferenceName, Context.MODE_PRIVATE)
            switchPreference.registerOnSharedPreferenceChangeListener(this)
            title = account.user.name
            summary = "@${account.user.screen_name}"
            widgetLayoutResource = R.layout.layout_preference_switch_indicator
        }

        override fun onSharedPreferenceChanged(preferences: SharedPreferences, key: String) {
            notifyChanged()
        }


        @SuppressLint("RestrictedApi")
        override fun onBindViewHolder(holder: PreferenceViewHolder) {
            super.onBindViewHolder(holder)
            val iconView = holder.findViewById(android.R.id.icon)
            if (iconView is PreferenceImageView) {
                val maxSize = context.resources.getDimensionPixelSize(R.dimen.element_size_normal)
                iconView.minimumWidth = maxSize
                iconView.minimumHeight = maxSize
                iconView.maxWidth = maxSize
                iconView.maxHeight = maxSize
                iconView.scaleType = ImageView.ScaleType.CENTER_CROP
            }
            val titleView = holder.findViewById(android.R.id.title)
            if (titleView is TextView) {
                titleView.setSingleLine(true)
            }
            val summaryView = holder.findViewById(android.R.id.summary)
            if (summaryView is TextView) {
                summaryView.setSingleLine(true)
            }
            val switchView = holder.findViewById(android.R.id.toggle) as SwitchCompat
            if (switchKey != null) {
                switchView.isChecked = switchPreference.getBoolean(switchKey, switchDefault)
                switchView.visibility = View.VISIBLE
            } else {
                switchView.visibility = View.GONE
            }
        }
    }

}