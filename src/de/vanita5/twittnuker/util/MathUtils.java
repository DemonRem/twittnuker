/*
 * Twittnuker - Twitter client for Android
 *
 * Copyright (C) 2013-2014 vanita5 <mail@vanita5.de>
 *
 * This program incorporates a modified version of Twidere.
 * Copyright (C) 2012-2014 Mariotaku Lee <mariotaku.lee@gmail.com>
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

package de.vanita5.twittnuker.util;

public class MathUtils {
    public static float clamp(final float num, final float bound1, final float bound2) {
        final float max = Math.max(bound1, bound2), min = Math.min(bound1, bound2);
		return Math.max(Math.min(num, max), min);
	}

    public static int clamp(final int num, final int bound1, final int bound2) {
        final int max = Math.max(bound1, bound2), min = Math.min(bound1, bound2);
		return Math.max(Math.min(num, max), min);
	}

}
