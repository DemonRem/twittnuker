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

package de.vanita5.twittnuker.api.twitter;

import org.mariotaku.restfu.annotation.method.POST;
import org.mariotaku.restfu.annotation.param.KeyValue;
import org.mariotaku.restfu.annotation.param.Param;
import org.mariotaku.restfu.annotation.param.Params;
import org.mariotaku.restfu.http.BodyType;
import org.mariotaku.restfu.http.mime.Body;
import org.mariotaku.restfu.http.mime.FileBody;
import de.vanita5.twittnuker.api.twitter.model.MediaUploadResponse;
import de.vanita5.twittnuker.api.twitter.model.ResponseCode;

import java.io.File;

@SuppressWarnings("RedundantThrows")
public interface TwitterUpload {

    @POST("/media/upload.json")
    @BodyType(BodyType.MULTIPART)
    MediaUploadResponse uploadMedia(@Param("media") File file) throws TwitterException;

    @POST("/media/upload.json")
    @BodyType(BodyType.MULTIPART)
    MediaUploadResponse uploadMedia(@Param("media") Body data) throws TwitterException;


    @POST("/media/upload.json")
    @Params(@KeyValue(key = "command", value = "INIT"))
    MediaUploadResponse initUploadMedia(@Param("media_type") String mediaType,
                                        @Param("total_bytes") long totalBytes) throws TwitterException;

    @POST("/media/upload.json")
    @Params(@KeyValue(key = "command", value = "APPEND"))
    ResponseCode initUploadMedia(@Param("media_id") long mediaId,
                                 @Param("segment_index") int segmentIndex,
                                 @Param("media") Body media) throws TwitterException;

    @POST("/media/upload.json")
    @Params(@KeyValue(key = "command", value = "FINALIZE"))
    MediaUploadResponse initUploadMedia(@Param("media_id") long mediaId) throws TwitterException;
}