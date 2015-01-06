/*
 * Twittnuker - Twitter client for Android
 *
 * Copyright (C) 2013-2015 vanita5 <mail@vanita5.de>
 *
 * This program incorporates a modified version of Twidere.
 * Copyright (C) 2012-2014 Mariotaku Lee <mariotaku.lee@gmail.com>
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
import android.support.v7.widget.RecyclerView.Adapter;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import de.vanita5.twittnuker.Constants;
import de.vanita5.twittnuker.R;
import de.vanita5.twittnuker.adapter.iface.IActivitiesAdapter;
import de.vanita5.twittnuker.app.TwittnukerApplication;
import de.vanita5.twittnuker.model.ParcelableActivity;
import de.vanita5.twittnuker.model.ParcelableStatus;
import de.vanita5.twittnuker.util.AsyncTwitterWrapper;
import de.vanita5.twittnuker.util.ImageLoaderWrapper;
import de.vanita5.twittnuker.util.ImageLoadingHandler;
import de.vanita5.twittnuker.util.SharedPreferencesWrapper;
import de.vanita5.twittnuker.util.ThemeUtils;
import de.vanita5.twittnuker.util.Utils;
import de.vanita5.twittnuker.view.holder.ActivityTitleSummaryViewHolder;
import de.vanita5.twittnuker.view.holder.GapViewHolder;
import de.vanita5.twittnuker.view.holder.LoadIndicatorViewHolder;
import de.vanita5.twittnuker.view.holder.StatusViewHolder;

public abstract class AbsActivitiesAdapter<Data> extends Adapter<ViewHolder> implements Constants,
        IActivitiesAdapter<Data>, OnClickListener {

	private static final int ITEM_VIEW_TYPE_STUB = 0;
	private static final int ITEM_VIEW_TYPE_GAP = 1;
	private static final int ITEM_VIEW_TYPE_LOAD_INDICATOR = 2;
	private static final int ITEM_VIEW_TYPE_TITLE_SUMMARY = 3;
	private static final int ITEM_VIEW_TYPE_STATUS = 4;

	private final Context mContext;
	private final LayoutInflater mInflater;
	private final ImageLoaderWrapper mImageLoader;
	private final ImageLoadingHandler mLoadingHandler;
	private final AsyncTwitterWrapper mTwitterWrapper;
	private final int mCardBackgroundColor;
	private final int mTextSize;
	private final int mProfileImageStyle, mMediaPreviewStyle;
	private boolean mLoadMoreIndicatorEnabled;

    protected AbsActivitiesAdapter(final Context context) {
		mContext = context;
		final TwittnukerApplication app = TwittnukerApplication.getInstance(context);
		mCardBackgroundColor = ThemeUtils.getCardBackgroundColor(context);
		mInflater = LayoutInflater.from(context);
		mImageLoader = app.getImageLoaderWrapper();
		mLoadingHandler = new ImageLoadingHandler(R.id.media_preview_progress);
		mTwitterWrapper = app.getTwitterWrapper();
		final SharedPreferencesWrapper preferences = SharedPreferencesWrapper.getInstance(context,
				SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
		mTextSize = preferences.getInt(KEY_TEXT_SIZE, context.getResources().getInteger(R.integer.default_text_size));
		mProfileImageStyle = Utils.getProfileImageStyle(preferences.getString(KEY_PROFILE_IMAGE_STYLE, null));
		mMediaPreviewStyle = Utils.getMediaPreviewStyle(preferences.getString(KEY_MEDIA_PREVIEW_STYLE, null));
	}

	public abstract ParcelableActivity getActivity(int position);

	public abstract int getActivityCount();

	@Override
    public void onClick(View v) {

    }

    @Override
	public void onStatusClick(StatusViewHolder holder, int position) {

	}

	@Override
	public void onUserProfileClick(StatusViewHolder holder, int position) {

	}

	public abstract Data getData();

	public abstract void setData(Data data);

	@Override
	public ImageLoaderWrapper getImageLoader() {
		return mImageLoader;
	}

	@Override
	public Context getContext() {
		return mContext;
	}

	@Override
	public ImageLoadingHandler getImageLoadingHandler() {
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

	@Override
	public AsyncTwitterWrapper getTwitterWrapper() {
		return mTwitterWrapper;
	}

	@Override
	public float getTextSize() {
		return mTextSize;
	}

	public boolean hasLoadMoreIndicator() {
		return mLoadMoreIndicatorEnabled;
	}

	@Override
	public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		switch (viewType) {
			case ITEM_VIEW_TYPE_STATUS: {
				final View view = mInflater.inflate(R.layout.card_item_status_compat, parent, false);
				final StatusViewHolder holder = new StatusViewHolder(view);
				holder.setTextSize(getTextSize());
                holder.setOnClickListeners(this);
				return holder;
			}
			case ITEM_VIEW_TYPE_TITLE_SUMMARY: {
                final View view = mInflater.inflate(R.layout.list_item_activity_title_summary, parent, false);
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
				if (activity.action == ParcelableActivity.ACTION_MENTION) {
					status = activity.target_object_statuses[0];
				} else {
					status = activity.target_statuses[0];
				}
				final StatusViewHolder statusViewHolder = (StatusViewHolder) holder;
				statusViewHolder.displayStatus(getContext(), getImageLoader(),
						getImageLoadingHandler(), getTwitterWrapper(),
						getProfileImageStyle(), getMediaPreviewStyle(), status, null);
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

    protected abstract void bindTitleSummaryViewHolder(ActivityTitleSummaryViewHolder holder, int position);

	@Override
	public int getItemViewType(int position) {
		if (position == getItemCount() - 1) {
			return ITEM_VIEW_TYPE_LOAD_INDICATOR;
		} else if (isGapItem(position)) {
			return ITEM_VIEW_TYPE_GAP;
		}
		switch (getActivityAction(position)) {
			case ParcelableActivity.ACTION_MENTION:
			case ParcelableActivity.ACTION_REPLY: {
				return ITEM_VIEW_TYPE_STATUS;
			}
			case ParcelableActivity.ACTION_FOLLOW:
			case ParcelableActivity.ACTION_FAVORITE:
            case ParcelableActivity.ACTION_RETWEET:
            case ParcelableActivity.ACTION_FAVORITED_RETWEET:
            case ParcelableActivity.ACTION_RETWEETED_RETWEET:
            case ParcelableActivity.ACTION_LIST_MEMBER_ADDED: {
				return ITEM_VIEW_TYPE_TITLE_SUMMARY;
			}
		}
		return ITEM_VIEW_TYPE_STUB;
	}

	public final int getItemCount() {
		return getActivityCount() + (mLoadMoreIndicatorEnabled ? 1 : 0);
	}

	public void setLoadMoreIndicatorEnabled(boolean enabled) {
		if (mLoadMoreIndicatorEnabled == enabled) return;
		mLoadMoreIndicatorEnabled = enabled;
		notifyDataSetChanged();
	}

	protected abstract int getActivityAction(int position);

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