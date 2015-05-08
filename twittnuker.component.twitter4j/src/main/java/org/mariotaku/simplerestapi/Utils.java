/*
 * Twidere - Twitter client for Android
 *
 *  Copyright (C) 2012-2015 Mariotaku Lee <mariotaku.lee@gmail.com>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mariotaku.simplerestapi;

import org.mariotaku.simplerestapi.http.KeyValuePair;

import java.io.Closeable;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by mariotaku on 15/2/4.
 */
public class Utils {
	public static String[] split(final String str, final String separator) {
		String[] returnValue;
		int index = str.indexOf(separator);
		if (index == -1) {
			returnValue = new String[]{str};
		} else {
			final List<String> strList = new ArrayList<>();
			int oldIndex = 0;
			while (index != -1) {
				final String subStr = str.substring(oldIndex, index);
				strList.add(subStr);
				oldIndex = index + separator.length();
				index = str.indexOf(separator, oldIndex);
			}
			if (oldIndex != str.length()) {
				strList.add(str.substring(oldIndex));
			}
			returnValue = strList.toArray(new String[strList.size()]);
		}

		return returnValue;
	}

	public static void closeSilently(Closeable c) {
		if (c == null) return;
		try {
			c.close();
		} catch (IOException ignore) {
		}
	}

	/**
	 * @param value string to be encoded
	 * @return encoded string
	 * @see <a href="http://wiki.oauth.net/TestCases">OAuth / TestCases</a>
	 * @see <a
	 * href="http://groups.google.com/group/oauth/browse_thread/thread/a8398d0521f4ae3d/9d79b698ab217df2?hl=en&lnk=gst&q=space+encoding#9d79b698ab217df2">Space
	 * encoding - OAuth | Google Groups</a>
	 * @see <a href="http://tools.ietf.org/html/rfc3986#section-2.1">RFC 3986 -
	 * Uniform Resource Identifier (URI): Generic Syntax - 2.1.
	 * Percent-Encoding</a>
	 */
	public static String encode(final String value, String encoding) {
		String encoded;
		try {
			encoded = URLEncoder.encode(value, encoding);
		} catch (final UnsupportedEncodingException ignore) {
			return null;
		}
		final StringBuilder buf = new StringBuilder(encoded.length());
		char focus;
		for (int i = 0; i < encoded.length(); i++) {
			focus = encoded.charAt(i);
			if (focus == '*') {
				buf.append("%2A");
			} else if (focus == '+') {
				buf.append("%20");
			} else if (focus == '%' && i + 1 < encoded.length() && encoded.charAt(i + 1) == '7'
					&& encoded.charAt(i + 2) == 'E') {
				buf.append('~');
				i += 2;
			} else {
				buf.append(focus);
			}
		}
		return buf.toString();
	}

	public static void parseGetParameters(final String queryString, final List<KeyValuePair> params,
										  final String encoding) {
		final String[] queryStrings = split(queryString, "&");
		try {
			for (final String query : queryStrings) {
				final String[] split = split(query, "=");
				final String key = URLDecoder.decode(split[0], encoding);
				if (split.length == 2) {
					params.add(new KeyValuePair(key, URLDecoder.decode(split[1], encoding)));
				} else {
					params.add(new KeyValuePair(key, ""));
				}
			}
		} catch (final UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

}