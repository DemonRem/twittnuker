/*
 * Copyright 2007 Yusuke Yamamoto
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package twitter4j.api;

import org.mariotaku.simplerestapi.method.GET;
import org.mariotaku.simplerestapi.param.Query;

import java.io.File;
import java.io.InputStream;

import twitter4j.AccountSettings;
import twitter4j.Category;
import twitter4j.IDs;
import twitter4j.PageableResponseList;
import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.SettingsUpdate;
import twitter4j.TwitterException;
import twitter4j.User;

/**
 * @author Joern Huxhorn - jhuxhorn at googlemail.com
 */
public interface UsersResources {
	User createBlock(long userId) throws TwitterException;

	User createBlock(String screenName) throws TwitterException;

	User createMute(long userId) throws TwitterException;

	User createMute(String screenName) throws TwitterException;

	User destroyBlock(long userId) throws TwitterException;

	User destroyBlock(String screenName) throws TwitterException;

	User destroyMute(long userId) throws TwitterException;

	User destroyMute(String screenName) throws TwitterException;

	AccountSettings getAccountSettings() throws TwitterException;

    @GET("/blocks/ids.json")
	IDs getBlocksIDs() throws TwitterException;

    @GET("/blocks/ids.json")
    IDs getBlocksIDs(@Query Paging paging) throws TwitterException;

    @GET("/blocks/list.json")
    PageableResponseList<User> getBlocksList() throws TwitterException;

    @GET("/blocks/list.json")
    PageableResponseList<User> getBlocksList(@Query Paging paging) throws TwitterException;

	ResponseList<User> getMemberSuggestions(String categorySlug) throws TwitterException;

	IDs getMutesUsersIDs() throws TwitterException;

    IDs getMutesUsersIDs(Paging paging) throws TwitterException;

    PageableResponseList<User> getMutesUsersList() throws TwitterException;

    @GET("/mutes/users/list.json")
    PageableResponseList<User> getMutesUsersList(@Query Paging paging) throws TwitterException;

	ResponseList<Category> getSuggestedUserCategories() throws TwitterException;

	ResponseList<User> getUserSuggestions(String categorySlug) throws TwitterException;

	ResponseList<User> lookupUsers(long[] ids) throws TwitterException;

	ResponseList<User> lookupUsers(String[] screenNames) throws TwitterException;

	void removeProfileBannerImage() throws TwitterException;

    @GET("/users/search.json")
    ResponseList<User> searchUsers(@Query("q") String query, @Query Paging paging) throws TwitterException;

    @GET("/users/show.json")
    User showUser(@Query("user_id") long userId) throws TwitterException;

    @GET("/users/show.json")
    User showUser(@Query("screen_name") String screenName) throws TwitterException;

    AccountSettings updateAccountSettings(SettingsUpdate settingsUpdate) throws TwitterException;

	User updateProfile(String name, String url, String location, String description) throws TwitterException;

	User updateProfileBackgroundImage(File image, boolean tile) throws TwitterException;

	User updateProfileBackgroundImage(InputStream image, boolean tile) throws TwitterException;

	void updateProfileBannerImage(File banner) throws TwitterException;

	void updateProfileBannerImage(File banner, int width, int height, int offsetLeft, int offsetTop)
			throws TwitterException;

	void updateProfileBannerImage(InputStream banner) throws TwitterException;

	void updateProfileBannerImage(InputStream banner, int width, int height, int offsetLeft, int offsetTop)
			throws TwitterException;

	User updateProfileColors(String profileBackgroundColor, String profileTextColor, String profileLinkColor,
			String profileSidebarFillColor, String profileSidebarBorderColor) throws TwitterException;

	User updateProfileImage(File image) throws TwitterException;

	User updateProfileImage(InputStream image) throws TwitterException;

    @GET(" account/verify_credentials.json")
	User verifyCredentials() throws TwitterException;
}