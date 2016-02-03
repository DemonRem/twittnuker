/*
 * Twittnuker - Twitter client for Android
 *
 * Copyright (C) 2013-2016 vanita5 <mail@vanit.as>
 *
 * This program incorporates a modified version of Twidere.
 * Copyright (C) 2012-2016 Mariotaku Lee <mariotaku.lee@gmail.com>
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
import android.text.TextUtils;

import org.apache.commons.lang3.ArrayUtils;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public final class TwidereArrayUtils {

    private TwidereArrayUtils() {
        throw new AssertionError("You are trying to create an instance for this utility class!");
    }

    public static boolean contains(final int[] array, final int value) {
        if (array == null) return false;
        for (final int item : array) {
            if (item == value) return true;
        }
        return false;
    }

    public static boolean contains(final long[] array, final long value) {
        if (array == null) return false;
        for (final long item : array) {
            if (item == value) return true;
        }
        return false;
    }

    public static boolean contains(final Object[] array, final Object value) {
        if (array == null || value == null) return false;
        return contains(array, new Object[]{value});
    }

    public static boolean contains(final Object[] array, final Object[] values) {
        if (array == null || values == null) return false;
        for (final Object value : values) {
            if (!ArrayUtils.contains(array, value)) return false;
        }
        return true;
    }

    public static boolean contentMatch(final long[] array1, final long[] array2) {
        if (array1 == null || array2 == null) return array1 == array2;
        if (array1.length != array2.length) return false;
        for (long anArray1 : array1) {
            if (!ArrayUtils.contains(array2, anArray1)) return false;
        }
        return true;
    }

    public static boolean contentMatch(final Object[] array1, final Object[] array2) {
        if (array1 == null || array2 == null) return array1 == array2;
        if (array1.length != array2.length) return false;
        for (Object item : array1) {
            if (!ArrayUtils.contains(array2, item)) return false;
        }
        return true;
    }

    public static long[] fromList(final List<Long> list) {
        if (list == null) return null;
        final int count = list.size();
        final long[] array = new long[count];
        for (int i = 0; i < count; i++) {
            array[i] = list.get(i);
        }
        return array;
    }

    public static int indexOf(final Object[] array, final Object value) {
        final int length = array.length;
        for (int i = 0; i < length; i++) {
            if (array[i].equals(value)) return i;
        }
        return -1;
    }

    public static long[] intersection(final long[] array1, final long[] array2) {
        if (array1 == null || array2 == null) return new long[0];
        final List<Long> list1 = new ArrayList<>();
        for (final long item : array1) {
            list1.add(item);
        }
        final List<Long> list2 = new ArrayList<>();
        for (final long item : array2) {
            list2.add(item);
        }
        list1.retainAll(list2);
        return fromList(list1);
    }

    @SuppressWarnings("SuspiciousSystemArraycopy")
    public static void mergeArray(final Object dest, @NonNull final Object... arrays) {
        for (int i = 0, j = arrays.length, k = 0; i < j; i++) {
            final Object array = arrays[i];
            final int length = Array.getLength(array);
            System.arraycopy(array, 0, dest, k, length);
            k += length;
        }
    }

    public static long min(final long[] array) {
        if (array == null || array.length == 0) throw new IllegalArgumentException();
        long min = array[0];
        for (int i = 1, j = array.length; i < j; i++) {
            if (min > array[i]) {
                min = array[i];
            }
        }
        return min;
    }

    @NonNull
    public static long[] parseLongArray(final String string, final char token) {
        if (TextUtils.isEmpty(string)) return new long[0];
        final String[] itemsStringArray = string.split(String.valueOf(token));
        final long[] array = new long[itemsStringArray.length];
        for (int i = 0, j = itemsStringArray.length; i < j; i++) {
            try {
                array[i] = Long.parseLong(itemsStringArray[i]);
            } catch (final NumberFormatException e) {
                return new long[0];
            }
        }
        return array;
    }

    public static String toString(final long[] array, final char token, final boolean include_space) {
        final StringBuilder builder = new StringBuilder();
        final int length = array.length;
        for (int i = 0; i < length; i++) {
            final String idString = String.valueOf(array[i]);
            if (i > 0) {
                builder.append(include_space ? token + " " : token);
            }
            builder.append(idString);
        }
        return builder.toString();
    }

    public static String toString(final Object[] array, final char token, final boolean include_space) {
        final StringBuilder builder = new StringBuilder();
        final int length = array.length;
        for (int i = 0; i < length; i++) {
            final String id_string = String.valueOf(array[i]);
            if (id_string != null) {
                if (i > 0) {
                    builder.append(include_space ? token + " " : token);
                }
                builder.append(id_string);
            }
        }
        return builder.toString();
    }

    public static String[] toStringArray(final Object[] array) {
        if (array == null) return null;
        final int length = array.length;
        final String[] stringArray = new String[length];
        for (int i = 0; i < length; i++) {
            stringArray[i] = ParseUtils.parseString(array[i]);
        }
        return stringArray;
    }

    public static String[] toStringArray(final long[] array) {
        if (array == null) return null;
        final int length = array.length;
        final String[] stringArray = new String[length];
        for (int i = 0; i < length; i++) {
            stringArray[i] = ParseUtils.parseString(array[i]);
        }
        return stringArray;
    }


    public static String toStringForSQL(final String[] array) {
        final int size = array != null ? array.length : 0;
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < size; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append('?');
        }
        return builder.toString();
    }

    public static void offset(long[] array, long offset) {
        for (int i = 0; i < array.length; i++) {
            array[i] += offset;
        }
    }
}