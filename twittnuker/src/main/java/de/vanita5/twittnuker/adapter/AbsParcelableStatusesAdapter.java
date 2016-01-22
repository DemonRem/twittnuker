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
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;

import org.mariotaku.library.objectcursor.ObjectCursor;
import de.vanita5.twittnuker.model.ParcelableStatus;
import de.vanita5.twittnuker.model.ParcelableStatusCursorIndices;

import java.util.List;

public abstract class AbsParcelableStatusesAdapter extends AbsStatusesAdapter<List<ParcelableStatus>> {
    private List<ParcelableStatus> mData;

    public AbsParcelableStatusesAdapter(Context context, boolean compact) {
        super(context, compact);
        setHasStableIds(true);
    }

    @Override
    public boolean isGapItem(int position) {
        return getStatus(position).is_gap && position != getStatusesCount() - 1;
    }

    @Override
    public ParcelableStatus getStatus(int position) {
        if (position == getStatusesCount()) return null;
        return mData.get(position);
    }

    @Override
    public int getStatusesCount() {
        if (mData == null) return 0;
        return mData.size();
    }

    @Override
    public long getItemId(int position) {
        if (position == getStatusesCount()) return RecyclerView.NO_ID;
        if (mData instanceof ObjectCursor) {
            final Cursor cursor = ((ObjectCursor) mData).getCursor(position);
            final ParcelableStatusCursorIndices indices = (ParcelableStatusCursorIndices) ((ObjectCursor) mData).getIndices();
            return cursor.getLong(indices._id);
        }
        return mData.get(position).hashCode();
    }

    @Override
    public long getStatusId(int position) {
        if (position == getStatusesCount()) return RecyclerView.NO_ID;
        if (mData instanceof ObjectCursor) {
            final Cursor cursor = ((ObjectCursor) mData).getCursor(position);
            final ParcelableStatusCursorIndices indices = (ParcelableStatusCursorIndices) ((ObjectCursor) mData).getIndices();
            return cursor.getLong(indices.id);
        }
        return mData.get(position).id;
    }

    @Override
    public long getAccountId(int position) {
        if (position == getStatusesCount()) return RecyclerView.NO_ID;
        if (mData instanceof ObjectCursor) {
            final Cursor cursor = ((ObjectCursor) mData).getCursor(position);
            final ParcelableStatusCursorIndices indices = (ParcelableStatusCursorIndices) ((ObjectCursor) mData).getIndices();
            return cursor.getLong(indices.account_id);
        }
        return mData.get(position).account_id;
    }

    @Override
    public void setData(List<ParcelableStatus> data) {
        mData = data;
        notifyDataSetChanged();
    }

    @Override
    public List<ParcelableStatus> getData() {
        return mData;
    }
}