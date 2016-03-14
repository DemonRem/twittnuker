/*
 * Twittnuker - Twitter client for Android
 *
 * Copyright (C) 2013-2016 vanita5 <mail@vanit.as>
 *
 * This program incorporates a modified version of Twidere.
 * Copyright (C) 2012-2016 Mariotaku Lee <mariotaku.lee@gmail.com>
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
import org.mariotaku.restfu.annotation.param.KeyValue;
import org.mariotaku.restfu.annotation.param.Param;
import org.mariotaku.restfu.annotation.param.Queries;
import org.mariotaku.restfu.annotation.param.Query;
import org.mariotaku.restfu.http.BodyType;
import org.mariotaku.restfu.http.mime.FileBody;
import de.vanita5.twittnuker.api.twitter.TwitterException;
import de.vanita5.twittnuker.api.twitter.model.AccountSettings;
import de.vanita5.twittnuker.api.twitter.model.Category;
import de.vanita5.twittnuker.api.twitter.model.IDs;
import de.vanita5.twittnuker.api.twitter.model.PageableResponseList;
import de.vanita5.twittnuker.api.twitter.model.Paging;
import de.vanita5.twittnuker.api.twitter.model.ProfileUpdate;
import de.vanita5.twittnuker.api.twitter.model.ResponseCode;
import de.vanita5.twittnuker.api.twitter.model.ResponseList;
import de.vanita5.twittnuker.api.twitter.model.SettingsUpdate;
import de.vanita5.twittnuker.api.twitter.model.User;

@SuppressWarnings("RedundantThrows")
@Queries({@KeyValue(key = "include_entities", valueKey = "include_entities"),
        @KeyValue(key = "include_cards", valueKey = "include_cards"),
        @KeyValue(key = "cards_platform", valueKey = "cards_platform")})
public interface UsersResources {

    @POST("/blocks/create.json")
    @BodyType(BodyType.FORM)
    User createBlock(@Param("user_id") String userId) throws TwitterException;

    @POST("/blocks/create.json")
    @BodyType(BodyType.FORM)
    User createBlockByScreenName(@Query("screen_name") String screenName) throws TwitterException;

    @POST("/mutes/users/create.json")
    @BodyType(BodyType.FORM)
    User createMute(@Param("user_id") String userId) throws TwitterException;

    @POST("/mutes/users/create.json")
    @BodyType(BodyType.FORM)
    User createMuteByScreenName(@Query("screen_name") String screenName) throws TwitterException;

    @POST("/blocks/destroy.json")
    @BodyType(BodyType.FORM)
    User destroyBlock(@Param("user_id") String userId) throws TwitterException;

    @POST("/blocks/destroy.json")
    @BodyType(BodyType.FORM)
    User destroyBlockByScreenName(@Query("screen_name") String screenName) throws TwitterException;

    @POST("/mutes/users/destroy.json")
    @BodyType(BodyType.FORM)
    User destroyMute(@Param("user_id") String userId) throws TwitterException;

    @POST("/mutes/users/destroy.json")
    @BodyType(BodyType.FORM)
    User destroyMuteByScreenName(@Query("screen_name") String screenName) throws TwitterException;

    @GET("/account/settings.json")
    AccountSettings getAccountSettings() throws TwitterException;

    @GET("/blocks/ids.json")
    IDs getBlocksIDs(@Query Paging paging) throws TwitterException;

    @GET("/blocks/list.json")
    PageableResponseList<User> getBlocksList(@Query Paging paging) throws TwitterException;

    ResponseList<User> getMemberSuggestions(String categorySlug) throws TwitterException;

    @GET("/mutes/users/ids.json")
    IDs getMutesUsersIDs(Paging paging) throws TwitterException;

    @GET("/mutes/users/list.json")
    PageableResponseList<User> getMutesUsersList(@Query Paging paging) throws TwitterException;

    ResponseList<Category> getSuggestedUserCategories() throws TwitterException;

    ResponseList<User> getUserSuggestions(String categorySlug) throws TwitterException;

    @POST("/users/lookup.json")
    @BodyType(BodyType.FORM)
    ResponseList<User> lookupUsers(@Param(value = "user_id", arrayDelimiter = ',') String[] ids) throws TwitterException;

    @GET("/users/lookup.json")
    ResponseList<User> lookupUsersByScreenName(@Param(value = "screen_name", arrayDelimiter = ',') String[] screenNames) throws TwitterException;

    @POST("/account/remove_profile_banner.json")
    @BodyType(BodyType.FORM)
    ResponseCode removeProfileBannerImage() throws TwitterException;

    @GET("/users/search.json")
    ResponseList<User> searchUsers(@Query("q") String query, @Query Paging paging) throws TwitterException;

    @GET("/users/show.json")
    User showUser(@Query("user_id") String userId) throws TwitterException;

    @GET("/users/show.json")
    User showUserByScreenName(@Query("screen_name") String screenName) throws TwitterException;

    @POST("/account/settings.json")
    @BodyType(BodyType.FORM)
    AccountSettings updateAccountSettings(@Param SettingsUpdate settingsUpdate) throws TwitterException;

    @POST("/account/update_profile.json")
    @BodyType(BodyType.FORM)
    User updateProfile(@Param ProfileUpdate profileUpdate) throws TwitterException;

    @POST("/account/update_profile_background_image.json")
    @BodyType(BodyType.MULTIPART)
    User updateProfileBackgroundImage(@Param("image") FileBody data, @Param("tile") boolean tile) throws TwitterException;

    @POST("/account/update_profile_background_image.json")
    @BodyType(BodyType.FORM)
    User updateProfileBackgroundImage(@Param("media_id") long mediaId, @Param("tile") boolean tile) throws TwitterException;

    @POST("/account/update_profile_banner.json")
    @BodyType(BodyType.MULTIPART)
    ResponseCode updateProfileBannerImage(@Param("banner") FileBody data, @Param("width") int width,
                                          @Param("height") int height, @Param("offset_left") int offsetLeft,
                                          @Param("offset_top") int offsetTop)
            throws TwitterException;

    @POST("/account/update_profile_banner.json")
    @BodyType(BodyType.MULTIPART)
    ResponseCode updateProfileBannerImage(@Param("banner") FileBody data) throws TwitterException;

    @POST("/account/update_profile_image.json")
    @BodyType(BodyType.MULTIPART)
    User updateProfileImage(@Param("image") FileBody data) throws TwitterException;

    @GET("/account/verify_credentials.json")
    User verifyCredentials() throws TwitterException;
}