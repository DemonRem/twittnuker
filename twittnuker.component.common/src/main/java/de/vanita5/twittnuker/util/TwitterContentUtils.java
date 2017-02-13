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

package de.vanita5.twittnuker.util;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import de.vanita5.twittnuker.library.twitter.model.User;
import de.vanita5.twittnuker.common.R;
import de.vanita5.twittnuker.model.ConsumerKeyType;

import java.nio.charset.Charset;
import java.util.zip.CRC32;

public class TwitterContentUtils {

    private TwitterContentUtils() {
    }

    public static boolean isOfficialKey(final Context context, final String consumerKey,
                                        final String consumerSecret) {
        if (context == null || consumerKey == null || consumerSecret == null) return false;
        final String[] keySecrets = context.getResources().getStringArray(R.array.values_official_consumer_secret_crc32);
        final CRC32 crc32 = new CRC32();
        final byte[] consumerSecretBytes = consumerSecret.getBytes(Charset.forName("UTF-8"));
        crc32.update(consumerSecretBytes, 0, consumerSecretBytes.length);
        final long value = crc32.getValue();
        crc32.reset();
        for (final String keySecret : keySecrets) {
            if (Long.parseLong(keySecret, 16) == value) return true;
        }
        return false;
    }

    public static String getOfficialKeyName(final Context context, final String consumerKey,
                                            final String consumerSecret) {
        if (context == null || consumerKey == null || consumerSecret == null) return null;
        final String[] keySecrets = context.getResources().getStringArray(R.array.values_official_consumer_secret_crc32);
        final String[] keyNames = context.getResources().getStringArray(R.array.names_official_consumer_secret);
        final CRC32 crc32 = new CRC32();
        final byte[] consumerSecretBytes = consumerSecret.getBytes(Charset.forName("UTF-8"));
        crc32.update(consumerSecretBytes, 0, consumerSecretBytes.length);
        final long value = crc32.getValue();
        crc32.reset();
        for (int i = 0, j = keySecrets.length; i < j; i++) {
            if (Long.parseLong(keySecrets[i], 16) == value) return keyNames[i];
        }
        return null;
    }

    @NonNull
    public static ConsumerKeyType getOfficialKeyType(final Context context, final String consumerKey,
                                                     final String consumerSecret) {
        if (context == null || consumerKey == null || consumerSecret == null) {
            return ConsumerKeyType.UNKNOWN;
        }
        final String[] keySecrets = context.getResources().getStringArray(R.array.values_official_consumer_secret_crc32);
        final String[] keyNames = context.getResources().getStringArray(R.array.types_official_consumer_secret);
        final CRC32 crc32 = new CRC32();
        final byte[] consumerSecretBytes = consumerSecret.getBytes(Charset.forName("UTF-8"));
        crc32.update(consumerSecretBytes, 0, consumerSecretBytes.length);
        final long value = crc32.getValue();
        crc32.reset();
        for (int i = 0, j = keySecrets.length; i < j; i++) {
            if (Long.parseLong(keySecrets[i], 16) == value) {
                return ConsumerKeyType.parse(keyNames[i]);
            }
        }
        return ConsumerKeyType.UNKNOWN;
    }

    public static String getProfileImageUrl(@Nullable User user) {
        if (user == null) return null;
        return TextUtils.isEmpty(user.getProfileImageUrlHttps()) ? user.getProfileImageUrl() : user.getProfileImageUrlHttps();
    }

}