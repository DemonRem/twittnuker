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
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.vanita5.twittnuker.Constants;
import de.vanita5.twittnuker.R;
import de.vanita5.twittnuker.adapter.iface.IUserListsAdapter;
import de.vanita5.twittnuker.model.ParcelableUserList;
import de.vanita5.twittnuker.util.ThemeUtils;
import de.vanita5.twittnuker.util.Utils;
import de.vanita5.twittnuker.view.holder.LoadIndicatorViewHolder;
import de.vanita5.twittnuker.view.holder.UserListViewHolder;

import java.util.List;

public class ParcelableUserListsAdapter extends LoadMoreSupportAdapter<RecyclerView.ViewHolder>
        implements Constants, IUserListsAdapter<List<ParcelableUserList>> {

    public static final int ITEM_VIEW_TYPE_USER_LIST = 2;
    private final LayoutInflater mInflater;
    private final int mCardBackgroundColor;
    private final int mProfileImageStyle;
    private final int mTextSize;
    private final boolean mDisplayProfileImage;
    private final boolean mShowAbsoluteTime;
    private final boolean mNameFirst;
    private final EventListener mEventListener;
    private List<ParcelableUserList> mData;
    private UserListAdapterListener mUserListAdapterListener;


    public ParcelableUserListsAdapter(Context context) {
        super(context);
        mCardBackgroundColor = ThemeUtils.getCardBackgroundColor(context,
                ThemeUtils.getThemeBackgroundOption(context),
                ThemeUtils.getUserThemeBackgroundAlpha(context));
        mEventListener = new EventListener(this);
        mInflater = LayoutInflater.from(context);
        mTextSize = mPreferences.getInt(KEY_TEXT_SIZE, context.getResources().getInteger(R.integer.default_text_size));
        mProfileImageStyle = Utils.getProfileImageStyle(mPreferences.getString(KEY_PROFILE_IMAGE_STYLE, null));
        mDisplayProfileImage = mPreferences.getBoolean(KEY_DISPLAY_PROFILE_IMAGE, true);
        mNameFirst = mPreferences.getBoolean(KEY_NAME_FIRST, true);
        mShowAbsoluteTime = mPreferences.getBoolean(KEY_SHOW_ABSOLUTE_TIME, false);
    }

    public List<ParcelableUserList> getData() {
        return mData;
    }


    @Override
    public void setData(List<ParcelableUserList> data) {
        mData = data;
        notifyDataSetChanged();
    }

    protected void bindUserList(UserListViewHolder holder, int position) {
        holder.displayUserList(getUserList(position));
    }

    @Override
    public int getItemCount() {
        final int position = getLoadMoreIndicatorPosition();
        int count = getUserListsCount();
        if ((position & IndicatorPosition.START) != 0) {
            count++;
        }
        if ((position & IndicatorPosition.END) != 0) {
            count++;
        }
        return count;
    }

    @Override
    public ParcelableUserList getUserList(int position) {
        if (position == getUserListsCount()) return null;
        return mData.get(position);
    }

    @Override
    public long getUserListId(int position) {
        if (position == getUserListsCount()) return -1;
        return mData.get(position).id;
    }

    @Override
    public int getUserListsCount() {
        if (mData == null) return 0;
        return mData.size();
    }

    @Override
    public int getProfileImageStyle() {
        return mProfileImageStyle;
    }

    @Override
    public float getTextSize() {
        return mTextSize;
    }

    @Override
    public boolean isProfileImageEnabled() {
        return mDisplayProfileImage;
    }

    @Override
    public boolean isNameFirst() {
        return mNameFirst;
    }

    @Override
    public boolean isShowAbsoluteTime() {
        return mShowAbsoluteTime;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case ITEM_VIEW_TYPE_USER_LIST: {
                return createUserListViewHolder(this, mInflater, parent, mCardBackgroundColor);
            }
            case ITEM_VIEW_TYPE_LOAD_INDICATOR: {
                final View view = mInflater.inflate(R.layout.card_item_load_indicator, parent, false);
                return new LoadIndicatorViewHolder(view);
            }
        }
        throw new IllegalStateException("Unknown view type " + viewType);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        switch (holder.getItemViewType()) {
            case ITEM_VIEW_TYPE_USER_LIST: {
                bindUserList(((UserListViewHolder) holder), position);
                break;
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        if ((getLoadMoreIndicatorPosition() & IndicatorPosition.START) != 0 && position == 0) {
            return ITEM_VIEW_TYPE_LOAD_INDICATOR;
        }
        if (position == getUserListsCount()) {
            return ITEM_VIEW_TYPE_LOAD_INDICATOR;
        }
        return ITEM_VIEW_TYPE_USER_LIST;
    }

    public void setListener(UserListAdapterListener userListAdapterListener) {
        mUserListAdapterListener = userListAdapterListener;
    }

    @Override
    public boolean shouldShowAccountsColor() {
        return false;
    }

    @Nullable
    @Override
    public IUserListsAdapter.UserListAdapterListener getUserListAdapterListener() {
        return mEventListener;
    }

    public static UserListViewHolder createUserListViewHolder(IUserListsAdapter<?> adapter,
                                                              LayoutInflater inflater,
                                                              ViewGroup parent,
                                                              int cardBackgroundColor) {
        final View view;
        view = inflater.inflate(R.layout.card_item_user_list_compact, parent, false);
        final View itemContent = view.findViewById(R.id.item_content);
        itemContent.setBackgroundColor(cardBackgroundColor);
        final UserListViewHolder holder = new UserListViewHolder(adapter, view);
        holder.setOnClickListeners();
        holder.setupViewOptions();
        return holder;
    }

    public interface UserListAdapterListener {

        void onUserListClick(UserListViewHolder holder, int position);

        boolean onUserListLongClick(UserListViewHolder holder, int position);

    }

    static class EventListener implements IUserListsAdapter.UserListAdapterListener {

        private final ParcelableUserListsAdapter mAdapter;

        public EventListener(ParcelableUserListsAdapter adapter) {
            mAdapter = adapter;
        }

        @Override
        public void onUserListClick(UserListViewHolder holder, int position) {
            final UserListAdapterListener listener = mAdapter.mUserListAdapterListener;
            if (listener == null) return;
            listener.onUserListClick(holder, position);
        }

        @Override
        public boolean onUserListLongClick(UserListViewHolder holder, int position) {
            final UserListAdapterListener listener = mAdapter.mUserListAdapterListener;
            return listener != null && listener.onUserListLongClick(holder, position);
        }
    }
}