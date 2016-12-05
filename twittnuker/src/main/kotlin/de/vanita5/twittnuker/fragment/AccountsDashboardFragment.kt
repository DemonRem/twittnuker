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

import android.accounts.AccountManager
import android.animation.Animator
import android.animation.Animator.AnimatorListener
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.*
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.database.ContentObserver
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.design.widget.NavigationView
import android.support.v4.app.LoaderManager.LoaderCallbacks
import android.support.v4.content.AsyncTaskLoader
import android.support.v4.content.Loader
import android.support.v4.view.MenuItemCompat
import android.support.v7.view.SupportMenuInflater
import android.support.v7.widget.ActionMenuView.OnMenuItemClickListener
import android.support.v7.widget.FixedLinearLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView.Adapter
import android.support.v7.widget.RecyclerView.ViewHolder
import android.view.*
import android.view.View.OnClickListener
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import kotlinx.android.synthetic.main.header_drawer_account_selector.view.*
import org.mariotaku.ktextension.setItemAvailability
import org.mariotaku.ktextension.setMenuItemIcon
import org.mariotaku.ktextension.setMenuItemTitle
import org.mariotaku.sqliteqb.library.Expression
import de.vanita5.twittnuker.R
import de.vanita5.twittnuker.TwittnukerConstants.*
import de.vanita5.twittnuker.activity.*
import de.vanita5.twittnuker.annotation.AccountType
import de.vanita5.twittnuker.annotation.CustomTabType
import de.vanita5.twittnuker.annotation.Referral
import de.vanita5.twittnuker.constant.KeyboardShortcutConstants.*
import de.vanita5.twittnuker.fragment.AccountsDashboardFragment.AccountsInfo
import de.vanita5.twittnuker.menu.AccountToggleProvider
import de.vanita5.twittnuker.model.AccountDetails
import de.vanita5.twittnuker.model.SupportTabSpec
import de.vanita5.twittnuker.model.UserKey
import de.vanita5.twittnuker.model.util.AccountUtils
import de.vanita5.twittnuker.model.util.ParcelableAccountUtils
import de.vanita5.twittnuker.provider.TwidereDataStore.Accounts
import de.vanita5.twittnuker.provider.TwidereDataStore.Drafts
import de.vanita5.twittnuker.util.*
import de.vanita5.twittnuker.util.KeyboardShortcutsHandler.KeyboardShortcutCallback
import de.vanita5.twittnuker.view.ShapedImageView

import java.util.*

class AccountsDashboardFragment : BaseSupportFragment(), LoaderCallbacks<AccountsInfo>, OnSharedPreferenceChangeListener, OnClickListener, KeyboardShortcutCallback, NavigationView.OnNavigationItemSelectedListener {

    private val mSystemWindowsInsets = Rect()
    private var mResolver: ContentResolver? = null

    private var mAccountsAdapter: AccountSelectorAdapter? = null

    @Suppress("HasPlatformType")
    val accountsSelector by lazy { accountsHeader.otherAccountsList }

    private val navigationView by lazy { view as NavigationView }
    private val accountsHeader by lazy { navigationView.getHeaderView(0) }
    private val accountProfileBanner by lazy { accountsHeader.accountProfileBanner }
    private val floatingProfileImageSnapshot by lazy { accountsHeader.floatingProfileImageSnapshot }
    private val accountProfileImageView by lazy { accountsHeader.profileImage }
    private val accountProfileNameView by lazy { accountsHeader.name }
    private val accountProfileScreenNameView by lazy { accountsHeader.screenName }
    private val accountDashboardMenu by lazy { accountsHeader.accountDashboardMenu }
    private val profileContainer by lazy { accountsHeader.profileContainer }
    private val noAccountContainer by lazy { accountsHeader.noAccountContainer }

    private var mAccountActionProvider: AccountToggleProvider? = null

    private var mSwitchAccountAnimationPlaying: Boolean = false
    private var mUseStarsForLikes: Boolean = false
    private var mLoaderInitialized: Boolean = false

    val activatedAccountIds: Array<UserKey>
        get() {
            if (mAccountActionProvider != null) {
                return mAccountActionProvider!!.activatedAccountIds
            }
            return DataStoreUtils.getActivatedAccountKeys(activity)
        }

    override fun handleKeyboardShortcutSingle(handler: KeyboardShortcutsHandler,
                                              keyCode: Int, event: KeyEvent, metaState: Int): Boolean {
        return false
    }

    override fun isKeyboardShortcutHandled(handler: KeyboardShortcutsHandler, keyCode: Int, event: KeyEvent, metaState: Int): Boolean {
        val action = handler.getKeyAction(CONTEXT_TAG_NAVIGATION, keyCode, event, metaState)
        return ACTION_NAVIGATION_PREVIOUS == action || ACTION_NAVIGATION_NEXT == action
    }

    override fun handleKeyboardShortcutRepeat(handler: KeyboardShortcutsHandler,
                                              keyCode: Int, repeatCount: Int,
                                              event: KeyEvent, metaState: Int): Boolean {
        val action = handler.getKeyAction(CONTEXT_TAG_NAVIGATION, keyCode, event, metaState) ?: return false
        val offset: Int
        when (action) {
            ACTION_NAVIGATION_PREVIOUS -> {
                offset = -1
            }
            ACTION_NAVIGATION_NEXT -> {
                offset = 1
            }
            else -> {
                return false
            }
        }
        //        final int selectedItem = mNavigationView.getSelectedItemPosition();
        //        final int count = mNavigationView.getCount();
        //        int resultPosition;
        //        if (!mNavigationView.isFocused() || selectedItem == ListView.INVALID_POSITION) {
        //            resultPosition = firstVisiblePosition;
        //        } else {
        //            resultPosition = selectedItem + offset;
        //            while (resultPosition >= 0 && resultPosition < count && !mAdapter.isEnabled(resultPosition)) {
        //                resultPosition += offset;
        //            }
        //        }
        //        final View focusedChild = mNavigationView.getFocusedChild();
        //        if (focusedChild == null) {
        //            mNavigationView.requestChildFocus(mNavigationView.getChildAt(0), null);
        //        }
        //        if (resultPosition >= 0 && resultPosition < count) {
        //            mNavigationView.setSelection(resultPosition);
        //        }
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_SETTINGS -> {
                if (data == null) return
                if (data.getBooleanExtra(EXTRA_SHOULD_RESTART, false)) {
                    Utils.restartActivity(activity)
                } else if (data.getBooleanExtra(EXTRA_SHOULD_RECREATE, false)) {
                    activity.recreate()
                }
                return
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onResume() {
        super.onResume()
        updateDefaultAccountState()
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.profileContainer -> {
                val account = mAccountsAdapter!!.selectedAccount ?: return
                val activity = activity
                if (account.user != null) {
                    IntentUtils.openUserProfile(activity, account.user!!, null,
                            preferences.getBoolean(KEY_NEW_DOCUMENT_API),
                            Referral.SELF_PROFILE)
                } else {
                    IntentUtils.openUserProfile(activity, account.key, account.key,
                            account.user.screen_name, null,
                            preferences.getBoolean(KEY_NEW_DOCUMENT_API),
                            Referral.SELF_PROFILE)
                }
            }
        }
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<AccountsInfo> {
        return AccountsInfoLoader(activity)
    }

    override fun onLoadFinished(loader: Loader<AccountsInfo>, data: AccountsInfo) {
        updateAccountProviderData(data)
    }

    private fun updateAccountProviderData(data: AccountsInfo) {
        val menu = accountDashboardMenu.menu
        mAccountActionProvider = MenuItemCompat.getActionProvider(menu.findItem(R.id.select_account)) as AccountToggleProvider
        val accounts = data.accounts
        if (accounts.isNotEmpty()) {
            noAccountContainer.visibility = View.GONE
            profileContainer.visibility = View.VISIBLE
        } else {
            noAccountContainer.visibility = View.VISIBLE
            profileContainer.visibility = View.INVISIBLE
        }
        var defaultId: UserKey? = null
        for (account in accounts) {
            if (account.activated) {
                defaultId = account.key
                break
            }
        }
        mUseStarsForLikes = preferences.getBoolean(KEY_I_WANT_MY_STARS_BACK)

        mAccountsAdapter!!.accounts = accounts
        var accountKey = UserKey.valueOf(preferences.getString(KEY_DEFAULT_ACCOUNT_KEY, null))
        if (accountKey == null) {
            accountKey = defaultId
        }
        var selectedAccount: AccountDetails? = null
        for (account in accounts) {
            if (account.key.maybeEquals(accountKey)) {
                selectedAccount = account
                break
            }
        }
        mAccountsAdapter!!.selectedAccount = selectedAccount

        if (mAccountActionProvider != null) {
            mAccountActionProvider!!.isExclusive = false
            mAccountActionProvider!!.accounts = accounts
        }
        updateAccountActions()
        val currentAccount = mAccountsAdapter!!.selectedAccount
        if (currentAccount != null) {
            displayAccountBanner(currentAccount)
            displayCurrentAccount(null)
        }
        updateDefaultAccountState()

        if (data.draftsCount > 0) {
            navigationView.menu.findItem(R.id.drafts).title = "${getString(R.string.drafts)} (${data.draftsCount})"
        } else {
            navigationView.menu.findItem(R.id.drafts).title = getString(R.string.drafts)
        }
    }

    override fun onLoaderReset(loader: Loader<AccountsInfo>) {
    }


    override fun onSharedPreferenceChanged(preferences: SharedPreferences, key: String) {
        if (KEY_DEFAULT_ACCOUNT_KEY == key) {
            updateDefaultAccountState()
        }
    }

    override fun fitSystemWindows(insets: Rect) {
        mSystemWindowsInsets.set(insets)
        updateSystemWindowsInsets()
    }

    private fun updateSystemWindowsInsets() {
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mResolver = contentResolver
        val inflater = getLayoutInflater(savedInstanceState)
        mAccountsAdapter = AccountSelectorAdapter(inflater, this)
        val layoutManager = FixedLinearLayoutManager(context)
        layoutManager.orientation = LinearLayoutManager.HORIZONTAL
        layoutManager.stackFromEnd = true
        accountsSelector.layoutManager = layoutManager
        accountsSelector.adapter = mAccountsAdapter
        accountsSelector.itemAnimator = null
        val menuInflater = SupportMenuInflater(context)
        menuInflater.inflate(R.menu.action_dashboard_timeline_toggle, accountDashboardMenu.menu)
        accountDashboardMenu.setOnMenuItemClickListener(OnMenuItemClickListener { item ->
            if (item.groupId != AccountToggleProvider.MENU_GROUP) {
                when (item.itemId) {
                    R.id.compose -> {
                        val account = mAccountsAdapter!!.selectedAccount ?: return@OnMenuItemClickListener true
                        val composeIntent = Intent(INTENT_ACTION_COMPOSE)
                        composeIntent.setClass(activity, ComposeActivity::class.java)
                        composeIntent.putExtra(EXTRA_ACCOUNT_KEY, account.key)
                        startActivity(composeIntent)
                        return@OnMenuItemClickListener true
                    }
                }
                return@OnMenuItemClickListener false
            }
            val accounts = mAccountActionProvider!!.accounts
            val account = accounts[item.order]
            val values = ContentValues()
            val newActivated = !account.activated
            mAccountActionProvider!!.setAccountActivated(account.key, newActivated)
            values.put(Accounts.IS_ACTIVATED, newActivated)
            val where = Expression.equalsArgs(Accounts.ACCOUNT_KEY).sql
            val whereArgs = arrayOf(account.key.toString())
            mResolver!!.update(Accounts.CONTENT_URI, values, where, whereArgs)
            true
        })

        profileContainer.setOnClickListener(this)

        accountProfileBanner.setInAnimation(context, android.R.anim.fade_in)
        accountProfileBanner.setOutAnimation(context, android.R.anim.fade_out)
        accountProfileBanner.setFactory {
            inflater.inflate(R.layout.layout_account_dashboard_profile_image,
                    accountProfileBanner, false)
        }

        navigationView.setNavigationItemSelectedListener(this)
        preferences.registerOnSharedPreferenceChangeListener(this)

        loadAccounts()

        updateSystemWindowsInsets()
    }

    fun loadAccounts() {
        if (!mLoaderInitialized) {
            mLoaderInitialized = true
            loaderManager.initLoader(0, null, this)
        } else {
            loaderManager.restartLoader(0, null, this)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_accounts_dashboard, container, false)
    }

    override fun onStart() {
        super.onStart()
        loaderManager.restartLoader(0, null, this)
    }

    override fun onStop() {
        super.onStop()
    }

    internal fun updateAccountActions() {
        val activity = activity as HomeActivity
        val tabs = activity.tabs
        val account = mAccountsAdapter!!.selectedAccount ?: return
        var hasDmTab = false
        var hasInteractionsTab = false
        for (tab in tabs) {
            if (tab.type == null) continue
            when (tab.type) {
                CustomTabType.DIRECT_MESSAGES -> {
                    if (!hasDmTab) {
                        hasDmTab = hasAccountInTab(tab, account.key, account.activated)
                    }
                }
                CustomTabType.NOTIFICATIONS_TIMELINE -> {
                    if (!hasInteractionsTab) {
                        hasInteractionsTab = hasAccountInTab(tab, account.key, account.activated)
                    }
                }
            }
        }
        val menu = navigationView.menu
        menu.setItemAvailability(R.id.interactions, !hasInteractionsTab)
        menu.setItemAvailability(R.id.messages, !hasDmTab)

        if (mUseStarsForLikes) {
            menu.setMenuItemTitle(R.id.favorites, R.string.favorites)
            menu.setMenuItemIcon(R.id.favorites, R.drawable.ic_action_star)
        } else {
            menu.setMenuItemTitle(R.id.favorites, R.string.likes)
            menu.setMenuItemIcon(R.id.favorites, R.drawable.ic_action_heart)
        }
        var hasLists = false
        var hasGroups = false
        var hasPublicTimeline = false
        when (AccountUtils.getAccountType(account)) {
            AccountType.TWITTER -> {
                hasLists = true
            }
            AccountType.STATUSNET -> {
                hasGroups = true
            }
            AccountType.FANFOU -> {
                hasPublicTimeline = true
            }
        }
        MenuUtils.setItemAvailability(menu, R.id.groups, hasGroups)
        MenuUtils.setItemAvailability(menu, R.id.lists, hasLists)
        MenuUtils.setItemAvailability(menu, R.id.public_timeline, hasPublicTimeline)
    }

    private fun hasAccountInTab(tab: SupportTabSpec, accountId: UserKey, isActivated: Boolean): Boolean {
        if (tab.args == null) return false
        val accountKeys = Utils.getAccountKeys(context, tab.args) ?: return isActivated
        return accountKeys.contains(accountId)
    }

    private fun closeAccountsDrawer() {
        val activity = activity
        if (activity is HomeActivity) {
            activity.closeAccountsDrawer()
        }
    }

    private fun getLocationOnScreen(view: View, rectF: RectF) {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        rectF.set(location[0].toFloat(), location[1].toFloat(), (location[0] + view.width).toFloat(), (location[1] + view.height).toFloat())
    }

    private fun onAccountSelected(holder: AccountProfileImageViewHolder, account: AccountDetails) {
        if (mSwitchAccountAnimationPlaying) return
        val snapshotView = floatingProfileImageSnapshot
        val profileImageView = accountProfileImageView
        val clickedImageView = holder.iconView

        // Reset snapshot view position
        snapshotView.pivotX = 0f
        snapshotView.pivotY = 0f
        snapshotView.translationX = 0f
        snapshotView.translationY = 0f

        val matrix = Matrix()
        val sourceBounds = RectF()
        val destBounds = RectF()
        val snapshotBounds = RectF()
        getLocationOnScreen(clickedImageView, sourceBounds)
        getLocationOnScreen(profileImageView, destBounds)
        getLocationOnScreen(snapshotView, snapshotBounds)
        val finalScale = destBounds.width() / sourceBounds.width()
        val snapshotBitmap = TransitionUtils.createViewBitmap(clickedImageView, matrix,
                RectF(0f, 0f, sourceBounds.width(), sourceBounds.height()))
        val lp = snapshotView.layoutParams
        lp.width = clickedImageView.width
        lp.height = clickedImageView.height
        snapshotView.layoutParams = lp
        // Copied from MaterialNavigationDrawer: https://github.com/madcyph3r/AdvancedMaterialDrawer/
        val set = AnimatorSet()
        set.play(ObjectAnimator.ofFloat(snapshotView, View.TRANSLATION_X, sourceBounds.left - snapshotBounds.left, destBounds.left - snapshotBounds.left))
                .with(ObjectAnimator.ofFloat(snapshotView, View.TRANSLATION_Y, sourceBounds.top - snapshotBounds.top, destBounds.top - snapshotBounds.top))
                .with(ObjectAnimator.ofFloat<View>(snapshotView, View.SCALE_X, 1f, finalScale))
                .with(ObjectAnimator.ofFloat<View>(snapshotView, View.SCALE_Y, 1f, finalScale))
                .with(ObjectAnimator.ofFloat<View>(profileImageView, View.ALPHA, 1f, 0f))
                .with(ObjectAnimator.ofFloat<View>(clickedImageView, View.SCALE_X, 0f, 1f))
                .with(ObjectAnimator.ofFloat<View>(clickedImageView, View.SCALE_Y, 0f, 1f))
        val animationTransition: Long = 400
        set.duration = animationTransition
        set.interpolator = DecelerateInterpolator()
        set.addListener(object : AnimatorListener {

            private var clickedDrawable: Drawable? = null
            private var clickedColors: IntArray? = null

            override fun onAnimationStart(animation: Animator) {
                snapshotView.visibility = View.VISIBLE
                snapshotView.setImageBitmap(snapshotBitmap)
                val profileDrawable = profileImageView.drawable
                clickedDrawable = clickedImageView.drawable
                clickedColors = clickedImageView.borderColors
                val oldSelectedAccount = mAccountsAdapter!!.selectedAccount ?: return
                mediaLoader.displayDashboardProfileImage(clickedImageView,
                        oldSelectedAccount, profileDrawable)
                clickedImageView.setBorderColors(*profileImageView.borderColors)

                displayAccountBanner(account)

                mSwitchAccountAnimationPlaying = true
            }

            override fun onAnimationEnd(animation: Animator) {
                finishAnimation()
            }

            override fun onAnimationCancel(animation: Animator) {
                finishAnimation()
            }

            override fun onAnimationRepeat(animation: Animator) {

            }

            private fun finishAnimation() {
                val editor = preferences.edit()
                editor.putString(KEY_DEFAULT_ACCOUNT_KEY, account.key.toString())
                editor.apply()
                mAccountsAdapter!!.selectedAccount = account
                updateAccountActions()
                displayCurrentAccount(clickedDrawable)
                snapshotView.visibility = View.INVISIBLE
                snapshotView.setImageDrawable(null)
                profileImageView.setImageDrawable(clickedDrawable)
                profileImageView.setBorderColors(*clickedColors!!)
                profileImageView.alpha = 1f
                clickedImageView.scaleX = 1f
                clickedImageView.scaleY = 1f
                clickedImageView.alpha = 1f
                mSwitchAccountAnimationPlaying = false
            }
        })
        set.start()

    }

    protected fun displayAccountBanner(account: AccountDetails) {
        val bannerWidth = accountProfileBanner.width
        val res = resources
        val defWidth = res.displayMetrics.widthPixels
        val width = if (bannerWidth > 0) bannerWidth else defWidth
        val bannerView = accountProfileBanner.nextView as ImageView
        if (bannerView.drawable == null || !CompareUtils.objectEquals(account, bannerView.tag)) {
            mediaLoader.displayProfileBanner(bannerView, account, width)
            bannerView.tag = account
        } else {
            mediaLoader.cancelDisplayTask(bannerView)
        }
    }

    private fun displayCurrentAccount(profileImageSnapshot: Drawable?) {
        val account = mAccountsAdapter!!.selectedAccount ?: return
        accountProfileNameView.text = account.user.name
        accountProfileScreenNameView.text = String.format("@%s", account.user.screen_name)
        mediaLoader.displayDashboardProfileImage(accountProfileImageView, account,
                profileImageSnapshot)
        accountProfileImageView.setBorderColors(account.color)
        accountProfileBanner.showNext()
    }

    private fun updateDefaultAccountState() {
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val account = mAccountsAdapter!!.selectedAccount ?: return false
        when (item.itemId) {
            R.id.search -> {
                val intent = Intent(activity, QuickSearchBarActivity::class.java)
                intent.putExtra(EXTRA_ACCOUNT_KEY, account.key)
                startActivity(intent)
                closeAccountsDrawer()
            }
            R.id.compose -> {
                val composeIntent = Intent(INTENT_ACTION_COMPOSE)
                composeIntent.setClass(activity, ComposeActivity::class.java)
                composeIntent.putExtra(EXTRA_ACCOUNT_KEY, account.key)
                startActivity(composeIntent)
            }
            R.id.favorites -> {
                IntentUtils.openUserFavorites(activity, account.key,
                        account.key, account.user.screen_name)
            }
            R.id.lists -> {
                IntentUtils.openUserLists(activity, account.key,
                        account.key, account.user.screen_name)
            }
            R.id.groups -> {
                IntentUtils.openUserGroups(activity, account.key,
                        account.key, account.user.screen_name)
            }
            R.id.public_timeline -> {
                IntentUtils.openPublicTimeline(activity, account.key)
            }
            R.id.messages -> {
                IntentUtils.openDirectMessages(activity, account.key)
            }
            R.id.interactions -> {
                IntentUtils.openInteractions(activity, account.key)
            }
            R.id.edit -> {
                IntentUtils.openProfileEditor(activity, account.key)
            }
            R.id.accounts -> {
                IntentUtils.openAccountsManager(activity)
                closeAccountsDrawer()
            }
            R.id.drafts -> {
                IntentUtils.openDrafts(activity)
                closeAccountsDrawer()
            }
            R.id.filters -> {
                IntentUtils.openFilters(activity)
                closeAccountsDrawer()
            }
            R.id.settings -> {
                val intent = Intent(activity, SettingsActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                startActivityForResult(intent, REQUEST_SETTINGS)
                closeAccountsDrawer()
            }
        }
        return false
    }

    fun setStatusBarHeight(height: Int) {
        val top = Utils.getInsetsTopWithoutActionBarHeight(activity, height)
        profileContainer.setPadding(0, top, 0, 0)
    }

    internal class AccountProfileImageViewHolder(val adapter: AccountSelectorAdapter, itemView: View) : ViewHolder(itemView), OnClickListener {
        val iconView: ShapedImageView

        init {
            itemView.setOnClickListener(this)
            iconView = itemView.findViewById(android.R.id.icon) as ShapedImageView
        }

        override fun onClick(v: View) {
            adapter.dispatchItemSelected(this)
        }
    }

    internal class AccountSelectorAdapter(
            private val inflater: LayoutInflater,
            private val fragment: AccountsDashboardFragment
    ) : Adapter<AccountProfileImageViewHolder>() {
        private val mediaLoader: MediaLoaderWrapper
        var accounts: Array<AccountDetails>? = null
            set(value) {
                if (value != null) {
                    val previousAccounts = accounts
                    if (previousAccounts != null) {
                        val tmpList = arrayListOf(*value)
                        val tmpResult = ArrayList<AccountDetails>()
                        previousAccounts.forEach { previousAccount ->
                            val prefIndexOfTmp = tmpList.indexOfFirst { previousAccount == it }
                            if (prefIndexOfTmp >= 0) {
                                tmpResult.add(tmpList.removeAt(prefIndexOfTmp))
                            }
                        }
                        tmpResult.addAll(tmpList)
                        field = tmpResult.toTypedArray()
                    } else {
                        field = value
                    }
                } else {
                    field = null
                }
                notifyDataSetChanged()
            }

        init {
            mediaLoader = fragment.mediaLoader
            setHasStableIds(true)
        }

        fun getAdapterAccount(adapterPosition: Int): AccountDetails? {
            if (accounts == null || accounts!!.isEmpty()) {
                return null
            }
            return accounts!![adapterPosition + 1]
        }

        var selectedAccount: AccountDetails?
            get() {
                if (accounts == null || accounts!!.isEmpty()) {
                    return null
                }
                return accounts!![0]
            }
            set(account) {
                val selectedAccount = selectedAccount
                if (selectedAccount == null || account == null) return
                swap(account, selectedAccount)
            }

        val selectedAccountKey: UserKey?
            get() {
                val selectedAccount = selectedAccount ?: return null
                return selectedAccount.key
            }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountProfileImageViewHolder {
            val view = inflater.inflate(R.layout.adapter_item_dashboard_account, parent, false)
            return AccountProfileImageViewHolder(this, view)
        }

        override fun onBindViewHolder(holder: AccountProfileImageViewHolder, position: Int) {
            val account = getAdapterAccount(position)
            mediaLoader.displayDashboardProfileImage(holder.iconView, account!!, null)
            holder.iconView.setBorderColor(account.color)
        }

        override fun getItemId(position: Int): Long {
            return getAdapterAccount(position)!!.hashCode().toLong()
        }

        override fun getItemCount(): Int {
            if (accounts == null || accounts!!.isEmpty()) return 0
            return accounts!!.size - 1
        }

        fun dispatchItemSelected(holder: AccountProfileImageViewHolder) {
            fragment.onAccountSelected(holder, getAdapterAccount(holder.adapterPosition)!!)
        }

        private fun swap(from: AccountDetails, to: AccountDetails) {
            val accounts = accounts ?: return
            val fromIdx = accounts.indexOfFirst { it == from }
            val toIdx = accounts.indexOfFirst { it == to }
            if (fromIdx < 0 || toIdx < 0) return
            val temp = accounts[toIdx]
            accounts[toIdx] = accounts[fromIdx]
            accounts[fromIdx] = temp
            notifyDataSetChanged()
        }
    }

    data class AccountsInfo(
            val accounts: Array<AccountDetails>,
            val draftsCount: Int
    )

    class AccountsInfoLoader(context: Context) : AsyncTaskLoader<AccountsInfo>(context) {

        private var contentObserver: ContentObserver? = null
        private var firstLoad: Boolean

        init {
            firstLoad = true
        }

        override fun loadInBackground(): AccountsInfo {
            val accounts = AccountUtils.getAllAccountDetails(AccountManager.get(context))
            val draftsCount = DataStoreUtils.queryCount(context, Drafts.CONTENT_URI_UNSENT, null, null)
            return AccountsInfo(accounts, draftsCount)
        }

        /**
         * Handles a request to completely reset the Loader.
         */
        override fun onReset() {
            super.onReset()

            // Ensure the loader is stopped
            onStopLoading()

            // Stop monitoring for changes.
            if (contentObserver != null) {
                val cr = context.contentResolver
                cr.unregisterContentObserver(contentObserver)
                contentObserver = null
            }
        }

        /**
         * Handles a request to start the Loader.
         */
        override fun onStartLoading() {

            // Start watching for changes in the app data.
            if (contentObserver == null) {
                contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
                    override fun onChange(selfChange: Boolean) {
                        onContentChanged()
                    }

                    override fun onChange(selfChange: Boolean, uri: Uri?) {
                        onContentChanged()
                    }
                }
                val cr = context.contentResolver
                cr.registerContentObserver(Accounts.CONTENT_URI, true, contentObserver)
                cr.registerContentObserver(Drafts.CONTENT_URI, true, contentObserver)
            }

            if (takeContentChanged() || firstLoad) {
                firstLoad = false
                // If the data has changed since the last time it was loaded
                // or is not currently available, start a load.
            forceLoad()
        }
    }

        /**
         * Handles a request to stop the Loader.
         */
        override fun onStopLoading() {
            // Attempt to cancel the current load task if possible.
            cancelLoad()
        }
    }
}