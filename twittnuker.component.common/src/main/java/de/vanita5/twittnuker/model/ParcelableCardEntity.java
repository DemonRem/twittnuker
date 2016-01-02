/*
 * Twittnuker - Twitter client for Android
 *
 * Copyright (C) 2013-2015 vanita5 <mail@vanit.as>
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

package de.vanita5.twittnuker.model;


import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;
import com.hannesdorfmann.parcelableplease.ParcelBagger;
import com.hannesdorfmann.parcelableplease.annotation.Bagger;
import com.hannesdorfmann.parcelableplease.annotation.ParcelablePlease;
import com.hannesdorfmann.parcelableplease.annotation.ParcelableThisPlease;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import de.vanita5.twittnuker.api.twitter.model.CardEntity;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
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
    public
    long account_id;
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

    public ParcelableCardEntity(CardEntity card, long accountId) {
        name = card.getName();
        url = card.getUrl();
        users = ParcelableUser.fromUsersArray(card.getUsers(), accountId);
        account_id = accountId;
        values = ParcelableBindingValue.from(card.getBindingValues());
    }

    public static ParcelableCardEntity fromCardEntity(CardEntity card, long accountId) {
        if (card == null) return null;
        return new ParcelableCardEntity(card, accountId);
    }

    @Nullable
    public static ParcelableBindingValue getValue(@Nullable ParcelableCardEntity entity, @Nullable String key) {
        if (entity == null) return null;
        return entity.getValue(key);
    }

    @Nullable
    public ParcelableBindingValue getValue(@Nullable String key) {
        if (key == null || values == null) return null;
        return values.get(key);
    }

    public boolean getAsBoolean(@Nullable String key, boolean def) {
        final ParcelableBindingValue value = getValue(key);
        if (value == null) return def;
        return Boolean.parseBoolean(value.value);
    }

    public String getAsString(@Nullable String key, String def) {
        final ParcelableBindingValue value = getValue(key);
        if (value == null) return def;
        return value.value;
    }

    public int getAsInteger(@Nullable String key, int def) {
        final ParcelableBindingValue value = getValue(key);
        if (value == null) return def;
        return NumberUtils.toInt(value.value, def);
    }

    public long getAsLong(@Nullable String key, long def) {
        final ParcelableBindingValue value = getValue(key);
        if (value == null) return def;
        return NumberUtils.toLong(value.value, def);
    }

    public Date getAsDate(String key, Date def) {
        final ParcelableBindingValue value = getValue(key);
        if (value == null) return def;
        try {
            return DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.parse(value.value);
        } catch (ParseException e) {
            return def;
        }
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
            final Map<String, ParcelableBindingValue> map = new ArrayMap<>(size);
            for (int i = 0; i < size; i++) {
                final String key = in.readString();
                final ParcelableBindingValue value = in.readParcelable(ParcelableBindingValue.class.getClassLoader());
                map.put(key, value);
            }
            return map;
        }

    }

}