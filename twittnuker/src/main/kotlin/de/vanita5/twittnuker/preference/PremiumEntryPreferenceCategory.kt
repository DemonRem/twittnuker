/*
 *  Twittnuker - Twitter client for Android
 *
 *  Copyright (C) 2013-2017 vanita5 <mail@vanit.as>
 *
 *  This program incorporates a modified version of Twidere.
 *  Copyright (C) 2012-2017 Mariotaku Lee <mariotaku.lee@gmail.com>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.vanita5.twittnuker.preference

import android.content.Context
import android.util.AttributeSet
import de.vanita5.twittnuker.R
import de.vanita5.twittnuker.extension.findParent
import de.vanita5.twittnuker.util.dagger.GeneralComponentHelper
import de.vanita5.twittnuker.util.premium.ExtraFeaturesService
import javax.inject.Inject


class PremiumEntryPreferenceCategory(context: Context, attrs: AttributeSet) : TintedPreferenceCategory(context, attrs) {

    @Inject
    internal lateinit var extraFeaturesService: ExtraFeaturesService

    init {
        GeneralComponentHelper.build(context).inject(this)
        val a = context.obtainStyledAttributes(attrs, R.styleable.PremiumEntryPreference)
        a.recycle()
        isEnabled = extraFeaturesService.isSupported()
    }

    override fun onAttached() {
        super.onAttached()
        if (!extraFeaturesService.isSupported()) {
            preferenceManager.preferenceScreen?.let { screen ->
                findParent(screen)?.removePreference(this)
            }
        }
    }

}