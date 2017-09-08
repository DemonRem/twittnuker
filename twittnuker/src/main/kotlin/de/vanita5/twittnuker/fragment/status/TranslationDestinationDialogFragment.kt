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
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.vanita5.twittnuker.fragment.status

import android.app.Dialog
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.support.v7.app.AlertDialog
import org.mariotaku.kpreferences.get
import org.mariotaku.kpreferences.set
import org.mariotaku.ktextension.Bundle
import org.mariotaku.ktextension.getTypedArray
import org.mariotaku.ktextension.mapToArray
import org.mariotaku.ktextension.set
import de.vanita5.twittnuker.constant.translationDestinationKey
import de.vanita5.twittnuker.extension.applyTheme
import de.vanita5.twittnuker.extension.onShow
import de.vanita5.twittnuker.fragment.BaseDialogFragment
import java.text.Collator
import java.util.*

class TranslationDestinationDialogFragment : BaseDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(context)
        val languages = arguments.getTypedArray<DisplayLanguage>(EXTRA_LANGUAGES).sortedArrayWith(LanguageComparator())
        val selectedLanguage = preferences[translationDestinationKey] ?: arguments.getString(EXTRA_SELECTED_LANGUAGE)
        val selectedIndex = languages.indexOfFirst { selectedLanguage == it.code }
        builder.setSingleChoiceItems(languages.mapToArray { it.name }, selectedIndex) { _, which ->
            preferences[translationDestinationKey] = languages[which].code
        }
        builder.setPositiveButton(android.R.string.ok) { _, _ ->

        }
        builder.setNegativeButton(android.R.string.cancel, null)
        val dialog = builder.create()
        dialog.onShow {
            it.applyTheme()
            it.listView?.isFastScrollEnabled = true
        }
        return dialog
    }

    data class DisplayLanguage(val name: String, val code: String) : Parcelable {

        constructor(parcel: Parcel) : this(
                parcel.readString(),
                parcel.readString())

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(name)
            parcel.writeString(code)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<DisplayLanguage> {

            override fun createFromParcel(parcel: Parcel): DisplayLanguage {
                return DisplayLanguage(parcel)
            }

            override fun newArray(size: Int): Array<DisplayLanguage?> {
                return arrayOfNulls(size)
            }

        }
    }


    private class LanguageComparator : Comparator<DisplayLanguage> {

        private val collator = Collator.getInstance(Locale.getDefault())

        override fun compare(object1: DisplayLanguage, object2: DisplayLanguage): Int {
            return collator.compare(object1.name, object2.name)
        }

    }

    companion object {
        const val EXTRA_SELECTED_LANGUAGE = "selected_language"
        const val EXTRA_LANGUAGES = "languages"

        fun create(languages: List<DisplayLanguage>, selectedLanguage: String?): TranslationDestinationDialogFragment {
            val df = TranslationDestinationDialogFragment()
            df.arguments = Bundle {
                this[EXTRA_LANGUAGES] = languages.toTypedArray()
                this[EXTRA_SELECTED_LANGUAGE] = selectedLanguage
            }
            return df
        }
    }
}