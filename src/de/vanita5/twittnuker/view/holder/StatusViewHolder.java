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

package de.vanita5.twittnuker.view.holder;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import de.vanita5.twittnuker.R;
import de.vanita5.twittnuker.adapter.iface.IStatusesAdapter;
import de.vanita5.twittnuker.model.ParcelableMedia;
import de.vanita5.twittnuker.model.ParcelableStatus;
import de.vanita5.twittnuker.model.ParcelableStatus.CursorIndices;
import de.vanita5.twittnuker.util.ImageLoaderWrapper;
import de.vanita5.twittnuker.util.ImageLoadingHandler;
import de.vanita5.twittnuker.util.UserColorUtils;
import de.vanita5.twittnuker.util.Utils;
import de.vanita5.twittnuker.view.CircularImageView;
import de.vanita5.twittnuker.view.ShortTimeView;

import java.util.Locale;

import static de.vanita5.twittnuker.util.Utils.getUserTypeIconRes;

public class StatusViewHolder extends RecyclerView.ViewHolder implements OnClickListener {

    private final IStatusesAdapter<?> adapter;

    private final ImageView retweetProfileImageView;
    private final CircularImageView profileImageView;
    private final ImageView profileTypeView;
    private final ImageView mediaPreviewView;
    private final TextView textView;
    private final TextView nameView, screenNameView;
    private final TextView replyRetweetView;
    private final ShortTimeView timeView;
    private final View mediaPreviewContainer;
    private final TextView replyCountView, retweetCountView, favoriteCountView;


    public StatusViewHolder(View itemView) {
        this(null, itemView);
    }

    public StatusViewHolder(IStatusesAdapter<?> adapter, View itemView) {
        super(itemView);
        this.adapter = adapter;
        profileImageView = (CircularImageView) itemView.findViewById(R.id.profile_image);
        profileTypeView = (ImageView) itemView.findViewById(R.id.profile_type);
        textView = (TextView) itemView.findViewById(R.id.text);
        nameView = (TextView) itemView.findViewById(R.id.name);
        screenNameView = (TextView) itemView.findViewById(R.id.screen_name);
        retweetProfileImageView = (ImageView) itemView.findViewById(R.id.retweet_profile_image);
        replyRetweetView = (TextView) itemView.findViewById(R.id.reply_retweet_status);
        timeView = (ShortTimeView) itemView.findViewById(R.id.time);

        mediaPreviewContainer = itemView.findViewById(R.id.media_preview_container);
        mediaPreviewView = (ImageView) itemView.findViewById(R.id.media_preview);

        replyCountView = (TextView) itemView.findViewById(R.id.reply_count);
        retweetCountView = (TextView) itemView.findViewById(R.id.retweet_count);
        favoriteCountView = (TextView) itemView.findViewById(R.id.favorite_count);
//TODO
//        profileImageView.setSelectorColor(ThemeUtils.getUserHighlightColor(itemView.getContext()));
    }

    public void setupViews() {
        itemView.findViewById(R.id.item_content).setOnClickListener(this);
        itemView.findViewById(R.id.item_menu).setOnClickListener(this);

        itemView.setOnClickListener(this);
        profileImageView.setOnClickListener(this);
        mediaPreviewContainer.setOnClickListener(this);
        replyCountView.setOnClickListener(this);
        retweetCountView.setOnClickListener(this);
        favoriteCountView.setOnClickListener(this);
	}

    public void displayStatus(final ParcelableStatus status) {
        displayStatus(adapter.getContext(), adapter.getImageLoader(),
				adapter.getImageLoadingHandler(), status);
    }

    public void displayStatus(final Context context, final ImageLoaderWrapper loader,
                              final ImageLoadingHandler handler, final ParcelableStatus status) {
        final ParcelableMedia[] media = status.media;

        if (status.retweet_id > 0) {
            if (status.retweet_count == 2) {
                replyRetweetView.setText(context.getString(R.string.name_and_another_retweeted,
                        status.retweeted_by_name));
            } else if (status.retweet_count > 2) {
                replyRetweetView.setText(context.getString(R.string.name_and_count_retweeted,
                        status.retweeted_by_name, status.retweet_count - 1));
            } else {
                replyRetweetView.setText(context.getString(R.string.name_retweeted, status.retweeted_by_name));
            }
            replyRetweetView.setVisibility(View.VISIBLE);
            retweetProfileImageView.setVisibility(View.GONE);
        } else if (status.in_reply_to_status_id > 0 && status.in_reply_to_user_id > 0) {
            replyRetweetView.setText(context.getString(R.string.in_reply_to_name, status.in_reply_to_name));
            replyRetweetView.setVisibility(View.VISIBLE);
            retweetProfileImageView.setVisibility(View.GONE);
        } else {
            replyRetweetView.setText(null);
            replyRetweetView.setVisibility(View.GONE);
            replyRetweetView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            retweetProfileImageView.setVisibility(View.GONE);
	    }

        final int typeIconRes = getUserTypeIconRes(status.user_is_verified, status.user_is_protected);
        if (typeIconRes != 0) {
            profileTypeView.setImageResource(typeIconRes);
            profileTypeView.setVisibility(View.VISIBLE);
        } else {
            profileTypeView.setImageDrawable(null);
            profileTypeView.setVisibility(View.GONE);
	    }

        nameView.setText(status.user_name);
        screenNameView.setText("@" + status.user_screen_name);
        timeView.setTime(status.timestamp);

        final int userColor = UserColorUtils.getUserColor(context, status.user_id);
        profileImageView.setBorderColor(userColor);

        loader.displayProfileImage(profileImageView, status.user_profile_image_url);

        if (media != null && media.length > 0) {
            final ParcelableMedia firstMedia = media[0];
            textView.setText(status.text_unescaped);
            loader.displayPreviewImageWithCredentials(mediaPreviewView, firstMedia.media_url,
                    status.account_id, handler);
            mediaPreviewContainer.setVisibility(View.VISIBLE);
        } else {
            loader.cancelDisplayTask(mediaPreviewView);
            textView.setText(status.text_unescaped);
            mediaPreviewContainer.setVisibility(View.GONE);
	    }

        if (status.reply_count > 0) {
            replyCountView.setText(Utils.getLocalizedNumber(Locale.getDefault(), status.reply_count));
        } else {
            replyCountView.setText(null);
	    }
        if (status.retweet_count > 0) {
            retweetCountView.setText(Utils.getLocalizedNumber(Locale.getDefault(), status.retweet_count));
        } else {
            retweetCountView.setText(null);
	    }
        if (status.favorite_count > 0) {
            favoriteCountView.setText(Utils.getLocalizedNumber(Locale.getDefault(), status.favorite_count));
        } else {
            favoriteCountView.setText(null);
        }

        retweetCountView.setEnabled(!status.user_is_protected);

        retweetCountView.setActivated(Utils.isMyRetweet(status));
        favoriteCountView.setActivated(status.is_favorite);
	}


    public void displayStatus(Cursor cursor, CursorIndices indices) {
        final ImageLoaderWrapper loader = adapter.getImageLoader();
        final Context context = adapter.getContext();

        final int reply_count = cursor.getInt(indices.reply_count);
        final int retweet_count = cursor.getInt(indices.retweet_count);
        final int favorite_count = cursor.getInt(indices.favorite_count);

        final long account_id = cursor.getLong(indices.account_id);
        final long timestamp = cursor.getLong(indices.status_timestamp);
        final long user_id = cursor.getLong(indices.user_id);
        final long retweet_id = cursor.getLong(indices.retweet_id);
        final long my_retweet_id = cursor.getLong(indices.my_retweet_id);
        final long retweeted_by_id = cursor.getLong(indices.retweeted_by_user_id);
        final long in_reply_to_status_id = cursor.getLong(indices.in_reply_to_status_id);
        final long in_reply_to_user_id = cursor.getLong(indices.in_reply_to_user_id);

        final boolean user_is_protected = cursor.getInt(indices.is_protected) == 1;

        final String user_name = cursor.getString(indices.user_name);
        final String user_screen_name = cursor.getString(indices.user_screen_name);
        final String user_profile_image_url = cursor.getString(indices.user_profile_image_url);
        final String retweeted_by_name = cursor.getString(indices.retweeted_by_user_name);
        final String in_reply_to_name = cursor.getString(indices.in_reply_to_user_name);

        final ParcelableMedia[] media = ParcelableMedia.fromJSONString(cursor.getString(indices.media));

        if (retweet_id > 0) {
            if (retweet_count == 2) {
                replyRetweetView.setText(context.getString(R.string.name_and_another_retweeted,
                        retweeted_by_name));
            } else if (retweet_count > 2) {
                replyRetweetView.setText(context.getString(R.string.name_and_count_retweeted,
                        retweeted_by_name, retweet_count - 1));
            } else {
                replyRetweetView.setText(context.getString(R.string.name_retweeted, retweeted_by_name));
            }
            replyRetweetView.setVisibility(View.VISIBLE);
            retweetProfileImageView.setVisibility(View.GONE);
        } else if (in_reply_to_status_id > 0 && in_reply_to_user_id > 0) {
            replyRetweetView.setText(context.getString(R.string.in_reply_to_name, in_reply_to_name));
            replyRetweetView.setVisibility(View.VISIBLE);
            retweetProfileImageView.setVisibility(View.GONE);
        } else {
            replyRetweetView.setText(null);
            replyRetweetView.setVisibility(View.GONE);
            replyRetweetView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            retweetProfileImageView.setVisibility(View.GONE);
        }

        final int typeIconRes = getUserTypeIconRes(cursor.getInt(indices.is_verified) == 1,
                user_is_protected);
        if (typeIconRes != 0) {
            profileTypeView.setImageResource(typeIconRes);
            profileTypeView.setVisibility(View.VISIBLE);
        } else {
            profileTypeView.setImageDrawable(null);
            profileTypeView.setVisibility(View.GONE);
        }

        nameView.setText(user_name);
        screenNameView.setText("@" + user_screen_name);
        timeView.setTime(timestamp);

        final int userColor = UserColorUtils.getUserColor(context, user_id);
        profileImageView.setBorderColor(userColor);

        loader.displayProfileImage(profileImageView, user_profile_image_url);

        if (media != null && media.length > 0) {
            final String textUnescaped = cursor.getString(indices.text_unescaped);
            final ParcelableMedia firstMedia = media[0];
            textView.setText(textUnescaped);
            loader.displayPreviewImageWithCredentials(mediaPreviewView, firstMedia.media_url,
                    account_id, adapter.getImageLoadingHandler());
            mediaPreviewContainer.setVisibility(View.VISIBLE);
        } else {
            final String text_unescaped = cursor.getString(indices.text_unescaped);
            loader.cancelDisplayTask(mediaPreviewView);
            textView.setText(text_unescaped);
            mediaPreviewContainer.setVisibility(View.GONE);
        }

        if (reply_count > 0) {
            replyCountView.setText(Utils.getLocalizedNumber(Locale.getDefault(), reply_count));
        } else {
            replyCountView.setText(null);
        }
        if (retweet_count > 0) {
            retweetCountView.setText(Utils.getLocalizedNumber(Locale.getDefault(), retweet_count));
        } else {
            retweetCountView.setText(null);
        }
        if (favorite_count > 0) {
            favoriteCountView.setText(Utils.getLocalizedNumber(Locale.getDefault(), favorite_count));
        } else {
            favoriteCountView.setText(null);
        }

        retweetCountView.setEnabled(!user_is_protected);
        retweetCountView.setActivated(Utils.isMyRetweet(account_id, retweeted_by_id, my_retweet_id));
        favoriteCountView.setActivated(cursor.getInt(indices.is_favorite) == 1);
    }

    public CardView getCardView() {
        return (CardView) itemView.findViewById(R.id.card);
    }

    public CircularImageView getProfileImageView() {
        return profileImageView;
    }

    public ImageView getProfileTypeView() {
        return profileTypeView;
    }


    @Override
    public void onClick(View v) {
        final int position = getPosition();
        switch (v.getId()) {
            case R.id.item_content: {
                adapter.onStatusClick(this, position);
                break;
		    }
            case R.id.item_menu: {
                adapter.onItemMenuClick(this, position);
                break;
	        }
            case R.id.profile_image: {
                adapter.onUserProfileClick(this, position);
                break;
	        }
            case R.id.reply_count:
            case R.id.retweet_count:
            case R.id.favorite_count: {
                adapter.onItemActionClick(this, v.getId(), position);
                break;
	        }
        }
    }
}
