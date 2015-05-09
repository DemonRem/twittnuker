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

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;

import de.vanita5.twittnuker.api.twitter.TwitterConverter;
import twitter4j.PageableResponseList;
import twitter4j.Status;
import twitter4j.User;

/**
 * Created by mariotaku on 15/5/7.
 */
@JsonObject
public class PageableResponseListWrapper extends TwitterResponseImpl implements Wrapper<PageableResponseList<?>> {

	@JsonField(name = "previous_cursor")
	long previousCursor;
	@JsonField(name = "next_cursor")
	long nextCursor;

	@JsonField(name = "users")
	ArrayList<User> users;

	@JsonField(name = "statuses")
	ArrayList<Status> statuses;

	@Override
	public PageableResponseList<?> getWrapped(Object extra) {
		final Type[] typeArgs = (Type[]) extra;
		final Class<?> elementType = (Class<?>) typeArgs[0];
		if (User.class.isAssignableFrom(elementType)) {
			return new PageableResponseListImpl<>(users);
		} else if (Status.class.isAssignableFrom(elementType)) {
			return new PageableResponseListImpl<>(statuses);
		}
		throw new TwitterConverter.UnsupportedTypeException(elementType);
	}
}