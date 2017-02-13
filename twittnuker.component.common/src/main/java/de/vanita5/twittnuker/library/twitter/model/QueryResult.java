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

package de.vanita5.twittnuker.library.twitter.model;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;

import org.mariotaku.restfu.http.HttpResponse;
import de.vanita5.twittnuker.library.twitter.util.InternalParseUtil;

import java.util.AbstractList;
import java.util.ArrayList;

/**
 * Created by mariotaku on 15/5/7.
 */
@JsonObject
public class QueryResult extends AbstractList<Status> implements TwitterResponse, CursorSupport {

    @JsonField(name = "previous_cursor")
    long previousCursor;
    @JsonField(name = "next_cursor")
    long nextCursor;

    @JsonField(name = "search_metadata")
    SearchMetadata metadata;

    @JsonField(name = "statuses")
    ArrayList<Status> statuses;

    @AccessLevel
    private int accessLevel;
    private RateLimitStatus rateLimitStatus;

    @Override
    public final void processResponseHeader(HttpResponse resp) {
        rateLimitStatus = RateLimitStatus.createFromResponseHeader(resp);
        accessLevel = InternalParseUtil.toAccessLevel(resp);
    }

    @AccessLevel
    @Override
    public final int getAccessLevel() {
        return accessLevel;
    }

    @Override
    public final RateLimitStatus getRateLimitStatus() {
        return rateLimitStatus;
    }

    @Override
    public Status get(int index) {
        return statuses.get(index);
    }

    @Override
    public int size() {
        return statuses.size();
    }

    public double getCompletedIn() {
        return metadata.completedIn;
    }

    public long getMaxId() {
        return metadata.maxId;
    }

    public String getQuery() {
        return metadata.query;
    }

    public int getResultsPerPage() {
        return metadata.count;
    }

    @Override
    public long getNextCursor() {
        return nextCursor;
    }

    @Override
    public boolean hasNext() {
        return nextCursor != 0;
    }

    @Override
    public boolean hasPrevious() {
        return previousCursor != 0;
    }

    @Override
    public long getPreviousCursor() {
        return previousCursor;
    }

    public long getSinceId() {
        return metadata.sinceId;
    }

    public String getWarning() {
        return metadata.warning;
    }

    @JsonObject
    public static class SearchMetadata {
        @JsonField(name = "max_id")
        long maxId;
        @JsonField(name = "since_id")
        long sinceId;
        @JsonField(name = "count")
        int count;
        @JsonField(name = "completed_in")
        double completedIn;
        @JsonField(name = "query")
        String query;
        @JsonField(name = "warning")
        String warning;
    }

}