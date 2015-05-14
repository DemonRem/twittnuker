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

package de.vanita5.twittnuker.api.twitter.api;

import org.mariotaku.simplerestapi.method.GET;
import org.mariotaku.simplerestapi.param.Query;

import de.vanita5.twittnuker.api.twitter.model.Friendship;
import de.vanita5.twittnuker.api.twitter.model.IDs;
import de.vanita5.twittnuker.api.twitter.model.PageableResponseList;
import de.vanita5.twittnuker.api.twitter.model.Paging;
import de.vanita5.twittnuker.api.twitter.model.Relationship;
import de.vanita5.twittnuker.api.twitter.model.ResponseList;
import de.vanita5.twittnuker.api.twitter.TwitterException;
import de.vanita5.twittnuker.api.twitter.model.User;

/**
 * @author Joern Huxhorn - jhuxhorn at googlemail.com
 */
@SuppressWarnings("RedundantThrows")
public interface FriendsFollowersResources {

	User createFriendship(long userId) throws TwitterException;

	User createFriendship(long userId, boolean follow) throws TwitterException;

	User createFriendship(String screenName) throws TwitterException;

	User createFriendship(String screenName, boolean follow) throws TwitterException;

	User destroyFriendship(long userId) throws TwitterException;

	User destroyFriendship(String screenName) throws TwitterException;

	IDs getFollowersIDs(Paging paging) throws TwitterException;

	IDs getFollowersIDs(long userId, Paging paging) throws TwitterException;

	IDs getFollowersIDs(String screenName, Paging paging) throws TwitterException;

    @GET("/followers/list.json")
    PageableResponseList<User> getFollowersList(@Query Paging paging) throws TwitterException;

    @GET("/followers/list.json")
    PageableResponseList<User> getFollowersList(@Query("user_id") long userId, @Query Paging paging) throws TwitterException;

    @GET("/followers/list.json")
    PageableResponseList<User> getFollowersList(@Query("screen_name") String screenName, @Query Paging paging) throws TwitterException;

	IDs getFriendsIDs(Paging paging) throws TwitterException;

	IDs getFriendsIDs(long userId, Paging paging) throws TwitterException;

	IDs getFriendsIDs(String screenName, Paging paging) throws TwitterException;

	PageableResponseList<User> getFriendsList(Paging paging) throws TwitterException;

	PageableResponseList<User> getFriendsList(long userId, Paging paging) throws TwitterException;

	PageableResponseList<User> getFriendsList(String screenName, Paging paging) throws TwitterException;

	IDs getIncomingFriendships(Paging paging) throws TwitterException;

	IDs getOutgoingFriendships(Paging paging) throws TwitterException;

	ResponseList<Friendship> lookupFriendships(long[] ids) throws TwitterException;

	ResponseList<Friendship> lookupFriendships(String[] screenNames) throws TwitterException;

    @GET("/friendships/show.json")
    Relationship showFriendship(@Query("source_id") long sourceId, @Query("target_id") long targetId) throws TwitterException;

    @GET("/friendships/show.json")
    Relationship showFriendship(@Query("target_id") long targetId) throws TwitterException;

	Relationship showFriendship(String sourceScreenName, String targetScreenName) throws TwitterException;

	Relationship updateFriendship(long userId, boolean enableDeviceNotification, boolean retweets)
			throws TwitterException;

	Relationship updateFriendship(String screenName, boolean enableDeviceNotification, boolean retweets)
			throws TwitterException;
}