/*
 * Twittnuker - Twitter client for Android
 *
 * Copyright (C) 2013-2014 vanita5 <mail@vanita5.de>
 *
 * This program incorporates a modified version of Twidere.
 * Copyright (C) 2012-2014 Mariotaku Lee <mariotaku.lee@gmail.com>
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

package de.vanita5.twittnuker.activity.support;

import android.app.ActionBar;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.view.Menu;

import de.vanita5.twittnuker.Constants;
import de.vanita5.twittnuker.activity.iface.IThemedActivity;
import de.vanita5.twittnuker.menu.TwidereMenuInflater;
import de.vanita5.twittnuker.util.StrictModeUtils;
import de.vanita5.twittnuker.util.ThemeUtils;
import de.vanita5.twittnuker.util.Utils;

import static de.vanita5.twittnuker.util.Utils.restartActivity;

public abstract class BaseSupportThemedActivity extends FragmentActivity implements Constants, IThemedActivity {

	private int mCurrentThemeResource, mCurrentThemeColor, mCurrentThemeBackgroundAlpha;
    private TwidereMenuInflater mMenuInflater;

	@Override
	public void finish() {
		super.finish();
		overrideCloseAnimationIfNeeded();
	}

	@Override
    public boolean onCreateOptionsMenu(Menu menu, TwidereMenuInflater inflater) {
        return false;
	}
	
	@Override
    public final boolean onCreateOptionsMenu(Menu menu) {
        return onCreateOptionsMenu(menu, getTwidereMenuInflater());
    }

	@Override
	public Resources getDefaultResources() {
		return super.getResources();
	}

    @Override
    public TwidereMenuInflater getTwidereMenuInflater() {
        if (mMenuInflater != null) return mMenuInflater;
        final ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            return mMenuInflater = new TwidereMenuInflater(actionBar.getThemedContext());
        }
        return mMenuInflater = new TwidereMenuInflater(this);
    }

    @Override
    public final int getCurrentThemeResourceId() {
        return mCurrentThemeResource;
	}

    @Override
    public int getThemeBackgroundAlpha() {
        return ThemeUtils.isTransparentBackground(this) ? ThemeUtils.getUserThemeBackgroundAlpha(this) : 0xff;
    }

    @Override
    public abstract int getOverrideAccentColor();


	@Override
	public String getThemeFontFamily() {
		return ThemeUtils.getThemeFontFamily(this);
	}

    @Override
    public abstract int getThemeResourceId();

	@Override
	public boolean isDarkDrawerEnabled() {
		return ThemeUtils.isDarkDrawerEnabled(this);
	}

	@Override
	public void navigateUpFromSameTask() {
		NavUtils.navigateUpFromSameTask(this);
		overrideCloseAnimationIfNeeded();
	}

	@Override
	public void overrideCloseAnimationIfNeeded() {
		if (shouldOverrideActivityAnimation()) {
			ThemeUtils.overrideActivityCloseAnimation(this);
		} else {
			ThemeUtils.overrideNormalActivityCloseAnimation(this);
		}
    }

	@Override
	public final void restart() {
		restartActivity(this);
	}

	@Override
	public boolean shouldOverrideActivityAnimation() {
		return true;
	}

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		if (Utils.isDebugBuild()) {
			StrictModeUtils.detectAllVmPolicy();
			StrictModeUtils.detectAllThreadPolicy();
		}
		if (shouldOverrideActivityAnimation()) {
			ThemeUtils.overrideActivityOpenAnimation(this);
		}
		setTheme();
		super.onCreate(savedInstanceState);
		setActionBarBackground();
	}

	@Override
	protected void onResume() {
		super.onResume();
			ThemeUtils.notifyStatusBarColorChanged(this, mCurrentThemeResource, mCurrentThemeColor,
					mCurrentThemeBackgroundAlpha);
		}

    protected boolean shouldSetWindowBackground() {
        return true;
    }

	private final void setActionBarBackground() {
        ThemeUtils.applyActionBarBackground(getActionBar(), this, mCurrentThemeResource);
	}

	private final void setTheme() {
		mCurrentThemeResource = getThemeResourceId();
		mCurrentThemeColor = getOverrideAccentColor();
        mCurrentThemeBackgroundAlpha = getThemeBackgroundAlpha();
		ThemeUtils.notifyStatusBarColorChanged(this, mCurrentThemeResource, mCurrentThemeColor,
				mCurrentThemeBackgroundAlpha);
		setTheme(mCurrentThemeResource);
        if (shouldSetWindowBackground() && ThemeUtils.isTransparentBackground(mCurrentThemeResource)) {
            getWindow().setBackgroundDrawable(ThemeUtils.getWindowBackground(this));
        }
	}
}