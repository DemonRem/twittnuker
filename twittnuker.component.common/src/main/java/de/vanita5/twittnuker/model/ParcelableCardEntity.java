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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;
import com.hannesdorfmann.parcelableplease.ParcelBagger;
import com.hannesdorfmann.parcelableplease.annotation.Bagger;
import com.hannesdorfmann.parcelableplease.annotation.ParcelablePlease;
import com.hannesdorfmann.parcelableplease.annotation.ParcelableThisPlease;

import de.vanita5.twittnuker.api.twitter.model.CardEntity;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@JsonObject
@ParcelablePlease
public final class ParcelableCardEntity implements Parcelable {

    public static final Creator<ParcelableCardEntity> CREATOR = new Creator<ParcelableCardEntity>() {
        public ParcelableCardEntity createFromParcel(Parcel source) {
            ParcelableCardEntity target = new ParcelableCardEntity();
            ParcelableCardEntityParcelablePlease.readFromParcel(target, source);
            return target;
        }

        public ParcelableCardEntity[] newArray(int size) {
            return new ParcelableCardEntity[size];
        }
    };
    @ParcelableThisPlease
    @JsonField(name = "account_id")
    public long account_id;
    @ParcelableThisPlease
    @JsonField(name = "account_host")
    public String account_host;
    @ParcelableThisPlease
    @JsonField(name = "name")
    public String name;
    @ParcelableThisPlease
    @JsonField(name = "url")
    public String url;
    @ParcelableThisPlease
    @JsonField(name = "users")
    public ParcelableUser[] users;
    @ParcelableThisPlease
    @Bagger(ValueMapConverter.class)
    @JsonField(name = "values")
    public Map<String, ParcelableBindingValue> values;

    public ParcelableCardEntity() {

    }

    @Nullable
    public ParcelableBindingValue getValue(@NonNull String key) {
        if (values == null) return null;
        return values.get(key);
    }

    @Override
    public String toString() {
        return "ParcelableCardEntity{" +
                "account_id=" + account_id +
                ", name='" + name + '\'' +
                ", url='" + url + '\'' +
                ", users=" + Arrays.toString(users) +
                ", values=" + values +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        ParcelableCardEntityParcelablePlease.writeToParcel(this, dest, flags);
    }

    public static class ValueMapConverter implements ParcelBagger<Map<String, ParcelableBindingValue>> {

        @Override
        public void write(Map<String, ParcelableBindingValue> map, Parcel out, int flags) {
            if (map != null) {
                final int size = map.size();
                out.writeInt(size);
                for (Map.Entry<String, ParcelableBindingValue> entry : map.entrySet()) {
                    out.writeString(entry.getKey());
                    out.writeParcelable(entry.getValue(), flags);
                }
            } else {
                out.writeInt(-1);
            }
        }

        @Override
        public Map<String, ParcelableBindingValue> read(Parcel in) {
            final int size = in.readInt();
            if (size == -1) return null;
            final Map<String, ParcelableBindingValue> map = new HashMap<>(size);
            for (int i = 0; i < size; i++) {
                final String key = in.readString();
                final ParcelableBindingValue value = in.readParcelable(ParcelableBindingValue.class.getClassLoader());
                map.put(key, value);
            }
            return map;
        }

    }

    @ParcelablePlease
    @JsonObject
    public static final class ParcelableBindingValue implements Parcelable {

        public static final Creator<ParcelableBindingValue> CREATOR = new Creator<ParcelableBindingValue>() {
            public ParcelableBindingValue createFromParcel(Parcel source) {
                ParcelableBindingValue target = new ParcelableBindingValue();
                ParcelableCardEntity$ParcelableBindingValueParcelablePlease.readFromParcel(target, source);
                return target;
            }

            public ParcelableBindingValue[] newArray(int size) {
                return new ParcelableBindingValue[size];
            }
        };
        @ParcelableThisPlease
        @JsonField(name = "type")
        public String type;
        @ParcelableThisPlease
        @JsonField(name = "value")
        public String value;

        public ParcelableBindingValue() {
        }

        public ParcelableBindingValue(CardEntity.BindingValue value) {
            if (value instanceof CardEntity.ImageValue) {
                this.type = CardEntity.BindingValue.TYPE_IMAGE;
                this.value = ((CardEntity.ImageValue) value).getUrl();
            } else if (value instanceof CardEntity.StringValue) {
                this.type = CardEntity.BindingValue.TYPE_STRING;
                this.value = ((CardEntity.StringValue) value).getValue();
            } else if (value instanceof CardEntity.BooleanValue) {
                this.type = CardEntity.BindingValue.TYPE_BOOLEAN;
                this.value = String.valueOf(((CardEntity.BooleanValue) value).getValue());
            } else if (value instanceof CardEntity.UserValue) {
                this.type = CardEntity.BindingValue.TYPE_USER;
                this.value = String.valueOf(((CardEntity.UserValue) value).getUserId());
            }
        }

        @Override
        public String toString() {
            return value + " (" + type + ")";
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            ParcelableCardEntity$ParcelableBindingValueParcelablePlease.writeToParcel(this, dest, flags);
        }
    }
}