/*
 *          Twittnuker - Twitter client for Android
 *
 *          This program incorporates a modified version of
 *          Twidere - Twitter client for Android
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package de.vanita5.twittnuker.library.twitter.api;

import org.mariotaku.restfu.annotation.method.GET;
import org.mariotaku.restfu.annotation.method.POST;
import org.mariotaku.restfu.annotation.param.KeyValue;
import org.mariotaku.restfu.annotation.param.Param;
import org.mariotaku.restfu.annotation.param.Queries;
import org.mariotaku.restfu.annotation.param.Query;
import org.mariotaku.restfu.http.BodyType;

import de.vanita5.twittnuker.library.MicroBlogException;
import de.vanita5.twittnuker.library.twitter.model.DirectMessage;
import de.vanita5.twittnuker.library.twitter.model.Paging;
import de.vanita5.twittnuker.library.twitter.model.ResponseList;

@SuppressWarnings("RedundantThrows")
@Queries({@KeyValue(key = "full_text", valueKey = "full_text"),
        @KeyValue(key = "include_entities", valueKey = "include_entities"),
        @KeyValue(key = "include_cards", valueKey = "include_cards"),
        @KeyValue(key = "cards_platform", valueKey = "cards_platform")})
public interface DirectMessagesResources {

    @POST("/direct_messages/destroy.json")
    @BodyType(BodyType.FORM)
    DirectMessage destroyDirectMessage(@Param("id") String id) throws MicroBlogException;

    @GET("/direct_messages.json")
    ResponseList<DirectMessage> getDirectMessages(@Query Paging paging) throws MicroBlogException;

    @GET("/direct_messages/sent.json")
    ResponseList<DirectMessage> getSentDirectMessages(@Query Paging paging) throws MicroBlogException;

    @POST("/direct_messages/new.json")
    DirectMessage sendDirectMessage(@Param("user_id") String userId, @Param("text") String text)
            throws MicroBlogException;

    @POST("/direct_messages/new.json")
    DirectMessage sendDirectMessage(@Param("user_id") String userId, @Param("text") String text,
                                    @Param("media_id") String mediaId) throws MicroBlogException;

    @POST("/direct_messages/new.json")
    DirectMessage sendDirectMessageToScreenName(@Param("screen_name") String screenName, @Param("text") String text)
            throws MicroBlogException;

    @POST("/direct_messages/new.json")
    DirectMessage sendDirectMessageToScreenName(@Param("screen_name") String screenName, @Param("text") String text,
                                                @Param("media_id") String mediaId) throws MicroBlogException;

    @GET("/direct_messages/show.json")
    DirectMessage showDirectMessage(@Query("id") String id) throws MicroBlogException;
}