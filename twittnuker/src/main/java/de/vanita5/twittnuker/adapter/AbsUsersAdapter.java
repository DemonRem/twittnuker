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
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.vanita5.twittnuker.Constants;
import de.vanita5.twittnuker.R;
import de.vanita5.twittnuker.adapter.iface.IUsersAdapter;
import de.vanita5.twittnuker.app.TwittnukerApplication;
import de.vanita5.twittnuker.util.AsyncTwitterWrapper;
import de.vanita5.twittnuker.util.MediaLoaderWrapper;
import de.vanita5.twittnuker.util.SharedPreferencesWrapper;
import de.vanita5.twittnuker.util.ThemeUtils;
import de.vanita5.twittnuker.util.Utils;
import de.vanita5.twittnuker.view.holder.LoadIndicatorViewHolder;
import de.vanita5.twittnuker.view.holder.UserViewHolder;

public abstract class AbsUsersAdapter<D> extends LoadMoreSupportAdapter<ViewHolder> implements Constants,
		IUsersAdapter<D> {

	public static final int ITEM_VIEW_TYPE_USER = 2;

	private final Context mContext;
	private final LayoutInflater mInflater;
	private final MediaLoaderWrapper mMediaLoader;

	private final int mCardBackgroundColor;
	private final boolean mCompactCards;
	private final int mProfileImageStyle;
	private final int mTextSize;
	private final AsyncTwitterWrapper mTwitterWrapper;
	private final boolean mDisplayProfileImage;

	public AbsUsersAdapter(final Context context, final boolean compact) {
		final TwittnukerApplication app = TwittnukerApplication.getInstance(context);
		mContext = context;
		mCardBackgroundColor = ThemeUtils.getCardBackgroundColor(context);
		mInflater = LayoutInflater.from(context);
		mMediaLoader = app.getMediaLoaderWrapper();
		mTwitterWrapper = app.getTwitterWrapper();
		final SharedPreferencesWrapper preferences = SharedPreferencesWrapper.getInstance(context,
				SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
		mTextSize = preferences.getInt(KEY_TEXT_SIZE, context.getResources().getInteger(R.integer.default_text_size));
		mProfileImageStyle = Utils.getProfileImageStyle(preferences.getString(KEY_PROFILE_IMAGE_STYLE, null));
		mDisplayProfileImage = preferences.getBoolean(KEY_DISPLAY_PROFILE_IMAGE, true);
		mCompactCards = compact;
	}

	@Override
	public Context getContext() {
		return mContext;
	}

	@Override
	public int getProfileImageStyle() {
		return mProfileImageStyle;
	}

	@Override
	public float getTextSize() {
		return mTextSize;
	}

	@NonNull
	@Override
	public AsyncTwitterWrapper getTwitterWrapper() {
		return mTwitterWrapper;
	}

	@Override
	public boolean isProfileImageEnabled() {
		return mDisplayProfileImage;
	}

	public abstract D getData();

	@Override
	public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		switch (viewType) {
			case ITEM_VIEW_TYPE_USER: {
				final View view;
				if (mCompactCards) {
					view = mInflater.inflate(R.layout.card_item_user_compact, parent, false);
//                    final View itemContent = view.findViewById(R.id.item_content);
//                    itemContent.setBackgroundColor(mCardBackgroundColor);
				} else {
					view = mInflater.inflate(R.layout.card_item_user, parent, false);
//                    final CardView cardView = (CardView) view.findViewById(R.id.card);
//                    cardView.setCardBackgroundColor(mCardBackgroundColor);
				}
				final UserViewHolder holder = new UserViewHolder(this, view);
//                holder.setOnClickListeners();
//                holder.setupViewOptions();
				return holder;
			}
			case ITEM_VIEW_TYPE_LOAD_INDICATOR: {
				final View view = mInflater.inflate(R.layout.card_item_load_indicator, parent, false);
				return new LoadIndicatorViewHolder(view);
			}
		}
		throw new IllegalStateException("Unknown view type " + viewType);
	}

	@Override
	public void onBindViewHolder(ViewHolder holder, int position) {
		switch (holder.getItemViewType()) {
			case ITEM_VIEW_TYPE_USER: {
				bindStatus(((UserViewHolder) holder), position);
				break;
			}
		}
	}

	public boolean isUser(int position) {
		return position < getUsersCount();
	}

	@Override
	public int getItemViewType(int position) {
		if (position == getUsersCount()) {
			return ITEM_VIEW_TYPE_LOAD_INDICATOR;
		}
		return ITEM_VIEW_TYPE_USER;
	}

	@Override
	public boolean shouldShowAccountsColor() {
		return false;
	}

	@Override
	public MediaLoaderWrapper getMediaLoader() {
		return mMediaLoader;
	}

	protected abstract void bindStatus(UserViewHolder holder, int position);

}