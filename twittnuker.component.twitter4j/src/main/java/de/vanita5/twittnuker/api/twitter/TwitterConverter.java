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
import com.bluelinelabs.logansquare.typeconverters.TypeConverter;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import org.mariotaku.simplerestapi.Converter;
import org.mariotaku.simplerestapi.http.ContentType;
import org.mariotaku.simplerestapi.http.RestResponse;
import org.mariotaku.simplerestapi.http.mime.TypedData;
import de.vanita5.twittnuker.api.twitter.auth.OAuthToken;
import de.vanita5.twittnuker.api.twitter.model.impl.HashtagEntityImpl;
import de.vanita5.twittnuker.api.twitter.model.impl.Indices;
import de.vanita5.twittnuker.api.twitter.model.impl.MediaEntityImpl;
import de.vanita5.twittnuker.api.twitter.model.impl.PageableResponseListWrapper;
import de.vanita5.twittnuker.api.twitter.model.impl.PlaceImpl;
import de.vanita5.twittnuker.api.twitter.model.impl.QueryResultWrapper;
import de.vanita5.twittnuker.api.twitter.model.impl.RelationshipImpl;
import de.vanita5.twittnuker.api.twitter.model.impl.RelationshipWrapper;
import de.vanita5.twittnuker.api.twitter.model.impl.ResponseListImpl;
import de.vanita5.twittnuker.api.twitter.model.impl.SavedSearchImpl;
import de.vanita5.twittnuker.api.twitter.model.impl.StatusImpl;
import de.vanita5.twittnuker.api.twitter.model.impl.TwitterResponseImpl;
import de.vanita5.twittnuker.api.twitter.model.impl.TypeConverterMapper;
import de.vanita5.twittnuker.api.twitter.model.impl.UrlEntityImpl;
import de.vanita5.twittnuker.api.twitter.model.impl.UserImpl;
import de.vanita5.twittnuker.api.twitter.model.impl.UserMentionEntityImpl;
import de.vanita5.twittnuker.api.twitter.model.impl.Wrapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import twitter4j.ErrorInfo;
import twitter4j.GeoLocation;
import twitter4j.HashtagEntity;
import twitter4j.MediaEntity;
import twitter4j.MediaUploadResponse;
import twitter4j.PageableResponseList;
import twitter4j.Place;
import twitter4j.QueryResult;
import twitter4j.Relationship;
import twitter4j.ResponseList;
import twitter4j.SavedSearch;
import twitter4j.Status;
import twitter4j.TranslationResult;
import twitter4j.TwitterException;
import twitter4j.UrlEntity;
import twitter4j.User;
import twitter4j.UserMentionEntity;

/**
 * Created by mariotaku on 15/5/5.
 */
public class TwitterConverter implements Converter {

    private static final Map<Class<?>, Class<? extends Wrapper<?>>> wrapperMap = new HashMap<>();

    static {
        TypeConverterMapper.register(Status.class, StatusImpl.class);
        TypeConverterMapper.register(User.class, UserImpl.class);
        TypeConverterMapper.register(SavedSearch.class, SavedSearchImpl.class);
        TypeConverterMapper.register(UrlEntity.class, UrlEntityImpl.class);
        TypeConverterMapper.register(MediaEntity.class, MediaEntityImpl.class);
        TypeConverterMapper.register(MediaEntity.Size.class, MediaEntityImpl.SizeImpl.class);
        TypeConverterMapper.register(MediaEntity.Feature.class, MediaEntityImpl.FeatureImpl.class);
        TypeConverterMapper.register(MediaEntity.Feature.Face.class, MediaEntityImpl.FeatureImpl.FaceImpl.class);
        TypeConverterMapper.register(MediaEntity.VideoInfo.class, MediaEntityImpl.VideoInfoImpl.class);
        TypeConverterMapper.register(MediaEntity.VideoInfo.Variant.class, MediaEntityImpl.VideoInfoImpl.VariantImpl.class);
        TypeConverterMapper.register(UserMentionEntity.class, UserMentionEntityImpl.class);
        TypeConverterMapper.register(HashtagEntity.class, HashtagEntityImpl.class);
        TypeConverterMapper.register(Place.class, PlaceImpl.class);
        TypeConverterMapper.register(Relationship.class, RelationshipImpl.class);
        TypeConverterMapper.register(MediaUploadResponse.class, MediaUploadResponseImpl.class);
        TypeConverterMapper.register(MediaUploadResponse.Image.class, MediaUploadResponseImpl.ImageImpl.class);
        TypeConverterMapper.register(ErrorInfo.class, ErrorInfoImpl.class);
        TypeConverterMapper.register(TranslationResult.class, TranslationResultImpl.class);

        LoganSquare.registerTypeConverter(Indices.class, Indices.CONVERTER);
        LoganSquare.registerTypeConverter(GeoLocation.class, GeoLocation.CONVERTER);
        LoganSquare.registerTypeConverter(MediaEntity.Type.class, EnumConverter.get(MediaEntity.Type.class));

        registerWrapper(QueryResult.class, QueryResultWrapper.class);
        registerWrapper(PageableResponseList.class, PageableResponseListWrapper.class);
        registerWrapper(Relationship.class, RelationshipWrapper.class);
//        TypeConverterMapper.register(DirectMessage.class, DirectMessageImpl.class);
    }

	@Override
    public Object convert(RestResponse response, Type type) throws Exception {
		final TypedData body = response.getBody();
        if (!response.isSuccessful()) {
            throw LoganSquare.parse(body.stream(), TwitterException.class);
        }
        final ContentType contentType = body.contentType();
		final InputStream stream = body.stream();
		if (type instanceof Class<?>) {
			final Class<?> cls = (Class<?>) type;
            final Class<?> wrapperCls = wrapperMap.get(cls);
            if (wrapperCls != null) {
                final Wrapper<?> wrapper = (Wrapper<?>) LoganSquare.parse(stream, wrapperCls);
                wrapper.processResponseHeader(response);
                return wrapper.getWrapped(null);
            } else if (OAuthToken.class.isAssignableFrom(cls)) {
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
            final Object object = LoganSquare.parse(stream, cls);
            checkResponse(cls, object, response);
            if (object instanceof TwitterResponseImpl) {
                ((TwitterResponseImpl) object).processResponseHeader(response);
            }
            return object;
        } else if (type instanceof ParameterizedType) {
            final Type rawType = ((ParameterizedType) type).getRawType();
            if (rawType instanceof Class<?>) {
                final Class<?> rawClass = (Class<?>) rawType;
                final Class<?> wrapperCls = wrapperMap.get(rawClass);
                if (wrapperCls != null) {
                    final Wrapper<?> wrapper = (Wrapper<?>) LoganSquare.parse(stream, wrapperCls);
                    wrapper.processResponseHeader(response);
                    return wrapper.getWrapped(((ParameterizedType) type).getActualTypeArguments());
                } else if (ResponseList.class.isAssignableFrom(rawClass)) {
                    final Type elementType = ((ParameterizedType) type).getActualTypeArguments()[0];
                    final ResponseListImpl<?> responseList = new ResponseListImpl<>(LoganSquare.parseList(stream, (Class<?>) elementType));
                    responseList.processResponseHeader(response);
                    return responseList;
				}
            }
        }
        throw new UnsupportedTypeException(type);
    }

    private void checkResponse(Class<?> cls, Object object, RestResponse response) throws TwitterException {
        if (User.class.isAssignableFrom(cls)) {
            if (object == null) throw new TwitterException("User is null");
        }
    }

    private static <T> void registerWrapper(Class<T> cls, Class<? extends Wrapper<? extends T>> wrapperCls) {
        wrapperMap.put(cls, wrapperCls);
    }

    private static class EnumConverter<T extends Enum<T>> implements TypeConverter<T> {
        private final Class<T> cls;

        EnumConverter(Class<T> cls) {
            this.cls = cls;
        }

        @SuppressWarnings({"unchecked", "TryWithIdenticalCatches"})
        @Override
        public T parse(JsonParser jsonParser) throws IOException {
            try {
                final Method method = cls.getMethod("parse", String.class);
                return (T) method.invoke(null, jsonParser.getValueAsString());
            } catch (NoSuchMethodException e) {
                return Enum.valueOf(cls, jsonParser.getValueAsString());
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void serialize(T object, String fieldName, boolean writeFieldNameForObject, JsonGenerator jsonGenerator) {
            throw new UnsupportedOperationException();
        }

        public static <T extends Enum<T>> EnumConverter<T> get(Class<T> cls) {
            return new EnumConverter<>(cls);
    	}
    }

    public static class UnsupportedTypeException extends UnsupportedOperationException {
        public UnsupportedTypeException(Type type) {
            super("Unsupported type " + type);
        }
	}
}