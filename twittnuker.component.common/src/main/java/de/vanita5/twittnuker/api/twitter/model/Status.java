/*
 * Twittnuker - Twitter client for Android
 *
 * Copyright (C) 2013-2015 vanita5 <mail@vanit.as>
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

package de.vanita5.twittnuker.api.twitter.model;

import android.support.annotation.NonNull;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;
import com.bluelinelabs.logansquare.annotation.OnJsonParseComplete;

import de.vanita5.twittnuker.api.twitter.util.TwitterDateConverter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;

/**
 * Created by mariotaku on 15/5/5.
 */
@JsonObject
public class Status extends TwitterResponseObject implements Comparable<Status>, TwitterResponse,
        ExtendedEntitySupport {

    @JsonField(name = "created_at", typeConverter = TwitterDateConverter.class)
    Date createdAt;

    @JsonField(name = "id")
    long id;

    @JsonField(name = "text")
    String text;

    @JsonField(name = "source")
    String source;

    @JsonField(name = "truncated")
    boolean truncated;

    @JsonField(name = "entities")
    Entities entities;

    @JsonField(name = "extended_entities")
    Entities extendedEntities;

    @JsonField(name = "in_reply_to_status_id")
    long inReplyToStatusId;

    @JsonField(name = "in_reply_to_user_id")
    long inReplyToUserId;

    @JsonField(name = "in_reply_to_screen_name")
    String inReplyToScreenName;

    @JsonField(name = "user")
    User user;

    @JsonField(name = "geo")
    GeoPoint geo;

    @JsonField(name = "place")
    Place place;

    @JsonField(name = "current_user_retweet")
    CurrentUserRetweet currentUserRetweet;

    @JsonField(name = "contributors")
    long[] contributors;

    @JsonField(name = "retweet_count")
    long retweetCount;

    @JsonField(name = "favorite_count")
    long favoriteCount;

    @JsonField(name = "reply_count")
    long replyCount;

    @JsonField(name = "favorited")
    boolean favorited;
    @JsonField(name = "retweeted")
    boolean retweeted;
    @JsonField(name = "lang")
    String lang;

    @JsonField(name = "descendent_reply_count")
    long descendentReplyCount;

    @JsonField(name = "retweeted_status")
    Status retweetedStatus;

    @JsonField(name = "quoted_status")
    Status quotedStatus;

    @JsonField(name = "card")
    CardEntity card;

    @JsonField(name = "possibly_sensitive")
    boolean possiblySensitive;
    private Status mThat;

    public static void setQuotedStatus(Status status, Status quoted) {
        if (!(status instanceof Status)) return;
        ((Status) status).quotedStatus = quoted;
    }


    public User getUser() {
        return user;
    }


    public String getInReplyToScreenName() {

        return inReplyToScreenName;
    }


    public long getInReplyToUserId() {

        return inReplyToUserId;
    }


    public long getInReplyToStatusId() {

        return inReplyToStatusId;
    }


    public boolean isTruncated() {

        return truncated;
    }


    public String getText() {

        return text;
    }


    public String getSource() {

        return source;
    }


    public Date getCreatedAt() {
        return createdAt;
    }


    public long getId() {
        return id;
    }


    public long getRetweetCount() {
        return retweetCount;
    }


    public long getReplyCount() {
        return replyCount;
    }


    public boolean isFavorited() {
        return favorited;
    }


    public boolean isRetweet() {
        return retweetedStatus != null;
    }


    public boolean isQuote() {
        return quotedStatus != null;
    }


    public boolean isRetweetedByMe() {
        return currentUserRetweet != null;
    }


    public long getFavoriteCount() {
        return favoriteCount;
    }


    public GeoLocation getGeoLocation() {
        if (geo == null) return null;
        return geo.getGeoLocation();
    }


    public long getCurrentUserRetweet() {
        if (currentUserRetweet == null) return -1;
        return currentUserRetweet.id;
    }


    public Status getQuotedStatus() {
        return quotedStatus;
    }


    public Status getRetweetedStatus() {
        return retweetedStatus;
    }


    public long getDescendentReplyCount() {
        return descendentReplyCount;
    }


    public Place getPlace() {
        return place;
    }


    public CardEntity getCard() {
        return card;
    }


    public boolean isPossiblySensitive() {
        return possiblySensitive;
    }


    public MediaEntity[] getExtendedMediaEntities() {
        if (extendedEntities == null) return null;
        return extendedEntities.getMedia();
    }


    public HashtagEntity[] getHashtagEntities() {
        if (entities == null) return null;
        return entities.getHashtags();
    }


    public MediaEntity[] getMediaEntities() {
        if (entities == null) return null;
        return entities.getMedia();
    }


    public UrlEntity[] getUrlEntities() {
        if (entities == null) return null;
        return entities.getUrls();
    }


    public UserMentionEntity[] getUserMentionEntities() {
        if (entities == null) return null;
        return entities.getUserMentions();
    }


    public long[] getContributors() {
        return contributors;
    }


    public int compareTo(@NonNull final Status that) {
        mThat = that;
        final long delta = id - that.getId();
        if (delta < Integer.MIN_VALUE)
            return Integer.MIN_VALUE;
        else if (delta > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        return (int) delta;
    }


    @Override
    public String toString() {
        return "Status{" +
                "createdAt=" + createdAt +
                ", id=" + id +
                ", text='" + text + '\'' +
                ", source='" + source + '\'' +
                ", truncated=" + truncated +
                ", entities=" + entities +
                ", extendedEntities=" + extendedEntities +
                ", inReplyToStatusId=" + inReplyToStatusId +
                ", inReplyToUserId=" + inReplyToUserId +
                ", inReplyToScreenName='" + inReplyToScreenName + '\'' +
                ", user=" + user +
                ", geo=" + geo +
                ", place=" + place +
                ", currentUserRetweet=" + currentUserRetweet +
                ", contributors=" + Arrays.toString(contributors) +
                ", retweetCount=" + retweetCount +
                ", favoriteCount=" + favoriteCount +
                ", replyCount=" + replyCount +
                ", favorited=" + favorited +
                ", retweeted=" + retweeted +
                ", lang='" + lang + '\'' +
                ", descendentReplyCount=" + descendentReplyCount +
                ", retweetedStatus=" + retweetedStatus +
                ", quotedStatus=" + quotedStatus +
                ", card=" + card +
                ", possiblySensitive=" + possiblySensitive +
                '}';
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Status status = (Status) o;

        if (id != status.id) return false;
        if (truncated != status.truncated) return false;
        if (inReplyToStatusId != status.inReplyToStatusId) return false;
        if (inReplyToUserId != status.inReplyToUserId) return false;
        if (retweetCount != status.retweetCount) return false;
        if (favoriteCount != status.favoriteCount) return false;
        if (replyCount != status.replyCount) return false;
        if (favorited != status.favorited) return false;
        if (retweeted != status.retweeted) return false;
        if (descendentReplyCount != status.descendentReplyCount) return false;
        if (possiblySensitive != status.possiblySensitive) return false;
        if (createdAt != null ? !createdAt.equals(status.createdAt) : status.createdAt != null)
            return false;
        if (text != null ? !text.equals(status.text) : status.text != null) return false;
        if (source != null ? !source.equals(status.source) : status.source != null) return false;
        if (entities != null ? !entities.equals(status.entities) : status.entities != null)
            return false;
        if (extendedEntities != null ? !extendedEntities.equals(status.extendedEntities) : status.extendedEntities != null)
            return false;
        if (inReplyToScreenName != null ? !inReplyToScreenName.equals(status.inReplyToScreenName) : status.inReplyToScreenName != null)
            return false;
        if (user != null ? !user.equals(status.user) : status.user != null) return false;
        if (geo != null ? !geo.equals(status.geo) : status.geo != null) return false;
        if (place != null ? !place.equals(status.place) : status.place != null) return false;
        if (currentUserRetweet != null ? !currentUserRetweet.equals(status.currentUserRetweet) : status.currentUserRetweet != null)
            return false;
        if (!Arrays.equals(contributors, status.contributors)) return false;
        if (lang != null ? !lang.equals(status.lang) : status.lang != null) return false;
        if (retweetedStatus != null ? !retweetedStatus.equals(status.retweetedStatus) : status.retweetedStatus != null)
            return false;
        if (quotedStatus != null ? !quotedStatus.equals(status.quotedStatus) : status.quotedStatus != null)
            return false;
        return !(card != null ? !card.equals(status.card) : status.card != null);

    }


    @Override
    public int hashCode() {
        int result = createdAt != null ? createdAt.hashCode() : 0;
        result = 31 * result + (int) (id ^ (id >>> 32));
        result = 31 * result + (text != null ? text.hashCode() : 0);
        result = 31 * result + (source != null ? source.hashCode() : 0);
        result = 31 * result + (truncated ? 1 : 0);
        result = 31 * result + (entities != null ? entities.hashCode() : 0);
        result = 31 * result + (extendedEntities != null ? extendedEntities.hashCode() : 0);
        result = 31 * result + (int) (inReplyToStatusId ^ (inReplyToStatusId >>> 32));
        result = 31 * result + (int) (inReplyToUserId ^ (inReplyToUserId >>> 32));
        result = 31 * result + (inReplyToScreenName != null ? inReplyToScreenName.hashCode() : 0);
        result = 31 * result + (user != null ? user.hashCode() : 0);
        result = 31 * result + (geo != null ? geo.hashCode() : 0);
        result = 31 * result + (place != null ? place.hashCode() : 0);
        result = 31 * result + (currentUserRetweet != null ? currentUserRetweet.hashCode() : 0);
        result = 31 * result + (contributors != null ? Arrays.hashCode(contributors) : 0);
        result = 31 * result + (int) (retweetCount ^ (retweetCount >>> 32));
        result = 31 * result + (int) (favoriteCount ^ (favoriteCount >>> 32));
        result = 31 * result + (int) (replyCount ^ (replyCount >>> 32));
        result = 31 * result + (favorited ? 1 : 0);
        result = 31 * result + (retweeted ? 1 : 0);
        result = 31 * result + (lang != null ? lang.hashCode() : 0);
        result = 31 * result + (int) (descendentReplyCount ^ (descendentReplyCount >>> 32));
        result = 31 * result + (retweetedStatus != null ? retweetedStatus.hashCode() : 0);
        result = 31 * result + (quotedStatus != null ? quotedStatus.hashCode() : 0);
        result = 31 * result + (card != null ? card.hashCode() : 0);
        result = 31 * result + (possiblySensitive ? 1 : 0);
        return result;
    }

    @OnJsonParseComplete
    void onJsonParseComplete() throws IOException {
        if (id <= 0 || text == null) throw new IOException("Malformed Status object");
    }

    @JsonObject
    public static class CurrentUserRetweet {
        @JsonField(name = "id")
        long id;

    }
}