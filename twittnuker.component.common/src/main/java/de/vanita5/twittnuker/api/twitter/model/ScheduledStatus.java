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

package de.vanita5.twittnuker.api.twitter.model;

import android.support.annotation.StringDef;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;

import de.vanita5.twittnuker.api.twitter.util.TwitterDateConverter;

import java.util.Date;

@JsonObject
public class ScheduledStatus {

    @JsonField(name = "updated_at", typeConverter = TwitterDateConverter.class)
    Date updatedAt;
    @JsonField(name = "created_at", typeConverter = TwitterDateConverter.class)
    Date createdAt;
    @JsonField(name = "execute_at", typeConverter = TwitterDateConverter.class)
    Date executeAt;
    @JsonField(name = "text")
    String text;
    @JsonField(name = "media_ids")
    long[] mediaIds;
    @JsonField(name = "id")
    long id;
    @JsonField(name = "possiblySensitive")
    boolean possiblySensitive;
    @JsonField(name = "user_id")
    long userId;
    @JsonField(name = "state")
    @State
    String state;

    public long getUserId() {
        return userId;
    }

    public boolean isPossiblySensitive() {
        return possiblySensitive;
    }

    public long getId() {
        return id;
    }

    public long[] getMediaIds() {
        return mediaIds;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public Date getExecuteAt() {
        return executeAt;
    }

    public String getText() {
        return text;
    }

    public
    @State
    String getState() {
        return state;
    }

    @StringDef({State.SCHEDULED, State.FAILED, State.CANCELED})
    public @interface State {
        String SCHEDULED = ("scheduled"), FAILED = ("failed"), CANCELED = ("canceled");

    }
}