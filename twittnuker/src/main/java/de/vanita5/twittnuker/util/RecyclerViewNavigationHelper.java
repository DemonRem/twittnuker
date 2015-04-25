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

package de.vanita5.twittnuker.util;

import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.KeyEvent;
import android.view.View;

import de.vanita5.twittnuker.util.KeyboardShortcutsHandler.KeyboardShortcutCallback;

public class RecyclerViewNavigationHelper implements KeyboardShortcutCallback {

	private int positionBackup;
    @NonNull
	private final RecyclerView view;
    @NonNull
	private final LinearLayoutManager manager;
    @NonNull
	private final Adapter<ViewHolder> adapter;

    public RecyclerViewNavigationHelper(@NonNull final RecyclerView view,
                                        @NonNull final LinearLayoutManager manager,
                                        @NonNull final Adapter<ViewHolder> adapter) {
		this.view = view;
		this.manager = manager;
		this.adapter = adapter;
	}

    @Override
    public boolean handleKeyboardShortcutRepeat(@NonNull final KeyboardShortcutsHandler handler,
                                                final int keyCode, final int repeatCount,
                                                @NonNull final KeyEvent event) {
        final String action = handler.getKeyAction(CONTEXT_TAG_NAVIGATION, keyCode, event);
		if (action == null) return false;
        final int direction;
        switch (action) {
            case ACTION_NAVIGATION_PREVIOUS: {
                direction = -1;
                break;
            }
            case ACTION_NAVIGATION_NEXT: {
                direction = 1;
                break;
            }
            default: {
                return false;
            }
        }
		final LinearLayoutManager layoutManager = this.manager;
		final View focusedChild = RecyclerViewUtils.findRecyclerViewChild(view, layoutManager.getFocusedChild());
		final int position;
        final int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
        final int lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition();
        final int itemCount = adapter.getItemCount();
		if (focusedChild != null) {
			position = view.getChildLayoutPosition(focusedChild);
        } else if (firstVisibleItemPosition == 0) {
			position = -1;
        } else if (lastVisibleItemPosition == itemCount - 1) {
				position = itemCount;
        } else if (direction > 0 && positionBackup < firstVisibleItemPosition) {
            position = firstVisibleItemPosition;
        } else if (direction < 0 && positionBackup > lastVisibleItemPosition) {
            position = lastVisibleItemPosition;
		} else {
			position = positionBackup;
		}
		positionBackup = position;
        RecyclerViewUtils.focusNavigate(view, layoutManager, position, direction);
        return false;
    }

    @Override
    public boolean handleKeyboardShortcutSingle(@NonNull KeyboardShortcutsHandler handler, int keyCode, @NonNull KeyEvent event) {
		return false;
	}
}