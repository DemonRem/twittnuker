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

import de.vanita5.twittnuker.Constants;
import de.vanita5.twittnuker.model.AccountKey;
import de.vanita5.twittnuker.model.ParcelableStatus;
import de.vanita5.twittnuker.model.ParcelableUser;

import java.util.ArrayList;
import java.util.List;

public class MultiSelectManager implements Constants {

    private final NoDuplicatesArrayList<Long> mSelectedStatusIds = new NoDuplicatesArrayList<>();
    private final NoDuplicatesArrayList<Long> mSelectedUserIds = new NoDuplicatesArrayList<>();
    private final NoDuplicatesArrayList<Callback> mCallbacks = new NoDuplicatesArrayList<>();
    private final ItemsList mSelectedItems = new ItemsList(this);
    private AccountKey mAccountKey;

    public void clearSelectedItems() {
        mSelectedItems.clear();
    }

    public AccountKey getAccountKey() {
        if (mAccountKey == null) return getFirstSelectAccountKey(mSelectedItems);
        return mAccountKey;
    }

    public int getCount() {
        return mSelectedItems.size();
    }

    public AccountKey getFirstSelectAccountKey() {
        return getFirstSelectAccountKey(mSelectedItems);
    }

    public List<Object> getSelectedItems() {
        return mSelectedItems;
    }

    public boolean isActive() {
        return !mSelectedItems.isEmpty();
    }

    public boolean isSelected(final Object object) {
        return mSelectedItems.contains(object);
    }

    public boolean isStatusSelected(final long status_id) {
        return mSelectedStatusIds.contains(status_id);
    }

    public boolean isUserSelected(final long user_id) {
        return mSelectedUserIds.contains(user_id);
    }

    public void registerCallback(final Callback callback) {
        if (callback == null) return;
        mCallbacks.add(callback);
    }

    public boolean selectItem(final Object item) {
        return mSelectedItems.add(item);
    }

    public void setAccountKey(final AccountKey accountKey) {
        mAccountKey = accountKey;
    }

    public void unregisterCallback(final Callback callback) {
        mCallbacks.remove(callback);
    }

    public boolean deselectItem(final Object item) {
        return mSelectedItems.remove(item);
    }

    private void onItemsCleared() {
        for (final Callback callback : mCallbacks) {
            callback.onItemsCleared();
        }
        mAccountKey = null;
    }

    private void onItemSelected(final Object object) {
        for (final Callback callback : mCallbacks) {
            callback.onItemSelected(object);
        }
    }

    private void onItemUnselected(final Object object) {
        for (final Callback callback : mCallbacks) {
            callback.onItemUnselected(object);
        }
    }

    public static AccountKey getFirstSelectAccountKey(final List<Object> selectedItems) {
        final Object obj = selectedItems.get(0);
        if (obj instanceof ParcelableUser) {
            final ParcelableUser user = (ParcelableUser) obj;
            return user.account_key;
        } else if (obj instanceof ParcelableStatus) {
            final ParcelableStatus status = (ParcelableStatus) obj;
            return status.account_key;
        }
        return null;
    }

    public static long[] getSelectedUserIds(final List<Object> selected_items) {
        final ArrayList<Long> ids_list = new ArrayList<>();
        for (final Object item : selected_items) {
            if (item instanceof ParcelableUser) {
                ids_list.add(((ParcelableUser) item).id);
            } else if (item instanceof ParcelableStatus) {
                ids_list.add(((ParcelableStatus) item).user_id);
            }
        }
        return TwidereArrayUtils.fromList(ids_list);
    }

    public interface Callback {

        void onItemsCleared();

        void onItemSelected(Object item);

        void onItemUnselected(Object item);

    }

    @SuppressWarnings("serial")
    static class ItemsList extends NoDuplicatesArrayList<Object> {

        private final MultiSelectManager manager;

        ItemsList(final MultiSelectManager manager) {
            this.manager = manager;
        }

        @Override
        public boolean add(final Object object) {
            if (object instanceof ParcelableStatus) {
                manager.mSelectedStatusIds.add(((ParcelableStatus) object).id);
            } else if (object instanceof ParcelableUser) {
                manager.mSelectedUserIds.add(((ParcelableUser) object).id);
            } else
                return false;
            final boolean ret = super.add(object);
            manager.onItemSelected(object);
            return ret;
        }

        @Override
        public void clear() {
            super.clear();
            manager.mSelectedStatusIds.clear();
            manager.mSelectedUserIds.clear();
            manager.onItemsCleared();
        }

        @Override
        public boolean remove(final Object object) {
            final boolean ret = super.remove(object);
            if (object instanceof ParcelableStatus) {
                manager.mSelectedStatusIds.remove(((ParcelableStatus) object).id);
            } else if (object instanceof ParcelableUser) {
                manager.mSelectedUserIds.remove(((ParcelableUser) object).id);
            }
            if (ret) {
                if (isEmpty()) {
                    manager.onItemsCleared();
                } else {
                    manager.onItemUnselected(object);
                }
            }
            return ret;
        }

    }
}