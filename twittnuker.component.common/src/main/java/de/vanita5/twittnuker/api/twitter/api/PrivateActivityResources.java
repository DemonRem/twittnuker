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

package de.vanita5.twittnuker.api.twitter.api;

import org.mariotaku.restfu.annotation.method.GET;
import org.mariotaku.restfu.annotation.method.POST;
import org.mariotaku.restfu.annotation.param.MethodExtra;
import org.mariotaku.restfu.annotation.param.Query;
import de.vanita5.twittnuker.api.twitter.TwitterException;
import de.vanita5.twittnuker.api.twitter.model.Activity;
import de.vanita5.twittnuker.api.twitter.model.CursorTimestampResponse;
import de.vanita5.twittnuker.api.twitter.model.Paging;
import de.vanita5.twittnuker.api.twitter.model.ResponseList;

@SuppressWarnings("RedundantThrows")
@MethodExtra(name = "extra_params", values = {"include_my_retweet", "include_rts", "include_entities",
        "include_cards", "cards_platform", "include_reply_count", "include_descendent_reply_count",
        "model_version"})
public interface PrivateActivityResources extends PrivateResources {

    @GET("/activity/about_me.json")
    ResponseList<Activity> getActivitiesAboutMe(@Query Paging paging) throws TwitterException;

    @GET("/activity/about_me/unread.json")
    CursorTimestampResponse getActivitiesAboutMeUnread(@Query("cursor") boolean cursor) throws TwitterException;

    @POST("/activity/about_me/unread.json")
    CursorTimestampResponse setActivitiesAboutMeUnread(@Query("cursor") long cursor) throws TwitterException;


    @GET("/activity/by_friends.json")
    ResponseList<Activity> getActivitiesByFriends(@Query Paging paging) throws TwitterException;


}