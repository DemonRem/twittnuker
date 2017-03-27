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

package de.vanita5.twittnuker.library.twitter.model;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;

import de.vanita5.twittnuker.library.twitter.model.util.UnixEpochMillisDateConverter;

import java.util.Date;

@JsonObject
public class MutedKeyword {
    @JsonField(name = "id")
    String id;
    @JsonField(name = "keyword")
    String keyword;
    @JsonField(name = "created_at", typeConverter = UnixEpochMillisDateConverter.class)
    Date createdAt;

    public String getId() {
        return id;
    }

    public String getKeyword() {
        return keyword;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "MutedKeyword{" +
                "id='" + id + '\'' +
                ", keyword='" + keyword + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}