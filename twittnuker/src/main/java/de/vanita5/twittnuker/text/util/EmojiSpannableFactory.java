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

package de.vanita5.twittnuker.text.util;

import android.text.Spannable;
import android.widget.TextView;

import de.vanita5.twittnuker.util.EmojiSupportUtils;
import de.vanita5.twittnuker.util.ExternalThemeManager;
import de.vanita5.twittnuker.util.dagger.ApplicationModule;
import de.vanita5.twittnuker.util.dagger.DaggerGeneralComponent;

import javax.inject.Inject;

public class EmojiSpannableFactory extends SafeSpannableFactory {

    private final TextView textView;
    @Inject
    ExternalThemeManager externalThemeManager;

    public EmojiSpannableFactory(TextView textView) {
        DaggerGeneralComponent.builder().applicationModule(ApplicationModule.get(textView.getContext())).build().inject(this);
        this.textView = textView;
    }

    @Override
    public Spannable newSpannable(CharSequence source) {
        final Spannable spannable = super.newSpannable(source);
        EmojiSupportUtils.applyEmoji(externalThemeManager, spannable);
        System.identityHashCode(spannable);
        return spannable;
    }
}