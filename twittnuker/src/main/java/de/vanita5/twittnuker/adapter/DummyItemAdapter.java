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

package de.vanita5.twittnuker.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.text.BidiFormatter;
import android.support.v7.widget.RecyclerView;

import de.vanita5.twittnuker.R;
import de.vanita5.twittnuker.TwittnukerConstants;
import de.vanita5.twittnuker.adapter.iface.IStatusesAdapter;
import de.vanita5.twittnuker.adapter.iface.IUserListsAdapter;
import de.vanita5.twittnuker.adapter.iface.IUsersAdapter;
import de.vanita5.twittnuker.constant.SharedPreferenceConstants;
import de.vanita5.twittnuker.model.ParcelableStatus;
import de.vanita5.twittnuker.model.ParcelableUser;
import de.vanita5.twittnuker.model.ParcelableUserList;
import de.vanita5.twittnuker.model.UserKey;
import de.vanita5.twittnuker.util.AsyncTwitterWrapper;
import de.vanita5.twittnuker.util.MediaLoaderWrapper;
import de.vanita5.twittnuker.util.MediaLoadingHandler;
import de.vanita5.twittnuker.util.SharedPreferencesWrapper;
import de.vanita5.twittnuker.util.TwidereLinkify;
import de.vanita5.twittnuker.util.UserColorNameManager;
import de.vanita5.twittnuker.util.Utils;
import de.vanita5.twittnuker.util.dagger.GeneralComponentHelper;
import de.vanita5.twittnuker.view.holder.iface.IStatusViewHolder;

import javax.inject.Inject;

public final class DummyItemAdapter implements IStatusesAdapter<Object>,
        IUsersAdapter<Object>, IUserListsAdapter<Object>, SharedPreferenceConstants {

    private final Context context;
    private final SharedPreferencesWrapper preferences;
    private final TwidereLinkify linkify;
    private final MediaLoadingHandler handler;
    @Nullable
    private final RecyclerView.Adapter<? extends RecyclerView.ViewHolder> adapter;
    @Inject
    MediaLoaderWrapper loader;
    @Inject
    AsyncTwitterWrapper twitter;
    @Inject
    UserColorNameManager manager;
    @Inject
    BidiFormatter formatter;

    private int profileImageStyle;
    private int mediaPreviewStyle;
    private int textSize;
    private int linkHighlightStyle;
    private boolean nameFirst;
    private boolean displayProfileImage;
    private boolean sensitiveContentEnabled;
    private boolean showCardActions;
    private boolean displayMediaPreview;
    private boolean shouldShowAccountsColor;
    private boolean useStarsForLikes;
    private boolean showAbsoluteTime;
    private int showingActionCardPosition = RecyclerView.NO_POSITION;
    private FollowClickListener followClickListener;
    private RequestClickListener requestClickListener;
    private IStatusViewHolder.StatusClickListener statusClickListener;
    private UserClickListener userClickListener;

    public DummyItemAdapter(Context context) {
        this(context, new TwidereLinkify(null), null);
    }

    public DummyItemAdapter(Context context, TwidereLinkify linkify,
                            @Nullable RecyclerView.Adapter<? extends RecyclerView.ViewHolder> adapter) {
        GeneralComponentHelper.build(context).inject(this);
        this.context = context;
        preferences = SharedPreferencesWrapper.getInstance(context, TwittnukerConstants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        handler = new MediaLoadingHandler(R.id.media_preview_progress);
        this.linkify = linkify;
        this.adapter = adapter;
        updateOptions();
    }

    public void setShouldShowAccountsColor(boolean shouldShowAccountsColor) {
        this.shouldShowAccountsColor = shouldShowAccountsColor;
    }

    @NonNull
    @Override
    public Context getContext() {
        return context;
    }

    @NonNull
    @Override
    public MediaLoaderWrapper getMediaLoader() {
        return loader;
    }

    @NonNull
    @Override
    public BidiFormatter getBidiFormatter() {
        return formatter;
    }

    @Override
    public MediaLoadingHandler getMediaLoadingHandler() {
        return handler;
    }

    @NonNull
    @Override
    public UserColorNameManager getUserColorNameManager() {
        return manager;
    }

    @Nullable
    @Override
    public UserListClickListener getUserListClickListener() {
        return null;
    }

    @Nullable
    @Override
    public IStatusViewHolder.StatusClickListener getStatusClickListener() {
        return statusClickListener;
    }

    public void setStatusClickListener(IStatusViewHolder.StatusClickListener statusClickListener) {
        this.statusClickListener = statusClickListener;
    }

    @Override
    public int getItemCount() {
        return 0;
    }

    @Override
    public int getProfileImageStyle() {
        return profileImageStyle;
    }

    @Override
    public int getMediaPreviewStyle() {
        return mediaPreviewStyle;
    }

    @NonNull
    @Override
    public AsyncTwitterWrapper getTwitterWrapper() {
        return twitter;
    }

    @Override
    public float getTextSize() {
        return textSize;
    }

    @Nullable
    public UserClickListener getUserClickListener() {
        return userClickListener;
    }

    public void setUserClickListener(UserClickListener userClickListener) {
        this.userClickListener = userClickListener;
    }

    @Override
    @IndicatorPosition
    public int getLoadMoreIndicatorPosition() {
        return IndicatorPosition.NONE;
    }

    @Override
    public void setLoadMoreIndicatorPosition(@IndicatorPosition int position) {

    }

    @Override
    @IndicatorPosition
    public int getLoadMoreSupportedPosition() {
        return IndicatorPosition.NONE;
    }

    @Override
    public void setLoadMoreSupportedPosition(@IndicatorPosition int supported) {

    }

    @Override
    public ParcelableStatus getStatus(int position) {
        if (adapter instanceof ParcelableStatusesAdapter) {
            return ((ParcelableStatusesAdapter) adapter).getStatus(position);
        } else if (adapter instanceof VariousItemsAdapter) {
            return (ParcelableStatus) ((VariousItemsAdapter) adapter).getItem(position);
        }
        return null;
    }

    @Override
    public int getStatusCount() {
        return 0;
    }

    @Override
    public int getRawStatusCount() {
        return 0;
    }

    @Nullable
    @Override
    public String getStatusId(int position) {
        return null;
    }

    @Override
    public long getStatusTimestamp(int adapterPosition) {
        return -1;
    }

    @Override
    public long getStatusPositionKey(int adapterPosition) {
        return -1;
    }

    @Override
    @Nullable
    public UserKey getAccountKey(int position) {
        return null;
    }

    @Nullable
    @Override
    public ParcelableStatus findStatusById(UserKey accountId, String statusId) {
        return null;
    }

    @Override
    public TwidereLinkify getTwidereLinkify() {
        return linkify;
    }

    @Override
    public boolean isMediaPreviewEnabled() {
        return displayMediaPreview;
    }

    public void setMediaPreviewEnabled(boolean enabled) {
        displayMediaPreview = enabled;
    }

    @Override
    public int getLinkHighlightingStyle() {
        return linkHighlightStyle;
    }

    @Override
    public boolean isNameFirst() {
        return nameFirst;
    }

    @Override
    public boolean isSensitiveContentEnabled() {
        return sensitiveContentEnabled;
    }

    @Override
    public boolean isCardActionsShown(int position) {
        if (position == RecyclerView.NO_POSITION) return showCardActions;
        return showCardActions || showingActionCardPosition == position;
    }

    @Override
    public void showCardActions(int position) {
        if (showingActionCardPosition != RecyclerView.NO_POSITION && adapter != null) {
            adapter.notifyItemChanged(showingActionCardPosition);
        }
        showingActionCardPosition = position;
        if (position != RecyclerView.NO_POSITION && adapter != null) {
            adapter.notifyItemChanged(position);
        }
    }

    @Override
    public ParcelableUser getUser(int position) {
        if (adapter instanceof ParcelableUsersAdapter) {
            return ((ParcelableUsersAdapter) adapter).getUser(position);
        } else if (adapter instanceof VariousItemsAdapter) {
            return (ParcelableUser) ((VariousItemsAdapter) adapter).getItem(position);
        }
        return null;
    }

    @Nullable
    @Override
    public String getUserId(int position) {
        return null;
    }

    @Override
    public int getUserCount() {
        return 0;
    }

    @Override
    public ParcelableUserList getUserList(int position) {
        return null;
    }

    @Override
    public long getUserListId(int position) {
        return 0;
    }

    @Override
    public int getUserListsCount() {
        return 0;
    }

    @Override
    public boolean setData(Object o) {
        return false;
    }


    @Override
    public RequestClickListener getRequestClickListener() {
        return requestClickListener;
    }

    public void setRequestClickListener(RequestClickListener requestClickListener) {
        this.requestClickListener = requestClickListener;
    }

    @Override
    public FollowClickListener getFollowClickListener() {
        return followClickListener;
    }

    public void setFollowClickListener(FollowClickListener followClickListener) {
        this.followClickListener = followClickListener;
    }

    @Override
    public boolean shouldUseStarsForLikes() {
        return useStarsForLikes;
    }

    public void setUseStarsForLikes(boolean useStarsForLikes) {
        this.useStarsForLikes = useStarsForLikes;
    }

    @Override
    public boolean shouldShowAccountsColor() {
        return shouldShowAccountsColor;
    }

    @Override
    public boolean isGapItem(int position) {
        return false;
    }

    @Override
    public GapClickListener getGapClickListener() {
        return null;
    }

    @Override
    public boolean isShowAbsoluteTime() {
        return showAbsoluteTime;
    }

    public void setShowAbsoluteTime(boolean showAbsoluteTime) {
        this.showAbsoluteTime = showAbsoluteTime;
    }

    @Override
    public boolean isProfileImageEnabled() {
        return displayProfileImage;
    }

    public void updateOptions() {
        profileImageStyle = Utils.getProfileImageStyle(preferences.getString(KEY_PROFILE_IMAGE_STYLE, null));
        mediaPreviewStyle = Utils.getMediaPreviewStyle(preferences.getString(KEY_MEDIA_PREVIEW_STYLE, null));
        textSize = preferences.getInt(KEY_TEXT_SIZE, context.getResources().getInteger(R.integer.default_text_size));
        nameFirst = preferences.getBoolean(KEY_NAME_FIRST, true);
        displayProfileImage = preferences.getBoolean(KEY_DISPLAY_PROFILE_IMAGE, true);
        setMediaPreviewEnabled(preferences.getBoolean(KEY_MEDIA_PREVIEW, false));
        sensitiveContentEnabled = preferences.getBoolean(KEY_DISPLAY_SENSITIVE_CONTENTS, false);
        showCardActions = !preferences.getBoolean(KEY_HIDE_CARD_ACTIONS, false);
        linkHighlightStyle = Utils.getLinkHighlightingStyleInt(preferences.getString(KEY_LINK_HIGHLIGHT_OPTION, null));
        setUseStarsForLikes(preferences.getBoolean(KEY_I_WANT_MY_STARS_BACK));
        setShowAbsoluteTime(preferences.getBoolean(KEY_SHOW_ABSOLUTE_TIME));
    }
}