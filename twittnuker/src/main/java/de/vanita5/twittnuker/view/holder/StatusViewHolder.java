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

package de.vanita5.twittnuker.view.holder;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.text.BidiFormatter;
import android.support.v4.widget.Space;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import org.apache.commons.lang3.ArrayUtils;
import de.vanita5.twittnuker.Constants;
import de.vanita5.twittnuker.R;
import de.vanita5.twittnuker.adapter.iface.IStatusesAdapter;
import de.vanita5.twittnuker.graphic.LikeAnimationDrawable;
import de.vanita5.twittnuker.model.ParcelableLocation;
import de.vanita5.twittnuker.model.ParcelableMedia;
import de.vanita5.twittnuker.model.ParcelableStatus;
import de.vanita5.twittnuker.util.AsyncTwitterWrapper;
import de.vanita5.twittnuker.util.DataStoreUtils;
import de.vanita5.twittnuker.util.HtmlSpanBuilder;
import de.vanita5.twittnuker.util.IntentUtils;
import de.vanita5.twittnuker.util.MediaLoaderWrapper;
import de.vanita5.twittnuker.util.TwidereLinkify;
import de.vanita5.twittnuker.util.TwitterCardUtils;
import de.vanita5.twittnuker.util.UnitConvertUtils;
import de.vanita5.twittnuker.util.UserColorNameManager;
import de.vanita5.twittnuker.util.Utils;
import de.vanita5.twittnuker.view.ActionIconThemedTextView;
import de.vanita5.twittnuker.view.CardMediaContainer;
import de.vanita5.twittnuker.view.ForegroundColorView;
import de.vanita5.twittnuker.view.NameView;
import de.vanita5.twittnuker.view.ShortTimeView;
import de.vanita5.twittnuker.view.holder.iface.IStatusViewHolder;
import de.vanita5.twittnuker.view.iface.IColorLabelView;

import java.lang.ref.WeakReference;

import static de.vanita5.twittnuker.util.HtmlEscapeHelper.toPlainText;
import static de.vanita5.twittnuker.util.Utils.getUserTypeIconRes;

public class StatusViewHolder extends ViewHolder implements Constants, IStatusViewHolder {

    @NonNull
    private final IStatusesAdapter<?> adapter;

    private final ImageView statusInfoIcon;
    private final ImageView profileImageView;
    private final ImageView profileTypeView;
    private final ImageView extraTypeView;
    private final TextView textView;
    private final TextView quotedTextView;
    private final NameView nameView;
    private final NameView quotedNameView;
    private final TextView statusInfoLabel;
    private final ShortTimeView timeView;
    private final CardMediaContainer mediaPreview;
    private final ActionIconThemedTextView replyCountView, retweetCountView, favoriteCountView;
    private final IColorLabelView itemContent;
    private final ForegroundColorView quoteIndicator;
    private final View actionButtons;
    private final View itemMenu;
    private final View profileImageSpace;
    private final View statusInfoSpace;
    private final EventListener eventListener;
    private final Space statusContentSpace;

    private StatusClickListener statusClickListener;

    public StatusViewHolder(@NonNull final IStatusesAdapter<?> adapter, @NonNull final View itemView) {
        super(itemView);
        this.adapter = adapter;
        this.eventListener = new EventListener(this);
        itemContent = (IColorLabelView) itemView.findViewById(R.id.item_content);
        profileImageView = (ImageView) itemView.findViewById(R.id.profile_image);
        profileTypeView = (ImageView) itemView.findViewById(R.id.profile_type);
        extraTypeView = (ImageView) itemView.findViewById(R.id.extra_type);
        textView = (TextView) itemView.findViewById(R.id.text);
        quotedTextView = (TextView) itemView.findViewById(R.id.quoted_text);
        nameView = (NameView) itemView.findViewById(R.id.name);
        quotedNameView = (NameView) itemView.findViewById(R.id.quoted_name);
        statusInfoIcon = (ImageView) itemView.findViewById(R.id.status_info_icon);
        statusInfoLabel = (TextView) itemView.findViewById(R.id.status_info_label);
        timeView = (ShortTimeView) itemView.findViewById(R.id.time);
        profileImageSpace = itemView.findViewById(R.id.profile_image_space);
        statusInfoSpace = itemView.findViewById(R.id.status_info_space);

        mediaPreview = (CardMediaContainer) itemView.findViewById(R.id.media_preview);

        quoteIndicator = (ForegroundColorView) itemView.findViewById(R.id.quote_indicator);

        itemMenu = itemView.findViewById(R.id.item_menu);
        actionButtons = itemView.findViewById(R.id.action_buttons);

        replyCountView = (ActionIconThemedTextView) itemView.findViewById(R.id.reply_count);
        retweetCountView = (ActionIconThemedTextView) itemView.findViewById(R.id.retweet_count);
        favoriteCountView = (ActionIconThemedTextView) itemView.findViewById(R.id.favorite_count);
        statusContentSpace = (Space) itemView.findViewById(R.id.status_content_space);
//TODO
//        profileImageView.setSelectorColor(ThemeUtils.getUserHighlightColor(itemView.getContext()));

        if (adapter.isMediaPreviewEnabled()) {
            View.inflate(mediaPreview.getContext(), R.layout.layout_card_media_preview, mediaPreview);
        }
    }

    public void displaySampleStatus() {
        profileImageView.setVisibility(adapter.isProfileImageEnabled() ? View.VISIBLE : View.GONE);
        if (profileImageSpace != null) {
            profileImageSpace.setVisibility(adapter.isProfileImageEnabled() ? View.VISIBLE : View.GONE);
        }
        if (statusInfoSpace != null) {
            statusInfoSpace.setVisibility(adapter.isProfileImageEnabled() ? View.VISIBLE : View.GONE);
        }
        profileImageView.setImageResource(R.mipmap.ic_launcher);
        nameView.setName(TWITTNUKER_PREVIEW_NAME);
        nameView.setScreenName("@" + TWITTNUKER_PREVIEW_SCREEN_NAME);
        nameView.updateText(adapter.getBidiFormatter());
        if (adapter.getLinkHighlightingStyle() == VALUE_LINK_HIGHLIGHT_OPTION_CODE_NONE) {
            final TwidereLinkify linkify = adapter.getTwidereLinkify();
            final CharSequence text = HtmlSpanBuilder.fromHtml(TWITTNUKER_PREVIEW_TEXT_HTML,
                    TWITTNUKER_PREVIEW_TEXT_UNESCAPED);
            if (text instanceof Spanned) {
            textView.setText(linkify.applyAllLinks(text, -1, -1, false,
                    adapter.getLinkHighlightingStyle()));
            }
        } else {
            textView.setText(toPlainText(TWITTNUKER_PREVIEW_TEXT_HTML));
        }
        timeView.setTime(System.currentTimeMillis());
        if (adapter.isMediaPreviewEnabled()) {
            mediaPreview.setVisibility(View.VISIBLE);
            statusContentSpace.setVisibility(adapter.isCardActionsHidden() ? View.GONE : View.VISIBLE);
        } else {
            mediaPreview.setVisibility(View.GONE);
            statusContentSpace.setVisibility(adapter.isCardActionsHidden() ? View.VISIBLE : View.GONE);
        }
        mediaPreview.displayMedia(R.drawable.nyan_stars_background);
        extraTypeView.setImageResource(R.drawable.ic_action_gallery);
    }

    @Override
    public void displayStatus(final ParcelableStatus status, final boolean displayInReplyTo) {
        displayStatus(status, displayInReplyTo, true);
    }

    @Override
    public void displayStatus(@NonNull final ParcelableStatus status, final boolean displayInReplyTo,
                              final boolean shouldDisplayExtraType) {
        final MediaLoaderWrapper loader = adapter.getMediaLoader();
        final AsyncTwitterWrapper twitter = adapter.getTwitterWrapper();
        final TwidereLinkify linkify = adapter.getTwidereLinkify();
        final UserColorNameManager manager = adapter.getUserColorNameManager();
        final BidiFormatter formatter = adapter.getBidiFormatter();
        final Context context = adapter.getContext();
        final boolean nameFirst = adapter.isNameFirst();

        final long replyCount = status.reply_count;
        final long retweetCount;
        final long favoriteCount;

        if (TwitterCardUtils.isPoll(status)) {
            statusInfoLabel.setText(R.string.label_poll);
            statusInfoIcon.setImageResource(R.drawable.ic_activity_action_poll);
            statusInfoLabel.setVisibility(View.VISIBLE);
            statusInfoIcon.setVisibility(View.VISIBLE);
        } else if (status.retweet_id > 0) {
            final String retweetedBy = manager.getDisplayName(status.retweeted_by_user_id,
                    status.retweeted_by_user_name, status.retweeted_by_user_screen_name, nameFirst, false);
            statusInfoLabel.setText(context.getString(R.string.name_retweeted, formatter.unicodeWrap(retweetedBy)));
            statusInfoIcon.setImageResource(R.drawable.ic_activity_action_retweet);
            statusInfoLabel.setVisibility(View.VISIBLE);
            statusInfoIcon.setVisibility(View.VISIBLE);
        } else if (status.in_reply_to_status_id > 0 && status.in_reply_to_user_id > 0 && displayInReplyTo) {
            final String inReplyTo = manager.getDisplayName(status.in_reply_to_user_id,
                    status.in_reply_to_name, status.in_reply_to_screen_name, nameFirst, false);
            statusInfoLabel.setText(context.getString(R.string.in_reply_to_name, formatter.unicodeWrap(inReplyTo)));
            statusInfoIcon.setImageResource(R.drawable.ic_activity_action_reply);
            statusInfoLabel.setVisibility(View.VISIBLE);
            statusInfoIcon.setVisibility(View.VISIBLE);
        } else {
            statusInfoLabel.setVisibility(View.GONE);
            statusInfoIcon.setVisibility(View.GONE);
        }


        if (status.is_quote && ArrayUtils.isEmpty(status.media)) {

            statusContentSpace.setVisibility(View.VISIBLE);

            quotedNameView.setVisibility(View.VISIBLE);
            quotedTextView.setVisibility(View.VISIBLE);
            quoteIndicator.setVisibility(View.VISIBLE);

            quotedNameView.setName(status.quoted_user_name);
            quotedNameView.setScreenName("@" + status.quoted_user_screen_name);

            if (adapter.getLinkHighlightingStyle() != VALUE_LINK_HIGHLIGHT_OPTION_CODE_NONE
                    && !TextUtils.isEmpty(status.quoted_text_html)) {
                final CharSequence text = HtmlSpanBuilder.fromHtml(status.quoted_text_html,
                        status.quoted_text_unescaped);
                if (text instanceof Spanned) {
                    quotedTextView.setText(linkify.applyAllLinks(text, status.account_id,
                            getLayoutPosition(), status.is_possibly_sensitive,
                            adapter.getLinkHighlightingStyle()));
                }
            } else {
                final String text = status.quoted_text_unescaped;
                quotedTextView.setText(text);
            }

            quoteIndicator.setColor(manager.getUserColor(status.user_id, false));
            itemContent.drawStart(manager.getUserColor(status.quoted_user_id, false),
                    manager.getUserColor(status.user_id, false));
        } else {

            statusContentSpace.setVisibility(adapter.isCardActionsHidden() ? View.VISIBLE :View.GONE);

            quotedNameView.setVisibility(View.GONE);
            quotedTextView.setVisibility(View.GONE);
            quoteIndicator.setVisibility(View.GONE);

            if (status.is_retweet) {
                itemContent.drawStart(manager.getUserColor(status.retweeted_by_user_id, false),
                        manager.getUserColor(status.user_id, false));
            } else {
                itemContent.drawStart(manager.getUserColor(status.user_id, false));
            }
        }

        if (status.is_retweet) {
            timeView.setTime(status.retweet_timestamp);
        } else {
            timeView.setTime(status.timestamp);
        }
        nameView.setName(status.user_name);
        nameView.setScreenName("@" + status.user_screen_name);


        if (adapter.isProfileImageEnabled()) {
            profileImageView.setVisibility(View.VISIBLE);
            if (profileImageSpace != null) {
                profileImageSpace.setVisibility(View.VISIBLE);
            }
            if (statusInfoSpace != null) {
                statusInfoSpace.setVisibility(View.VISIBLE);
            }
            final String user_profile_image_url = status.user_profile_image_url;

            loader.displayProfileImage(profileImageView, user_profile_image_url);

            profileTypeView.setImageResource(getUserTypeIconRes(status.user_is_verified, status.user_is_protected));
            profileTypeView.setVisibility(View.VISIBLE);
        } else {
            profileTypeView.setVisibility(View.GONE);
            profileImageView.setVisibility(View.GONE);
            if (profileImageSpace != null) {
                profileImageSpace.setVisibility(View.GONE);
            }
            if (statusInfoSpace != null) {
                statusInfoSpace.setVisibility(View.GONE);
            }

            loader.cancelDisplayTask(profileImageView);

            profileTypeView.setImageDrawable(null);
            profileTypeView.setVisibility(View.GONE);
        }

        if (adapter.shouldShowAccountsColor()) {
            itemContent.drawEnd(DataStoreUtils.getAccountColor(context, status.account_id));
        } else {
            itemContent.drawEnd();
        }

        final ParcelableMedia[] media = IntentUtils.getPrimaryMedia(status);

        if (adapter.isMediaPreviewEnabled()) {
            mediaPreview.setStyle(adapter.getMediaPreviewStyle());
            final boolean hasMedia = !ArrayUtils.isEmpty(media);
            if (hasMedia && (adapter.isSensitiveContentEnabled() || !status.is_possibly_sensitive)) {
                mediaPreview.setVisibility(View.VISIBLE);
                statusContentSpace.setVisibility(adapter.isCardActionsHidden() ? View.GONE : View.VISIBLE);

                mediaPreview.displayMedia(media, loader, status.account_id, -1, this,
                        adapter.getMediaLoadingHandler());
            } else {
                mediaPreview.setVisibility(View.GONE);
            }
        } else {
            mediaPreview.setVisibility(View.GONE);
        }
        if (adapter.getLinkHighlightingStyle() == VALUE_LINK_HIGHLIGHT_OPTION_CODE_NONE) {
            textView.setText(status.text_unescaped);
        } else {
            final CharSequence text = HtmlSpanBuilder.fromHtml(status.text_html, status.text_unescaped);
            if (text instanceof Spanned) {
                textView.setText(linkify.applyAllLinks(text, status.account_id, getLayoutPosition(),
                        status.is_possibly_sensitive, adapter.getLinkHighlightingStyle()));
            }
        }

        if (replyCount > 0) {
            replyCountView.setText(UnitConvertUtils.calculateProperCount(replyCount));
        } else {
            replyCountView.setText(null);
        }

        if (twitter.isDestroyingStatus(status.account_id, status.my_retweet_id)) {
            retweetCountView.setActivated(false);
            retweetCount = Math.max(0, status.retweet_count - 1);
        } else {
            final boolean creatingRetweet = twitter.isCreatingRetweet(status.account_id, status.id);
            retweetCountView.setActivated(creatingRetweet || status.retweeted ||
                    Utils.isMyRetweet(status.account_id, status.retweeted_by_user_id, status.my_retweet_id));
            retweetCount = status.retweet_count + (creatingRetweet ? 1 : 0);
        }
        if (retweetCount > 0) {
            retweetCountView.setText(UnitConvertUtils.calculateProperCount(retweetCount));
        } else {
            retweetCountView.setText(null);
        }
        if (twitter.isDestroyingFavorite(status.account_id, status.id)) {
            favoriteCountView.setActivated(false);
            favoriteCount = Math.max(0, status.favorite_count - 1);
        } else {
            final boolean creatingFavorite = twitter.isCreatingFavorite(status.account_id, status.id);
            favoriteCountView.setActivated(creatingFavorite || status.is_favorite);
            favoriteCount = status.favorite_count + (creatingFavorite ? 1 : 0);
        }
        if (favoriteCount > 0) {
            favoriteCountView.setText(UnitConvertUtils.calculateProperCount(favoriteCount));
        } else {
            favoriteCountView.setText(null);
        }
        if (shouldDisplayExtraType) {
            displayExtraTypeIcon(status.card_name, media, status.location, status.place_full_name,
                    status.is_possibly_sensitive);
        } else {
            extraTypeView.setVisibility(View.GONE);
        }

        nameView.updateText(formatter);
        quotedNameView.updateText(formatter);
    }

    @Override
    public ImageView getProfileImageView() {
        return profileImageView;
    }

    @Override
    public ImageView getProfileTypeView() {
        return profileTypeView;
    }


    @Override
    public void onMediaClick(View view, ParcelableMedia media, long accountId, long extraId) {
        if (statusClickListener == null) return;
        final int position = getLayoutPosition();
        statusClickListener.onMediaClick(this, view, media, position);
    }

    public void setOnClickListeners() {
        setStatusClickListener(adapter.getStatusClickListener());
    }

    @Override
    public void setStatusClickListener(StatusClickListener listener) {
        statusClickListener = listener;
        ((View) itemContent).setOnClickListener(eventListener);
        ((View) itemContent).setOnLongClickListener(eventListener);

        itemMenu.setOnClickListener(eventListener);
        profileImageView.setOnClickListener(eventListener);
        replyCountView.setOnClickListener(eventListener);
        retweetCountView.setOnClickListener(eventListener);
        favoriteCountView.setOnClickListener(eventListener);
    }

    @Override
    public void setTextSize(final float textSize) {
        nameView.setPrimaryTextSize(textSize);
        quotedNameView.setPrimaryTextSize(textSize);
        textView.setTextSize(textSize);
        quotedTextView.setTextSize(textSize);
        nameView.setSecondaryTextSize(textSize * 0.85f);
        quotedNameView.setSecondaryTextSize(textSize * 0.85f);
        timeView.setTextSize(textSize * 0.85f);
        statusInfoLabel.setTextSize(textSize * 0.75f);
        replyCountView.setTextSize(textSize);
        retweetCountView.setTextSize(textSize);
        favoriteCountView.setTextSize(textSize);
    }

    public void setupViewOptions() {
        setTextSize(adapter.getTextSize());
        mediaPreview.setStyle(adapter.getMediaPreviewStyle());
//        profileImageView.setStyle(adapter.getProfileImageStyle());
        actionButtons.setVisibility(adapter.isCardActionsHidden() ? View.GONE : View.VISIBLE);
        itemMenu.setVisibility(adapter.isCardActionsHidden() ? View.GONE : View.VISIBLE);

        final boolean nameFirst = adapter.isNameFirst();
        nameView.setNameFirst(nameFirst);
        quotedNameView.setNameFirst(nameFirst);

        final int likeIcon, likeStyle;
        if (adapter.shouldUseStarsForLikes()) {
            favoriteCountView.setActivatedColor(ContextCompat.getColor(adapter.getContext(),
                    R.color.highlight_favorite));
            likeIcon = R.drawable.ic_action_star;
            likeStyle = LikeAnimationDrawable.Style.FAVORITE;
        } else {
            likeIcon = R.drawable.ic_action_heart;
            likeStyle = LikeAnimationDrawable.Style.LIKE;
        }
        final LikeAnimationDrawable drawable = new LikeAnimationDrawable(adapter.getContext(),
                likeIcon, favoriteCountView.getColor(), favoriteCountView.getActivatedColor(),
                likeStyle);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        TextViewCompat.setCompoundDrawablesRelative(favoriteCountView, drawable, null, null, null);
        drawable.setCallback(favoriteCountView);
        timeView.setShowAbsoluteTime(adapter.isShowAbsoluteTime());
    }

    @Override
    public void playLikeAnimation(@NonNull LikeAnimationDrawable.OnLikedListener listener) {
        boolean handled = false;
        for (Drawable drawable : favoriteCountView.getCompoundDrawables()) {
            if (drawable instanceof LikeAnimationDrawable) {
                ((LikeAnimationDrawable) drawable).setOnLikedListener(listener);
                ((LikeAnimationDrawable) drawable).start();
                handled = true;
            }
        }
        if (!handled) {
            listener.onLiked();
        }
    }

    void displayExtraTypeIcon(String cardName, ParcelableMedia[] media, ParcelableLocation location, String placeFullName, boolean sensitive) {
        if (TwitterCardUtils.CARD_NAME_AUDIO.equals(cardName)) {
            extraTypeView.setImageResource(sensitive ? R.drawable.ic_action_warning : R.drawable.ic_action_music);
            extraTypeView.setVisibility(View.VISIBLE);
        } else if (TwitterCardUtils.CARD_NAME_ANIMATED_GIF.equals(cardName)) {
            extraTypeView.setImageResource(sensitive ? R.drawable.ic_action_warning : R.drawable.ic_action_movie);
            extraTypeView.setVisibility(View.VISIBLE);
        } else if (TwitterCardUtils.CARD_NAME_PLAYER.equals(cardName)) {
            extraTypeView.setImageResource(sensitive ? R.drawable.ic_action_warning : R.drawable.ic_action_play_circle);
            extraTypeView.setVisibility(View.VISIBLE);
        } else if (!ArrayUtils.isEmpty(media)) {
            if (hasVideo(media)) {
                extraTypeView.setImageResource(sensitive ? R.drawable.ic_action_warning : R.drawable.ic_action_movie);
            } else {
                extraTypeView.setImageResource(sensitive ? R.drawable.ic_action_warning : R.drawable.ic_action_gallery);
            }
            extraTypeView.setVisibility(View.VISIBLE);
        } else if (ParcelableLocation.isValidLocation(location) || !TextUtils.isEmpty(placeFullName)) {
            extraTypeView.setImageResource(R.drawable.ic_action_location);
            extraTypeView.setVisibility(View.VISIBLE);
        } else {
            extraTypeView.setVisibility(View.GONE);
        }
    }

    boolean hasVideo(ParcelableMedia[] media) {
        if (media == null) return false;
        for (ParcelableMedia item : media) {
            if (item == null) continue;
            switch (item.type) {
                case ParcelableMedia.Type.VIDEO:
                case ParcelableMedia.Type.ANIMATED_GIF:
                case ParcelableMedia.Type.EXTERNAL_PLAYER:
                    return true;
            }
        }
        return false;
    }

    static class EventListener implements OnClickListener, OnLongClickListener {

        final WeakReference<StatusViewHolder> holderRef;

        EventListener(StatusViewHolder holder) {
            this.holderRef = new WeakReference<>(holder);
        }

        @Override
        public void onClick(View v) {
            StatusViewHolder holder = holderRef.get();
            if (holder == null) return;
            StatusClickListener listener = holder.statusClickListener;
            if (listener == null) return;
            final int position = holder.getLayoutPosition();
            switch (v.getId()) {
                case R.id.item_content: {
                    listener.onStatusClick(holder, position);
                    break;
                }
                case R.id.item_menu: {
                    listener.onItemMenuClick(holder, v, position);
                    break;
                }
                case R.id.profile_image: {
                    listener.onUserProfileClick(holder, position);
                    break;
                }
                case R.id.reply_count:
                case R.id.retweet_count:
                case R.id.favorite_count: {
                    listener.onItemActionClick(holder, v.getId(), position);
                    break;
                }
            }
        }

        @Override
        public boolean onLongClick(View v) {
            StatusViewHolder holder = holderRef.get();
            if (holder == null) return false;
            StatusClickListener listener = holder.statusClickListener;
            if (listener == null) return false;
            final int position = holder.getLayoutPosition();
            switch (v.getId()) {
                case R.id.item_content: {
                    return listener.onStatusLongClick(holder, position);
                }
            }
            return false;
        }
    }

}