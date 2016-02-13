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

package de.vanita5.twittnuker.api.twitter.util;

import android.support.annotation.NonNull;
import android.support.v4.util.SimpleArrayMap;

import com.bluelinelabs.logansquare.LoganSquare;
import com.bluelinelabs.logansquare.ParameterizedType;
import com.bluelinelabs.logansquare.ParameterizedTypeAccessor;
import com.fasterxml.jackson.core.JsonParseException;

import org.mariotaku.restfu.RestConverter;
import org.mariotaku.restfu.http.HttpResponse;
import org.mariotaku.restfu.http.mime.Body;
import de.vanita5.twittnuker.api.twitter.TwitterException;
import de.vanita5.twittnuker.api.twitter.auth.OAuthToken;
import de.vanita5.twittnuker.api.twitter.model.ResponseCode;
import de.vanita5.twittnuker.api.twitter.model.TwitterResponse;
import de.vanita5.twittnuker.api.twitter.model.User;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;

public class TwitterConverterFactory extends RestConverter.SimpleFactory<TwitterException> {

    private static SimpleArrayMap<Type, RestConverter<HttpResponse, ?, TwitterException>> sResponseConverters = new SimpleArrayMap<>();
    private static SimpleArrayMap<Type, RestConverter<?, Body, TwitterException>> sBodyConverters = new SimpleArrayMap<>();

    static {
        sResponseConverters.put(ResponseCode.class, new ResponseCode.Converter());
        sResponseConverters.put(OAuthToken.class, new OAuthToken.Converter());
    }

    public static TwitterException parseTwitterException(HttpResponse resp) {
        try {
            final Body body = resp.getBody();
            if (body == null) return new TwitterException(resp);
            final TwitterException parse = LoganSquare.parse(body.stream(), TwitterException.class);
            if (parse != null) return parse;
            return new TwitterException(resp);
        } catch (JsonParseException e) {
            return new TwitterException("Malformed JSON Data", e, resp);
        } catch (IOException e) {
            return new TwitterException("IOException while throwing exception", e, resp);
        }
    }

    @NonNull
    private static <T> T parseOrThrow(InputStream stream, Type type)
            throws IOException, TwitterException, RestConverter.ConvertException {
        try {
            final ParameterizedType<T> parameterizedType = ParameterizedTypeAccessor.create(type);
            final T parsed = LoganSquare.parse(stream, parameterizedType);
            if (parsed == null) {
                throw new TwitterException("Empty data");
            }
            return parsed;
        } catch (JsonParseException e) {
            throw new RestConverter.ConvertException("Malformed JSON Data");
        }
    }

    private static void checkResponse(Type type, Object object, HttpResponse response) throws TwitterException {
        if (User.class == type) {
            if (object == null) throw new TwitterException("User is null");
        }
    }

    @Override
    public RestConverter<HttpResponse, ?, TwitterException> forResponse(Type type) {
        RestConverter<HttpResponse, ?, TwitterException> converter = sResponseConverters.get(type);
        if (converter != null) {
            return converter;
        }
        return new TwitterConverter(type);
    }

    @Override
    public RestConverter<?, Body, TwitterException> forRequest(Type type) {
        final RestConverter<?, Body, TwitterException> converter = sBodyConverters.get(type);
        if (converter != null) {
            return converter;
        }
        return super.forRequest(type);
    }

    public static class UnsupportedTypeException extends UnsupportedOperationException {
        public UnsupportedTypeException(Type type) {
            super("Unsupported type " + type);
        }
    }

    public static class TwitterConverter implements RestConverter<HttpResponse, Object, TwitterException> {
        private final Type type;

        public TwitterConverter(Type type) {
            this.type = type;
        }

        @Override
        public Object convert(HttpResponse httpResponse) throws IOException, ConvertException, TwitterException {
            final Body body = httpResponse.getBody();
            final InputStream stream = body.stream();
            final Object object = parseOrThrow(stream, type);
            checkResponse(type, object, httpResponse);
            if (object instanceof TwitterResponse) {
                ((TwitterResponse) object).processResponseHeader(httpResponse);
            }
            return object;
        }
    }
}