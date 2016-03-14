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

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.NfcAdapter.CreateNdefMessageCallback;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.ListFragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.net.ConnectivityManagerCompat;
import android.support.v4.util.Pair;
import android.support.v4.view.ActionProvider;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.accessibility.AccessibilityEventCompat;
import android.support.v7.app.AppCompatActivity;
import android.system.ErrnoException;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.text.style.CharacterStyle;
import android.text.style.StyleSpan;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.webkit.MimeTypeMap;
import android.widget.AbsListView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.json.JSONException;
import org.mariotaku.sqliteqb.library.AllColumns;
import org.mariotaku.sqliteqb.library.Columns;
import org.mariotaku.sqliteqb.library.Columns.Column;
import org.mariotaku.sqliteqb.library.Expression;
import org.mariotaku.sqliteqb.library.SQLFunctions;
import org.mariotaku.sqliteqb.library.Selectable;

import de.vanita5.twittnuker.BuildConfig;
import de.vanita5.twittnuker.Constants;
import de.vanita5.twittnuker.R;
import de.vanita5.twittnuker.activity.CopyLinkActivity;
import de.vanita5.twittnuker.adapter.iface.IBaseAdapter;
import de.vanita5.twittnuker.adapter.iface.IBaseCardAdapter;
import de.vanita5.twittnuker.annotation.CustomTabType;
import de.vanita5.twittnuker.annotation.ReadPositionTag;
import de.vanita5.twittnuker.api.twitter.Twitter;
import de.vanita5.twittnuker.api.twitter.TwitterException;
import de.vanita5.twittnuker.api.twitter.model.DirectMessage;
import de.vanita5.twittnuker.api.twitter.model.GeoLocation;
import de.vanita5.twittnuker.api.twitter.model.RateLimitStatus;
import de.vanita5.twittnuker.api.twitter.model.Relationship;
import de.vanita5.twittnuker.api.twitter.model.Status;
import de.vanita5.twittnuker.api.twitter.model.UrlEntity;
import de.vanita5.twittnuker.fragment.iface.IBaseFragment.SystemWindowsInsetsCallback;
import de.vanita5.twittnuker.fragment.support.AbsStatusesFragment;
import de.vanita5.twittnuker.fragment.support.AccountsManagerFragment;
import de.vanita5.twittnuker.fragment.support.DirectMessagesFragment;
import de.vanita5.twittnuker.fragment.support.DraftsFragment;
import de.vanita5.twittnuker.fragment.support.FiltersFragment;
import de.vanita5.twittnuker.fragment.support.IncomingFriendshipsFragment;
import de.vanita5.twittnuker.fragment.support.InteractionsTimelineFragment;
import de.vanita5.twittnuker.fragment.support.ListsFragment;
import de.vanita5.twittnuker.fragment.support.MessagesConversationFragment;
import de.vanita5.twittnuker.fragment.support.MutesUsersListFragment;
import de.vanita5.twittnuker.fragment.support.SavedSearchesListFragment;
import de.vanita5.twittnuker.fragment.support.ScheduledStatusesFragment;
import de.vanita5.twittnuker.fragment.support.SearchFragment;
import de.vanita5.twittnuker.fragment.support.StatusFavoritersListFragment;
import de.vanita5.twittnuker.fragment.support.StatusFragment;
import de.vanita5.twittnuker.fragment.support.StatusRetweetersListFragment;
import de.vanita5.twittnuker.fragment.support.StatusesListFragment;
import de.vanita5.twittnuker.fragment.support.UserBlocksListFragment;
import de.vanita5.twittnuker.fragment.support.UserFavoritesFragment;
import de.vanita5.twittnuker.fragment.support.UserFollowersFragment;
import de.vanita5.twittnuker.fragment.support.UserFragment;
import de.vanita5.twittnuker.fragment.support.UserFriendsFragment;
import de.vanita5.twittnuker.fragment.support.UserListFragment;
import de.vanita5.twittnuker.fragment.support.UserListMembersFragment;
import de.vanita5.twittnuker.fragment.support.UserListMembershipsFragment;
import de.vanita5.twittnuker.fragment.support.UserListSubscribersFragment;
import de.vanita5.twittnuker.fragment.support.UserListTimelineFragment;
import de.vanita5.twittnuker.fragment.support.UserMediaTimelineFragment;
import de.vanita5.twittnuker.fragment.support.UserMentionsFragment;
import de.vanita5.twittnuker.fragment.support.UserProfileEditorFragment;
import de.vanita5.twittnuker.fragment.support.UserTimelineFragment;
import de.vanita5.twittnuker.fragment.support.UsersListFragment;
import de.vanita5.twittnuker.menu.support.FavoriteItemProvider;
import de.vanita5.twittnuker.model.UserKey;
import de.vanita5.twittnuker.model.AccountPreferences;
import de.vanita5.twittnuker.model.ParcelableAccount;
import de.vanita5.twittnuker.model.ParcelableCredentials;
import de.vanita5.twittnuker.model.ParcelableCredentialsCursorIndices;
import de.vanita5.twittnuker.model.ParcelableDirectMessage;
import de.vanita5.twittnuker.model.ParcelableDirectMessageCursorIndices;
import de.vanita5.twittnuker.model.ParcelableStatus;
import de.vanita5.twittnuker.model.ParcelableStatusCursorIndices;
import de.vanita5.twittnuker.model.ParcelableUser;
import de.vanita5.twittnuker.model.ParcelableUserMention;
import de.vanita5.twittnuker.model.PebbleMessage;
import de.vanita5.twittnuker.model.TwitterAccountExtra;
import de.vanita5.twittnuker.model.util.ParcelableCredentialsUtils;
import de.vanita5.twittnuker.model.util.ParcelableStatusUtils;
import de.vanita5.twittnuker.model.util.ParcelableUserUtils;
import de.vanita5.twittnuker.model.util.UserKeyUtils;
import de.vanita5.twittnuker.provider.TwidereDataStore;
import de.vanita5.twittnuker.provider.TwidereDataStore.AccountSupportColumns;
import de.vanita5.twittnuker.provider.TwidereDataStore.Accounts;
import de.vanita5.twittnuker.provider.TwidereDataStore.CachedRelationships;
import de.vanita5.twittnuker.provider.TwidereDataStore.CachedStatuses;
import de.vanita5.twittnuker.provider.TwidereDataStore.CachedUsers;
import de.vanita5.twittnuker.provider.TwidereDataStore.DirectMessages;
import de.vanita5.twittnuker.provider.TwidereDataStore.DirectMessages.ConversationEntries;
import de.vanita5.twittnuker.provider.TwidereDataStore.Statuses;
import de.vanita5.twittnuker.service.RefreshService;
import de.vanita5.twittnuker.util.TwidereLinkify.HighlightStyle;
import de.vanita5.twittnuker.view.CardMediaContainer.PreviewStyle;
import de.vanita5.twittnuker.view.ShapedImageView;
import de.vanita5.twittnuker.view.ShapedImageView.ShapeStyle;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;

import javax.net.ssl.SSLException;

import static android.text.TextUtils.isEmpty;
import static android.text.format.DateUtils.getRelativeTimeSpanString;
import static de.vanita5.twittnuker.provider.TwidereDataStore.DIRECT_MESSAGES_URIS;
import static de.vanita5.twittnuker.provider.TwidereDataStore.STATUSES_URIS;
import static de.vanita5.twittnuker.util.TwidereLinkify.PATTERN_TWITTER_PROFILE_IMAGES;
import static de.vanita5.twittnuker.util.TwidereLinkify.TWITTER_PROFILE_IMAGES_AVAILABLE_SIZES;

@SuppressWarnings("unused")
public final class Utils implements Constants {

    public static final Pattern PATTERN_XML_RESOURCE_IDENTIFIER = Pattern.compile("res/xml/([\\w_]+)\\.xml");
    public static final Pattern PATTERN_RESOURCE_IDENTIFIER = Pattern.compile("@([\\w_]+)/([\\w_]+)");

    private static final UriMatcher LINK_HANDLER_URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
    private static final UriMatcher HOME_TABS_URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

    static {

        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_STATUS, null, LINK_ID_STATUS);
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_USER, null, LINK_ID_USER);
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_USER_TIMELINE, null, LINK_ID_USER_TIMELINE);
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_USER_MEDIA_TIMELINE, null, LINK_ID_USER_MEDIA_TIMELINE);
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_USER_FOLLOWERS, null, LINK_ID_USER_FOLLOWERS);
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_USER_FRIENDS, null, LINK_ID_USER_FRIENDS);
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_USER_FAVORITES, null, LINK_ID_USER_FAVORITES);
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_USER_BLOCKS, null, LINK_ID_USER_BLOCKS);
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_DIRECT_MESSAGES_CONVERSATION, null,
                LINK_ID_DIRECT_MESSAGES_CONVERSATION);
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_DIRECT_MESSAGES, null, LINK_ID_DIRECT_MESSAGES);
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_INTERACTIONS, null, LINK_ID_INTERACTIONS);
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_USER_LIST, null, LINK_ID_USER_LIST);
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_USER_LIST_TIMELINE, null, LINK_ID_USER_LIST_TIMELINE);
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_USER_LIST_MEMBERS, null, LINK_ID_USER_LIST_MEMBERS);
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_USER_LIST_SUBSCRIBERS, null, LINK_ID_USER_LIST_SUBSCRIBERS);
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_USER_LIST_MEMBERSHIPS, null, LINK_ID_USER_LIST_MEMBERSHIPS);
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_USER_LISTS, null, LINK_ID_USER_LISTS);
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_SAVED_SEARCHES, null, LINK_ID_SAVED_SEARCHES);
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_USER_MENTIONS, null, LINK_ID_USER_MENTIONS);
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_INCOMING_FRIENDSHIPS, null, LINK_ID_INCOMING_FRIENDSHIPS);
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_USERS, null, LINK_ID_USERS);
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_STATUSES, null, LINK_ID_STATUSES);
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_STATUS_RETWEETERS, null, LINK_ID_STATUS_RETWEETERS);
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_STATUS_FAVORITERS, null, LINK_ID_STATUS_FAVORITERS);
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_SEARCH, null, LINK_ID_SEARCH);
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_MUTES_USERS, null, LINK_ID_MUTES_USERS);
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_MAP, null, LINK_ID_MAP);
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_SCHEDULED_STATUSES, null, LINK_ID_SCHEDULED_STATUSES);

        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_ACCOUNTS, null, LINK_ID_ACCOUNTS);
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_DRAFTS, null, LINK_ID_DRAFTS);
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_FILTERS, null, LINK_ID_FILTERS);
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_PROFILE_EDITOR, null, LINK_ID_PROFILE_EDITOR);

        HOME_TABS_URI_MATCHER.addURI(CustomTabType.HOME_TIMELINE, null, TAB_CODE_HOME_TIMELINE);
        HOME_TABS_URI_MATCHER.addURI(CustomTabType.NOTIFICATIONS_TIMELINE, null, TAB_CODE_NOTIFICATIONS_TIMELINE);
        HOME_TABS_URI_MATCHER.addURI(CustomTabType.DIRECT_MESSAGES, null, TAB_CODE_DIRECT_MESSAGES);
    }


    private Utils() {
        throw new AssertionError("You are trying to create an instance for this utility class!");
    }

    public static void announceForAccessibilityCompat(final Context context, final View view, final CharSequence text,
                                                      final Class<?> cls) {
        final AccessibilityManager accessibilityManager = (AccessibilityManager) context
                .getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (!accessibilityManager.isEnabled()) return;
        // Prior to SDK 16, announcements could only be made through FOCUSED
        // events. Jelly Bean (SDK 16) added support for speaking text verbatim
        // using the ANNOUNCEMENT event type.
        final int eventType;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            eventType = AccessibilityEvent.TYPE_VIEW_FOCUSED;
        } else {
            eventType = AccessibilityEventCompat.TYPE_ANNOUNCEMENT;
        }

        // Construct an accessibility event with the minimum recommended
        // attributes. An event without a class name or package may be dropped.
        final AccessibilityEvent event = AccessibilityEvent.obtain(eventType);
        event.getText().add(text);
        event.setClassName(cls.getName());
        event.setPackageName(context.getPackageName());
        event.setSource(view);

        // Sends the event directly through the accessibility manager. If your
        // application only targets SDK 14+, you should just call
        // getParent().requestSendAccessibilityEvent(this, event);
        accessibilityManager.sendAccessibilityEvent(event);
    }

    public static Uri buildDirectMessageConversationUri(final UserKey accountKey, final String conversationId,
                                                        final String screenName) {
        if (conversationId == null && screenName == null) return TwidereDataStore.CONTENT_URI_NULL;
        final Uri.Builder builder;
        if (conversationId != null) {
            builder = DirectMessages.Conversation.CONTENT_URI.buildUpon();
        } else {
            builder = DirectMessages.Conversation.CONTENT_URI_SCREEN_NAME.buildUpon();
        }
        builder.appendPath(String.valueOf(accountKey));
        if (conversationId != null) {
            builder.appendPath(String.valueOf(conversationId));
        } else {
            builder.appendPath(screenName);
        }
        return builder.build();
    }

    public static int calculateInSampleSize(final int width, final int height, final int preferredWidth,
                                            final int preferredHeight) {
        if (preferredHeight > height && preferredWidth > width) return 1;
        final int result = Math.round(Math.max(width, height) / (float) Math.max(preferredWidth, preferredHeight));
        return Math.max(1, result);
    }

    public static boolean checkActivityValidity(final Context context, final Intent intent) {
        final PackageManager pm = context.getPackageManager();
        return !pm.queryIntentActivities(intent, 0).isEmpty();
    }

    public static void clearListViewChoices(final AbsListView view) {
        if (view == null) return;
        final ListAdapter adapter = view.getAdapter();
        if (adapter == null) return;
        view.clearChoices();
        for (int i = 0, j = view.getChildCount(); i < j; i++) {
            view.setItemChecked(i, false);
        }
        view.post(new Runnable() {
            @Override
            public void run() {
                view.setChoiceMode(AbsListView.CHOICE_MODE_NONE);
            }
        });
        // Workaround for Android bug
        // http://stackoverflow.com/questions/9754170/listview-selection-remains-persistent-after-exiting-choice-mode
//        final int position = view.getFirstVisiblePosition(), offset = Utils.getFirstChildOffset(view);
//        view.setAdapter(adapter);
//        Utils.scrollListToPosition(view, position, offset);
    }

    public static boolean closeSilently(final Closeable c) {
        if (c == null) return false;
        try {
            c.close();
        } catch (final IOException e) {
            return false;
        }
        return true;
    }

    public static boolean closeSilently(final Cursor c) {
        if (c == null) return false;
        c.close();
        return true;
    }

    public static void configBaseAdapter(final Context context, final IBaseAdapter adapter) {
        if (context == null) return;
        final SharedPreferences pref = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        adapter.setDisplayProfileImage(pref.getBoolean(KEY_DISPLAY_PROFILE_IMAGE, true));
        adapter.setDisplayNameFirst(pref.getBoolean(KEY_NAME_FIRST, true));
        adapter.setLinkHighlightOption(pref.getString(KEY_LINK_HIGHLIGHT_OPTION, VALUE_LINK_HIGHLIGHT_OPTION_HIGHLIGHT));
        adapter.setTextSize(pref.getInt(KEY_TEXT_SIZE, getDefaultTextSize(context)));
        adapter.notifyDataSetChanged();
    }

    public static void configBaseCardAdapter(final Context context, final IBaseCardAdapter adapter) {
        if (context == null) return;
        configBaseAdapter(context, adapter);
        adapter.notifyDataSetChanged();
    }

    public static int[] getAccountColors(@Nullable final ParcelableAccount[] accounts) {
        if (accounts == null) return null;
        final int[] colors = new int[accounts.length];
        for (int i = 0, j = accounts.length; i < j; i++) {
            colors[i] = accounts[i].color;
        }
        return colors;
    }

    public static Fragment createFragmentForIntent(final Context context, final Intent intent) {
        final Uri uri = intent.getData();
        return createFragmentForIntent(context, matchLinkId(uri), intent);
    }

    public static Fragment createFragmentForIntent(final Context context, final int linkId, final Intent intent) {
        intent.setExtrasClassLoader(context.getClassLoader());
        final Bundle extras = intent.getExtras();
        final Uri uri = intent.getData();
        final Fragment fragment;
        if (uri == null) return null;
        final Bundle args = new Bundle();
        if (extras != null) {
            args.putAll(extras);
        }
        boolean isAccountIdRequired = true;
        switch (linkId) {
            case LINK_ID_ACCOUNTS: {
                fragment = new AccountsManagerFragment();
                break;
            }
            case LINK_ID_DRAFTS: {
                fragment = new DraftsFragment();
                break;
            }
            case LINK_ID_FILTERS: {
                fragment = new FiltersFragment();
                break;
            }
            case LINK_ID_PROFILE_EDITOR: {
                fragment = new UserProfileEditorFragment();
                break;
            }
            case LINK_ID_MAP: {
                if (!args.containsKey(EXTRA_LATITUDE) && !args.containsKey(EXTRA_LONGITUDE)) {
                    final double lat = NumberUtils.toDouble(uri.getQueryParameter(QUERY_PARAM_LAT), Double.NaN);
                    final double lng = NumberUtils.toDouble(uri.getQueryParameter(QUERY_PARAM_LNG), Double.NaN);
                    if (Double.isNaN(lat) || Double.isNaN(lng)) return null;
                    args.putDouble(EXTRA_LATITUDE, lat);
                    args.putDouble(EXTRA_LONGITUDE, lng);
                }
                fragment = MapFragmentFactory.SINGLETON.createMapFragment(context);
                break;
            }
            case LINK_ID_STATUS: {
                fragment = new StatusFragment();
                if (!args.containsKey(EXTRA_STATUS_ID)) {
                    final String param_status_id = uri.getQueryParameter(QUERY_PARAM_STATUS_ID);
                    args.putLong(EXTRA_STATUS_ID, NumberUtils.toLong(param_status_id, -1));
                }
                break;
            }
            case LINK_ID_USER: {
                fragment = new UserFragment();
                final String paramScreenName = uri.getQueryParameter(QUERY_PARAM_SCREEN_NAME);
                final String param_user_id = uri.getQueryParameter(QUERY_PARAM_USER_ID);
                if (!args.containsKey(EXTRA_SCREEN_NAME)) {
                    args.putString(EXTRA_SCREEN_NAME, paramScreenName);
                }
                if (!args.containsKey(EXTRA_USER_ID)) {
                    args.putLong(EXTRA_USER_ID, NumberUtils.toLong(param_user_id, -1));
                }
                args.putString(EXTRA_REFERRAL, intent.getStringExtra(EXTRA_REFERRAL));
                break;
            }
            case LINK_ID_USER_LIST_MEMBERSHIPS: {
                fragment = new UserListMembershipsFragment();
                final String paramScreenName = uri.getQueryParameter(QUERY_PARAM_SCREEN_NAME);
                final String paramUserId = uri.getQueryParameter(QUERY_PARAM_USER_ID);
                if (!args.containsKey(EXTRA_SCREEN_NAME)) {
                    args.putString(EXTRA_SCREEN_NAME, paramScreenName);
                }
                if (!args.containsKey(EXTRA_USER_ID)) {
                    args.putLong(EXTRA_USER_ID, NumberUtils.toLong(paramUserId, -1));
                }
                break;
            }
            case LINK_ID_USER_TIMELINE: {
                fragment = new UserTimelineFragment();
                final String paramScreenName = uri.getQueryParameter(QUERY_PARAM_SCREEN_NAME);
                final String paramUserId = uri.getQueryParameter(QUERY_PARAM_USER_ID);
                if (!args.containsKey(EXTRA_SCREEN_NAME)) {
                    args.putString(EXTRA_SCREEN_NAME, paramScreenName);
                }
                if (!args.containsKey(EXTRA_USER_ID)) {
                    args.putLong(EXTRA_USER_ID, NumberUtils.toLong(paramUserId, -1));
                }
                if (isEmpty(paramScreenName) && isEmpty(paramUserId)) return null;
                break;
            }
            case LINK_ID_USER_MEDIA_TIMELINE: {
                fragment = new UserMediaTimelineFragment();
                final String paramScreenName = uri.getQueryParameter(QUERY_PARAM_SCREEN_NAME);
                final String paramUserId = uri.getQueryParameter(QUERY_PARAM_USER_ID);
                if (!args.containsKey(EXTRA_SCREEN_NAME)) {
                    args.putString(EXTRA_SCREEN_NAME, paramScreenName);
                }
                if (!args.containsKey(EXTRA_USER_ID)) {
                    args.putLong(EXTRA_USER_ID, NumberUtils.toLong(paramUserId, -1));
                }
                if (isEmpty(paramScreenName) && isEmpty(paramUserId)) return null;
                break;
            }
            case LINK_ID_USER_FAVORITES: {
                fragment = new UserFavoritesFragment();
                final String paramScreenName = uri.getQueryParameter(QUERY_PARAM_SCREEN_NAME);
                final String paramUserId = uri.getQueryParameter(QUERY_PARAM_USER_ID);
                if (!args.containsKey(EXTRA_SCREEN_NAME)) {
                    args.putString(EXTRA_SCREEN_NAME, paramScreenName);
                }
                if (!args.containsKey(EXTRA_USER_ID)) {
                    args.putLong(EXTRA_USER_ID, NumberUtils.toLong(paramUserId, -1));
                }
                if (!args.containsKey(EXTRA_SCREEN_NAME) && !args.containsKey(EXTRA_USER_ID))
                    return null;
                break;
            }
            case LINK_ID_USER_FOLLOWERS: {
                fragment = new UserFollowersFragment();
                final String paramScreenName = uri.getQueryParameter(QUERY_PARAM_SCREEN_NAME);
                final String paramUserId = uri.getQueryParameter(QUERY_PARAM_USER_ID);
                if (!args.containsKey(EXTRA_SCREEN_NAME)) {
                    args.putString(EXTRA_SCREEN_NAME, paramScreenName);
                }
                if (!args.containsKey(EXTRA_USER_ID)) {
                    args.putLong(EXTRA_USER_ID, NumberUtils.toLong(paramUserId, -1));
                }
                if (isEmpty(paramScreenName) && isEmpty(paramUserId)) return null;
                break;
            }
            case LINK_ID_USER_FRIENDS: {
                fragment = new UserFriendsFragment();
                final String paramScreenName = uri.getQueryParameter(QUERY_PARAM_SCREEN_NAME);
                final String param_user_id = uri.getQueryParameter(QUERY_PARAM_USER_ID);
                if (!args.containsKey(EXTRA_SCREEN_NAME)) {
                    args.putString(EXTRA_SCREEN_NAME, paramScreenName);
                }
                if (!args.containsKey(EXTRA_USER_ID)) {
                    args.putLong(EXTRA_USER_ID, NumberUtils.toLong(param_user_id, -1));
                }
                if (isEmpty(paramScreenName) && isEmpty(param_user_id)) return null;
                break;
            }
            case LINK_ID_USER_BLOCKS: {
                fragment = new UserBlocksListFragment();
                break;
            }
            case LINK_ID_MUTES_USERS: {
                fragment = new MutesUsersListFragment();
                break;
            }
            case LINK_ID_SCHEDULED_STATUSES: {
                fragment = new ScheduledStatusesFragment();
                break;
            }
            case LINK_ID_DIRECT_MESSAGES_CONVERSATION: {
                fragment = new MessagesConversationFragment();
                isAccountIdRequired = false;
                final String paramRecipientId = uri.getQueryParameter(QUERY_PARAM_RECIPIENT_ID);
                final String paramScreenName = uri.getQueryParameter(QUERY_PARAM_SCREEN_NAME);
                final long conversationId = NumberUtils.toLong(paramRecipientId, -1);
                if (conversationId > 0) {
                    args.putLong(EXTRA_RECIPIENT_ID, conversationId);
                } else if (paramScreenName != null) {
                    args.putString(EXTRA_SCREEN_NAME, paramScreenName);
                }
                break;
            }
            case LINK_ID_DIRECT_MESSAGES: {
                fragment = new DirectMessagesFragment();
                break;
            }
            case LINK_ID_INTERACTIONS: {
                fragment = new InteractionsTimelineFragment();
                break;
            }
            case LINK_ID_USER_LIST: {
                fragment = new UserListFragment();
                final String paramScreenName = uri.getQueryParameter(QUERY_PARAM_SCREEN_NAME);
                final String paramUserId = uri.getQueryParameter(QUERY_PARAM_USER_ID);
                final String paramListId = uri.getQueryParameter(QUERY_PARAM_LIST_ID);
                final String paramListName = uri.getQueryParameter(QUERY_PARAM_LIST_NAME);
                if (isEmpty(paramListId)
                        && (isEmpty(paramListName) || isEmpty(paramScreenName) && isEmpty(paramUserId)))
                    return null;
                args.putLong(EXTRA_LIST_ID, NumberUtils.toLong(paramListId, -1));
                args.putLong(EXTRA_USER_ID, NumberUtils.toLong(paramUserId, -1));
                args.putString(EXTRA_SCREEN_NAME, paramScreenName);
                args.putString(EXTRA_LIST_NAME, paramListName);
                break;
            }
            case LINK_ID_USER_LISTS: {
                fragment = new ListsFragment();
                final String paramScreenName = uri.getQueryParameter(QUERY_PARAM_SCREEN_NAME);
                final String paramUserId = uri.getQueryParameter(QUERY_PARAM_USER_ID);
                if (!args.containsKey(EXTRA_SCREEN_NAME)) {
                    args.putString(EXTRA_SCREEN_NAME, paramScreenName);
                }
                if (!args.containsKey(EXTRA_USER_ID)) {
                    args.putLong(EXTRA_USER_ID, NumberUtils.toLong(paramUserId, -1));
                }
                if (isEmpty(paramScreenName) && isEmpty(paramUserId)) return null;
                break;
            }
            case LINK_ID_USER_LIST_TIMELINE: {
                fragment = new UserListTimelineFragment();
                final String paramScreenName = uri.getQueryParameter(QUERY_PARAM_SCREEN_NAME);
                final String paramUserId = uri.getQueryParameter(QUERY_PARAM_USER_ID);
                final String paramListId = uri.getQueryParameter(QUERY_PARAM_LIST_ID);
                final String paramListName = uri.getQueryParameter(QUERY_PARAM_LIST_NAME);
                if (isEmpty(paramListId)
                        && (isEmpty(paramListName) || isEmpty(paramScreenName) && isEmpty(paramUserId)))
                    return null;
                args.putLong(EXTRA_LIST_ID, NumberUtils.toLong(paramListId, -1));
                args.putLong(EXTRA_USER_ID, NumberUtils.toLong(paramUserId, -1));
                args.putString(EXTRA_SCREEN_NAME, paramScreenName);
                args.putString(EXTRA_LIST_NAME, paramListName);
                break;
            }
            case LINK_ID_USER_LIST_MEMBERS: {
                fragment = new UserListMembersFragment();
                final String paramScreenName = uri.getQueryParameter(QUERY_PARAM_SCREEN_NAME);
                final String paramUserId = uri.getQueryParameter(QUERY_PARAM_USER_ID);
                final String paramListId = uri.getQueryParameter(QUERY_PARAM_LIST_ID);
                final String paramListName = uri.getQueryParameter(QUERY_PARAM_LIST_NAME);
                if (isEmpty(paramListId)
                        && (isEmpty(paramListName) || isEmpty(paramScreenName) && isEmpty(paramUserId)))
                    return null;
                args.putLong(EXTRA_LIST_ID, NumberUtils.toLong(paramListId, -1));
                args.putLong(EXTRA_USER_ID, NumberUtils.toLong(paramUserId, -1));
                args.putString(EXTRA_SCREEN_NAME, paramScreenName);
                args.putString(EXTRA_LIST_NAME, paramListName);
                break;
            }
            case LINK_ID_USER_LIST_SUBSCRIBERS: {
                fragment = new UserListSubscribersFragment();
                final String paramScreenName = uri.getQueryParameter(QUERY_PARAM_SCREEN_NAME);
                final String paramUserId = uri.getQueryParameter(QUERY_PARAM_USER_ID);
                final String paramListId = uri.getQueryParameter(QUERY_PARAM_LIST_ID);
                final String paramListName = uri.getQueryParameter(QUERY_PARAM_LIST_NAME);
                if (isEmpty(paramListId)
                        && (isEmpty(paramListName) || isEmpty(paramScreenName) && isEmpty(paramUserId)))
                    return null;
                args.putLong(EXTRA_LIST_ID, NumberUtils.toLong(paramListId, -1));
                args.putLong(EXTRA_USER_ID, NumberUtils.toLong(paramUserId, -1));
                args.putString(EXTRA_SCREEN_NAME, paramScreenName);
                args.putString(EXTRA_LIST_NAME, paramListName);
                break;
            }
            case LINK_ID_SAVED_SEARCHES: {
                fragment = new SavedSearchesListFragment();
                break;
            }
            case LINK_ID_USER_MENTIONS: {
                fragment = new UserMentionsFragment();
                final String paramScreenName = uri.getQueryParameter(QUERY_PARAM_SCREEN_NAME);
                if (!args.containsKey(EXTRA_SCREEN_NAME) && !isEmpty(paramScreenName)) {
                    args.putString(EXTRA_SCREEN_NAME, paramScreenName);
                }
                if (isEmpty(args.getString(EXTRA_SCREEN_NAME))) return null;
                break;
            }
            case LINK_ID_INCOMING_FRIENDSHIPS: {
                fragment = new IncomingFriendshipsFragment();
                break;
            }
            case LINK_ID_USERS: {
                fragment = new UsersListFragment();
                break;
            }
            case LINK_ID_STATUSES: {
                fragment = new StatusesListFragment();
                break;
            }
            case LINK_ID_STATUS_RETWEETERS: {
                fragment = new StatusRetweetersListFragment();
                if (!args.containsKey(EXTRA_STATUS_ID)) {
                    final String paramStatusId = uri.getQueryParameter(QUERY_PARAM_STATUS_ID);
                    args.putLong(EXTRA_STATUS_ID, NumberUtils.toLong(paramStatusId, -1));
                }
                break;
            }
            case LINK_ID_STATUS_FAVORITERS: {
                fragment = new StatusFavoritersListFragment();
                if (!args.containsKey(EXTRA_STATUS_ID)) {
                    final String paramStatusId = uri.getQueryParameter(QUERY_PARAM_STATUS_ID);
                    args.putLong(EXTRA_STATUS_ID, NumberUtils.toLong(paramStatusId, -1));
                }
                break;
            }
            case LINK_ID_SEARCH: {
                final String paramQuery = uri.getQueryParameter(QUERY_PARAM_QUERY);
                if (!args.containsKey(EXTRA_QUERY) && !isEmpty(paramQuery)) {
                    args.putString(EXTRA_QUERY, paramQuery);
                }
                if (!args.containsKey(EXTRA_QUERY)) {
                    return null;
                }
                fragment = new SearchFragment();
                break;
            }
            default: {
                return null;
            }
        }
        UserKey accountKey = UserKey.valueOf(uri.getQueryParameter(QUERY_PARAM_ACCOUNT_KEY));
        if (accountKey == null) {
            final long accountId = NumberUtils.toLong(uri.getQueryParameter(QUERY_PARAM_ACCOUNT_ID), -1);
            final String paramAccountName = uri.getQueryParameter(QUERY_PARAM_ACCOUNT_NAME);
            if (accountId != -1) {
                accountKey = DataStoreUtils.findAccountKey(context,
                        accountId);
                args.putParcelable(EXTRA_ACCOUNT_KEY, accountKey);
            } else if (paramAccountName != null) {
                accountKey = DataStoreUtils.findAccountKey(context,
                        paramAccountName);
            } else {
                accountKey = getDefaultAccountKey(context);
            }
        }

        if (isAccountIdRequired && accountKey == null) {
            return null;
        }
        args.putParcelable(EXTRA_ACCOUNT_KEY, accountKey);
        fragment.setArguments(args);
        return fragment;
    }

    public static Intent createStatusShareIntent(@NonNull final Context context, @NonNull final ParcelableStatus status) {
        final Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, IntentUtils.getStatusShareSubject(context, status));
        intent.putExtra(Intent.EXTRA_TEXT, IntentUtils.getStatusShareText(context, status));
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        return intent;
    }

    @Nullable
    public static UserKey[] getAccountKeys(@NonNull Context context, @Nullable Bundle args) {
        if (args == null) return null;
        if (args.containsKey(EXTRA_ACCOUNT_KEYS)) {
            return newParcelableArray(args.getParcelableArray(EXTRA_ACCOUNT_KEYS), UserKey.CREATOR);
        } else if (args.containsKey(EXTRA_ACCOUNT_KEY)) {
            final UserKey accountKey = args.getParcelable(EXTRA_ACCOUNT_KEY);
            if (accountKey == null) return new UserKey[0];
            return new UserKey[]{accountKey};
        } else if (args.containsKey(EXTRA_ACCOUNT_IDS)) {
            final long[] accountIds = args.getLongArray(EXTRA_ACCOUNT_IDS);
            if (accountIds == null) return null;
            final UserKey[] accountKeys = UserKeyUtils.findByIds(context, accountIds);
            args.putParcelableArray(EXTRA_ACCOUNT_KEYS, accountKeys);
            return accountKeys;
        } else if (args.containsKey(EXTRA_ACCOUNT_ID)) {
            final long accountId = args.getLong(EXTRA_ACCOUNT_ID, -1);
            if (accountId <= 0) return null;
            final UserKey accountKey = UserKeyUtils.findById(context, accountId);
            args.putParcelable(EXTRA_ACCOUNT_KEY, accountKey);
            if (accountKey == null) return new UserKey[0];
            return new UserKey[]{accountKey};
        }
        return null;
    }

    @Nullable
    public static String getReadPositionTagWithAccounts(@Nullable final String tag,
                                                        final UserKey... accountKeys) {
        if (tag == null) return null;
        if (ArrayUtils.isEmpty(accountKeys)) return tag;
        final UserKey[] accountIdsClone = accountKeys.clone();
        Arrays.sort(accountIdsClone);
        return tag + "_" + TwidereArrayUtils.toString(accountIdsClone, '_', false);
    }

    @Nullable
    public static String getReadPositionTagWithAccounts(Context context, boolean activatedIfMissing,
                                                        @Nullable @ReadPositionTag String tag,
                                                        UserKey... accountKeys) {
        if (tag == null) return null;
        if (ArrayUtils.isEmpty(accountKeys)) {
            final UserKey[] activatedIds = DataStoreUtils.getActivatedAccountKeys(context);
            Arrays.sort(activatedIds);
            return tag + "_" + TwidereArrayUtils.toString(activatedIds, '_', false);
        }
        final UserKey[] accountIdsClone = accountKeys.clone();
        Arrays.sort(accountIdsClone);
        return tag + "_" + TwidereArrayUtils.toString(accountIdsClone, '_', false);
    }

    public static String encodeQueryParams(final String value) throws IOException {
        final String encoded = URLEncoder.encode(value, "UTF-8");
        final StringBuilder buf = new StringBuilder();
        final int length = encoded.length();
        char focus;
        for (int i = 0; i < length; i++) {
            focus = encoded.charAt(i);
            if (focus == '*') {
                buf.append("%2A");
            } else if (focus == '+') {
                buf.append("%20");
            } else if (focus == '%' && i + 1 < encoded.length() && encoded.charAt(i + 1) == '7'
                    && encoded.charAt(i + 2) == 'E') {
                buf.append('~');
                i += 2;
            } else {
                buf.append(focus);
            }
        }
        return buf.toString();
    }

    public static ParcelableDirectMessage findDirectMessageInDatabases(final Context context,
                                                                       final UserKey accountKey,
                                                                       final long messageId) {
        if (context == null) return null;
        final ContentResolver resolver = context.getContentResolver();
        ParcelableDirectMessage message = null;
        final String where = Expression.and(Expression.equalsArgs(DirectMessages.ACCOUNT_KEY),
                Expression.equalsArgs(DirectMessages.MESSAGE_ID)).getSQL();
        final String[] whereArgs = {accountKey.toString(), String.valueOf(messageId)};
        for (final Uri uri : DIRECT_MESSAGES_URIS) {
            final Cursor cur = resolver.query(uri, DirectMessages.COLUMNS, where, whereArgs, null);
            if (cur == null) {
                continue;
            }
            if (cur.getCount() > 0 && cur.moveToFirst()) {
                message = ParcelableDirectMessageCursorIndices.fromCursor(cur);
            }
            cur.close();
        }
        return message;
    }

    @NonNull
    public static ParcelableStatus findStatus(final Context context, final UserKey accountKey,
                                              final long statusId)
            throws TwitterException {
        if (context == null) throw new NullPointerException();
        final ParcelableStatus cached = findStatusInDatabases(context, accountKey, statusId);
        if (cached != null) return cached;
        final Twitter twitter = TwitterAPIFactory.getTwitterInstance(context, accountKey, true);
        if (twitter == null) throw new TwitterException("Account does not exist");
        final Status status = twitter.showStatus(statusId);
        final String where = Expression.and(Expression.equals(Statuses.ACCOUNT_KEY, accountKey.getId()),
                Expression.equals(Statuses.STATUS_ID, statusId)).getSQL();
        final ContentResolver resolver = context.getContentResolver();
        resolver.delete(CachedStatuses.CONTENT_URI, where, null);
        resolver.insert(CachedStatuses.CONTENT_URI, ContentValuesCreator.createStatus(status, accountKey));
        return ParcelableStatusUtils.fromStatus(status, accountKey, false);
    }

    @Nullable
    public static ParcelableStatus findStatusInDatabases(final Context context, final UserKey accountKey,
                                                         final long statusId) {
        if (context == null) return null;
        final ContentResolver resolver = context.getContentResolver();
        ParcelableStatus status = null;
        final String where = Expression.and(Expression.equals(Statuses.ACCOUNT_KEY, accountKey.getId()),
                Expression.equals(Statuses.STATUS_ID, statusId)).getSQL();
        for (final Uri uri : STATUSES_URIS) {
            final Cursor cur = resolver.query(uri, Statuses.COLUMNS, where, null, null);
            if (cur == null) {
                continue;
            }
            if (cur.getCount() > 0) {
                cur.moveToFirst();
                status = ParcelableStatusCursorIndices.fromCursor(cur);
            }
            cur.close();
        }
        return status;
    }

    @SuppressWarnings("deprecation")
    public static String formatSameDayTime(final Context context, final long timestamp) {
        if (context == null) return null;
        if (DateUtils.isToday(timestamp))
            return DateUtils.formatDateTime(context, timestamp,
                    DateFormat.is24HourFormat(context) ? DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_24HOUR
                            : DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_12HOUR);
        return DateUtils.formatDateTime(context, timestamp, DateUtils.FORMAT_SHOW_DATE);
    }

    @SuppressWarnings("deprecation")
    public static String formatTimeStampString(final Context context, final long timestamp) {
        if (context == null) return null;
        final Time then = new Time();
        then.set(timestamp);
        final Time now = new Time();
        now.setToNow();

        int format_flags = DateUtils.FORMAT_NO_NOON_MIDNIGHT | DateUtils.FORMAT_ABBREV_ALL | DateUtils.FORMAT_CAP_AMPM;

        if (then.year != now.year) {
            format_flags |= DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_SHOW_DATE;
        } else if (then.yearDay != now.yearDay) {
            format_flags |= DateUtils.FORMAT_SHOW_DATE;
        } else {
            format_flags |= DateUtils.FORMAT_SHOW_TIME;
        }

        return DateUtils.formatDateTime(context, timestamp, format_flags);
    }

    @SuppressWarnings("deprecation")
    public static String formatTimeStampString(final Context context, final String date_time) {
        if (context == null) return null;
        return formatTimeStampString(context, Date.parse(date_time));
    }

    @SuppressWarnings("deprecation")
    public static String formatToLongTimeString(final Context context, final long timestamp) {
        if (context == null) return null;
        final Time then = new Time();
        then.set(timestamp);
        final Time now = new Time();
        now.setToNow();

        int format_flags = DateUtils.FORMAT_NO_NOON_MIDNIGHT | DateUtils.FORMAT_ABBREV_ALL | DateUtils.FORMAT_CAP_AMPM;

        format_flags |= DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME;

        return DateUtils.formatDateTime(context, timestamp, format_flags);
    }

    public static int getAccountNotificationId(final int notificationType, final long accountId) {
        return Arrays.hashCode(new long[]{notificationType, accountId});
    }

    public static boolean isComposeNowSupported(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN || context == null) return false;
        return hasNavBar(context);
    }

    public static boolean isOfficialCredentials(@NonNull final Context context, final UserKey accountKey) {
        final ParcelableCredentials credentials = ParcelableCredentialsUtils.getCredentials(context, accountKey);
        if (credentials == null) return false;
        return isOfficialCredentials(context, credentials);
    }

    public static boolean isOfficialCredentials(@NonNull final Context context,
                                                @NonNull final ParcelableCredentials account) {
        if (ParcelableCredentials.ACCOUNT_TYPE_TWITTER.equals(account.account_type)) {
            final TwitterAccountExtra extra = JsonSerializer.parse(account.account_extras,
                    TwitterAccountExtra.class);
            if (extra != null) {
                return extra.isOfficialCredentials();
            }
        }
        final boolean isOAuth = ParcelableCredentialsUtils.isOAuth(account.auth_type);
        final String consumerKey = account.consumer_key, consumerSecret = account.consumer_secret;
        return isOAuth && TwitterContentUtils.isOfficialKey(context, consumerKey, consumerSecret);
    }

    public static TextView newSectionView(final Context context, final int titleRes) {
        return newSectionView(context, titleRes != 0 ? context.getString(titleRes) : null);
    }

    public static TextView newSectionView(final Context context, final CharSequence title) {
        final TextView textView = new TextView(context, null, android.R.attr.listSeparatorTextViewStyle);
        textView.setText(title);
        return textView;
    }

    public static boolean setLastSeen(Context context, ParcelableUserMention[] entities, long time) {
        if (entities == null) return false;
        boolean result = false;
        for (ParcelableUserMention entity : entities) {
            result |= setLastSeen(context, entity.key, time);
        }
        return result;
    }

    public static boolean setLastSeen(Context context, UserKey userId, long time) {
        final ContentResolver cr = context.getContentResolver();
        final ContentValues values = new ContentValues();
        if (time > 0) {
            values.put(CachedUsers.LAST_SEEN, time);
        } else {
            // Zero or negative value means remove last seen
            values.putNull(CachedUsers.LAST_SEEN);
        }
        final String where = Expression.equalsArgs(CachedUsers.USER_KEY).getSQL();
        final String[] selectionArgs = {userId.toString()};
        return cr.update(CachedUsers.CONTENT_URI, values, where, selectionArgs) > 0;
    }

    public static File getBestCacheDir(final Context context, final String cacheDirName) {
        if (context == null) throw new NullPointerException();
        final File extCacheDir;
        try {
            // Workaround for https://github.com/mariotaku/twidere/issues/138
            extCacheDir = context.getExternalCacheDir();
        } catch (final Exception e) {
            return new File(context.getCacheDir(), cacheDirName);
        }
        if (extCacheDir != null && extCacheDir.isDirectory()) {
            final File cacheDir = new File(extCacheDir, cacheDirName);
            if (cacheDir.isDirectory() || cacheDir.mkdirs()) return cacheDir;
        }
        return new File(context.getCacheDir(), cacheDirName);
    }

    public static String getBiggerTwitterProfileImage(final String url) {
        return getTwitterProfileImageOfSize(url, "bigger");
    }

    public static Bitmap getBitmap(final Drawable drawable) {
        if (drawable instanceof NinePatchDrawable) return null;
        if (drawable instanceof BitmapDrawable)
            return ((BitmapDrawable) drawable).getBitmap();
        else if (drawable instanceof TransitionDrawable) {
            final int layer_count = ((TransitionDrawable) drawable).getNumberOfLayers();
            for (int i = 0; i < layer_count; i++) {
                final Drawable layer = ((TransitionDrawable) drawable).getDrawable(i);
                if (layer instanceof BitmapDrawable) return ((BitmapDrawable) layer).getBitmap();
            }
        }
        return null;
    }

    public static Bitmap.CompressFormat getBitmapCompressFormatByMimeType(final String mimeType,
                                                                          final Bitmap.CompressFormat def) {
        final String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
        if ("jpeg".equalsIgnoreCase(extension) || "jpg".equalsIgnoreCase(extension))
            return Bitmap.CompressFormat.JPEG;
        else if ("png".equalsIgnoreCase(extension))
            return Bitmap.CompressFormat.PNG;
        else if ("webp".equalsIgnoreCase(extension)) return Bitmap.CompressFormat.WEBP;
        return def;
    }

    public static int getCardHighlightColor(final Context context, final boolean isMention,
                                            final boolean isFavorite, final boolean isRetweet) {
        if (isMention)
            return ContextCompat.getColor(context, R.color.highlight_reply);
        else if (isRetweet)
            return ContextCompat.getColor(context, R.color.highlight_retweet);
        else if (isFavorite)
            return ContextCompat.getColor(context, R.color.highlight_like);
        return Color.TRANSPARENT;
    }

    public static String getCardHighlightOption(final Context context) {
        if (context == null) return null;
        final String defaultOption = context.getString(R.string.default_tab_display_option);
        final SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_TAB_DISPLAY_OPTION, defaultOption);
    }

    public static int getCardHighlightOptionInt(final Context context) {
        return getCardHighlightOptionInt(getCardHighlightOption(context));
    }

    public static int getCardHighlightOptionInt(final String option) {
        if (VALUE_CARD_HIGHLIGHT_OPTION_NONE.equals(option))
            return VALUE_CARD_HIGHLIGHT_OPTION_CODE_NONE;
        else if (VALUE_CARD_HIGHLIGHT_OPTION_LINE.equals(option))
            return VALUE_CARD_HIGHLIGHT_OPTION_CODE_LINE;
        return VALUE_CARD_HIGHLIGHT_OPTION_CODE_BACKGROUND;
    }

    public static Selectable getColumnsFromProjection(final String... projection) {
        if (projection == null) return new AllColumns();
        final int length = projection.length;
        final Column[] columns = new Column[length];
        for (int i = 0; i < length; i++) {
            columns[i] = new Column(projection[i]);
        }
        return new Columns(columns);
    }

    @Nullable
    public static UserKey getDefaultAccountKey(final Context context) {
        if (context == null) return null;
        final SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFERENCES_NAME,
                Context.MODE_PRIVATE);
        UserKey accountKey = UserKey.valueOf(prefs.getString(KEY_DEFAULT_ACCOUNT_KEY, null));
        final UserKey[] accountKeys = DataStoreUtils.getAccountKeys(context);
        int idMatchIdx = -1;
        for (int i = 0, accountIdsLength = accountKeys.length; i < accountIdsLength; i++) {
            if (accountKeys[i].equals(accountKey)) {
                idMatchIdx = i;
            }
        }
        if (idMatchIdx != -1) {
            return accountKeys[idMatchIdx];
        }
        if (accountKeys.length > 0 && !ArrayUtils.contains(accountKeys, accountKey)) {
             /* TODO: this is just a quick fix */
            return accountKeys[0];
        }
        return null;
    }

    public static int getDefaultTextSize(final Context context) {
        if (context == null) return 15;
        return context.getResources().getInteger(R.integer.default_text_size);
    }

    public static String getErrorMessage(final Context context, final CharSequence message) {
        if (context == null) return ParseUtils.parseString(message);
        if (isEmpty(message)) return context.getString(R.string.error_unknown_error);
        return context.getString(R.string.error_message, message);
    }

    public static String getErrorMessage(final Context context, final CharSequence action, final CharSequence message) {
        if (context == null || isEmpty(action)) return ParseUtils.parseString(message);
        if (isEmpty(message)) return context.getString(R.string.error_unknown_error);
        return context.getString(R.string.error_message_with_action, action, message);
    }

    public static String getErrorMessage(final Context context, final CharSequence action, final Throwable t) {
        if (context == null) return null;
        if (t instanceof TwitterException)
            return getTwitterErrorMessage(context, action, (TwitterException) t);
        else if (t != null) return getErrorMessage(context, trimLineBreak(t.getMessage()));
        return context.getString(R.string.error_unknown_error);
    }

    public static String getErrorMessage(final Context context, final Throwable t) {
        if (t == null) return null;
        if (context != null && t instanceof TwitterException)
            return getTwitterErrorMessage(context, (TwitterException) t);
        return t.getMessage();
    }

    public static int getFirstChildOffset(final AbsListView list) {
        if (list == null || list.getChildCount() == 0) return 0;
        final View child = list.getChildAt(0);
        final int[] location = new int[2];
        child.getLocationOnScreen(location);
        Log.d(LOGTAG, String.format("getFirstChildOffset %d vs %d", child.getTop(), location[1]));
        return child.getTop();
    }


    public static String getImageMimeType(final File image) {
        if (image == null) return null;
        final BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(image.getPath(), o);
        return o.outMimeType;
    }

    public static String getImageMimeType(final InputStream is) {
        if (is == null) return null;
        final BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(is, null, o);
        return o.outMimeType;
    }

    public static String getImagePathFromUri(final Context context, final Uri uri) {
        if (context == null || uri == null) return null;

        final String mediaUriStart = ParseUtils.parseString(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        if (ParseUtils.parseString(uri).startsWith(mediaUriStart)) {

            final String[] proj = {MediaStore.Images.Media.DATA};
            final Cursor cur = context.getContentResolver().query(uri, proj, null, null, null);

            if (cur == null) return null;

            final int idxData = cur.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

            cur.moveToFirst();
            try {
                return cur.getString(idxData);
            } finally {
                cur.close();
            }
        } else {
            final String path = uri.getPath();
            if (path != null && new File(path).exists()) return path;
        }
        return null;
    }

    public static String getImageUploadStatus(@NonNull final Context context,
                                              @Nullable final CharSequence[] links,
                                              @Nullable final CharSequence text) {
        if (ArrayUtils.isEmpty(links) || text == null) return ParseUtils.parseString(text);
        return text + " " + TwidereArrayUtils.toString(links, ' ', false);
    }

    public static File getInternalCacheDir(final Context context, final String cacheDirName) {
        if (context == null) throw new NullPointerException();
        final File cacheDir = new File(context.getCacheDir(), cacheDirName);
        if (cacheDir.isDirectory() || cacheDir.mkdirs()) return cacheDir;
        return new File(context.getCacheDir(), cacheDirName);
    }

    @Nullable
    public static File getExternalCacheDir(final Context context, final String cacheDirName) {
        if (context == null) throw new NullPointerException();
        final File externalCacheDir = context.getExternalCacheDir();
        if (externalCacheDir == null) return null;
        final File cacheDir = new File(externalCacheDir, cacheDirName);
        if (cacheDir.isDirectory() || cacheDir.mkdirs()) return cacheDir;
        return new File(context.getCacheDir(), cacheDirName);
    }

    public static CharSequence getKeywordBoldedText(final CharSequence orig, final String... keywords) {
        return getKeywordHighlightedText(orig, new StyleSpan(Typeface.BOLD), keywords);
    }

    public static CharSequence getKeywordHighlightedText(final CharSequence orig, final CharacterStyle style,
                                                         final String... keywords) {
        if (keywords == null || keywords.length == 0 || orig == null) return orig;
        final SpannableStringBuilder sb = SpannableStringBuilder.valueOf(orig);
        final StringBuilder patternBuilder = new StringBuilder();
        for (int i = 0, j = keywords.length; i < j; i++) {
            if (i != 0) {
                patternBuilder.append('|');
            }
            patternBuilder.append(Pattern.quote(keywords[i]));
        }
        final Matcher m = Pattern.compile(patternBuilder.toString(), Pattern.CASE_INSENSITIVE).matcher(orig);
        while (m.find()) {
            sb.setSpan(style, m.start(), m.end(), SpannableStringBuilder.SPAN_INCLUSIVE_INCLUSIVE);
        }
        return sb;
    }

    public static String getLinkHighlightingStyleName(final Context context) {
        if (context == null) return null;
        final SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_LINK_HIGHLIGHT_OPTION, VALUE_LINK_HIGHLIGHT_OPTION_HIGHLIGHT);
    }

    @HighlightStyle
    public static int getLinkHighlightingStyle(final Context context) {
        return getLinkHighlightingStyleInt(getLinkHighlightingStyleName(context));
    }

    @HighlightStyle
    public static int getLinkHighlightingStyleInt(final String option) {
        if (option == null) return VALUE_LINK_HIGHLIGHT_OPTION_CODE_HIGHLIGHT;
        switch (option) {
            case VALUE_LINK_HIGHLIGHT_OPTION_BOTH:
                return VALUE_LINK_HIGHLIGHT_OPTION_CODE_BOTH;
            case VALUE_LINK_HIGHLIGHT_OPTION_HIGHLIGHT:
                return VALUE_LINK_HIGHLIGHT_OPTION_CODE_HIGHLIGHT;
            case VALUE_LINK_HIGHLIGHT_OPTION_UNDERLINE:
                return VALUE_LINK_HIGHLIGHT_OPTION_CODE_UNDERLINE;
        }
        return VALUE_LINK_HIGHLIGHT_OPTION_CODE_HIGHLIGHT;
    }

    public static String getLocalizedNumber(final Locale locale, final Number number) {
        final NumberFormat nf = NumberFormat.getInstance(locale);
        return nf.format(number);
    }

    @NonNull
    public static String getNonEmptyString(final SharedPreferences pref, final String key, final String def) {
        if (pref == null) return def;
        final String val = pref.getString(key, def);
        return isEmpty(val) ? def : val;
    }

    public static String getNormalTwitterProfileImage(final String url) {
        return getTwitterProfileImageOfSize(url, "normal");
    }

    public static Uri getNotificationUri(final int tableId, final Uri def) {
        switch (tableId) {
            case TABLE_ID_DIRECT_MESSAGES:
            case TABLE_ID_DIRECT_MESSAGES_CONVERSATION:
            case TABLE_ID_DIRECT_MESSAGES_CONVERSATION_SCREEN_NAME:
            case TABLE_ID_DIRECT_MESSAGES_CONVERSATIONS_ENTRIES:
                return DirectMessages.CONTENT_URI;
        }
        return def;
    }

    public static String getOriginalTwitterProfileImage(final String url) {
        if (url == null) return null;
        if (PATTERN_TWITTER_PROFILE_IMAGES.matcher(url).matches())
            return replaceLast(url, "_" + TWITTER_PROFILE_IMAGES_AVAILABLE_SIZES, "");
        return url;
    }

    @ShapeStyle
    public static int getProfileImageStyle(Context context) {
        final SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        final String style = prefs.getString(KEY_PROFILE_IMAGE_STYLE, null);
        return getProfileImageStyle(style);
    }

    @ShapeStyle
    public static int getProfileImageStyle(String style) {
        if (VALUE_PROFILE_IMAGE_STYLE_ROUND.equalsIgnoreCase(style)) {
            return ShapedImageView.SHAPE_CIRCLE;
        }
        return ShapedImageView.SHAPE_RECTANGLE;
    }

    @PreviewStyle
    public static int getMediaPreviewStyle(String style) {
        if (VALUE_MEDIA_PREVIEW_STYLE_SCALE.equalsIgnoreCase(style)) {
            return VALUE_MEDIA_PREVIEW_STYLE_CODE_SCALE;
        }
        return VALUE_MEDIA_PREVIEW_STYLE_CODE_CROP;
    }

    public static String getQuoteStatus(final Context context, long statusId, final String screen_name, final String text) {
        if (context == null) return null;
        String quoteFormat = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).getString(
                KEY_QUOTE_FORMAT, DEFAULT_QUOTE_FORMAT);
        if (isEmpty(quoteFormat)) {
            quoteFormat = DEFAULT_QUOTE_FORMAT;
        }
        String result = quoteFormat.replace(FORMAT_PATTERN_LINK, LinkCreator.getTwitterStatusLink(screen_name, statusId).toString());
        result = result.replace(FORMAT_PATTERN_NAME, screen_name);
        result = result.replace(FORMAT_PATTERN_TEXT, text);
        return result;
    }

    public static String getReasonablySmallTwitterProfileImage(final String url) {
        return getTwitterProfileImageOfSize(url, "reasonably_small");
    }

    public static int getResId(final Context context, final String string) {
        if (context == null || string == null) return 0;
        Matcher m = PATTERN_RESOURCE_IDENTIFIER.matcher(string);
        final Resources res = context.getResources();
        if (m.matches()) return res.getIdentifier(m.group(2), m.group(1), context.getPackageName());
        m = PATTERN_XML_RESOURCE_IDENTIFIER.matcher(string);
        if (m.matches()) return res.getIdentifier(m.group(1), "xml", context.getPackageName());
        return 0;
    }


    public static String getSenderUserName(final Context context, final ParcelableDirectMessage user) {
        if (context == null || user == null) return null;
        final SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        final boolean display_name = prefs.getBoolean(KEY_NAME_FIRST, true);
        return display_name ? user.sender_name : "@" + user.sender_screen_name;
    }

    public static String getShareStatus(final Context context, final CharSequence title, final CharSequence text) {
        if (context == null) return null;
        String share_format = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).getString(
                KEY_SHARE_FORMAT, DEFAULT_SHARE_FORMAT);
        if (isEmpty(share_format)) {
            share_format = DEFAULT_SHARE_FORMAT;
        }
        if (isEmpty(title)) return ParseUtils.parseString(text);
        return share_format.replace(FORMAT_PATTERN_TITLE, title).replace(FORMAT_PATTERN_TEXT, text != null ? text : "");
    }

    public static String getTabDisplayOption(final Context context) {
        if (context == null) return null;
        final String defaultOption = context.getString(R.string.default_tab_display_option);
        final SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_TAB_DISPLAY_OPTION, defaultOption);
    }

    public static int getTabDisplayOptionInt(final Context context) {
        return getTabDisplayOptionInt(getTabDisplayOption(context));
    }

    public static int getTabDisplayOptionInt(final String option) {
        if (VALUE_TAB_DISPLAY_OPTION_ICON.equals(option))
            return VALUE_TAB_DISPLAY_OPTION_CODE_ICON;
        else if (VALUE_TAB_DISPLAY_OPTION_LABEL.equals(option))
            return VALUE_TAB_DISPLAY_OPTION_CODE_LABEL;
        return VALUE_TAB_DISPLAY_OPTION_CODE_BOTH;
    }

    public static long getTimestampFromDate(final Date date) {
        if (date == null) return -1;
        return date.getTime();
    }

    public static boolean hasNavBar(@NonNull Context context) {
        final Resources resources = context.getResources();
        if (resources == null) return false;
        int id = resources.getIdentifier("config_showNavigationBar", "bool", "android");
        if (id > 0) {
            return resources.getBoolean(id);
        } else {
            // Check for keys
            return !KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK)
                    && !KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_HOME);
        }
    }

    public static String getTwitterErrorMessage(final Context context, final CharSequence action,
                                                final TwitterException te) {
        if (context == null) return null;
        if (te == null) return context.getString(R.string.error_unknown_error);
        if (te.exceededRateLimitation()) {
            final RateLimitStatus status = te.getRateLimitStatus();
            final long secUntilReset = status.getSecondsUntilReset() * 1000;
            final String nextResetTime = ParseUtils.parseString(getRelativeTimeSpanString(System.currentTimeMillis()
                    + secUntilReset));
            if (isEmpty(action))
                return context.getString(R.string.error_message_rate_limit, nextResetTime.trim());
            return context.getString(R.string.error_message_rate_limit_with_action, action, nextResetTime.trim());
        } else if (te.getErrorCode() > 0) {
            final String msg = StatusCodeMessageUtils.getTwitterErrorMessage(context, te.getErrorCode());
            return getErrorMessage(context, action, msg != null ? msg : trimLineBreak(te.getMessage()));
        } else if (te.getCause() instanceof SSLException) {
            final String msg = te.getCause().getMessage();
            if (msg != null && msg.contains("!="))
                return getErrorMessage(context, action, context.getString(R.string.ssl_error));
            else
                return getErrorMessage(context, action, context.getString(R.string.network_error));
        } else if (te.getCause() instanceof IOException)
            return getErrorMessage(context, action, context.getString(R.string.network_error));
        else if (te.getCause() instanceof JSONException)
            return getErrorMessage(context, action, context.getString(R.string.api_data_corrupted));
        else
            return getErrorMessage(context, action, trimLineBreak(te.getMessage()));
    }

    public static String getTwitterErrorMessage(final Context context, final TwitterException te) {
        if (te == null) return null;
        if (StatusCodeMessageUtils.containsTwitterError(te.getErrorCode()))
            return StatusCodeMessageUtils.getTwitterErrorMessage(context, te.getErrorCode());
        else if (StatusCodeMessageUtils.containsHttpStatus(te.getStatusCode()))
            return StatusCodeMessageUtils.getHttpStatusMessage(context, te.getStatusCode());
        else
            return te.getMessage();
    }


    public static String getTwitterProfileImageOfSize(final String url, final String size) {
        if (url == null) return null;
        if (PATTERN_TWITTER_PROFILE_IMAGES.matcher(url).matches())
            return replaceLast(url, "_" + TWITTER_PROFILE_IMAGES_AVAILABLE_SIZES, String.format("_%s", size));
        return url;
    }

    @DrawableRes
    public static int getUserTypeIconRes(final boolean isVerified, final boolean isProtected) {
        if (isVerified)
            return R.drawable.ic_user_type_verified;
        else if (isProtected) return R.drawable.ic_user_type_protected;
        return 0;
    }

    @StringRes
    public static int getUserTypeDescriptionRes(final boolean isVerified, final boolean isProtected) {
        if (isVerified)
            return R.string.user_type_verified;
        else if (isProtected) return R.string.user_type_protected;
        return 0;
    }

    public static boolean hasAccountSignedWithOfficialKeys(final Context context) {
        if (context == null) return false;
        final Cursor cur = context.getContentResolver().query(Accounts.CONTENT_URI, Accounts.COLUMNS, null, null, null);
        if (cur == null) return false;
        final String[] keySecrets = context.getResources().getStringArray(R.array.values_official_consumer_secret_crc32);
        final ParcelableCredentialsCursorIndices indices = new ParcelableCredentialsCursorIndices(cur);
        cur.moveToFirst();
        final CRC32 crc32 = new CRC32();
        try {
            while (!cur.isAfterLast()) {
                final String consumerSecret = cur.getString(indices.consumer_secret);
                if (consumerSecret != null) {
                    final byte[] consumerSecretBytes = consumerSecret.getBytes(Charset.forName("UTF-8"));
                    crc32.update(consumerSecretBytes, 0, consumerSecretBytes.length);
                    final long value = crc32.getValue();
                    crc32.reset();
                    for (final String keySecret : keySecrets) {
                        if (Long.parseLong(keySecret, 16) == value) return true;
                    }
                }
                cur.moveToNext();
            }
        } finally {
            cur.close();
        }
        return false;
    }

    public static boolean hasAutoRefreshAccounts(final Context context) {
        final UserKey[] accountKeys = DataStoreUtils.getAccountKeys(context);
        return !ArrayUtils.isEmpty(AccountPreferences.getAutoRefreshEnabledAccountIds(context, accountKeys));
    }

    public static boolean hasStaggeredTimeline() {
        return false;
    }

    public static boolean isBatteryOkay(final Context context) {
        if (context == null) return false;
        final Context app = context.getApplicationContext();
        final IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        final Intent intent = app.registerReceiver(null, filter);
        if (intent == null) return false;
        final boolean plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0;
        final float level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
        final float scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        return plugged || level / scale > 0.15f;
    }

    public static boolean isCompactCards(final Context context) {
        final SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        return prefs != null && prefs.getBoolean(KEY_COMPACT_CARDS, false);
    }

    public static boolean isDatabaseReady(final Context context) {
        final Cursor c = context.getContentResolver().query(TwidereDataStore.CONTENT_URI_DATABASE_READY, null, null, null,
                null);
        try {
            return c != null;
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    public static boolean isMyAccount(final Context context, @Nullable final UserKey accountKey) {
        if (context == null || accountKey == null) return false;
        final String[] projection = new String[]{SQLFunctions.COUNT()};
        final Cursor cur = DataStoreUtils.getAccountCursor(context, projection, accountKey);
        if (cur == null) return false;
        try {
            if (cur.moveToFirst()) return cur.getLong(0) > 0;
        } finally {
            cur.close();
        }
        return false;
    }

    public static boolean isMyAccount(final Context context, final String screen_name) {
        if (context == null) return false;
        final ContentResolver resolver = context.getContentResolver();
        final String where = Expression.equalsArgs(Accounts.SCREEN_NAME).getSQL();
        final String[] projection = new String[0];
        final Cursor cur = resolver.query(Accounts.CONTENT_URI, projection, where, new String[]{screen_name}, null);
        try {
            return cur != null && cur.getCount() > 0;
        } finally {
            if (cur != null) {
                cur.close();
            }
        }
    }

    public static boolean isMyRetweet(final ParcelableStatus status) {
        return status != null && isMyRetweet(status.account_key, status.retweeted_by_user_id,
                status.my_retweet_id);
    }

    public static boolean isMyRetweet(final UserKey accountId, final UserKey retweetedById, final long myRetweetId) {
        return accountId.equals(retweetedById) || myRetweetId > 0;
    }

    public static boolean isNetworkAvailable(final Context context) {
        final ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo info = cm.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }

    public static boolean isUserLoggedIn(final Context context, final long accountId) {
        if (context == null) return false;
        final UserKey[] ids = DataStoreUtils.getAccountKeys(context);
        for (final UserKey id : ids) {
            if (id.getId() == accountId) return true;
        }
        return false;
    }

    public static int matchLinkId(@Nullable final Uri uri) {
        if (uri == null) return UriMatcher.NO_MATCH;
        return LINK_HANDLER_URI_MATCHER.match(uri);
    }


    public static int matchTabCode(@Nullable final Uri uri) {
        if (uri == null) return UriMatcher.NO_MATCH;
        return HOME_TABS_URI_MATCHER.match(uri);
    }


    @CustomTabType
    public static String matchTabType(@Nullable final Uri uri) {
        return getTabType(matchTabCode(uri));
    }

    @CustomTabType
    public static String getTabType(final int code) {
        switch (code) {
            case TAB_CODE_HOME_TIMELINE: {
                return CustomTabType.HOME_TIMELINE;
            }
            case TAB_CODE_NOTIFICATIONS_TIMELINE: {
                return CustomTabType.NOTIFICATIONS_TIMELINE;
            }
            case TAB_CODE_DIRECT_MESSAGES: {
                return CustomTabType.DIRECT_MESSAGES;
            }
        }
        return null;
    }


    @SuppressWarnings("SuspiciousSystemArraycopy")
    public static <T extends Parcelable> T[] newParcelableArray(Parcelable[] array, Parcelable.Creator<T> creator) {
        if (array == null) return null;
        final T[] result = creator.newArray(array.length);
        System.arraycopy(array, 0, result, 0, array.length);
        return result;
    }

    public static boolean setNdefPushMessageCallback(Activity activity, CreateNdefMessageCallback callback) {
        try {
            final NfcAdapter adapter = NfcAdapter.getDefaultAdapter(activity);
            if (adapter == null) return false;
            adapter.setNdefPushMessageCallback(callback, activity);
            return true;
        } catch (SecurityException e) {
            Log.w(LOGTAG, e);
        }
        return false;
    }

    public static int getInsetsTopWithoutActionBarHeight(Context context, int top) {
        final int actionBarHeight;
        if (context instanceof AppCompatActivity) {
            actionBarHeight = getActionBarHeight(((AppCompatActivity) context).getSupportActionBar());
        } else if (context instanceof Activity) {
            actionBarHeight = getActionBarHeight(((Activity) context).getActionBar());
        } else {
            return top;
        }
        if (actionBarHeight > top) {
            return top;
        }
        return top - actionBarHeight;
    }

    public static int getInsetsTopWithoutActionBarHeight(Context context, int top, int actionBarHeight) {
        if (actionBarHeight > top) {
            return top;
        }
        return top - actionBarHeight;
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

    public static String replaceLast(final String text, final String regex, final String replacement) {
        if (text == null || regex == null || replacement == null) return text;
        return text.replaceFirst("(?s)" + regex + "(?!.*?" + regex + ")", replacement);
    }

    public static void restartActivity(final Activity activity) {
        if (activity == null) return;
        final int enterAnim = android.R.anim.fade_in;
        final int exitAnim = android.R.anim.fade_out;
        activity.finish();
        activity.overridePendingTransition(enterAnim, exitAnim);
        activity.startActivity(activity.getIntent());
        activity.overridePendingTransition(enterAnim, exitAnim);
    }

    public static void scrollListToPosition(final AbsListView list, final int position) {
        scrollListToPosition(list, position, 0);
    }

    public static void scrollListToPosition(final AbsListView absListView, final int position, final int offset) {
        if (absListView == null) return;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            if (absListView instanceof ListView) {
                final ListView listView = ((ListView) absListView);
                listView.setSelectionFromTop(position, offset);
            } else {
                absListView.setSelection(position);
            }
            stopListView(absListView);
        } else {
            stopListView(absListView);
            if (absListView instanceof ListView) {
                final ListView listView = ((ListView) absListView);
                listView.setSelectionFromTop(position, offset);
            } else {
                absListView.setSelection(position);
            }
        }
    }

    public static void scrollListToTop(final AbsListView list) {
        if (list == null) return;
        scrollListToPosition(list, 0);
    }

    public static void addCopyLinkIntent(Context context, Intent chooserIntent, Uri uri) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;
        final Intent copyLinkIntent = new Intent(context, CopyLinkActivity.class);
        copyLinkIntent.setData(uri);
        final Intent[] alternateIntents = {copyLinkIntent};
        chooserIntent.putExtra(Intent.EXTRA_ALTERNATE_INTENTS, alternateIntents);
    }

    static boolean isMyStatus(ParcelableStatus status) {
        if (isMyRetweet(status)) return true;
        return status.account_key.maybeEquals(status.user_key);
    }

    public static boolean shouldStopAutoRefreshOnBatteryLow(final Context context) {
        final SharedPreferences mPreferences = context.getSharedPreferences(SHARED_PREFERENCES_NAME,
                Context.MODE_PRIVATE);
        return mPreferences.getBoolean(KEY_STOP_AUTO_REFRESH_WHEN_BATTERY_LOW, true);
    }

    public static void showErrorMessage(final Context context, final CharSequence message, final boolean longMessage) {
        if (context == null) return;
        final Toast toast = Toast.makeText(context, message, longMessage ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT);
        toast.show();
    }

    public static void showErrorMessage(final Context context, final CharSequence action,
                                        final CharSequence message, final boolean longMessage) {
        if (context == null) return;
        showErrorMessage(context, getErrorMessage(context, action, message), longMessage);
    }

    public static void showErrorMessage(final Context context, final CharSequence action,
                                        final Throwable t, final boolean longMessage) {
        if (context == null) return;
        if (t instanceof TwitterException) {
            showTwitterErrorMessage(context, action, (TwitterException) t, longMessage);
            return;
        }
        showErrorMessage(context, getErrorMessage(context, action, t), longMessage);
    }

    public static void showErrorMessage(final Context context, final int actionRes, final String desc,
                                        final boolean longMessage) {
        if (context == null) return;
        showErrorMessage(context, context.getString(actionRes), desc, longMessage);
    }

    public static void showErrorMessage(final Context context, final int action, final Throwable t,
                                        final boolean long_message) {
        if (context == null) return;
        showErrorMessage(context, context.getString(action), t, long_message);
    }

    public static void showInfoMessage(final Context context, final CharSequence message, final boolean long_message) {
        if (context == null || isEmpty(message)) return;
        final Toast toast = Toast.makeText(context, message, long_message ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT);
        toast.show();
    }

    public static void showInfoMessage(final Context context, final int resId, final boolean long_message) {
        if (context == null) return;
        showInfoMessage(context, context.getText(resId), long_message);
    }

    public static void showMenuItemToast(final View v, final CharSequence text) {
        final int[] screenPos = new int[2];
        final Rect displayFrame = new Rect();
        v.getLocationOnScreen(screenPos);
        v.getWindowVisibleDisplayFrame(displayFrame);
        final int height = v.getHeight();
        final int midy = screenPos[1] + height / 2;
        showMenuItemToast(v, text, midy >= displayFrame.height());
    }

    public static void showMenuItemToast(final View v, final CharSequence text, final boolean isBottomBar) {
        final int[] screenPos = new int[2];
        final Rect displayFrame = new Rect();
        v.getLocationOnScreen(screenPos);
        v.getWindowVisibleDisplayFrame(displayFrame);
        final int width = v.getWidth();
        final int height = v.getHeight();
        final int screenWidth = v.getResources().getDisplayMetrics().widthPixels;
        final Toast cheatSheet = Toast.makeText(v.getContext().getApplicationContext(), text, Toast.LENGTH_SHORT);
        if (isBottomBar) {
            // Show along the bottom center
            cheatSheet.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, height);
        } else {
            // Show along the top; follow action buttons
            cheatSheet.setGravity(Gravity.TOP | GravityCompat.END, screenWidth - screenPos[0] - width / 2, height);
        }
        cheatSheet.show();
    }

    public static void showOkMessage(final Context context, final CharSequence message, final boolean longMessage) {
        if (context == null || isEmpty(message)) return;
        final Toast toast = Toast.makeText(context, message, longMessage ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT);
        toast.show();
    }

    public static void showOkMessage(final Context context, final int resId, final boolean long_message) {
        if (context == null) return;
        showOkMessage(context, context.getText(resId), long_message);
    }

    public static void showTwitterErrorMessage(final Context context, final CharSequence action,
                                               final TwitterException te, final boolean long_message) {
        if (context == null) return;
        final String message;
        if (te != null) {
            if (action != null) {
                final RateLimitStatus status = te.getRateLimitStatus();
                if (te.exceededRateLimitation() && status != null) {
                    final long secUntilReset = status.getSecondsUntilReset() * 1000;
                    final String nextResetTime = ParseUtils.parseString(getRelativeTimeSpanString(System
                            .currentTimeMillis() + secUntilReset));
                    message = context.getString(R.string.error_message_rate_limit_with_action, action,
                            nextResetTime.trim());
                } else if (isErrorCodeMessageSupported(te)) {
                    final String msg = StatusCodeMessageUtils
                            .getMessage(context, te.getStatusCode(), te.getErrorCode());
                    message = context.getString(R.string.error_message_with_action, action, msg != null ? msg
                            : trimLineBreak(te.getMessage()));
                } else if (te.getCause() instanceof SSLException) {
                    final String msg = te.getCause().getMessage();
                    if (msg != null && msg.contains("!=")) {
                        message = context.getString(R.string.error_message_with_action, action,
                                context.getString(R.string.ssl_error));
                    } else {
                        message = context.getString(R.string.error_message_with_action, action,
                                context.getString(R.string.network_error));
                    }
                } else if (te.getCause() instanceof IOException) {
                    message = context.getString(R.string.error_message_with_action, action,
                            context.getString(R.string.network_error));
                } else {
                    message = context.getString(R.string.error_message_with_action, action,
                            trimLineBreak(te.getMessage()));
                }
            } else {
                message = context.getString(R.string.error_message, trimLineBreak(te.getMessage()));
            }
        } else {
            message = context.getString(R.string.error_unknown_error);
        }
        showErrorMessage(context, message, long_message);
    }

    public static void showWarnMessage(final Context context, final CharSequence message, final boolean longMessage) {
        if (context == null || isEmpty(message)) return;
        final Toast toast = Toast.makeText(context, message, longMessage ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT);
        toast.show();
    }

    public static void showWarnMessage(final Context context, final int resId, final boolean long_message) {
        if (context == null) return;
        showWarnMessage(context, context.getText(resId), long_message);
    }

    public static void startRefreshServiceIfNeeded(@NonNull final Context context) {
        final Context appContext = context.getApplicationContext();
        if (appContext == null) return;
        final Intent refreshServiceIntent = new Intent(appContext, RefreshService.class);
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                if (isNetworkAvailable(appContext) && hasAutoRefreshAccounts(appContext)) {
                    if (BuildConfig.DEBUG) {
                        Log.d(LOGTAG, "Start background refresh service");
                    }
                    appContext.startService(refreshServiceIntent);
                } else {
                    appContext.stopService(refreshServiceIntent);
                }
            }
        });
    }

    public static void startStatusShareChooser(final Context context, final ParcelableStatus status) {
        final Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        final String name = status.user_name, screenName = status.user_screen_name;
        final String timeString = formatToLongTimeString(context, status.timestamp);
        final String subject = context.getString(R.string.status_share_subject_format_with_time, name, screenName, timeString);
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, status.text_plain);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.share)));
    }

    public static void stopListView(final AbsListView list) {
        if (list == null) return;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            list.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
                    MotionEvent.ACTION_CANCEL, 0, 0, 0));
        } else {
            list.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
                    MotionEvent.ACTION_DOWN, 0, 0, 0));
            list.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
                    MotionEvent.ACTION_UP, 0, 0, 0));
        }
    }

    public static String trim(final String str) {
        return str != null ? str.trim() : null;
    }

    public static String trimLineBreak(final String orig) {
        if (orig == null) return null;
        return orig.replaceAll("\\n+", "\n");
    }

    public static boolean truncateMessages(final List<DirectMessage> in, final List<DirectMessage> out,
                                           final long sinceId) {
        if (in == null) return false;
        for (final DirectMessage message : in) {
            if (sinceId > 0 && message.getId() <= sinceId) {
                continue;
            }
            out.add(message);
        }
        return in.size() != out.size();
    }

    public static boolean truncateStatuses(final List<Status> in, final List<Status> out,
                                           final long sinceId) {
        if (in == null) return false;
        for (final Status status : in) {
            if (sinceId > 0 && status.getId() <= sinceId) {
                continue;
            }
            out.add(status);
        }
        return in.size() != out.size();
    }

    public static void updateRelationship(Context context, Relationship relationship, UserKey accountId) {
        final ContentResolver resolver = context.getContentResolver();
        final ContentValues values = ContentValuesCreator.createCachedRelationship(relationship, accountId);
        resolver.insert(CachedRelationships.CONTENT_URI, values);
    }

    public static boolean useShareScreenshot() {
        return Boolean.parseBoolean("false");
    }

    private static Drawable getMetadataDrawable(final PackageManager pm, final ActivityInfo info, final String key) {
        if (pm == null || info == null || info.metaData == null || key == null || !info.metaData.containsKey(key))
            return null;
        return pm.getDrawable(info.packageName, info.metaData.getInt(key), info.applicationInfo);
    }

    private static boolean isErrorCodeMessageSupported(final TwitterException te) {
        if (te == null) return false;
        return StatusCodeMessageUtils.containsHttpStatus(te.getStatusCode())
                || StatusCodeMessageUtils.containsTwitterError(te.getErrorCode());
    }

    public static int getActionBarHeight(@Nullable ActionBar actionBar) {
        if (actionBar == null) return 0;
        final Context context = actionBar.getThemedContext();
        final TypedValue tv = new TypedValue();
        final int height = actionBar.getHeight();
        if (height > 0) return height;
        if (context.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            return TypedValue.complexToDimensionPixelSize(tv.data, context.getResources().getDisplayMetrics());
        }
        return 0;
    }

    public static String parseURLEntities(String text, final UrlEntity[] entities) {
        for (UrlEntity entity : entities) {
            final int start = entity.getStart(), end = entity.getEnd();
            final String displayUrl = entity.getDisplayUrl();
            if (displayUrl != null && !displayUrl.isEmpty() && start >= 0 && end >= 0) {
                StringBuffer bf = new StringBuffer(text);
                return bf.replace(start, end, displayUrl).toString();
            }
        }
        return text;
    }

    public static String parseString(final Object object) {
        if (object == null) return null;
        return String.valueOf(object);
    }

    public static void retweet(ParcelableStatus status, AsyncTwitterWrapper twitter) {
        if (isMyRetweet(status)) {
            twitter.cancelRetweetAsync(status.account_key,
                    status.id, status.my_retweet_id);
        } else {
            twitter.retweetStatusAsync(status.account_key,
                    status.id);
        }
    }

    public static void favorite(ParcelableStatus status, AsyncTwitterWrapper twitter, MenuItem item) {
        if (status.is_favorite) {
            twitter.destroyFavoriteAsync(status.account_key, status.id);
        } else {
            ActionProvider provider = MenuItemCompat.getActionProvider(item);
            if (provider instanceof FavoriteItemProvider) {
                ((FavoriteItemProvider) provider).invokeItem(item,
                        new AbsStatusesFragment.DefaultOnLikedListener(twitter, status));
            } else {
                twitter.createFavoriteAsync(status.account_key, status.id);
            }
        }
    }

    public static int getActionBarHeight(@Nullable android.support.v7.app.ActionBar actionBar) {
        if (actionBar == null) return 0;
        final Context context = actionBar.getThemedContext();
        final TypedValue tv = new TypedValue();
        final int height = actionBar.getHeight();
        if (height > 0) return height;
        if (context.getTheme().resolveAttribute(R.attr.actionBarSize, tv, true)) {
            return TypedValue.complexToDimensionPixelSize(tv.data, context.getResources().getDisplayMetrics());
        }
        return 0;
    }

    public static void makeListFragmentFitsSystemWindows(ListFragment fragment) {
        final FragmentActivity activity = fragment.getActivity();
        if (!(activity instanceof SystemWindowsInsetsCallback)) return;
        final SystemWindowsInsetsCallback callback = (SystemWindowsInsetsCallback) activity;
        final Rect insets = new Rect();
        if (callback.getSystemWindowsInsets(insets)) {
            makeListFragmentFitsSystemWindows(fragment, insets);
        }
    }


    public static void makeListFragmentFitsSystemWindows(ListFragment fragment, Rect insets) {
        final ListView listView = fragment.getListView();
        listView.setPadding(insets.left, insets.top, insets.right, insets.bottom);
        listView.setClipToPadding(false);
    }

    @Nullable
    public static ParcelableUser getUserForConversation(@NonNull final Context context,
                                                        @NonNull final UserKey accountKey,
                                                        final long conversationId) {
        final ContentResolver cr = context.getContentResolver();
        final Expression where = Expression.and(Expression.equalsArgs(ConversationEntries.ACCOUNT_KEY),
                Expression.equalsArgs(ConversationEntries.CONVERSATION_ID));
        final String[] whereArgs = {accountKey.toString(), String.valueOf(conversationId)};
        final Cursor c = cr.query(ConversationEntries.CONTENT_URI, null, where.getSQL(), whereArgs,
                null);
        if (c == null) return null;
        try {
            if (c.moveToFirst()) return ParcelableUserUtils.fromDirectMessageConversationEntry(c);
        } finally {
            c.close();
        }
        return null;
    }

    @SafeVarargs
    public static Bundle makeSceneTransitionOption(final Activity activity,
                                                   final Pair<View, String>... sharedElements) {
        if (ThemeUtils.isTransparentBackground(activity)) return null;
        return ActivityOptionsCompat.makeSceneTransitionAnimation(activity, sharedElements).toBundle();
    }


    public static void setSharedElementTransition(Context context, Window window, int transitionRes) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return;
        UtilsL.setSharedElementTransition(context, window, transitionRes);
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

    public static <T> Object findFieldOfTypes(T obj, Class<? extends T> cls, Class<?>... checkTypes) {
        labelField:
        for (Field field : cls.getDeclaredFields()) {
            field.setAccessible(true);
            final Object fieldObj;
            try {
                fieldObj = field.get(obj);
            } catch (Exception ignore) {
                continue;
            }
            if (fieldObj != null) {
                final Class<?> type = fieldObj.getClass();
                for (Class<?> checkType : checkTypes) {
                    if (!checkType.isAssignableFrom(type)) continue labelField;
                }
                return fieldObj;
            }
        }
        return null;
    }

    public static boolean isCustomConsumerKeySecret(String consumerKey, String consumerSecret) {
        if (TextUtils.isEmpty(consumerKey) || TextUtils.isEmpty(consumerSecret)) return false;
        return (!TWITTER_CONSUMER_KEY.equals(consumerKey) && !TWITTER_CONSUMER_SECRET.equals(consumerKey))
                && (!TWITTER_CONSUMER_KEY.equals(consumerKey) && !TWITTER_CONSUMER_SECRET.equals(consumerSecret));
    }

    public static int getErrorNo(Throwable t) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return 0;
        return UtilsL.getErrorNo(t);
    }

    public static boolean isOutOfMemory(Throwable ex) {
        if (ex == null) return false;
        final Throwable cause = ex.getCause();
        if (cause == null || cause == ex) return false;
        if (cause instanceof OutOfMemoryError) return true;
        return isOutOfMemory(cause);
    }

    public static boolean hasOfficialAPIAccess(@NonNull Context context, @NonNull ParcelableCredentials account) {
        if (ParcelableCredentials.ACCOUNT_TYPE_TWITTER.equals(account.account_type)) {
            final TwitterAccountExtra extra = JsonSerializer.parse(account.account_extras,
                    TwitterAccountExtra.class);
            if (extra != null) {
                return extra.isOfficialCredentials();
            }
        }
        final boolean isOAuth = ParcelableCredentialsUtils.isOAuth(account.auth_type);
        final String consumerKey = account.consumer_key, consumerSecret = account.consumer_secret;
        return isOAuth && TwitterContentUtils.isOfficialKey(context, consumerKey, consumerSecret);
    }

    public static int getNotificationId(int baseId, @Nullable UserKey accountId) {
        int result = baseId;
        result = 31 * result + (accountId != null ? accountId.hashCode() : 0);
        return result;
    }

    @SuppressLint("InlinedApi")
    public static boolean isCharging(final Context context) {
        if (context == null) return false;
        final Intent intent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (intent == null) return false;
        final int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        return plugged == BatteryManager.BATTERY_PLUGGED_AC
                || plugged == BatteryManager.BATTERY_PLUGGED_USB
                || plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS;
    }

    public static boolean isMediaPreviewEnabled(Context context, SharedPreferencesWrapper preferences) {
        if (!preferences.getBoolean(KEY_MEDIA_PREVIEW)) return false;
        final ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return !ConnectivityManagerCompat.isActiveNetworkMetered(cm) || !preferences.getBoolean(KEY_BANDWIDTH_SAVING_MODE);
    }

    public static Expression getAccountCompareExpression() {
        return Expression.equalsArgs(AccountSupportColumns.ACCOUNT_KEY);
    }

    static class UtilsL {

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        static void setSharedElementTransition(Context context, Window window, int transitionRes) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return;
            window.requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS);
            final TransitionInflater inflater = TransitionInflater.from(context);
            final Transition transition = inflater.inflateTransition(transitionRes);
            window.setSharedElementEnterTransition(transition);
            window.setSharedElementExitTransition(transition);
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        public static int getErrorNo(Throwable t) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return 0;
            if (t instanceof ErrnoException) {
                return ((ErrnoException) t).errno;
            }
            return 0;
        }
    }

    /**
     * Send Notifications to Pebble smartwatches
     *
     * @param context Context
     * @param message String
     */
    public static void sendPebbleNotification(final Context context, final String message) {
        if (context == null || TextUtils.isEmpty(message)) return;
        final SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);

        if (prefs.getBoolean(KEY_PEBBLE_NOTIFICATIONS, false)) {

            final String appName = context.getString(R.string.app_name);

            final List<PebbleMessage> messages = new ArrayList<>();
            messages.add(new PebbleMessage(appName, message));

            final Intent intent = new Intent(INTENT_ACTION_PEBBLE_NOTIFICATION);
            intent.putExtra("messageType", "PEBBLE_ALERT");
            intent.putExtra("sender", appName);
            intent.putExtra("notificationData", JsonSerializer.serialize(messages, PebbleMessage.class));

            context.getApplicationContext().sendBroadcast(intent);
        }
    }

    @Nullable
    public static GeoLocation getCachedGeoLocation(Context context) {
        final Location location = getCachedLocation(context);
        if (location == null) return null;
        return new GeoLocation(location.getLatitude(), location.getLongitude());
    }

    @Nullable
    public static Location getCachedLocation(Context context) {
        Location location = null;
        final LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (lm == null) return null;
        try {
            location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        } catch (SecurityException ignore) {

        }
        if (location != null) return location;
        try {
            location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        } catch (SecurityException ignore) {

        }
        return location;
    }

    public static boolean checkPlayServices(final Activity activity) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(activity);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(activity, resultCode, 9000)
                        .show();
            } else {
                Log.i("PlayServices", "This device is not supported.");
            }
            return false;
        }
        return true;
    }
}