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
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;

import org.mariotaku.library.objectcursor.ObjectCursor;
import de.vanita5.twittnuker.model.ParcelableActivity;
import de.vanita5.twittnuker.model.ParcelableActivityCursorIndices;
import de.vanita5.twittnuker.view.holder.ActivityTitleSummaryViewHolder;

import java.util.List;

public class ParcelableActivitiesAdapter extends AbsActivitiesAdapter<List<ParcelableActivity>> {

    private List<ParcelableActivity> mData;
    private final boolean mIsByFriends;

    public ParcelableActivitiesAdapter(Context context, boolean compact, boolean byFriends) {
        super(context, compact);
        mIsByFriends = byFriends;
    }

    @Override
    public boolean isGapItem(int position) {
        return getActivity(position).is_gap && position != getActivityCount() - 1;
    }

    @Override
    public long getItemId(int adapterPosition) {
        int dataPosition = adapterPosition - getActivityStartIndex();
        if (dataPosition < 0 || dataPosition >= getActivityCount()) return RecyclerView.NO_ID;
        if (mData instanceof ObjectCursor) {
            final Cursor cursor = ((ObjectCursor) mData).getCursor(dataPosition);
            final ParcelableActivityCursorIndices indices = (ParcelableActivityCursorIndices) ((ObjectCursor) mData).getIndices();
            final long account_id = cursor.getLong(indices.account_id);
            final long timestamp = cursor.getLong(indices.timestamp);
            final long max_position = cursor.getLong(indices.max_position);
            final long min_position = cursor.getLong(indices.min_position);
            final long id = cursor.getLong(indices.timestamp);
            return ParcelableActivity.calculateHashCode(account_id, timestamp, max_position,
                    min_position);
        }
        return System.identityHashCode(mData.get(dataPosition));
    }

    @Nullable
    @Override
    public String getActivityAction(int adapterPosition) {
        int dataPosition = adapterPosition - getActivityStartIndex();
        if (dataPosition < 0 || dataPosition >= getActivityCount()) return null;
        if (mData instanceof ObjectCursor) {
            final ParcelableActivityCursorIndices indices = (ParcelableActivityCursorIndices) ((ObjectCursor) mData).getIndices();
            return ((ObjectCursor) mData).getCursor(dataPosition).getString(indices.action);
        }
        return mData.get(dataPosition).action;
    }

    @Override
    public long getTimestamp(int adapterPosition) {
        int dataPosition = adapterPosition - getActivityStartIndex();
        if (dataPosition < 0 || dataPosition >= getActivityCount()) return RecyclerView.NO_ID;
        if (mData instanceof ObjectCursor) {
            final ParcelableActivityCursorIndices indices = (ParcelableActivityCursorIndices) ((ObjectCursor) mData).getIndices();
            return ((ObjectCursor) mData).getCursor(dataPosition).getLong(indices.timestamp);
        }
        return mData.get(dataPosition).timestamp;
    }

    @Override
    public ParcelableActivity getActivity(int position) {
        int offset = 0;
        if ((getLoadMoreIndicatorPosition() & IndicatorPosition.START) != 0) {
            offset = -1;
        }
        if (position == getActivityCount()) return null;
        return mData.get(position + offset);
    }

    @Override
    public int getActivityCount() {
        if (mData == null) return 0;
        return mData.size();
    }

    @Override
    protected void onSetData(List<ParcelableActivity> data) {
        mData = data;
        notifyDataSetChanged();
    }

    @Override
    protected void bindTitleSummaryViewHolder(ActivityTitleSummaryViewHolder holder, int position) {
        holder.displayActivity(getActivity(position), mIsByFriends);
    }

    @Override
    public List<ParcelableActivity> getData() {
        return mData;
    }


}