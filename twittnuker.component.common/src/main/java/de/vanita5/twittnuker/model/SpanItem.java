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

package de.vanita5.twittnuker.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.IntDef;
import android.text.Spanned;
import android.text.style.URLSpan;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;
import com.hannesdorfmann.parcelableplease.annotation.ParcelableNoThanks;
import com.hannesdorfmann.parcelableplease.annotation.ParcelablePlease;
import com.hannesdorfmann.parcelableplease.annotation.ParcelableThisPlease;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@JsonObject
@ParcelablePlease
public class SpanItem implements Parcelable {
    public static final Creator<SpanItem> CREATOR = new Creator<SpanItem>() {
        @Override
        public SpanItem createFromParcel(Parcel in) {
            final SpanItem obj = new SpanItem();
            SpanItemParcelablePlease.readFromParcel(obj, in);
            return obj;
        }

        @Override
        public SpanItem[] newArray(int size) {
            return new SpanItem[size];
        }
    };

    @JsonField(name = "start")
    @ParcelableThisPlease
    public int start;
    @JsonField(name = "end")
    @ParcelableThisPlease
    public int end;
    @JsonField(name = "link")
    @ParcelableThisPlease
    public String link;

    @ParcelableNoThanks
    public int orig_start = -1;
    @ParcelableNoThanks
    public int orig_end = -1;

    @ParcelableNoThanks
    @SpanType
    public int type = SpanType.LINK;

    @Override
    public String toString() {
        return "SpanItem{" +
                "start=" + start +
                ", end=" + end +
                ", link='" + link + '\'' +
                ", orig_start=" + orig_start +
                ", orig_end=" + orig_end +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        SpanItemParcelablePlease.writeToParcel(this, dest, flags);
    }

    public static SpanItem from(Spanned spanned, URLSpan span) {
        SpanItem spanItem = new SpanItem();
        spanItem.link = span.getURL();
        spanItem.start = spanned.getSpanStart(span);
        spanItem.end = spanned.getSpanEnd(span);
        return spanItem;
    }

    @IntDef({SpanType.HIDE, SpanType.LINK})
    @Retention(RetentionPolicy.SOURCE)
    public @interface SpanType {
        int HIDE = -1;
        int LINK = 0;
    }
}