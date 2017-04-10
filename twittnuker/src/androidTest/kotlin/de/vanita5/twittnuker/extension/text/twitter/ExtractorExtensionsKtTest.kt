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

package de.vanita5.twittnuker.extension.text.twitter

import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import com.bluelinelabs.logansquare.LoganSquare
import com.twitter.Extractor
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import de.vanita5.twittnuker.model.ParcelableStatus
import de.vanita5.twittnuker.model.ParcelableUserMention
import de.vanita5.twittnuker.test.R

@RunWith(AndroidJUnit4::class)
class ExtractorExtensionsKtTest {

    private val extractor = Extractor()
    private lateinit var inReplyTo: ParcelableStatus

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getContext()

        // This is a tweet by @t_deyarmin, mentioning @nixcraft
        inReplyTo = context.resources.openRawResource(R.raw.parcelable_status_848051071444410368).use {
            LoganSquare.parse(it, ParcelableStatus::class.java)
        }
    }

    @Test
    fun testExtractReplyTextAndMentionsReplyToAll() {
        // This reply to all users, which is the normal case
        extractor.extractReplyTextAndMentions("@t_deyarmin @nixcraft lol", inReplyTo).let {
            Assert.assertEquals("lol", it.replyText)
            Assert.assertTrue("extraMentions.isEmpty()", it.extraMentions.isEmpty())
            Assert.assertTrue("excludedMentions.isEmpty()", it.excludedMentions.isEmpty())
            Assert.assertTrue("replyToOriginalUser", it.replyToOriginalUser)
        }
    }


    @Test
    fun testExtractReplyTextAndMentionsReplyToAllExtraMentions() {
        // This reply to all users, which is the normal case
        extractor.extractReplyTextAndMentions("@t_deyarmin @nixcraft @mariotaku lol", inReplyTo).let {
            Assert.assertEquals("@mariotaku lol", it.replyText)
            Assert.assertTrue("extraMentions.isEmpty()", it.extraMentions.entitiesContainsAll("mariotaku"))
            Assert.assertTrue("excludedMentions.isEmpty()", it.excludedMentions.isEmpty())
            Assert.assertTrue("replyToOriginalUser", it.replyToOriginalUser)
        }
    }

    @Test
    fun testExtractReplyTextAndMentionsReplyToAllSuffixMentions() {
        // This reply to all users, which is the normal case
        extractor.extractReplyTextAndMentions("@t_deyarmin @nixcraft lol @mariotaku", inReplyTo).let {
            Assert.assertEquals("lol @mariotaku", it.replyText)
            Assert.assertTrue("extraMentions.isEmpty()", it.extraMentions.entitiesContainsAll("mariotaku"))
            Assert.assertTrue("excludedMentions.isEmpty()", it.excludedMentions.isEmpty())
            Assert.assertTrue("replyToOriginalUser", it.replyToOriginalUser)
        }
    }

    @Test
    fun testExtractReplyTextAndMentionsAuthorOnly() {
        // This reply removed @nixcraft and replying to author only
        extractor.extractReplyTextAndMentions("@t_deyarmin lol", inReplyTo).let {
            Assert.assertEquals("lol", it.replyText)
            Assert.assertTrue("extraMentions.isEmpty()", it.extraMentions.isEmpty())
            Assert.assertTrue("excludedMentions.containsAll(expectation)",
                    it.excludedMentions.mentionsContainsAll("nixcraft"))
            Assert.assertTrue("replyToOriginalUser", it.replyToOriginalUser)
        }
    }

    @Test
    fun testExtractReplyTextAndMentionsAuthorOnlyExtraMentions() {
        // This reply removed @nixcraft and replying to author only
        extractor.extractReplyTextAndMentions("@t_deyarmin @mariotaku lol", inReplyTo).let {
            Assert.assertEquals("@mariotaku lol", it.replyText)
            val extraExpectation = setOf("mariotaku")
            Assert.assertTrue("extraMentions.containsAll(expectation)", it.extraMentions.all {
                it.value in extraExpectation
            })
            Assert.assertTrue("excludedMentions.containsAll(expectation)",
                    it.excludedMentions.mentionsContainsAll("nixcraft"))
            Assert.assertTrue("replyToOriginalUser", it.replyToOriginalUser)
        }
    }

    @Test
    fun testExtractReplyTextAndMentionsAuthorOnlySuffixMention() {
        // This reply removed @nixcraft and replying to author only
        extractor.extractReplyTextAndMentions("@t_deyarmin lol @mariotaku", inReplyTo).let {
            Assert.assertEquals("lol @mariotaku", it.replyText)
            Assert.assertTrue("extraMentions.isEmpty()",
                    it.extraMentions.entitiesContainsAll("mariotaku"))
            Assert.assertTrue("excludedMentions.containsAll(expectation)",
                    it.excludedMentions.mentionsContainsAll("nixcraft"))
            Assert.assertTrue("replyToOriginalUser", it.replyToOriginalUser)
        }
    }

    @Test
    fun testExtractReplyTextAndMentionsNoAuthor() {
        // This reply removed author @t_deyarmin
        extractor.extractReplyTextAndMentions("@nixcraft lol", inReplyTo).let {
            Assert.assertEquals("@nixcraft lol", it.replyText)
            Assert.assertTrue("extraMentions.isEmpty()", it.extraMentions.isEmpty())
            Assert.assertTrue("excludedMentions.isEmpty()", it.excludedMentions.isEmpty())
            Assert.assertFalse("replyToOriginalUser", it.replyToOriginalUser)
        }
    }

    @Test
    fun testExtractReplyTextAndMentionsNoAuthorExtraMentions() {
        // This reply removed author @t_deyarmin
        extractor.extractReplyTextAndMentions("@nixcraft @mariotaku lol", inReplyTo).let {
            Assert.assertEquals("@nixcraft @mariotaku lol", it.replyText)
            Assert.assertTrue("extraMentions.containsAll(expectation)",
                    it.extraMentions.entitiesContainsAll("mariotaku"))
            Assert.assertTrue("excludedMentions.isEmpty()", it.excludedMentions.isEmpty())
            Assert.assertFalse("replyToOriginalUser", it.replyToOriginalUser)
        }
    }

    private fun List<Extractor.Entity>.entitiesContainsAll(vararg screenNames: String): Boolean {
        return all { entity ->
            screenNames.any { expectation ->
                expectation.equals(entity.value, ignoreCase = true)
            }
        }
    }

    private fun List<ParcelableUserMention>.mentionsContainsAll(vararg screenNames: String): Boolean {
        return all { mention ->
            screenNames.any { expectation ->
                expectation.equals(mention.screen_name, ignoreCase = true)
            }
        }
    }
}