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

package de.vanita5.twittnuker.preference;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.SearchRecentSuggestions;
import android.util.AttributeSet;

import de.vanita5.twittnuker.R;
import de.vanita5.twittnuker.provider.RecentSearchProvider;
import de.vanita5.twittnuker.provider.TwidereDataStore.SearchHistory;

public class ClearSearchHistoryPreference extends AsyncTaskPreference {

    public ClearSearchHistoryPreference(final Context context) {
        this(context, null);
    }

    public ClearSearchHistoryPreference(final Context context, final AttributeSet attrs) {
        this(context, attrs, R.attr.preferenceStyle);
    }

    public ClearSearchHistoryPreference(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void doInBackground() {
        final Context context = getContext();
        if (context == null) return;
        final SearchRecentSuggestions suggestions = new SearchRecentSuggestions(context,
                RecentSearchProvider.AUTHORITY, RecentSearchProvider.MODE);
        suggestions.clearHistory();
        final ContentResolver cr = context.getContentResolver();
        cr.delete(SearchHistory.CONTENT_URI, null, null);
    }

}