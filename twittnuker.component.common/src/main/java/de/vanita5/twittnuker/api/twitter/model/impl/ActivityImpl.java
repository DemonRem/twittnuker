/*
 * Twittnuker - Twitter client for Android
 *
 * Copyright (C) 2013-2015 vanita5 <mail@vanit.as>
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

package de.vanita5.twittnuker.api.twitter.model.impl;

import android.support.annotation.NonNull;

import org.mariotaku.library.logansquare.extension.annotation.Mapper;
import de.vanita5.twittnuker.api.twitter.model.Activity;
import de.vanita5.twittnuker.api.twitter.model.Status;
import de.vanita5.twittnuker.api.twitter.model.User;
import de.vanita5.twittnuker.api.twitter.model.UserList;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

@Mapper(ActivityImplMapper.class)
public class ActivityImpl extends TwitterResponseImpl implements Activity {

    static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH);
    Action action;
    String rawAction;

    Date createdAt;

    User[] sources;
    User[] targetUsers;
    User[] targetObjectUsers;
    Status[] targetObjectStatuses, targetStatuses;
    UserList[] targetUserLists, targetObjectUserLists;
    long maxPosition, minPosition;
    int targetObjectsSize, targetsSize, sourcesSize;

    ActivityImpl() {
    }

    @Override
    public String getRawAction() {
        return rawAction;
    }

    @Override
    public User[] getTargetObjectUsers() {
        return targetObjectUsers;
    }

    @Override
    public int compareTo(@NonNull final Activity another) {
        final Date thisDate = getCreatedAt(), thatDate = another.getCreatedAt();
        if (thisDate == null || thatDate == null) return 0;
        return thisDate.compareTo(thatDate);
    }

    @Override
    public Action getAction() {
        return action;
    }

    @Override
    public Date getCreatedAt() {
        return createdAt;
    }

    @Override
    public long getMaxPosition() {
        return maxPosition;
    }

    @Override
    public long getMinPosition() {
        return minPosition;
    }

    @Override
    public User[] getSources() {
        return sources;
    }

    @Override
    public int getSourcesSize() {
        return sourcesSize;
    }

    @Override
    public int getTargetObjectsSize() {
        return targetObjectsSize;
    }

    @Override
    public Status[] getTargetObjectStatuses() {
        return targetObjectStatuses;
    }

    @Override
    public UserList[] getTargetObjectUserLists() {
        return targetObjectUserLists;
    }

    @Override
    public int getTargetsSize() {
        return targetsSize;
    }

    @Override
    public Status[] getTargetStatuses() {
        return targetStatuses;
    }

    @Override
    public UserList[] getTargetUserLists() {
        return targetUserLists;
    }

    @Override
    public User[] getTargetUsers() {
        return targetUsers;
    }

    @Override
    public String toString() {
        return "ActivityJSONImpl{" +
                "action=" + action +
                ", createdAt=" + createdAt +
                ", sources=" + Arrays.toString(sources) +
                ", targetUsers=" + Arrays.toString(targetUsers) +
                ", targetObjectStatuses=" + Arrays.toString(targetObjectStatuses) +
                ", targetStatuses=" + Arrays.toString(targetStatuses) +
                ", targetUserLists=" + Arrays.toString(targetUserLists) +
                ", targetObjectUserLists=" + Arrays.toString(targetObjectUserLists) +
                ", maxPosition=" + maxPosition +
                ", minPosition=" + minPosition +
                ", targetObjectsSize=" + targetObjectsSize +
                ", targetsSize=" + targetsSize +
                ", sourcesSize=" + sourcesSize +
                '}';
    }

    public static Activity fromMention(long accountId, Status status) {
        final ActivityImpl activity = new ActivityImpl();

        activity.maxPosition = activity.minPosition = status.getId();
        activity.createdAt = status.getCreatedAt();

        if (status.getInReplyToUserId() == accountId) {
            activity.action = Action.REPLY;
            activity.rawAction = "reply";
            activity.targetStatuses = new Status[]{status};

            //TODO set target statuses (in reply to status)
            activity.targetObjectStatuses = new Status[0];
        } else {
            activity.action = Action.MENTION;
            activity.rawAction = "mention";
            activity.targetObjectStatuses = new Status[]{status};

            // TODO set target users (mentioned users)
            activity.targetUsers = null;
        }
        activity.sourcesSize = 1;
        activity.sources = new User[]{status.getUser()};
        return activity;
    }
}