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

package de.vanita5.twittnuker.library.twitter.template;

import org.mariotaku.restfu.annotation.param.KeyValue;
import org.mariotaku.restfu.annotation.param.Queries;

@Queries({@KeyValue(key = "include_entities", valueKey = "include_entities"),
        @KeyValue(key = "include_cards", valueKey = "include_cards"),
        @KeyValue(key = "cards_platform", valueKey = "cards_platform"),
        @KeyValue(key = "include_ext_alt_text", valueKey = "include_ext_alt_text"),
        @KeyValue(key = "tweet_mode", valueKey = "tweet_mode"),
        @KeyValue(key = "include_groups", value = "true"),
        @KeyValue(key = "include_conversation_info", value = "true"),
        @KeyValue(key = "ext", value = "stickerInfo,mediaRestrictions,altText"),
        @KeyValue(key = "include_blocking", value = "true"),
        @KeyValue(key = "include_blocked_by", value = "true")
})
public class DMAnnotationTemplate {
}