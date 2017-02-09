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

package de.vanita5.twittnuker.util

import android.accounts.AccountManager
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.twitter.Extractor
import de.vanita5.twittnuker.R
import de.vanita5.twittnuker.activity.BaseActivity
import de.vanita5.twittnuker.constant.IntentConstants.*
import de.vanita5.twittnuker.extension.model.getAccountUser
import de.vanita5.twittnuker.menu.AccountActionProvider
import de.vanita5.twittnuker.model.AccountDetails
import de.vanita5.twittnuker.model.ParcelableStatus
import de.vanita5.twittnuker.model.ParcelableUser
import de.vanita5.twittnuker.model.UserKey
import de.vanita5.twittnuker.model.util.AccountUtils
import de.vanita5.twittnuker.provider.TwidereDataStore.Filters
import de.vanita5.twittnuker.util.content.ContentResolverUtils
import de.vanita5.twittnuker.util.dagger.GeneralComponentHelper
import java.util.*
import javax.inject.Inject

@SuppressLint("Registered")
class MultiSelectEventHandler(
        private val activity: BaseActivity
) : ActionMode.Callback, MultiSelectManager.Callback {

    @Inject
    lateinit var twitterWrapper: AsyncTwitterWrapper

    @Inject
    lateinit var multiSelectManager: MultiSelectManager

    private var actionMode: ActionMode? = null

    private var accountActionProvider: AccountActionProvider? = null

    init {
        GeneralComponentHelper.build(activity).inject(this)
    }

    /**
     * Call before super.onCreate
     */
    fun dispatchOnCreate() {
    }

    /**
     * Call after super.onStart
     */
    fun dispatchOnStart() {
        multiSelectManager.registerCallback(this)
        updateMultiSelectState()
    }

    /**
     * Call before super.onStop
     */
    fun dispatchOnStop() {
        multiSelectManager.unregisterCallback(this)
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        val selectedItems = multiSelectManager.selectedItems
        if (selectedItems.isEmpty()) return false
        when (item.itemId) {
            R.id.reply -> {
                val extractor = Extractor()
                val am = AccountManager.get(activity)
                val intent = Intent(INTENT_ACTION_REPLY_MULTIPLE)
                val bundle = Bundle()
                val accountScreenNames = AccountUtils.getAccounts(am).map { it.getAccountUser(am).name }.toTypedArray()
                val allMentions = TreeSet(String.CASE_INSENSITIVE_ORDER)
                for (selected in selectedItems) {
                    if (selected is ParcelableStatus) {
                        allMentions.add(selected.user_screen_name)
                        allMentions.addAll(extractor.extractMentionedScreennames(selected.text_plain))
                    } else if (selected is ParcelableUser) {
                        allMentions.add(selected.screen_name)
                    }
                }
                allMentions.removeAll(Arrays.asList(*accountScreenNames))
                val firstObj = selectedItems[0]
                if (firstObj is ParcelableStatus) {
                    bundle.putString(EXTRA_IN_REPLY_TO_ID, firstObj.id)
                }
                bundle.putParcelable(EXTRA_ACCOUNT_KEY, multiSelectManager.accountKey)
                bundle.putStringArray(EXTRA_SCREEN_NAMES, allMentions.toTypedArray())
                intent.putExtras(bundle)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                activity.startActivity(intent)
                mode.finish()
            }
            R.id.mute_user -> {
                val resolver = activity.contentResolver
                val valuesList = ArrayList<ContentValues>()
                val userIds = HashSet<UserKey>()
                for (`object` in selectedItems) {
                    if (`object` is ParcelableStatus) {
                        userIds.add(`object`.user_key)
                        valuesList.add(ContentValuesCreator.createFilteredUser(`object`))
                    } else if (`object` is ParcelableUser) {
                        userIds.add(`object`.key)
                        valuesList.add(ContentValuesCreator.createFilteredUser(`object`))
                    }
                }
                ContentResolverUtils.bulkDelete(resolver, Filters.Users.CONTENT_URI,
                        Filters.Users.USER_KEY, false, userIds, null)
                ContentResolverUtils.bulkInsert(resolver, Filters.Users.CONTENT_URI, valuesList)
                Toast.makeText(activity, R.string.message_toast_users_filters_added, Toast.LENGTH_SHORT).show()
                mode.finish()
            }
            R.id.block -> {
                val accountKey = multiSelectManager.accountKey
                val userIds = UserKey.getIds(MultiSelectManager.getSelectedUserIds(selectedItems))
                if (accountKey != null && userIds != null) {
                    twitterWrapper.createMultiBlockAsync(accountKey, userIds)
                }
                mode.finish()
            }
            R.id.report_spam -> {
                val accountKey = multiSelectManager.accountKey
                val userIds = UserKey.getIds(MultiSelectManager.getSelectedUserIds(selectedItems))
                if (accountKey != null && userIds != null) {
                    twitterWrapper.reportMultiSpam(accountKey, userIds)
                }
                mode.finish()
            }
        }
        if (item.groupId == AccountActionProvider.MENU_GROUP) {
            val intent = item.intent
            if (intent == null || !intent.hasExtra(EXTRA_ACCOUNT)) return false
            val account: AccountDetails = intent.getParcelableExtra(EXTRA_ACCOUNT)
            multiSelectManager.accountKey = account.key
            accountActionProvider?.selectedAccountIds = arrayOf(account.key)
            mode.invalidate()
        }
        return true
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.menuInflater.inflate(R.menu.action_multi_select_contents, menu)
        accountActionProvider = menu.findItem(R.id.select_account).actionProvider as AccountActionProvider
        accountActionProvider?.selectedAccountIds = arrayOf(multiSelectManager.firstSelectAccountKey)
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        if (multiSelectManager.count != 0) {
            multiSelectManager.clearSelectedItems()
        }
        accountActionProvider = null
        actionMode = null
    }

    override fun onItemsCleared() {
        updateMultiSelectState()
    }

    override fun onItemSelected(item: Any) {
        updateMultiSelectState()
    }

    override fun onItemUnselected(item: Any) {
        updateMultiSelectState()
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        updateSelectedCount(mode)
        return true
    }

    private fun updateMultiSelectState() {
        if (multiSelectManager.isActive) {
            if (actionMode == null) {
                actionMode = activity.startActionMode(this)
            }
            updateSelectedCount(actionMode)
        } else {
            actionMode?.finish()
            actionMode = null
        }
    }

    private fun updateSelectedCount(mode: ActionMode?) {
        if (mode == null) return
        val count = multiSelectManager.count
        mode.title = activity.resources.getQuantityString(R.plurals.Nitems_selected, count, count)
    }

    companion object {

        val MENU_GROUP = 201
    }

}