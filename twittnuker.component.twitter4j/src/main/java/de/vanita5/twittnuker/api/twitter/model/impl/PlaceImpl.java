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

package de.vanita5.twittnuker.api.twitter.model.impl;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;

import twitter4j.GeoLocation;
import twitter4j.Place;
import twitter4j.RateLimitStatus;

/**
 * Created by mariotaku on 15/5/7.
 */
@JsonObject
public class PlaceImpl implements Place {

	@JsonField(name = "full_name")
	String fullName;

	@Override
	public GeoLocation[][] getBoundingBoxCoordinates() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getBoundingBoxType() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Place[] getContainedWithIn() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getCountry() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getCountryCode() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getFullName() {
		return fullName;
	}

	@Override
	public GeoLocation[][] getGeometryCoordinates() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getGeometryType() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getId() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getName() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getPlaceType() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getStreetAddress() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getUrl() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int compareTo(Place another) {
		return 0;
	}

	@Override
	public int getAccessLevel() {
		return 0;
	}

	@Override
	public RateLimitStatus getRateLimitStatus() {
		return null;
	}
}