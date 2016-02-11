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

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.apache.commons.lang3.math.NumberUtils;
import de.vanita5.twittnuker.Constants;
import de.vanita5.twittnuker.model.ParcelableMedia;
import de.vanita5.twittnuker.util.TwidereLinkify.OnLinkClickListener;
import de.vanita5.twittnuker.util.media.preview.PreviewMediaExtractor;

public class OnLinkClickHandler implements OnLinkClickListener, Constants {

    @NonNull
    protected final Context context;
    @Nullable
    protected final MultiSelectManager manager;

    public OnLinkClickHandler(@NonNull final Context context, @Nullable final MultiSelectManager manager) {
        this.context = context;
        this.manager = manager;
    }

    @Override
    public void onLinkClick(final String link, final String orig, final long accountId,
                            final long extraId, final int type, final boolean sensitive,
                            final int start, final int end) {
        if (manager != null && manager.isActive()) return;

        switch (type) {
            case TwidereLinkify.LINK_TYPE_MENTION: {
                IntentUtils.openUserProfile(context, accountId, -1, link, null, true);
                break;
            }
            case TwidereLinkify.LINK_TYPE_HASHTAG: {
                Utils.openTweetSearch(context, accountId, "#" + link);
                break;
            }
            case TwidereLinkify.LINK_TYPE_LINK: {
                if (PreviewMediaExtractor.isSupported(link)) {
                    openMedia(accountId, extraId, sensitive, link, start, end);
                } else {
                    openLink(link);
                }
                break;
            }
            case TwidereLinkify.LINK_TYPE_LIST: {
                final String[] mentionList = link.split("/");
                if (mentionList.length != 2) {
                    break;
                }
                Utils.openUserListDetails(context, accountId, -1, -1, mentionList[0], mentionList[1]);
                break;
            }
            case TwidereLinkify.LINK_TYPE_CASHTAG: {
                Utils.openTweetSearch(context, accountId, link);
                break;
            }
            case TwidereLinkify.LINK_TYPE_USER_ID: {
                IntentUtils.openUserProfile(context, accountId, NumberUtils.toLong(link, -1), null, null, true);
                break;
            }
            case TwidereLinkify.LINK_TYPE_STATUS: {
                Utils.openStatus(context, accountId, NumberUtils.toLong(link, -1));
                break;
            }
        }
    }

    protected boolean isPrivateData() {
        return false;
    }

    protected void openMedia(long accountId, long extraId, boolean sensitive, String link, int start, int end) {
        final ParcelableMedia[] media = {ParcelableMedia.image(link)};
        //TODO open media animation
        Utils.openMedia(context, accountId, sensitive, null, media, null);
    }

    protected void openLink(final String link) {
        if (manager != null && manager.isActive()) return;
        final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
        } catch (final ActivityNotFoundException e) {
            // TODO
        }
    }
}