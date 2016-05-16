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

package de.vanita5.twittnuker.library.twitter.model;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;

@JsonObject
public class StatusActivitySummary extends TwitterResponseObject implements TwitterResponse {

    @JsonField(name = "favoriters", typeConverter = IDs.Converter.class)
    IDs favoriters;
    @JsonField(name = "repliers", typeConverter = IDs.Converter.class)
    IDs repliers;
    @JsonField(name = "retweeters", typeConverter = IDs.Converter.class)
    IDs retweeters;

    @JsonField(name = "favoriters_count")
    long favoritersCount;
    @JsonField(name = "repliers_count")
    long repliersCount;
    @JsonField(name = "retweeters_count")
    long retweetersCount;
    @JsonField(name = "descendent_reply_count")
    long descendentReplyCount;

    public IDs getFavoriters() {
        return favoriters;
    }

    public IDs getRepliers() {
        return repliers;
    }

    public IDs getRetweeters() {
        return retweeters;
    }

    public long getFavoritersCount() {
        return favoritersCount;
    }

    public long getRepliersCount() {
        return repliersCount;
    }

    public long getRetweetersCount() {
        return retweetersCount;
    }

    public long getDescendentReplyCount() {
        return descendentReplyCount;
    }
}