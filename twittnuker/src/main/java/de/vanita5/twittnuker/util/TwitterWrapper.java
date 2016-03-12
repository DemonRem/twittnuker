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

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.util.LongSparseArray;
import android.support.v4.util.SimpleArrayMap;
import android.util.Log;

import org.mariotaku.restfu.http.mime.FileBody;
import de.vanita5.twittnuker.Constants;
import de.vanita5.twittnuker.api.twitter.Twitter;
import de.vanita5.twittnuker.api.twitter.TwitterException;
import de.vanita5.twittnuker.api.twitter.model.DirectMessage;
import de.vanita5.twittnuker.api.twitter.model.Paging;
import de.vanita5.twittnuker.api.twitter.model.ResponseList;
import de.vanita5.twittnuker.api.twitter.model.Status;
import de.vanita5.twittnuker.api.twitter.model.User;
import de.vanita5.twittnuker.model.AccountKey;
import de.vanita5.twittnuker.model.ListResponse;
import de.vanita5.twittnuker.model.SingleResponse;
import de.vanita5.twittnuker.provider.TwidereDataStore.Notifications;
import de.vanita5.twittnuker.provider.TwidereDataStore.UnreadCounts;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

public class TwitterWrapper implements Constants {

    public static int clearNotification(final Context context, final int notificationType, final AccountKey accountId) {
        final Uri.Builder builder = Notifications.CONTENT_URI.buildUpon();
        builder.appendPath(String.valueOf(notificationType));
        if (accountId != null) {
            builder.appendPath(String.valueOf(accountId));
        }
        return context.getContentResolver().delete(builder.build(), null, null);
    }

    public static int clearUnreadCount(final Context context, final int position) {
        if (context == null || position < 0) return 0;
        final Uri uri = UnreadCounts.CONTENT_URI.buildUpon().appendPath(String.valueOf(position)).build();
        return context.getContentResolver().delete(uri, null, null);
    }

    public static SingleResponse<Boolean> deleteProfileBannerImage(final Context context,
                                                                   final AccountKey accountKey) {
        final Twitter twitter = TwitterAPIFactory.getTwitterInstance(context, accountKey, false);
        if (twitter == null) return new SingleResponse<>(false, null);
        try {
            twitter.removeProfileBannerImage();
            return new SingleResponse<>(true, null);
        } catch (final TwitterException e) {
            return new SingleResponse<>(false, e);
        }
    }

    public static int removeUnreadCounts(final Context context, final int position, final long account_id,
                                         final long... status_ids) {
        if (context == null || position < 0 || status_ids == null || status_ids.length == 0)
            return 0;
        int result = 0;
        final Uri.Builder builder = UnreadCounts.CONTENT_URI.buildUpon();
        builder.appendPath(String.valueOf(position));
        builder.appendPath(String.valueOf(account_id));
        builder.appendPath(TwidereArrayUtils.toString(status_ids, ',', false));
        result += context.getContentResolver().delete(builder.build(), null, null);
        return result;
    }

    public static int removeUnreadCounts(final Context context, final int position,
                                         final SimpleArrayMap<AccountKey, Set<Long>> counts) {
        if (context == null || position < 0 || counts == null) return 0;
        int result = 0;
        for (int i = 0, j = counts.size(); i < j; i++) {
            final AccountKey key = counts.keyAt(i);
            final Set<Long> value = counts.valueAt(i);
            final Uri.Builder builder = UnreadCounts.CONTENT_URI.buildUpon();
            builder.appendPath(String.valueOf(position));
            builder.appendPath(String.valueOf(key));
            builder.appendPath(CollectionUtils.toString(value, ',', false));
            result += context.getContentResolver().delete(builder.build(), null, null);
        }
        return result;
    }

    @NonNull
    public static User showUser(final Twitter twitter, final long id, final String screenName) throws TwitterException {
        if (id != -1) {
            return twitter.showUser(id);
        } else if (screenName != null) {
            return twitter.showUser(screenName);
        }
        throw new TwitterException("Invalid user id or screen name");
    }

    @NonNull
    public static User showUserAlternative(final Twitter twitter, final long id, final String screenName)
            throws TwitterException {
        final String searchScreenName;
        if (screenName != null) {
            searchScreenName = screenName;
        } else if (id != -1) {
            searchScreenName = twitter.showFriendship(id).getTargetUserScreenName();
        } else
            throw new IllegalArgumentException();
        final Paging paging = new Paging();
        paging.count(1);
        for (final User user : twitter.searchUsers(searchScreenName, paging)) {
            if (user.getId() == id || searchScreenName.equalsIgnoreCase(user.getScreenName()))
                return user;
        }
        if (id != -1) {
            final ResponseList<Status> timeline = twitter.getUserTimeline(id, paging);
            for (final Status status : timeline) {
                final User user = status.getUser();
                if (user.getId() == id) return user;
            }
        } else {
            final ResponseList<Status> timeline = twitter.getUserTimeline(screenName, paging);
            for (final Status status : timeline) {
                final User user = status.getUser();
                if (searchScreenName.equalsIgnoreCase(user.getScreenName()))
                return user;
            }
        }
        throw new TwitterException("can't find user");
    }

    @NonNull
    public static User tryShowUser(final Twitter twitter, final long id, final String screenName)
            throws TwitterException {
        try {
            return showUser(twitter, id, screenName);
        } catch (final TwitterException e) {
            // Twitter specific error for private API calling through proxy
            if (e.getStatusCode() == 200) {
                return showUserAlternative(twitter, id, screenName);
            }
            throw e;
        }
    }

    public static void updateProfileBannerImage(final Context context, final Twitter twitter,
                                                final Uri imageUri, final boolean deleteImage)
            throws FileNotFoundException, TwitterException {
        InputStream is = null;
        try {
            is = context.getContentResolver().openInputStream(imageUri);
            twitter.updateProfileBannerImage(new FileBody(is, "image", -1, null));
        } finally {
            Utils.closeSilently(is);
            if (deleteImage && "file".equals(imageUri.getScheme())) {
                final File file = new File(imageUri.getPath());
                if (!file.delete()) {
                    Log.w(LOGTAG, String.format("Unable to delete %s", file));
                }
            }
        }
    }

    public static User updateProfileImage(final Context context, final Twitter twitter,
                                          final Uri imageUri, final boolean deleteImage)
            throws FileNotFoundException, TwitterException {
        InputStream is = null;
        try {
            is = context.getContentResolver().openInputStream(imageUri);
            return twitter.updateProfileImage(new FileBody(is, "image", -1, null));
        } finally {
            Utils.closeSilently(is);
            if (deleteImage && "file".equals(imageUri.getScheme())) {
                final File file = new File(imageUri.getPath());
                if (!file.delete()) {
                    Log.w(LOGTAG, String.format("Unable to delete %s", file));
                }
            }
        }
    }

    public static final class MessageListResponse extends TwitterListResponse<DirectMessage> {

        public final boolean truncated;

        public MessageListResponse(final AccountKey accountKey, final Exception exception) {
            this(accountKey, -1, -1, null, false, exception);
        }

        public MessageListResponse(final AccountKey accountKey, final List<DirectMessage> list) {
            this(accountKey, -1, -1, list, false, null);
        }

        public MessageListResponse(final AccountKey accountKey, final long maxId, final long sinceId,
                                   final List<DirectMessage> list, final boolean truncated) {
            this(accountKey, maxId, sinceId, list, truncated, null);
        }

        MessageListResponse(final AccountKey accountKey, final long maxId, final long sinceId,
                            final List<DirectMessage> list, final boolean truncated, final Exception exception) {
            super(accountKey, maxId, sinceId, list, exception);
            this.truncated = truncated;
        }

    }

    public static final class StatusListResponse extends TwitterListResponse<Status> {

        public final boolean truncated;

        public StatusListResponse(final AccountKey accountKey, final Exception exception) {
            this(accountKey, -1, -1, null, false, exception);
        }

        public StatusListResponse(final AccountKey accountKey, final List<Status> list) {
            this(accountKey, -1, -1, list, false, null);
        }

        public StatusListResponse(final AccountKey accountKey, final long maxId, final long sinceId,
                                  final List<Status> list, final boolean truncated) {
            this(accountKey, maxId, sinceId, list, truncated, null);
        }

        StatusListResponse(final AccountKey accountKey, final long maxId, final long sinceId, final List<Status> list,
                           final boolean truncated, final Exception exception) {
            super(accountKey, maxId, sinceId, list, exception);
            this.truncated = truncated;
        }

    }

    public static class TwitterListResponse<Data> extends ListResponse<Data> {

        public final AccountKey mAccountKey;
        public final long maxId;
        public final long sinceId;

        public TwitterListResponse(final AccountKey accountKey,
                                   final Exception exception) {
            this(accountKey, -1, -1, null, exception);
        }

        public TwitterListResponse(final AccountKey accountKey, final long maxId,
                                   final long sinceId, final List<Data> list) {
            this(accountKey, maxId, sinceId, list, null);
        }

        TwitterListResponse(final AccountKey accountKey, final long maxId,
                            final long sinceId, final List<Data> list, final Exception exception) {
            super(list, exception);
            this.mAccountKey = accountKey;
            this.maxId = maxId;
            this.sinceId = sinceId;
        }

    }
}