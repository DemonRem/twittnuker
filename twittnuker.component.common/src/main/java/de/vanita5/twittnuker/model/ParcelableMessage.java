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

import android.support.annotation.StringDef;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;
import com.bluelinelabs.logansquare.annotation.OnJsonParseComplete;
import com.bluelinelabs.logansquare.annotation.OnPreJsonSerialize;
import com.hannesdorfmann.parcelableplease.annotation.ParcelableNoThanks;

import org.mariotaku.commons.objectcursor.LoganSquareCursorFieldConverter;
import org.mariotaku.library.objectcursor.annotation.CursorField;
import org.mariotaku.library.objectcursor.annotation.CursorObject;
import de.vanita5.twittnuker.model.message.MessageExtras;
import de.vanita5.twittnuker.model.message.StickerExtras;
import de.vanita5.twittnuker.model.util.MessageExtrasConverter;
import de.vanita5.twittnuker.model.util.UserKeyCursorFieldConverter;
import de.vanita5.twittnuker.provider.TwidereDataStore;
import de.vanita5.twittnuker.provider.TwidereDataStore.Messages;

import java.util.Arrays;

@JsonObject
@CursorObject(tableInfo = true, valuesCreator = true)
public class ParcelableMessage {

    @CursorField(value = Messages._ID, type = TwidereDataStore.TYPE_PRIMARY_KEY, excludeWrite = true)
    public long _id;

    @JsonField(name = "account_key")
    @CursorField(value = Messages.ACCOUNT_KEY, converter = UserKeyCursorFieldConverter.class)
    public UserKey account_key;

    @JsonField(name = "id")
    @CursorField(Messages.MESSAGE_ID)
    public String id;

    @JsonField(name = "conversation_id")
    @CursorField(Messages.CONVERSATION_ID)
    public String conversation_id;

    @JsonField(name = "type")
    @CursorField(Messages.MESSAGE_TYPE)
    @MessageType
    public String message_type;

    @JsonField(name = "timestamp")
    @CursorField(Messages.MESSAGE_TIMESTAMP)
    public long message_timestamp;

    @JsonField(name = "local_timestamp")
    @CursorField(Messages.LOCAL_TIMESTAMP)
    public long local_timestamp;

    @JsonField(name = "sort_id")
    @CursorField(value = Messages.SORT_ID)
    public long sort_id;

    @JsonField(name = "text_unescaped")
    @CursorField(Messages.TEXT_UNESCAPED)
    public String text_unescaped;

    @JsonField(name = "media")
    @CursorField(value = Messages.MEDIA, converter = LoganSquareCursorFieldConverter.class)
    public ParcelableMedia[] media;

    @JsonField(name = "spans")
    @CursorField(value = Messages.SPANS, converter = LoganSquareCursorFieldConverter.class)
    public SpanItem[] spans;
    @CursorField(value = Messages.EXTRAS, converter = MessageExtrasConverter.class)
    public MessageExtras extras;

    @JsonField(name = "extras")
    @ParcelableNoThanks
    InternalExtras internalExtras;

    @JsonField(name = "sender_key")
    @CursorField(value = Messages.SENDER_KEY, converter = UserKeyCursorFieldConverter.class)
    public UserKey sender_key;

    @JsonField(name = "recipient_key")
    @CursorField(value = Messages.RECIPIENT_KEY, converter = UserKeyCursorFieldConverter.class)
    public UserKey recipient_key;

    @JsonField(name = "is_outgoing")
    @CursorField(Messages.IS_OUTGOING)
    public boolean is_outgoing;

    @JsonField(name = "request_cursor")
    @CursorField(value = Messages.REQUEST_CURSOR)
    public String request_cursor;

    @Override
    public String toString() {
        return "ParcelableMessage{" +
                "_id=" + _id +
                ", account_key=" + account_key +
                ", id='" + id + '\'' +
                ", conversation_id='" + conversation_id + '\'' +
                ", message_type='" + message_type + '\'' +
                ", message_timestamp=" + message_timestamp +
                ", local_timestamp=" + local_timestamp +
                ", sort_id=" + sort_id +
                ", text_unescaped='" + text_unescaped + '\'' +
                ", media=" + Arrays.toString(media) +
                ", spans=" + Arrays.toString(spans) +
                ", extras=" + extras +
                ", internalExtras=" + internalExtras +
                ", sender_key=" + sender_key +
                ", recipient_key=" + recipient_key +
                ", is_outgoing=" + is_outgoing +
                ", request_cursor='" + request_cursor + '\'' +
                '}';
    }

    @OnPreJsonSerialize
    void beforeJsonSerialize() {
        internalExtras = InternalExtras.from(extras);
    }


    @OnJsonParseComplete
    void onJsonParseComplete() {
        if (internalExtras != null) {
            extras = internalExtras.getExtras();
        }
    }


    @StringDef({MessageType.TEXT, MessageType.STICKER, MessageType.CONVERSATION_CREATE,
            MessageType.JOIN_CONVERSATION, MessageType.PARTICIPANTS_LEAVE})
    public @interface MessageType {
        String CONVERSATION_CREATE = "conversation_create";
        String JOIN_CONVERSATION = "join_conversation";
        String PARTICIPANTS_LEAVE = "participants_leave";
        String PARTICIPANTS_JOIN = "participants_join";
        String TEXT = "text";
        String STICKER = "sticker";
    }

    @JsonObject
    static class InternalExtras {
        @JsonField(name = "sticker")
        StickerExtras sticker;

        public static InternalExtras from(final MessageExtras extras) {
            if (extras == null) return null;
            InternalExtras result = new InternalExtras();
            if (extras instanceof StickerExtras) {
                result.sticker = (StickerExtras) extras;
            } else {
                return null;
            }
            return result;
        }

        public MessageExtras getExtras() {
            if (sticker != null) {
                return sticker;
            }
            return null;
        }
    }
}