/*
 * Twittnuker - Twitter client for Android
 *
 * Copyright (C) 2013-2017 vanita5 <mail@vanit.as>
 *
 * This program incorporates a modified version of Twidere.
 * Copyright (C) 2012-2017 Mariotaku Lee <mariotaku.lee@gmail.com>
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

import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.text.Spannable;
import android.text.Spanned;
import android.text.style.ReplacementSpan;

public class TwidereStringUtils {
    private TwidereStringUtils() {
    }

    public static boolean regionMatchesIgnoreCase(@NonNull final String string, final int thisStart,
                                                  @NonNull final String match, final int start,
                                                  final int length) {
        return string.substring(thisStart, thisStart + length).equalsIgnoreCase(match.substring(start, start + length));
    }


    public static boolean startsWithIgnoreCase(@NonNull String string, @NonNull String prefix) {
        return startsWithIgnoreCase(string, prefix, 0);
    }

    public static boolean startsWithIgnoreCase(@NonNull String string, @NonNull String prefix,
                                               int start) {
        if (prefix.length() > string.length()) return false;
        return regionMatchesIgnoreCase(string, start, prefix, 0, prefix.length());
    }

    /**
     * Fix to https://github.com/TwidereProject/Twidere-Android/issues/449
     */
    public static void fixSHY(Spannable string) {
        for (int i = 0, j = string.length(); i < j; i++) {
            if (string.charAt(i) == '\u00ad') {
                string.setSpan(new ZeroWidthSpan(), i, i + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    private static class ZeroWidthSpan extends ReplacementSpan {

        @Override
        public int getSize(@NonNull Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
            return 0;
        }

        @Override
        public void draw(@NonNull Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, @NonNull Paint paint) {

        }
    }
}