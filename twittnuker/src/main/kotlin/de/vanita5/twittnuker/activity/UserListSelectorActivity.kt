/*
 *  Twittnuker - Twitter client for Android
 *
 *  Copyright (C) 2013-2016 vanita5 <mail@vanit.as>
 *
 *  This program incorporates a modified version of Twidere.
 *  Copyright (C) 2012-2016 Mariotaku Lee <mariotaku.lee@gmail.com>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.vanita5.twittnuker.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.LoaderManager
import android.support.v4.app.hasRunningLoadersSafe
import android.support.v4.content.Loader
import android.view.View
import android.widget.AdapterView.OnItemClickListener
import android.widget.TextView
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.layout_list_with_empty_view.*
import org.mariotaku.ktextension.Bundle
import org.mariotaku.ktextension.set
import de.vanita5.twittnuker.R
import de.vanita5.twittnuker.TwittnukerConstants.REQUEST_SELECT_USER
import de.vanita5.twittnuker.adapter.SimpleParcelableUserListsAdapter
import de.vanita5.twittnuker.adapter.iface.ILoadMoreSupportAdapter
import de.vanita5.twittnuker.adapter.iface.ILoadMoreSupportAdapter.IndicatorPosition
import de.vanita5.twittnuker.constant.IntentConstants.*
import de.vanita5.twittnuker.loader.UserListOwnershipsLoader
import de.vanita5.twittnuker.loader.iface.ICursorSupportLoader
import de.vanita5.twittnuker.model.ParcelableUser
import de.vanita5.twittnuker.model.ParcelableUserList
import de.vanita5.twittnuker.model.UserKey
import de.vanita5.twittnuker.util.ContentScrollHandler
import de.vanita5.twittnuker.util.ListViewScrollHandler

class UserListSelectorActivity : BaseActivity(),
        ContentScrollHandler.ContentListSupport<SimpleParcelableUserListsAdapter>,
        LoaderManager.LoaderCallbacks<List<ParcelableUserList>> {

    override lateinit var adapter: SimpleParcelableUserListsAdapter

    private val accountKey: UserKey?
        get() = intent.getParcelableExtra<UserKey>(EXTRA_ACCOUNT_KEY)
    private val showMyLists: Boolean
        get() = intent.getBooleanExtra(EXTRA_SHOW_MY_LISTS, false)

    private var userKey: UserKey? = null
    private var nextCursor: Long = -1

    private var loaderInitialized: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val accountKey = accountKey ?: run {
            finish()
            return
        }
        setContentView(R.layout.activity_user_list_selector)

        adapter = SimpleParcelableUserListsAdapter(this, Glide.with(this))
        adapter.loadMoreSupportedPosition = ILoadMoreSupportAdapter.END
        listView.addFooterView(layoutInflater.inflate(R.layout.simple_list_item_activated_1,
                listView, false).apply {
            (findViewById(android.R.id.text1) as TextView).setText(R.string.action_select_user)
        }, SelectUserAction, true)
        listView.adapter = adapter
        val handler = ListViewScrollHandler(this, listView)
        listView.setOnScrollListener(handler)
        listView.setOnTouchListener(handler.touchListener)
        listView.onItemClickListener = OnItemClickListener { view, child, position, id ->
            val item = view.getItemAtPosition(position)
            when (item) {
                is ParcelableUserList -> {
                    val data = Intent()
                    data.putExtra(EXTRA_USER_LIST, item)
                    setResult(Activity.RESULT_OK, data)
                    finish()
                }
                is SelectUserAction -> {
                    selectUser()
                }
            }
        }

        val userKey = intent.getParcelableExtra<UserKey>(EXTRA_USER_KEY) ?: if (showMyLists) {
            accountKey
        } else {
            null
    }

        if (userKey != null) {
            loadUserLists(accountKey, userKey)
        } else if (savedInstanceState == null) {
            selectUser()
        }
    }

    override fun onStart() {
        super.onStart()
        bus.register(this)
    }

    override fun onStop() {
        bus.unregister(this)
        super.onStop()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_SELECT_USER -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val user = data.getParcelableExtra<ParcelableUser>(EXTRA_USER)
                    loadUserLists(accountKey!!, user.key)
                }
            }
        }
    }

    override fun onCreateLoader(id: Int, args: Bundle): Loader<List<ParcelableUserList>> {
        val accountKey = args.getParcelable<UserKey>(EXTRA_ACCOUNT_KEY)
        val userKey = args.getParcelable<UserKey>(EXTRA_USER_KEY)
        val nextCursor = args.getLong(EXTRA_NEXT_CURSOR)
        return UserListOwnershipsLoader(this, accountKey, userKey, null, nextCursor, adapter.all)
        }

    override fun onLoaderReset(loader: Loader<List<ParcelableUserList>>?) {
        adapter.setData(null)
    }


    override fun onLoadFinished(loader: Loader<List<ParcelableUserList>>?, data: List<ParcelableUserList>?) {
        adapter.loadMoreIndicatorPosition = ILoadMoreSupportAdapter.NONE
        adapter.loadMoreSupportedPosition = if (adapter.all != data) {
            ILoadMoreSupportAdapter.END
        } else {
            ILoadMoreSupportAdapter.NONE
        }
        adapter.setData(data)
        refreshing = false
        if (loader is ICursorSupportLoader) {
            nextCursor = loader.nextCursor
        }
        showList()
    }

    override fun setControlVisible(visible: Boolean) {
    }

    override var refreshing: Boolean
        get() {
            return supportLoaderManager.hasRunningLoadersSafe()
        }
        set(value) {
        }

    override val reachingStart: Boolean
        get() = listView.firstVisiblePosition <= 0

    override val reachingEnd: Boolean
        get() = listView.lastVisiblePosition >= listView.count - 1

    override fun onLoadMoreContents(@IndicatorPosition position: Long) {
        val accountKey = this.accountKey ?: return
        val userKey = this.userKey ?: return
        if (refreshing || position and adapter.loadMoreSupportedPosition == 0L) {
            return
        }
        adapter.loadMoreIndicatorPosition = position
        loadUserLists(accountKey, userKey, nextCursor)
    }

    private fun loadUserLists(accountKey: UserKey, userKey: UserKey, nextCursor: Long = -1) {
        if (userKey != this.userKey) {
            adapter.clear()
            showProgress()
            this.userKey = userKey
        }
        val args = Bundle {
            this[EXTRA_ACCOUNT_KEY] = accountKey
            this[EXTRA_USER_KEY] = userKey
            this[EXTRA_NEXT_CURSOR] = nextCursor
        }
        if (!loaderInitialized) {
            loaderInitialized = true
            supportLoaderManager.initLoader(0, args, this)
        } else {
            supportLoaderManager.restartLoader(0, args, this)
        }
    }

    private fun showProgress() {
        progressContainer.visibility = View.VISIBLE
        listContainer.visibility = View.GONE
    }

    private fun showList() {
        progressContainer.visibility = View.GONE
        listContainer.visibility = View.VISIBLE
        listView.visibility = View.VISIBLE
        emptyView.visibility = View.GONE
    }

    private fun selectUser() {
        val selectUserIntent = Intent(this, UserSelectorActivity::class.java)
        selectUserIntent.putExtra(EXTRA_ACCOUNT_KEY, accountKey)
        startActivityForResult(selectUserIntent, REQUEST_SELECT_USER)
    }

    object SelectUserAction

}