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
import android.support.annotation.Nullable;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;
import com.hannesdorfmann.parcelableplease.annotation.ParcelablePlease;
import com.hannesdorfmann.parcelableplease.annotation.ParcelableThisPlease;

import org.mariotaku.library.objectcursor.annotation.CursorField;
import org.mariotaku.library.objectcursor.annotation.CursorObject;

import de.vanita5.twittnuker.model.util.LoganSquareCursorFieldConverter;
import de.vanita5.twittnuker.provider.TwidereDataStore.Accounts;

@CursorObject(valuesCreator = true)
@ParcelablePlease(allFields = false)
@JsonObject
public class ParcelableAccount implements Parcelable {

    @ParcelableThisPlease
    @JsonField(name = "screen_name")
    @CursorField(Accounts.SCREEN_NAME)
    public String screen_name;

    @ParcelableThisPlease
    @JsonField(name = "name")
    @CursorField(Accounts.NAME)
    public String name;

    @ParcelableThisPlease
    @JsonField(name = "profile_image_url")
    @CursorField(Accounts.PROFILE_IMAGE_URL)
    public String profile_image_url;

    @ParcelableThisPlease
    @JsonField(name = "profile_banner_url")
    @CursorField(Accounts.PROFILE_BANNER_URL)
    public String profile_banner_url;

    @ParcelableThisPlease
    @JsonField(name = "account_id")
    @CursorField(Accounts.ACCOUNT_ID)
    public long account_id;

    @ParcelableThisPlease
    @JsonField(name = "color")
    @CursorField(Accounts.COLOR)
    public int color;

    @ParcelableThisPlease
    @JsonField(name = "is_activated")
    @CursorField(Accounts.IS_ACTIVATED)
    public boolean is_activated;

    @Nullable
    @ParcelableThisPlease
    @JsonField(name = "account_type")
    @CursorField(Accounts.ACCOUNT_TYPE)
    public String account_type;

    @Nullable
    @ParcelableThisPlease
    @JsonField(name = "account_user")
    @CursorField(value = Accounts.ACCOUNT_USER, converter = LoganSquareCursorFieldConverter.class)
    public ParcelableUser account_user;

    public static final Creator<ParcelableAccount> CREATOR = new Creator<ParcelableAccount>() {
        public ParcelableAccount createFromParcel(Parcel source) {
            ParcelableAccount target = new ParcelableAccount();
            ParcelableAccountParcelablePlease.readFromParcel(target, source);
            return target;
        }

        public ParcelableAccount[] newArray(int size) {
            return new ParcelableAccount[size];
        }
    };
    public boolean is_dummy;

    ParcelableAccount() {
    }

    public static ParcelableCredentials dummyCredentials() {
        final ParcelableCredentials credentials = new ParcelableCredentials();
        credentials.is_dummy = true;
        return credentials;
    }

    @Override
    public String toString() {
        return "ParcelableAccount{" +
                "screen_name='" + screen_name + '\'' +
                ", name='" + name + '\'' +
                ", profile_image_url='" + profile_image_url + '\'' +
                ", profile_banner_url='" + profile_banner_url + '\'' +
                ", account_id=" + account_id +
                ", color=" + color +
                ", is_activated=" + is_activated +
                ", account_type='" + account_type + '\'' +
                ", is_dummy=" + is_dummy +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
        }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        ParcelableAccountParcelablePlease.writeToParcel(this, dest, flags);
    }
}