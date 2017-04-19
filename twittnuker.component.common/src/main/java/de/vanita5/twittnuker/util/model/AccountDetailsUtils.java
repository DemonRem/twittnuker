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

package de.vanita5.twittnuker.util.model;

import de.vanita5.twittnuker.annotation.AccountType;
import de.vanita5.twittnuker.model.account.AccountExtras;
import de.vanita5.twittnuker.model.account.MastodonAccountExtras;
import de.vanita5.twittnuker.model.account.StatusNetAccountExtras;
import de.vanita5.twittnuker.model.account.TwitterAccountExtras;
import de.vanita5.twittnuker.model.account.cred.BasicCredentials;
import de.vanita5.twittnuker.model.account.cred.Credentials;
import de.vanita5.twittnuker.model.account.cred.EmptyCredentials;
import de.vanita5.twittnuker.model.account.cred.OAuth2Credentials;
import de.vanita5.twittnuker.model.account.cred.OAuthCredentials;
import de.vanita5.twittnuker.util.JsonSerializer;

import java.io.IOException;

public class AccountDetailsUtils {
    public static Credentials parseCredentials(String json, @Credentials.Type String type) {
        try {
            switch (type) {
                case Credentials.Type.OAUTH:
                case Credentials.Type.XAUTH: {
                    return JsonSerializer.parse(json, OAuthCredentials.class);
                }
                case Credentials.Type.BASIC: {
                    return JsonSerializer.parse(json, BasicCredentials.class);
                }
                case Credentials.Type.EMPTY: {
                    return JsonSerializer.parse(json, EmptyCredentials.class);
                }
                case Credentials.Type.OAUTH2: {
                    return JsonSerializer.parse(json, OAuth2Credentials.class);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        throw new UnsupportedOperationException(type);
    }

    public static AccountExtras parseAccountExtras(String json, @AccountType String type) {
        if (json == null) return null;
        try {
            switch (type) {
                case AccountType.TWITTER: {
                    return JsonSerializer.parse(json, TwitterAccountExtras.class);
                }
                case AccountType.STATUSNET: {
                    return JsonSerializer.parse(json, StatusNetAccountExtras.class);
                }
                case AccountType.MASTODON: {
                    return JsonSerializer.parse(json, MastodonAccountExtras.class);
                }
            }
        } catch (IOException e) {
            return null;
        }
        return null;
    }
}