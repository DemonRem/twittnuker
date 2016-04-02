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

import android.support.v7.widget.RecyclerView;

import de.vanita5.twittnuker.adapter.iface.IStatusesAdapter;
import de.vanita5.twittnuker.model.ParcelableMedia;
import de.vanita5.twittnuker.model.ParcelableStatus;
import de.vanita5.twittnuker.model.UserKey;

public class StatusAdapterLinkClickHandler<D> extends OnLinkClickHandler {

    private final IStatusesAdapter<D> adapter;

    public StatusAdapterLinkClickHandler(IStatusesAdapter<D> adapter,
                                         SharedPreferencesWrapper preferences) {
        super(adapter.getContext(), null, preferences);
        this.adapter = adapter;
    }

    @Override
    protected void openMedia(final UserKey accountKey, final long extraId, final boolean sensitive,
                             final String link, final int start, final int end) {
        if (extraId == RecyclerView.NO_POSITION) return;
        final ParcelableStatus status = adapter.getStatus((int) extraId);
        final ParcelableMedia current = StatusLinkClickHandler.findByLink(status.media, link);
        if (current != null && current.open_browser) {
            openLink(link);
        } else {
            IntentUtils.openMedia(context, status, current, null,
                    preferences.getBoolean(KEY_NEW_DOCUMENT_API));
        }
    }

}