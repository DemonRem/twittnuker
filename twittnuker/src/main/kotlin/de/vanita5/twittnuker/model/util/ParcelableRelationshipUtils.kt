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

package de.vanita5.twittnuker.model.util

import android.content.ContentResolver
import android.support.v4.util.ArraySet
import org.mariotaku.library.objectcursor.ObjectCursor
import de.vanita5.microblog.library.twitter.model.Relationship
import de.vanita5.microblog.library.twitter.model.User
import org.mariotaku.sqliteqb.library.Expression
import de.vanita5.twittnuker.model.ParcelableRelationship
import de.vanita5.twittnuker.model.ParcelableUser
import de.vanita5.twittnuker.model.UserKey
import de.vanita5.twittnuker.provider.TwidereDataStore.CachedRelationships
import de.vanita5.twittnuker.util.content.ContentResolverUtils

object ParcelableRelationshipUtils {

    fun create(accountKey: UserKey, userKey: UserKey, relationship: Relationship?,
               filtering: Boolean = false): ParcelableRelationship {
        val obj = ParcelableRelationship()
        obj.account_key = accountKey
        obj.user_key = userKey
        if (relationship != null) {
            obj.following = relationship.isSourceFollowingTarget
            obj.followed_by = relationship.isSourceFollowedByTarget
            obj.blocking = relationship.isSourceBlockingTarget
            obj.blocked_by = relationship.isSourceBlockedByTarget
            obj.muting = relationship.isSourceMutingTarget
            obj.retweet_enabled = relationship.isSourceWantRetweetsFromTarget
            obj.notifications_enabled = relationship.isSourceNotificationsEnabledForTarget
            obj.can_dm = relationship.canSourceDMTarget()
        }
        obj.filtering = filtering
        return obj
    }

    fun create(user: ParcelableUser, filtering: Boolean): ParcelableRelationship {
        val obj = ParcelableRelationship()
        obj.account_key = user.account_key
        obj.user_key = user.key
        obj.filtering = filtering
        if (user.extras != null) {
            obj.following = user.is_following
            obj.followed_by = user.extras?.followed_by ?: false
            obj.blocking = user.extras?.blocking ?: false
            obj.blocked_by = user.extras?.blocked_by ?: false
            obj.can_dm = user.extras?.followed_by ?: false
            obj.notifications_enabled = user.extras?.notifications_enabled ?: false
        }
        return obj
    }

    fun create(accountKey: UserKey, userKey: UserKey, user: User,
               filtering: Boolean = false): ParcelableRelationship {
        val obj = ParcelableRelationship()
        obj.account_key = accountKey
        obj.user_key = userKey
        obj.filtering = filtering
        obj.following = user.isFollowing == true
        obj.followed_by = user.isFollowedBy == true
        obj.blocking = user.isBlocking == true
        obj.blocked_by = user.isBlockedBy == true
        obj.can_dm = user.isFollowedBy == true
        obj.notifications_enabled = user.isNotificationsEnabled == true
        return obj
    }

    /**
     * @param relationships Relationships to update, if an item has _id, then we will call
     * `ContentResolver.update`, `ContentResolver.bulkInsert` otherwise.
     */
    fun insert(cr: ContentResolver, relationships: Collection<ParcelableRelationship>) {
        val insertItems = ArraySet<ParcelableRelationship>()
        val valuesCreator = ObjectCursor.valuesCreatorFrom(ParcelableRelationship::class.java)
        relationships.forEach {
            if (it._id > 0) {
                val values = valuesCreator.create(it)
                val where = Expression.equals(CachedRelationships._ID, it._id).sql
                cr.update(CachedRelationships.CONTENT_URI, values, where, null)
            } else {
                insertItems.add(it)
            }
        }
        ContentResolverUtils.bulkInsert(cr, CachedRelationships.CONTENT_URI,
                insertItems.map(valuesCreator::create))
    }
}