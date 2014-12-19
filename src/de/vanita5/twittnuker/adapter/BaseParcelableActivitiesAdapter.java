/*
 * Twittnuker - Twitter client for Android
 *
 * Copyright (C) 2013-2014 vanita5 <mail@vanita5.de>
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
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import de.vanita5.twittnuker.R;
import de.vanita5.twittnuker.adapter.iface.IBaseCardAdapter;
import de.vanita5.twittnuker.app.TwittnukerApplication;
import de.vanita5.twittnuker.model.ParcelableActivity;
import de.vanita5.twittnuker.model.ParcelableStatus;
import de.vanita5.twittnuker.model.ParcelableUser;
import de.vanita5.twittnuker.util.ImageLoaderWrapper;
import de.vanita5.twittnuker.util.MultiSelectManager;
import de.vanita5.twittnuker.view.holder.ActivityListViewHolder;

import java.util.List;

import static de.vanita5.twittnuker.util.Utils.configBaseCardAdapter;
import static de.vanita5.twittnuker.util.Utils.getAccountColor;
import static de.vanita5.twittnuker.util.Utils.getDisplayName;
import static de.vanita5.twittnuker.util.Utils.isCompactCards;

public abstract class BaseParcelableActivitiesAdapter extends BaseArrayAdapter<ParcelableActivity> implements
		IBaseCardAdapter {

	private final MultiSelectManager mMultiSelectManager;
	private final ImageLoaderWrapper mImageLoader;

    private boolean mShowAbsoluteTime;

	public BaseParcelableActivitiesAdapter(final Context context) {
        this(context, isCompactCards(context));
	}

    public BaseParcelableActivitiesAdapter(final Context context, final boolean compactCards) {
		super(context, getItemResource(compactCards));
		final TwittnukerApplication app = TwittnukerApplication.getInstance(context);
		mMultiSelectManager = app.getMultiSelectManager();
		mImageLoader = app.getImageLoaderWrapper();
		configBaseCardAdapter(context, this);
	}

	public abstract void bindView(final int position, final ActivityListViewHolder holder, final ParcelableActivity item);

	@Override
	public ImageLoaderWrapper getImageLoader() {
		return mImageLoader;
	}

	@Override
	public long getItemId(final int position) {
		final Object obj = getItem(position);
		return obj != null ? obj.hashCode() : 0;
	}

	@Override
	public View getView(final int position, final View convertView, final ViewGroup parent) {
		final View view = super.getView(position, convertView, parent);
		final Object tag = view.getTag();
		final ActivityListViewHolder holder = tag instanceof ActivityListViewHolder ? (ActivityListViewHolder) tag
				: new ActivityListViewHolder(view);
		if (!(tag instanceof ActivityListViewHolder)) {
			view.setTag(holder);
		}

		final boolean showAccountColor = isShowAccountColor();

		holder.setTextSize(getTextSize());
		holder.my_profile_image.setVisibility(View.GONE);
		final ParcelableActivity item = getItem(position);
		holder.setAccountColorEnabled(showAccountColor);
		if (showAccountColor) {
			holder.setAccountColor(getAccountColor(getContext(), item.account_id));
		}
		if (mShowAbsoluteTime) {
			holder.time.setTime(item.activity_timestamp);
		} else {
			holder.time.setTime(item.activity_timestamp);
		}
		bindView(position, holder, item);
		return view;
	}

	public void onItemSelected(final Object item) {
		notifyDataSetChanged();
	}

	public void onItemUnselected(final Object item) {
		notifyDataSetChanged();
	}

	public void setData(final List<ParcelableActivity> data) {
		clear();
		if (data == null) return;
		addAll(data);
	}

	public void setShowAbsoluteTime(final boolean show) {
		if (show != mShowAbsoluteTime) {
			mShowAbsoluteTime = show;
			notifyDataSetChanged();
		}
	}

	protected void displayActivityUserProfileImages(final ActivityListViewHolder holder, final ParcelableStatus[] statuses) {
		if (statuses == null) {
			displayActivityUserProfileImages(holder, new String[0]);
		} else {
			final String[] urls = new String[statuses.length];
			for (int i = 0, j = statuses.length; i < j; i++) {
				urls[i] = statuses[i].user_profile_image_url;
			}
			displayActivityUserProfileImages(holder, urls);
		}
	}

	protected void displayActivityUserProfileImages(final ActivityListViewHolder holder, final ParcelableUser[] users) {
		if (users == null) {
			displayActivityUserProfileImages(holder, new String[0]);
		} else {
			final String[] urls = new String[users.length];
			for (int i = 0, j = users.length; i < j; i++) {
				urls[i] = users[i].profile_image_url;
			}
			displayActivityUserProfileImages(holder, urls);
		}
	}

	protected void displayProfileImage(final ImageView view, final ParcelableUser user) {
		if (isDisplayProfileImage()) {
			mImageLoader.displayProfileImage(view, user.profile_image_url);
		} else {
			view.setImageDrawable(null);
		}
	}

	protected String getName(final ParcelableStatus status) {
		if (status == null) return null;
		return getDisplayName(status.user_name, status.user_screen_name,
				isDisplayNameFirst());
	}

	protected String getName(final ParcelableUser user) {
		if (user == null) return null;
		return getDisplayName(user.name, user.screen_name, isDisplayNameFirst());
	}

	protected void setProfileImage(final ImageView view, final ParcelableStatus status) {
		if (isDisplayProfileImage()) {
			mImageLoader.displayProfileImage(view, status.user_profile_image_url);
		} else {
			view.setImageDrawable(null);
		}
	}

	protected boolean shouldDisplayProfileImage() {
		return isDisplayProfileImage();
	}

	private void displayActivityUserProfileImages(final ActivityListViewHolder holder, final String[] urls) {
		final int length = urls != null ? Math.min(holder.activity_profile_images.length, urls.length) : 0;
		final boolean shouldDisplayImages = isDisplayProfileImage() && length > 0;
		holder.activity_profile_images_container.setVisibility(shouldDisplayImages ? View.VISIBLE : View.GONE);
		if (!shouldDisplayImages) return;
		for (int i = 0, j = holder.activity_profile_images.length; i < j; i++) {
			final ImageView view = holder.activity_profile_images[i];
			view.setImageDrawable(null);
			if (i < length) {
				view.setVisibility(View.VISIBLE);
				mImageLoader.displayProfileImage(view, urls[i]);
			} else {
				mImageLoader.cancelDisplayTask(view);
				view.setVisibility(View.GONE);
			}
		}
		if (urls.length > holder.activity_profile_images.length) {
			final int moreNumber = urls.length - holder.activity_profile_images.length;
			holder.activity_profile_image_more_number.setVisibility(View.VISIBLE);
			holder.activity_profile_image_more_number.setText(getContext().getString(R.string.and_more, moreNumber));
		} else {
			holder.activity_profile_image_more_number.setVisibility(View.GONE);
		}
	}

	private static int getItemResource(final boolean compactCards) {
		return compactCards ? R.layout.card_item_activity_compact : R.layout.card_item_activity;
	}

}
