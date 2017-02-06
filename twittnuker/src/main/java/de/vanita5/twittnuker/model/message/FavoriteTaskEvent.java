/*
 * Twittnuker - Twitter client for Android
 *
 * Copyright (C) 2013-2017 vanita5 <mail@vanit.as>
 *
 * This program incorporates a modified version of Twidere.
 * Copyright (C) 2012-2017 Mariotaku Lee <mariotaku.lee@gmail.com>
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

package de.vanita5.twittnuker.model.message;

import android.support.annotation.IntDef;
import android.support.annotation.Nullable;

import de.vanita5.twittnuker.model.UserKey;
import de.vanita5.twittnuker.model.ParcelableStatus;

public class FavoriteTaskEvent {

    private int action;
    private UserKey mAccountKey;
    private String statusId;

    @Nullable
    private ParcelableStatus status;
    private boolean finished;
    private boolean succeeded;

    public FavoriteTaskEvent(@Action final int action, final UserKey accountKey, final String statusId) {
        this.action = action;
        this.mAccountKey = accountKey;
        this.statusId = statusId;
    }

    public int getAction() {
        return action;
    }

    public UserKey getAccountKey() {
        return mAccountKey;
    }

    public String getStatusId() {
        return statusId;
    }

    @Nullable
    public ParcelableStatus getStatus() {
        return status;
    }

    public void setStatus(@Nullable ParcelableStatus status) {
        this.status = status;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public boolean isSucceeded() {
        return succeeded;
    }

    public void setSucceeded(boolean succeeded) {
        this.succeeded = succeeded;
    }

    @IntDef({Action.CREATE, Action.DESTROY})
    public @interface Action {
        int CREATE = 1;
        int DESTROY = 2;
    }
}