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

import org.mariotaku.restfu.annotation.method.GET;
import org.mariotaku.restfu.annotation.param.Path;
import org.mariotaku.restfu.annotation.param.Query;
import de.vanita5.twittnuker.api.twitter.TwitterException;
import de.vanita5.twittnuker.api.twitter.model.Paging;
import de.vanita5.twittnuker.api.twitter.model.ResponseList;
import de.vanita5.twittnuker.api.twitter.model.Status;
import de.vanita5.twittnuker.api.twitter.model.StatusActivitySummary;
import de.vanita5.twittnuker.api.twitter.model.TranslationResult;

@SuppressWarnings("RedundantThrows")
public interface PrivateTweetResources extends PrivateResources {

    @GET("/statuses/{id}/activity/summary.json")
    StatusActivitySummary getStatusActivitySummary(@Path("id") long statusId) throws TwitterException;

    @GET("/statuses/{id}/activity/summary.json")
    StatusActivitySummary getStatusActivitySummary(@Path("id") long statusId, boolean includeUserEntities) throws TwitterException;

    @GET("/conversation/show.json")
    ResponseList<Status> showConversation(@Query("id") long statusId) throws TwitterException;

    @GET("/conversation/show.json")
    ResponseList<Status> showConversation(@Query("id") long statusId, @Query Paging paging) throws TwitterException;

    @GET("/translations/show.json")
    TranslationResult showTranslation(@Query("id") long statusId, @Query("dest") String dest) throws TwitterException;
}