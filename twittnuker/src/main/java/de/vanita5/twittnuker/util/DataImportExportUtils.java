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

package de.vanita5.twittnuker.util;

import android.content.Context;
import android.util.Log;

import de.vanita5.twittnuker.Constants;
import de.vanita5.twittnuker.annotation.Preference;
import de.vanita5.twittnuker.constant.SharedPreferenceConstants;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static de.vanita5.twittnuker.annotation.Preference.Type.INVALID;

public class DataImportExportUtils implements Constants {

    public static final String ENTRY_PREFERENCES = "preferences.json";
    public static final String ENTRY_USER_COLORS = "user_colors.json";
    public static final String ENTRY_HOST_MAPPING = "host_mapping.json";
    public static final String ENTRY_KEYBOARD_SHORTCUTS = "keyboard_shortcuts.json";

    public static final int FLAG_PREFERENCES = 0x1;
    public static final int FLAG_NICKNAMES = 0x2;
    public static final int FLAG_USER_COLORS = 0x4;
    public static final int FLAG_HOST_MAPPING = 0x8;
    public static final int FLAG_KEYBOARD_SHORTCUTS = 0x10;
    public static final int FLAG_ALL = FLAG_PREFERENCES | FLAG_NICKNAMES | FLAG_USER_COLORS | FLAG_HOST_MAPPING | FLAG_KEYBOARD_SHORTCUTS;

	public static void exportData(final Context context, final File dst, final int flags) throws IOException {
		if (dst == null) throw new FileNotFoundException();
		dst.delete();
		final FileOutputStream fos = new FileOutputStream(dst);
		final ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fos));
		if (hasFlag(flags, FLAG_PREFERENCES)) {
			writeSharedPreferencesData(zos, context, SHARED_PREFERENCES_NAME, ENTRY_PREFERENCES);
		}
		if (hasFlag(flags, FLAG_USER_COLORS)) {
			writeRawSharedPreferencesData(zos, context, USER_COLOR_PREFERENCES_NAME, ENTRY_USER_COLORS);
		}
		if (hasFlag(flags, FLAG_HOST_MAPPING)) {
			writeRawSharedPreferencesData(zos, context, HOST_MAPPING_PREFERENCES_NAME, ENTRY_HOST_MAPPING);
		}
        if (hasFlag(flags, FLAG_KEYBOARD_SHORTCUTS)) {
            writeRawSharedPreferencesData(zos, context, KEYBOARD_SHORTCUTS_PREFERENCES_NAME, ENTRY_KEYBOARD_SHORTCUTS);
        }
		zos.finish();
		zos.flush();
		Utils.closeSilently(zos);
		Utils.closeSilently(fos);
	}

	public static int getImportedSettingsFlags(final File src) throws IOException {
		if (src == null) return 0;
		final ZipFile zipFile = new ZipFile(src);
		int flags = 0;
		if (zipFile.getEntry(ENTRY_PREFERENCES) != null) {
		    flags |= FLAG_PREFERENCES;
		}
		if (zipFile.getEntry(ENTRY_USER_COLORS) != null) {
		    flags |= FLAG_USER_COLORS;
		}
		if (zipFile.getEntry(ENTRY_HOST_MAPPING) != null) {
			flags |= FLAG_HOST_MAPPING;
		}
        if (zipFile.getEntry(ENTRY_KEYBOARD_SHORTCUTS) != null) {
            flags |= FLAG_KEYBOARD_SHORTCUTS;
        }
        zipFile.close();
		return flags;
	}

	public static HashMap<String, Preference> getSupportedPreferencesMap() {
		final Field[] fields = SharedPreferenceConstants.class.getDeclaredFields();
        final HashMap<String, Preference> supportedPrefsMap = new HashMap<>();
		for (final Field field : fields) {
			final Preference annotation = field.getAnnotation(Preference.class);
			if (Modifier.isStatic(field.getModifiers()) && CompareUtils.classEquals(field.getType(), String.class)
					&& annotation != null && annotation.exportable() && annotation.type() != INVALID) {
				try {
					supportedPrefsMap.put((String) field.get(null), annotation);
                } catch (final IllegalAccessException | IllegalArgumentException e) {
                    Log.w(LOGTAG, e);
				}
			}
		}
		return supportedPrefsMap;
	}

	public static void importData(final Context context, final File src, final int flags) throws IOException {
		if (src == null) throw new FileNotFoundException();
		final ZipFile zipFile = new ZipFile(src);
		if (hasFlag(flags, FLAG_PREFERENCES)) {
			readSharedPreferencesData(zipFile, context, SHARED_PREFERENCES_NAME, ENTRY_PREFERENCES);
		}
		if (hasFlag(flags, FLAG_USER_COLORS)) {
		    readRawSharedPreferencesData(zipFile, context, USER_COLOR_PREFERENCES_NAME, ENTRY_USER_COLORS);
		}
		if (hasFlag(flags, FLAG_HOST_MAPPING)) {
		    readRawSharedPreferencesData(zipFile, context, HOST_MAPPING_PREFERENCES_NAME, ENTRY_HOST_MAPPING);
		}
        if (hasFlag(flags, FLAG_KEYBOARD_SHORTCUTS)) {
            readRawSharedPreferencesData(zipFile, context, KEYBOARD_SHORTCUTS_PREFERENCES_NAME, ENTRY_KEYBOARD_SHORTCUTS);
        }
        zipFile.close();
	}

	private static boolean hasFlag(final int flags, final int flag) {
		return (flags & flag) != 0;
	}

	private static void readRawSharedPreferencesData(final ZipFile zipFile, final Context context,
		final String preferencesName, final String entryName) throws IOException {
//        final ZipEntry entry = zipFile.getEntry(entryName);
//        if (entry == null) return;
//        final JSONObject json = JSONFileIO.convertJSONObject(zipFile.getInputStream(entry));
//        final RawSharedPreferencesData data = JSONSerializer.createObject(RawSharedPreferencesData.JSON_CREATOR, json);
//        if (data != null) {
//            data.writeToSharedPreferences(context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE));
//        }
	}

	private static void readSharedPreferencesData(final ZipFile zipFile, final Context context,
			final String preferencesName, final String entryName) throws IOException {
//        final ZipEntry entry = zipFile.getEntry(entryName);
//        if (entry == null) return;
//        final JSONObject json = JSONFileIO.convertJSONObject(zipFile.getInputStream(entry));
//        final SharedPreferencesData data = JSONSerializer.createObject(SharedPreferencesData.JSON_CREATOR, json);
//        if (data != null) {
//            data.writeToSharedPreferences(context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE));
//        }
	}

	private static void writeRawSharedPreferencesData(final ZipOutputStream zos, final Context context,
													  final String preferencesName, final String entryName) throws IOException {
//        final byte[] data = getSerializedRawSharedPreferencesData(context, preferencesName);
//        if (data == null) return;
//        zos.putNextEntry(new ZipEntry(entryName));
//        zos.write(data);
//        zos.closeEntry();
	}

	private static void writeSharedPreferencesData(final ZipOutputStream zos, final Context context,
												   final String preferencesName, final String entryName) throws IOException {
//        final byte[] data = getSerializedSharedPreferencesData(context, preferencesName);
//        if (data == null) return;
//        zos.putNextEntry(new ZipEntry(entryName));
//        zos.write(data);
//        zos.closeEntry();
	}

}