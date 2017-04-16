/*
 * Twittnuker - Twitter client for Android
 *
 * Copyright (C) 2013-2017 vanita5 <mail@vanit.as>
 *
 * This program incorporates a modified version of Twidere.
 * Copyright (C) 2012-2017 Mariotaku Lee <mariotaku.lee@gmail.com>
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

package de.vanita5.twittnuker.util.api

import android.support.v4.util.SimpleArrayMap
import com.bluelinelabs.logansquare.JsonMapper
import com.bluelinelabs.logansquare.ParameterizedType
import org.mariotaku.commons.logansquare.LoganSquareMapperFinder

import de.vanita5.twittnuker.library.MicroBlogException
import de.vanita5.twittnuker.library.twitter.model.ResponseCode
import de.vanita5.twittnuker.library.twitter.model.TwitterResponse
import de.vanita5.twittnuker.library.twitter.util.OAuthTokenResponseConverter
import org.mariotaku.restfu.RestConverter
import org.mariotaku.restfu.http.HttpResponse
import org.mariotaku.restfu.http.mime.Body
import org.mariotaku.restfu.http.mime.SimpleBody
import org.mariotaku.restfu.logansqaure.LoganSquareConverterFactory
import org.mariotaku.restfu.oauth.OAuthToken

import java.lang.reflect.Type

/**
 * Convert JSON responses
 */
object TwitterConverterFactory : LoganSquareConverterFactory<MicroBlogException>() {

    private val responseConverters = SimpleArrayMap<Type, RestConverter<HttpResponse, *, MicroBlogException>>()
    private val sBodyConverters = SimpleArrayMap<Type, RestConverter<*, Body, MicroBlogException>>()

    init {
        responseConverters.put(ResponseCode::class.java, ResponseCode.ResponseConverter())
        responseConverters.put(OAuthToken::class.java, OAuthTokenResponseConverter())
    }

    override fun <T : Any?> mapperFor(type: ParameterizedType<T>): JsonMapper<T> {
        return LoganSquareMapperFinder.mapperFor(type)
    }

    override fun <T : Any?> mapperFor(type: Class<T>): JsonMapper<T> {
        return LoganSquareMapperFinder.mapperFor(type)
    }

    @Throws(RestConverter.ConvertException::class)
    override fun forResponse(type: Type): RestConverter<HttpResponse, *, MicroBlogException> {
        val converter = responseConverters.get(type)
        if (converter != null) {
            return converter
        }
        return super.forResponse(type)
    }

    @Throws(RestConverter.ConvertException::class)
    override fun forRequest(type: Type): RestConverter<*, Body, MicroBlogException> {
        val converter = sBodyConverters.get(type)
        if (converter != null) {
            return converter
        }
        if (SimpleBody.supports(type)) {
            return SimpleBodyConverter<MicroBlogException>(type)
        }
        return super.forRequest(type)
    }

    override fun processParsedObject(obj: Any, httpResponse: HttpResponse) {
        if (obj is TwitterResponse) {
            obj.processResponseHeader(httpResponse)
        }
    }
}