/*
 * Twittnuker - Twitter client for Android
 *
 * Copyright (C) 2013-2015 vanita5 <mail@vanit.as>
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
import de.vanita5.twittnuker.fragment.support.UserFragment;
import de.vanita5.twittnuker.model.ParcelableActivity;
import de.vanita5.twittnuker.model.ParcelableMedia;
import de.vanita5.twittnuker.model.ParcelableStatus;
import de.vanita5.twittnuker.util.MediaLoadingHandler;
import de.vanita5.twittnuker.util.ThemeUtils;
import de.vanita5.twittnuker.util.TwidereLinkify;
import de.vanita5.twittnuker.util.TwidereLinkify.OnLinkClickListener;
import de.vanita5.twittnuker.util.Utils;
import de.vanita5.twittnuker.view.holder.ActivityTitleSummaryViewHolder;
import de.vanita5.twittnuker.view.holder.GapViewHolder;
import de.vanita5.twittnuker.view.holder.LoadIndicatorViewHolder;
import de.vanita5.twittnuker.view.holder.StatusViewHolder;
import de.vanita5.twittnuker.view.holder.StatusViewHolder.DummyStatusHolderAdapter;
import de.vanita5.twittnuker.view.holder.iface.IStatusViewHolder;

public abstract class AbsActivitiesAdapter<Data> extends LoadMoreSupportAdapter<ViewHolder> implements Constants,
        IActivitiesAdapter<Data>, IStatusViewHolder.StatusClickListener, OnLinkClickListener,
        ActivityTitleSummaryViewHolder.ActivityClickListener {

    private static final int ITEM_VIEW_TYPE_STUB = 0;
    private static final int ITEM_VIEW_TYPE_GAP = 1;
    private static final int ITEM_VIEW_TYPE_LOAD_INDICATOR = 2;
    private static final int ITEM_VIEW_TYPE_TITLE_SUMMARY = 3;
    private static final int ITEM_VIEW_TYPE_STATUS = 4;

    private final LayoutInflater mInflater;
    private final MediaLoadingHandler mLoadingHandler;
    private final int mCardBackgroundColor;
    private final boolean mCompactCards;
    private final TwidereLinkify mLinkify;
    private final DummyStatusHolderAdapter mStatusAdapterDelegate;
    private ActivityAdapterListener mActivityAdapterListener;

    protected AbsActivitiesAdapter(final Context context, boolean compact) {
        super(context);
        mStatusAdapterDelegate = new DummyStatusHolderAdapter(context);
        mCardBackgroundColor = ThemeUtils.getCardBackgroundColor(context,
                ThemeUtils.getThemeBackgroundOption(context),
                ThemeUtils.getUserThemeBackgroundAlpha(context));
        mInflater = LayoutInflater.from(context);
        mLoadingHandler = new MediaLoadingHandler(R.id.media_preview_progress);
        mCompactCards = compact;
        mLinkify = new TwidereLinkify(this);
        mStatusAdapterDelegate.updateOptions();
    }

    @Override
    public abstract ParcelableActivity getActivity(int position);

    @Override
    public abstract int getActivityCount();

    public abstract Data getData();

    @Override
    public abstract void setData(Data data);

    @Override
    public MediaLoadingHandler getMediaLoadingHandler() {
        return mLoadingHandler;
    }

    @Override
    public int getProfileImageStyle() {
        return mStatusAdapterDelegate.getProfileImageStyle();
    }

    @Override
    public int getMediaPreviewStyle() {
        return mStatusAdapterDelegate.getMediaPreviewStyle();
    }

    @Override
    public float getTextSize() {
        return mStatusAdapterDelegate.getTextSize();
    }

    public int getLinkHighlightingStyle() {
        return mStatusAdapterDelegate.getLinkHighlightingStyle();
    }

    public TwidereLinkify getLinkify() {
        return mLinkify;
    }

    public boolean isNameFirst() {
        return mStatusAdapterDelegate.isNameFirst();
    }

    @Override
    public boolean isProfileImageEnabled() {
        return mStatusAdapterDelegate.isProfileImageEnabled();
    }

    @Override
    public void onStatusClick(IStatusViewHolder holder, int position) {
        if (mActivityAdapterListener != null) {
            mActivityAdapterListener.onStatusClick(holder, position);
        }
    }

    @Override
    public void onMediaClick(IStatusViewHolder holder, View view, ParcelableMedia media, int position) {

    }

    @Override
    public boolean shouldUseStarsForLikes() {
        return mStatusAdapterDelegate.shouldUseStarsForLikes();
    }

    @Override
    public void onUserProfileClick(IStatusViewHolder holder, int position) {
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
                holder.setupViewOptions();
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
                holder.setOnClickListeners();
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
                final IStatusViewHolder IStatusViewHolder = (IStatusViewHolder) holder;
                IStatusViewHolder.displayStatus(status, null, true, true);
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
    public boolean onStatusLongClick(IStatusViewHolder holder, int position) {
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
            case Activity.ACTION_LIST_CREATED:
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
    public final void onGapClick(ViewHolder holder, int position) {
        if (mActivityAdapterListener != null) {
            mActivityAdapterListener.onGapClick((GapViewHolder) holder, position);
        }
    }

    @Override
    public final void onItemActionClick(ViewHolder holder, int id, int position) {
        if (mActivityAdapterListener != null) {
            mActivityAdapterListener.onStatusActionClick(((IStatusViewHolder) holder), id, position);
        }
    }

    @Override
    public final void onItemMenuClick(ViewHolder holder, View menuView, int position) {
        if (mActivityAdapterListener != null) {
            mActivityAdapterListener.onStatusMenuClick((StatusViewHolder) holder, menuView, position);
        }
    }

    @Override
    public void onLinkClick(String link, String orig, long accountId, long extraId, int type, boolean sensitive, int start, int end) {

    }

    public void setListener(ActivityAdapterListener listener) {
        mActivityAdapterListener = listener;
    }

    protected abstract void bindTitleSummaryViewHolder(ActivityTitleSummaryViewHolder holder, int position);

    protected abstract int getActivityAction(int position);

    @Override
    public boolean isMediaPreviewEnabled() {
        return mStatusAdapterDelegate.isMediaPreviewEnabled();
    }

    @Override
    public void onActivityClick(ActivityTitleSummaryViewHolder holder, int position) {
        if (mActivityAdapterListener == null) return;
        mActivityAdapterListener.onActivityClick(holder, position);
    }

    public interface ActivityAdapterListener {
        void onGapClick(GapViewHolder holder, int position);

        void onActivityClick(ActivityTitleSummaryViewHolder holder, int position);

        void onStatusActionClick(IStatusViewHolder holder, int id, int position);

        void onStatusMenuClick(IStatusViewHolder holder, View menuView, int position);

        void onStatusClick(IStatusViewHolder holder, int position);
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