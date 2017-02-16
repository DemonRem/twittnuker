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

package de.vanita5.twittnuker.task.message

import android.content.Context
import org.mariotaku.ktextension.isNotNullOrEmpty
import de.vanita5.twittnuker.library.MicroBlog
import de.vanita5.twittnuker.library.MicroBlogException
import de.vanita5.twittnuker.library.twitter.TwitterUpload
import de.vanita5.twittnuker.library.twitter.model.DirectMessage
import de.vanita5.twittnuker.library.twitter.model.NewDm
import de.vanita5.twittnuker.annotation.AccountType
import de.vanita5.twittnuker.extension.model.isOfficial
import de.vanita5.twittnuker.extension.model.newMicroBlogInstance
import de.vanita5.twittnuker.model.AccountDetails
import de.vanita5.twittnuker.model.ParcelableMessageConversation
import de.vanita5.twittnuker.model.ParcelableNewMessage
import de.vanita5.twittnuker.model.util.ParcelableMessageUtils
import de.vanita5.twittnuker.task.ExceptionHandlingAbstractTask
import de.vanita5.twittnuker.task.GetMessagesTask
import de.vanita5.twittnuker.task.GetMessagesTask.Companion.addConversation
import de.vanita5.twittnuker.task.GetMessagesTask.Companion.addLocalConversations
import de.vanita5.twittnuker.task.twitter.UpdateStatusTask

class SendMessageTask(
        context: Context
) : ExceptionHandlingAbstractTask<ParcelableNewMessage, Unit, MicroBlogException, Unit>(context) {
    override fun onExecute(params: ParcelableNewMessage) {
        val account = params.account
        val microBlog = account.newMicroBlogInstance(context, cls = MicroBlog::class.java)
        val updateData = requestSendMessage(microBlog, account, params)
        GetMessagesTask.storeMessages(context, updateData, account)
    }

    fun requestSendMessage(microBlog: MicroBlog, account: AccountDetails, message: ParcelableNewMessage): GetMessagesTask.DatabaseUpdateData {
        when (account.type) {
            AccountType.TWITTER -> {
                if (account.isOfficial(context)) {
                    return sendTwitterOfficialDM(microBlog, account, message)
                }
            }
            AccountType.FANFOU -> {
                return sendFanfouDM(microBlog, account, message)
            }
        }
        return sendDefaultDM(microBlog, account, message)
    }

    private fun sendTwitterOfficialDM(microBlog: MicroBlog, account: AccountDetails, message: ParcelableNewMessage): GetMessagesTask.DatabaseUpdateData {
        var deleteOnSuccess: List<UpdateStatusTask.MediaDeletionItem>? = null
        var deleteAlways: List<UpdateStatusTask.MediaDeletionItem>? = null
        val response = try {
            val newDm = NewDm()
            val conversationId = message.conversation_id
            if (conversationId != null) {
                newDm.setConversationId(conversationId)
            } else {
                newDm.setRecipientIds(message.recipient_ids)
            }
            newDm.setText(message.text)

            if (message.media.isNotNullOrEmpty()) {
                val upload = account.newMicroBlogInstance(context, cls = TwitterUpload::class.java)
                val uploadResult = UpdateStatusTask.uploadAllMediaShared(context,
                        mediaLoader, upload, account, message.media, null, true, null)
                newDm.setMediaId(uploadResult.ids[0])
                deleteAlways = uploadResult.deleteAlways
                deleteOnSuccess = uploadResult.deleteOnSuccess
            }
            microBlog.sendDm(newDm)
        } finally {
            deleteOnSuccess?.forEach { it.delete(context) }
        }
        deleteAlways?.forEach { it.delete(context) }
        return GetMessagesTask.createDatabaseUpdateData(context, account, response)
    }

    private fun sendFanfouDM(microBlog: MicroBlog, account: AccountDetails, message: ParcelableNewMessage): GetMessagesTask.DatabaseUpdateData {
        val recipientId = message.recipient_ids.singleOrNull() ?: throw MicroBlogException("No recipient")
        val response = microBlog.sendFanfouDirectMessage(recipientId, message.text)
        return createDatabaseUpdateData(account, response)
    }

    private fun sendDefaultDM(microBlog: MicroBlog, account: AccountDetails, message: ParcelableNewMessage): GetMessagesTask.DatabaseUpdateData {
        val recipientId = message.recipient_ids.singleOrNull() ?: throw MicroBlogException("No recipient")
        val response = microBlog.sendDirectMessage(recipientId, message.text)
        return createDatabaseUpdateData(account, response)
    }

    private fun createDatabaseUpdateData(details: AccountDetails, dm: DirectMessage): GetMessagesTask.DatabaseUpdateData {
        val accountKey = details.key
        val conversationIds = setOf(ParcelableMessageUtils.outgoingConversationId(dm.senderId, dm.recipientId))
        val conversations = hashMapOf<String, ParcelableMessageConversation>()
        conversations.addLocalConversations(context, accountKey, conversationIds)
        val message = ParcelableMessageUtils.fromMessage(accountKey, dm, true)
        conversations.addConversation(message.conversation_id, details, message, setOf(dm.sender, dm.recipient))
        return GetMessagesTask.DatabaseUpdateData(conversations.values, listOf(message))
    }
}