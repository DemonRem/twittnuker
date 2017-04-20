/*
 *          Twittnuker - Twitter client for Android
 *
 *  Copyright 2013-2017 vanita5 <mail@vanit.as>
 *
 *          This program incorporates a modified version of
 *          Twidere - Twitter client for Android
 *
 *  Copyright 2012-2017 Mariotaku Lee <mariotaku.lee@gmail.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package de.vanita5.twittnuker.library.mastodon.model;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;

import java.util.Arrays;

/**
 * {@see https://github.com/tootsuite/documentation/blob/master/Using-the-API/API.md#results}
 *
 * Created by mariotaku on 2017/4/17.
 */
@JsonObject
public class Results {
    /**
     * An array of matched {@link Account}
     */
    @JsonField(name = "accounts")
    Account[] accounts;
    /**
     * An array of matched {@link Status}
     */
    @JsonField(name = "statuses")
    Status[] statuses;
    /**
     * An array of matched hashtags, as strings
     */
    @JsonField(name = "hashtags")
    String[] hashtags;

    public Account[] getAccounts() {
        return accounts;
    }

    public Status[] getStatuses() {
        return statuses;
    }

    public String[] getHashtags() {
        return hashtags;
    }

    @Override
    public String toString() {
        return "Results{" +
                "accounts=" + Arrays.toString(accounts) +
                ", statuses=" + Arrays.toString(statuses) +
                ", hashtags=" + Arrays.toString(hashtags) +
                '}';
    }
}