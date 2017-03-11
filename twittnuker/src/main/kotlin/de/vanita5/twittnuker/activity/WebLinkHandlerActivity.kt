/*
 *  Twittnuker - Twitter client for Android
 *  
 *  Copyright (C) 2013-2017 vanita5 <mail@vanit.as>
 *  
 *  This program incorporates a modified version of Twidere.
 *  Copyright (C) 2012-2017 Mariotaku Lee <mariotaku.lee@gmail.com>
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
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import org.apache.commons.lang3.ArrayUtils
import org.mariotaku.ktextension.toLong
import de.vanita5.twittnuker.R
import de.vanita5.twittnuker.TwittnukerConstants.*
import de.vanita5.twittnuker.app.TwittnukerApplication
import de.vanita5.twittnuker.model.UserKey
import de.vanita5.twittnuker.util.Analyzer
import de.vanita5.twittnuker.util.IntentUtils
import de.vanita5.twittnuker.util.Utils
import java.util.*

class WebLinkHandlerActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val packageManager = packageManager
        intent.setExtrasClassLoader(TwittnukerApplication::class.java.classLoader)
        val uri = intent.data
        if (uri == null || uri.host == null) {
            finish()
            return
        }

        val (handledIntent, handledSuccessfully) = when (uri.host.toLowerCase(Locale.US)) {
            "twitter.com", "www.twitter.com", "mobile.twitter.com" -> handleTwitterLink(regulateTwitterUri(uri))
            "fanfou.com" -> handleFanfouLink(uri)
            "twittnuker.org" -> handleTwidereExternalLink(uri)
            else -> Pair(null, false)
            }
        if (handledIntent != null) {
            handledIntent.putExtras(intent)
            startActivity(handledIntent)
        } else {
            if (!handledSuccessfully) {
                Analyzer.logException(TwitterLinkException("Unable to handle twitter uri " + uri))
            }
            val fallbackIntent = Intent(Intent.ACTION_VIEW, uri)
            fallbackIntent.addCategory(Intent.CATEGORY_BROWSABLE)
            fallbackIntent.`package` = IntentUtils.getDefaultBrowserPackage(this, uri, false)
            val componentName = fallbackIntent.resolveActivity(packageManager)
            if (componentName == null) {
                val targetIntent = Intent(Intent.ACTION_VIEW, uri)
                targetIntent.addCategory(Intent.CATEGORY_BROWSABLE)
                startActivity(Intent.createChooser(targetIntent, getString(R.string.action_open_in_browser)))
            } else if (!TextUtils.equals(packageName, componentName.packageName)) {
                startActivity(fallbackIntent)
            } else {
                // TODO show error
            }
        }
        finish()
    }

    override fun onStart() {
        super.onStart()
        setVisible(true)
    }

    private fun handleTwidereExternalLink(uri: Uri): Pair<Intent?, Boolean> {
        val pathSegments = uri.pathSegments
        if (pathSegments.size < 2 || pathSegments[0] != "external") {
            return Pair(null, false)
        }
        val builder = Uri.Builder()
        builder.scheme(SCHEME_TWITTNUKER)
        builder.authority(pathSegments[1])
        if (pathSegments.size >= 3) {
            for (segment in pathSegments.slice(2..pathSegments.lastIndex)) {
                builder.appendPath(segment)
            }
        }
        builder.encodedQuery(uri.encodedQuery)
        builder.encodedFragment(uri.encodedFragment)
        return Pair(Intent(Intent.ACTION_VIEW, builder.build()), true)
    }

    private fun handleFanfouLink(uri: Uri): Pair<Intent?, Boolean> {
        val pathSegments = uri.pathSegments
        if (pathSegments.size > 0) {
            when (pathSegments[0]) {
                "statuses" -> {
                    val builder = Uri.Builder()
                    builder.scheme(SCHEME_TWITTNUKER)
                    builder.authority(AUTHORITY_STATUS)
                    builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_HOST, USER_TYPE_FANFOU_COM)
                    builder.appendQueryParameter(QUERY_PARAM_STATUS_ID, pathSegments[1])
                    return Pair(Intent(Intent.ACTION_VIEW, builder.build()), true)
                }
                else -> {
                    if (FANFOU_RESERVED_PATHS.contains(pathSegments[0])) return Pair(null, false)
                    if (pathSegments.size == 1) {
                        val builder = Uri.Builder()
                        builder.scheme(SCHEME_TWITTNUKER)
                        builder.authority(AUTHORITY_USER)
                        builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_HOST, USER_TYPE_FANFOU_COM)
                        val userKey = UserKey(pathSegments[0], USER_TYPE_FANFOU_COM)
                        builder.appendQueryParameter(QUERY_PARAM_USER_KEY, userKey.toString())
                        return Pair(Intent(Intent.ACTION_VIEW, builder.build()), true)
                    }
                    return Pair(null, false)
                }
            }
        }
        return Pair(null, false)
    }

    private fun handleTwitterLink(uri: Uri): Pair<Intent?, Boolean> {
        val pathSegments = uri.pathSegments
        if (pathSegments.size > 0) {
            when (pathSegments[0]) {
                "i" -> {
                    return getIUriIntent(uri, pathSegments)
                }
                "intent" -> {
                    return getTwitterIntentUriIntent(uri, pathSegments)
                }
                "share" -> {
                    val handledIntent = Intent(this, ComposeActivity::class.java)
                    handledIntent.action = Intent.ACTION_SEND
                    val text = uri.getQueryParameter("text")
                    val url = uri.getQueryParameter("url")
                    handledIntent.putExtra(Intent.EXTRA_TEXT, Utils.getShareStatus(this, text, url))
                    return Pair(handledIntent, true)
                }
                "search" -> {
                    val builder = Uri.Builder()
                    builder.scheme(SCHEME_TWITTNUKER)
                    builder.authority(AUTHORITY_SEARCH)
                    builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_HOST, USER_TYPE_TWITTER_COM)
                    builder.appendQueryParameter(QUERY_PARAM_QUERY, uri.getQueryParameter("q"))
                    return Pair(Intent(Intent.ACTION_VIEW, builder.build()), true)
                }
                "hashtag" -> {
                    val builder = Uri.Builder()
                    builder.scheme(SCHEME_TWITTNUKER)
                    builder.authority(AUTHORITY_SEARCH)
                    builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_HOST, USER_TYPE_TWITTER_COM)
                    builder.appendQueryParameter(QUERY_PARAM_QUERY, "#${uri.lastPathSegment}")
                    return Pair(Intent(Intent.ACTION_VIEW, builder.build()), true)
                }
                "following" -> {
                    val builder = Uri.Builder()
                    builder.scheme(SCHEME_TWITTNUKER)
                    builder.authority(AUTHORITY_USER_FRIENDS)
                    builder.appendQueryParameter(QUERY_PARAM_USER_KEY, UserKey.SELF.toString())
                    return Pair(Intent(Intent.ACTION_VIEW, builder.build()), true)
                }
                "followers" -> {
                    val builder = Uri.Builder()
                    builder.scheme(SCHEME_TWITTNUKER)
                    builder.authority(AUTHORITY_USER_FOLLOWERS)
                    builder.appendQueryParameter(QUERY_PARAM_USER_KEY, UserKey.SELF.toString())
                    return Pair(Intent(Intent.ACTION_VIEW, builder.build()), true)
                }
                "favorites" -> {
                    val builder = Uri.Builder()
                    builder.scheme(SCHEME_TWITTNUKER)
                    builder.authority(AUTHORITY_USER_FAVORITES)
                    builder.appendQueryParameter(QUERY_PARAM_USER_KEY, UserKey.SELF.toString())
                    return Pair(Intent(Intent.ACTION_VIEW, builder.build()), true)
                }
                else -> {
                    if (ArrayUtils.contains(TWITTER_RESERVED_PATHS, pathSegments[0])) {
                        return Pair(null, true)
                    }
                    return handleUserSpecificPageIntent(uri, pathSegments, pathSegments[0])
                }
            }
        }
        return Pair(null, false)
    }

    private fun handleUserSpecificPageIntent(uri: Uri, pathSegments: List<String>, screenName: String): Pair<Intent?, Boolean> {
        val segsSize = pathSegments.size
        if (segsSize == 1) {
            val builder = Uri.Builder()
            builder.scheme(SCHEME_TWITTNUKER)
            builder.authority(AUTHORITY_USER)
            builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_HOST, USER_TYPE_TWITTER_COM)
            builder.appendQueryParameter(QUERY_PARAM_SCREEN_NAME, screenName)
            return Pair(Intent(Intent.ACTION_VIEW, builder.build()), true)
        } else if (segsSize == 2) {
            when (pathSegments[1]) {
                "following" -> {
                    val builder = Uri.Builder()
                    builder.scheme(SCHEME_TWITTNUKER)
                    builder.authority(AUTHORITY_USER_FRIENDS)
                    builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_HOST, USER_TYPE_TWITTER_COM)
                    builder.appendQueryParameter(QUERY_PARAM_SCREEN_NAME, screenName)
                    return Pair(Intent(Intent.ACTION_VIEW, builder.build()), true)
                }
                "followers" -> {
                    val builder = Uri.Builder()
                    builder.scheme(SCHEME_TWITTNUKER)
                    builder.authority(AUTHORITY_USER_FOLLOWERS)
                    builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_HOST, USER_TYPE_TWITTER_COM)
                    builder.appendQueryParameter(QUERY_PARAM_SCREEN_NAME, screenName)
                    return Pair(Intent(Intent.ACTION_VIEW, builder.build()), true)
                }
                "favorites" -> {
                    val builder = Uri.Builder()
                    builder.scheme(SCHEME_TWITTNUKER)
                    builder.authority(AUTHORITY_USER_FAVORITES)
                    builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_HOST, USER_TYPE_TWITTER_COM)
                    builder.appendQueryParameter(QUERY_PARAM_SCREEN_NAME, screenName)
                    return Pair(Intent(Intent.ACTION_VIEW, builder.build()), true)
                }
                else -> {
                    val builder = Uri.Builder()
                    builder.scheme(SCHEME_TWITTNUKER)
                    builder.authority(AUTHORITY_USER_LIST)
                    builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_HOST, USER_TYPE_TWITTER_COM)
                    builder.appendQueryParameter(QUERY_PARAM_SCREEN_NAME, screenName)
                    builder.appendQueryParameter(QUERY_PARAM_LIST_NAME, pathSegments[1])
                    return Pair(Intent(Intent.ACTION_VIEW, builder.build()), true)
                }
            }
        } else if (segsSize >= 3) {
            if ("status" == pathSegments[1] && pathSegments[2].toLong(-1) != -1L) {
                val builder = Uri.Builder()
                builder.scheme(SCHEME_TWITTNUKER)
                builder.authority(AUTHORITY_STATUS)
                builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_HOST, USER_TYPE_TWITTER_COM)
                builder.appendQueryParameter(QUERY_PARAM_STATUS_ID, pathSegments[2])
                return Pair(Intent(Intent.ACTION_VIEW, builder.build()), true)
            } else {
                when (pathSegments[2]) {
                    "members" -> {
                        val builder = Uri.Builder()
                        builder.scheme(SCHEME_TWITTNUKER)
                        builder.authority(AUTHORITY_USER_LIST_MEMBERS)
                        builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_HOST, USER_TYPE_TWITTER_COM)
                        builder.appendQueryParameter(QUERY_PARAM_SCREEN_NAME, screenName)
                        builder.appendQueryParameter(QUERY_PARAM_LIST_NAME, pathSegments[1])
                        return Pair(Intent(Intent.ACTION_VIEW, builder.build()), true)
                    }
                    "subscribers" -> {
                        val builder = Uri.Builder()
                        builder.scheme(SCHEME_TWITTNUKER)
                        builder.authority(AUTHORITY_USER_LIST_SUBSCRIBERS)
                        builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_HOST, USER_TYPE_TWITTER_COM)
                        builder.appendQueryParameter(QUERY_PARAM_SCREEN_NAME, screenName)
                        builder.appendQueryParameter(QUERY_PARAM_LIST_NAME, pathSegments[1])
                        return Pair(Intent(Intent.ACTION_VIEW, builder.build()), true)
                    }
                }
            }
        }
        return Pair(null, false)
    }

    private fun getTwitterIntentUriIntent(uri: Uri, pathSegments: List<String>): Pair<Intent?, Boolean> {
        if (pathSegments.size < 2) return Pair(null, false)
        when (pathSegments[1]) {
            "tweet" -> {
                val handledIntent = Intent(this, ComposeActivity::class.java)
                handledIntent.action = Intent.ACTION_SEND
                val text = uri.getQueryParameter("text")
                val url = uri.getQueryParameter("url")
                val sb = StringBuilder()
                if (!text.isNullOrEmpty()) {
                    sb.append(text)
                }
                if (!url.isNullOrEmpty()) {
                    if (!sb.isEmpty()) {
                        sb.append(" ")
                    }
                    sb.append(url)
                }
                handledIntent.putExtra(Intent.EXTRA_TEXT, sb.toString())
                return Pair(handledIntent, true)
            }
            "favorite", "retweet" -> {
                val tweetId = uri.getQueryParameter("tweet_id") ?: return Pair(null, false)
                return Pair(IntentUtils.status(null, tweetId), true)
            }
            "user", "follow" -> {
                val userKey = uri.getQueryParameter("user_id")?.let { UserKey(it, "twitter.com") }
                val screenName = uri.getQueryParameter("screen_name")
                return Pair(IntentUtils.userProfile(null, userKey, screenName), true)
            }
        }
        return Pair(null, false)
    }

    private fun getIUriIntent(uri: Uri, pathSegments: List<String>): Pair<Intent?, Boolean> {
        return Pair(null, false)
    }

    private inner class TwitterLinkException(s: String) : Exception(s)

    companion object {

        val TWITTER_RESERVED_PATHS = arrayOf("about", "account", "accounts", "activity", "all",
                "announcements", "anywhere", "api_rules", "api_terms", "apirules", "apps", "auth",
                "badges", "blog", "business", "buttons", "contacts", "devices", "direct_messages",
                "download", "downloads", "edit_announcements", "faq", "favorites", "find_sources",
                "find_users", "followers", "following", "friend_request", "friendrequest", "friends",
                "goodies", "help", "home", "im_account", "inbox", "invitations", "invite", "jobs",
                "list", "login", "logo", "logout", "me", "mentions", "messages", "mockview",
                "newtwitter", "notifications", "nudge", "oauth", "phoenix_search", "positions",
                "privacy", "public_timeline", "related_tweets", "replies", "retweeted_of_mine",
                "retweets", "retweets_by_others", "rules", "saved_searches", "search", "sent",
                "settings", "share", "signup", "signin", "similar_to", "statistics", "terms", "tos",
                "translate", "trends", "tweetbutton", "twttr", "update_discoverability", "users",
                "welcome", "who_to_follow", "widgets", "zendesk_auth", "media_signup")

        val FANFOU_RESERVED_PATHS = arrayOf("home", "privatemsg", "finder", "browse", "search",
                "settings", "message", "mentions", "favorites", "friends", "followers", "sharer",
                "photo", "album", "paipai", "q", "userview", "dialogue")


        private val AUTHORITY_TWITTER_COM = "twitter.com"


        private fun regulateTwitterUri(data: Uri): Uri {
            val encodedFragment = data.encodedFragment
            if (encodedFragment != null && encodedFragment.startsWith("!/")) {
                return regulateTwitterUri(Uri.parse("https://twitter.com" + encodedFragment.substring(1)))
            }
            val builder = data.buildUpon()
            builder.scheme("https")
            builder.authority(AUTHORITY_TWITTER_COM)
            return builder.build()
        }
    }
}