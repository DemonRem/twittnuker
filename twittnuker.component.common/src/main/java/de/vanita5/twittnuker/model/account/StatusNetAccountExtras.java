/*
 *          Twittnuker - Twitter client for Android
 *
 *          This program incorporates a modified version of
 *          Twidere - Twitter client for Android
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package de.vanita5.twittnuker.model.account;

import android.os.Parcel;
import android.os.Parcelable;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;
import com.hannesdorfmann.parcelableplease.annotation.ParcelablePlease;
import com.hannesdorfmann.parcelableplease.annotation.ParcelableThisPlease;

@ParcelablePlease
@JsonObject
public class StatusNetAccountExtras implements Parcelable, AccountExtras {

    public static final Creator<StatusNetAccountExtras> CREATOR = new Creator<StatusNetAccountExtras>() {
        @Override
        public StatusNetAccountExtras createFromParcel(Parcel source) {
            StatusNetAccountExtras target = new StatusNetAccountExtras();
            StatusNetAccountExtrasParcelablePlease.readFromParcel(target, source);
            return target;
        }

        @Override
        public StatusNetAccountExtras[] newArray(int size) {
            return new StatusNetAccountExtras[size];
        }
    };

    @ParcelableThisPlease
    @JsonField(name = "text_limit")
    int textLimit;

    public int getTextLimit() {
        return textLimit;
    }

    public void setTextLimit(int textLimit) {
        this.textLimit = textLimit;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        StatusNetAccountExtrasParcelablePlease.writeToParcel(this, dest, flags);
    }
}