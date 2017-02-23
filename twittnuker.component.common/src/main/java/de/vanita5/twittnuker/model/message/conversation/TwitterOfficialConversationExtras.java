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

package de.vanita5.twittnuker.model.message.conversation;

import android.os.Parcel;
import android.os.Parcelable;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;
import com.hannesdorfmann.parcelableplease.annotation.ParcelablePlease;

import de.vanita5.twittnuker.library.twitter.model.DMResponse;


@ParcelablePlease
@JsonObject
public class TwitterOfficialConversationExtras extends ConversationExtras implements Parcelable {

    @JsonField(name = "max_entry_id")
    public String maxEntryId;
    @JsonField(name = "min_entry_id")
    public String minEntryId;
    @JsonField(name = "status")
    @DMResponse.Status
    public String status;
    @JsonField(name = "max_entry_timestamp")
    public long maxEntryTimestamp;
    @JsonField(name = "read_only")
    public boolean readOnly;

    @Override
    public String toString() {
        return "TwitterOfficialConversationExtras{" +
                "maxEntryId='" + maxEntryId + '\'' +
                ", minEntryId='" + minEntryId + '\'' +
                ", status='" + status + '\'' +
                ", maxEntryTimestamp=" + maxEntryTimestamp +
                ", readOnly=" + readOnly +
                "} " + super.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        TwitterOfficialConversationExtrasParcelablePlease.writeToParcel(this, dest, flags);
    }

    public static final Creator<TwitterOfficialConversationExtras> CREATOR = new Creator<TwitterOfficialConversationExtras>() {
        public TwitterOfficialConversationExtras createFromParcel(Parcel source) {
            TwitterOfficialConversationExtras target = new TwitterOfficialConversationExtras();
            TwitterOfficialConversationExtrasParcelablePlease.readFromParcel(target, source);
            return target;
        }

        public TwitterOfficialConversationExtras[] newArray(int size) {
            return new TwitterOfficialConversationExtras[size];
        }
    };
}