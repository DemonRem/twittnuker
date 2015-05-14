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

package de.vanita5.twittnuker.api.twitter.model;

import java.util.Date;

public interface Activity extends TwitterResponse, Comparable<Activity> {

    Action getAction();

    Date getCreatedAt();

    long getMaxPosition();

    long getMinPosition();

    User[] getSources();

    int getSourcesSize();

    int getTargetObjectsSize();

    Status[] getTargetObjectStatuses();

    UserList[] getTargetObjectUserLists();

    int getTargetsSize();

    Status[] getTargetStatuses();

    UserList[] getTargetUserLists();

    User[] getTargetUsers();

    enum Action {
        FAVORITE(0x1), FOLLOW(0x2), MENTION(0x3), REPLY(0x4), RETWEET(0x5), LIST_MEMBER_ADDED(0x06),
        LIST_CREATED(0x07), FAVORITED_RETWEET(0x08), RETWEETED_RETWEET(0x09);

		public final static int ACTION_FAVORITE = 0x01;
		public final static int ACTION_FOLLOW = 0x02;
		public final static int ACTION_MENTION = 0x03;
		public final static int ACTION_REPLY = 0x04;
		public final static int ACTION_RETWEET = 0x05;
		public final static int ACTION_LIST_MEMBER_ADDED = 0x06;
		public final static int ACTION_LIST_CREATED = 0x07;
        public final static int ACTION_FAVORITED_RETWEET = 0x08;
        public final static int ACTION_RETWEETED_RETWEET = 0x09;

		private final int actionId;

		private Action(final int action) {
			actionId = action;
		}

		public int getActionId() {
			return actionId;
		}

		public static Action fromString(final String string) {
			if ("favorite".equalsIgnoreCase(string)) return FAVORITE;
			if ("follow".equalsIgnoreCase(string)) return FOLLOW;
			if ("mention".equalsIgnoreCase(string)) return MENTION;
			if ("reply".equalsIgnoreCase(string)) return REPLY;
			if ("retweet".equalsIgnoreCase(string)) return RETWEET;
			if ("list_member_added".equalsIgnoreCase(string)) return LIST_MEMBER_ADDED;
			if ("list_created".equalsIgnoreCase(string)) return LIST_CREATED;
            if ("favorited_retweet".equalsIgnoreCase(string)) return FAVORITED_RETWEET;
            if ("retweeted_retweet".equalsIgnoreCase(string)) return RETWEETED_RETWEET;
			throw new IllegalArgumentException("Unknown action " + string);
		}
	}
}