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

import android.Manifest
import android.accounts.AccountManager
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.FragmentActivity
import android.support.v4.app.LoaderManager
import android.support.v4.content.ContextCompat
import android.support.v4.content.Loader
import android.support.v4.widget.TextViewCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.FixedLinearLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.*
import android.widget.Toast
import com.bumptech.glide.Glide
import com.squareup.otto.Subscribe
import kotlinx.android.synthetic.main.activity_premium_dashboard.*
import kotlinx.android.synthetic.main.fragment_messages_conversation.*
import kotlinx.android.synthetic.main.fragment_messages_conversation.view.*
import kotlinx.android.synthetic.main.layout_toolbar_message_conversation_title.*
import org.mariotaku.abstask.library.TaskStarter
import org.mariotaku.chameleon.Chameleon
import org.mariotaku.chameleon.ChameleonUtils
import org.mariotaku.kpreferences.get
import org.mariotaku.ktextension.contains
import org.mariotaku.ktextension.empty
import org.mariotaku.ktextension.mapToArray
import org.mariotaku.ktextension.set
import org.mariotaku.pickncrop.library.MediaPickerActivity
import org.mariotaku.sqliteqb.library.Expression
import org.mariotaku.sqliteqb.library.OrderBy
import de.vanita5.twittnuker.R
import de.vanita5.twittnuker.SecretConstants
import de.vanita5.twittnuker.TwittnukerConstants.REQUEST_PICK_MEDIA
import de.vanita5.twittnuker.activity.LinkHandlerActivity
import de.vanita5.twittnuker.activity.ThemedMediaPickerActivity
import de.vanita5.twittnuker.adapter.MediaPreviewAdapter
import de.vanita5.twittnuker.adapter.MessagesConversationAdapter
import de.vanita5.twittnuker.adapter.iface.ILoadMoreSupportAdapter
import de.vanita5.twittnuker.constant.IntentConstants.*
import de.vanita5.twittnuker.constant.nameFirstKey
import de.vanita5.twittnuker.constant.newDocumentApiKey
import de.vanita5.twittnuker.constant.profileImageStyleKey
import de.vanita5.twittnuker.extension.loadProfileImage
import de.vanita5.twittnuker.extension.model.*
import de.vanita5.twittnuker.fragment.AbsContentListRecyclerViewFragment
import de.vanita5.twittnuker.fragment.EditAltTextDialogFragment
import de.vanita5.twittnuker.fragment.iface.IToolBarSupportFragment
import de.vanita5.twittnuker.loader.ObjectCursorLoader
import de.vanita5.twittnuker.model.*
import de.vanita5.twittnuker.model.ParcelableMessageConversation.ConversationType
import de.vanita5.twittnuker.model.event.GetMessagesTaskEvent
import de.vanita5.twittnuker.model.event.SendMessageTaskEvent
import de.vanita5.twittnuker.model.util.AccountUtils
import de.vanita5.twittnuker.provider.TwidereDataStore.Messages
import de.vanita5.twittnuker.service.LengthyOperationsService
import de.vanita5.twittnuker.task.compose.AbsAddMediaTask
import de.vanita5.twittnuker.task.compose.AbsDeleteMediaTask
import de.vanita5.twittnuker.task.twitter.message.DestroyMessageTask
import de.vanita5.twittnuker.task.twitter.message.GetMessagesTask
import de.vanita5.twittnuker.task.twitter.message.MarkMessageReadTask
import de.vanita5.twittnuker.util.ClipboardUtils
import de.vanita5.twittnuker.util.DataStoreUtils
import de.vanita5.twittnuker.util.IntentUtils
import de.vanita5.twittnuker.util.PreviewGridItemDecoration
import de.vanita5.twittnuker.view.ExtendedRecyclerView
import de.vanita5.twittnuker.view.holder.compose.MediaPreviewViewHolder
import org.mariotaku.ktextension.checkAnySelfPermissionsGranted
import xyz.klinker.giphy.Giphy
import xyz.klinker.giphy.GiphyActivity
import java.util.concurrent.atomic.AtomicReference

class MessagesConversationFragment : AbsContentListRecyclerViewFragment<MessagesConversationAdapter>(),
        IToolBarSupportFragment, LoaderManager.LoaderCallbacks<List<ParcelableMessage>?>,
        EditAltTextDialogFragment.EditAltTextCallback {
    private lateinit var mediaPreviewAdapter: MediaPreviewAdapter

    private val accountKey: UserKey get() = arguments.getParcelable(EXTRA_ACCOUNT_KEY)

    private val conversationId: String get() = arguments.getString(EXTRA_CONVERSATION_ID)

    private val account: AccountDetails? by lazy {
        AccountUtils.getAccountDetails(AccountManager.get(context), accountKey, true)
    }

    private val loadMoreTaskTag: String
        get() = "loadMore:$accountKey:$conversationId"

    // Layout manager reversed, so treat start as end
    override val reachingEnd: Boolean
        get() = super.reachingStart

    // Layout manager reversed, so treat end as start
    override val reachingStart: Boolean
        get() = super.reachingEnd

    override val controlBarHeight: Int
        get() = toolbar.height

    override var controlBarOffset: Float = 1f

    override val toolbar: Toolbar
        get() = conversationContainer.toolbar

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true)
        val account = this.account ?: run {
            activity?.finish()
            return
        }
        adapter.listener = object : MessagesConversationAdapter.Listener {
            override fun onMediaClick(position: Int, media: ParcelableMedia, accountKey: UserKey?) {
                val message = adapter.getMessage(position) ?: return
                IntentUtils.openMediaDirectly(context = context, accountKey = accountKey,
                        media = message.media, current = media,
                        newDocument = preferences[newDocumentApiKey], message = message)
            }

            override fun onMessageLongClick(position: Int, holder: RecyclerView.ViewHolder): Boolean {
                return recyclerView.showContextMenuForChild(holder.itemView)
            }
        }
        mediaPreviewAdapter = MediaPreviewAdapter(context, Glide.with(this))

        mediaPreviewAdapter.listener = object : MediaPreviewAdapter.Listener {
            override fun onRemoveClick(position: Int, holder: MediaPreviewViewHolder) {
                val task = DeleteMediaTask(this@MessagesConversationFragment,
                        arrayOf(mediaPreviewAdapter.getItem(position)))
                TaskStarter.execute(task)
            }

            override fun onEditClick(position: Int, holder: MediaPreviewViewHolder) {
                attachedMediaPreview.showContextMenuForChild(holder.itemView)
            }
        }
        attachedMediaPreview.layoutManager = FixedLinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        attachedMediaPreview.adapter = mediaPreviewAdapter
        attachedMediaPreview.addItemDecoration(PreviewGridItemDecoration(resources.getDimensionPixelSize(R.dimen.element_spacing_small)))

        registerForContextMenu(recyclerView)
        registerForContextMenu(attachedMediaPreview)

        sendMessage.setOnClickListener {
            performSendMessage()
        }
        addMedia.setOnClickListener {
            openMediaPicker()
        }
        conversationTitleContainer.setOnClickListener {
            val intent = IntentUtils.messageConversationInfo(accountKey, conversationId)
            startActivityForResult(intent, REQUEST_MANAGE_CONVERSATION_INFO)
        }

        val activity = this.activity
        if (activity is AppCompatActivity) {
            activity.supportActionBar?.setDisplayShowTitleEnabled(false)
        }
        val theme = Chameleon.getOverrideTheme(context, activity)
        conversationTitle.setTextColor(ChameleonUtils.getColorDependent(theme.colorToolbar))
        conversationSubtitle.setTextColor(ChameleonUtils.getColorDependent(theme.colorToolbar))

        conversationAvatar.style = preferences[profileImageStyleKey]

        // No refresh for this fragment
        refreshEnabled = false
        adapter.loadMoreSupportedPosition = ILoadMoreSupportAdapter.NONE

        if (account.isOfficial(context)) {
            addMedia.visibility = View.VISIBLE
        } else {
            addMedia.visibility = View.GONE
        }

        updateMediaPreview()

        loaderManager.initLoader(0, null, this)
        showProgress()
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
        when (resultCode) {
            Giphy.REQUEST_GIPHY -> {
                requestOrPickGif()
            }
        }
        when (requestCode) {
            REQUEST_PICK_MEDIA, Giphy.REQUEST_GIPHY -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val mediaUris = MediaPickerActivity.getMediaUris(data)
                    TaskStarter.execute(AddMediaTask(this, mediaUris))
                }
            }
            REQUEST_MANAGE_CONVERSATION_INFO -> {
                if (resultCode == MessageConversationInfoFragment.RESULT_CLOSE) {
                    activity?.finish()
                }
            }
        }
    }

    private fun requestOrPickGif() {
        if (this.context.checkAnySelfPermissionsGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            pickGif()
            return
        }
        ActivityCompat.requestPermissions(this.activity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_PICK_MEDIA_PERMISSION)
    }

    private fun pickGif(): Boolean {
//        Giphy.Builder(this.activity, SecretConstants.GIPHY_API_KEY).maxFileSize(10 * 1024 * 1024).build()

        val intent = Intent(activity, GiphyActivity::class.java)
        intent.putExtra(GiphyActivity.EXTRA_API_KEY, SecretConstants.GIPHY_API_KEY)
        intent.putExtra(GiphyActivity.EXTRA_SIZE_LIMIT, 10 * 1024 * 1024)
        startActivityForResult(intent, Giphy.REQUEST_GIPHY)
        return true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_messages_conversation, container, false)
    }

    override fun setupWindow(activity: FragmentActivity): Boolean {
        return false
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<List<ParcelableMessage>?> {
        return ConversationLoader(context, accountKey, conversationId)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_messages_conversation, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
        }
        return false
    }

    override fun onLoaderReset(loader: Loader<List<ParcelableMessage>?>) {
        adapter.setData(null, null)
    }

    override fun onLoadFinished(loader: Loader<List<ParcelableMessage>?>, data: List<ParcelableMessage>?) {
        val conversationLoader = loader as? ConversationLoader
        val conversation = conversationLoader?.conversation
        adapter.setData(conversation, data)
        adapter.displaySenderProfile = conversation?.conversation_type == ConversationType.GROUP
        if (conversation?.conversation_extras_type == ParcelableMessageConversation.ExtrasType.TWITTER_OFFICIAL) {
            adapter.loadMoreSupportedPosition = ILoadMoreSupportAdapter.START
        } else {
            adapter.loadMoreSupportedPosition = ILoadMoreSupportAdapter.NONE
        }
        showContent()

        if (conversation != null && !conversation.is_temp) {
            markRead()
        }
        updateConversationStatus()
    }

    override fun onCreateAdapter(context: Context): MessagesConversationAdapter {
        return MessagesConversationAdapter(context, Glide.with(this))
    }

    override fun onCreateLayoutManager(context: Context): LinearLayoutManager {
        return FixedLinearLayoutManager(context, LinearLayoutManager.VERTICAL, true)
    }

    override fun onCreateItemDecoration(context: Context, recyclerView: RecyclerView,
                                      layoutManager: LinearLayoutManager): RecyclerView.ItemDecoration? {
        return null
    }

    override fun onLoadMoreContents(position: Long) {
        if (ILoadMoreSupportAdapter.START !in position) return
        val message = adapter.getMessage(adapter.messageRange.endInclusive) ?: return
        setLoadMoreIndicatorPosition(position)
        val param = GetMessagesTask.LoadMoreMessageTaskParam(context, accountKey, conversationId,
                message.id)
        param.taskTag = loadMoreTaskTag
        twitterWrapper.getMessagesAsync(param)
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo) {
        if (menuInfo !is ExtendedRecyclerView.ContextMenuInfo) return
        when (menuInfo.recyclerViewId) {
            R.id.recyclerView -> {
                val message = adapter.getMessage(menuInfo.position) ?: return
                val conversation = adapter.conversation
                menu.setHeaderTitle(message.getSummaryText(context, userColorNameManager, conversation,
                        preferences[nameFirstKey]))
                activity.menuInflater.inflate(R.menu.menu_conversation_message_item, menu)
            }
            R.id.attachedMediaPreview -> {
                menu.setHeaderTitle(R.string.edit_media)
                activity.menuInflater.inflate(R.menu.menu_attached_media_edit, menu)
            }
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val menuInfo = item.menuInfo as? ExtendedRecyclerView.ContextMenuInfo ?: run {
            return super.onContextItemSelected(item)
        }
        when (menuInfo.recyclerViewId) {
            R.id.recyclerView -> {
                val message = adapter.getMessage(menuInfo.position) ?: return true
                when (item.itemId) {
                    R.id.copy -> {
                        ClipboardUtils.setText(context, message.text_unescaped)
                    }
                    R.id.delete -> {
                        val task = DestroyMessageTask(context, message.account_key,
                                message.conversation_id, message.id)
                        TaskStarter.execute(task)
                    }
                }
                return true
            }
            R.id.attachedMediaPreview -> {
                when (item.itemId) {
                    R.id.edit_description -> {
                        val position = menuInfo.position
                        val altText = mediaPreviewAdapter.getItem(position).alt_text
                        executeAfterFragmentResumed { fragment ->
                            EditAltTextDialogFragment.show(fragment.childFragmentManager, position,
                                    altText)
                        }
                    }
                }
                return true
            }
        }
        return super.onContextItemSelected(item)
    }

    override fun onSetAltText(position: Int, altText: String?) {
        mediaPreviewAdapter.setAltText(position, altText)
    }

    @Subscribe
    fun onGetMessagesTaskEvent(event: GetMessagesTaskEvent) {
        if (!event.running && event.taskTag == loadMoreTaskTag) {
            setLoadMoreIndicatorPosition(ILoadMoreSupportAdapter.NONE)
        }
    }

    @Subscribe
    fun onSendMessageTaskEvent(event: SendMessageTaskEvent) {
        if (!event.success || event.accountKey != accountKey || event.conversationId != conversationId) {
            return
        }
        val newConversationId = event.newConversationId ?: return
        arguments[EXTRA_CONVERSATION_ID] = newConversationId
        if (activity is LinkHandlerActivity) {
            activity.intent = IntentUtils.messageConversation(accountKey, newConversationId)
        }
        loaderManager.restartLoader(0, null, this)
    }

    private fun performSendMessage() {
        val conversation = adapter.conversation ?: return
        val conversationAccount = this.account ?: return
        if (conversation.readOnly) return
        if (editText.empty && mediaPreviewAdapter.itemCount == 0) {
            editText.error = getString(R.string.hint_error_message_no_content)
            return
        }
        if (conversationAccount.isOfficial(context)) {
            if (mediaPreviewAdapter.itemCount > defaultFeatures.twitterDirectMessageMediaLimit) {
                editText.error = getString(R.string.error_message_media_message_too_many)
                return
            } else {
                editText.error = null
            }
        } else if (mediaPreviewAdapter.itemCount > 0) {
            editText.error = getString(R.string.error_message_media_message_attachment_not_supported)
            return
        }
        val text = editText.text.toString()
        val message = ParcelableNewMessage().apply {
            this.account = conversationAccount
            this.media = mediaPreviewAdapter.asList().toTypedArray()
            this.conversation_id = conversation.id
            this.recipient_ids = conversation.participants?.filter {
                it.key != accountKey
            }?.map {
                it.key.id
            }?.toTypedArray()
            this.text = text
            this.is_temp_conversation = conversation.is_temp
        }
        LengthyOperationsService.sendMessageAsync(context, message)
        editText.text = null

        // Clear media, those media will be deleted after sent
        mediaPreviewAdapter.clear()
        updateMediaPreview()
    }

    private fun openMediaPicker() {
        val intent = ThemedMediaPickerActivity.withThemed(context)
                .pickSources(arrayOf(MediaPickerActivity.SOURCE_CAMERA,
                        MediaPickerActivity.SOURCE_CAMCORDER,
                        MediaPickerActivity.SOURCE_GALLERY,
                        MediaPickerActivity.SOURCE_CLIPBOARD))
                .addEntry(getString(R.string.add_gif), INTENT_ACTION_PICK_GIF, Giphy.REQUEST_GIPHY)
                .containsVideo(true)
                .allowMultiple(false)
                .build()
        startActivityForResult(intent, REQUEST_PICK_MEDIA)
    }

    private fun attachMedia(media: List<ParcelableMediaUpdate>) {
        mediaPreviewAdapter.addAll(media)
        updateMediaPreview()
    }

    private fun removeMedia(media: List<ParcelableMediaUpdate>) {
        mediaPreviewAdapter.removeAll(media)
        updateMediaPreview()
    }

    private fun updateMediaPreview() {
        attachedMediaPreview.visibility = if (mediaPreviewAdapter.itemCount > 0) {
            View.VISIBLE
        } else {
            View.GONE
        }
        editText.error = null
    }

    private fun setProgressVisible(visible: Boolean) {

    }

    private fun markRead() {
        TaskStarter.execute(MarkMessageReadTask(context, accountKey, conversationId))
    }

    private fun updateConversationStatus() {
        val conversation = adapter.conversation ?: return
        val title = conversation.getTitle(context, userColorNameManager,
                preferences[nameFirstKey]).first
        val subtitle = conversation.getSubtitle(context)
        activity.title = title
        val readOnly = conversation.readOnly
        addMedia.isEnabled = !readOnly
        sendMessage.isEnabled = !readOnly
        editText.isEnabled = !readOnly

        conversationTitle.text = title
        if (subtitle != null) {
            conversationSubtitle.visibility = View.VISIBLE
            conversationSubtitle.text = subtitle
        } else {
            conversationSubtitle.visibility = View.GONE
        }


        val stateIcon = if (conversation.notificationDisabled) {
            ContextCompat.getDrawable(context, R.drawable.ic_message_type_speaker_muted).apply {
                mutate()
                setColorFilter(conversationTitle.currentTextColor, PorterDuff.Mode.SRC_ATOP)
            }
        } else {
            null
        }
        TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(conversationTitle, null,
                null, stateIcon, null)

        Glide.with(this).loadProfileImage(context, conversation, preferences[profileImageStyleKey])
                .into(conversationAvatar)
    }

    internal class AddMediaTask(
            fragment: MessagesConversationFragment,
            sources: Array<Uri>
    ) : AbsAddMediaTask<MessagesConversationFragment>(fragment.context, sources) {

        init {
            callback = fragment
        }

        override fun afterExecute(callback: MessagesConversationFragment?, result: List<ParcelableMediaUpdate>?) {
            if (callback == null || result == null) return
            callback.setProgressVisible(false)
            callback.attachMedia(result)
        }

        override fun beforeExecute() {
            val fragment = callback ?: return
            fragment.setProgressVisible(true)
        }

    }

    internal class DeleteMediaTask(
            fragment: MessagesConversationFragment,
            val media: Array<ParcelableMediaUpdate>
    ) : AbsDeleteMediaTask<MessagesConversationFragment>(fragment.context,
            media.mapToArray { Uri.parse(it.uri) }) {

        init {
            callback = fragment
        }

        override fun afterExecute(callback: MessagesConversationFragment?, result: BooleanArray?) {
            if (callback == null || result == null) return
            callback.setProgressVisible(false)
            callback.removeMedia(media.filterIndexed { i, _ -> result[i] })
            if (result.any { false }) {
                Toast.makeText(callback.context, R.string.message_toast_error_occurred, Toast.LENGTH_SHORT).show()
            }
        }

        override fun beforeExecute() {
            val fragment = callback ?: return
            fragment.setProgressVisible(true)
        }

    }

    internal class ConversationLoader(
            context: Context,
            val accountKey: UserKey,
            val conversationId: String
    ) : ObjectCursorLoader<ParcelableMessage>(context, ParcelableMessage::class.java) {

        private val atomicConversation = AtomicReference<ParcelableMessageConversation?>()
        val conversation: ParcelableMessageConversation? get() = atomicConversation.get()

        init {
            uri = Messages.CONTENT_URI
            projection = Messages.COLUMNS
            selection = Expression.and(Expression.equalsArgs(Messages.ACCOUNT_KEY),
                    Expression.equalsArgs(Messages.CONVERSATION_ID)).sql
            selectionArgs = arrayOf(accountKey.toString(), conversationId)
            sortOrder = OrderBy(Messages.SORT_ID, false).sql
        }

        override fun onLoadInBackground(): MutableList<ParcelableMessage> {
            atomicConversation.set(DataStoreUtils.findMessageConversation(context, accountKey, conversationId))
            return super.onLoadInBackground()
        }
    }

    companion object {
        private const val REQUEST_MANAGE_CONVERSATION_INFO = 101

        private const val REQUEST_PICK_MEDIA_PERMISSION = 302
    }

}
