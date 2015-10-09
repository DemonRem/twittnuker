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

package de.vanita5.twittnuker.adapter;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v4.util.Pair;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import de.vanita5.twittnuker.Constants;
import de.vanita5.twittnuker.R;
import de.vanita5.twittnuker.adapter.iface.IActivitiesAdapter;
import de.vanita5.twittnuker.api.twitter.model.Activity;
import de.vanita5.twittnuker.app.TwittnukerApplication;
import de.vanita5.twittnuker.fragment.support.UserFragment;
import de.vanita5.twittnuker.model.ParcelableActivity;
import de.vanita5.twittnuker.model.ParcelableMedia;
import de.vanita5.twittnuker.model.ParcelableStatus;
import de.vanita5.twittnuker.util.AsyncTwitterWrapper;
import de.vanita5.twittnuker.util.MediaLoaderWrapper;
import de.vanita5.twittnuker.util.MediaLoadingHandler;
import de.vanita5.twittnuker.util.SharedPreferencesWrapper;
import de.vanita5.twittnuker.util.ThemeUtils;
import de.vanita5.twittnuker.util.TwidereLinkify;
import de.vanita5.twittnuker.util.TwidereLinkify.OnLinkClickListener;
import de.vanita5.twittnuker.util.UserColorNameManager;
import de.vanita5.twittnuker.util.Utils;
import de.vanita5.twittnuker.view.holder.ActivityTitleSummaryViewHolder;
import de.vanita5.twittnuker.view.holder.GapViewHolder;
import de.vanita5.twittnuker.view.holder.LoadIndicatorViewHolder;
import de.vanita5.twittnuker.view.holder.StatusViewHolder;
import de.vanita5.twittnuker.view.holder.StatusViewHolder.DummyStatusHolderAdapter;
import de.vanita5.twittnuker.view.holder.StatusViewHolder.StatusClickListener;

public abstract class AbsActivitiesAdapter<Data> extends LoadMoreSupportAdapter<ViewHolder> implements Constants,
        IActivitiesAdapter<Data>, StatusClickListener, OnLinkClickListener {

    private static final int ITEM_VIEW_TYPE_STUB = 0;
    private static final int ITEM_VIEW_TYPE_GAP = 1;
    private static final int ITEM_VIEW_TYPE_LOAD_INDICATOR = 2;
    private static final int ITEM_VIEW_TYPE_TITLE_SUMMARY = 3;
    private static final int ITEM_VIEW_TYPE_STATUS = 4;

    private final Context mContext;
    private final LayoutInflater mInflater;
    private final MediaLoadingHandler mLoadingHandler;
    private final int mCardBackgroundColor;
    private final int mTextSize;
    private final int mProfileImageStyle, mMediaPreviewStyle, mLinkHighlightingStyle;
    private final boolean mCompactCards;
    private final boolean mDisplayMediaPreview;
    private final boolean mNameFirst;
    private final boolean mDisplayProfileImage;
    private final TwidereLinkify mLinkify;
    private final DummyStatusHolderAdapter mStatusAdapterDelegate;
    private final UserColorNameManager mUserColorNameManager;
    private ActivityAdapterListener mActivityAdapterListener;

    protected AbsActivitiesAdapter(final Context context, boolean compact) {
        super(context);
        mContext = context;
        final TwittnukerApplication app = TwittnukerApplication.getInstance(context);
        mCardBackgroundColor = ThemeUtils.getCardBackgroundColor(context, ThemeUtils.getThemeBackgroundOption(context), ThemeUtils.getUserThemeBackgroundAlpha(context));
        mInflater = LayoutInflater.from(context);
        mLoadingHandler = new MediaLoadingHandler(R.id.media_preview_progress);
        mUserColorNameManager = app.getUserColorNameManager();
        final SharedPreferencesWrapper preferences = SharedPreferencesWrapper.getInstance(context,
                SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        mTextSize = preferences.getInt(KEY_TEXT_SIZE, context.getResources().getInteger(R.integer.default_text_size));
        mCompactCards = compact;
        mProfileImageStyle = Utils.getProfileImageStyle(preferences.getString(KEY_PROFILE_IMAGE_STYLE, null));
        mMediaPreviewStyle = Utils.getMediaPreviewStyle(preferences.getString(KEY_MEDIA_PREVIEW_STYLE, null));
        mLinkHighlightingStyle = Utils.getLinkHighlightingStyleInt(preferences.getString(KEY_LINK_HIGHLIGHT_OPTION, null));
        mDisplayProfileImage = preferences.getBoolean(KEY_DISPLAY_PROFILE_IMAGE, true);
        mDisplayMediaPreview = preferences.getBoolean(KEY_MEDIA_PREVIEW, false);
        mNameFirst = preferences.getBoolean(KEY_NAME_FIRST, true);
        mLinkify = new TwidereLinkify(this);
        mStatusAdapterDelegate = new DummyStatusHolderAdapter(context);
    }

    @Override
    public abstract ParcelableActivity getActivity(int position);

    @Override
    public abstract int getActivityCount();

    public abstract Data getData();

    @Override
    public abstract void setData(Data data);

    @NonNull
    @Override
    public MediaLoaderWrapper getMediaLoader() {
        return mMediaLoader;
    }

    @NonNull
    @Override
    public Context getContext() {
        return mContext;
    }

    @Override
    public MediaLoadingHandler getMediaLoadingHandler() {
        return mLoadingHandler;
    }

    @Override
    public int getProfileImageStyle() {
        return mProfileImageStyle;
    }

    @Override
    public int getMediaPreviewStyle() {
        return mMediaPreviewStyle;
    }

    @NonNull
    @Override
    public AsyncTwitterWrapper getTwitterWrapper() {
        return mTwitterWrapper;
    }

    @Override
    public float getTextSize() {
        return mTextSize;
    }

    public int getLinkHighlightingStyle() {
        return mLinkHighlightingStyle;
    }

    public TwidereLinkify getLinkify() {
        return mLinkify;
    }

    public boolean isNameFirst() {
        return mNameFirst;
    }

    @Override
    public boolean isProfileImageEnabled() {
        return mDisplayProfileImage;
    }

    @Override
    public void onStatusClick(StatusViewHolder holder, int position) {
        final ParcelableActivity activity = getActivity(position);
        final ParcelableStatus status;
        if (activity.action == Activity.ACTION_MENTION) {
            status = activity.target_object_statuses[0];
        } else {
            status = activity.target_statuses[0];
        }
        Utils.openStatus(getContext(), status, null);
    }

    @Override
    public void onMediaClick(StatusViewHolder holder, View view, ParcelableMedia media, int position) {

    }

    @Override
    public void onUserProfileClick(StatusViewHolder holder, int position) {
        final Context context = getContext();
        final ParcelableActivity activity = getActivity(position);
        final ParcelableStatus status;
        if (activity.action == Activity.ACTION_MENTION) {
            status = activity.target_object_statuses[0];
        } else {
            status = activity.target_statuses[0];
        }
        final View profileImageView = holder.getProfileImageView();
        final View profileTypeView = holder.getProfileTypeView();
        if (context instanceof FragmentActivity) {
            final Bundle options = Utils.makeSceneTransitionOption((FragmentActivity) context,
                    new Pair<>(profileImageView, UserFragment.TRANSITION_NAME_PROFILE_IMAGE),
                    new Pair<>(profileTypeView, UserFragment.TRANSITION_NAME_PROFILE_TYPE));
            Utils.openUserProfile(context, status.account_id, status.user_id, status.user_screen_name, options);
        } else {
            Utils.openUserProfile(context, status.account_id, status.user_id, status.user_screen_name, null);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case ITEM_VIEW_TYPE_STATUS: {
                final View view;
                if (mCompactCards) {
                    view = mInflater.inflate(R.layout.card_item_status_compact, parent, false);
                    final View itemContent = view.findViewById(R.id.item_content);
                    itemContent.setBackgroundColor(mCardBackgroundColor);
                } else {
                    view = mInflater.inflate(R.layout.card_item_status, parent, false);
                    final CardView cardView = (CardView) view.findViewById(R.id.card);
                    cardView.setCardBackgroundColor(mCardBackgroundColor);
                }
                final StatusViewHolder holder = new StatusViewHolder(mStatusAdapterDelegate, view);
                holder.setTextSize(getTextSize());
                holder.setStatusClickListener(this);
                return holder;
            }
            case ITEM_VIEW_TYPE_TITLE_SUMMARY: {
                final View view;
                if (mCompactCards) {
                    view = mInflater.inflate(R.layout.card_item_activity_summary_compact, parent, false);
                } else {
                    view = mInflater.inflate(R.layout.card_item_activity_summary, parent, false);
                    final CardView cardView = (CardView) view.findViewById(R.id.card);
                    cardView.setCardBackgroundColor(mCardBackgroundColor);
                }
                final ActivityTitleSummaryViewHolder holder = new ActivityTitleSummaryViewHolder(this, view);
                holder.setTextSize(getTextSize());
                return holder;
            }
            case ITEM_VIEW_TYPE_GAP: {
                final View view = mInflater.inflate(R.layout.card_item_gap, parent, false);
                return new GapViewHolder(this, view);
            }
            case ITEM_VIEW_TYPE_LOAD_INDICATOR: {
                final View view = mInflater.inflate(R.layout.card_item_load_indicator, parent, false);
                return new LoadIndicatorViewHolder(view);
            }
            default: {
                final View view = mInflater.inflate(R.layout.list_item_two_line, parent, false);
                return new StubViewHolder(view);
            }
        }
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        switch (getItemViewType(position)) {
            case ITEM_VIEW_TYPE_STATUS: {
                final ParcelableActivity activity = getActivity(position);
                final ParcelableStatus status;
                if (activity.action == Activity.ACTION_MENTION) {
                    status = activity.target_object_statuses[0];
                } else {
                    status = activity.target_statuses[0];
                }
                final StatusViewHolder statusViewHolder = (StatusViewHolder) holder;
                statusViewHolder.displayStatus(status, null, true, true);
                break;
            }
            case ITEM_VIEW_TYPE_TITLE_SUMMARY: {
                bindTitleSummaryViewHolder((ActivityTitleSummaryViewHolder) holder, position);
                break;
            }
            case ITEM_VIEW_TYPE_STUB: {
                ((StubViewHolder) holder).displayActivity(getActivity(position));
                break;
            }
        }
    }

    @Override
    public boolean onStatusLongClick(StatusViewHolder holder, int position) {
        return false;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == getActivityCount()) {
            return ITEM_VIEW_TYPE_LOAD_INDICATOR;
        } else if (isGapItem(position)) {
            return ITEM_VIEW_TYPE_GAP;
        }
        switch (getActivityAction(position)) {
            case Activity.ACTION_MENTION:
            case Activity.ACTION_REPLY:
            case Activity.ACTION_QUOTE: {
                return ITEM_VIEW_TYPE_STATUS;
            }
            case Activity.ACTION_FOLLOW:
            case Activity.ACTION_FAVORITE:
            case Activity.ACTION_RETWEET:
            case Activity.ACTION_FAVORITED_RETWEET:
            case Activity.ACTION_RETWEETED_RETWEET:
            case Activity.ACTION_RETWEETED_MENTION:
            case Activity.ACTION_FAVORITED_MENTION:
            case Activity.ACTION_LIST_MEMBER_ADDED: {
                return ITEM_VIEW_TYPE_TITLE_SUMMARY;
            }
        }
        return ITEM_VIEW_TYPE_STUB;
    }

    @Override
    public final int getItemCount() {
        return getActivityCount() + (isLoadMoreIndicatorVisible() ? 1 : 0);
    }

    @Override
    public void onGapClick(ViewHolder holder, int position) {
        if (mActivityAdapterListener != null) {
            mActivityAdapterListener.onGapClick((GapViewHolder) holder, position);
        }
    }

    @Override
    public void onItemActionClick(ViewHolder holder, int id, int position) {

    }

    @Override
    public void onItemMenuClick(ViewHolder holder, View menuView, int position) {

    }

    @Override
    public void onLinkClick(String link, String orig, long accountId, long extraId, int type, boolean sensitive, int start, int end) {

    }

    @NonNull
    @Override
    public UserColorNameManager getUserColorNameManager() {
        return mUserColorNameManager;
    }

    public void setListener(ActivityAdapterListener listener) {
        mActivityAdapterListener = listener;
    }

    protected abstract void bindTitleSummaryViewHolder(ActivityTitleSummaryViewHolder holder, int position);

    protected abstract int getActivityAction(int position);

    private boolean isMediaPreviewEnabled() {
        return mDisplayMediaPreview;
    }

    public interface ActivityAdapterListener {
        void onGapClick(GapViewHolder holder, int position);
    }

    private static class StubViewHolder extends ViewHolder {

        private final TextView text1, text2;

        public StubViewHolder(View itemView) {
            super(itemView);
            text1 = (TextView) itemView.findViewById(android.R.id.text1);
            text2 = (TextView) itemView.findViewById(android.R.id.text2);
        }

        public void displayActivity(ParcelableActivity activity) {
            text1.setText(String.valueOf(activity.action));
            text2.setText(activity.toString());
        }
    }


}