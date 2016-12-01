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

package de.vanita5.twittnuker.model.util;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

import com.bluelinelabs.logansquare.LoganSquare;

import org.mariotaku.library.objectcursor.converter.CursorFieldConverter;
import de.vanita5.twittnuker.model.Draft;
import de.vanita5.twittnuker.model.draft.ActionExtra;
import de.vanita5.twittnuker.model.draft.SendDirectMessageActionExtra;
import de.vanita5.twittnuker.model.draft.UpdateStatusActionExtra;
import de.vanita5.twittnuker.provider.TwidereDataStore.Drafts;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;

public class DraftExtrasConverter implements CursorFieldConverter<ActionExtra> {
    @Override
    public ActionExtra parseField(Cursor cursor, int columnIndex, ParameterizedType fieldType) throws IOException {
        final String actionType = cursor.getString(cursor.getColumnIndex(Drafts.ACTION_TYPE));
        if (TextUtils.isEmpty(actionType)) return null;
        switch (actionType) {
            case "0":
            case "1":
            case Draft.Action.UPDATE_STATUS:
            case Draft.Action.REPLY:
            case Draft.Action.QUOTE: {
                final String string = cursor.getString(columnIndex);
                if (TextUtils.isEmpty(string)) return null;
                return LoganSquare.parse(string, UpdateStatusActionExtra.class);
            }
            case "2":
            case Draft.Action.SEND_DIRECT_MESSAGE: {
                final String string = cursor.getString(columnIndex);
                if (TextUtils.isEmpty(string)) return null;
                return LoganSquare.parse(string, SendDirectMessageActionExtra.class);
            }
        }
        return null;
    }

    @Override
    public void writeField(ContentValues values, ActionExtra object, String columnName, ParameterizedType fieldType) throws IOException {
        if (object == null) return;
        values.put(columnName, LoganSquare.serialize(object));
    }
}