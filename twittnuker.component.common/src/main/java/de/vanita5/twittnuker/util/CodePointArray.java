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

package de.vanita5.twittnuker.util;

import android.support.annotation.NonNull;

import org.apache.commons.lang3.ArrayUtils;

public final class CodePointArray {

    private final int[] codePoints;
    private final int length;

    public CodePointArray(@NonNull final CharSequence cs) {
        final int inputLength = cs.length();
        codePoints = new int[inputLength];
        int codePointsLength = 0;
        for (int offset = 0; offset < inputLength; ) {
            final int codePoint = Character.codePointAt(cs, offset);
            codePoints[codePointsLength++] = codePoint;
            offset += Character.charCount(codePoint);
        }
        this.length = codePointsLength;
    }

    public int get(int pos) {
        return codePoints[pos];
    }

    public int length() {
        return length;
    }

    public int indexOfText(int codePoint, int start) {
        int index = 0;
        for (int i = 0; i < length; i++) {
            final int current = codePoints[i];
            if (current == codePoint && i >= start) return index;
            index += Character.charCount(current);
        }
        return -1;
    }

    @NonNull
    public String substring(int start, int end) {
        final StringBuilder sb = new StringBuilder();
        for (int i = start; i < end; i++) {
            sb.appendCodePoint(codePoints[i]);
        }
        return sb.toString();
    }

    public int[] subarray(int start, int end) {
        return ArrayUtils.subarray(codePoints, start, end);
    }
}
