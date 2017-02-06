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

package de.vanita5.twittnuker.loader

import android.accounts.AccountManager
import android.content.Context
import android.os.Bundle
import android.support.v4.content.AsyncTaskLoader
import android.text.TextUtils
import android.util.Log
import org.mariotaku.abstask.library.TaskStarter
import de.vanita5.twittnuker.library.MicroBlog
import de.vanita5.twittnuker.library.MicroBlogException
import de.vanita5.twittnuker.library.twitter.model.User
import org.mariotaku.sqliteqb.library.Columns
import org.mariotaku.sqliteqb.library.Expression
import de.vanita5.twittnuker.Constants
import de.vanita5.twittnuker.TwittnukerConstants.*
import de.vanita5.twittnuker.annotation.AccountType
import de.vanita5.twittnuker.annotation.Referral
import de.vanita5.twittnuker.extension.model.newMicroBlogInstance
import de.vanita5.twittnuker.model.*
import de.vanita5.twittnuker.model.util.AccountUtils
import de.vanita5.twittnuker.model.util.ParcelableUserUtils
import de.vanita5.twittnuker.model.util.UserKeyUtils
import de.vanita5.twittnuker.provider.TwidereDataStore.CachedUsers
import de.vanita5.twittnuker.task.UpdateAccountInfoTask
import de.vanita5.twittnuker.util.ContentValuesCreator.createCachedUser
import de.vanita5.twittnuker.util.TwitterWrapper
import de.vanita5.twittnuker.util.UserColorNameManager
import de.vanita5.twittnuker.util.dagger.GeneralComponentHelper
import javax.inject.Inject

class ParcelableUserLoader(
        context: Context,
        private val accountKey: UserKey,
        private val userKey: UserKey?,
        private val screenName: String?,
        private val extras: Bundle?,
        private val omitIntentExtra: Boolean,
        private val loadFromCache: Boolean
) : AsyncTaskLoader<SingleResponse<ParcelableUser>>(context), Constants {

    @Inject
    lateinit var userColorNameManager: UserColorNameManager

    init {
        GeneralComponentHelper.build(context).inject(this)
    }

    override fun loadInBackground(): SingleResponse<ParcelableUser> {
        val context = context
        val resolver = context.contentResolver
        val accountKey = accountKey
        val am = AccountManager.get(context)
        val details = AccountUtils.getAllAccountDetails(am, AccountUtils.getAccounts(am), true).firstOrNull {
            if (it.key == accountKey) {
                return@firstOrNull true
            } else if (it.user.account_key == accountKey) {
                return@firstOrNull true
            }
            return@firstOrNull false
        } ?: return SingleResponse()
        if (!omitIntentExtra && extras != null) {
            val user = extras.getParcelable<ParcelableUser>(EXTRA_USER)
            if (user != null) {
                val values = ParcelableUserValuesCreator.create(user)
                resolver.insert(CachedUsers.CONTENT_URI, values)
                ParcelableUserUtils.updateExtraInformation(user, details, userColorNameManager)
                val response = SingleResponse(user)
                response.extras.putParcelable(EXTRA_ACCOUNT, details)
                return response
            }
        }
        val twitter = details.newMicroBlogInstance(context = context, cls = MicroBlog::class.java)
        if (loadFromCache) {
            val where: Expression
            val whereArgs: Array<String>
            if (userKey != null) {
                where = Expression.equalsArgs(CachedUsers.USER_KEY)
                whereArgs = arrayOf(userKey.toString())
            } else if (screenName != null) {
                val host = this.accountKey.host
                if (host != null) {
                    where = Expression.and(
                            Expression.likeRaw(Columns.Column(CachedUsers.USER_KEY), "'%@'||?"),
                            Expression.equalsArgs(CachedUsers.SCREEN_NAME))
                    whereArgs = arrayOf(host, screenName)
                } else {
                    where = Expression.equalsArgs(CachedUsers.SCREEN_NAME)
                    whereArgs = arrayOf(screenName)
                }
            } else {
                return SingleResponse()
            }
            resolver.query(CachedUsers.CONTENT_URI, CachedUsers.COLUMNS, where.sql,
                    whereArgs, null)?.let { cur ->
                try {
                    cur.moveToFirst()
                    val indices = ParcelableUserCursorIndices(cur)
                    while (!cur.isAfterLast) {
                        val user = indices.newObject(cur)
                        if (TextUtils.equals(UserKeyUtils.getUserHost(user), user.key.host)) {
                            user.account_key = accountKey
                            user.account_color = details.color
                            val response = SingleResponse(user)
                            response.extras.putParcelable(EXTRA_ACCOUNT, details)
                            return response
                        }
                        cur.moveToNext()
                    }
                } finally {
                    cur.close()
                }
            }
        }
        try {
            val twitterUser: User
            if (extras != null && Referral.SELF_PROFILE == extras.getString(EXTRA_REFERRAL)) {
                twitterUser = twitter.verifyCredentials()
            } else {
                var profileUrl: String? = null
                if (extras != null) {
                    profileUrl = extras.getString(EXTRA_PROFILE_URL)
                }
                if (details.type == AccountType.STATUSNET && userKey != null && profileUrl != null
                        && details.key.host != userKey.host) {
                    twitterUser = twitter.showExternalProfile(profileUrl)
                } else {
                    val id = userKey?.id
                    twitterUser = TwitterWrapper.tryShowUser(twitter, id, screenName,
                            details.type)
                }
            }
            val cachedUserValues = createCachedUser(twitterUser)
            resolver.insert(CachedUsers.CONTENT_URI, cachedUserValues)
            val user = ParcelableUserUtils.fromUser(twitterUser, accountKey)
            ParcelableUserUtils.updateExtraInformation(user, details, userColorNameManager)
            val response = SingleResponse.Companion.getInstance(user)
            response.extras.putParcelable(EXTRA_ACCOUNT, details)
            return response
        } catch (e: MicroBlogException) {
            Log.w(LOGTAG, e)
            return SingleResponse(exception = e)
        }

    }

    override fun onStartLoading() {
        if (!omitIntentExtra && extras != null) {
            val user = extras.getParcelable<ParcelableUser>(EXTRA_USER)
            if (user != null) {
//                deliverResult(SingleResponse(user))
            }
        }
        forceLoad()
    }

    override fun deliverResult(data: SingleResponse<ParcelableUser>) {
        super.deliverResult(data)
        val user = data.data ?: return
        if (user.is_cache) return
        val account = data.extras.getParcelable<AccountDetails>(EXTRA_ACCOUNT)
        if (account != null) {
            val task = UpdateAccountInfoTask(context)
            task.params = Pair(account, user)
            TaskStarter.execute(task)
        }
    }
}