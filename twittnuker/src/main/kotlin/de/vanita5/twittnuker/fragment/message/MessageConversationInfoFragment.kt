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

package de.vanita5.twittnuker.fragment.message

import android.content.Context
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.support.v4.app.LoaderManager
import android.support.v4.content.AsyncTaskLoader
import android.support.v4.content.Loader
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.FixedLinearLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.*
import kotlinx.android.synthetic.main.activity_home_content.view.*
import kotlinx.android.synthetic.main.fragment_messages_conversation_info.*
import kotlinx.android.synthetic.main.header_message_conversation_info.view.*
import kotlinx.android.synthetic.main.layout_toolbar_message_conversation_title.*
import org.mariotaku.chameleon.Chameleon
import org.mariotaku.chameleon.ChameleonUtils
import org.mariotaku.kpreferences.get
import org.mariotaku.ktextension.useCursor
import org.mariotaku.sqliteqb.library.Expression
import de.vanita5.twittnuker.R
import de.vanita5.twittnuker.adapter.BaseRecyclerViewAdapter
import de.vanita5.twittnuker.adapter.iface.IContentAdapter
import de.vanita5.twittnuker.adapter.iface.IItemCountsAdapter
import de.vanita5.twittnuker.constant.IntentConstants.EXTRA_ACCOUNT_KEY
import de.vanita5.twittnuker.constant.IntentConstants.EXTRA_CONVERSATION_ID
import de.vanita5.twittnuker.constant.nameFirstKey
import de.vanita5.twittnuker.constant.profileImageStyleKey
import de.vanita5.twittnuker.extension.model.displayAvatarTo
import de.vanita5.twittnuker.extension.model.getConversationName
import de.vanita5.twittnuker.extension.model.notificationDisabled
import de.vanita5.twittnuker.extension.view.calculateSpaceItemHeight
import de.vanita5.twittnuker.fragment.BaseFragment
import de.vanita5.twittnuker.fragment.iface.IToolBarSupportFragment
import de.vanita5.twittnuker.fragment.message.MessageConversationInfoFragment.ConversationInfoAdapter.Companion.VIEW_TYPE_HEADER
import de.vanita5.twittnuker.fragment.message.MessageConversationInfoFragment.ConversationInfoAdapter.Companion.VIEW_TYPE_SPACE
import de.vanita5.twittnuker.model.*
import de.vanita5.twittnuker.provider.TwidereDataStore.Messages.Conversations
import de.vanita5.twittnuker.view.holder.SimpleUserViewHolder


class MessageConversationInfoFragment : BaseFragment(), IToolBarSupportFragment,
        LoaderManager.LoaderCallbacks<ParcelableMessageConversation?> {

    private val accountKey: UserKey get() = arguments.getParcelable(EXTRA_ACCOUNT_KEY)
    private val conversationId: String get() = arguments.getString(EXTRA_CONVERSATION_ID)

    private lateinit var adapter: ConversationInfoAdapter

    override val controlBarHeight: Int get() = toolbar.measuredHeight
    override var controlBarOffset: Float = 0f

    override val toolbar: Toolbar
        get() = toolbarLayout.toolbar

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true)
        val activity = this.activity

        if (activity is AppCompatActivity) {
            activity.supportActionBar?.setDisplayShowTitleEnabled(false)
        }
        val theme = Chameleon.getOverrideTheme(context, activity)

        adapter = ConversationInfoAdapter(context, theme)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LayoutManager(context)


        val profileImageStyle = preferences[profileImageStyleKey]
        appBarIcon.style = profileImageStyle
        conversationAvatar.style = profileImageStyle

        val avatarBackground = ChameleonUtils.getColorDependent(theme.colorToolbar)
        appBarIcon.setBackgroundColor(avatarBackground)
        appBarTitle.setTextColor(ChameleonUtils.getColorDependent(theme.colorToolbar))
        appBarSubtitle.setTextColor(ChameleonUtils.getColorDependent(theme.colorToolbar))

        conversationAvatar.setBackgroundColor(avatarBackground)
        conversationName.setTextColor(ChameleonUtils.getColorDependent(theme.colorToolbar))
        conversationSummary.setTextColor(ChameleonUtils.getColorDependent(theme.colorToolbar))

        loaderManager.initLoader(0, null, this)


    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_messages_conversation_info, container, false)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_messages_conversation_info, menu)
    }

    override fun setupWindow(activity: FragmentActivity): Boolean {
        return false
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<ParcelableMessageConversation?> {
        return ConversationInfoLoader(context, accountKey, conversationId)
    }

    override fun onLoaderReset(loader: Loader<ParcelableMessageConversation?>?) {
    }

    override fun onLoadFinished(loader: Loader<ParcelableMessageConversation?>?, data: ParcelableMessageConversation?) {
        if (data == null) {
            activity?.finish()
            return
        }
        adapter.conversation = data

        val name = data.getConversationName(context, userColorNameManager, preferences[nameFirstKey]).first
        val summary = resources.getQuantityString(R.plurals.N_message_participants, data.participants.size, data.participants.size)

        data.displayAvatarTo(mediaLoader, conversationAvatar)
        data.displayAvatarTo(mediaLoader, appBarIcon)
        appBarTitle.text = name
        appBarSubtitle.text = summary
        conversationName.text = name
        conversationSummary.text = summary
    }

    class ConversationInfoLoader(
            context: Context,
            val accountKey: UserKey,
            val conversationId: String) : AsyncTaskLoader<ParcelableMessageConversation?>(context) {
        override fun loadInBackground(): ParcelableMessageConversation? {
            val where = Expression.and(Expression.equalsArgs(Conversations.ACCOUNT_KEY),
                    Expression.equalsArgs(Conversations.CONVERSATION_ID)).sql
            val whereArgs = arrayOf(accountKey.toString(), conversationId)
            context.contentResolver.query(Conversations.CONTENT_URI, Conversations.COLUMNS, where,
                    whereArgs, null).useCursor { cur ->
                if (cur.moveToFirst()) {
                    return ParcelableMessageConversationCursorIndices.fromCursor(cur)
                }
            }
            return null
        }

        override fun onStartLoading() {
            forceLoad()
        }
    }

    class ConversationInfoAdapter(context: Context, val theme: Chameleon.Theme) : BaseRecyclerViewAdapter<RecyclerView.ViewHolder>(context),
            IItemCountsAdapter {
        private val inflater = LayoutInflater.from(context)
        override val itemCounts: ItemCounts = ItemCounts(4)
        var conversation: ParcelableMessageConversation? = null
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        override fun getItemCount(): Int {
            val conversation = this.conversation ?: return 0
            itemCounts[ITEM_INDEX_HEADER] = 1
            itemCounts[ITEM_INDEX_ITEM] = conversation.participants.size
            itemCounts[ITEM_INDEX_ADD_USER] = 1
            itemCounts[ITEM_INDEX_SPACE] = 1
            return itemCounts.itemCount
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (holder.itemViewType) {
                VIEW_TYPE_HEADER -> {
                    (holder as HeaderViewHolder).display(this.conversation!!)
                }
                VIEW_TYPE_USER -> {
                    val participantIdx = position - itemCounts.getItemStartPosition(ITEM_INDEX_ITEM)
                    val user = this.conversation!!.participants[participantIdx]
                    (holder as UserViewHolder).display(user, participantIdx == 0)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            when (viewType) {
                VIEW_TYPE_HEADER -> {
                    val view = inflater.inflate(HeaderViewHolder.layoutResource, parent, false)
                    return HeaderViewHolder(view)
                }
                VIEW_TYPE_USER -> {
                    val view = inflater.inflate(R.layout.list_item_conversation_info_user, parent, false)
                    return UserViewHolder(view, this)
                }
                VIEW_TYPE_ADD_USER -> {
                    val view = inflater.inflate(R.layout.list_item_conversation_info_add_user, parent, false)
                    return AddUserViewHolder(view, theme)
                }
                VIEW_TYPE_SPACE -> {
                    val view = inflater.inflate(R.layout.list_item_conversation_info_space, parent, false)
                    return SpaceViewHolder(view)
                }
            }
            throw UnsupportedOperationException()
        }

        override fun getItemViewType(position: Int): Int {
            when (itemCounts.getItemCountIndex(position)) {
                ITEM_INDEX_HEADER -> return VIEW_TYPE_HEADER
                ITEM_INDEX_ITEM -> return VIEW_TYPE_USER
                ITEM_INDEX_ADD_USER -> return VIEW_TYPE_ADD_USER
                ITEM_INDEX_SPACE -> return VIEW_TYPE_SPACE
            }
            throw UnsupportedOperationException()
        }

        companion object {
            private const val ITEM_INDEX_HEADER = 0
            private const val ITEM_INDEX_ITEM = 1
            private const val ITEM_INDEX_ADD_USER = 2
            private const val ITEM_INDEX_SPACE = 3

            internal const val VIEW_TYPE_HEADER = 1
            internal const val VIEW_TYPE_USER = 2
            internal const val VIEW_TYPE_ADD_USER = 3
            internal const val VIEW_TYPE_SPACE = 4
        }

    }

    internal class SpaceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    internal class AddUserViewHolder(itemView: View, theme: Chameleon.Theme) : RecyclerView.ViewHolder(itemView)

    internal class UserViewHolder(itemView: View, adapter: IContentAdapter) : SimpleUserViewHolder(itemView, adapter) {
        private val headerIcon = itemView.findViewById(R.id.headerIcon)
        fun display(user: ParcelableUser, displayHeaderIcon: Boolean) {
            super.displayUser(user)
            headerIcon.visibility = if (displayHeaderIcon) View.VISIBLE else View.INVISIBLE
        }
    }

    internal class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val muteSwitch = itemView.muteNotifications

        fun display(conversation: ParcelableMessageConversation) {
            muteSwitch.isChecked = conversation.notificationDisabled
        }

        companion object {
            const val layoutResource = R.layout.header_message_conversation_info

        }
    }

    internal class LayoutManager(
            context: Context
    ) : FixedLinearLayoutManager(context, LinearLayoutManager.VERTICAL, false) {

        override fun getDecoratedMeasuredHeight(child: View): Int {
            if (getItemViewType(child) == VIEW_TYPE_SPACE) {
                return calculateSpaceItemHeight(child, VIEW_TYPE_SPACE, VIEW_TYPE_HEADER)
            }
            return super.getDecoratedMeasuredHeight(child)
        }

    }
}