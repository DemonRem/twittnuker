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

package de.vanita5.twittnuker.model;

import android.support.annotation.Nullable;

import org.mariotaku.commons.objectcursor.LoganSquareCursorFieldConverter;
import org.mariotaku.library.objectcursor.annotation.AfterCursorObjectCreated;
import org.mariotaku.library.objectcursor.annotation.CursorField;
import org.mariotaku.library.objectcursor.annotation.CursorObject;
import de.vanita5.twittnuker.annotation.AccountType;
import de.vanita5.twittnuker.model.util.UserKeyCursorFieldConverter;
import de.vanita5.twittnuker.provider.TwidereDataStore.Accounts;

@CursorObject
@Deprecated
public class ParcelableAccount {


    @CursorField(value = Accounts._ID, excludeWrite = true)
    public long id;


    @CursorField(value = Accounts.ACCOUNT_KEY, converter = UserKeyCursorFieldConverter.class)
    public UserKey account_key;


    @CursorField(Accounts.SCREEN_NAME)
    public String screen_name;


    @CursorField(Accounts.NAME)
    public String name;

    @Nullable
    @AccountType
    @CursorField(Accounts.ACCOUNT_TYPE)
    public String account_type;


    @CursorField(Accounts.PROFILE_IMAGE_URL)
    public String profile_image_url;


    @CursorField(Accounts.PROFILE_BANNER_URL)
    public String profile_banner_url;


    @CursorField(Accounts.COLOR)
    public int color;


    @CursorField(Accounts.IS_ACTIVATED)
    public boolean is_activated;
    @Nullable


    @CursorField(value = Accounts.ACCOUNT_USER, converter = LoganSquareCursorFieldConverter.class)
    public ParcelableUser account_user;

    public boolean is_dummy;

    @AfterCursorObjectCreated
    void afterObjectCreated() {
        if (account_user != null) {
            account_user.is_cache = true;
            account_user.account_color = color;
        }
    }

}