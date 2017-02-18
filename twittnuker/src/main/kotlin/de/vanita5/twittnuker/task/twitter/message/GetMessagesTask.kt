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

package de.vanita5.twittnuker.task.twitter.message

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import org.mariotaku.ktextension.toInt
import org.mariotaku.ktextension.useCursor
import de.vanita5.twittnuker.library.twitter.model.DMResponse
import de.vanita5.twittnuker.library.twitter.model.User
import de.vanita5.twittnuker.library.twitter.model.fixMedia
import org.mariotaku.sqliteqb.library.Expression
import de.vanita5.twittnuker.extension.model.applyFrom
import de.vanita5.twittnuker.extension.model.isOfficial
import de.vanita5.twittnuker.extension.model.newMicroBlogInstance
import de.vanita5.twittnuker.extension.model.timestamp
import de.vanita5.twittnuker.model.*
import de.vanita5.twittnuker.model.ParcelableMessageConversation.ConversationType
import de.vanita5.twittnuker.model.message.conversation.TwitterOfficialConversationExtras
import de.vanita5.twittnuker.model.util.AccountUtils.getAccountDetails
import de.vanita5.twittnuker.model.util.ParcelableUserUtils
import de.vanita5.twittnuker.model.util.UserKeyUtils
import de.vanita5.twittnuker.provider.TwidereDataStore.Messages
import de.vanita5.twittnuker.provider.TwidereDataStore.Messages.Conversations
import de.vanita5.twittnuker.util.content.ContentResolverUtils
import java.util.*


class GetMessagesTask(
        context: android.content.Context
) : de.vanita5.twittnuker.task.BaseAbstractTask<GetMessagesTask.RefreshMessagesTaskParam, Unit, (Boolean) -> Unit>(context) {
    override fun doLongOperation(param: de.vanita5.twittnuker.task.twitter.message.GetMessagesTask.RefreshMessagesTaskParam) {
        val accountKeys = param.accountKeys
        val am = android.accounts.AccountManager.get(context)
        accountKeys.forEachIndexed { i, accountKey ->
            val details = getAccountDetails(am, accountKey, true) ?: return@forEachIndexed
            val microBlog = details.newMicroBlogInstance(context, true, cls = de.vanita5.twittnuker.library.MicroBlog::class.java)
            val messages = try {
                getMessages(microBlog, details, param, i)
            } catch (e: de.vanita5.twittnuker.library.MicroBlogException) {
                return@forEachIndexed
            }
            de.vanita5.twittnuker.task.twitter.message.GetMessagesTask.Companion.storeMessages(context, messages, details)
        }
    }

    override fun afterExecute(callback: ((Boolean) -> Unit)?, result: Unit) {
        callback?.invoke(true)
        bus.post(de.vanita5.twittnuker.model.event.GetMessagesTaskEvent(de.vanita5.twittnuker.provider.TwidereDataStore.Messages.CONTENT_URI, params?.taskTag, false, null))
    }

    private fun getMessages(microBlog: de.vanita5.twittnuker.library.MicroBlog, details: de.vanita5.twittnuker.model.AccountDetails, param: de.vanita5.twittnuker.task.twitter.message.GetMessagesTask.RefreshMessagesTaskParam, index: Int): de.vanita5.twittnuker.task.twitter.message.GetMessagesTask.DatabaseUpdateData {
        when (details.type) {
            de.vanita5.twittnuker.annotation.AccountType.FANFOU -> {
                // Use fanfou DM api, disabled since it's conversation api is not suitable for paging
                // return getFanfouMessages(microBlog, details, param, index)
            }
            de.vanita5.twittnuker.annotation.AccountType.TWITTER -> {
                // Use official DM api
                if (details.isOfficial(context)) {
                    return getTwitterOfficialMessages(microBlog, details, param, index)
                }
            }
        }
        // Use default method
        return getDefaultMessages(microBlog, details, param, index)
    }

    private fun getTwitterOfficialMessages(microBlog: de.vanita5.twittnuker.library.MicroBlog, details: de.vanita5.twittnuker.model.AccountDetails,
            param: de.vanita5.twittnuker.task.twitter.message.GetMessagesTask.RefreshMessagesTaskParam, index: Int): de.vanita5.twittnuker.task.twitter.message.GetMessagesTask.DatabaseUpdateData {
        val conversationId = param.conversationId
        if (conversationId == null) {
            return getTwitterOfficialUserInbox(microBlog, details, param, index)
        } else {
            return getTwitterOfficialConversation(microBlog, details, conversationId, param, index)
        }
    }

    private fun getFanfouMessages(microBlog: de.vanita5.twittnuker.library.MicroBlog, details: de.vanita5.twittnuker.model.AccountDetails, param: de.vanita5.twittnuker.task.twitter.message.GetMessagesTask.RefreshMessagesTaskParam, index: Int): de.vanita5.twittnuker.task.twitter.message.GetMessagesTask.DatabaseUpdateData {
        val conversationId = param.conversationId
        if (conversationId == null) {
            return getFanfouConversations(microBlog, details, param, index)
        } else {
            return de.vanita5.twittnuker.task.twitter.message.GetMessagesTask.DatabaseUpdateData(emptyList(), emptyList())
        }
    }

    private fun getDefaultMessages(microBlog: de.vanita5.twittnuker.library.MicroBlog, details: de.vanita5.twittnuker.model.AccountDetails, param: de.vanita5.twittnuker.task.twitter.message.GetMessagesTask.RefreshMessagesTaskParam, index: Int): de.vanita5.twittnuker.task.twitter.message.GetMessagesTask.DatabaseUpdateData {
        val accountKey = details.key

        val sinceIds = if (param.hasSinceIds) param.sinceIds else null
        val maxIds = if (param.hasMaxIds) param.maxIds else null

        val received = microBlog.getDirectMessages(de.vanita5.twittnuker.library.twitter.model.Paging().apply {
            count(100)
            val maxId = maxIds?.get(index)
            val sinceId = sinceIds?.get(index)
            if (maxId != null) {
                maxId(maxId)
            }
            if (sinceId != null) {
                sinceId(sinceId)
            }
        })
        val sent = microBlog.getSentDirectMessages(de.vanita5.twittnuker.library.twitter.model.Paging().apply {
            count(100)
            val accountsCount = param.accountKeys.size
            val maxId = maxIds?.get(accountsCount + index)
            val sinceId = sinceIds?.get(accountsCount + index)
            if (maxId != null) {
                maxId(maxId)
            }
            if (sinceId != null) {
                sinceId(sinceId)
            }
        })


        val insertMessages = arrayListOf<de.vanita5.twittnuker.model.ParcelableMessage>()
        val conversations = hashMapOf<String, de.vanita5.twittnuker.model.ParcelableMessageConversation>()

        val conversationIds = hashSetOf<String>()
        received.forEach {
            conversationIds.add(de.vanita5.twittnuker.model.util.ParcelableMessageUtils.incomingConversationId(it.senderId, it.recipientId))
        }
        sent.forEach {
            conversationIds.add(de.vanita5.twittnuker.model.util.ParcelableMessageUtils.outgoingConversationId(it.senderId, it.recipientId))
        }

        conversations.addLocalConversations(context, accountKey, conversationIds)

        received.forEachIndexed { i, dm ->
            val message = de.vanita5.twittnuker.model.util.ParcelableMessageUtils.fromMessage(accountKey, dm, false,
                    1.0 - (i.toDouble() / received.size))
            insertMessages.add(message)
            conversations.addConversation(message.conversation_id, details, message, setOf(dm.sender, dm.recipient))
        }
        sent.forEachIndexed { i, dm ->
            val message = de.vanita5.twittnuker.model.util.ParcelableMessageUtils.fromMessage(accountKey, dm, true,
                    1.0 - (i.toDouble() / sent.size))
            insertMessages.add(message)
            conversations.addConversation(message.conversation_id, details, message, setOf(dm.sender, dm.recipient))
        }
        return de.vanita5.twittnuker.task.twitter.message.GetMessagesTask.DatabaseUpdateData(conversations.values, insertMessages)
    }

    private fun getTwitterOfficialConversation(microBlog: de.vanita5.twittnuker.library.MicroBlog, details: de.vanita5.twittnuker.model.AccountDetails,
            conversationId: String, param: de.vanita5.twittnuker.task.twitter.message.GetMessagesTask.RefreshMessagesTaskParam, index: Int): de.vanita5.twittnuker.task.twitter.message.GetMessagesTask.DatabaseUpdateData {
        val maxId = param.maxIds?.get(index) ?: return de.vanita5.twittnuker.task.twitter.message.GetMessagesTask.DatabaseUpdateData(emptyList(), emptyList())
        val paging = de.vanita5.twittnuker.library.twitter.model.Paging().apply {
            maxId(maxId)
        }

        val response = microBlog.getDmConversation(conversationId, paging).conversationTimeline
        response.fixMedia(microBlog)
        return de.vanita5.twittnuker.task.twitter.message.GetMessagesTask.Companion.createDatabaseUpdateData(context, details, response)
    }

    private fun getTwitterOfficialUserInbox(microBlog: de.vanita5.twittnuker.library.MicroBlog, details: de.vanita5.twittnuker.model.AccountDetails,
            param: de.vanita5.twittnuker.task.twitter.message.GetMessagesTask.RefreshMessagesTaskParam, index: Int): de.vanita5.twittnuker.task.twitter.message.GetMessagesTask.DatabaseUpdateData {
        val maxId = if (param.hasMaxIds) param.maxIds?.get(index) else null
        val cursor = if (param.hasCursors) param.cursors?.get(index) else null
        val response = if (cursor != null) {
            microBlog.getUserUpdates(cursor).userEvents
        } else {
            microBlog.getUserInbox(de.vanita5.twittnuker.library.twitter.model.Paging().apply {
                if (maxId != null) {
                    maxId(maxId)
                }
            }).userInbox
        }
        response.fixMedia(microBlog)
        return de.vanita5.twittnuker.task.twitter.message.GetMessagesTask.Companion.createDatabaseUpdateData(context, details, response)
    }


    private fun getFanfouConversations(microBlog: de.vanita5.twittnuker.library.MicroBlog, details: de.vanita5.twittnuker.model.AccountDetails, param: de.vanita5.twittnuker.task.twitter.message.GetMessagesTask.RefreshMessagesTaskParam, index: Int): de.vanita5.twittnuker.task.twitter.message.GetMessagesTask.DatabaseUpdateData {
        val accountKey = details.key
        val cursor = param.cursors?.get(index)
        val page = cursor?.substringAfter("page:").toInt(-1)
        val result = microBlog.getConversationList(de.vanita5.twittnuker.library.twitter.model.Paging().apply {
            count(60)
            if (page >= 0) {
                page(page)
            }
        })
        val conversations = hashMapOf<String, de.vanita5.twittnuker.model.ParcelableMessageConversation>()

        val conversationIds = hashSetOf<String>()
        result.mapTo(conversationIds) { "${accountKey.id}-${it.otherId}" }
        conversations.addLocalConversations(context, accountKey, conversationIds)
        result.forEachIndexed { i, item ->
            val dm = item.dm
            // Sender is our self, treat as outgoing message
            val message = de.vanita5.twittnuker.model.util.ParcelableMessageUtils.fromMessage(accountKey, dm, dm.senderId == accountKey.id,
                    1.0 - (i.toDouble() / result.size))
            val mc = conversations.addConversation(message.conversation_id, details, message,
                    setOf(dm.sender, dm.recipient))
            mc.request_cursor = "page:$page"
        }
        return de.vanita5.twittnuker.task.twitter.message.GetMessagesTask.DatabaseUpdateData(conversations.values, emptyList())
    }

    data class DatabaseUpdateData(
            val conversations: Collection<de.vanita5.twittnuker.model.ParcelableMessageConversation>,
            val messages: Collection<de.vanita5.twittnuker.model.ParcelableMessage>,
            val deleteConversations: List<String> = emptyList(),
            val deleteMessages: Map<String, List<String>> = emptyMap(),
            val conversationRequestCursor: String? = null
    )

    abstract class RefreshNewTaskParam(
            context: android.content.Context
    ) : de.vanita5.twittnuker.task.twitter.message.GetMessagesTask.RefreshMessagesTaskParam(context) {

        override val sinceIds: Array<String?>?
            get() {
                val incomingIds = de.vanita5.twittnuker.util.DataStoreUtils.getNewestMessageIds(context, de.vanita5.twittnuker.provider.TwidereDataStore.Messages.CONTENT_URI,
                        defaultKeys, false)
                val outgoingIds = de.vanita5.twittnuker.util.DataStoreUtils.getNewestMessageIds(context, de.vanita5.twittnuker.provider.TwidereDataStore.Messages.CONTENT_URI,
                        defaultKeys, true)
                return incomingIds + outgoingIds
            }

        override val cursors: Array<String?>?
            get() {
                val cursors = arrayOfNulls<String>(defaultKeys.size)
                val newestConversations = de.vanita5.twittnuker.util.DataStoreUtils.getNewestConversations(context,
                        de.vanita5.twittnuker.provider.TwidereDataStore.Messages.Conversations.CONTENT_URI, twitterOfficialKeys)
                newestConversations.forEachIndexed { i, conversation ->
                    cursors[i] = conversation?.request_cursor
                }
                return cursors
            }

        override val hasSinceIds: Boolean = true
        override val hasMaxIds: Boolean = false
        override val hasCursors: Boolean = true
    }

    abstract class LoadMoreEntriesTaskParam(
            context: android.content.Context
    ) : de.vanita5.twittnuker.task.twitter.message.GetMessagesTask.RefreshMessagesTaskParam(context) {

        override val maxIds: Array<String?>? by lazy {
            val incomingIds = de.vanita5.twittnuker.util.DataStoreUtils.getOldestMessageIds(context, de.vanita5.twittnuker.provider.TwidereDataStore.Messages.CONTENT_URI,
                    defaultKeys, false)
            val outgoingIds = de.vanita5.twittnuker.util.DataStoreUtils.getOldestMessageIds(context, de.vanita5.twittnuker.provider.TwidereDataStore.Messages.CONTENT_URI,
                    defaultKeys, true)
            val oldestConversations = de.vanita5.twittnuker.util.DataStoreUtils.getOldestConversations(context,
                    de.vanita5.twittnuker.provider.TwidereDataStore.Messages.Conversations.CONTENT_URI, twitterOfficialKeys)
            oldestConversations.forEachIndexed { i, conversation ->
                val extras = conversation?.conversation_extras as? de.vanita5.twittnuker.model.message.conversation.TwitterOfficialConversationExtras ?: return@forEachIndexed
                incomingIds[i] = extras.maxEntryId
            }
            return@lazy incomingIds + outgoingIds
        }

        override val hasSinceIds: Boolean = false
        override val hasMaxIds: Boolean = true
    }

    class LoadMoreMessageTaskParam(
            context: android.content.Context,
            accountKey: de.vanita5.twittnuker.model.UserKey,
            override val conversationId: String,
            maxId: String
    ) : de.vanita5.twittnuker.task.twitter.message.GetMessagesTask.RefreshMessagesTaskParam(context) {
        override val accountKeys: Array<de.vanita5.twittnuker.model.UserKey> = arrayOf(accountKey)
        override val maxIds: Array<String?>? = arrayOf(maxId)
        override val hasMaxIds: Boolean = true
    }

    abstract class RefreshMessagesTaskParam(
            val context: android.content.Context
    ) : de.vanita5.twittnuker.model.SimpleRefreshTaskParam() {

        /**
         * If `conversationId` has value, load messages in conversationId
         */
        open val conversationId: String? = null

        var taskTag: String? = null

        protected val accounts: Array<de.vanita5.twittnuker.model.AccountDetails?> by lazy {
            de.vanita5.twittnuker.model.util.AccountUtils.getAllAccountDetails(android.accounts.AccountManager.get(context), accountKeys, false)
        }

        protected val defaultKeys: Array<de.vanita5.twittnuker.model.UserKey?>by lazy {
            return@lazy accounts.map { account ->
                account ?: return@map null
                if (account.isOfficial(context) || account.type == de.vanita5.twittnuker.annotation.AccountType.FANFOU) {
                    return@map null
                }
                return@map account.key
            }.toTypedArray()
        }

        protected val twitterOfficialKeys: Array<de.vanita5.twittnuker.model.UserKey?> by lazy {
            return@lazy accounts.map { account ->
                account ?: return@map null
                if (!account.isOfficial(context)) {
                    return@map null
                }
                return@map account.key
            }.toTypedArray()
        }

    }

    companion object {

        fun createDatabaseUpdateData(context: android.content.Context, account: de.vanita5.twittnuker.model.AccountDetails, response: de.vanita5.twittnuker.library.twitter.model.DMResponse):
                de.vanita5.twittnuker.task.twitter.message.GetMessagesTask.DatabaseUpdateData {
            val respConversations = response.conversations.orEmpty()
            val respEntries = response.entries.orEmpty()
            val respUsers = response.users.orEmpty()

            val conversations = hashMapOf<String, de.vanita5.twittnuker.model.ParcelableMessageConversation>()

            conversations.addLocalConversations(context, account.key, respConversations.keys)
            val messages = java.util.ArrayList<de.vanita5.twittnuker.model.ParcelableMessage>()
            val messageDeletionsMap = java.util.HashMap<String, java.util.ArrayList<String>>()
            val conversationDeletions = java.util.ArrayList<String>()
            respEntries.mapNotNullTo(messages) { entry ->
                when {
                    entry.messageDelete != null -> {
                        val list = messageDeletionsMap.getOrPut(entry.messageDelete.conversationId) { java.util.ArrayList<String>() }
                        entry.messageDelete.messages?.forEach {
                            list.add(it.messageId)
                        }
                        return@mapNotNullTo null
                    }
                    entry.removeConversation != null -> {
                        conversationDeletions.add(entry.removeConversation.conversationId)
                        return@mapNotNullTo null
                    }
                    else -> {
                        return@mapNotNullTo de.vanita5.twittnuker.model.util.ParcelableMessageUtils.fromEntry(account.key, entry, respUsers)
                    }
                }
            }
            val messagesMap = messages.groupBy(de.vanita5.twittnuker.model.ParcelableMessage::conversation_id)
            for ((k, v) in respConversations) {
                val message = messagesMap[k]?.maxBy(ParcelableMessage::message_timestamp) ?: continue
                val participants = respUsers.filterKeys { userId ->
                    v.participants.any { it.userId == userId }
                }.values
                val conversationType = when (v.type?.toUpperCase(Locale.US)) {
                    DMResponse.Conversation.Type.ONE_TO_ONE -> ConversationType.ONE_TO_ONE
                    DMResponse.Conversation.Type.GROUP_DM -> ConversationType.GROUP
                    else -> ConversationType.ONE_TO_ONE
                }
                val conversation = conversations.addConversation(k, account, message, participants,
                        conversationType)
                conversation.conversation_name = v.name
                conversation.conversation_avatar = v.avatarImageHttps
                conversation.request_cursor = response.cursor
                conversation.conversation_extras_type = ParcelableMessageConversation.ExtrasType.TWITTER_OFFICIAL
                conversation.conversation_extras = TwitterOfficialConversationExtras().apply {
                    this.minEntryId = v.minEntryId
                    this.maxEntryId = v.maxEntryId
                    this.status = v.status
                }
            }
            return DatabaseUpdateData(conversations.values, messages, conversationDeletions,
                    messageDeletionsMap, response.cursor)
        }

        fun storeMessages(context: Context, data: DatabaseUpdateData, details: AccountDetails) {
            val resolver = context.contentResolver
            val conversationsValues = data.conversations.map {
                val values = ParcelableMessageConversationValuesCreator.create(it)
                if (it._id > 0) {
                    values.put(Conversations._ID, it._id)
                }
                return@map values
            }
            val messagesValues = data.messages.map(ParcelableMessageValuesCreator::create)

            for ((conversationId, messageIds) in data.deleteMessages) {
                val where = Expression.and(Expression.equalsArgs(Messages.ACCOUNT_KEY),
                        Expression.equalsArgs(Messages.CONVERSATION_ID)).sql
                val whereArgs = arrayOf(details.key.toString(), conversationId)
                ContentResolverUtils.bulkDelete(resolver, Messages.CONTENT_URI, Messages.MESSAGE_ID,
                        false, messageIds, where, whereArgs)
            }

            val accountWhere = Expression.equalsArgs(Messages.ACCOUNT_KEY).sql
            val accountWhereArgs = arrayOf(details.key.toString())

            ContentResolverUtils.bulkDelete(resolver, Conversations.CONTENT_URI, Conversations.CONVERSATION_ID,
                    false, data.deleteConversations, accountWhere, accountWhereArgs)
            ContentResolverUtils.bulkDelete(resolver, Messages.CONTENT_URI, Messages.CONVERSATION_ID,
                    false, data.deleteConversations, accountWhere, accountWhereArgs)

            ContentResolverUtils.bulkInsert(resolver, Conversations.CONTENT_URI, conversationsValues)
            ContentResolverUtils.bulkInsert(resolver, Messages.CONTENT_URI, messagesValues)

            if (data.conversationRequestCursor != null) {
                resolver.update(Conversations.CONTENT_URI, ContentValues().apply {
                    put(Conversations.REQUEST_CURSOR, data.conversationRequestCursor)
                }, accountWhere, accountWhereArgs)
            }
        }


    @SuppressLint("Recycle")
        fun MutableMap<String, ParcelableMessageConversation>.addLocalConversations(context: Context,
                                                                                    accountKey: UserKey, conversationIds: Set<String>) {
        val where = Expression.and(Expression.inArgs(Conversations.CONVERSATION_ID, conversationIds.size),
                Expression.equalsArgs(Conversations.ACCOUNT_KEY)).sql
        val whereArgs = conversationIds.toTypedArray() + accountKey.toString()
        return context.contentResolver.query(Conversations.CONTENT_URI, Conversations.COLUMNS,
                where, whereArgs, null).useCursor { cur ->
            val indices = ParcelableMessageConversationCursorIndices(cur)
            cur.moveToFirst()
            while (!cur.isAfterLast) {
                val conversationId = cur.getString(indices.id)
                val timestamp = cur.getLong(indices.local_timestamp)
                val conversation = this[conversationId] ?: run {
                    val obj = indices.newObject(cur)
                    this[conversationId] = obj
                    return@run obj
                }
                if (timestamp > conversation.local_timestamp) {
                    this[conversationId] = indices.newObject(cur)
                }
                indices.newObject(cur)
                cur.moveToNext()
            }
        }
    }

    private fun ParcelableMessageConversation.addParticipant(
            accountKey: UserKey,
            user: User
    ) {
            val userKey = UserKeyUtils.fromUser(user)
            val participants = this.participants
            if (participants == null) {
                this.participants = arrayOf(ParcelableUserUtils.fromUser(user, accountKey))
            } else {
                val index = participants.indexOfFirst { it.key == userKey }
                if (index >= 0) {
                    participants[index] = ParcelableUserUtils.fromUser(user, accountKey)
                } else {
                    this.participants = participants + ParcelableUserUtils.fromUser(user, accountKey)
                }
            }
        }

        fun MutableMap<String, ParcelableMessageConversation>.addConversation(
                conversationId: String,
                details: AccountDetails,
                message: ParcelableMessage,
                users: Collection<User>,
                conversationType: String = ConversationType.ONE_TO_ONE
    ): ParcelableMessageConversation {
        val conversation = this[conversationId] ?: run {
            val obj = ParcelableMessageConversation()
            obj.id = conversationId
            obj.conversation_type = conversationType
            obj.applyFrom(message, details)
            this[conversationId] = obj
            return@run obj
        }
        if (message.timestamp > conversation.timestamp) {
            conversation.applyFrom(message, details)
        }
        users.forEach { user ->
            conversation.addParticipant(details.key, user)
        }
        return conversation
    }

    }
}
