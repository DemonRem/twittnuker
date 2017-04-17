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

package de.vanita5.twittnuker.library.mastodon.model;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;

/**
 * {@see https://github.com/tootsuite/documentation/blob/master/Using-the-API/API.md#relationship}
 *
 * Created by mariotaku on 2017/4/17.
 */
@JsonObject
public class Relationship {
    /**
     * Target account id
     */
    @JsonField(name = "id")
    String id;
    /**
     * Whether the user is currently following the account
     */
    @JsonField(name = "following")
    boolean following;
    /**
     * Whether the user is currently being followed by the account
     */
    @JsonField(name = "followed_by")
    boolean followedBy;
    /**
     * Whether the user is currently blocking the account
     */
    @JsonField(name = "blocking")
    boolean blocking;
    /**
     * Whether the user is currently muting the account
     */
    @JsonField(name = "muting")
    boolean muting;
    /**
     * Whether the user has requested to follow the account
     */
    @JsonField(name = "requested")
    boolean requested;

    public String getId() {
        return id;
    }

    public boolean isFollowing() {
        return following;
    }

    public boolean isFollowedBy() {
        return followedBy;
    }

    public boolean isBlocking() {
        return blocking;
    }

    public boolean isMuting() {
        return muting;
    }

    public boolean isRequested() {
        return requested;
    }

    @Override
    public String toString() {
        return "Relationship{" +
                "id='" + id + '\'' +
                ", following=" + following +
                ", followedBy=" + followedBy +
                ", blocking=" + blocking +
                ", muting=" + muting +
                ", requested=" + requested +
                '}';
    }
}