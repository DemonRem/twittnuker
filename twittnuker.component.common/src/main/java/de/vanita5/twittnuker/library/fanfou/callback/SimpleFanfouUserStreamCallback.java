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

package de.vanita5.twittnuker.library.fanfou.callback;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import de.vanita5.twittnuker.library.twitter.model.Status;
import de.vanita5.twittnuker.library.twitter.model.User;

import java.io.IOException;
import java.util.Date;


public abstract class SimpleFanfouUserStreamCallback extends FanfouUserStreamCallback {
    @Override
    protected boolean onConnected() {
        return false;
    }

    @Override
    protected boolean onDisconnect(final int code, final String reason) {
        return false;
    }

    @Override
    protected boolean onException(@NonNull final Throwable ex) {
        return false;
    }

    @Override
    protected boolean onStatusCreation(@NonNull final Date createdAt, @NonNull final User source,
                                       @Nullable final User target, @NonNull final Status status) {
        return false;
    }

    @Override
    protected void onUnhandledEvent(@NonNull final String event, @NonNull final String json)
            throws IOException {

    }
}