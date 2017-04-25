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

import android.accounts.AccountManager
import android.animation.ArgbEvaluator
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.graphics.Outline
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter.CreateNdefMessageCallback
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.support.annotation.ColorRes
import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import android.support.annotation.UiThread
import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v4.app.LoaderManager.LoaderCallbacks
import android.support.v4.content.ContextCompat
import android.support.v4.content.FixedAsyncTaskLoader
import android.support.v4.content.Loader
import android.support.v4.content.res.ResourcesCompat
import android.support.v4.graphics.ColorUtils
import android.support.v4.view.ViewCompat
import android.support.v4.view.ViewPager.OnPageChangeListener
import android.support.v4.view.WindowCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.text.util.Linkify
import android.util.SparseBooleanArray
import android.view.*
import android.view.View.OnClickListener
import android.view.View.OnTouchListener
import android.view.animation.AnimationUtils
import android.widget.Toast
import com.bumptech.glide.Glide
import com.squareup.otto.Subscribe
import kotlinx.android.synthetic.main.fragment_user.*
import kotlinx.android.synthetic.main.fragment_user.view.*
import kotlinx.android.synthetic.main.header_user.*
import kotlinx.android.synthetic.main.header_user.view.*
import kotlinx.android.synthetic.main.layout_content_fragment_common.*
import kotlinx.android.synthetic.main.layout_content_pages_common.*
import nl.komponents.kovenant.task
import nl.komponents.kovenant.then
import nl.komponents.kovenant.ui.alwaysUi
import nl.komponents.kovenant.ui.failUi
import nl.komponents.kovenant.ui.successUi
import org.mariotaku.chameleon.Chameleon
import org.mariotaku.chameleon.ChameleonUtils
import org.mariotaku.kpreferences.get
import org.mariotaku.ktextension.*
import org.mariotaku.library.objectcursor.ObjectCursor
import de.vanita5.twittnuker.library.MicroBlog
import de.vanita5.twittnuker.library.MicroBlogException
import de.vanita5.twittnuker.library.twitter.model.FriendshipUpdate
import de.vanita5.twittnuker.library.twitter.model.Paging
import de.vanita5.twittnuker.library.twitter.model.UserList
import de.vanita5.twittnuker.BuildConfig
import de.vanita5.twittnuker.Constants.*
import de.vanita5.twittnuker.R
import de.vanita5.twittnuker.activity.AccountSelectorActivity
import de.vanita5.twittnuker.activity.BaseActivity
import de.vanita5.twittnuker.activity.ColorPickerDialogActivity
import de.vanita5.twittnuker.activity.LinkHandlerActivity
import de.vanita5.twittnuker.activity.iface.IBaseActivity
import de.vanita5.twittnuker.adapter.SupportTabsAdapter
import de.vanita5.twittnuker.annotation.AccountType
import de.vanita5.twittnuker.annotation.Referral
import de.vanita5.twittnuker.constant.*
import de.vanita5.twittnuker.constant.KeyboardShortcutConstants.*
import de.vanita5.twittnuker.extension.*
import de.vanita5.twittnuker.extension.model.api.microblog.toParcelable
import de.vanita5.twittnuker.extension.model.applyTo
import de.vanita5.twittnuker.extension.model.getBestProfileBanner
import de.vanita5.twittnuker.extension.model.originalProfileImage
import de.vanita5.twittnuker.extension.model.urlPreferred
import de.vanita5.twittnuker.fragment.AbsStatusesFragment.StatusesFragmentDelegate
import de.vanita5.twittnuker.fragment.statuses.UserTimelineFragment.UserTimelineFragmentDelegate
import de.vanita5.twittnuker.fragment.iface.IBaseFragment.SystemWindowsInsetsCallback
import de.vanita5.twittnuker.fragment.iface.IToolBarSupportFragment
import de.vanita5.twittnuker.fragment.iface.RefreshScrollTopInterface
import de.vanita5.twittnuker.fragment.iface.SupportFragmentCallback
import de.vanita5.twittnuker.fragment.statuses.UserFavoritesFragment
import de.vanita5.twittnuker.fragment.statuses.UserMediaTimelineFragment
import de.vanita5.twittnuker.fragment.statuses.UserTimelineFragment
import de.vanita5.twittnuker.graphic.ActionBarColorDrawable
import de.vanita5.twittnuker.graphic.ActionIconDrawable
import de.vanita5.twittnuker.loader.ParcelableUserLoader
import de.vanita5.twittnuker.model.*
import de.vanita5.twittnuker.model.event.FriendshipTaskEvent
import de.vanita5.twittnuker.model.event.FriendshipUpdatedEvent
import de.vanita5.twittnuker.model.event.ProfileUpdatedEvent
import de.vanita5.twittnuker.model.event.TaskStateChangedEvent
import de.vanita5.twittnuker.model.util.*
import de.vanita5.twittnuker.provider.TwidereDataStore.CachedRelationships
import de.vanita5.twittnuker.provider.TwidereDataStore.CachedUsers
import de.vanita5.twittnuker.text.TwidereURLSpan
import de.vanita5.twittnuker.util.*
import de.vanita5.twittnuker.util.KeyboardShortcutsHandler.KeyboardShortcutCallback
import de.vanita5.twittnuker.util.TwidereLinkify.OnLinkClickListener
import de.vanita5.twittnuker.util.UserColorNameManager.UserColorChangedListener
import de.vanita5.twittnuker.util.menu.TwidereMenuInfo
import de.vanita5.twittnuker.util.support.ActivitySupport
import de.vanita5.twittnuker.util.support.ActivitySupport.TaskDescriptionCompat
import de.vanita5.twittnuker.util.support.ViewSupport
import de.vanita5.twittnuker.util.support.WindowSupport
import de.vanita5.twittnuker.view.HeaderDrawerLayout.DrawerCallback
import de.vanita5.twittnuker.view.TabPagerIndicator
import de.vanita5.twittnuker.view.iface.IExtendedView.OnSizeChangedListener
import java.lang.ref.WeakReference
import java.util.*

class UserFragment : BaseFragment(), OnClickListener, OnLinkClickListener,
        OnSizeChangedListener, OnTouchListener, DrawerCallback, SupportFragmentCallback,
        SystemWindowsInsetsCallback, RefreshScrollTopInterface, OnPageChangeListener,
        KeyboardShortcutCallback, UserColorChangedListener,
        IToolBarSupportFragment, StatusesFragmentDelegate, UserTimelineFragmentDelegate,
        AbsContentRecyclerViewFragment.RefreshCompleteListener {

    override val toolbar: Toolbar
        get() = profileContentContainer.toolbar

    override val pinnedStatusIds: Array<String>?
        get() = user?.extras?.pinned_status_ids

    private lateinit var profileBirthdayBanner: View
    private lateinit var actionBarBackground: ActionBarDrawable
    private lateinit var pagerAdapter: SupportTabsAdapter

    // Data fields
    var user: ParcelableUser? = null
        private set
    private var account: AccountDetails? = null
    private var relationship: ParcelableRelationship? = null
    private var getUserInfoLoaderInitialized: Boolean = false
    private var getFriendShipLoaderInitialized: Boolean = false
    private var bannerWidth: Int = 0
    private var cardBackgroundColor: Int = 0
    private var actionBarShadowColor: Int = 0
    private var uiColor: Int = 0
    private var primaryColor: Int = 0
    private var primaryColorDark: Int = 0
    private var nameFirst: Boolean = false
    private var previousTabItemIsDark: Int = 0
    private var previousActionBarItemIsDark: Int = 0
    private var hideBirthdayView: Boolean = false

    private val friendshipLoaderCallbacks = object : LoaderCallbacks<SingleResponse<ParcelableRelationship>> {

        override fun onCreateLoader(id: Int, args: Bundle): Loader<SingleResponse<ParcelableRelationship>> {
            activity.invalidateOptionsMenu()
            val accountKey = args.getParcelable<UserKey>(EXTRA_ACCOUNT_KEY)
            val user = args.getParcelable<ParcelableUser>(EXTRA_USER)
            if (user != null && user.key == accountKey) {
                followingYouIndicator.visibility = View.GONE
                followContainer.follow.visibility = View.VISIBLE
                followProgress.visibility = View.VISIBLE
            } else {
                followingYouIndicator.visibility = View.GONE
                followContainer.follow.visibility = View.GONE
                followProgress.visibility = View.VISIBLE
            }
            return UserRelationshipLoader(activity, accountKey, user)
        }

        override fun onLoaderReset(loader: Loader<SingleResponse<ParcelableRelationship>>) {

        }

        override fun onLoadFinished(loader: Loader<SingleResponse<ParcelableRelationship>>,
                                    data: SingleResponse<ParcelableRelationship>) {
            followProgress.visibility = View.GONE
            displayRelationship(data.data)
            updateOptionsMenuVisibility()
        }

    }
    private val userInfoLoaderCallbacks = object : LoaderCallbacks<SingleResponse<ParcelableUser>> {

        override fun onCreateLoader(id: Int, args: Bundle): Loader<SingleResponse<ParcelableUser>> {
            val omitIntentExtra = args.getBoolean(EXTRA_OMIT_INTENT_EXTRA, true)
            val accountKey = args.getParcelable<UserKey?>(EXTRA_ACCOUNT_KEY)
            val userKey = args.getParcelable<UserKey?>(EXTRA_USER_KEY)
            val screenName = args.getString(EXTRA_SCREEN_NAME)
            if (user == null && (!omitIntentExtra || !args.containsKey(EXTRA_USER))) {
                cardContent.visibility = View.GONE
                errorContainer.visibility = View.GONE
                progressContainer.visibility = View.VISIBLE
                errorText.text = null
                errorText.visibility = View.GONE
            }
            val user = this@UserFragment.user
            val loadFromCache = user == null || !user.is_cache && user.key.maybeEquals(userKey)
            return ParcelableUserLoader(activity, accountKey, userKey, screenName, arguments,
                    omitIntentExtra, loadFromCache)
        }

        override fun onLoaderReset(loader: Loader<SingleResponse<ParcelableUser>>) {

        }

        override fun onLoadFinished(loader: Loader<SingleResponse<ParcelableUser>>,
                                    data: SingleResponse<ParcelableUser>) {
            val activity = activity ?: return
            if (data.data != null) {
                val user = data.data
                cardContent.visibility = View.VISIBLE
                errorContainer.visibility = View.GONE
                progressContainer.visibility = View.GONE
                val account: AccountDetails = data.extras.getParcelable(EXTRA_ACCOUNT)
                displayUser(user, account)
                if (user.is_cache) {
                    val args = Bundle()
                    args.putParcelable(EXTRA_ACCOUNT_KEY, user.account_key)
                    args.putParcelable(EXTRA_USER_KEY, user.key)
                    args.putString(EXTRA_SCREEN_NAME, user.screen_name)
                    args.putBoolean(EXTRA_OMIT_INTENT_EXTRA, true)
                    loaderManager.restartLoader(LOADER_ID_USER, args, this)
                }
                updateOptionsMenuVisibility()
            } else if (user?.is_cache ?: false) {
                cardContent.visibility = View.VISIBLE
                errorContainer.visibility = View.GONE
                progressContainer.visibility = View.GONE
                displayUser(user, account)
                updateOptionsMenuVisibility()
            } else {
                if (data.hasException()) {
                    errorText.text = data.exception?.getErrorMessage(activity)
                    errorText.visibility = View.VISIBLE
                }
                cardContent.visibility = View.GONE
                errorContainer.visibility = View.VISIBLE
                progressContainer.visibility = View.GONE
                displayUser(null, null)
                updateOptionsMenuVisibility()
            }
        }

    }

    private fun updateOptionsMenuVisibility() {
        setHasOptionsMenu(user != null && relationship != null)
    }

    private fun displayRelationship(relationship: ParcelableRelationship?) {
        val user = this.user ?: run {
            this.relationship = null
            return
        }
        if (user.key.maybeEquals(user.account_key)) {
            setFollowEditButton(R.drawable.ic_action_edit, R.color.material_light_blue,
                    R.string.action_edit)
            followContainer.follow.visibility = View.VISIBLE
            this.relationship = relationship
            return
        }
        if (relationship == null || !relationship.check(user)) {
            this.relationship = null
            return
        } else {
            this.relationship = relationship
        }
        activity.invalidateOptionsMenu()
        if (relationship.blocked_by) {
            pagesErrorContainer.visibility = View.GONE
            pagesErrorText.text = null
            pagesContent.visibility = View.VISIBLE
        } else if (!relationship.following && user.is_protected) {
            pagesErrorContainer.visibility = View.VISIBLE
            pagesErrorText.setText(R.string.user_protected_summary)
            pagesErrorIcon.setImageResource(R.drawable.ic_info_locked)
            pagesContent.visibility = View.GONE
        } else {
            pagesErrorContainer.visibility = View.GONE
            pagesErrorText.text = null
            pagesContent.visibility = View.VISIBLE
        }
        if (relationship.blocking) {
            setFollowEditButton(R.drawable.ic_action_block, R.color.material_red,
                    R.string.action_unblock)
        } else if (relationship.blocked_by) {
            setFollowEditButton(R.drawable.ic_action_block, R.color.material_grey,
                    R.string.action_block)
        } else if (relationship.following) {
            setFollowEditButton(R.drawable.ic_action_confirm, R.color.material_light_blue,
                    R.string.action_unfollow)
        } else if (user.is_follow_request_sent) {
            setFollowEditButton(R.drawable.ic_action_time, R.color.material_light_blue,
                    R.string.label_follow_request_sent)
        } else {
            setFollowEditButton(R.drawable.ic_action_add, android.R.color.white,
                    R.string.action_follow)
        }
        followingYouIndicator.visibility = if (relationship.followed_by) View.VISIBLE else View.GONE

        val resolver = context.applicationContext.contentResolver
        task {
            resolver.insert(CachedUsers.CONTENT_URI, ObjectCursor.valuesCreatorFrom(ParcelableUser::class.java).create(user))
            resolver.insert(CachedRelationships.CONTENT_URI, ObjectCursor.valuesCreatorFrom(ParcelableRelationship::class.java).create(relationship))
        }
        followContainer.follow.visibility = View.VISIBLE
    }

    override fun canScroll(dy: Float): Boolean {
        val fragment = currentVisibleFragment
        return fragment is DrawerCallback && fragment.canScroll(dy)
    }

    override fun cancelTouch() {
        val fragment = currentVisibleFragment
        if (fragment is DrawerCallback) {
            fragment.cancelTouch()
        }
    }

    override fun fling(velocity: Float) {
        val fragment = currentVisibleFragment
        if (fragment is DrawerCallback) {
            fragment.fling(velocity)
        }
    }

    override fun isScrollContent(x: Float, y: Float): Boolean {
        val v = viewPager
        val location = IntArray(2)
        v.getLocationInWindow(location)
        return x >= location[0] && x <= location[0] + v.width
                && y >= location[1] && y <= location[1] + v.height
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {

    }

    override fun onPageSelected(position: Int) {
        updateSubtitle()
    }

    override fun onPageScrollStateChanged(state: Int) {

    }

    override fun scrollBy(dy: Float) {
        val fragment = currentVisibleFragment
        if (fragment is DrawerCallback) {
            fragment.scrollBy(dy)
        }
    }

    override fun shouldLayoutHeaderBottom(): Boolean {
        val drawer = userProfileDrawer
        val card = profileDetailsContainer
        if (drawer == null || card == null) return false
        return card.top + drawer.headerTop - drawer.paddingTop <= 0
    }

    override fun topChanged(top: Int) {
        val drawer = userProfileDrawer ?: return
        val offset = drawer.paddingTop - top
        updateScrollOffset(offset)

        val fragment = currentVisibleFragment
        if (fragment is DrawerCallback) {
            fragment.topChanged(top)
        }
    }

    @UiThread
    fun displayUser(user: ParcelableUser?, account: AccountDetails?) {
        val activity = activity ?: return
        this.user = user
        this.account = account
        if (user == null || user.key == null) {
            profileImage.visibility = View.GONE
            profileType.visibility = View.GONE
            val theme = Chameleon.getOverrideTheme(activity, activity)
            setUiColor(theme.colorPrimary)
            return
        }
        val adapter = pagerAdapter
        for (i in 0 until adapter.count) {
            val sf = adapter.instantiateItem(viewPager, i) as? AbsStatusesFragment
            sf?.initLoaderIfNeeded()
        }
        profileImage.visibility = View.VISIBLE
        val resources = resources
        val lm = loaderManager
        lm.destroyLoader(LOADER_ID_USER)
        lm.destroyLoader(LOADER_ID_FRIENDSHIP)
        cardContent.visibility = View.VISIBLE
        errorContainer.visibility = View.GONE
        progressContainer.visibility = View.GONE
        this.user = user
        profileImage.setBorderColor(if (user.color != 0) user.color else Color.WHITE)
        profileNameContainer.drawEnd(user.account_color)
        profileNameContainer.name.text = bidiFormatter.unicodeWrap(user.name)
        val typeIconRes = Utils.getUserTypeIconRes(user.is_verified, user.is_protected)
        if (typeIconRes != 0) {
            profileType.setImageResource(typeIconRes)
            profileType.visibility = View.VISIBLE
        } else {
            profileType.setImageDrawable(null)
            profileType.visibility = View.GONE
        }
        @SuppressLint("SetTextI18n")
        profileNameContainer.screenName.text = "@${user.screen_name}"
        val linkHighlightOption = preferences[linkHighlightOptionKey]
        val linkify = TwidereLinkify(this, linkHighlightOption)
        if (user.description_unescaped != null) {
            val text = SpannableStringBuilder.valueOf(user.description_unescaped).apply {
                user.description_spans?.applyTo(this)
                linkify.applyAllLinks(this, user.account_key, false, false)
            }
            descriptionContainer.description.text = text
        } else {
            descriptionContainer.description.text = user.description_plain
            Linkify.addLinks(descriptionContainer.description, Linkify.WEB_URLS)
        }
        descriptionContainer.visibility = if (descriptionContainer.description.empty) View.GONE else View.VISIBLE

        locationContainer.location.text = user.location
        locationContainer.visibility = if (locationContainer.location.empty) View.GONE else View.VISIBLE
        urlContainer.url.text = user.urlPreferred?.let {
            val ssb = SpannableStringBuilder(it)
            ssb.setSpan(TwidereURLSpan(it, highlightStyle = linkHighlightOption), 0, ssb.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            return@let ssb
        }
        urlContainer.visibility = if (urlContainer.url.empty) View.GONE else View.VISIBLE
        if (user.created_at >= 0) {
            val createdAt = Utils.formatToLongTimeString(activity, user.created_at)
            val daysSinceCreation = (System.currentTimeMillis() - user.created_at) / 1000 / 60 / 60 / 24.toFloat()
            val dailyTweets = Math.round(user.statuses_count / Math.max(1f, daysSinceCreation))

            createdAtContainer.visibility = View.VISIBLE
            createdAtContainer.createdAt.text = resources.getQuantityString(R.plurals.created_at_with_N_tweets_per_day, dailyTweets,
                    createdAt, dailyTweets)
        } else {
            createdAtContainer.visibility = View.GONE
        }
        val locale = Locale.getDefault()

        listedContainer.listedCount.text = Utils.getLocalizedNumber(locale, user.listed_count)
        val groupsCount = if (user.extras != null) user.extras.groups_count else -1
        groupsContainer.groupsCount.text = Utils.getLocalizedNumber(locale, groupsCount)
        followersContainer.followersCount.text = Utils.getLocalizedNumber(locale, user.followers_count)
        friendsContainer.friendsCount.text = Utils.getLocalizedNumber(locale, user.friends_count)

        listedContainer.visibility = if (user.listed_count < 0) View.GONE else View.VISIBLE
        groupsContainer.visibility = if (groupsCount < 0) View.GONE else View.VISIBLE

        if (user.color != 0) {
            setUiColor(user.color)
        } else if (user.link_color != 0) {
            setUiColor(user.link_color)
        } else {
            val theme = Chameleon.getOverrideTheme(activity, activity)
            setUiColor(theme.colorPrimary)
        }
        val defWidth = resources.displayMetrics.widthPixels
        val width = if (bannerWidth > 0) bannerWidth else defWidth
        val requestManager = Glide.with(this)
        requestManager.loadProfileBanner(context, user, width).into(profileBanner)
        requestManager.loadOriginalProfileImage(context, user, profileImage.style,
                profileImage.cornerRadius, profileImage.cornerRadiusRatio)
                .thumbnail(requestManager.loadProfileImage(context, user, profileImage.style,
                        profileImage.cornerRadius, profileImage.cornerRadiusRatio,
                        getString(R.string.profile_image_size))).into(profileImage)
        val relationship = relationship
        if (relationship == null) {
            getFriendship()
        }
        activity.title = UserColorNameManager.decideDisplayName(user.name,
                user.screen_name, nameFirst)

        val userCreationDay = condition@ if (user.created_at >= 0) {
            val cal = Calendar.getInstance()
            val currentMonth = cal.get(Calendar.MONTH)
            val currentDay = cal.get(Calendar.DAY_OF_MONTH)
            cal.timeInMillis = user.created_at
            cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.DAY_OF_MONTH) == currentDay
        } else {
            false
        }

        if (userCreationDay && !hideBirthdayView) {
            if (profileBirthdayStub != null) {
                profileBirthdayBanner = profileBirthdayStub.inflate()
                profileBirthdayBanner.setOnClickListener(this)
            } else {
                profileBirthdayBanner.visibility = View.VISIBLE
            }
        } else if (profileBirthdayStub == null) {
            profileBirthdayBanner.visibility = View.GONE
        }

        urlContainer.url.movementMethod = null

        updateTitleAlpha()
        activity.invalidateOptionsMenu()
        updateSubtitle()
    }

    override val currentVisibleFragment: Fragment?
        get() {
            val currentItem = viewPager.currentItem
            if (currentItem < 0 || currentItem >= pagerAdapter.count) return null
            return pagerAdapter.instantiateItem(viewPager, currentItem)
        }

    override fun triggerRefresh(position: Int): Boolean {
        return false
    }

    override fun getSystemWindowsInsets(insets: Rect): Boolean {
        return false
    }

    fun getUserInfo(accountKey: UserKey, userKey: UserKey?, screenName: String?,
                    omitIntentExtra: Boolean) {
        val lm = loaderManager
        lm.destroyLoader(LOADER_ID_USER)
        lm.destroyLoader(LOADER_ID_FRIENDSHIP)
        val args = Bundle()
        args.putParcelable(EXTRA_ACCOUNT_KEY, accountKey)
        args.putParcelable(EXTRA_USER_KEY, userKey)
        args.putString(EXTRA_SCREEN_NAME, screenName)
        args.putBoolean(EXTRA_OMIT_INTENT_EXTRA, omitIntentExtra)
        if (!getUserInfoLoaderInitialized) {
            lm.initLoader(LOADER_ID_USER, args, userInfoLoaderCallbacks)
            getUserInfoLoaderInitialized = true
        } else {
            lm.restartLoader(LOADER_ID_USER, args, userInfoLoaderCallbacks)
        }
        if (userKey == null && screenName == null) {
            cardContent.visibility = View.GONE
            errorContainer.visibility = View.GONE
        }
    }

    @Subscribe
    fun notifyFriendshipUpdated(event: FriendshipUpdatedEvent) {
        val user = user
        if (user == null || !event.isAccount(user.account_key) || !event.isUser(user.key.id))
            return
        getFriendship()
    }

    @Subscribe
    fun notifyFriendshipUserUpdated(event: FriendshipTaskEvent) {
        val user = user
        if (user == null || !event.isSucceeded || !event.isUser(user)) return
        getFriendship()
    }

    @Subscribe
    fun notifyProfileUpdated(event: ProfileUpdatedEvent) {
        val user = user
        // TODO check account status
        if (user == null || user != event.user) return
        displayUser(event.user, account)
    }

    @Subscribe
    fun notifyTaskStateChanged(event: TaskStateChangedEvent) {
        activity.invalidateOptionsMenu()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val user = user
        when (requestCode) {
            REQUEST_SET_COLOR -> {
                if (user == null) return
                if (resultCode == Activity.RESULT_OK) {
                    if (data == null) return
                    val color = data.getIntExtra(EXTRA_COLOR, Color.TRANSPARENT)
                    userColorNameManager.setUserColor(this.user!!.key, color)
                } else if (resultCode == ColorPickerDialogActivity.RESULT_CLEARED) {
                    userColorNameManager.clearUserColor(this.user!!.key)
                }
            }
            REQUEST_ADD_TO_LIST -> {
                if (user == null) return
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val twitter = twitterWrapper
                    val list = data.getParcelableExtra<ParcelableUserList>(EXTRA_USER_LIST) ?: return
                    twitter.addUserListMembersAsync(user.account_key, list.id, user)
                }
            }
            REQUEST_SELECT_ACCOUNT -> {
                if (user == null) return
                if (resultCode == Activity.RESULT_OK) {
                    if (data == null || !data.hasExtra(EXTRA_ID)) return
                    val accountKey = data.getParcelableExtra<UserKey>(EXTRA_ACCOUNT_KEY)
                    @Referral
                    val referral = arguments.getString(EXTRA_REFERRAL)
                    IntentUtils.openUserProfile(activity, accountKey, user.key, user.screen_name,
                            user.extras.statusnet_profile_url, preferences[newDocumentApiKey],
                            referral, null)
                }
            }
        }

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_user, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val activity = activity
        nameFirst = preferences[nameFirstKey]
        cardBackgroundColor = ThemeUtils.getCardBackgroundColor(activity,
                preferences[themeBackgroundOptionKey], preferences[themeBackgroundAlphaKey])
        actionBarShadowColor = 0xA0000000.toInt()
        val args = arguments
        val accountKey = args.getParcelable<UserKey?>(EXTRA_ACCOUNT_KEY) ?: run {
            activity.finish()
            return
        }
        val userKey = args.getParcelable<UserKey?>(EXTRA_USER_KEY)
        val screenName = args.getString(EXTRA_SCREEN_NAME)

        Utils.setNdefPushMessageCallback(activity, CreateNdefMessageCallback {
            val user = user ?: return@CreateNdefMessageCallback null
            NdefMessage(arrayOf(NdefRecord.createUri(LinkCreator.getUserWebLink(user))))
        })


        userFragmentView.setWindowInsetsListener { _, top, _, _ ->
            profileContentContainer.setPadding(0, top, 0, 0)
            profileBannerSpace.statusBarHeight = top

            if (profileBannerSpace.toolbarHeight == 0) {
                var toolbarHeight = toolbar.measuredHeight
                if (toolbarHeight == 0) {
                    toolbarHeight = ThemeUtils.getActionBarHeight(context)
                }
                profileBannerSpace.toolbarHeight = toolbarHeight
            }
        }
        profileContentContainer.setOnSizeChangedListener { _, _, _, _, _ ->
            val toolbarHeight = toolbar.measuredHeight
            userProfileDrawer.setPadding(0, toolbarHeight, 0, 0)
            profileBannerSpace.toolbarHeight = toolbarHeight
        }

        userProfileDrawer.setDrawerCallback(this)

        pagerAdapter = SupportTabsAdapter(activity, childFragmentManager)

        viewPager.offscreenPageLimit = 3
        viewPager.adapter = pagerAdapter
        toolbarTabs.setViewPager(viewPager)
        toolbarTabs.setTabDisplayOption(TabPagerIndicator.DisplayOption.LABEL)
        toolbarTabs.setOnPageChangeListener(this)

        followContainer.follow.setOnClickListener(this)
        profileImage.setOnClickListener(this)
        profileBanner.setOnClickListener(this)
        listedContainer.setOnClickListener(this)
        groupsContainer.setOnClickListener(this)
        followersContainer.setOnClickListener(this)
        friendsContainer.setOnClickListener(this)
        errorIcon.setOnClickListener(this)
        urlContainer.setOnClickListener(this)
        profileBanner.setOnSizeChangedListener(this)
        profileBannerSpace.setOnTouchListener(this)

        userProfileSwipeLayout.setOnRefreshListener {
            if (!triggerRefresh()) {
                userProfileSwipeLayout.isRefreshing = false
            }
        }

        profileNameBackground.setBackgroundColor(cardBackgroundColor)
        profileDetailsContainer.setBackgroundColor(cardBackgroundColor)
        toolbarTabs.setBackgroundColor(cardBackgroundColor)

        val actionBarElevation = ThemeUtils.getSupportActionBarElevation(activity)
        ViewCompat.setElevation(toolbarTabs, actionBarElevation)

        actionBarBackground = ActionBarDrawable(ResourcesCompat.getDrawable(activity.resources,
                R.drawable.shadow_user_banner_action_bar, null)!!)
        setupBaseActionBar()
        setupViewStyle()
        setupUserPages()

        getUserInfo(accountKey, userKey, screenName, false)
    }

    override fun onStart() {
        super.onStart()
        bus.register(this)
        userColorNameManager.registerColorChangedListener(this)
    }


    override fun onStop() {
        userColorNameManager.unregisterColorChangedListener(this)
        bus.unregister(this)
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        setUiColor(uiColor)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable(EXTRA_USER, user)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        user = null
        relationship = null
        val lm = loaderManager
        lm.destroyLoader(LOADER_ID_USER)
        lm.destroyLoader(LOADER_ID_FRIENDSHIP)
        super.onDestroyView()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_user_profile, menu)
    }

    @UiThread
    override fun onPrepareOptionsMenu(menu: Menu) {
        val user = this.user ?: return
        val account = this.account
        val relationship = this.relationship

        val isMyself = user.account_key.maybeEquals(user.key)
        val mentionItem = menu.findItem(R.id.mention)
        if (mentionItem != null) {
            val displayName = UserColorNameManager.decideDisplayName(
                    user.name, user.screen_name, nameFirst)
            mentionItem.title = getString(R.string.mention_user_name, displayName)
        }
        menu.setItemAvailability(R.id.mention, !isMyself)
        menu.setItemAvailability(R.id.qr_code, isMyself || BuildConfig.DEBUG)
        menu.setItemAvailability(R.id.incoming_friendships, isMyself)
        menu.setItemAvailability(R.id.saved_searches, isMyself)

        menu.setItemAvailability(R.id.blocked_users, isMyself)
        menu.setItemAvailability(R.id.block, !isMyself)


        val isTwitter: Boolean

        if (account != null) {
            isTwitter = AccountType.TWITTER == account.type
        } else {
            isTwitter = false
        }

        menu.setItemAvailability(R.id.add_to_list, isTwitter)
        menu.setItemAvailability(R.id.mute_user, !isMyself && isTwitter)
        menu.setItemAvailability(R.id.muted_users, isMyself && isTwitter)
        menu.setItemAvailability(R.id.report_spam, !isMyself && isTwitter)
        menu.setItemAvailability(R.id.enable_retweets, !isMyself && isTwitter)

        if (relationship != null) {
            menu.findItem(R.id.add_to_filter)?.apply {
                isChecked = relationship.filtering
            }

            if (isMyself) {
                menu.setItemAvailability(R.id.send_direct_message, false)
                menu.setItemAvailability(R.id.enable_notifications, false)
            } else {
                menu.setItemAvailability(R.id.send_direct_message, relationship.can_dm)
                menu.setItemAvailability(R.id.block, true)
                menu.setItemAvailability(R.id.enable_notifications, isTwitter && relationship.following)

                menu.findItem(R.id.block)?.apply {
                    ActionIconDrawable.setMenuHighlight(this, TwidereMenuInfo(relationship.blocking))
                    this.setTitle(if (relationship.blocking) R.string.action_unblock else R.string.action_block)
                }
                menu.findItem(R.id.mute_user)?.apply {
                    isChecked = relationship.muting
                }
                menu.findItem(R.id.enable_retweets)?.apply {
                    isChecked = relationship.retweet_enabled
                }
                menu.findItem(R.id.enable_notifications)?.apply {
                    isChecked = relationship.notifications_enabled
                }
            }

        } else {
            menu.setItemAvailability(R.id.send_direct_message, false)
            menu.setItemAvailability(R.id.enable_notifications, false)
        }
        val drawer = userProfileDrawer
        if (drawer != null) {
            val offset = drawer.paddingTop - drawer.headerTop
            previousActionBarItemIsDark = 0
            previousTabItemIsDark = 0
            updateScrollOffset(offset)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val context = context
        val twitter = twitterWrapper
        val user = user
        val userRelationship = relationship
        if (user == null) return false
        when (item.itemId) {
            R.id.block -> {
                if (userRelationship == null) return true
                if (userRelationship.blocking) {
                    twitter.destroyBlockAsync(user.account_key, user.key)
                } else {
                    CreateUserBlockDialogFragment.show(fragmentManager, user)
                }
            }
            R.id.report_spam -> {
                ReportUserSpamDialogFragment.show(fragmentManager, user)
            }
            R.id.add_to_filter -> {
                if (userRelationship == null) return true
                if (userRelationship.filtering) {
                    DataStoreUtils.removeFromFilter(context, listOf(user))
                    Toast.makeText(activity, R.string.message_toast_user_filters_removed,
                            Toast.LENGTH_SHORT).show()
                    getFriendship()
                } else {
                    AddUserFilterDialogFragment.show(fragmentManager, user)
                }
            }
            R.id.mute_user -> {
                if (userRelationship == null) return true
                if (userRelationship.muting) {
                    twitter.destroyMuteAsync(user.account_key, user.key)
                } else {
                    CreateUserMuteDialogFragment.show(fragmentManager, user)
                }
            }
            R.id.mention -> {
                val intent = Intent(INTENT_ACTION_MENTION)
                val bundle = Bundle()
                bundle.putParcelable(EXTRA_USER, user)
                intent.putExtras(bundle)
                startActivity(intent)
            }
            R.id.send_direct_message -> {
                val am = AccountManager.get(activity)
                val builder = Uri.Builder().apply {
                    scheme(SCHEME_TWITTNUKER)
                    authority(AUTHORITY_MESSAGES)
                    path(PATH_MESSAGES_CONVERSATION_NEW)
                    appendQueryParameter(QUERY_PARAM_ACCOUNT_KEY, user.account_key.toString())
                }
                val intent = Intent(Intent.ACTION_VIEW, builder.build())
                intent.putExtra(EXTRA_ACCOUNT, AccountUtils.getAccountDetails(am, user.account_key,
                        true))
                intent.putExtra(EXTRA_USERS, arrayOf(user))
                startActivity(intent)
            }
            R.id.set_color -> {
                val intent = Intent(activity, ColorPickerDialogActivity::class.java)
                intent.putExtra(EXTRA_COLOR, userColorNameManager.getUserColor(user.key))
                intent.putExtra(EXTRA_ALPHA_SLIDER, false)
                intent.putExtra(EXTRA_CLEAR_BUTTON, true)
                startActivityForResult(intent, REQUEST_SET_COLOR)
            }
            R.id.add_to_list -> {
                showAddToListDialog(user)
            }
            R.id.open_with_account -> {
                val intent = Intent(INTENT_ACTION_SELECT_ACCOUNT)
                intent.setClass(activity, AccountSelectorActivity::class.java)
                intent.putExtra(EXTRA_SINGLE_SELECTION, true)
                intent.putExtra(EXTRA_ACCOUNT_HOST, user.key.host)
                startActivityForResult(intent, REQUEST_SELECT_ACCOUNT)
            }
            R.id.follow -> {
                if (userRelationship == null) return true
                val updatingRelationship = twitter.isUpdatingRelationship(user.account_key,
                        user.key)
                if (!updatingRelationship) {
                    if (userRelationship.following) {
                        DestroyFriendshipDialogFragment.show(fragmentManager, user)
                    } else {
                        twitter.createFriendshipAsync(user.account_key, user.key)
                    }
                }
                return true
            }
            R.id.enable_retweets -> {
                val newState = !item.isChecked
                val update = FriendshipUpdate()
                update.retweets(newState)
                twitter.updateFriendship(user.account_key, user.key, update)
                item.isChecked = newState
                return true
            }
            R.id.enable_notifications -> {
                val newState = !item.isChecked
                if (newState) {
                    Toast.makeText(context, R.string.message_toast_notification_enabled_hint,
                            Toast.LENGTH_SHORT).show()
                }
                val update = FriendshipUpdate()
                update.deviceNotifications(newState)
                twitter.updateFriendship(user.account_key, user.key, update)
                item.isChecked = newState
                return true
            }
            R.id.muted_users -> {
                IntentUtils.openMutesUsers(activity, user.account_key)
                return true
            }
            R.id.blocked_users -> {
                IntentUtils.openUserBlocks(activity, user.account_key)
                return true
            }
            R.id.incoming_friendships -> {
                IntentUtils.openIncomingFriendships(activity, user.account_key)
                return true
            }
            R.id.user_mentions -> {
                IntentUtils.openUserMentions(context, user.account_key, user.screen_name)
                return true
            }
            R.id.saved_searches -> {
                IntentUtils.openSavedSearches(context, user.account_key)
                return true
            }
            R.id.open_in_browser -> {
                val uri = LinkCreator.getUserWebLink(user)
                val intent = Intent(Intent.ACTION_VIEW, uri)
                intent.addCategory(Intent.CATEGORY_BROWSABLE)
                intent.`package` = IntentUtils.getDefaultBrowserPackage(context, uri, true)
                if (intent.resolveActivity(context.packageManager) != null) {
                    startActivity(intent)
                }
                return true
            }
            R.id.qr_code -> {
                executeAfterFragmentResumed {
                    val df = UserQrDialogFragment()
                    df.arguments = Bundle {
                        this[EXTRA_USER] = user
                    }
                    df.show(it.childFragmentManager, "user_qr_code")
                }
                return true
            }
            else -> {
                val intent = item.intent
                if (intent != null && intent.resolveActivity(context.packageManager) != null) {
                    startActivity(intent)
                }
            }
        }
        return true
    }


    override fun handleKeyboardShortcutSingle(handler: KeyboardShortcutsHandler, keyCode: Int, event: KeyEvent, metaState: Int): Boolean {
        if (handleFragmentKeyboardShortcutSingle(handler, keyCode, event, metaState)) return true
        val action = handler.getKeyAction(CONTEXT_TAG_NAVIGATION, keyCode, event, metaState)
        if (action != null) {
            when (action) {
                ACTION_NAVIGATION_PREVIOUS_TAB -> {
                    val previous = viewPager.currentItem - 1
                    if (previous >= 0 && previous < pagerAdapter.count) {
                        viewPager.setCurrentItem(previous, true)
                    }
                    return true
                }
                ACTION_NAVIGATION_NEXT_TAB -> {
                    val next = viewPager.currentItem + 1
                    if (next >= 0 && next < pagerAdapter.count) {
                        viewPager.setCurrentItem(next, true)
                    }
                    return true
                }
            }
        }
        return handler.handleKey(activity, null, keyCode, event, metaState)
    }

    override fun isKeyboardShortcutHandled(handler: KeyboardShortcutsHandler, keyCode: Int, event: KeyEvent, metaState: Int): Boolean {
        if (isFragmentKeyboardShortcutHandled(handler, keyCode, event, metaState)) return true
        val action = handler.getKeyAction(CONTEXT_TAG_NAVIGATION, keyCode, event, metaState)
        if (action != null) {
            when (action) {
                ACTION_NAVIGATION_PREVIOUS_TAB, ACTION_NAVIGATION_NEXT_TAB -> return true
            }
        }
        return false
    }

    override fun handleKeyboardShortcutRepeat(handler: KeyboardShortcutsHandler,
                                              keyCode: Int, repeatCount: Int,
                                              event: KeyEvent, metaState: Int): Boolean {
        return handleFragmentKeyboardShortcutRepeat(handler, keyCode, repeatCount, event, metaState)
    }

    private fun updateSubtitle() {
        val activity = activity as AppCompatActivity
        val actionBar = activity.supportActionBar ?: return
        val user = this.user
        if (user == null) {
            actionBar.subtitle = null
            return
        }
        val spec = pagerAdapter.get(viewPager.currentItem)
        assert(spec.type != null)
        when (spec.type) {
            TAB_TYPE_STATUSES, TAB_TYPE_STATUSES_WITH_REPLIES -> {
                actionBar.subtitle = resources.getQuantityString(R.plurals.N_statuses,
                        user.statuses_count.toInt(), user.statuses_count)
            }
            TAB_TYPE_MEDIA -> {
                if (user.media_count < 0) {
                    actionBar.setSubtitle(R.string.recent_media)
                } else {
                    actionBar.subtitle = resources.getQuantityString(R.plurals.N_media,
                            user.media_count.toInt(), user.media_count)
                }
            }
            TAB_TYPE_FAVORITES -> {
                if (user.favorites_count < 0) {
                    if (preferences[iWantMyStarsBackKey]) {
                        actionBar.setSubtitle(R.string.title_favorites)
                    } else {
                        actionBar.setSubtitle(R.string.title_likes)
                    }
                } else if (preferences[iWantMyStarsBackKey]) {
                    actionBar.subtitle = resources.getQuantityString(R.plurals.N_favorites,
                            user.favorites_count.toInt(), user.favorites_count)
                } else {
                    actionBar.subtitle = resources.getQuantityString(R.plurals.N_likes,
                            user.favorites_count.toInt(), user.favorites_count)
                }
            }
            else -> {
                actionBar.subtitle = null
            }
        }
        updateTitleAlpha()
    }

    private fun handleFragmentKeyboardShortcutRepeat(handler: KeyboardShortcutsHandler,
                                                     keyCode: Int, repeatCount: Int, event: KeyEvent, metaState: Int): Boolean {
        val fragment = keyboardShortcutRecipient
        if (fragment is KeyboardShortcutCallback) {
            return fragment.handleKeyboardShortcutRepeat(handler, keyCode, repeatCount, event, metaState)
        }
        return false
    }

    private fun handleFragmentKeyboardShortcutSingle(handler: KeyboardShortcutsHandler,
                                                     keyCode: Int, event: KeyEvent, metaState: Int): Boolean {
        val fragment = keyboardShortcutRecipient
        if (fragment is KeyboardShortcutCallback) {
            return fragment.handleKeyboardShortcutSingle(handler, keyCode, event, metaState)
        }
        return false
    }

    private fun isFragmentKeyboardShortcutHandled(handler: KeyboardShortcutsHandler,
                                                  keyCode: Int, event: KeyEvent, metaState: Int): Boolean {
        val fragment = keyboardShortcutRecipient
        if (fragment is KeyboardShortcutCallback) {
            return fragment.isKeyboardShortcutHandled(handler, keyCode, event, metaState)
        }
        return false
    }

    private val keyboardShortcutRecipient: Fragment?
        get() = currentVisibleFragment

    override fun fitSystemWindows(insets: Rect) {
    }

    override fun setupWindow(activity: FragmentActivity): Boolean {
        if (activity is AppCompatActivity) {
            activity.supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
            activity.supportRequestWindowFeature(WindowCompat.FEATURE_ACTION_MODE_OVERLAY)
        }
        WindowSupport.setStatusBarColor(activity.window, Color.TRANSPARENT)
        return true
    }

    override fun onClick(view: View) {
        val activity = activity
        val user = user
        if (activity == null || user == null) return
        when (view.id) {
            R.id.errorContainer -> {
                getUserInfo(true)
            }
            R.id.follow -> {
                if (user.account_key.maybeEquals(user.key)) {
                    IntentUtils.openProfileEditor(getActivity(), user.account_key)
                } else {
                    val userRelationship = relationship
                    val twitter = twitterWrapper
                    if (userRelationship == null) return
                    if (userRelationship.blocking) {
                        twitter.destroyBlockAsync(user.account_key, user.key)
                    } else if (userRelationship.blocked_by) {
                        CreateUserBlockDialogFragment.show(childFragmentManager, user)
                    } else if (userRelationship.following) {
                        DestroyFriendshipDialogFragment.show(fragmentManager, user)
                    } else {
                        twitter.createFriendshipAsync(user.account_key, user.key)
                    }
                }
            }
            R.id.profileImage -> {
                val url = user.originalProfileImage ?: return
                val profileImage = ParcelableMediaUtils.image(url)
                profileImage.type = ParcelableMedia.Type.IMAGE
                profileImage.preview_url = user.profile_image_url
                val media = arrayOf(profileImage)
                IntentUtils.openMedia(activity, user.account_key, media, null, false,
                        preferences[newDocumentApiKey], preferences[displaySensitiveContentsKey])
            }
            R.id.profileBanner -> {
                val url = user.getBestProfileBanner(0) ?: return
                val profileBanner = ParcelableMediaUtils.image(url)
                profileBanner.type = ParcelableMedia.Type.IMAGE
                val media = arrayOf(profileBanner)
                IntentUtils.openMedia(activity, user.account_key, media, null, false,
                        preferences[newDocumentApiKey], preferences[displaySensitiveContentsKey])
            }
            R.id.listedContainer -> {
                IntentUtils.openUserLists(getActivity(), user.account_key, user.key,
                        user.screen_name)
            }
            R.id.groupsContainer -> {
                IntentUtils.openUserGroups(getActivity(), user.account_key, user.key,
                        user.screen_name)
            }
            R.id.followersContainer -> {
                IntentUtils.openUserFollowers(getActivity(), user.account_key, user.key,
                        user.screen_name)
            }
            R.id.friendsContainer -> {
                IntentUtils.openUserFriends(getActivity(), user.account_key, user.key,
                        user.screen_name)
            }
            R.id.nameContainer -> {
                if (user.account_key == user.key) return
                IntentUtils.openProfileEditor(getActivity(), user.account_key)
            }
            R.id.urlContainer -> {
                val uri = user.urlPreferred?.let(Uri::parse) ?: return
                OnLinkClickHandler.openLink(context, preferences, uri)
            }
            R.id.profileBirthdayBanner -> {
                hideBirthdayView = true
                profileBirthdayBanner.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_out))
                profileBirthdayBanner.visibility = View.GONE
            }
        }

    }

    override fun onLinkClick(link: String, orig: String?, accountKey: UserKey?,
                             extraId: Long, type: Int, sensitive: Boolean,
                             start: Int, end: Int): Boolean {
        val user = user ?: return false
        when (type) {
            TwidereLinkify.LINK_TYPE_MENTION -> {
                IntentUtils.openUserProfile(activity, user.account_key, null, link, null,
                        preferences[newDocumentApiKey], Referral.USER_MENTION, null)
                return true
            }
            TwidereLinkify.LINK_TYPE_HASHTAG -> {
                IntentUtils.openTweetSearch(activity, user.account_key, "#" + link)
                return true
            }
            TwidereLinkify.LINK_TYPE_LINK_IN_TEXT, TwidereLinkify.LINK_TYPE_ENTITY_URL -> {
                val uri = Uri.parse(link)
                val intent: Intent
                if (uri.scheme != null) {
                    intent = Intent(Intent.ACTION_VIEW, uri)
                } else {
                    intent = Intent(Intent.ACTION_VIEW, uri.buildUpon().scheme("http").build())
                }
                startActivity(intent)
                return true
            }
            TwidereLinkify.LINK_TYPE_LIST -> {
                val mentionList = link.split("/").dropLastWhile(String::isEmpty)
                if (mentionList.size != 2) {
                    return false
                }
                return true
            }
        }
        return false
    }

    override fun onUserColorChanged(userKey: UserKey, color: Int) {
        if (user?.key != userKey) return
        displayUser(user, account)
    }

    override fun onSizeChanged(view: View, w: Int, h: Int, oldw: Int, oldh: Int) {
        bannerWidth = w
        if (w != oldw || h != oldh) {
            requestFitSystemWindows()
        }
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        if (profileBirthdayStub == null && profileBirthdayBanner.visibility == View.VISIBLE) {
            return profileBirthdayBanner.dispatchTouchEvent(event)
        }
        return profileBanner.dispatchTouchEvent(event)
    }

    override fun scrollToStart(): Boolean {
        val fragment = currentVisibleFragment as? RefreshScrollTopInterface ?: return false
        fragment.scrollToStart()
        return true
    }

    override fun triggerRefresh(): Boolean {
        val fragment = currentVisibleFragment as? RefreshScrollTopInterface ?: return false
        fragment.triggerRefresh()
        return true
    }

    override fun onRefreshComplete(fragment: AbsContentRecyclerViewFragment<*, *>) {
        userProfileSwipeLayout.isRefreshing = false
    }

    private fun getFriendship() {
        val user = user ?: return
        relationship = null
        val lm = loaderManager
        lm.destroyLoader(LOADER_ID_FRIENDSHIP)
        val args = Bundle()
        args.putParcelable(EXTRA_ACCOUNT_KEY, user.account_key)
        args.putParcelable(EXTRA_USER, user)
        if (!getFriendShipLoaderInitialized) {
            lm.initLoader(LOADER_ID_FRIENDSHIP, args, friendshipLoaderCallbacks)
            getFriendShipLoaderInitialized = true
        } else {
            lm.restartLoader(LOADER_ID_FRIENDSHIP, args, friendshipLoaderCallbacks)
        }
    }

    private fun getUserInfo(omitIntentExtra: Boolean) {
        val user = this.user ?: return
        getUserInfo(user.account_key, user.key, user.screen_name, omitIntentExtra)
    }

    private fun setUiColor(color: Int) {
        val theme = Chameleon.getOverrideTheme(activity, activity)
        uiColor = if (color != 0) color else theme.colorPrimary
        previousActionBarItemIsDark = 0
        previousTabItemIsDark = 0
        setupBaseActionBar()
        val activity = activity as BaseActivity
        if (theme.isToolbarColored) {
            primaryColor = color
        } else {
            primaryColor = theme.colorToolbar
        }
        primaryColorDark = ChameleonUtils.darkenColor(primaryColor)
        actionBarBackground.color = primaryColor
        val taskColor: Int
        if (theme.isToolbarColored) {
            taskColor = ColorUtils.setAlphaComponent(color, 0xFF)
        } else {
            taskColor = ColorUtils.setAlphaComponent(theme.colorToolbar, 0xFF)
        }
        val user = this.user
        if (user != null) {
            val name = userColorNameManager.getDisplayName(user, nameFirst)
            ActivitySupport.setTaskDescription(activity, TaskDescriptionCompat(name, null, taskColor))
        } else {
            ActivitySupport.setTaskDescription(activity, TaskDescriptionCompat(null, null, taskColor))
        }
        val optimalAccentColor = ThemeUtils.getOptimalAccentColor(color,
                descriptionContainer.description.currentTextColor)
        descriptionContainer.description.setLinkTextColor(optimalAccentColor)
        locationContainer.location.setLinkTextColor(optimalAccentColor)
        urlContainer.url.setLinkTextColor(optimalAccentColor)
        profileBanner.setBackgroundColor(color)

        toolbarTabs.setBackgroundColor(primaryColor)

        val drawer = userProfileDrawer
        if (drawer != null) {
            val offset = drawer.paddingTop - drawer.headerTop
            updateScrollOffset(offset)
        }
    }

    private fun setupBaseActionBar() {
        val activity = activity as? LinkHandlerActivity ?: return
        val actionBar = activity.supportActionBar ?: return
        if (!ThemeUtils.isWindowFloating(activity) && ThemeUtils.isTransparentBackground(activity.currentThemeBackgroundOption)) {
            profileBanner.alpha = activity.currentThemeBackgroundAlpha / 255f
        }
        actionBar.setBackgroundDrawable(actionBarBackground)
    }


    private fun setupViewStyle() {
        profileImage.style = preferences[profileImageStyleKey]

        val lightFont = preferences[lightFontKey]

        profileNameContainer.name.applyFontFamily(lightFont)
        profileNameContainer.screenName.applyFontFamily(lightFont)
        profileNameContainer.followingYouIndicator.applyFontFamily(lightFont)
        descriptionContainer.description.applyFontFamily(lightFont)
        urlContainer.url.applyFontFamily(lightFont)
        locationContainer.location.applyFontFamily(lightFont)
        createdAtContainer.createdAt.applyFontFamily(lightFont)
    }

    private fun setupUserPages() {
        val args = arguments
        val tabArgs = Bundle()
        val user = args.getParcelable<ParcelableUser>(EXTRA_USER)
        val userKey: UserKey?
        if (user != null) {
            userKey = user.account_key
            tabArgs.putParcelable(EXTRA_ACCOUNT_KEY, userKey)
            tabArgs.putParcelable(EXTRA_USER_KEY, user.key)
            tabArgs.putString(EXTRA_SCREEN_NAME, user.screen_name)
            tabArgs.putString(EXTRA_PROFILE_URL, user.extras?.statusnet_profile_url)
        } else {
            userKey = args.getParcelable<UserKey?>(EXTRA_ACCOUNT_KEY)
            tabArgs.putParcelable(EXTRA_ACCOUNT_KEY, userKey)
            tabArgs.putParcelable(EXTRA_USER_KEY, args.getParcelable<Parcelable>(EXTRA_USER_KEY))
            tabArgs.putString(EXTRA_SCREEN_NAME, args.getString(EXTRA_SCREEN_NAME))
            tabArgs.putString(EXTRA_PROFILE_URL, args.getString(EXTRA_PROFILE_URL))
        }
        pagerAdapter.add(cls = UserTimelineFragment::class.java, args = Bundle(tabArgs).apply {
            this[UserTimelineFragment.EXTRA_ENABLE_TIMELINE_FILTER] = true
        }, name = getString(R.string.title_statuses), type = TAB_TYPE_STATUSES,
                position = TAB_POSITION_STATUSES)
        pagerAdapter.add(cls = UserMediaTimelineFragment::class.java, args = tabArgs,
                name = getString(R.string.media), type = TAB_TYPE_MEDIA, position = TAB_POSITION_MEDIA)
        if (account?.type != AccountType.MASTODON || account?.key == userKey) {
            if (preferences[iWantMyStarsBackKey]) {
                pagerAdapter.add(cls = UserFavoritesFragment::class.java, args = tabArgs,
                        name = getString(R.string.title_favorites), type = TAB_TYPE_FAVORITES,
                        position = TAB_POSITION_FAVORITES)
            } else {
                pagerAdapter.add(cls = UserFavoritesFragment::class.java, args = tabArgs,
                        name = getString(R.string.title_likes), type = TAB_TYPE_FAVORITES,
                        position = TAB_POSITION_FAVORITES)
            }
        }
    }

    private fun updateScrollOffset(offset: Int) {
        val spaceHeight = profileBannerSpace.height
        val factor = (if (spaceHeight == 0) 0f else offset / spaceHeight.toFloat()).coerceIn(0f, 1f)
        profileBannerContainer.translationY = (-offset).toFloat()
        profileBanner.translationY = (offset / 2).toFloat()
        if (profileBirthdayStub == null) {
            profileBirthdayBanner.translationY = (offset / 2).toFloat()
        }

        val activity = activity as BaseActivity


        val statusBarColor = sArgbEvaluator.evaluate(factor, 0xA0000000.toInt(),
                ChameleonUtils.darkenColor(primaryColorDark)) as Int
        val window = activity.window
        userFragmentView.setStatusBarColor(statusBarColor)
        WindowSupport.setLightStatusBar(window, ThemeUtils.isLightColor(statusBarColor))
        val stackedTabColor = primaryColor


        val profileContentHeight = (profileNameContainer!!.height + profileDetailsContainer.height).toFloat()
        val tabOutlineAlphaFactor: Float
        if (offset - spaceHeight > 0) {
            tabOutlineAlphaFactor = 1f - ((offset - spaceHeight) / profileContentHeight).coerceIn(0f, 1f)
        } else {
            tabOutlineAlphaFactor = 1f
        }

        actionBarBackground.apply {
            this.factor = factor
            this.outlineAlphaFactor = tabOutlineAlphaFactor
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            windowOverlay.alpha = factor * tabOutlineAlphaFactor
        }

        val currentTabColor = sArgbEvaluator.evaluate(tabOutlineAlphaFactor,
                stackedTabColor, cardBackgroundColor) as Int

        val tabBackground = toolbarTabs.background
        (tabBackground as ColorDrawable).color = currentTabColor
        val tabItemIsDark = ThemeUtils.isLightColor(currentTabColor)

        if (previousTabItemIsDark == 0 || (if (tabItemIsDark) 1 else -1) != previousTabItemIsDark) {
            val tabContrastColor = ThemeUtils.getColorDependent(currentTabColor)
            toolbarTabs.setIconColor(tabContrastColor)
            toolbarTabs.setLabelColor(tabContrastColor)
            val theme = Chameleon.getOverrideTheme(activity, activity)
            if (theme.isToolbarColored) {
                toolbarTabs.setStripColor(tabContrastColor)
            } else {
                toolbarTabs.setStripColor(ThemeUtils.getOptimalAccentColor(uiColor, tabContrastColor))
            }
            toolbarTabs.updateAppearance()
        }
        previousTabItemIsDark = if (tabItemIsDark) 1 else -1

        val currentActionBarColor = sArgbEvaluator.evaluate(factor, actionBarShadowColor,
                stackedTabColor) as Int
        val actionItemIsDark = ThemeUtils.isLightColor(currentActionBarColor)
        if (previousActionBarItemIsDark == 0 || (if (actionItemIsDark) 1 else -1) != previousActionBarItemIsDark) {
            ThemeUtils.applyToolbarItemColor(activity, toolbar, currentActionBarColor)
        }
        previousActionBarItemIsDark = if (actionItemIsDark) 1 else -1

        updateTitleAlpha()
    }

    override var controlBarOffset: Float
        get() = 0f
        set(value) = Unit //Ignore

    override val controlBarHeight: Int
        get() = 0


    override val shouldInitLoader: Boolean
        get() = user != null

    private fun updateTitleAlpha() {
        val location = IntArray(2)
        profileNameContainer.name.getLocationInWindow(location)
        val nameShowingRatio = (userProfileDrawer.paddingTop - location[1]) / profileNameContainer.name.height.toFloat()
        val textAlpha = nameShowingRatio.coerceIn(0f, 1f)
        val titleView = ViewSupport.findViewByText(toolbar, toolbar.title)
        if (titleView != null) {
            titleView.alpha = textAlpha
        }
        val subtitleView = ViewSupport.findViewByText(toolbar, toolbar.subtitle)
        if (subtitleView != null) {
            subtitleView.alpha = textAlpha
        }
    }

    private fun ParcelableRelationship.check(user: ParcelableUser): Boolean {
        if (account_key != user.account_key) {
            return false
        }
        return user.extras != null && TextUtils.equals(user_key.id, user.extras.unique_id) || TextUtils.equals(user_key.id, user.key.id)
    }

    private fun setFollowEditButton(@DrawableRes icon: Int, @ColorRes color: Int, @StringRes label: Int) {
        val followButton = followContainer.follow
        followButton.setImageResource(icon)
        ViewCompat.setBackgroundTintMode(followButton, PorterDuff.Mode.SRC_ATOP)
        ViewCompat.setBackgroundTintList(followButton, ContextCompat.getColorStateList(context, color))
        followButton.contentDescription = getString(label)
    }

    private fun showAddToListDialog(user: ParcelableUser) {
        val weakThis = WeakReference(this)
        executeAfterFragmentResumed {
            ProgressDialogFragment.show(it.childFragmentManager, "get_list_progress")
        }.then {
            val fragment = weakThis.get() ?: throw InterruptedException()
            fun MicroBlog.getUserListOwnerMemberships(id: String): ArrayList<UserList> {
                val result = ArrayList<UserList>()
                var nextCursor: Long
                val paging = Paging()
                paging.count(100)
                do {
                    val resp = getUserListMemberships(id, paging, true)
                    result.addAll(resp)
                    nextCursor = resp.nextCursor
                    paging.cursor(nextCursor)
                } while (nextCursor > 0)

                return result
            }

            val microBlog = MicroBlogAPIFactory.getInstance(fragment.context, user.account_key)
            val ownedLists = ArrayList<ParcelableUserList>()
            val listMemberships = microBlog.getUserListOwnerMemberships(user.key.id)
            val paging = Paging()
            paging.count(100)
            var nextCursor: Long
            do {
                val resp = microBlog.getUserListOwnerships(paging)
                resp.mapTo(ownedLists) { item ->
                    val userList = item.toParcelable( user.account_key)
                    userList.is_user_inside = listMemberships.any { it.id == item.id }
                    return@mapTo userList
                }
                nextCursor = resp.nextCursor
                paging.cursor(nextCursor)
            } while (nextCursor > 0)
            return@then ownedLists.toTypedArray()
        }.alwaysUi {
            val fragment = weakThis.get() ?: return@alwaysUi
            fragment.executeAfterFragmentResumed {
                it.childFragmentManager.dismissDialogFragment("get_list_progress")
            }
        }.successUi { result ->
            val fragment = weakThis.get() ?: return@successUi
            fragment.executeAfterFragmentResumed { fragment ->
                val df = AddRemoveUserListDialogFragment()
                df.arguments = Bundle {
                    this[EXTRA_ACCOUNT_KEY] = user.account_key
                    this[EXTRA_USER_KEY] = user.key
                    this[EXTRA_USER_LISTS] = result
                }
                df.show(fragment.childFragmentManager, "add_remove_list")
            }
        }.failUi {
            val fragment = weakThis.get() ?: return@failUi
            Toast.makeText(fragment.context, it.getErrorMessage(fragment.context),
                    Toast.LENGTH_SHORT).show()
        }
    }

    private class ActionBarDrawable(shadow: Drawable) : LayerDrawable(arrayOf(shadow, ActionBarColorDrawable.create(true))) {

        private val shadowDrawable = getDrawable(0)
        private val colorDrawable = getDrawable(1) as ColorDrawable
        private var alphaValue: Int = 0

        var factor: Float = 0f
            set(value) {
                field = value
                updateValue()
            }

        var color: Int = 0
            set(value) {
                field = value
                colorDrawable.color = value
                updateValue()
            }

        var outlineAlphaFactor: Float = 0f
            set(value) {
                field = value
                updateValue()
            }

        init {
            alpha = 0xFF
            updateValue()
        }

        override fun setAlpha(alpha: Int) {
            alphaValue = alpha
            updateValue()
        }

        override fun getAlpha(): Int {
            return alphaValue
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        override fun getOutline(outline: Outline) {
            colorDrawable.getOutline(outline)
            outline.alpha = factor * outlineAlphaFactor * 0.99f
        }

        override fun getIntrinsicWidth(): Int {
            return colorDrawable.intrinsicWidth
        }

        override fun getIntrinsicHeight(): Int {
            return colorDrawable.intrinsicHeight
        }

        private fun updateValue() {
            val shadowAlpha = Math.round(alpha * (1 - factor).coerceIn(0f, 1f))
            shadowDrawable.alpha = shadowAlpha
            val hasColor = color != 0
            val colorAlpha = if (hasColor) Math.round(alpha * factor.coerceIn(0f, 1f)) else 0
            colorDrawable.alpha = colorAlpha
            invalidateSelf()
        }

    }


    internal class UserRelationshipLoader(
            context: Context,
            private val accountKey: UserKey?,
            private val user: ParcelableUser?
    ) : FixedAsyncTaskLoader<SingleResponse<ParcelableRelationship>>(context) {

        override fun loadInBackground(): SingleResponse<ParcelableRelationship> {
            if (accountKey == null || user == null) {
                return SingleResponse.Companion.getInstance<ParcelableRelationship>(MicroBlogException("Null parameters"))
            }
            val userKey = user.key
            val isFiltering = DataStoreUtils.isFilteringUser(context, userKey)
            if (accountKey == user.key) {
                return SingleResponse.getInstance(ParcelableRelationshipUtils.create(accountKey, userKey,
                        null, isFiltering))
            }
            val details = AccountUtils.getAccountDetails(AccountManager.get(context),
                    accountKey, true) ?: return SingleResponse.getInstance<ParcelableRelationship>(MicroBlogException("No Account"))
            if (details.type == AccountType.TWITTER) {
                if (!UserKeyUtils.isSameHost(accountKey, user.key)) {
                    return SingleResponse.getInstance(ParcelableRelationshipUtils.create(user, isFiltering))
                }
            }
            val twitter = MicroBlogAPIFactory.getInstance(context, accountKey) ?: return SingleResponse.Companion.getInstance<ParcelableRelationship>(MicroBlogException("No Account"))
            try {
                val relationship = twitter.showFriendship(user.key.id)
                if (relationship.isSourceBlockingTarget || relationship.isSourceBlockedByTarget) {
                    Utils.setLastSeen(context, userKey, -1)
                } else {
                    Utils.setLastSeen(context, userKey, System.currentTimeMillis())
                }
                val data = ParcelableRelationshipUtils.create(accountKey, userKey, relationship,
                        isFiltering)
                val resolver = context.contentResolver
                val values = ObjectCursor.valuesCreatorFrom(ParcelableRelationship::class.java).create(data)
                resolver.insert(CachedRelationships.CONTENT_URI, values)
                return SingleResponse.getInstance(data)
            } catch (e: MicroBlogException) {
                return SingleResponse.Companion.getInstance<ParcelableRelationship>(e)
            }

        }

        override fun onStartLoading() {
            forceLoad()
        }
    }

    class AddRemoveUserListDialogFragment : BaseDialogFragment() {

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val lists = arguments.getParcelableArray(EXTRA_USER_LISTS).toTypedArray(ParcelableUserList.CREATOR)
            val userKey = arguments.getParcelable<UserKey>(EXTRA_USER_KEY)
            val accountKey = arguments.getParcelable<UserKey>(EXTRA_ACCOUNT_KEY)
            val builder = AlertDialog.Builder(context)
            builder.setTitle(R.string.title_add_or_remove_from_list)
            val entries = Array(lists.size) { idx ->
                lists[idx].name
            }
            val states = BooleanArray(lists.size) { idx ->
                lists[idx].is_user_inside
            }
            builder.setPositiveButton(android.R.string.ok, null)
            builder.setNeutralButton(R.string.new_user_list, null)
            builder.setNegativeButton(android.R.string.cancel, null)

            builder.setMultiChoiceItems(entries, states, null)
            val dialog = builder.create()
            dialog.setOnShowListener { dialog ->
                dialog as AlertDialog
                dialog.applyTheme()
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                    val checkedPositions = dialog.listView.checkedItemPositions
                    val weakActivity = WeakReference(activity)
                    (activity as IBaseActivity<*>).executeAfterFragmentResumed {
                        ProgressDialogFragment.show(it.supportFragmentManager, "update_lists_progress")
                    }.then {
                        val activity = weakActivity.get() ?: throw IllegalStateException()
                        val twitter = MicroBlogAPIFactory.getInstance(activity, accountKey)
                        val successfulStates = SparseBooleanArray()
                        try {
                            for (i in 0 until checkedPositions.size()) {
                                val pos = checkedPositions.keyAt(i)
                                val checked = checkedPositions.valueAt(i)
                                if (states[pos] != checked) {
                                    if (checked) {
                                        twitter.addUserListMember(lists[pos].id, userKey.id)
                                    } else {
                                        twitter.deleteUserListMember(lists[pos].id, userKey.id)
                                    }
                                    successfulStates.put(pos, checked)
                                }
                            }
                        } catch (e: MicroBlogException) {
                            throw UpdateListsException(successfulStates)
                        }
                    }.alwaysUi {
                        val activity = weakActivity.get() as? IBaseActivity<*> ?: return@alwaysUi
                        activity.executeAfterFragmentResumed { activity ->
                            val manager = activity.supportFragmentManager
                            val df = manager.findFragmentByTag("update_lists_progress") as? DialogFragment
                            df?.dismiss()
                        }
                    }.successUi {
                        dismiss()
                    }.failUi { e ->
                        if (e is UpdateListsException) {
                            val successfulStates = e.successfulStates
                            for (i in 0 until successfulStates.size()) {
                                val pos = successfulStates.keyAt(i)
                                val checked = successfulStates.valueAt(i)
                                dialog.listView.setItemChecked(pos, checked)
                                states[pos] = checked
                            }
                        }
                        Toast.makeText(context, e.getErrorMessage(context), Toast.LENGTH_SHORT).show()
                    }
                }
                dialog.getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener {
                    val df = CreateUserListDialogFragment()
                    df.arguments = Bundle {
                        this[EXTRA_ACCOUNT_KEY] = accountKey
                    }
                    df.show(fragmentManager, "create_user_list")
                }
            }
            return dialog
        }

        class UpdateListsException(val successfulStates: SparseBooleanArray) : MicroBlogException()
    }

    companion object {

        private val sArgbEvaluator = ArgbEvaluator()
        private val LOADER_ID_USER = 1
        private val LOADER_ID_FRIENDSHIP = 2

        private val TAB_POSITION_STATUSES = 0
        private val TAB_POSITION_MEDIA = 1
        private val TAB_POSITION_FAVORITES = 2
        private val TAB_TYPE_STATUSES = "statuses"
        private val TAB_TYPE_STATUSES_WITH_REPLIES = "statuses_with_replies"
        private val TAB_TYPE_MEDIA = "media"
        private val TAB_TYPE_FAVORITES = "favorites"
    }
}