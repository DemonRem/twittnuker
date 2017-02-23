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

import android.accounts.AccountManager
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.support.v4.app.LoaderManager.LoaderCallbacks
import android.support.v4.content.Loader
import android.support.v7.widget.LinearLayoutManager
import android.text.Editable
import android.text.Spannable
import android.text.TextUtils
import android.text.style.ReplacementSpan
import android.view.*
import kotlinx.android.synthetic.main.fragment_messages_conversation_new.*
import org.mariotaku.kpreferences.get
import org.mariotaku.ktextension.Bundle
import org.mariotaku.ktextension.set
import org.mariotaku.ktextension.setItemAvailability
import de.vanita5.twittnuker.R
import de.vanita5.twittnuker.adapter.SelectableUsersAdapter
import de.vanita5.twittnuker.constant.IntentConstants.*
import de.vanita5.twittnuker.constant.nameFirstKey
import de.vanita5.twittnuker.extension.model.isOfficial
import de.vanita5.twittnuker.fragment.BaseFragment
import de.vanita5.twittnuker.loader.CacheUserSearchLoader
import de.vanita5.twittnuker.model.ParcelableMessageConversation
import de.vanita5.twittnuker.model.ParcelableMessageConversation.ConversationType
import de.vanita5.twittnuker.model.ParcelableMessageConversationValuesCreator
import de.vanita5.twittnuker.model.ParcelableUser
import de.vanita5.twittnuker.model.UserKey
import de.vanita5.twittnuker.model.util.AccountUtils
import de.vanita5.twittnuker.provider.TwidereDataStore.Messages.Conversations
import de.vanita5.twittnuker.task.twitter.message.SendMessageTask
import de.vanita5.twittnuker.text.MarkForDeleteSpan
import de.vanita5.twittnuker.util.IntentUtils
import de.vanita5.twittnuker.util.view.SimpleTextWatcher
import java.lang.ref.WeakReference

class MessageNewConversationFragment : BaseFragment(), LoaderCallbacks<List<ParcelableUser>?> {

    private val accountKey by lazy { arguments.getParcelable<UserKey>(EXTRA_ACCOUNT_KEY) }
    private val account by lazy {
        AccountUtils.getAccountDetails(AccountManager.get(context), accountKey, true)
    }

    private val selectedRecipients: List<ParcelableUser>
        get() {
            val text = editParticipants.editableText ?: return emptyList()
            return text.getSpans(0, text.length, ParticipantSpan::class.java).map(ParticipantSpan::user)
        }

    private var loaderInitialized: Boolean = false
    private var performSearchRequestRunnable: Runnable? = null

    private lateinit var usersAdapter: SelectableUsersAdapter

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true)
        usersAdapter = SelectableUsersAdapter(context)
        recyclerView.adapter = usersAdapter
        recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

        editParticipants.addTextChangedListener(object : SimpleTextWatcher {
            override fun afterTextChanged(s: Editable) {
                s.getSpans(0, s.length, MarkForDeleteSpan::class.java).forEach { span ->
                    val deleteStart = s.getSpanStart(span)
                    val deleteEnd = s.getSpanEnd(span)
                    s.removeSpan(span)
                    s.delete(deleteStart, deleteEnd)
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                super.beforeTextChanged(s, start, count, after)
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (s !is Spannable) return
                s.getSpans(0, s.length, PendingQuerySpan::class.java).forEach { span ->
                    s.removeSpan(span)
                }
                // Processing deletion
                if (count < before) {
                    val spans = s.getSpans(start, start, ParticipantSpan::class.java)
                    if (spans.isNotEmpty()) {
                        spans.forEach { span ->
                            val deleteStart = s.getSpanStart(span)
                            val deleteEnd = s.getSpanEnd(span)
                            s.removeSpan(span)
                            s.setSpan(MarkForDeleteSpan(), deleteStart, deleteEnd,
                                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                            updateCheckState()
                        }
                        return
                    }
                }
                val spaceNextStart = run {
                    val spaceIdx = s.indexOfLast(Char::isWhitespace)
                    if (spaceIdx < 0) return@run 0
                    return@run spaceIdx + 1
                }
                // Skip if last char is space
                if (spaceNextStart > s.lastIndex) return
                if (s.getSpans(start, start + count, ParticipantSpan::class.java).isEmpty()) {
                    s.setSpan(PendingQuerySpan(), spaceNextStart, start + count, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    searchUser(s.substring(spaceNextStart), true)
                }
            }
        })
        val nameFirst = preferences[nameFirstKey]
        val roundRadius = resources.getDimension(R.dimen.element_spacing_xsmall)
        val spanPadding = resources.getDimension(R.dimen.element_spacing_xsmall)
        usersAdapter.itemCheckedListener = itemChecked@ { pos, checked ->
            val text: Editable = editParticipants.editableText ?: return@itemChecked
            val user = usersAdapter.getUser(pos) ?: return@itemChecked
            if (checked) {
                text.getSpans(0, text.length, PendingQuerySpan::class.java).forEach { pending ->
                    val start = text.getSpanStart(pending)
                    val end = text.getSpanEnd(pending)
                    text.removeSpan(pending)
                    if (start < 0 || end < 0 || end < start) return@forEach
                    text.delete(start, end)
                }
                val displayName = userColorNameManager.getDisplayName(user, nameFirst)
                val span = ParticipantSpan(user, displayName, roundRadius, spanPadding)
                val start = text.length
                text.append(user.screen_name)
                val end = text.length
                text.setSpan(span, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                text.append(' ')
            } else {
                text.getSpans(0, text.length, ParticipantSpan::class.java).forEach { span ->
                    if (user != span.user) {
                        return@forEach
                    }
                    val start = text.getSpanStart(span)
                    var end = text.getSpanEnd(span)
                    text.removeSpan(span)
                    // Also remove last whitespace
                    if (end <= text.lastIndex && text[end].isWhitespace()) {
                        end += 1
                    }
                    text.delete(start, end)
                }
            }
            editParticipants.clearComposingText()
            updateCheckState()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_messages_conversation_new, container, false)
    }

    override fun onCreateLoader(id: Int, args: Bundle): Loader<List<ParcelableUser>?> {
        val query = args.getString(EXTRA_QUERY)
        val fromCache = args.getBoolean(EXTRA_FROM_CACHE)
        val fromUser = args.getBoolean(EXTRA_FROM_USER)
        return CacheUserSearchLoader(context, accountKey, query, !fromCache, true, fromUser)
    }

    override fun onLoaderReset(loader: Loader<List<ParcelableUser>?>) {
        usersAdapter.data = null
    }

    override fun onLoadFinished(loader: Loader<List<ParcelableUser>?>, data: List<ParcelableUser>?) {
        usersAdapter.data = data
        updateCheckState()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_messages_conversation_new, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.setItemAvailability(R.id.create_conversation, selectedRecipients.isNotEmpty())
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.create_conversation -> {
                createConversation()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun createConversation() {
        val account = this.account ?: return
        val selected = this.selectedRecipients
        if (selected.isEmpty()) return
        val maxParticipants = if (account.isOfficial(context)) {
            defaultFeatures.twitterDirectMessageMaxParticipants
        } else {
            1
        }
        if (selected.size > maxParticipants) {
            editParticipants.error = getString(R.string.error_message_message_too_many_participants)
            return
        }
        val conversation = ParcelableMessageConversation()
        conversation.account_color = account.color
        conversation.account_key = account.key
        conversation.id = "${SendMessageTask.TEMP_CONVERSATION_ID_PREFIX}${System.currentTimeMillis()}"
        conversation.local_timestamp = System.currentTimeMillis()
        conversation.conversation_type = if (selected.size > 1) {
            ConversationType.ONE_TO_ONE
        } else {
            ConversationType.GROUP
        }
        conversation.participants = (selected + account.user).toTypedArray()
        conversation.is_temp = true
        val values = ParcelableMessageConversationValuesCreator.create(conversation)
        context.contentResolver.insert(Conversations.CONTENT_URI, values)
        activity.startActivity(IntentUtils.messageConversation(accountKey, conversation.id))
        activity.finish()
    }

    private fun updateCheckState() {
        val selected = selectedRecipients
        usersAdapter.clearCheckState()
        selected.forEach { user ->
            usersAdapter.setCheckState(user.key, true)
        }
        usersAdapter.notifyDataSetChanged()
        activity?.supportInvalidateOptionsMenu()
    }


    private fun searchUser(query: String, fromType: Boolean) {
        if (TextUtils.isEmpty(query)) {
            return
        }
        val args = Bundle {
            this[EXTRA_ACCOUNT_KEY] = accountKey
            this[EXTRA_QUERY] = query
            this[EXTRA_FROM_CACHE] = fromType
        }
        if (loaderInitialized) {
            loaderManager.initLoader(0, args, this)
            loaderInitialized = true
        } else {
            loaderManager.restartLoader(0, args, this)
        }
        if (performSearchRequestRunnable != null) {
            editParticipants.removeCallbacks(performSearchRequestRunnable)
        }
        if (fromType) {
            performSearchRequestRunnable = PerformSearchRequestRunnable(query, this)
            editParticipants.postDelayed(performSearchRequestRunnable, 1000L)
        }
    }

    internal class PerformSearchRequestRunnable(val query: String, fragment: MessageNewConversationFragment) : Runnable {
        val fragmentRef = WeakReference(fragment)
        override fun run() {
            val fragment = fragmentRef.get() ?: return
            fragment.searchUser(query, false)
        }

    }

    class PendingQuerySpan

    class ParticipantSpan(
            val user: ParcelableUser,
            val displayName: String,
            val roundRadius: Float,
            val padding: Float
    ) : ReplacementSpan() {

        private var backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private var backgroundBounds = RectF()
        private var nameWidth: Float = 0f

        init {
            backgroundPaint.color = 0x20808080
        }

        override fun draw(canvas: Canvas, text: CharSequence, start: Int, end: Int, x: Float, top: Int, y: Int, bottom: Int, paint: Paint) {
            backgroundBounds.set(x, top.toFloat() + padding / 2, x + nameWidth + padding * 2, bottom - padding / 2)
            canvas.drawRoundRect(backgroundBounds, roundRadius, roundRadius, backgroundPaint)
            val textSizeBackup = paint.textSize
            paint.textSize = textSizeBackup - padding
            canvas.drawText(displayName, x + padding, y - padding / 2, paint)
            paint.textSize = textSizeBackup
        }

        override fun getSize(paint: Paint, text: CharSequence, start: Int, end: Int, fm: Paint.FontMetricsInt?): Int {
            val textSizeBackup = paint.textSize
            paint.textSize = textSizeBackup - padding
            nameWidth = paint.measureText(displayName)
            paint.textSize = textSizeBackup
            return Math.round(nameWidth + padding * 2)
        }

    }
}