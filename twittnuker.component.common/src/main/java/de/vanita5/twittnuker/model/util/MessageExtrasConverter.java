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

package de.vanita5.twittnuker.model.util;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

import com.bluelinelabs.logansquare.LoganSquare;

import org.mariotaku.library.objectcursor.converter.CursorFieldConverter;
import de.vanita5.twittnuker.model.message.MessageExtras;
import de.vanita5.twittnuker.provider.TwidereDataStore.Messages;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;

public class MessageExtrasConverter implements CursorFieldConverter<MessageExtras> {
    @Override
    public MessageExtras parseField(Cursor cursor, int columnIndex, ParameterizedType fieldType) throws IOException {
        final String messageType = cursor.getString(cursor.getColumnIndex(Messages.MESSAGE_TYPE));
        if (TextUtils.isEmpty(messageType)) return null;
        return MessageExtras.parse(messageType, cursor.getString(columnIndex));
    }

    @Override
    public void writeField(ContentValues values, MessageExtras object, String columnName, ParameterizedType fieldType) throws IOException {
        if (object == null) return;
        values.put(columnName, LoganSquare.serialize(object));
    }
}