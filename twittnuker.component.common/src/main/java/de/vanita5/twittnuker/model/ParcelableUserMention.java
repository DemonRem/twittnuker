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

package de.vanita5.twittnuker.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;
import com.hannesdorfmann.parcelableplease.annotation.ParcelablePlease;
import com.hannesdorfmann.parcelableplease.annotation.ParcelableThisPlease;

import de.vanita5.twittnuker.api.twitter.model.UserMentionEntity;
import de.vanita5.twittnuker.model.util.UserKeyConverter;

@JsonObject
@ParcelablePlease(allFields = false)
public class ParcelableUserMention implements Parcelable {

    @ParcelableThisPlease
    @JsonField(name = "id", typeConverter = UserKeyConverter.class)
    public UserKey key;
    @ParcelableThisPlease
    @JsonField(name = "name")
    public String name;
    @ParcelableThisPlease
    @JsonField(name = "screen_name")
    public String screen_name;

    public ParcelableUserMention() {

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ParcelableUserMention that = (ParcelableUserMention) o;

        if (key != null ? !key.equals(that.key) : that.key != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        return !(screen_name != null ? !screen_name.equals(that.screen_name) : that.screen_name != null);

    }

    @Override
    public int hashCode() {
        int result = key != null ? key.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (screen_name != null ? screen_name.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ParcelableUserMention{id=" + key + ", name=" + name + ", screen_name=" + screen_name + "}";
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        ParcelableUserMentionParcelablePlease.writeToParcel(this, dest, flags);
    }

    public static final Creator<ParcelableUserMention> CREATOR = new Creator<ParcelableUserMention>() {
        public ParcelableUserMention createFromParcel(Parcel source) {
            ParcelableUserMention target = new ParcelableUserMention();
            ParcelableUserMentionParcelablePlease.readFromParcel(target, source);
            return target;
        }

        public ParcelableUserMention[] newArray(int size) {
            return new ParcelableUserMention[size];
        }
    };
}