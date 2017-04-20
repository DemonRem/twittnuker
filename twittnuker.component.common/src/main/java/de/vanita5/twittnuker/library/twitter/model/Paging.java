/*
 *          Twittnuker - Twitter client for Android
 *
 *  Copyright 2013-2017 vanita5 <mail@vanit.as>
 *
 *          This program incorporates a modified version of
 *          Twidere - Twitter client for Android
 *
 *  Copyright 2012-2017 Mariotaku Lee <mariotaku.lee@gmail.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package de.vanita5.twittnuker.library.twitter.model;

import org.mariotaku.restfu.http.SimpleValueMap;

public class Paging extends SimpleValueMap {

    public void setSinceId(String sinceId) {
        put("since_id", sinceId);
    }

    public void setMaxId(String maxId) {
        put("max_id", maxId);
    }

    public void setMinPosition(long minPosition) {
        put("min_position", minPosition);
    }

    public void setMaxPosition(long maxPosition) {
        put("max_position", maxPosition);
    }

    public void setCount(int count) {
        put("count", count);
    }

    public void setPage(int page) {
        put("page", page);
    }

    public void setCursor(long cursor) {
        put("cursor", cursor);
    }

    public void setCursor(String cursor) {
        put("cursor", cursor);
    }

    public void setLatestResults(boolean latestResults) {
        put("latest_results", latestResults);
    }

    public void setRpp(int rpp) {
        put("rpp", rpp);
    }

    public Paging sinceId(String sinceId) {
        setSinceId(sinceId);
        return this;
    }

    public Paging latestResults(boolean latestResults) {
        setLatestResults(latestResults);
        return this;
    }

    public Paging maxId(String maxId) {
        setMaxId(maxId);
        return this;
    }

    public Paging maxPosition(long maxPosition) {
        setMaxPosition(maxPosition);
        return this;
    }

    public Paging minPosition(long minPosition) {
        setMinPosition(minPosition);
        return this;
    }

    public Paging count(int count) {
        setCount(count);
        return this;
    }

    public Paging page(int page) {
        setPage(page);
        return this;
    }

    public Paging cursor(long cursor) {
        setCursor(cursor);
        return this;
    }

    public Paging cursor(String cursor) {
        setCursor(cursor);
        return this;
    }

    public Paging rpp(int rpp) {
        setRpp(rpp);
        return this;
    }
}