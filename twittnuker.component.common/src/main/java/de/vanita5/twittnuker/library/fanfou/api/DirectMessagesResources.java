/*
 *          Twittnuker - Twitter client for Android
 *
 *  Copyright 2013-2017 vanita5 <mail@vanit.as>
 *
 *          This program incorporates a modified version of
 *          Twidere - Twitter client for Android
 *
 *  Copyright 2012-2017 Mariotaku Lee <mariotaku.lee@gmail.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package de.vanita5.twittnuker.library.fanfou.api;

import de.vanita5.twittnuker.library.MicroBlogException;
import de.vanita5.twittnuker.library.fanfou.model.Conversation;
import de.vanita5.twittnuker.library.twitter.model.DirectMessage;
import de.vanita5.twittnuker.library.twitter.model.Paging;
import de.vanita5.twittnuker.library.twitter.model.ResponseList;
import org.mariotaku.restfu.annotation.method.GET;
import org.mariotaku.restfu.annotation.method.POST;
import org.mariotaku.restfu.annotation.param.Param;
import org.mariotaku.restfu.annotation.param.Query;

@SuppressWarnings("RedundantThrows")
public interface DirectMessagesResources {

    @POST("/direct_messages/new.json")
    DirectMessage sendFanfouDirectMessage(@Param("user") String user, @Param("text") String text,
            @Param("in_reply_to_id") String inReplyToId) throws MicroBlogException;

    @POST("/direct_messages/new.json")
    DirectMessage sendFanfouDirectMessage(@Param("user") String user, @Param("text") String text)
            throws MicroBlogException;

    @GET("/direct_messages/conversation_list.json")
    ResponseList<Conversation> getConversationList(@Query Paging paging) throws MicroBlogException;
}