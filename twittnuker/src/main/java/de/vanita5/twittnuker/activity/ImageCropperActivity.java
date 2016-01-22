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

package de.vanita5.twittnuker.activity;

import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.view.MarginLayoutParamsCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.WindowCompat;
import android.view.View;
import android.view.WindowManager;

import com.soundcloud.android.crop.CropImageActivity;

import de.vanita5.twittnuker.R;
import de.vanita5.twittnuker.activity.iface.IThemedActivity;
import de.vanita5.twittnuker.util.ThemeUtils;
import de.vanita5.twittnuker.util.Utils;
import de.vanita5.twittnuker.util.support.ViewSupport;
import de.vanita5.twittnuker.view.ShapedImageView;
import de.vanita5.twittnuker.view.TintedStatusFrameLayout;

public class ImageCropperActivity extends CropImageActivity implements IThemedActivity {

    // Data fields
    private int mCurrentThemeResource, mCurrentThemeColor, mCurrentActionBarColor, mCurrentThemeBackgroundAlpha;
    @ShapedImageView.ShapeStyle
    private int mProfileImageStyle;
    private String mCurrentThemeBackgroundOption;
    private String mCurrentThemeFontFamily;


    private TintedStatusFrameLayout mMainContent;
    private View mDoneCancelBar;

    @Override
    public void onContentChanged() {
        super.onContentChanged();
        mMainContent = (TintedStatusFrameLayout) findViewById(R.id.main_content);
        mDoneCancelBar = findViewById(R.id.done_cancel_bar);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
        final int themeColor = getThemeColor();
        final int themeResId = getCurrentThemeResourceId();
        final String backgroundOption = getCurrentThemeBackgroundOption();
        mMainContent.setDrawColor(true);
        mMainContent.setDrawShadow(false);
        mMainContent.setColor(themeColor);
        ViewSupport.setBackground(mDoneCancelBar, ThemeUtils.getActionBarBackground(this, themeResId,
                themeColor, backgroundOption, true));
        ViewCompat.setElevation(mDoneCancelBar, ThemeUtils.getSupportActionBarElevation(this));
        final View windowOverlay = findViewById(R.id.window_overlay);
        ViewSupport.setBackground(windowOverlay, ThemeUtils.getNormalWindowContentOverlay(this, themeResId));
    }

    @Override
    public int getThemeColor() {
        return ThemeUtils.getUserAccentColor(this);
    }

    @Override
    public int getActionBarColor() {
        return ThemeUtils.getActionBarColor(this);
    }

    @Override
    public void setContentView(final int layoutResID) {
        super.setContentView(R.layout.activity_image_cropper);
    }


    @Override
    public void setTheme(final int resid) {
        super.setTheme(mCurrentThemeResource = getThemeResourceId());
        ThemeUtils.applyWindowBackground(this, getWindow(), mCurrentThemeResource,
                mCurrentThemeBackgroundOption, mCurrentThemeBackgroundAlpha);
    }

    @Override
    public int getThemeResourceId() {
        return ThemeUtils.getNoActionBarThemeResource(this);
    }

    @Override
    public String getCurrentThemeFontFamily() {
        return mCurrentThemeFontFamily;
    }

    @Override
    public int getCurrentThemeBackgroundAlpha() {
        return mCurrentThemeBackgroundAlpha;
    }

    @Override
    public String getCurrentThemeBackgroundOption() {
        return mCurrentThemeBackgroundOption;
    }

    @Override
    public int getCurrentThemeColor() {
        return mCurrentThemeColor;
    }

    @Override
    public int getCurrentActionBarColor() {
        return mCurrentActionBarColor;
    }

    @Override
    public final int getCurrentThemeResourceId() {
        return mCurrentThemeResource;
    }

    @Override
    public int getThemeBackgroundAlpha() {
        return ThemeUtils.getUserThemeBackgroundAlpha(this);
    }

    @Override
    public String getThemeBackgroundOption() {
        return ThemeUtils.getThemeBackgroundOption(this);
    }

    @Override
    public String getThemeFontFamily() {
        return ThemeUtils.getThemeFontFamily(this);
    }

    @Override
    @ShapedImageView.ShapeStyle
    public int getCurrentProfileImageStyle() {
        return mProfileImageStyle;
    }

    @Override
    public final void restart() {
        Utils.restartActivity(this);
    }

    @Override
    protected void onApplyThemeResource(@NonNull Resources.Theme theme, int resId, boolean first) {
        mCurrentThemeColor = getThemeColor();
        mCurrentActionBarColor = getActionBarColor();
        mCurrentThemeFontFamily = getThemeFontFamily();
        mCurrentThemeBackgroundAlpha = getThemeBackgroundAlpha();
        mCurrentThemeBackgroundOption = getThemeBackgroundOption();
        mProfileImageStyle = Utils.getProfileImageStyle(this);
        super.onApplyThemeResource(theme, resId, first);
    }

}