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

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentActivity
import android.support.v4.app.LoaderManager
import android.support.v4.content.AsyncTaskLoader
import android.support.v4.content.Loader
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.FixedLinearLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.*
import android.widget.CompoundButton
import kotlinx.android.synthetic.main.activity_home_content.view.*
import kotlinx.android.synthetic.main.fragment_messages_conversation_info.*
import kotlinx.android.synthetic.main.header_message_conversation_info.view.*
import kotlinx.android.synthetic.main.layout_toolbar_message_conversation_title.*
import org.mariotaku.abstask.library.TaskStarter
import org.mariotaku.chameleon.Chameleon
import org.mariotaku.chameleon.ChameleonUtils
import org.mariotaku.kpreferences.get
import org.mariotaku.ktextension.useCursor
import org.mariotaku.sqliteqb.library.Expression
import de.vanita5.twittnuker.R
import de.vanita5.twittnuker.activity.UserSelectorActivity
import de.vanita5.twittnuker.adapter.BaseRecyclerViewAdapter
import de.vanita5.twittnuker.adapter.iface.IItemCountsAdapter
import de.vanita5.twittnuker.constant.IntentConstants
import de.vanita5.twittnuker.constant.IntentConstants.*
import de.vanita5.twittnuker.constant.nameFirstKey
import de.vanita5.twittnuker.constant.profileImageStyleKey
import de.vanita5.twittnuker.extension.applyTheme
import de.vanita5.twittnuker.extension.getDirectMessageMaxParticipants
import de.vanita5.twittnuker.extension.model.displayAvatarTo
import de.vanita5.twittnuker.extension.model.getSubtitle
import de.vanita5.twittnuker.extension.model.getTitle
import de.vanita5.twittnuker.extension.model.notificationDisabled
import de.vanita5.twittnuker.extension.view.calculateSpaceItemHeight
import de.vanita5.twittnuker.fragment.BaseDialogFragment
import de.vanita5.twittnuker.fragment.BaseFragment
import de.vanita5.twittnuker.fragment.ProgressDialogFragment
import de.vanita5.twittnuker.fragment.iface.IToolBarSupportFragment
import de.vanita5.twittnuker.fragment.message.MessageConversationInfoFragment.ConversationInfoAdapter.Companion.VIEW_TYPE_BOTTOM_SPACE
import de.vanita5.twittnuker.fragment.message.MessageConversationInfoFragment.ConversationInfoAdapter.Companion.VIEW_TYPE_HEADER
import de.vanita5.twittnuker.model.*
import de.vanita5.twittnuker.model.ParcelableMessageConversation.ConversationType
import de.vanita5.twittnuker.model.ParcelableMessageConversation.ExtrasType
import de.vanita5.twittnuker.provider.TwidereDataStore.Messages.Conversations
import de.vanita5.twittnuker.task.twitter.message.AddParticipantsTask
import de.vanita5.twittnuker.task.twitter.message.DestroyConversationTask
import de.vanita5.twittnuker.task.twitter.message.SetConversationNotificationDisabledTask
import de.vanita5.twittnuker.util.IntentUtils
import de.vanita5.twittnuker.view.holder.SimpleUserViewHolder
import java.lang.ref.WeakReference


class MessageConversationInfoFragment : BaseFragment(), IToolBarSupportFragment,
        LoaderManager.LoaderCallbacks<ParcelableMessageConversation?> {

    private val accountKey: UserKey get() = arguments.getParcelable(EXTRA_ACCOUNT_KEY)
    private val conversationId: String get() = arguments.getString(EXTRA_CONVERSATION_ID)

    private lateinit var adapter: ConversationInfoAdapter
    private lateinit var itemDecoration: ConversationInfoDecoration

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

        adapter = ConversationInfoAdapter(context)
        adapter.listener = object : ConversationInfoAdapter.Listener {
            override fun onUserClick(position: Int) {
                val user = adapter.getUser(position) ?: return
                startActivity(IntentUtils.userProfile(user))
            }

            override fun onAddUserClick(position: Int) {
                val conversation = adapter.conversation ?: return
                val intent = Intent(IntentConstants.INTENT_ACTION_SELECT_USER)
                intent.putExtra(EXTRA_ACCOUNT_KEY, conversation.account_key)
                intent.setClass(context, UserSelectorActivity::class.java)
                startActivityForResult(intent, REQUEST_CONVERSATION_ADD_USER)
            }

            override fun onDisableNotificationChanged(disabled: Boolean) {
                performSetNotificationDisabled(disabled)
            }

        }
        itemDecoration = ConversationInfoDecoration(adapter,
                resources.getDimensionPixelSize(R.dimen.element_spacing_large)
        )

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LayoutManager(context)
        recyclerView.addItemDecoration(itemDecoration)


        val profileImageStyle = preferences[profileImageStyleKey]
        appBarIcon.style = profileImageStyle
        conversationAvatar.style = profileImageStyle

        val avatarBackground = ChameleonUtils.getColorDependent(theme.colorToolbar)
        appBarIcon.setBackgroundColor(avatarBackground)
        appBarTitle.setTextColor(ChameleonUtils.getColorDependent(theme.colorToolbar))
        appBarSubtitle.setTextColor(ChameleonUtils.getColorDependent(theme.colorToolbar))

        conversationAvatar.setBackgroundColor(avatarBackground)
        conversationTitle.setTextColor(ChameleonUtils.getColorDependent(theme.colorToolbar))
        conversationSubtitle.setTextColor(ChameleonUtils.getColorDependent(theme.colorToolbar))

        editButton.setOnClickListener {
            executeAfterFragmentResumed { fragment ->
                val df = EditInfoDialogFragment()
                df.show(fragment.childFragmentManager, "edit_info")
            }
        }

        loaderManager.initLoader(0, null, this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_CONVERSATION_ADD_USER -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val user = data.getParcelableExtra<ParcelableUser>(EXTRA_USER)
                    performAddParticipant(user)
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_messages_conversation_info, container, false)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_messages_conversation_info, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.leave_conversation -> {
                val df = DestroyConversationConfirmDialogFragment()
                df.show(childFragmentManager, "destroy_conversation_confirm")
                return true
            }
            R.id.delete_messages -> {

            }
        }
        return false
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

        val name = data.getTitle(context, userColorNameManager, preferences[nameFirstKey]).first
        val summary = data.getSubtitle(context)

        data.displayAvatarTo(mediaLoader, conversationAvatar)
        data.displayAvatarTo(mediaLoader, appBarIcon)
        appBarTitle.text = name
        conversationTitle.text = name
        if (summary != null) {
            appBarSubtitle.visibility = View.VISIBLE
            conversationSubtitle.visibility = View.VISIBLE

            appBarSubtitle.text = summary
            conversationSubtitle.text = summary
        } else {
            appBarSubtitle.visibility = View.GONE
            conversationSubtitle.visibility = View.GONE
        }
        if (data.conversation_extras_type == ExtrasType.TWITTER_OFFICIAL
                && data.conversation_type == ConversationType.GROUP) {
            editButton.visibility = View.VISIBLE
            adapter.showButtonSpace = true
        } else {
            editButton.visibility = View.GONE
            adapter.showButtonSpace = false
        }

        adapter.conversation = data
    }

    private fun performDestroyConversation() {
        ProgressDialogFragment.show(childFragmentManager, "leave_conversation_progress")
        val weakThis = WeakReference(this)
        val task = DestroyConversationTask(context, accountKey, conversationId)
        task.callback = callback@ { succeed ->
            val f = weakThis.get() ?: return@callback
            f.dismissAlertDialogThen("leave_conversation_progress") {
                if (succeed) {
                    activity?.setResult(RESULT_CLOSE)
                    activity?.finish()
                }
            }
        }
        TaskStarter.execute(task)
    }

    private fun performAddParticipant(user: ParcelableUser) {
        ProgressDialogFragment.show(childFragmentManager, "add_participant_progress")
        val weakThis = WeakReference(this)
        val task = AddParticipantsTask(context, accountKey, conversationId, listOf(user))
        task.callback = callback@ { succeed ->
            val f = weakThis.get() ?: return@callback
            f.dismissAlertDialogThen("add_participant_progress") {
                loaderManager.restartLoader(0, null, this@MessageConversationInfoFragment)
            }
        }
        TaskStarter.execute(task)
    }

    private fun performSetNotificationDisabled(disabled: Boolean) {
        ProgressDialogFragment.show(childFragmentManager, "set_notifications_disabled_progress")
        val weakThis = WeakReference(this)
        val task = SetConversationNotificationDisabledTask(context, accountKey, conversationId, disabled)
        task.callback = callback@ { succeed ->
            val f = weakThis.get() ?: return@callback
            f.dismissAlertDialogThen("set_notifications_disabled_progress") {
                loaderManager.restartLoader(0, null, this@MessageConversationInfoFragment)
            }
        }
        TaskStarter.execute(task)
    }


    private fun openEditAction(type: String) {
        when (type) {
            "name" -> {
                executeAfterFragmentResumed { fragment ->
                    val df = EditNameDialogFragment()
                    df.show(fragment.childFragmentManager, "edit_name")
                }
            }
            "avatar" -> {

            }
        }
    }

    private inline fun dismissAlertDialogThen(tag: String, crossinline action: BaseFragment.() -> Unit) {
        executeAfterFragmentResumed { fragment ->
            val df = fragment.childFragmentManager.findFragmentByTag(tag) as? DialogFragment
            df?.dismiss()
            action(fragment)
        }
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

    class ConversationInfoAdapter(context: Context) : BaseRecyclerViewAdapter<RecyclerView.ViewHolder>(context),
            IItemCountsAdapter {
        private val inflater = LayoutInflater.from(context)

        override val itemCounts: ItemCounts = ItemCounts(5)

        var listener: Listener? = null

        var conversation: ParcelableMessageConversation? = null
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        var showButtonSpace: Boolean = false
            set(value) {
                field = value
                notifyDataSetChanged()
            }


        init {
            setHasStableIds(true)
        }

        override fun getItemCount(): Int {
            val conversation = this.conversation ?: return 0
            val participantsSize = conversation.participants.size
            itemCounts[ITEM_INDEX_TOP_SPACE] = if (showButtonSpace) 1 else 0
            itemCounts[ITEM_INDEX_HEADER] = 1
            itemCounts[ITEM_INDEX_ITEM] = participantsSize
            when (conversation.conversation_type) {
                ConversationType.GROUP -> {
                    if (participantsSize < defaultFeatures.getDirectMessageMaxParticipants(conversation.conversation_extras_type)) {
                        itemCounts[ITEM_INDEX_ADD_USER] = 1
                    } else {
                        itemCounts[ITEM_INDEX_ADD_USER] = 0
                    }
                }
                else -> {
                    itemCounts[ITEM_INDEX_ADD_USER] = 0
                }
            }

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
                    val user = getUser(position)!!
                    (holder as UserViewHolder).display(user, participantIdx == 0)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            when (viewType) {
                VIEW_TYPE_TOP_SPACE -> {
                    val view = inflater.inflate(R.layout.header_message_conversation_info_button_space, parent, false)
                    return SpaceViewHolder(view)
                }
                VIEW_TYPE_HEADER -> {
                    val view = inflater.inflate(HeaderViewHolder.layoutResource, parent, false)
                    return HeaderViewHolder(view, this)
                }
                VIEW_TYPE_USER -> {
                    val view = inflater.inflate(R.layout.list_item_conversation_info_user, parent, false)
                    return UserViewHolder(view, this)
                }
                VIEW_TYPE_ADD_USER -> {
                    val view = inflater.inflate(R.layout.list_item_conversation_info_add_user, parent, false)
                    return AddUserViewHolder(view, this)
                }
                VIEW_TYPE_BOTTOM_SPACE -> {
                    val view = inflater.inflate(R.layout.list_item_conversation_info_space, parent, false)
                    return SpaceViewHolder(view)
                }
            }
            throw UnsupportedOperationException()
        }

        override fun getItemViewType(position: Int): Int {
            when (itemCounts.getItemCountIndex(position)) {
                ITEM_INDEX_TOP_SPACE -> return VIEW_TYPE_TOP_SPACE
                ITEM_INDEX_HEADER -> return VIEW_TYPE_HEADER
                ITEM_INDEX_ITEM -> return VIEW_TYPE_USER
                ITEM_INDEX_ADD_USER -> return VIEW_TYPE_ADD_USER
                ITEM_INDEX_SPACE -> return VIEW_TYPE_BOTTOM_SPACE
            }
            throw UnsupportedOperationException()
        }

        override fun getItemId(position: Int): Long {
            when (itemCounts.getItemCountIndex(position)) {
                ITEM_INDEX_ITEM -> {
                    val user = getUser(position)!!
                    return user.hashCode().toLong()
                }
                else -> {
                    return Integer.MAX_VALUE.toLong() + getItemViewType(position)
                }
            }
        }

        fun getUser(position: Int): ParcelableUser? {
            val itemPos = position - itemCounts.getItemStartPosition(ITEM_INDEX_ITEM)
            return conversation?.participants?.getOrNull(itemPos)
        }

        interface Listener {
            fun onUserClick(position: Int) {}
            fun onAddUserClick(position: Int) {}
            fun onDisableNotificationChanged(disabled: Boolean) {}

        }

        companion object {
            internal const val ITEM_INDEX_TOP_SPACE = 0
            internal const val ITEM_INDEX_HEADER = 1
            internal const val ITEM_INDEX_ITEM = 2
            internal const val ITEM_INDEX_ADD_USER = 3
            internal const val ITEM_INDEX_SPACE = 4

            internal const val VIEW_TYPE_TOP_SPACE = 0
            internal const val VIEW_TYPE_HEADER = 1
            internal const val VIEW_TYPE_USER = 2
            internal const val VIEW_TYPE_ADD_USER = 3
            internal const val VIEW_TYPE_BOTTOM_SPACE = 4

        }

    }

    internal class SpaceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    internal class AddUserViewHolder(itemView: View, adapter: ConversationInfoAdapter) : RecyclerView.ViewHolder(itemView) {

        private val itemContent = itemView.findViewById(R.id.itemContent)

        init {
            itemContent.setOnClickListener {
                adapter.listener?.onAddUserClick(layoutPosition)
            }
        }

    }

    internal class UserViewHolder(
            itemView: View,
            adapter: ConversationInfoAdapter
    ) : SimpleUserViewHolder<ConversationInfoAdapter>(itemView, adapter) {
        private val headerIcon = itemView.findViewById(R.id.headerIcon)

        private val itemContent = itemView.findViewById(R.id.itemContent)

        init {
            itemContent.setOnClickListener {
                adapter.listener?.onUserClick(layoutPosition)
            }
        }

        fun display(user: ParcelableUser, displayHeaderIcon: Boolean) {
            super.displayUser(user)
            headerIcon.visibility = if (displayHeaderIcon) View.VISIBLE else View.INVISIBLE
        }

    }

    internal class HeaderViewHolder(itemView: View, adapter: ConversationInfoAdapter) : RecyclerView.ViewHolder(itemView) {

        private val muteSwitch = itemView.muteNotifications

        private val listener = CompoundButton.OnCheckedChangeListener { button, checked ->
            adapter.listener?.onDisableNotificationChanged(checked)
        }

        fun display(conversation: ParcelableMessageConversation) {
            muteSwitch.setOnCheckedChangeListener(null)
            muteSwitch.isChecked = conversation.notificationDisabled
            muteSwitch.setOnCheckedChangeListener(listener)
        }

        companion object {
            const val layoutResource = R.layout.header_message_conversation_info
        }

    }

    internal class LayoutManager(
            context: Context
    ) : FixedLinearLayoutManager(context, LinearLayoutManager.VERTICAL, false) {

        override fun getDecoratedMeasuredHeight(child: View): Int {
            if (getItemViewType(child) == VIEW_TYPE_BOTTOM_SPACE) {
                return calculateSpaceItemHeight(child, VIEW_TYPE_BOTTOM_SPACE, VIEW_TYPE_HEADER)
            }
            return super.getDecoratedMeasuredHeight(child)
        }

    }

    class EditInfoDialogFragment : BaseDialogFragment() {

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val actions = arrayOf(Action(getString(R.string.action_edit_conversation_name), "name"),
                    Action(getString(R.string.action_edit_conversation_avatar), "avatar"))
            val builder = AlertDialog.Builder(context)
            builder.setItems(actions.map(Action::title).toTypedArray()) { dialog, which ->
                val action = actions[which]
                (parentFragment as MessageConversationInfoFragment).openEditAction(action.type)
            }
            val dialog = builder.create()
            dialog.setOnShowListener {
                it as AlertDialog
                it.applyTheme()
            }
            return dialog
        }

        data class Action(val title: String, val type: String)

    }

    class EditNameDialogFragment : BaseDialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val builder = AlertDialog.Builder(context)
            builder.setView(R.layout.dialog_edit_conversation_name)
            builder.setNegativeButton(android.R.string.cancel, null)
            builder.setPositiveButton(android.R.string.ok) { dialog, which ->

            }
            val dialog = builder.create()
            dialog.setOnShowListener {
                it as AlertDialog
                it.applyTheme()
            }
            return dialog
        }
    }

    class DestroyConversationConfirmDialogFragment : BaseDialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val builder = AlertDialog.Builder(context)
            builder.setMessage(R.string.message_destroy_conversation_confirm)
            builder.setPositiveButton(R.string.action_leave_conversation) { dialog, which ->
                (parentFragment as MessageConversationInfoFragment).performDestroyConversation()
            }
            builder.setNegativeButton(android.R.string.cancel, null)
            val dialog = builder.create()
            dialog.setOnShowListener {
                it as AlertDialog
                it.applyTheme()
            }
            return dialog
        }

    }

    internal class ConversationInfoDecoration(
            val adapter: ConversationInfoAdapter,
            val typeSpacing: Int
    ) : RecyclerView.ItemDecoration() {

        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
            val position = parent.getChildLayoutPosition(view)
            if (position < 0) return
            val itemCounts = adapter.itemCounts
            val countIndex = itemCounts.getItemCountIndex(position)
            when (countIndex) {
                ConversationInfoAdapter.ITEM_INDEX_TOP_SPACE,
                ConversationInfoAdapter.ITEM_INDEX_SPACE,
                ConversationInfoAdapter.ITEM_INDEX_ADD_USER -> {
                    outRect.setEmpty()
                }
                else -> {
                    // Previous item is space or first item
                    if (position == 0 || itemCounts.getItemCountIndex(position - 1)
                            == ConversationInfoAdapter.ITEM_INDEX_TOP_SPACE) {
                        outRect.setEmpty()
                    } else if (itemCounts.getItemStartPosition(countIndex) == position) {
                        outRect.set(0, typeSpacing, 0, 0)
                    } else {
                        outRect.setEmpty()
                    }
                }
            }
        }
    }

    companion object {
        const val RESULT_CLOSE = 101
        const val REQUEST_CONVERSATION_ADD_USER = 101
    }

}