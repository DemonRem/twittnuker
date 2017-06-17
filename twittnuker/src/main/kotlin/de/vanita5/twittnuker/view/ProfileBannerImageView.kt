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
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import de.vanita5.twittnuker.R
import de.vanita5.twittnuker.view.iface.IExtendedView

class ProfileBannerImageView(context: Context, attrs: AttributeSet) :
        ForegroundImageView(context, attrs), IExtendedView {

    override var onSizeChangedListener: IExtendedView.OnSizeChangedListener? = null
    override var touchInterceptor: IExtendedView.TouchInterceptor? = null
    override var onFitSystemWindowsListener: IExtendedView.OnFitSystemWindowsListener? = null

    var bannerAspectRatio: Float = 0.toFloat()

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.ProfileBannerImageView)
        bannerAspectRatio = a.getFraction(R.styleable.ProfileBannerImageView_bannerAspectRatio, 1, 1, 2f)
        a.recycle()
        scaleType = ImageView.ScaleType.CENTER_CROP
    }

    @Deprecated("")
    override fun fitSystemWindows(insets: Rect): Boolean {
        if (onFitSystemWindowsListener != null) {
            onFitSystemWindowsListener!!.onFitSystemWindows(insets)
        }
        return super.fitSystemWindows(insets)
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (touchInterceptor != null) {
            val ret = touchInterceptor!!.dispatchTouchEvent(this, event)
            if (ret) return true
        }
        return super.dispatchTouchEvent(event)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (touchInterceptor != null) {
            val ret = touchInterceptor!!.onTouchEvent(this, event)
            if (ret) return true
        }
        return super.onTouchEvent(event)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = View.MeasureSpec.getSize(widthMeasureSpec)
        val height = Math.round(width / bannerAspectRatio)
        setMeasuredDimension(width, height)
        super.onMeasure(widthMeasureSpec, View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (onSizeChangedListener != null) {
            onSizeChangedListener!!.onSizeChanged(this, w, h, oldw, oldh)
        }
    }
}