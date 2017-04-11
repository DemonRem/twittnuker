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

package de.vanita5.twittnuker.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.BadParcelableException
import android.support.customtabs.CustomTabsIntent
import org.mariotaku.chameleon.Chameleon
import org.mariotaku.chameleon.ChameleonUtils
import org.mariotaku.kpreferences.get
import de.vanita5.twittnuker.activity.WebLinkHandlerActivity
import de.vanita5.twittnuker.annotation.Referral
import de.vanita5.twittnuker.app.TwittnukerApplication
import de.vanita5.twittnuker.constant.IntentConstants.EXTRA_ACCOUNT_KEY
import de.vanita5.twittnuker.constant.chromeCustomTabKey
import de.vanita5.twittnuker.constant.displaySensitiveContentsKey
import de.vanita5.twittnuker.constant.newDocumentApiKey
import de.vanita5.twittnuker.model.UserKey
import de.vanita5.twittnuker.model.util.ParcelableMediaUtils
import de.vanita5.twittnuker.util.TwidereLinkify.OnLinkClickListener
import de.vanita5.twittnuker.util.TwidereLinkify.USER_TYPE_FANFOU_COM
import de.vanita5.twittnuker.util.media.preview.PreviewMediaExtractor

open class OnLinkClickHandler(
        protected val context: Context,
        protected val manager: MultiSelectManager?,
        protected val preferences: SharedPreferences
) : OnLinkClickListener {

    override fun onLinkClick(link: String, orig: String?, accountKey: UserKey?,
                             extraId: Long, type: Int, sensitive: Boolean,
                             start: Int, end: Int): Boolean {
        if (manager != null && manager.isActive) return false

        when (type) {
            TwidereLinkify.LINK_TYPE_MENTION -> {
                IntentUtils.openUserProfile(context, accountKey, null, link, null,
                        preferences[newDocumentApiKey], Referral.USER_MENTION, null)
                return true
            }
            TwidereLinkify.LINK_TYPE_HASHTAG -> {
                IntentUtils.openTweetSearch(context, accountKey, "#" + link)
                return true
            }
            TwidereLinkify.LINK_TYPE_LINK_IN_TEXT -> {
                if (accountKey != null && isMedia(link, extraId)) {
                    openMedia(accountKey, extraId, sensitive, link, start, end)
                } else {
                    openLink(link)
                }
                return true
            }
            TwidereLinkify.LINK_TYPE_ENTITY_URL -> {
                if (accountKey != null && isMedia(link, extraId)) {
                    openMedia(accountKey, extraId, sensitive, link, start, end)
                } else {
                    val authority = UriUtils.getAuthority(link)
                    if (authority == null) {
                        openLink(link)
                        return true
                    }
                    when (authority) {
                        "fanfou.com" -> if (accountKey != null && handleFanfouLink(link, orig, accountKey)) {
                            return true
                        }
                        else -> if (IntentUtils.isWebLinkHandled(context, Uri.parse(link))) {
                            openTwitterLink(link, accountKey!!)
                            return true
                        }
                    }
                    openLink(link)
                }
                return true
            }
            TwidereLinkify.LINK_TYPE_LIST -> {
                val mentionList = link.split("/")
                if (mentionList.size != 2) {
                    return false
                }
                IntentUtils.openUserListDetails(context, accountKey, null, null, mentionList[0],
                        mentionList[1])
                return true
            }
            TwidereLinkify.LINK_TYPE_CASHTAG -> {
                IntentUtils.openTweetSearch(context, accountKey, link)
                return true
            }
            TwidereLinkify.LINK_TYPE_USER_ID -> {
                IntentUtils.openUserProfile(context, accountKey, UserKey.valueOf(link), null, null,
                        preferences[newDocumentApiKey], Referral.USER_MENTION, null)
                return true
            }
        }
        return false
    }

    protected open fun isMedia(link: String, extraId: Long): Boolean {
        return PreviewMediaExtractor.isSupported(link)
    }

    protected open fun openMedia(accountKey: UserKey, extraId: Long, sensitive: Boolean, link: String, start: Int, end: Int) {
        val media = arrayOf(ParcelableMediaUtils.image(link))
        IntentUtils.openMedia(context, accountKey, media, null, sensitive, preferences[newDocumentApiKey],
                preferences[displaySensitiveContentsKey])
    }

    protected open fun openLink(link: String) {
        if (manager != null && manager.isActive) return
        openLink(context, preferences, link)
    }

    protected fun openTwitterLink(link: String, accountKey: UserKey) {
        if (manager != null && manager.isActive) return
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.setClass(context, WebLinkHandlerActivity::class.java)
        intent.putExtra(EXTRA_ACCOUNT_KEY, accountKey)
        intent.setExtrasClassLoader(TwittnukerApplication::class.java.classLoader)
        if (intent.resolveActivity(context.packageManager) != null) {
            try {
                context.startActivity(intent)
            } catch (e: BadParcelableException) {
                // Ignore
            }

        }
    }

    private fun handleFanfouLink(link: String, orig: String?, accountKey: UserKey): Boolean {
        if (orig == null) return false
        // Process special case for fanfou
        val ch = orig[0]
        // Extend selection
        val length = orig.length
        if (TwidereLinkify.isAtSymbol(ch)) {
            var id = UriUtils.getPath(link)
            if (id != null) {
                val idxOfSlash = id.indexOf('/')
                if (idxOfSlash == 0) {
                    id = id.substring(1)
                }
                val screenName = orig.substring(1, length)
                IntentUtils.openUserProfile(context, accountKey, UserKey.valueOf(id), screenName,
                        null, preferences[newDocumentApiKey], Referral.USER_MENTION, null)
                return true
            }
        } else if (TwidereLinkify.isHashSymbol(ch) && TwidereLinkify.isHashSymbol(orig[length - 1])) {
            IntentUtils.openSearch(context, accountKey, orig.substring(1, length - 1))
            return true
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
        intent.setClass(context, WebLinkHandlerActivity::class.java)
        if (accountKey.host == USER_TYPE_FANFOU_COM) {
            intent.putExtra(EXTRA_ACCOUNT_KEY, accountKey)
        }
        context.startActivity(intent)
        return true
    }

    companion object {

        fun openLink(context: Context, preferences: SharedPreferences, link: String) {
            val uri = Uri.parse(link)
            if (!preferences[chromeCustomTabKey]) {
                val viewIntent = Intent(Intent.ACTION_VIEW, uri)
                viewIntent.addCategory(Intent.CATEGORY_BROWSABLE)
                try {
                    return context.startActivity(viewIntent)
                } catch (e: ActivityNotFoundException) {
                    // Ignore
                }
                return
            }
            val builder = CustomTabsIntent.Builder()
            builder.addDefaultShareMenuItem()
            (ChameleonUtils.getActivity(context) as? Chameleon.Themeable)?.overrideTheme?.let { theme ->
                builder.setToolbarColor(theme.colorToolbar)
            }
            val intent = builder.build()
            try {
                intent.launchUrl(context, uri)
            } catch (e: ActivityNotFoundException) {
                // Ignore
            }

        }
    }
}