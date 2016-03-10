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

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;

@JsonObject
public class UserEventsResponse extends TwitterResponseObject {
    @JsonField(name = "user_events")
    UserEvents userEvents;

    public UserEvents getUserEvents() {
        return userEvents;
    }

    @JsonObject
    public static class UserEvents {
        @JsonField(name = "cursor")
        String cursor;
        @JsonField(name = "last_seen_event_id")
        long lastSeenEventId;

        public String getCursor() {
            return cursor;
        }

        public long getLastSeenEventId() {
            return lastSeenEventId;
        }
    }

}