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

package de.vanita5.twittnuker.view

import android.content.Context
import android.text.InputType
import android.text.Selection
import android.text.method.ArrowKeyMovementMethod
import android.text.method.MovementMethod
import android.util.AttributeSet
import android.widget.AdapterView
import com.bumptech.glide.Glide
import org.mariotaku.chameleon.view.ChameleonMultiAutoCompleteTextView
import de.vanita5.twittnuker.adapter.ComposeAutoCompleteAdapter
import de.vanita5.twittnuker.model.UserKey
import de.vanita5.twittnuker.util.EmojiSupportUtils
import de.vanita5.twittnuker.util.widget.StatusTextTokenizer

class ComposeEditText(
        context: Context,
        attrs: AttributeSet? = null
) : ChameleonMultiAutoCompleteTextView(context, attrs) {

    private var adapter: ComposeAutoCompleteAdapter? = null
    var accountKey: UserKey? = null
        set(value) {
            field = value
            updateAccountKey()
        }

    init {
        EmojiSupportUtils.initForTextView(this)
        setTokenizer(StatusTextTokenizer())
        onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            removeIMESuggestions()
        }
        // HACK: remove AUTO_COMPLETE flag to force IME show auto completion
        setRawInputType(inputType and InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE.inv())
    }

    override fun getDefaultMovementMethod(): MovementMethod {
        return ArrowKeyMovementMethod.getInstance()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!isInEditMode && adapter == null) {
            adapter = ComposeAutoCompleteAdapter(context, Glide.with(context))
        }
        setAdapter(adapter)
        updateAccountKey()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        adapter?.closeCursor()
        adapter = null
    }

    override fun onTextContextMenuItem(id: Int): Boolean {
        try {
            return super.onTextContextMenuItem(id)
        } catch (e: AbstractMethodError) {
            // http://crashes.to/s/69acd0ea0de
            return true
        }
    }

    private fun updateAccountKey() {
        adapter?.accountKey = accountKey
    }

    private fun removeIMESuggestions() {
        val selectionEnd = selectionEnd
        val selectionStart = selectionStart
        Selection.removeSelection(text)
        setSelection(selectionStart, selectionEnd)
    }
}