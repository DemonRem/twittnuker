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

import com.bluelinelabs.logansquare.JsonMapper;
import com.bluelinelabs.logansquare.LoganSquare;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class Activity$$JsonObjectMapper extends JsonMapper<Activity> {

    public static final Activity$$JsonObjectMapper INSTANCE = new Activity$$JsonObjectMapper();
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH);

    @SuppressWarnings("TryWithIdenticalCatches")
    @Override
    public Activity parse(JsonParser jsonParser) throws IOException {
        Activity instance = new Activity();
        if (jsonParser.getCurrentToken() == null) {
            jsonParser.nextToken();
        }
        if (jsonParser.getCurrentToken() != JsonToken.START_OBJECT) {
            jsonParser.skipChildren();
            return null;
        }
        while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = jsonParser.getCurrentName();
            jsonParser.nextToken();
            parseField(instance, fieldName, jsonParser);
            jsonParser.skipChildren();
        }
        return instance;
    }

    @Override
    public void serialize(Activity activity, JsonGenerator jsonGenerator, boolean writeStartAndEnd) {
        throw new UnsupportedOperationException();
    }

    public void parseField(Activity instance, String fieldName, JsonParser jsonParser) throws IOException {
        if ("action".equals(fieldName)) {
            //noinspection ResourceType
            instance.action = jsonParser.getValueAsString();
        } else if ("created_at".equals(fieldName)) {
            try {
                instance.createdAt = DATE_FORMAT.parse(jsonParser.getValueAsString());
            } catch (ParseException e) {
                throw new IOException(e);
            }
        } else if ("min_position".equals(fieldName)) {
            instance.minPosition = jsonParser.getValueAsLong();
        } else if ("max_position".equals(fieldName)) {
            instance.maxPosition = jsonParser.getValueAsLong();
        } else if ("sources_size".equals(fieldName)) {
            instance.sourcesSize = jsonParser.getValueAsInt();
        } else if ("targets_size".equals(fieldName)) {
            instance.targetsSize = jsonParser.getValueAsInt();
        } else if ("target_objects_size".equals(fieldName)) {
            instance.targetObjectsSize = jsonParser.getValueAsInt();
        } else if ("sources".equals(fieldName)) {
            instance.sources = LoganSquare.mapperFor(User.class).parseList(jsonParser).toArray(new User[instance.sourcesSize]);
        } else if ("targets".equals(fieldName)) {
            if (instance.action == null) throw new IOException();
            switch (instance.action) {
                case Activity.Action.FAVORITE:
                case Activity.Action.REPLY:
                case Activity.Action.RETWEET:
                case Activity.Action.QUOTE:
                case Activity.Action.FAVORITED_RETWEET:
                case Activity.Action.RETWEETED_RETWEET:
                case Activity.Action.RETWEETED_MENTION:
                case Activity.Action.FAVORITED_MENTION:
                case Activity.Action.MEDIA_TAGGED:
                case Activity.Action.FAVORITED_MEDIA_TAGGED:
                case Activity.Action.RETWEETED_MEDIA_TAGGED: {
                    instance.targetStatuses = LoganSquare.mapperFor(Status.class).parseList(jsonParser).toArray(new Status[instance.targetsSize]);
                    break;
                }
                case Activity.Action.FOLLOW:
                case Activity.Action.MENTION:
                case Activity.Action.LIST_MEMBER_ADDED: {
                    instance.targetUsers = LoganSquare.mapperFor(User.class).parseList(jsonParser).toArray(new User[instance.targetsSize]);
                    break;
                }
                case Activity.Action.LIST_CREATED: {
                    instance.targetUserLists = LoganSquare.mapperFor(UserList.class).parseList(jsonParser).toArray(new UserList[instance.targetsSize]);
                    break;
                }
            }
        } else if ("target_objects".equals(fieldName)) {
            if (instance.action == null) throw new IOException();
            switch (instance.action) {
                case Activity.Action.FAVORITE:
                case Activity.Action.FOLLOW:
                case Activity.Action.MENTION:
                case Activity.Action.REPLY:
                case Activity.Action.RETWEET:
                case Activity.Action.LIST_CREATED:
                case Activity.Action.QUOTE: {
                    instance.targetObjectStatuses = LoganSquare.mapperFor(Status.class).parseList(jsonParser).toArray(new Status[instance.targetObjectsSize]);
                    break;
                }
                case Activity.Action.LIST_MEMBER_ADDED: {
                    instance.targetObjectUserLists = LoganSquare.mapperFor(UserList.class).parseList(jsonParser).toArray(new UserList[instance.targetObjectsSize]);
                    break;
                }
                case Activity.Action.FAVORITED_RETWEET:
                case Activity.Action.RETWEETED_RETWEET:
                case Activity.Action.RETWEETED_MENTION:
                case Activity.Action.FAVORITED_MENTION:
                case Activity.Action.MEDIA_TAGGED:
                case Activity.Action.FAVORITED_MEDIA_TAGGED:
                case Activity.Action.RETWEETED_MEDIA_TAGGED: {
                    instance.targetObjectUsers = LoganSquare.mapperFor(User.class).parseList(jsonParser).toArray(new User[instance.targetObjectsSize]);
                    break;
                }
            }
        }
    }
}