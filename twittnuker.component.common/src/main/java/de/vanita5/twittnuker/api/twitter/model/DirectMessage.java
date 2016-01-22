/*
 * Twittnuker - Twitter client for Android
 *
 * Copyright (C) 2013-2016 vanita5 <mail@vanit.as>
 *
 * This program incorporates a modified version of Twidere.
 * Copyright (C) 2012-2016 Mariotaku Lee <mariotaku.lee@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.vanita5.twittnuker.api.twitter.model;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;

import de.vanita5.twittnuker.api.twitter.util.TwitterDateConverter;

import java.util.Date;

/**
 * Created by mariotaku on 15/5/7.
 */
@JsonObject
public class DirectMessage extends TwitterResponseObject implements TwitterResponse, EntitySupport {

    @JsonField(name = "created_at", typeConverter = TwitterDateConverter.class)
    Date createdAt;

    @JsonField(name = "sender")
    User sender;

    @JsonField(name = "recipient")
    User recipient;

    @JsonField(name = "entities")
    Entities entities;

    @JsonField(name = "text")
    String text;

    @JsonField(name = "id")
    long id;

    public long getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    @Override
    public HashtagEntity[] getHashtagEntities() {
        if (entities == null) return null;
        return entities.getHashtags();
    }

    @Override
    public MediaEntity[] getMediaEntities() {
        if (entities == null) return null;
        return entities.getMedia();
    }

    @Override
    public UrlEntity[] getUrlEntities() {
        if (entities == null) return null;
        return entities.getUrls();
    }

    @Override
    public UserMentionEntity[] getUserMentionEntities() {
        if (entities == null) return null;
        return entities.getUserMentions();
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public User getSender() {
        return sender;
    }

    public long getSenderId() {
        return sender.id;
    }

    public String getSenderScreenName() {
        return sender.screenName;
    }

    public User getRecipient() {
        return recipient;
    }

    public long getRecipientId() {
        return recipient.id;
    }

    public String getRecipientScreenName() {
        return recipient.screenName;
    }
}