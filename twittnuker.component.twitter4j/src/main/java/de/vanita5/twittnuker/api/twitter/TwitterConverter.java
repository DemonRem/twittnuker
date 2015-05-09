/*
 * Twittnuker - Twitter client for Android
 *
 * Copyright (C) 2013-2015 vanita5 <mail@vanita5.de>
 *
 * This program incorporates a modified version of Twidere.
 * Copyright (C) 2012-2015 Mariotaku Lee <mariotaku.lee@gmail.com>
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

package de.vanita5.twittnuker.api.twitter;

import com.bluelinelabs.logansquare.LoganSquare;

import org.mariotaku.simplerestapi.Converter;
import org.mariotaku.simplerestapi.http.ContentType;
import org.mariotaku.simplerestapi.http.RestResponse;
import org.mariotaku.simplerestapi.http.mime.TypedData;
import de.vanita5.twittnuker.api.twitter.auth.OAuthToken;
import de.vanita5.twittnuker.api.twitter.model.impl.ResponseListImpl;
import de.vanita5.twittnuker.api.twitter.model.impl.SavedSearchImpl;
import de.vanita5.twittnuker.api.twitter.model.impl.StatusImpl;
import de.vanita5.twittnuker.api.twitter.model.impl.TypeConverterMapper;
import de.vanita5.twittnuker.api.twitter.model.impl.UserImpl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.text.ParseException;

import twitter4j.ResponseList;
import twitter4j.SavedSearch;
import twitter4j.Status;
import twitter4j.User;

/**
 * Created by mariotaku on 15/5/5.
 */
public class TwitterConverter implements Converter {

    static {
        TypeConverterMapper.register(Status.class, StatusImpl.class);
        TypeConverterMapper.register(User.class, UserImpl.class);
        TypeConverterMapper.register(SavedSearch.class, SavedSearchImpl.class);
//        TypeConverterMapper.register(DirectMessage.class, DirectMessageImpl.class);
    }

	@Override
	public Object convert(RestResponse response, Type type) throws IOException {
		final TypedData body = response.getBody();
		final ContentType contentType = body.contentType();
		final InputStream stream = body.stream();
		if (type instanceof Class<?>) {
			final Class<?> cls = (Class<?>) type;
			if (OAuthToken.class.isAssignableFrom(cls)) {
				final ByteArrayOutputStream os = new ByteArrayOutputStream();
				body.writeTo(os);
                Charset charset = contentType != null ? contentType.getCharset() : null;
                if (charset == null) {
                    charset = Charset.defaultCharset();
                }
				try {
					return new OAuthToken(os.toString(charset.name()), charset);
				} catch (ParseException e) {
					throw new IOException(e);
				}
			}
			LoganSquare.parse(stream, cls);
        } else if (type instanceof ParameterizedType) {
            final Type rawType = ((ParameterizedType) type).getRawType();
            if (rawType instanceof Class<?>) {
                final Class<?> rawClass = (Class<?>) rawType;
                if (ResponseList.class.isAssignableFrom(rawClass)) {
                    final Type elementType = ((ParameterizedType) type).getActualTypeArguments()[0];
                    return new ResponseListImpl<>(LoganSquare.parseList(stream, (Class<?>) elementType));
				}
            }
        }
        throw new UnsupportedTypeException(type);
    }

    private class UnsupportedTypeException extends UnsupportedOperationException {
        public UnsupportedTypeException(Type type) {
            super("Unsupported type " + type);
        }
	}
}