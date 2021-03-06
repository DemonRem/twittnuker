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

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.WindowInsets
import android.widget.FrameLayout
import org.mariotaku.ktextension.systemWindowInsets
import de.vanita5.twittnuker.view.iface.IExtendedView

open class ExtendedFrameLayout(context: Context, attrs: AttributeSet? = null) :
        FrameLayout(context, attrs), IExtendedView {

    override var touchInterceptor: IExtendedView.TouchInterceptor? = null
    override var onSizeChangedListener: IExtendedView.OnSizeChangedListener? = null
    override var onApplySystemWindowInsetsListener: IExtendedView.OnApplySystemWindowInsetsListener? = null

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (touchInterceptor != null) {
            val ret = touchInterceptor!!.dispatchTouchEvent(this, event)
            if (ret) return true
        }
        return super.dispatchTouchEvent(event)
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        if (touchInterceptor != null) {
            val ret = touchInterceptor!!.onInterceptTouchEvent(this, event)
            if (ret) return true
        }
        return super.onInterceptTouchEvent(event)
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
        onApplySystemWindowInsetsListener?.onApplySystemWindowInsets(insets.systemWindowInsets)
        return super.onApplyWindowInsets(insets)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (touchInterceptor != null) {
            val ret = touchInterceptor!!.onTouchEvent(this, event)
            if (ret) return true
        }
        return super.onTouchEvent(event)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (onSizeChangedListener != null) {
            onSizeChangedListener!!.onSizeChanged(this, w, h, oldw, oldh)
        }
    }

}