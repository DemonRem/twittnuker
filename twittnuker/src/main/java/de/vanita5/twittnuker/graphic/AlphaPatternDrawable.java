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

package de.vanita5.twittnuker.graphic;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/**
 * This drawable that draws a simple white and gray chessboard pattern. It's
 * pattern you will often see as a background behind a partly transparent image
 * in many applications.
 * 
 * @author Daniel Nilsson
 */
public class AlphaPatternDrawable extends Drawable {

	private final int mAlphaPatternSize;

    private int mNumRectanglesHorizontal;
    private int mNumRectanglesVertical;

	private final Rect mRect = new Rect(), mBounds = new Rect();
	private final Paint mPaint = new Paint();

    public AlphaPatternDrawable(final int alphaPatternSize) {
        mAlphaPatternSize = alphaPatternSize;
	}

	@Override
	public void draw(final Canvas canvas) {

		boolean verticalStartWhite = true;
        for (int i = 0; i <= mNumRectanglesVertical; i++) {
			boolean horizontalStartWhite = verticalStartWhite;
            for (int j = 0; j <= mNumRectanglesHorizontal; j++) {
				mRect.setEmpty();
                mRect.top = i * mAlphaPatternSize + mBounds.top;
                mRect.left = j * mAlphaPatternSize + mBounds.left;
                mRect.bottom = Math.min(mRect.top + mAlphaPatternSize, mBounds.bottom);
                mRect.right = Math.min(mRect.left + mAlphaPatternSize, mBounds.right);

                mPaint.setColor(horizontalStartWhite ? Color.WHITE : Color.LTGRAY);
				canvas.drawRect(mRect, mPaint);

				horizontalStartWhite = !horizontalStartWhite;
			}
			verticalStartWhite = !verticalStartWhite;
		}
	}

	@Override
	public int getOpacity() {
		return 0;
	}

	@Override
	public void setAlpha(final int alpha) {

	}

	@Override
	public void setColorFilter(final ColorFilter cf) {

	}

	@Override
	protected void onBoundsChange(final Rect bounds) {
		super.onBoundsChange(bounds);
		mBounds.set(bounds);
		final int height = bounds.height();
		final int width = bounds.width();
        mNumRectanglesHorizontal = (int) Math.ceil(width / mAlphaPatternSize);
        mNumRectanglesVertical = (int) Math.ceil(height / mAlphaPatternSize);
		invalidateSelf();
	}

}
