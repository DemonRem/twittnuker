/*
 * Twittnuker - Twitter client for Android
 *
 * Copyright (C) 2013-2015 vanita5 <mail@vanita5.de>
 *
 * This program incorporates a modified version of Twidere.
 * Copyright (C) 2012-2015 Mariotaku Lee <mariotaku.lee@gmail.com>
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

package de.vanita5.twittnuker.model;

import android.support.annotation.NonNull;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;

import java.util.Arrays;

import de.vanita5.twittnuker.api.twitter.model.Activity;
import de.vanita5.twittnuker.api.twitter.model.Status;
import de.vanita5.twittnuker.api.twitter.model.User;
import de.vanita5.twittnuker.api.twitter.model.UserList;

@JsonObject
public class ParcelableActivity implements Comparable<ParcelableActivity> {

    public final static int ACTION_FAVORITE = Activity.Action.ACTION_FAVORITE;
    public final static int ACTION_FOLLOW = Activity.Action.ACTION_FOLLOW;
    public final static int ACTION_MENTION = Activity.Action.ACTION_MENTION;
    public final static int ACTION_REPLY = Activity.Action.ACTION_REPLY;
    public final static int ACTION_RETWEET = Activity.Action.ACTION_RETWEET;
    public final static int ACTION_LIST_MEMBER_ADDED = Activity.Action.ACTION_LIST_MEMBER_ADDED;
    public final static int ACTION_LIST_CREATED = Activity.Action.ACTION_LIST_CREATED;
    public final static int ACTION_FAVORITED_RETWEET = Activity.Action.ACTION_FAVORITED_RETWEET;
    public final static int ACTION_RETWEETED_RETWEET = Activity.Action.ACTION_RETWEETED_RETWEET;

    @JsonField(name = "account_id")
    public long account_id;
    @JsonField(name = "timestamp")
    public long timestamp;
    @JsonField(name = "max_position")
    public long max_position;
    @JsonField(name = "min_position")
    public long min_position;
    @JsonField(name = "action")
    public int action;

    @JsonField(name = "sources")
    public ParcelableUser[] sources;
    @JsonField(name = "target_users")
    public ParcelableUser[] target_users;
    @JsonField(name = "target_statuses")
    public ParcelableStatus[] target_statuses;
    @JsonField(name = "target_user_lists")
    public ParcelableUserList[] target_user_lists;

    @JsonField(name = "target_object_user_lists")
    public ParcelableUserList[] target_object_user_lists;
    @JsonField(name = "target_object_statuses")
    public ParcelableStatus[] target_object_statuses;
    @JsonField(name = "is_gap")
    public boolean is_gap;

    public ParcelableActivity() {
    }

    public ParcelableActivity(final Activity activity, final long account_id, boolean is_gap) {
		this.account_id = account_id;
        timestamp = activity.getCreatedAt().getTime();
		action = activity.getAction().getActionId();
		max_position = activity.getMaxPosition();
		min_position = activity.getMinPosition();
		final int sources_size = activity.getSourcesSize();
		sources = new ParcelableUser[sources_size];
		for (int i = 0; i < sources_size; i++) {
			sources[i] = new ParcelableUser(activity.getSources()[i], account_id);
		}
		final int targets_size = activity.getTargetsSize();
        final User[] targetUsers = activity.getTargetUsers();
        if (targetUsers != null) {
			target_users = new ParcelableUser[targets_size];
			for (int i = 0; i < targets_size; i++) {
                target_users[i] = new ParcelableUser(targetUsers[i], account_id);
			}
        } else {
            target_users = null;
        }
        final UserList[] targetUserLists = activity.getTargetUserLists();
        if (targetUserLists != null) {
			target_user_lists = new ParcelableUserList[targets_size];
			for (int i = 0; i < targets_size; i++) {
                target_user_lists[i] = new ParcelableUserList(targetUserLists[i], account_id);
			}
		} else {
            target_user_lists = null;
        }
        final Status[] targetStatuses = activity.getTargetStatuses();
        if (targetStatuses != null) {
			target_statuses = new ParcelableStatus[targets_size];
			for (int i = 0; i < targets_size; i++) {
                target_statuses[i] = new ParcelableStatus(targetStatuses[i], account_id, false);
			}
        } else {
            target_statuses = null;
		}
		final int target_objects_size = activity.getTargetObjectsSize();
        final Status[] targetObjectStatuses = activity.getTargetObjectStatuses();
        if (targetObjectStatuses != null) {
            target_object_statuses = new ParcelableStatus[target_objects_size];
            for (int i = 0; i < target_objects_size; i++) {
                target_object_statuses[i] = new ParcelableStatus(targetObjectStatuses[i], account_id, false);
            }
        } else {
            target_object_statuses = null;
        }
        final UserList[] targetObjectUserLists = activity.getTargetObjectUserLists();
        if (targetObjectUserLists != null) {
			target_object_user_lists = new ParcelableUserList[target_objects_size];
			for (int i = 0; i < target_objects_size; i++) {
                target_object_user_lists[i] = new ParcelableUserList(targetObjectUserLists[i], account_id);
			}
		} else {
			target_object_user_lists = null;
			}
        this.is_gap = is_gap;
		}


	@Override
    public int compareTo(@NonNull final ParcelableActivity another) {
        final long delta = another.timestamp - timestamp;
		if (delta < Integer.MIN_VALUE) return Integer.MIN_VALUE;
		if (delta > Integer.MAX_VALUE) return Integer.MAX_VALUE;
		return (int) delta;
	}

	@Override
	public boolean equals(final Object that) {
		if (!(that instanceof ParcelableActivity)) return false;
		final ParcelableActivity activity = (ParcelableActivity) that;
		return max_position == activity.max_position && min_position == activity.min_position;
	}

	@Override
	public String toString() {
        return "ParcelableActivity{account_id=" + account_id + ", timestamp=" + timestamp
				+ ", max_position=" + max_position + ", min_position=" + min_position + ", action=" + action
				+ ", sources=" + Arrays.toString(sources) + ", target_users=" + Arrays.toString(target_users)
				+ ", target_statuses=" + Arrays.toString(target_statuses) + ", target_user_lists="
				+ Arrays.toString(target_user_lists) + ", target_object_user_lists="
				+ Arrays.toString(target_object_user_lists) + ", target_object_statuses="
				+ Arrays.toString(target_object_statuses) + "}";
	}


}