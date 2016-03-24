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

package de.vanita5.twittnuker.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;

import org.apache.commons.lang3.ArrayUtils;

import de.vanita5.twittnuker.BuildConfig;
import de.vanita5.twittnuker.Constants;
import de.vanita5.twittnuker.R;
import de.vanita5.twittnuker.activity.MediaViewerActivity;
import de.vanita5.twittnuker.constant.SharedPreferenceConstants;
import de.vanita5.twittnuker.fragment.SensitiveContentWarningDialogFragment;
import de.vanita5.twittnuker.fragment.UserFragment;
import de.vanita5.twittnuker.model.ParcelableDirectMessage;
import de.vanita5.twittnuker.model.ParcelableGroup;
import de.vanita5.twittnuker.model.ParcelableMedia;
import de.vanita5.twittnuker.model.ParcelableStatus;
import de.vanita5.twittnuker.model.ParcelableUser;
import de.vanita5.twittnuker.model.ParcelableUserList;
import de.vanita5.twittnuker.model.UserKey;
import de.vanita5.twittnuker.model.util.ParcelableLocationUtils;

import java.util.ArrayList;
import java.util.List;

import static android.text.TextUtils.isEmpty;

public class IntentUtils implements Constants {
    public static String getStatusShareText(@NonNull final Context context, @NonNull final ParcelableStatus status) {
        final Uri link = LinkCreator.getStatusWebLink(status);
        return context.getString(R.string.status_share_text_format_with_link,
                status.text_plain, link.toString());
    }

    public static String getStatusShareSubject(@NonNull final Context context, @NonNull final ParcelableStatus status) {
        final String timeString = Utils.formatToLongTimeString(context, status.timestamp);
        return context.getString(R.string.status_share_subject_format_with_time,
                status.user_name, status.user_screen_name, timeString);
    }

    public static void openUserProfile(@NonNull final Context context, @NonNull final ParcelableUser user,
                                       final Bundle activityOptions, final boolean newDocument,
                                       @UserFragment.Referral final String referral) {
        final Bundle extras = new Bundle();
        extras.putParcelable(EXTRA_USER, user);
        final Uri.Builder builder = new Uri.Builder();
        builder.scheme(SCHEME_TWITTNUKER);
        builder.authority(AUTHORITY_USER);
        builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_KEY, user.account_key.toString());
        if (user.key != null) {
            builder.appendQueryParameter(QUERY_PARAM_USER_ID, user.key.toString());
        }
        if (user.screen_name != null) {
            builder.appendQueryParameter(QUERY_PARAM_SCREEN_NAME, user.screen_name);
        }
        final Intent intent = new Intent(Intent.ACTION_VIEW, builder.build());
        intent.setExtrasClassLoader(context.getClassLoader());
        intent.putExtras(extras);
        intent.putExtra(EXTRA_REFERRAL, referral);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && newDocument) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        }
        if (context instanceof Activity) {
            ActivityCompat.startActivity((Activity) context, intent, activityOptions);
        } else {
            context.startActivity(intent);
        }
    }

    public static void openUserProfile(@NonNull final Context context, @Nullable final UserKey accountKey,
                                       final UserKey userKey, final String screenName,
                                       final Bundle activityOptions, final boolean newDocument,
                                       @UserFragment.Referral final String referral) {
        if (userKey == null && isEmpty(screenName)) return;
        final Uri uri = LinkCreator.getTwidereUserLink(accountKey, userKey, screenName);
        final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.putExtra(EXTRA_REFERRAL, referral);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && newDocument) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        }
        if (context instanceof Activity) {
            ActivityCompat.startActivity((Activity) context, intent, activityOptions);
        } else {
            context.startActivity(intent);
        }
    }

    public static void openItems(@NonNull final Context context, final List<Parcelable> items) {
        if (items == null) return;
        final Bundle extras = new Bundle();
        extras.putParcelableArrayList(EXTRA_ITEMS, new ArrayList<>(items));
        final Uri.Builder builder = new Uri.Builder();
        builder.scheme(SCHEME_TWITTNUKER);
        builder.authority(AUTHORITY_ITEMS);
        final Intent intent = new Intent(Intent.ACTION_VIEW, builder.build());
        intent.putExtras(extras);
        context.startActivity(intent);
    }

    public static void openUserMentions(@NonNull final Context context, @Nullable final UserKey accountKey,
                                        @NonNull final String screenName) {
        final Uri.Builder builder = new Uri.Builder();
        builder.scheme(SCHEME_TWITTNUKER);
        builder.authority(AUTHORITY_USER_MENTIONS);
        if (accountKey != null) {
            builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_KEY, accountKey.toString());
        }
        builder.appendQueryParameter(QUERY_PARAM_SCREEN_NAME, screenName);
        final Intent intent = new Intent(Intent.ACTION_VIEW, builder.build());
        context.startActivity(intent);
    }

    public static void openMedia(@NonNull final Context context, final ParcelableDirectMessage message,
                                 final ParcelableMedia current, @Nullable final Bundle options,
                                 final boolean newDocument) {
        openMedia(context, message.account_key, false, null, message, current, message.media,
                options, newDocument);
    }

    public static void openUserTimeline(@NonNull  final Context context, @Nullable final UserKey accountKey,
                                        final String userId, final String screenName) {
        final Uri.Builder builder = new Uri.Builder();
        builder.scheme(SCHEME_TWITTNUKER);
        builder.authority(AUTHORITY_USER_TIMELINE);
        if (accountKey != null) {
            builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_KEY, accountKey.toString());
        }
        if (userId != null) {
            builder.appendQueryParameter(QUERY_PARAM_USER_ID, userId);
        }
        if (screenName != null) {
            builder.appendQueryParameter(QUERY_PARAM_SCREEN_NAME, screenName);
        }
        final Intent intent = new Intent(Intent.ACTION_VIEW, builder.build());
        context.startActivity(intent);
    }

    public static void openMedia(@NonNull final Context context, final ParcelableStatus status,
                                 final ParcelableMedia current, final Bundle options,
                                 final boolean newDocument) {
        openMedia(context, status.account_key, status.is_possibly_sensitive, status, null, current,
                getPrimaryMedia(status), options, newDocument);
    }

    public static void openMedia(@NonNull final Context context, @Nullable final UserKey accountKey, final boolean isPossiblySensitive,
                                 final ParcelableMedia current, final ParcelableMedia[] media,
                                 final Bundle options, final boolean newDocument) {
        openMedia(context, accountKey, isPossiblySensitive, null, null, current, media, options, newDocument);
    }

    public static void openMedia(@NonNull final Context context, @Nullable final UserKey accountKey, final boolean isPossiblySensitive,
                                 final ParcelableStatus status, final ParcelableDirectMessage message,
                                 final ParcelableMedia current, final ParcelableMedia[] media,
                                 final Bundle options, final boolean newDocument) {
        if (media == null) return;
        final SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        if (context instanceof FragmentActivity && isPossiblySensitive
                && !prefs.getBoolean(SharedPreferenceConstants.KEY_DISPLAY_SENSITIVE_CONTENTS, false)) {
            final FragmentActivity activity = (FragmentActivity) context;
            final FragmentManager fm = activity.getSupportFragmentManager();
            final DialogFragment fragment = new SensitiveContentWarningDialogFragment();
            final Bundle args = new Bundle();
            args.putParcelable(EXTRA_ACCOUNT_KEY, accountKey);
            args.putParcelable(EXTRA_CURRENT_MEDIA, current);
            if (status != null) {
                args.putParcelable(EXTRA_STATUS, status);
            }
            if (message != null) {
                args.putParcelable(EXTRA_MESSAGE, message);
            }
            args.putParcelableArray(EXTRA_MEDIA, media);
            args.putBundle(EXTRA_ACTIVITY_OPTIONS, options);
            args.putBundle(EXTRA_ACTIVITY_OPTIONS, options);
            args.putBoolean(EXTRA_NEW_DOCUMENT, newDocument);
            fragment.setArguments(args);
            fragment.show(fm, "sensitive_content_warning");
        } else {
            openMediaDirectly(context, accountKey, status, message, current, media, options,
                    newDocument);
        }
    }

    public static void openMediaDirectly(@NonNull final Context context, @Nullable final UserKey accountKey,
                                         final ParcelableStatus status, final ParcelableMedia current,
                                         final Bundle options, final boolean newDocument) {
        openMediaDirectly(context, accountKey, status, null, current, getPrimaryMedia(status),
                options, newDocument);
    }

    public static ParcelableMedia[] getPrimaryMedia(ParcelableStatus status) {
        if (status.is_quote && ArrayUtils.isEmpty(status.media)) {
            return status.quoted_media;
        } else {
            return status.media;
        }
    }

    public static void openMediaDirectly(@NonNull final Context context,
                                         @Nullable final UserKey accountKey,
                                         final ParcelableDirectMessage message, final ParcelableMedia current,
                                         final ParcelableMedia[] media, final Bundle options,
                                         final boolean newDocument) {
        openMediaDirectly(context, accountKey, null, message, current, media, options, newDocument);
    }

    public static void openMediaDirectly(@NonNull final Context context, @Nullable final UserKey accountKey,
                                         final ParcelableStatus status, final ParcelableDirectMessage message,
                                         final ParcelableMedia current, final ParcelableMedia[] media,
                                         final Bundle options, final boolean newDocument) {
        if (media == null) return;
        final Intent intent = new Intent(context, MediaViewerActivity.class);
        intent.putExtra(EXTRA_ACCOUNT_KEY, accountKey);
        intent.putExtra(EXTRA_CURRENT_MEDIA, current);
        intent.putExtra(EXTRA_MEDIA, media);
        if (status != null) {
            intent.putExtra(EXTRA_STATUS, status);
            intent.setData(getMediaViewerUri("status", status.id, accountKey));
        }
        if (message != null) {
            intent.putExtra(EXTRA_MESSAGE, message);
            intent.setData(getMediaViewerUri("message", String.valueOf(message.id), accountKey));
        }
        if (newDocument && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        }
        if (context instanceof Activity) {
            ActivityCompat.startActivity((Activity) context, intent, options);
        } else {
            context.startActivity(intent);
        }
    }

    public static Uri getMediaViewerUri(@NonNull final String type, final String id,
                                        @Nullable final UserKey accountKey) {
        final Uri.Builder builder = new Uri.Builder();
        builder.scheme(SCHEME_TWITTNUKER);
        builder.authority("media");
        builder.appendPath(type);
        builder.appendPath(id);
        if (accountKey != null) {
            builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_KEY, accountKey.toString());
        }
        return builder.build();
    }

    public static void openMessageConversation(@NonNull final Context context,
                                               @Nullable final UserKey accountKey,
                                               final String recipientId) {
        final Uri.Builder builder = new Uri.Builder();
        builder.scheme(SCHEME_TWITTNUKER);
        builder.authority(AUTHORITY_DIRECT_MESSAGES_CONVERSATION);
        if (accountKey != null) {
            builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_KEY, accountKey.toString());
            if (recipientId != null) {
                builder.appendQueryParameter(QUERY_PARAM_RECIPIENT_ID, recipientId);
            }
        }
        final Intent intent = new Intent(Intent.ACTION_VIEW, builder.build());
        intent.setPackage(BuildConfig.APPLICATION_ID);
        context.startActivity(intent);
    }

    public static void openIncomingFriendships(@NonNull final Context context,
                                               @Nullable final UserKey accountKey) {
        final Uri.Builder builder = new Uri.Builder();
        builder.scheme(SCHEME_TWITTNUKER);
        builder.authority(AUTHORITY_INCOMING_FRIENDSHIPS);
        if (accountKey != null) {
            builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_KEY, accountKey.toString());
        }
        final Intent intent = new Intent(Intent.ACTION_VIEW, builder.build());
        intent.setPackage(BuildConfig.APPLICATION_ID);
        context.startActivity(intent);
    }

    public static void openMap(@NonNull final Context context, final double latitude, final double longitude) {
        if (!ParcelableLocationUtils.isValidLocation(latitude, longitude)) return;
        final Uri.Builder builder = new Uri.Builder();
        builder.scheme(SCHEME_TWITTNUKER);
        builder.authority(AUTHORITY_MAP);
        builder.appendQueryParameter(QUERY_PARAM_LAT, String.valueOf(latitude));
        builder.appendQueryParameter(QUERY_PARAM_LNG, String.valueOf(longitude));
        final Intent intent = new Intent(Intent.ACTION_VIEW, builder.build());
        intent.setPackage(BuildConfig.APPLICATION_ID);
        context.startActivity(intent);
    }

    public static void openMutesUsers(@NonNull final Context context,
                                      @Nullable final UserKey accountKey) {
        final Uri.Builder builder = new Uri.Builder();
        builder.scheme(SCHEME_TWITTNUKER);
        builder.authority(AUTHORITY_MUTES_USERS);
        if (accountKey != null) {
            builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_KEY, accountKey.toString());
        }
        final Intent intent = new Intent(Intent.ACTION_VIEW, builder.build());
        intent.setPackage(BuildConfig.APPLICATION_ID);
        context.startActivity(intent);
    }

    public static void openScheduledStatuses(@NonNull final Context context,
                                             @Nullable final UserKey accountKey) {
        final Uri.Builder builder = new Uri.Builder();
        builder.scheme(SCHEME_TWITTNUKER);
        builder.authority(AUTHORITY_SCHEDULED_STATUSES);
        if (accountKey != null) {
            builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_KEY, accountKey.toString());
        }
        final Intent intent = new Intent(Intent.ACTION_VIEW, builder.build());
        intent.setPackage(BuildConfig.APPLICATION_ID);
        context.startActivity(intent);
    }

    public static void openSavedSearches(@NonNull final Context context, @Nullable final UserKey accountKey) {
        final Uri.Builder builder = new Uri.Builder();
        builder.scheme(SCHEME_TWITTNUKER);
        builder.authority(AUTHORITY_SAVED_SEARCHES);
        if (accountKey != null) {
            builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_KEY, accountKey.toString());
        }
        final Intent intent = new Intent(Intent.ACTION_VIEW, builder.build());
        intent.setPackage(BuildConfig.APPLICATION_ID);
        context.startActivity(intent);
    }

    public static void openSearch(@NonNull final Context context, @Nullable final UserKey accountKey, final String query) {
        openSearch(context, accountKey, query, null);
    }

    public static void openSearch(@NonNull final Context context, @Nullable final UserKey accountKey, final String query, String type) {
        final Intent intent = new Intent(Intent.ACTION_VIEW);
        // Some devices cannot process query parameter with hashes well, so add this intent extra
        intent.putExtra(EXTRA_QUERY, query);
        if (accountKey != null) {
            intent.putExtra(EXTRA_ACCOUNT_KEY, accountKey);
        }

        final Uri.Builder builder = new Uri.Builder();
        builder.scheme(SCHEME_TWITTNUKER);
        builder.authority(AUTHORITY_SEARCH);
        if (accountKey != null) {
            builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_KEY, accountKey.toString());
        }
        builder.appendQueryParameter(QUERY_PARAM_QUERY, query);
        if (!TextUtils.isEmpty(type)) {
            builder.appendQueryParameter(QUERY_PARAM_TYPE, type);
            intent.putExtra(EXTRA_TYPE, type);
        }
        intent.setData(builder.build());

        context.startActivity(intent);
    }

    public static void openStatus(@NonNull final Context context, @Nullable final UserKey accountKey,
                                  @NonNull final String statusId) {
        final Uri uri = LinkCreator.getTwidereStatusLink(accountKey, statusId);
        final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        context.startActivity(intent);
    }

    public static void openStatus(@NonNull final Context context, @NonNull final ParcelableStatus status, Bundle activityOptions) {
        final Bundle extras = new Bundle();
        extras.putParcelable(EXTRA_STATUS, status);
        final Uri.Builder builder = new Uri.Builder();
        builder.scheme(SCHEME_TWITTNUKER);
        builder.authority(AUTHORITY_STATUS);
        builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_KEY, status.account_key.toString());
        builder.appendQueryParameter(QUERY_PARAM_STATUS_ID, status.id);
        final Intent intent = new Intent(Intent.ACTION_VIEW, builder.build());
        intent.setExtrasClassLoader(context.getClassLoader());
        intent.putExtras(extras);
        if (context instanceof Activity) {
            ActivityCompat.startActivity((Activity) context, intent, activityOptions);
        } else {
            context.startActivity(intent);
        }
    }

    public static void openStatusFavoriters(@NonNull final Context context, @Nullable final UserKey accountKey,
                                            @NonNull final String statusId) {
        final Uri.Builder builder = new Uri.Builder();
        builder.scheme(SCHEME_TWITTNUKER);
        builder.authority(AUTHORITY_STATUS_FAVORITERS);
        if (accountKey != null) {
            builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_KEY, accountKey.toString());
        }
        builder.appendQueryParameter(QUERY_PARAM_STATUS_ID, statusId);
        final Intent intent = new Intent(Intent.ACTION_VIEW, builder.build());
        context.startActivity(intent);
    }

    public static void openStatusRetweeters(@NonNull final Context context, @Nullable final UserKey accountKey,
                                            @NonNull final String statusId) {
        final Uri.Builder builder = new Uri.Builder();
        builder.scheme(SCHEME_TWITTNUKER);
        builder.authority(AUTHORITY_STATUS_RETWEETERS);
        if (accountKey != null) {
            builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_KEY, accountKey.toString());
        }
        builder.appendQueryParameter(QUERY_PARAM_STATUS_ID, statusId);
        final Intent intent = new Intent(Intent.ACTION_VIEW, builder.build());
        context.startActivity(intent);
    }

    public static void openTweetSearch(@NonNull final Context context, @Nullable final UserKey accountKey,
                                       @NonNull final String query) {
        openSearch(context, accountKey, query, QUERY_PARAM_VALUE_TWEETS);
    }

    public static void openUserBlocks(final Activity activity, final UserKey accountKey) {
        if (activity == null) return;
        final Uri.Builder builder = new Uri.Builder();
        builder.scheme(SCHEME_TWITTNUKER);
        builder.authority(AUTHORITY_USER_BLOCKS);
        builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_KEY, accountKey.toString());
        final Intent intent = new Intent(Intent.ACTION_VIEW, builder.build());
        activity.startActivity(intent);
    }

    public static void openUserFavorites(@NonNull final Context context,
                                         @Nullable final UserKey accountKey,
                                         final String userId, final String screenName) {
        final Uri.Builder builder = new Uri.Builder();
        builder.scheme(SCHEME_TWITTNUKER);
        builder.authority(AUTHORITY_USER_FAVORITES);
        if (accountKey != null) {
            builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_KEY, accountKey.toString());
        }
        if (userId != null) {
            builder.appendQueryParameter(QUERY_PARAM_USER_ID, userId);
        }
        if (screenName != null) {
            builder.appendQueryParameter(QUERY_PARAM_SCREEN_NAME, screenName);
        }
        final Intent intent = new Intent(Intent.ACTION_VIEW, builder.build());
        context.startActivity(intent);

    }

    public static void openUserFollowers(@NonNull final Context context,
                                         @Nullable final UserKey accountKey, final String userId,
                                         final String screenName) {
        final Uri.Builder builder = new Uri.Builder();
        builder.scheme(SCHEME_TWITTNUKER);
        builder.authority(AUTHORITY_USER_FOLLOWERS);
        if (accountKey != null) {
            builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_KEY, accountKey.toString());
        }
        if (userId != null) {
            builder.appendQueryParameter(QUERY_PARAM_USER_ID, userId);
        }
        if (screenName != null) {
            builder.appendQueryParameter(QUERY_PARAM_SCREEN_NAME, screenName);
        }
        final Intent intent = new Intent(Intent.ACTION_VIEW, builder.build());
        context.startActivity(intent);
    }

    public static void openUserFriends(@NonNull final Context context,
                                       @Nullable final UserKey accountKey, final String userId,
                                       final String screenName) {
        final Uri.Builder builder = new Uri.Builder();
        builder.scheme(SCHEME_TWITTNUKER);
        builder.authority(AUTHORITY_USER_FRIENDS);
        if (accountKey != null) {
            builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_KEY, accountKey.toString());
        }
        if (userId != null) {
            builder.appendQueryParameter(QUERY_PARAM_USER_ID, userId);
        }
        if (screenName != null) {
            builder.appendQueryParameter(QUERY_PARAM_SCREEN_NAME, screenName);
        }
        final Intent intent = new Intent(Intent.ACTION_VIEW, builder.build());
        context.startActivity(intent);

    }

    public static void openUserListDetails(@NonNull final Context context,
                                           @Nullable final UserKey accountKey, final long listId,
                                           final String userId, final String screenName, final String listName) {
        final Uri.Builder builder = new Uri.Builder();
        builder.scheme(SCHEME_TWITTNUKER);
        builder.authority(AUTHORITY_USER_LIST);
        if (accountKey != null) {
            builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_KEY, accountKey.toString());
        }
        if (listId > 0) {
            builder.appendQueryParameter(QUERY_PARAM_LIST_ID, String.valueOf(listId));
        }
        if (userId != null) {
            builder.appendQueryParameter(QUERY_PARAM_USER_ID, userId);
        }
        if (screenName != null) {
            builder.appendQueryParameter(QUERY_PARAM_SCREEN_NAME, screenName);
        }
        if (listName != null) {
            builder.appendQueryParameter(QUERY_PARAM_LIST_NAME, listName);
        }
        final Intent intent = new Intent(Intent.ACTION_VIEW, builder.build());
        context.startActivity(intent);
    }

    public static void openUserListDetails(@NonNull final Context context, @NonNull final ParcelableUserList userList) {
        final UserKey userId = userList.user_key;
        final long listId = userList.id;
        final Bundle extras = new Bundle();
        extras.putParcelable(EXTRA_USER_LIST, userList);
        final Uri.Builder builder = new Uri.Builder();
        builder.scheme(SCHEME_TWITTNUKER);
        builder.authority(AUTHORITY_USER_LIST);
        builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_KEY, String.valueOf(userList.account_key));
        builder.appendQueryParameter(QUERY_PARAM_USER_ID, String.valueOf(userId));
        builder.appendQueryParameter(QUERY_PARAM_LIST_ID, String.valueOf(listId));
        final Intent intent = new Intent(Intent.ACTION_VIEW, builder.build());
        intent.setExtrasClassLoader(context.getClassLoader());
        intent.putExtras(extras);
        context.startActivity(intent);
    }

    public static void openGroupDetails(@NonNull final Context context, @NonNull final ParcelableGroup group) {
        final Bundle extras = new Bundle();
        extras.putParcelable(EXTRA_GROUP, group);
        final Uri.Builder builder = new Uri.Builder();
        builder.scheme(SCHEME_TWITTNUKER);
        builder.authority(AUTHORITY_GROUP);
        builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_KEY, group.account_key.toString());
        builder.appendQueryParameter(QUERY_PARAM_GROUP_ID, group.id);
        builder.appendQueryParameter(QUERY_PARAM_GROUP_NAME, group.nickname);
        final Intent intent = new Intent(Intent.ACTION_VIEW, builder.build());
        intent.setExtrasClassLoader(context.getClassLoader());
        intent.putExtras(extras);
        context.startActivity(intent);
    }

    public static void openUserLists(@NonNull final Context context, @Nullable final UserKey accountKey, final String userId,
                                     final String screenName) {
        final Uri.Builder builder = new Uri.Builder();
        builder.scheme(SCHEME_TWITTNUKER);
        builder.authority(AUTHORITY_USER_LISTS);
        if (accountKey != null) {
            builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_KEY, accountKey.toString());
        }
        if (userId != null) {
            builder.appendQueryParameter(QUERY_PARAM_USER_ID, userId);
        }
        if (screenName != null) {
            builder.appendQueryParameter(QUERY_PARAM_SCREEN_NAME, screenName);
        }
        final Intent intent = new Intent(Intent.ACTION_VIEW, builder.build());
        context.startActivity(intent);
    }


    public static void openUserGroups(@NonNull final Context context, @Nullable final UserKey accountKey, final String userId,
                                      final String screenName) {
        final Uri.Builder builder = new Uri.Builder();
        builder.scheme(SCHEME_TWITTNUKER);
        builder.authority(AUTHORITY_USER_GROUPS);
        if (accountKey != null) {
            builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_KEY, accountKey.toString());
        }
        if (userId != null) {
            builder.appendQueryParameter(QUERY_PARAM_USER_ID, userId);
        }
        if (screenName != null) {
            builder.appendQueryParameter(QUERY_PARAM_SCREEN_NAME, screenName);
        }
        final Intent intent = new Intent(Intent.ACTION_VIEW, builder.build());
        context.startActivity(intent);
    }

    public static void openDirectMessages(@NonNull final Context context, @Nullable final UserKey accountKey) {
        final Uri.Builder builder = new Uri.Builder();
        builder.scheme(SCHEME_TWITTNUKER);
        builder.authority(AUTHORITY_DIRECT_MESSAGES);
        if (accountKey != null) {
            builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_KEY, accountKey.toString());
        }
        final Intent intent = new Intent(Intent.ACTION_VIEW, builder.build());
        context.startActivity(intent);
    }

    public static void openInteractions(@NonNull final Context context, @Nullable final UserKey accountKey) {
        final Uri.Builder builder = new Uri.Builder();
        builder.scheme(SCHEME_TWITTNUKER);
        builder.authority(AUTHORITY_INTERACTIONS);
        if (accountKey != null) {
            builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_KEY, accountKey.toString());
        }
        final Intent intent = new Intent(Intent.ACTION_VIEW, builder.build());
        context.startActivity(intent);
    }

    public static void openPublicTimeline(@NonNull final Context context, @Nullable final UserKey accountKey) {
        final Uri.Builder builder = new Uri.Builder();
        builder.scheme(SCHEME_TWITTNUKER);
        builder.authority(AUTHORITY_PUBLIC_TIMELINE);
        if (accountKey != null) {
            builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_KEY, accountKey.toString());
        }
        final Intent intent = new Intent(Intent.ACTION_VIEW, builder.build());
        context.startActivity(intent);
    }

    public static void openUserMediaTimeline(final Activity activity, final UserKey accountKey,
                                             final long userId, final String screenName) {
        if (activity == null) return;
        final Uri.Builder builder = new Uri.Builder();
        builder.scheme(SCHEME_TWITTNUKER);
        builder.authority(AUTHORITY_USER_MEDIA_TIMELINE);
        if (accountKey != null) {
            builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_KEY, String.valueOf(accountKey));
        }
        if (userId > 0) {
            builder.appendQueryParameter(QUERY_PARAM_USER_ID, String.valueOf(userId));
        }
        if (screenName != null) {
            builder.appendQueryParameter(QUERY_PARAM_SCREEN_NAME, screenName);
        }
        final Intent intent = new Intent(Intent.ACTION_VIEW, builder.build());
        activity.startActivity(intent);
    }

    public static void openAccountsManager(Context context) {
        final Intent intent = new Intent();
        final Uri.Builder builder = new Uri.Builder();
        builder.scheme(SCHEME_TWITTNUKER);
        builder.authority(AUTHORITY_ACCOUNTS);
        intent.setData(builder.build());
        intent.setPackage(BuildConfig.APPLICATION_ID);
        context.startActivity(intent);
    }

    public static void openDrafts(Context context) {
        final Intent intent = new Intent();
        final Uri.Builder builder = new Uri.Builder();
        builder.scheme(SCHEME_TWITTNUKER);
        builder.authority(AUTHORITY_DRAFTS);
        intent.setData(builder.build());
        intent.setPackage(BuildConfig.APPLICATION_ID);
        context.startActivity(intent);
    }

    public static void openProfileEditor(Context context, @Nullable UserKey accountId) {
        final Intent intent = new Intent();
        final Uri.Builder builder = new Uri.Builder();
        builder.scheme(SCHEME_TWITTNUKER);
        builder.authority(AUTHORITY_PROFILE_EDITOR);
        if (accountId != null) {
            builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_KEY, accountId.toString());
        }
        intent.setData(builder.build());
        intent.setPackage(BuildConfig.APPLICATION_ID);
        context.startActivity(intent);
    }

    public static void openFilters(Context context) {
        final Intent intent = new Intent();
        final Uri.Builder builder = new Uri.Builder();
        builder.scheme(SCHEME_TWITTNUKER);
        builder.authority(AUTHORITY_FILTERS);
        intent.setData(builder.build());
        intent.setPackage(BuildConfig.APPLICATION_ID);
        context.startActivity(intent);
    }
}