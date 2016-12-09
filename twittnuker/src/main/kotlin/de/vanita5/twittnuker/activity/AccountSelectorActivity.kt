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

package de.vanita5.twittnuker.activity

import android.accounts.AccountManager
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.View.OnClickListener
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ListView
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_account_selector.*
import org.mariotaku.ktextension.toTypedArray
import de.vanita5.twittnuker.R
import de.vanita5.twittnuker.TwittnukerConstants.*
import de.vanita5.twittnuker.adapter.AccountDetailsAdapter
import de.vanita5.twittnuker.annotation.AccountType
import de.vanita5.twittnuker.app.TwittnukerApplication
import de.vanita5.twittnuker.extension.model.is_oauth
import de.vanita5.twittnuker.model.UserKey
import de.vanita5.twittnuker.model.util.AccountUtils

class AccountSelectorActivity : BaseActivity(), OnClickListener, OnItemClickListener {

    private lateinit var adapter: AccountDetailsAdapter

    private var firstCreated: Boolean = false


    private val keysWhiteList: Array<UserKey>?
        get() {
            return intent.getParcelableArrayExtra(EXTRA_ACCOUNT_KEYS)?.toTypedArray(UserKey.CREATOR)
        }

    private val isOAuthOnly: Boolean
        get() {
            return intent.getBooleanExtra(EXTRA_OAUTH_ONLY, false)
        }

    private val accountHost: String?
        get() {
            return intent.getStringExtra(EXTRA_ACCOUNT_HOST)
        }

    private val isSelectNoneAllowed: Boolean
        get() {
            return intent.getBooleanExtra(EXTRA_ALLOW_SELECT_NONE, false)
        }

    private val isSingleSelection: Boolean
        get() {
            return intent.getBooleanExtra(EXTRA_SINGLE_SELECTION, false)
        }

    private val startIntent: Intent?
        get() {
            val startIntent = intent.getParcelableExtra<Intent>(EXTRA_START_INTENT)
            startIntent?.setExtrasClassLoader(TwittnukerApplication::class.java.classLoader)
            return startIntent
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firstCreated = savedInstanceState == null
        setContentView(R.layout.activity_account_selector)
        adapter = AccountDetailsAdapter(this).apply {
            setSwitchEnabled(!isSingleSelection)
            setSortEnabled(false)
            isProfileImageDisplayed = preferences.getBoolean(KEY_DISPLAY_PROFILE_IMAGE, true)
            val am = AccountManager.get(context)
            val allAccountDetails = AccountUtils.getAllAccountDetails(am, AccountUtils.getAccounts(am))
            val extraKeys = keysWhiteList
            val oAuthOnly = isOAuthOnly
            val accountHost = accountHost
            addAll(allAccountDetails.filter {
                if (extraKeys != null) {
                    return@filter extraKeys.contains(it.key)
                }
                if (oAuthOnly && !it.is_oauth) {
                    return@filter false
                }
                if (USER_TYPE_TWITTER_COM == accountHost) {
                    if (it.key.host != null && it.type != AccountType.TWITTER) return@filter false
                } else if (accountHost != null) {
                    if (accountHost != it.key.host) return@filter false
                }
                return@filter true
            })
        }
        accountsList.choiceMode = if (isSingleSelection) ListView.CHOICE_MODE_NONE else ListView.CHOICE_MODE_MULTIPLE
        if (isSingleSelection) {
            accountsList.onItemClickListener = this
        }
        selectAccountButtons.visibility = if (isSingleSelection) View.GONE else View.VISIBLE
        accountsList.adapter = adapter
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.save -> {
                val checkedIds = accountsList.checkedItemIds
                if (checkedIds.isEmpty() && !isSelectNoneAllowed) {
                    Toast.makeText(this, R.string.no_account_selected, Toast.LENGTH_SHORT).show()
                    return
                }
                val data = Intent()
                data.putExtra(EXTRA_IDS, checkedIds)
                setResult(Activity.RESULT_OK, data)
                finish()
            }
        }
    }

    override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        selectSingleAccount(position)
    }

    fun selectSingleAccount(position: Int) {
        val account = adapter.getItem(position)
        val data = Intent()
        data.putExtra(EXTRA_ID, account.key.id)
        data.putExtra(EXTRA_ACCOUNT_KEY, account.key)

        val startIntent = startIntent
        if (startIntent != null) {
            startIntent.putExtra(EXTRA_ACCOUNT_KEY, account.key)
            startActivity(startIntent)
        }

        setResult(Activity.RESULT_OK, data)
        finish()
    }

}