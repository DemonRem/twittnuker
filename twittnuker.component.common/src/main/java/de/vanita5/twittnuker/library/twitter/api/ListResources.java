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

import de.vanita5.twittnuker.library.MicroBlogException;
import de.vanita5.twittnuker.library.twitter.model.PageableResponseList;
import de.vanita5.twittnuker.library.twitter.model.Paging;
import de.vanita5.twittnuker.library.twitter.model.ResponseList;
import de.vanita5.twittnuker.library.twitter.model.Status;
import de.vanita5.twittnuker.library.twitter.model.User;
import de.vanita5.twittnuker.library.twitter.model.UserList;
import de.vanita5.twittnuker.library.twitter.model.UserListUpdate;
import de.vanita5.twittnuker.library.twitter.template.StatusAnnotationTemplate;
import de.vanita5.twittnuker.library.twitter.template.UserAnnotationTemplate;
import org.mariotaku.restfu.annotation.method.GET;
import org.mariotaku.restfu.annotation.method.POST;
import org.mariotaku.restfu.annotation.param.Param;
import org.mariotaku.restfu.annotation.param.Queries;
import org.mariotaku.restfu.annotation.param.Query;

@SuppressWarnings("RedundantThrows")
public interface ListResources {
    @POST("/lists/members/create.json")
    UserList addUserListMember(@Query("list_id") String listId, @Query("user_id") String userId) throws MicroBlogException;

    @POST("/lists/members/create.json")
    UserList addUserListMemberByScreenName(@Query("list_id") String listId, @Query("screen_name") String userScreenName) throws MicroBlogException;

    @POST("/lists/members/create_all.json")
    UserList addUserListMembers(@Param("list_id") String listId, @Param(value = "user_id", arrayDelimiter = ',') String[] userIds) throws MicroBlogException;

    @POST("/lists/members/create_all.json")
    UserList addUserListMembersByScreenName(@Param("list_id") String listId, @Param(value = "screen_name", arrayDelimiter = ',') String[] screenNames) throws MicroBlogException;

    @POST("/lists/create.json")
    UserList createUserList(@Param UserListUpdate update) throws MicroBlogException;

    @POST("/lists/subscribers/create.json")
    UserList createUserListSubscription(@Param("list_id") String listId) throws MicroBlogException;

    @POST("/lists/members/destroy.json")
    UserList deleteUserListMember(@Query("list_id") String listId, @Query("user_id") String userId) throws MicroBlogException;

    @POST("/lists/members/destroy.json")
    UserList deleteUserListMemberByScreenName(@Query("list_id") String listId, @Param("screen_name") String screenName) throws MicroBlogException;

    @POST("/lists/members/destroy_all.json")
    UserList deleteUserListMembers(@Param("list_id") String listId, @Param(value = "user_id", arrayDelimiter = ',') String[] userIds) throws MicroBlogException;

    @POST("/lists/members/destroy_all.json")
    UserList deleteUserListMembersByScreenName(@Query("list_id") String listId, @Param(value = "screen_name", arrayDelimiter = ',') String[] screenNames) throws MicroBlogException;

    @POST("/lists/destroy.json")
    UserList destroyUserList(@Param("list_id") String listId) throws MicroBlogException;

    @POST("/lists/subscribers/destroy.json")
    UserList destroyUserListSubscription(@Param("list_id") String listId) throws MicroBlogException;

    @GET("/lists/members.json")
    @Queries(template = UserAnnotationTemplate.class)
    PageableResponseList<User> getUserListMembers(@Query("list_id") String listId, @Query Paging paging) throws MicroBlogException;

    @GET("/lists/members.json")
    @Queries(template = UserAnnotationTemplate.class)
    PageableResponseList<User> getUserListMembers(@Query("slug") String slug,
                                                  @Query("owner_id") String ownerId,
                                                  @Query Paging paging)
            throws MicroBlogException;

    @GET("/lists/members.json")
    @Queries(template = UserAnnotationTemplate.class)
    PageableResponseList<User> getUserListMembersByScreenName(@Query("slug") String slug, @Query("owner_screen_name") String ownerScreenName, @Query Paging paging)
            throws MicroBlogException;

    @GET("/lists/memberships.json")
    PageableResponseList<UserList> getUserListMemberships(@Query("user_id") String listMemberId, @Query Paging paging) throws MicroBlogException;

    @GET("/lists/memberships.json")
    PageableResponseList<UserList> getUserListMemberships(@Query("user_id") String listMemberId, @Query Paging paging,
                                                          @Query("filter_to_owned_lists") boolean filterToOwnedLists) throws MicroBlogException;

    @GET("/lists/memberships.json")
    PageableResponseList<UserList> getUserListMembershipsByScreenName(@Query("screen_name") String listMemberScreenName, @Query Paging paging)
            throws MicroBlogException;

    @GET("/lists/ownerships.json")
    PageableResponseList<UserList> getUserListMembershipsByScreenName(@Query("screen_name") String listMemberScreenName, @Query Paging paging,
                                                                      boolean filterToOwnedLists) throws MicroBlogException;

    @GET("/lists/ownerships.json")
    PageableResponseList<UserList> getUserListOwnerships(@Query Paging paging) throws MicroBlogException;

    @GET("/lists/ownerships.json")
    PageableResponseList<UserList> getUserListOwnerships(@Query("user_id") String ownerId, @Query Paging paging) throws MicroBlogException;

    @GET("/lists/ownerships.json")
    PageableResponseList<UserList> getUserListOwnershipsByScreenName(@Query("screen_name") String ownerScreenName, @Query Paging paging)
            throws MicroBlogException;

    @GET("/lists/list.json")
    ResponseList<UserList> getUserLists(@Query("user_id") String userId, @Query("reverse") boolean reverse) throws MicroBlogException;

    @GET("/lists/list.json")
    ResponseList<UserList> getUserListsByScreenName(@Query("screen_name") String screenName, @Query("reverse") boolean reverse) throws MicroBlogException;

    @GET("/lists/statuses.json")
    @Queries(template = StatusAnnotationTemplate.class)
    ResponseList<Status> getUserListStatuses(@Query("list_id") String listId, @Query Paging paging) throws MicroBlogException;

    @GET("/lists/statuses.json")
    @Queries(template = StatusAnnotationTemplate.class)
    ResponseList<Status> getUserListStatuses(@Query("slug") String slug, @Query("owner_id") long ownerId, @Query Paging paging) throws MicroBlogException;

    @GET("/lists/statuses.json")
    @Queries(template = StatusAnnotationTemplate.class)
    ResponseList<Status> getUserListStatuses(@Query("slug") String slug, @Query("owner_screen_name") String ownerScreenName, @Query Paging paging)
            throws MicroBlogException;

    @GET("/lists/subscribers.json")
    @Queries(template = UserAnnotationTemplate.class)
    PageableResponseList<User> getUserListSubscribers(@Query("list_id") String listId, @Query Paging paging) throws MicroBlogException;

    @GET("/lists/subscribers.json")
    @Queries(template = UserAnnotationTemplate.class)
    PageableResponseList<User> getUserListSubscribers(@Query("list_id") String slug, @Query("owner_id") String ownerId, @Query Paging paging)
            throws MicroBlogException;

    @GET("/lists/subscribers.json")
    @Queries(template = UserAnnotationTemplate.class)
    PageableResponseList<User> getUserListSubscribersByScreenName(@Query("list_id") String slug, @Query("owner_screen_name") String ownerScreenName, @Query Paging paging)
            throws MicroBlogException;


    @GET("/lists/subscriptions.json")
    PageableResponseList<UserList> getUserListSubscriptionsByScreenName(@Query("screen_name") String listOwnerScreenName, @Query Paging paging)
            throws MicroBlogException;

    @GET("/lists/subscriptions.json")
    PageableResponseList<UserList> getUserListSubscriptions(@Query("user_id") String userId, @Query Paging paging)
            throws MicroBlogException;

    @GET("/lists/show.json")
    UserList showUserList(@Query("list_id") String listId) throws MicroBlogException;

    @GET("/lists/show.json")
    UserList showUserList(@Query("slug") String slug, @Query("owner_id") String ownerId) throws MicroBlogException;

    @GET("/lists/show.json")
    UserList showUserListByScrenName(@Query("slug") String slug, @Query("owner_screen_name") String ownerScreenName) throws MicroBlogException;

    @POST("/lists/update.json")
    UserList updateUserList(@Param("list_id") String listId, @Param UserListUpdate update) throws MicroBlogException;
}