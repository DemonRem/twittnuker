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

package de.vanita5.twittnuker.fragment.content

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.BaseColumns
import android.support.annotation.CheckResult
import android.support.v4.app.FragmentManager
import android.support.v7.app.AlertDialog
import android.support.v7.widget.PopupMenu
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.Toast
import com.twitter.Validator
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.task
import org.mariotaku.kpreferences.get
import org.mariotaku.ktextension.*
import org.mariotaku.library.objectcursor.ObjectCursor
import de.vanita5.twittnuker.library.MicroBlog
import de.vanita5.twittnuker.R
import de.vanita5.twittnuker.activity.content.RetweetQuoteDialogActivity
import de.vanita5.twittnuker.annotation.AccountType
import de.vanita5.twittnuker.constant.IntentConstants.*
import de.vanita5.twittnuker.constant.quickSendKey
import de.vanita5.twittnuker.extension.applyTheme
import de.vanita5.twittnuker.extension.model.api.toParcelable
import de.vanita5.twittnuker.extension.model.newMicroBlogInstance
import de.vanita5.twittnuker.extension.model.textLimit
import de.vanita5.twittnuker.fragment.BaseDialogFragment
import de.vanita5.twittnuker.model.*
import de.vanita5.twittnuker.model.draft.QuoteStatusActionExtras
import de.vanita5.twittnuker.model.util.ParcelableStatusUtils
import de.vanita5.twittnuker.provider.TwidereDataStore.Drafts
import de.vanita5.twittnuker.service.LengthyOperationsService
import de.vanita5.twittnuker.util.Analyzer
import de.vanita5.twittnuker.util.EditTextEnterHandler
import de.vanita5.twittnuker.util.LinkCreator
import de.vanita5.twittnuker.util.Utils.isMyRetweet
import de.vanita5.twittnuker.util.view.SimpleTextWatcher
import de.vanita5.twittnuker.view.ComposeEditText
import de.vanita5.twittnuker.view.StatusTextCountView
import java.util.*

/**
 * Asks user to retweet/quote a status.
 */
class RetweetQuoteDialogFragment : AbsStatusDialogFragment() {

    override val Dialog.loadProgress: View get() = findViewById(R.id.loadProgress)
    override val Dialog.itemContent: View get() = findViewById(R.id.itemContent)

    private lateinit var popupMenu: PopupMenu

    private val Dialog.textCountView get() = findViewById(R.id.commentTextCount) as StatusTextCountView

    private val Dialog.commentContainer get() = findViewById(R.id.commentContainer) as RelativeLayout
    private val Dialog.editComment get() = findViewById(R.id.editComment) as ComposeEditText
    private val Dialog.commentMenu get() = findViewById(R.id.commentMenu) as ImageButton

    private val PopupMenu.quoteOriginalStatus get() = menu.isItemChecked(R.id.quote_original_status)

    private val text: String?
        get() = arguments.getString(EXTRA_TEXT)

    override fun AlertDialog.Builder.setupAlertDialog() {
        setTitle(R.string.title_retweet_quote_confirm)
        setView(R.layout.dialog_status_quote_retweet)
        setPositiveButton(R.string.action_retweet, null)
        setNegativeButton(android.R.string.cancel, null)
        setNeutralButton(R.string.action_quote, null)
    }

    override fun AlertDialog.onStatusLoaded(details: AccountDetails, status: ParcelableStatus,
            savedInstanceState: Bundle?) {
        textCountView.maxLength = details.textLimit

        val useQuote = useQuote(!status.user_is_protected, details)

        commentContainer.visibility = if (useQuote) View.VISIBLE else View.GONE
        editComment.accountKey = details.key

        val enterHandler = EditTextEnterHandler.attach(editComment, object : EditTextEnterHandler.EnterListener {
            override fun shouldCallListener(): Boolean {
                return true
            }

            override fun onHitEnter(): Boolean {
                if (retweetOrQuote(details, status, SHOW_PROTECTED_CONFIRM)) {
                    dismiss()
                    return true
                }
                return false
            }
        }, preferences[quickSendKey])
        enterHandler.addTextChangedListener(object : SimpleTextWatcher {

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                updateTextCount(getDialog(), s, status, details)
            }
        })

        popupMenu = PopupMenu(context, commentMenu, Gravity.NO_GRAVITY,
                R.attr.actionOverflowMenuStyle, 0).apply {
            inflate(R.menu.menu_dialog_comment)
            menu.setItemAvailability(R.id.quote_original_status, status.retweet_id != null || status.quoted_id != null)
            setOnMenuItemClickListener(PopupMenu.OnMenuItemClickListener { item ->
                if (item.isCheckable) {
                    item.isChecked = !item.isChecked
                    return@OnMenuItemClickListener true
                }
                false
            })
        }
        commentMenu.setOnClickListener { popupMenu.show() }
        commentMenu.setOnTouchListener(popupMenu.dragToOpenListener)
        commentMenu.visibility = if (popupMenu.menu.hasVisibleItems()) View.VISIBLE else View.GONE

        getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            var dismissDialog = false
            if (editComment.length() > 0) {
                dismissDialog = retweetOrQuote(details, status, SHOW_PROTECTED_CONFIRM)
            } else if (isMyRetweet(status)) {
                twitterWrapper.cancelRetweetAsync(details.key, status.id, status.my_retweet_id)
                dismissDialog = true
            } else if (useQuote(!status.user_is_protected, details)) {
                dismissDialog = retweetOrQuote(details, status, SHOW_PROTECTED_CONFIRM)
            } else {
                Analyzer.logException(IllegalStateException(status.toString()))
            }
            if (dismissDialog) {
                dismiss()
            }
        }
        getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener {
            val intent = Intent(INTENT_ACTION_QUOTE)
            val menu = popupMenu.menu
            val quoteOriginalStatus = menu.findItem(R.id.quote_original_status)
            intent.putExtra(EXTRA_STATUS, status)
            intent.putExtra(EXTRA_QUOTE_ORIGINAL_STATUS, quoteOriginalStatus.isChecked)
            startActivity(intent)
            dismiss()
        }

            if (savedInstanceState == null) {
            editComment.setText(text)
            }
        editComment.setSelection(editComment.length())

        updateTextCount(dialog, editComment.text, status, details)
    }

    override fun onCancel(dialog: DialogInterface) {
        if (dialog !is Dialog) return
        if (dialog.editComment.empty) return
        dialog.saveToDrafts()
        Toast.makeText(context, R.string.message_toast_status_saved_to_draft, Toast.LENGTH_SHORT).show()
        finishRetweetQuoteActivity()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        finishRetweetQuoteActivity()
    }

    private fun finishRetweetQuoteActivity() {
        val activity = this.activity
        if (activity is RetweetQuoteDialogActivity && !activity.isFinishing) {
            activity.finish()
        }
    }

    private fun updateTextCount(dialog: DialogInterface, s: CharSequence, status: ParcelableStatus,
                                credentials: AccountDetails) {
        if (dialog !is AlertDialog) return
        val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE) ?: return
        if (s.isNotEmpty()) {
            positiveButton.setText(R.string.comment)
            positiveButton.isEnabled = true
        } else if (isMyRetweet(status)) {
            positiveButton.setText(R.string.action_cancel_retweet)
            positiveButton.isEnabled = true
        } else if (useQuote(false, credentials)) {
            positiveButton.setText(R.string.action_retweet)
            positiveButton.isEnabled = true
        } else {
            positiveButton.setText(R.string.action_retweet)
            positiveButton.isEnabled = !status.user_is_protected
        }
        val textCountView = dialog.findViewById(R.id.commentTextCount) as StatusTextCountView
        textCountView.textCount = validator.getTweetLength(s.toString())
    }

    @CheckResult
    private fun retweetOrQuote(account: AccountDetails, status: ParcelableStatus,
                               showProtectedConfirmation: Boolean): Boolean {
        val twitter = twitterWrapper
        val dialog = dialog ?: return false
        val editComment = dialog.findViewById(R.id.editComment) as EditText
        if (useQuote(editComment.length() > 0, account)) {
            val quoteOriginalStatus = popupMenu.quoteOriginalStatus

            var commentText: String
            val update = ParcelableStatusUpdate()
            update.accounts = arrayOf(account)
            val editingComment = editComment.text.toString()
            when (account.type) {
                AccountType.FANFOU -> {
                    if (!status.is_quote || !quoteOriginalStatus) {
                        if (status.user_is_protected && showProtectedConfirmation) {
                            QuoteProtectedStatusWarnFragment.show(this, account, status)
                            return false
                        }
                        update.repost_status_id = status.id
                        commentText = getString(R.string.fanfou_repost_format, editingComment,
                                status.user_screen_name, status.text_plain)
                    } else {
                        if (status.quoted_user_is_protected && showProtectedConfirmation) {
                            return false
                        }
                        commentText = getString(R.string.fanfou_repost_format, editingComment,
                                status.quoted_user_screen_name, status.quoted_text_plain)
                        update.repost_status_id = status.quoted_id
                    }
                    if (commentText.length > Validator.MAX_TWEET_LENGTH) {
                        commentText = commentText.substring(0, Math.max(Validator.MAX_TWEET_LENGTH,
                                editingComment.length))
                    }
                }
                else -> {
                    val statusLink = if (!status.is_quote || !quoteOriginalStatus) {
                        LinkCreator.getStatusWebLink(status)
                    } else {
                        LinkCreator.getQuotedStatusWebLink(status)
                    }
                    update.attachment_url = statusLink.toString()
                    commentText = editingComment
                }
            }
            update.text = commentText
            update.is_possibly_sensitive = status.is_possibly_sensitive
            update.draft_action = Draft.Action.QUOTE
            update.draft_extras = QuoteStatusActionExtras().apply {
                this.status = status
                this.isQuoteOriginalStatus = quoteOriginalStatus
            }
            LengthyOperationsService.updateStatusesAsync(context, Draft.Action.QUOTE, update)
        } else {
            twitter.retweetStatusAsync(account.key, status)
        }
        return true
    }

    private fun useQuote(preCondition: Boolean, account: AccountDetails): Boolean {
        return preCondition || AccountType.FANFOU == account.type
    }

    private fun Dialog.saveToDrafts() {
        val text = dialog.editComment.text.toString()
        val draft = Draft()
        draft.unique_id = UUID.randomUUID().toString()
        draft.action_type = Draft.Action.QUOTE
        draft.account_keys = arrayOf(accountKey)
        draft.text = text
        draft.timestamp = System.currentTimeMillis()
        draft.action_extras = QuoteStatusActionExtras().apply {
            this.status = this@RetweetQuoteDialogFragment.status
            this.isQuoteOriginalStatus = popupMenu.quoteOriginalStatus
        }
        val values = ObjectCursor.valuesCreatorFrom(Draft::class.java).create(draft)
        val contentResolver = context.contentResolver
        val draftUri = contentResolver.insert(Drafts.CONTENT_URI, values)
        displayNewDraftNotification(draftUri)
    }


    private fun displayNewDraftNotification(draftUri: Uri) {
        val contentResolver = context.contentResolver
        val values = ContentValues {
            this[BaseColumns._ID] = draftUri.lastPathSegment
        }
        contentResolver.insert(Drafts.CONTENT_URI_NOTIFICATIONS, values)
    }

    class QuoteProtectedStatusWarnFragment : BaseDialogFragment(), DialogInterface.OnClickListener {

        override fun onClick(dialog: DialogInterface, which: Int) {
            val fragment = parentFragment as RetweetQuoteDialogFragment
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> {
                    val args = arguments
                    val account: AccountDetails = args.getParcelable(EXTRA_ACCOUNT)
                    val status: ParcelableStatus = args.getParcelable(EXTRA_STATUS)
                    if (fragment.retweetOrQuote(account, status, false)) {
                        fragment.dismiss()
                    }
                }
            }

        }

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val context = activity
            val builder = AlertDialog.Builder(context)
            builder.setMessage(R.string.quote_protected_status_warning_message)
            builder.setPositiveButton(R.string.send_anyway, this)
            builder.setNegativeButton(android.R.string.cancel, null)
            val dialog = builder.create()
            dialog.setOnShowListener {
                it as AlertDialog
                it.applyTheme()
            }
            return dialog
        }

        companion object {

            fun show(pf: RetweetQuoteDialogFragment,
                     account: AccountDetails,
                     status: ParcelableStatus): QuoteProtectedStatusWarnFragment {
                val f = QuoteProtectedStatusWarnFragment()
                val args = Bundle()
                args.putParcelable(EXTRA_ACCOUNT, account)
                args.putParcelable(EXTRA_STATUS, status)
                f.arguments = args
                f.show(pf.childFragmentManager, "quote_protected_status_warning")
                return f
            }
        }
    }

    companion object {

        val FRAGMENT_TAG = "retweet_quote"
        private val SHOW_PROTECTED_CONFIRM = java.lang.Boolean.parseBoolean("false")

        fun show(fm: FragmentManager, accountKey: UserKey, statusId: String,
                status: ParcelableStatus? = null, text: String? = null):
                RetweetQuoteDialogFragment {
            val f = RetweetQuoteDialogFragment()
            f.arguments = Bundle {
                this[EXTRA_ACCOUNT_KEY] = accountKey
                this[EXTRA_STATUS_ID] = statusId
                this[EXTRA_STATUS] = status
                this[EXTRA_TEXT] = text
            }
            f.show(fm, FRAGMENT_TAG)
            return f
        }

        fun showStatus(context: Context, details: AccountDetails, statusId: String,
                status: ParcelableStatus?): Promise<ParcelableStatus, Exception> {
            if (status != null) {
                status.apply {
                    if (account_key != details.key) {
                        my_retweet_id = null
                    }
                    account_key = details.key
                    account_color = details.color
                }
                return Promise.ofSuccess(status)
            }
            val microBlog = details.newMicroBlogInstance(context, MicroBlog::class.java)
            val profileImageSize = context.getString(R.string.profile_image_size)
            return task {
                microBlog.showStatus(statusId).toParcelable(details.key, details.type, profileImageSize)
            }
        }

    }
}