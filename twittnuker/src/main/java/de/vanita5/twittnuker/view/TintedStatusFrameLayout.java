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

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.WindowInsetsCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.Window;

import org.mariotaku.chameleon.Chameleon;
import org.mariotaku.chameleon.Chameleon.Theme.LightStatusBarMode;
import org.mariotaku.chameleon.ChameleonUtils;
import org.mariotaku.chameleon.ChameleonView;
import org.mariotaku.chameleon.internal.WindowSupport;
import de.vanita5.twittnuker.R;
import de.vanita5.twittnuker.view.iface.TintedStatusLayout;

public class TintedStatusFrameLayout extends ExtendedFrameLayout implements TintedStatusLayout,
        ChameleonView, ChameleonView.StatusBarThemeable {

    private final Paint mColorPaint;
    private boolean mSetPadding;

    private int mStatusBarHeight;
    private Rect mSystemWindowsInsets;
    private WindowInsetsListener mWindowInsetsListener;

    public TintedStatusFrameLayout(Context context) {
        this(context, null);
    }

    public TintedStatusFrameLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TintedStatusFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TintedStatusLayout);
        setSetPaddingEnabled(a.getBoolean(R.styleable.TintedStatusLayout_setPadding, false));
        a.recycle();
        mColorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mSystemWindowsInsets = new Rect();
        setWillNotDraw(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setSystemUiVisibility(SYSTEM_UI_FLAG_LAYOUT_STABLE | SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            ViewCompat.setOnApplyWindowInsetsListener(this, new android.support.v4.view.OnApplyWindowInsetsListener() {
                @Override
                public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                    final int top = insets.getSystemWindowInsetTop();
                    final int left = insets.getSystemWindowInsetLeft();
                    final int right = insets.getSystemWindowInsetRight();
                    final int bottom = insets.getSystemWindowInsetBottom();
                    if (mSetPadding) {
                        setPadding(left, top, right, bottom);
                    }
                    setStatusBarHeight(top);
                    if (mWindowInsetsListener != null) {
                        mWindowInsetsListener.onApplyWindowInsets(left, top, right, bottom);
                    }
                    return insets.consumeSystemWindowInsets();
                }
            });
        }
    }


    @Override
    public void setStatusBarColor(int color) {
        mColorPaint.setColor(0xFF000000 | color);
        mColorPaint.setAlpha(Color.alpha(color));
        invalidate();
    }

    @Override
    public void setSetPaddingEnabled(boolean enabled) {
        mSetPadding = enabled;
    }

    public void setStatusBarHeight(int height) {
        mStatusBarHeight = height;
        invalidate();
    }

    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {
        super.dispatchDraw(canvas);
        canvas.drawRect(0, 0, canvas.getWidth(), mStatusBarHeight, mColorPaint);
    }

    @Override
    protected boolean fitSystemWindows(@NonNull Rect insets) {
        mSystemWindowsInsets.set(insets);
        return true;
    }

    public void setWindowInsetsListener(WindowInsetsListener listener) {
        mWindowInsetsListener = listener;
    }

    @Override
    public boolean isPostApplyTheme() {
        return false;
    }

    @Nullable
    @Override
    public Appearance createAppearance(Context context, AttributeSet attributeSet, Chameleon.Theme theme) {
        Appearance appearance = new Appearance();
        appearance.setStatusBarColor(theme.getStatusBarColor());
        appearance.setLightStatusBarMode(theme.getLightStatusBarMode());
        return appearance;
    }

    @Override
    public void applyAppearance(@NonNull ChameleonView.Appearance appearance) {
        Appearance a = (Appearance) appearance;
        final int statusBarColor = a.getStatusBarColor();
        setStatusBarColor(statusBarColor);
        final Activity activity = ChameleonUtils.getActivity(getContext());
        if (activity != null) {
            final Window window = activity.getWindow();
            WindowSupport.setStatusBarColor(window, Color.TRANSPARENT);
            ChameleonUtils.applyLightStatusBar(window, statusBarColor, a.getLightStatusBarMode());
        }
    }

    @Override
    public boolean isStatusBarColorHandled() {
        return true;
    }

    public static class Appearance implements ChameleonView.Appearance {
        private int statusBarColor;
        @LightStatusBarMode
        private int lightStatusBarMode;

        public int getStatusBarColor() {
            return statusBarColor;
        }

        public void setStatusBarColor(int statusBarColor) {
            this.statusBarColor = statusBarColor;
        }

        @LightStatusBarMode
        public int getLightStatusBarMode() {
            return lightStatusBarMode;
        }

        public void setLightStatusBarMode(@LightStatusBarMode int lightStatusBarMode) {
            this.lightStatusBarMode = lightStatusBarMode;
        }
    }

    public interface WindowInsetsListener {
        void onApplyWindowInsets(int left, int top, int right, int bottom);
    }
}