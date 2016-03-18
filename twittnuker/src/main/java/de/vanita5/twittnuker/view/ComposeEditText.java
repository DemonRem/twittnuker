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

package de.vanita5.twittnuker.view;

import android.content.Context;
import android.content.res.ColorStateList;
import android.support.annotation.NonNull;
import android.text.InputType;
import android.text.Selection;
import android.text.method.ArrowKeyMovementMethod;
import android.text.method.MovementMethod;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;

import com.afollestad.appthemeengine.inflation.ATEMultiAutoCompleteTextView;
import com.afollestad.appthemeengine.inflation.ATEMultiAutoCompleteTextView2;

import de.vanita5.twittnuker.R;
import de.vanita5.twittnuker.adapter.ComposeAutoCompleteAdapter;
import de.vanita5.twittnuker.model.UserKey;
import de.vanita5.twittnuker.util.EmojiSupportUtils;
import de.vanita5.twittnuker.util.widget.StatusTextTokenizer;
import de.vanita5.twittnuker.view.iface.IThemeBackgroundTintView;

public class ComposeEditText extends ATEMultiAutoCompleteTextView2 implements IThemeBackgroundTintView {

    private ComposeAutoCompleteAdapter mAdapter;
    private UserKey mAccountKey;

    public ComposeEditText(final Context context) {
        this(context, null);
    }

    public ComposeEditText(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        EmojiSupportUtils.initForTextView(this);
        setTokenizer(new StatusTextTokenizer());
        setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                removeIMESuggestions();
            }
        });
        // HACK: remove AUTO_COMPLETE flag to force IME show auto completion
        setRawInputType(getInputType() & ~InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE);
    }

    @Override
    public void setBackgroundTintColor(@NonNull ColorStateList color) {
        setSupportBackgroundTintList(color);
    }

    public void setAccountKey(UserKey accountKey) {
        mAccountKey = accountKey;
        updateAccountId();
    }

    @Override
    protected MovementMethod getDefaultMovementMethod() {
        return ArrowKeyMovementMethod.getInstance();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!isInEditMode() && mAdapter == null) {
            mAdapter = new ComposeAutoCompleteAdapter(getContext());
        }
        setAdapter(mAdapter);
        updateAccountId();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mAdapter != null) {
            mAdapter.closeCursor();
            mAdapter = null;
        }
    }

    private void updateAccountId() {
        if (mAdapter == null) return;
        mAdapter.setAccountKey(mAccountKey);
    }

    private void removeIMESuggestions() {
        final int selectionEnd = getSelectionEnd(), selectionStart = getSelectionStart();
        Selection.removeSelection(getText());
        setSelection(selectionStart, selectionEnd);
    }
}