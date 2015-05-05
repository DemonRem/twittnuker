/*
 * Twittnuker - Twitter client for Android
 *
 * Copyright (C) 2013-2015 vanita5 <mail@vanita5.de>
 *
 * This program incorporates a modified version of Twidere.
 * Copyright (C) 2012-2015 Mariotaku Lee <mariotaku.lee@gmail.com>
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

package de.vanita5.twittnuker.util;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.app.AppCompatDelegateTrojan;
import android.support.v7.internal.app.ToolbarActionBar;
import android.support.v7.internal.app.WindowDecorActionBar;
import android.support.v7.internal.app.WindowDecorActionBar.ActionModeImpl;
import android.support.v7.internal.view.StandaloneActionMode;
import android.support.v7.internal.view.SupportActionModeWrapper;
import android.support.v7.internal.view.SupportActionModeWrapperTrojan;
import android.support.v7.internal.view.menu.ActionMenuItemView;
import android.support.v7.internal.widget.ActionBarContainer;
import android.support.v7.internal.widget.ActionBarContextView;
import android.support.v7.internal.widget.ActionBarOverlayLayout;
import android.support.v7.internal.widget.ContentFrameLayout;
import android.support.v7.widget.ActionMenuPresenter;
import android.support.v7.widget.ActionMenuView;
import android.support.v7.widget.Toolbar;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import org.apache.commons.lang3.ArrayUtils;
import de.vanita5.twittnuker.Constants;
import de.vanita5.twittnuker.R;
import de.vanita5.twittnuker.activity.iface.IThemedActivity;
import de.vanita5.twittnuker.activity.support.LinkHandlerActivity;
import de.vanita5.twittnuker.graphic.ActionBarColorDrawable;
import de.vanita5.twittnuker.graphic.ActionIconDrawable;
import de.vanita5.twittnuker.text.ParagraphSpacingSpan;
import de.vanita5.twittnuker.util.menu.TwidereMenuInfo;
import de.vanita5.twittnuker.util.support.ViewSupport;
import de.vanita5.twittnuker.view.TabPagerIndicator;
import de.vanita5.twittnuker.view.TwidereToolbar;

import java.lang.reflect.Field;

public class ThemeUtils implements Constants {

    public static final int ACCENT_COLOR_THRESHOLD = 192;

	private static final int[] ANIM_OPEN_STYLE_ATTRS = { android.R.attr.activityOpenEnterAnimation,
			android.R.attr.activityOpenExitAnimation };
	private static final int[] ANIM_CLOSE_STYLE_ATTRS = { android.R.attr.activityCloseEnterAnimation,
			android.R.attr.activityCloseExitAnimation };
    public static final int[] ATTRS_TEXT_COLOR_PRIMARY = {android.R.attr.textColorPrimary};
    public static final int[] ATTRS_TEXT_COLOR_PRIMARY_INVERSE = {android.R.attr.textColorPrimaryInverse};
    public static final int[] ATTRS_TEXT_COLOR_PRIMARY_AND_INVERSE = {android.R.attr.textColorPrimary,
            android.R.attr.textColorPrimaryInverse};
    public static final int[] ATTRS_COLOR_FOREGROUND_AND_INVERSE = {android.R.attr.colorForeground,
            android.R.attr.colorForegroundInverse};

	private ThemeUtils() {
		throw new AssertionError();
	}


    public static void applyActionBarBackground(final ActionBar actionBar, final Context context,
                                                final int themeRes, final int actionBarColor,
                                                final String backgroundOption, boolean outlineEnabled) {
        if (actionBar == null || context == null) return;
        actionBar.setBackgroundDrawable(getActionBarBackground(context, themeRes, actionBarColor, backgroundOption, outlineEnabled));
        actionBar.setSplitBackgroundDrawable(getActionBarSplitBackground(context, themeRes));
        actionBar.setStackedBackgroundDrawable(getActionBarBackground(context, themeRes, actionBarColor, backgroundOption, outlineEnabled));
    }


    public static void applyActionBarBackground(final android.support.v7.app.ActionBar actionBar, final Context context,
                                                final int themeRes, final int actionBarColor, String backgroundOption, boolean outlineEnabled) {
        if (actionBar == null || context == null) return;
        actionBar.setBackgroundDrawable(getActionBarBackground(context, themeRes, actionBarColor, backgroundOption, outlineEnabled));
        actionBar.setSplitBackgroundDrawable(getActionBarSplitBackground(context, themeRes));
        actionBar.setStackedBackgroundDrawable(getActionBarStackedBackground(context, themeRes, actionBarColor, outlineEnabled));
    }


    public static void applyActionBarBackground(final ActionBarContainer actionBar, final Context context,
                                                final int themeRes, final int actionBarColor, String backgroundOption, boolean outlineEnabled) {
        if (actionBar == null || context == null) return;
        actionBar.setPrimaryBackground(getActionBarBackground(context, themeRes, actionBarColor, backgroundOption, outlineEnabled));
        actionBar.setSplitBackground(getActionBarSplitBackground(context, themeRes));
        actionBar.setStackedBackground(getActionBarStackedBackground(context, themeRes, actionBarColor, outlineEnabled));
	}

    public static void applyColorFilterToMenuIcon(Activity activity, Menu menu) {
        final ActionBar actionBar = activity.getActionBar();
        final Context context = actionBar != null ? actionBar.getThemedContext() : activity;
        final int color = getThemeForegroundColor(context);
        final int popupTheme = getActionBarPopupThemeRes(context);
        final int popupColor = getThemeForegroundColor(context, popupTheme);
        final int highlightColor = getUserAccentColor(activity);
        applyColorFilterToMenuIcon(menu, color, popupColor, highlightColor, Mode.SRC_ATOP);
    }

    public static void applyColorFilterToMenuIcon(final Menu menu, final int color,
                                                  final int highlightColor, final Mode mode,
                                                  final int... excludedGroups) {
        applyColorFilterToMenuIcon(menu, color, color, highlightColor, mode, excludedGroups);
    }

    public static void applyColorFilterToMenuIcon(final Menu menu, final int color, final int popupColor,
                                                  final int highlightColor, final Mode mode,
                                                  final int... excludedGroups) {
        for (int i = 0, j = menu.size(); i < j; i++) {
            final MenuItem item = menu.getItem(i);
            final Drawable icon = item.getIcon();
            final ContextMenuInfo info = item.getMenuInfo();
            if (icon != null && !ArrayUtils.contains(excludedGroups, item.getGroupId())) {
                icon.mutate();
                if (info instanceof TwidereMenuInfo) {
                    final TwidereMenuInfo sInfo = (TwidereMenuInfo) info;
                    icon.setColorFilter(sInfo.isHighlight() ?
                            sInfo.getHighlightColor(highlightColor) : color, mode);
                } else {
                    icon.setColorFilter(color, mode);
                }
            }
            if (item.hasSubMenu()) {
                // SubMenu item is always in popup
                applyColorFilterToMenuIcon(item.getSubMenu(), popupColor, popupColor, highlightColor, mode, excludedGroups);
            }
        }
    }

    public static void applyParagraphSpacing(TextView textView, float multiplier) {
        final SpannableStringBuilder builder = SpannableStringBuilder.valueOf(textView.getText());
        int prevLineBreak, currLineBreak = 0;
        for (int i = 0, j = builder.length(); i < j; i++) {
            if (builder.charAt(i) == '\n') {
                prevLineBreak = currLineBreak;
                currLineBreak = i;
                if (currLineBreak > 0) {
                    builder.setSpan(new ParagraphSpacingSpan(multiplier), prevLineBreak, currLineBreak,
                            Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                }
            }
        }
        textView.setText(builder);
    }

    public static void applySupportActionModeColor(final ActionMode mode, final Activity activity,
                                                   int themeRes, int actionBarColor,
                                                        String backgroundOption, boolean outlineEnabled) {
        // Very dirty implementation
        if (!(mode instanceof SupportActionModeWrapper) || !(activity instanceof IThemedActivity))
            return;
        final android.support.v7.view.ActionMode modeCompat = SupportActionModeWrapperTrojan.getWrappedObject((SupportActionModeWrapper) mode);
        applySupportActionModeColor(modeCompat, activity, themeRes, actionBarColor, backgroundOption, outlineEnabled);
    }

    public static void applySupportActionModeColor(final android.support.v7.view.ActionMode modeCompat,
                                                   Activity activity, int themeRes,
                                                   int actionBarColor, String backgroundOption,
                                                   boolean outlineEnabled) {
        // Very dirty implementation
        // This call ensures TitleView created
        modeCompat.setTitle(null);
        try {
            View contextView = null;
            Context actionBarContext = null;
            if (modeCompat instanceof ActionModeImpl) {
                WindowDecorActionBar actionBar = (WindowDecorActionBar) Utils.findFieldOfTypes(modeCompat,
                        ActionModeImpl.class, WindowDecorActionBar.class);
				if (actionBar == null) return;
                actionBarContext = actionBar.getThemedContext();
				final Field contextViewField = WindowDecorActionBar.class.getDeclaredField("mContextView");
				contextViewField.setAccessible(true);
                contextView = (View) contextViewField.get(actionBar);
            } else if (modeCompat instanceof StandaloneActionMode) {
                final Field[] fields = StandaloneActionMode.class.getDeclaredFields();
                for (Field field : fields) {
                    if (ActionBarContextView.class.isAssignableFrom(field.getType())) {
                        field.setAccessible(true);
                        contextView = (View) field.get(modeCompat);
                    } else if (Context.class.isAssignableFrom(field.getType())) {
                        field.setAccessible(true);
                        actionBarContext = (Context) field.get(modeCompat);
                    }
                    if (contextView != null && actionBarContext != null) break;
                }
            }
            if (!(contextView instanceof ActionBarContextView) || actionBarContext == null) return;
            setActionBarContextViewColor(actionBarContext, contextView, themeRes, actionBarColor, backgroundOption, outlineEnabled);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void setActionBarContextViewColor(Context context, View contextView, int themeRes, int actionBarColor, String backgroundOption, boolean outlineEnabled) {
		final TextView actionBarTitleView = (TextView) contextView.findViewById(android.support.v7.appcompat.R.id.action_bar_title);
		final TextView actionBarSubtitleView = (TextView) contextView.findViewById(android.support.v7.appcompat.R.id.action_bar_subtitle);
		final ImageView actionModeCloseButton = (ImageView) contextView.findViewById(android.support.v7.appcompat.R.id.action_mode_close_button);
		final ActionMenuView menuView = ViewSupport.findViewByType(contextView, ActionMenuView.class);
		final int titleColor = getContrastActionBarTitleColor(context, themeRes, actionBarColor);
		final int itemColor = getContrastActionBarItemColor(context, themeRes, actionBarColor);
		if (actionBarTitleView != null) {
			actionBarTitleView.setTextColor(titleColor);
		}
		if (actionBarSubtitleView != null) {
			actionBarSubtitleView.setTextColor(titleColor);
		}
		if (actionModeCloseButton != null) {
			actionModeCloseButton.setColorFilter(itemColor, Mode.SRC_ATOP);
		}
		if (menuView != null) {
			setActionBarOverflowColor(menuView, itemColor);
			ThemeUtils.wrapToolbarMenuIcon(menuView, itemColor, itemColor);
		}
		ViewSupport.setBackground(contextView, getActionBarBackground(context, themeRes, actionBarColor, backgroundOption, outlineEnabled));
    }

    public static void applyWindowBackground(Context context, Window window, int theme, String option, int alpha) {
        if (isWindowFloating(context, theme)) return;
        if (VALUE_THEME_BACKGROUND_TRANSPARENT.equals(option)) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
            window.setBackgroundDrawable(ThemeUtils.getWindowBackgroundApplyAlpha(context, alpha));
        } else if (VALUE_THEME_BACKGROUND_SOLID.equals(option)) {
            window.setBackgroundDrawable(new ColorDrawable(isDarkTheme(theme) ? Color.BLACK : Color.WHITE));
        }
    }

    public static void applyWindowBackground(Context context, Window window, AppCompatDelegate delegate, int theme, String option, int alpha) {
        if (isWindowFloating(delegate)) return;
        if (VALUE_THEME_BACKGROUND_TRANSPARENT.equals(option)) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
            window.setBackgroundDrawable(ThemeUtils.getWindowBackgroundApplyAlpha(context, alpha));
        } else if (VALUE_THEME_BACKGROUND_SOLID.equals(option)) {
            window.setBackgroundDrawable(new ColorDrawable(isDarkTheme(theme) ? Color.BLACK : Color.WHITE));
        }
    }

    private static boolean isWindowFloating(AppCompatDelegate delegate) {
        return AppCompatDelegateTrojan.isFloating(delegate);
    }

    public static void applyWindowBackground(Context context, View window, int theme, String option, int alpha) {
        if (isWindowFloating(context, theme)) return;
        if (VALUE_THEME_BACKGROUND_TRANSPARENT.equals(option)) {
            ViewSupport.setBackground(window, ThemeUtils.getWindowBackgroundApplyAlpha(context, alpha));
        } else if (VALUE_THEME_BACKGROUND_SOLID.equals(option)) {
            ViewSupport.setBackground(window, new ColorDrawable(isDarkTheme(theme) ? Color.BLACK : Color.WHITE));
        } else {
            ViewSupport.setBackground(window, ThemeUtils.getWindowBackground(context));
        }
    }


    @NonNull
    public static Drawable getActionBarBackground(final Context context, final int themeRes,
                                                  final int actionBarColor, final String backgroundOption,
                                                  final boolean outlineEnabled) {
        return ActionBarColorDrawable.create(actionBarColor, outlineEnabled);
    }

    public static Drawable getActionBarHomeAsUpIndicator(android.support.v7.app.ActionBar actionBar) {
        final Context context = actionBar.getThemedContext();
        @SuppressWarnings("ConstantConditions")
        final TypedArray a = context.obtainStyledAttributes(null, R.styleable.ActionBar, R.attr.actionBarStyle, 0);
        try {
            return a.getDrawable(R.styleable.ActionBar_homeAsUpIndicator);
        } finally {
            a.recycle();
        }
    }

    public static int getActionBarPopupThemeRes(final Context context) {
        final TypedArray a = context.obtainStyledAttributes(new int[]{R.attr.actionBarPopupTheme});
        try {
            return a.getResourceId(0, 0);
        } finally {
            a.recycle();
        }
    }

    public static Drawable getActionBarSplitBackground(final Context context, final int themeRes) {
        @SuppressWarnings("ConstantConditions")
        final TypedArray a = context.obtainStyledAttributes(null, new int[]{android.R.attr.backgroundSplit},
                android.R.attr.actionBarStyle, themeRes);
        try {
            return a.getDrawable(0);
        } finally {
        	a.recycle();
        }
    }

    @NonNull
    public static Drawable getActionBarStackedBackground(final Context context, final int themeRes,
                                                         final int actionBarColor, boolean outlineEnabled) {
        return ActionBarColorDrawable.create(actionBarColor, outlineEnabled);
    }

    public static int getCardBackgroundColor(final Context context, String backgroundOption, int themeAlpha) {
        final TypedArray a = context.obtainStyledAttributes(new int[]{R.attr.cardItemBackgroundColor});
        final int color = a.getColor(0, Color.TRANSPARENT);
        a.recycle();
        if (isTransparentBackground(backgroundOption)) {
        	return themeAlpha << 24 | (0x00FFFFFF & color);
        } else if (isSolidBackground(backgroundOption)) {
            return TwidereColorUtils.getContrastYIQ(color, Color.WHITE, Color.BLACK);
        } else {
            return color;
        }
    }

    public static Drawable getCompatToolbarOverlay(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) return null;
        final Window window = activity.getWindow();
        final View view = window.findViewById(android.support.v7.appcompat.R.id.decor_content_parent);
        if (!(view instanceof ActionBarOverlayLayout)) {
            final View contentLayout = window.findViewById(android.support.v7.appcompat.R.id.action_bar_activity_content);
            if (contentLayout instanceof ContentFrameLayout) {
                return ((ContentFrameLayout) contentLayout).getForeground();
            }
            return null;
        }
        try {
            final Field field = ActionBarOverlayLayout.class.getDeclaredField("mWindowContentOverlay");
            field.setAccessible(true);
            return (Drawable) field.get(view);
        } catch (Exception ignore) {
        }
        return null;
    }

	public static int getComposeThemeResource(final Context context) {
        return getComposeThemeResource(getThemeNameOption(context));
	}

    public static int getComposeThemeResource(final String name) {
        if (VALUE_THEME_NAME_DARK.equals(name)) return R.style.Theme_Twidere_Dark_Compose;
        return R.style.Theme_Twidere_Light_Compose;
	}

    public static int getContrastActionBarItemColor(Context context, int theme, int color) {
        if (isDarkTheme(theme) || TwidereColorUtils.getYIQLuminance(color) <= ACCENT_COLOR_THRESHOLD) {
            //return light text color
            return context.getResources().getColor(R.color.action_icon_light);
        }
        //return dark text color
        return context.getResources().getColor(R.color.action_icon_dark);
    }

    public static int getContrastActionBarTitleColor(Context context, int theme, int color) {
        if (isDarkTheme(theme)) {
            //return light text color
            return getColorFromAttribute(context, android.R.attr.colorForeground, Color.RED);
        } else if (TwidereColorUtils.getYIQLuminance(color) <= ACCENT_COLOR_THRESHOLD) {
            //return light text color
            return getColorFromAttribute(context, android.R.attr.colorForegroundInverse, Color.RED);
        } else {
        //return dark text color
            return getColorFromAttribute(context, android.R.attr.colorForeground, Color.RED);
        }
    }

	public static int getDialogThemeResource(final Context context) {
		return getDialogThemeResource(getThemeNameOption(context));
	}

	public static int getDialogThemeResource(final String name) {
		if (VALUE_THEME_NAME_DARK.equals(name)) return R.style.Theme_Twidere_Dark_Dialog;
		return R.style.Theme_Twidere_Light_Dialog;
	}

    public static Context getDialogThemedContext(final Context context) {
        return new ContextThemeWrapper(context, getDialogThemeResource(context));
    }

    public static int getDialogWhenLargeThemeResource(final Context context) {
        return getDialogWhenLargeThemeResource(getThemeNameOption(context));
    }

    public static int getDialogWhenLargeThemeResource(final String name) {
        if (VALUE_THEME_NAME_DARK.equals(name)) {
            return R.style.Theme_Twidere_Dark_DialogWhenLarge_NoActionBar;
        }
        return R.style.Theme_Twidere_Light_DialogWhenLarge_NoActionBar;
    }

	public static int getDrawerThemeResource(final Context context) {
        return getDrawerThemeResource(getNoActionBarThemeResource(context));
	}

	public static int getDrawerThemeResource(final int themeRes) {
		return R.style.Theme_Twidere_Drawer_Dark;
	}

    public static Drawable getImageHighlightDrawable(final Context context) {
        final Drawable d = getSelectableItemBackgroundDrawable(context);
        if (d != null) {
            d.setAlpha(0x80);
        }
        return d;
    }

	public static int getNoDisplayThemeResource(final Context context) {
		if (context == null) return R.style.Theme_Twidere_Dark_NoDisplay;
        final SharedPreferencesWrapper pref = getSharedPreferencesWrapper(context);
		final String theme = pref.getString(KEY_THEME, VALUE_THEME_NAME_LIGHT);
		if (VALUE_THEME_NAME_DARK.equals(theme)) return R.style.Theme_Twidere_Dark_NoDisplay;
		return R.style.Theme_Twidere_Light_NoDisplay;
	}

    public static int getOptimalLinkColor(int linkColor, int color) {
        final int[] yiq = new int[3];
        TwidereColorUtils.colorToYIQ(color, yiq);
        final int y = yiq[0];
        TwidereColorUtils.colorToYIQ(linkColor, yiq);
        if (y < 32 && yiq[0] <= ACCENT_COLOR_THRESHOLD) {
            return linkColor;
        } else if (y > ACCENT_COLOR_THRESHOLD && yiq[0] > 32) {
            return linkColor;
        }
        yiq[0] = yiq[0] + (y - yiq[0]) / 2;
        return TwidereColorUtils.YIQToColor(Color.alpha(linkColor), yiq);
    }

    public static int getQuickSearchBarThemeResource(final Context context) {
        return getQuickSearchBarThemeResource(getThemeNameOption(context));
    }

    public static int getQuickSearchBarThemeResource(final String name) {
        if (VALUE_THEME_NAME_DARK.equals(name)) return R.style.Theme_Twidere_Dark_QuickSearchBar;
        return R.style.Theme_Twidere_Light_QuickSearchBar;
    }

    public static Resources getResources(final Context context) {
        return context.getResources();
    }

	public static Drawable getSelectableItemBackgroundDrawable(final Context context) {
        final TypedArray a = context.obtainStyledAttributes(new int[]{android.R.attr.selectableItemBackground});
		try {
			return a.getDrawable(0);
		} finally {
			a.recycle();
		}
	}

    public static Drawable getSupportActionBarBackground(final Context context, final int themeRes) {
        @SuppressWarnings("ConstantConditions")
        final TypedArray array = context.obtainStyledAttributes(null, new int[]{android.R.attr.background},
                R.attr.actionBarStyle, themeRes);
        try {
            return array.getDrawable(0);
        } finally {
            array.recycle();
        }
    }

    public static float getSupportActionBarElevation(final Context context) {
        @SuppressWarnings("ConstantConditions")
        final TypedArray a = context.obtainStyledAttributes(null, new int[]{R.attr.elevation}, R.attr.actionBarStyle, 0);
        try {
            return a.getDimension(0, 0);
        } finally {
            a.recycle();
        }
    }

    public static int getColorFromAttribute(Context context, int attr, int def) {
        final TypedValue outValue = new TypedValue();
        if (!context.getTheme().resolveAttribute(attr, outValue, true))
            return def;
        if (outValue.type == TypedValue.TYPE_REFERENCE)
            return context.getResources().getColor(attr);
        return outValue.data;
    }

	public static int getTextAppearanceLarge(final Context context) {
		// final Context wrapped = getThemedContext(context,
		// getResources(context));
		final TypedArray a = context.obtainStyledAttributes(new int[] { android.R.attr.textAppearanceLarge });
		final int textAppearance = a.getResourceId(0, android.R.style.TextAppearance_Holo_Large);
		a.recycle();
		return textAppearance;
	}

	public static int getTextColorPrimary(final Context context) {
        final TypedArray a = context.obtainStyledAttributes(ATTRS_TEXT_COLOR_PRIMARY);
		try {
			return a.getColor(0, Color.TRANSPARENT);
	    } finally {
			a.recycle();
		}
	}

    public static void getTextColorPrimaryAndInverse(final Context context, int[] colors) {
        final TypedArray a = context.obtainStyledAttributes(ATTRS_TEXT_COLOR_PRIMARY_AND_INVERSE);
        try {
            colors[0] = a.getColor(0, Color.TRANSPARENT);
            colors[1] = a.getColor(1, Color.TRANSPARENT);
        } finally {
            a.recycle();
        }
    }

    public static void getDarkLightForegroundColors(final Context context, int themeRes, int[] colors) {
        final TypedArray a = context.obtainStyledAttributes(ATTRS_COLOR_FOREGROUND_AND_INVERSE);
        try {
            if (isDarkTheme(themeRes)) {
                colors[0] = a.getColor(1, Color.WHITE);
                colors[1] = a.getColor(0, Color.BLACK);
            } else {
                colors[0] = a.getColor(0, Color.WHITE);
                colors[1] = a.getColor(1, Color.BLACK);
            }
        } finally {
            a.recycle();
        }
    }

    public static void getColorForegroundAndInverse(final Context context, int[] colors) {
        final TypedArray a = context.obtainStyledAttributes(ATTRS_COLOR_FOREGROUND_AND_INVERSE);
        try {
            colors[0] = a.getColor(0, Color.WHITE);
            colors[1] = a.getColor(1, Color.BLACK);
        } finally {
            a.recycle();
        }
    }

    public static int getTextColorPrimaryInverse(final Context context) {
        final TypedArray a = context.obtainStyledAttributes(ATTRS_TEXT_COLOR_PRIMARY_INVERSE);
        try {
            return a.getColor(0, Color.TRANSPARENT);
        } finally {
            a.recycle();
        }
    }

	public static int getTextColorSecondary(final Context context) {
        @SuppressWarnings("ConstantConditions")
		final TypedArray a = context.obtainStyledAttributes(new int[]{android.R.attr.textColorSecondary});
		try {
		    return a.getColor(0, Color.TRANSPARENT);
		} finally {
		    a.recycle();
		}
	}

    public static int getThemeBackgroundColor(final Context context) {
        final TypedArray a = context.obtainStyledAttributes(new int[]{android.R.attr.colorBackground});
        try {
            return a.getColor(0, 0);
        } finally {
            a.recycle();
        }
    }

    public static int getThemeBackgroundColor(final Context context, int themeRes) {
        if (themeRes == 0) {
            return getThemeBackgroundColor(context);
        }
        @SuppressWarnings("ConstantConditions")
        final TypedArray a = context.obtainStyledAttributes(null, new int[]{android.R.attr.colorBackground},
                0, themeRes);
        try {
            return a.getColor(0, 0);
        } finally {
            a.recycle();
        }
    }

	public static String getThemeBackgroundOption(final Context context) {
		if (context == null) return VALUE_THEME_BACKGROUND_DEFAULT;
        final SharedPreferencesWrapper pref = getSharedPreferencesWrapper(context);
		return pref.getString(KEY_THEME_BACKGROUND, VALUE_THEME_BACKGROUND_DEFAULT);
	}

	public static int getThemeColor(final Context context) {
        final TypedArray a = context.obtainStyledAttributes(new int[]{android.R.attr.colorActivatedHighlight});
		try {
            final Resources resources = context.getResources();
            return a.getColor(0, resources.getColor(R.color.material_light_blue));
		} finally {
		    a.recycle();
		}
	}

    public static int getThemeColor(Context context, int themeResourceId) {
        final Context appContext = context.getApplicationContext();
        final Resources res = appContext.getResources();
        @SuppressWarnings("ConstantConditions")
        final TypedArray a = appContext.obtainStyledAttributes(null,
                new int[]{android.R.attr.colorActivatedHighlight}, 0, themeResourceId);
        try {
            return a.getColor(0, res.getColor(R.color.material_light_blue));
        } finally {
            a.recycle();
    	}
    }

	public static String getThemeFontFamily(final Context context) {
		if (context == null) return VALUE_THEME_FONT_FAMILY_REGULAR;
        final SharedPreferencesWrapper pref = getSharedPreferencesWrapper(context);
		final String fontFamily = pref.getString(KEY_THEME_FONT_FAMILY, VALUE_THEME_FONT_FAMILY_REGULAR);
		if (!TextUtils.isEmpty(fontFamily)) return fontFamily;
		return VALUE_THEME_FONT_FAMILY_REGULAR;
	}

	public static int getThemeForegroundColor(final Context context) {
        return getThemeForegroundColor(context, 0);
    }

    public static int getThemeForegroundColor(final Context context, int theme) {
        final Context wrapped = theme != 0 ? new ContextThemeWrapper(context, theme) : context;
		final TypedArray a = wrapped.obtainStyledAttributes(new int[]{android.R.attr.colorForeground});
		try {
            return a.getColor(0, 0);
		} finally {
			a.recycle();
        }
    }

    public static String getThemeNameOption(final Context context) {
		if (context == null) return VALUE_THEME_NAME_LIGHT;
        final SharedPreferencesWrapper pref = getSharedPreferencesWrapper(context);
        return pref.getString(KEY_THEME, VALUE_THEME_NAME_LIGHT);
	}

	public static int getThemeResource(final Context context) {
        return getThemeResource(getThemeNameOption(context));
	}

    public static int getThemeResource(final String name) {
        if (VALUE_THEME_NAME_DARK.equals(name)) {
			return R.style.Theme_Twidere_Dark;
		}
        return R.style.Theme_Twidere_Light;
	}


    public static int getThemeResource(final int otherTheme) {
        if (isDarkTheme(otherTheme)) {
            return R.style.Theme_Twidere_Dark;
        }
        return R.style.Theme_Twidere_Light;
    }


    public static int getNoActionBarThemeResource(final Context context) {
        return getNoActionBarThemeResource(getThemeNameOption(context));
    }

    public static int getNoActionBarThemeResource(final String name) {
        if (VALUE_THEME_NAME_DARK.equals(name)) {
            return R.style.Theme_Twidere_Dark_NoActionBar;
        }
        return R.style.Theme_Twidere_Light_NoActionBar;
    }

    public static Context getThemedContext(final Context context) {
        return context;
	}

    public static Context getThemedContext(final Context context, final Resources res) {
        return context;
    }

	public static int getTitleTextAppearance(final Context context) {
        @SuppressWarnings("ConstantConditions")
        final TypedArray a = context.obtainStyledAttributes(null, new int[]{android.R.attr.titleTextStyle},
                android.R.attr.actionBarStyle, android.R.style.Widget_Holo_ActionBar);
        final int textAppearance = a.getResourceId(0, android.R.style.TextAppearance_Holo);
        a.recycle();
        return textAppearance;
	}

    public static int getUserAccentColor(final Context context) {
        if (context == null) return Color.TRANSPARENT;
        final SharedPreferencesWrapper pref = getSharedPreferencesWrapper(context);
        final Resources res = context.getResources();
        final int def = res.getColor(R.color.branding_color);
        return pref.getInt(KEY_THEME_COLOR, def);
    }

    public static int getOptimalAccentColor(final Context context, boolean isActionBarContext, int themeResId) {
        final int userAccentColor = getUserAccentColor(context);
        final int backgroundColorApprox;
        final boolean isDarkTheme = isDarkTheme(themeResId);
        if (!isActionBarContext) {
            backgroundColorApprox = isDarkTheme ? Color.BLACK : Color.WHITE;
        } else if (isDarkTheme) {
            // View context is derived from ActionBar but is currently dark theme, so we should show
            // light
            backgroundColorApprox = Color.BLACK;
        } else {
            // View context is derived from ActionBar and it's light theme, so we use contrast color
            backgroundColorApprox = Color.WHITE;
        }
        if (Math.abs(TwidereColorUtils.getYIQContrast(backgroundColorApprox, userAccentColor)) > 64)
            return userAccentColor;
        return getColorFromAttribute(context, R.attr.colorAccent, context.getResources().getColor(R.color.branding_color));
    }

    public static int getUserHighlightColor(final Context context) {
        final int color = getUserLinkTextColor(context);
        final int red = Color.red(color), green = Color.green(color), blue = Color.blue(color);
        return Color.argb(0x66, red, green, blue);
    }

    public static int getUserLinkTextColor(final Context context) {
        if (context == null) return new TextPaint().linkColor;
        final int themeColor = getUserAccentColor(context);
        final float[] hsv = new float[3];
        Color.colorToHSV(themeColor, hsv);
        if (isDarkTheme(context)) {
            hsv[2] = MathUtils.clamp(hsv[2], 1, 0.5f);
        } else {
            hsv[2] = MathUtils.clamp(hsv[2], 0.1f, 0.75f);
        }
        return Color.HSVToColor(hsv);
    }

    public static int getUserThemeBackgroundAlpha(final Context context) {
        if (context == null) return DEFAULT_THEME_BACKGROUND_ALPHA;
        final SharedPreferencesWrapper pref = getSharedPreferencesWrapper(context);
        return pref.getInt(KEY_THEME_BACKGROUND_ALPHA, DEFAULT_THEME_BACKGROUND_ALPHA);
    }

	public static int getActionBarColor(final Context context) {
        if (context == null) return MATERIAL_DARK;
		final SharedPreferencesWrapper pref = getSharedPreferencesWrapper(context);
        final Resources res = context.getResources();
		final int def = res.getColor(R.color.twittnuker_material_dark);
		return pref.getInt(KEY_ACTION_BAR_COLOR, def);
	}

	public static Typeface getUserTypeface(final Context context, final Typeface defTypeface) {
        if (context == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN)
            return Typeface.DEFAULT;
		final int fontStyle = defTypeface != null ? defTypeface.getStyle() : Typeface.NORMAL;
		final String fontFamily = getThemeFontFamily(context);
		final Typeface tf = Typeface.create(fontFamily, fontStyle);
		if (tf != null) return tf;
		return Typeface.create(Typeface.DEFAULT, fontStyle);
	}

    public static Typeface getUserTypeface(final Context context, final String fontFamily, final Typeface defTypeface) {
        if (context == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN)
            return Typeface.DEFAULT;
        final int fontStyle = defTypeface != null ? defTypeface.getStyle() : Typeface.NORMAL;
        final Typeface tf = Typeface.create(fontFamily, fontStyle);
        if (tf != null) return tf;
        return Typeface.create(Typeface.DEFAULT, fontStyle);
    }

	public static int getViewerThemeResource(final Context context) {
        return R.style.Theme_Twidere_Viewer;
	}

    public static Drawable getWindowBackground(final Context context) {
        final TypedArray a = context.obtainStyledAttributes(new int[]{android.R.attr.windowBackground});
        try {
            return a.getDrawable(0);
        } finally {
            a.recycle();
        }
    }

    public static Drawable getWindowBackgroundApplyAlpha(final Context context, final int alpha) {
        final TypedArray a = context.obtainStyledAttributes(new int[]{android.R.attr.windowBackground});
		final Drawable d = a.getDrawable(0);
		a.recycle();
        if (d != null) {
            d.setAlpha(alpha);
        }
		return d;
	}

    public static Drawable getWindowBackgroundFromTheme(final Context context, int theme) {
        @SuppressWarnings("ConstantConditions")
        final TypedArray a = context.obtainStyledAttributes(null, new int[]{android.R.attr.windowBackground}, 0, theme);
        try {
            return a.getDrawable(0);
        } finally {
			a.recycle();
        }
	}

	public static Drawable getWindowContentOverlay(final Context context) {
        final TypedArray a = context.obtainStyledAttributes(new int[]{android.R.attr.windowContentOverlay});
        try {
            return a.getDrawable(0);
        } finally {
            a.recycle();
        }
    }

    public static Drawable getWindowContentOverlay(final Context context, int themeRes) {
        @SuppressWarnings("ConstantConditions")
        final TypedArray a = context.obtainStyledAttributes(null, new int[]{android.R.attr.windowContentOverlay}, 0, themeRes);
        try {
            return a.getDrawable(0);
        } finally {
            a.recycle();
        }
    }

    public static Drawable getNormalWindowContentOverlay(final Context context, int themeRes) {
        final int normalThemeResId = ThemeUtils.getThemeResource(themeRes);
        return getWindowContentOverlay(context, normalThemeResId);
    }

    public static void initPagerIndicatorAsActionBarTab(FragmentActivity activity, TabPagerIndicator indicator, @Nullable View pagerOverlay) {
        final float supportActionBarElevation = getSupportActionBarElevation(activity);
        ViewCompat.setElevation(indicator, supportActionBarElevation);
        if (!(activity instanceof IThemedActivity)) return;
        final int themeRes = ((IThemedActivity) activity).getCurrentThemeResourceId();
        final int themeColor = ((IThemedActivity) activity).getCurrentThemeColor();
        final int actionBarColor = ((IThemedActivity) activity).getCurrentActionBarColor();
        final int colorDark, colorLight;
        final int[] textColors = new int[2];
        getTextColorPrimaryAndInverse(activity, textColors);
        if (isDarkTheme(themeRes)) {
            colorDark = textColors[1];
            colorLight = textColors[0];
        } else {
            colorDark = textColors[0];
            colorLight = textColors[1];
        }
        final int contrastColor = TwidereColorUtils.getContrastYIQ(actionBarColor, ACCENT_COLOR_THRESHOLD,
                colorDark, colorLight);
        ViewSupport.setBackground(indicator, getActionBarStackedBackground(activity, themeRes, actionBarColor, true));
		indicator.setIconColor(contrastColor);
		indicator.setLabelColor(contrastColor);
		indicator.setStripColor(themeColor);
        indicator.updateAppearance();
        if (pagerOverlay != null) {
            ViewSupport.setBackground(pagerOverlay, getNormalWindowContentOverlay(activity, themeRes));
        }
    }

    public static boolean isColoredActionBar(int themeRes) {
        return !isDarkTheme(themeRes);
	}

	public static boolean isDarkTheme(final Context context) {
        return isDarkTheme(getNoActionBarThemeResource(context));
	}

	public static boolean isDarkTheme(final int themeRes) {
		switch (themeRes) {
			case R.style.Theme_Twidere_Dark:
            case R.style.Theme_Twidere_Dark_NoActionBar:
			case R.style.Theme_Twidere_Dark_Dialog:
            case R.style.Theme_Twidere_Dark_DialogWhenLarge_NoActionBar:
			case R.style.Theme_Twidere_Dark_Compose:
				return true;
		}
		return false;
	}

    public static boolean isSolidBackground(final String option) {
        return VALUE_THEME_BACKGROUND_SOLID.equals(option);
    }

	public static boolean isTransparentBackground(final Context context) {
        return isTransparentBackground(getThemeBackgroundOption(context));
    }

    public static boolean isTransparentBackground(final String option) {
        return VALUE_THEME_BACKGROUND_TRANSPARENT.equals(option);
	}

    public static boolean isWindowFloating(Context context, int theme) {
        final TypedArray a;
        if (theme != 0) {
            //noinspection ConstantConditions
            a = context.obtainStyledAttributes(null, new int[]{android.R.attr.windowIsFloating}, 0, theme);
        } else {
            a = context.obtainStyledAttributes(new int[]{android.R.attr.windowIsFloating});
        }
        try {
            return a.getBoolean(0, false);
        } finally {
            a.recycle();
        }
    }

	public static void overrideActivityCloseAnimation(final Activity activity) {
        TypedArray a = activity.obtainStyledAttributes(new int[] { android.R.attr.windowAnimationStyle });
        final int windowAnimationStyleResId = a.getResourceId(0, 0);
        a.recycle();
		// Now retrieve the resource ids of the actual animations used in the
		// animation style pointed to by
		// the window animation resource id.
        a = activity.obtainStyledAttributes(windowAnimationStyleResId, ANIM_CLOSE_STYLE_ATTRS);
        final int activityCloseEnterAnimation = a.getResourceId(0, 0);
        final int activityCloseExitAnimation = a.getResourceId(1, 0);
        a.recycle();
		activity.overridePendingTransition(activityCloseEnterAnimation, activityCloseExitAnimation);
	}

	public static void overrideActivityOpenAnimation(final Activity activity) {

        TypedArray a = activity.obtainStyledAttributes(new int[] { android.R.attr.windowAnimationStyle });
        final int windowAnimationStyleResId = a.getResourceId(0, 0);
        a.recycle();
		// Now retrieve the resource ids of the actual animations used in the
		// animation style pointed to by
		// the window animation resource id.
        a = activity.obtainStyledAttributes(windowAnimationStyleResId, ANIM_OPEN_STYLE_ATTRS);
        final int activityOpenEnterAnimation = a.getResourceId(0, 0);
        final int activityOpenExitAnimation = a.getResourceId(1, 0);
        a.recycle();
		activity.overridePendingTransition(activityOpenEnterAnimation, activityOpenExitAnimation);
	}

	public static void overrideNormalActivityCloseAnimation(final Activity activity) {
        @SuppressWarnings("ConstantConditions")
        final TypedArray a = activity.obtainStyledAttributes(null, ANIM_CLOSE_STYLE_ATTRS,
                0, android.R.style.Animation_Activity);
		final int activityCloseEnterAnimation = a.getResourceId(0, 0);
		final int activityCloseExitAnimation = a.getResourceId(1, 0);
		a.recycle();
		activity.overridePendingTransition(activityCloseEnterAnimation, activityCloseExitAnimation);
	}

    public static void resetCheatSheet(ActionMenuView menuView) {
        final OnLongClickListener listener = new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (((ActionMenuItemView) v).hasText()) return false;
                final MenuItem menuItem = ((ActionMenuItemView) v).getItemData();
                Utils.showMenuItemToast(v, menuItem.getTitle(), true);
                return true;
            }
        };
        for (int i = 0, j = menuView.getChildCount(); i < j; i++) {
            final View child = menuView.getChildAt(i);
            if (!(child instanceof ActionMenuItemView)) continue;
            final ActionMenuItemView itemView = (ActionMenuItemView) child;
            final MenuItem menuItem = itemView.getItemData();
            if (menuItem.hasSubMenu()) continue;
            itemView.setOnLongClickListener(listener);
        }
    }

    public static void setActionBarColor(Window window, android.support.v7.app.ActionBar actionBar,
                                         int titleColor, int itemColor) {
        final Drawable drawable = getActionBarHomeAsUpIndicator(actionBar);
        if (drawable != null) {
            drawable.setColorFilter(itemColor, Mode.SRC_ATOP);
        }
        actionBar.setHomeAsUpIndicator(drawable);
        // Ensure title view created
        if (actionBar instanceof WindowDecorActionBar) {
            actionBar.setTitle(actionBar.getTitle());
            actionBar.setSubtitle(actionBar.getSubtitle());
			setActionBarTitleTextColor(window, titleColor);
			setActionBarSubtitleTextColor(window, titleColor);
        } else if (actionBar instanceof ToolbarActionBar) {

		}
    }

    public static void setToolBarColor(@NonNull Toolbar toolbar, int titleColor, int itemColor) {
        final Drawable drawable = toolbar.getNavigationIcon();
        if (drawable != null) {
            drawable.setColorFilter(itemColor, Mode.SRC_ATOP);
        }
        toolbar.setNavigationIcon(drawable);
        toolbar.setTitleTextColor(titleColor);
        toolbar.setSubtitleTextColor(titleColor);
    }

    public static void setActionBarOverflowColor(Toolbar toolbar, int itemColor) {
        if (toolbar == null) return;
        if (toolbar instanceof TwidereToolbar) {
            ((TwidereToolbar) toolbar).setItemColor(itemColor);
        }
        final ActionMenuView actionMenuView = ViewSupport.findViewByType(toolbar, ActionMenuView.class);
        if (actionMenuView == null) return;
        View overflowView = null;
        for (int i = 0, j = actionMenuView.getChildCount(); i < j; i++) {
            final View child = actionMenuView.getChildAt(i);
            final ActionMenuView.LayoutParams lp = (ActionMenuView.LayoutParams) child.getLayoutParams();
            if (lp.isOverflowButton) {
                overflowView = child;
                break;
            }
        }
        if (!(overflowView instanceof ImageView)) return;
        ((ImageView) overflowView).setColorFilter(itemColor, Mode.SRC_ATOP);
    }

    public static void setActionBarMenuItemsColor(Toolbar toolbar, int itemColor) {
        if (toolbar == null) return;
        if (toolbar instanceof TwidereToolbar) {
            ((TwidereToolbar) toolbar).setItemColor(itemColor);
        }
        final ActionMenuView actionMenuView = ViewSupport.findViewByType(toolbar, ActionMenuView.class);
        if (actionMenuView == null) return;
        for (int i = 0, j = actionMenuView.getChildCount(); i < j; i++) {
            final View child = actionMenuView.getChildAt(i);
            if (child instanceof ActionMenuView.ActionMenuChildView && child instanceof ImageView) {
                ((ImageView) child).setColorFilter(itemColor, Mode.SRC_ATOP);
            }
        }
    }

    public static void setActionBarOverflowColor(ActionMenuPresenter presenter, int itemColor) {
        if (presenter == null) return;
        final View view = (View) Utils.findFieldOfTypes(presenter, ActionMenuPresenter.class,
                ActionMenuView.ActionMenuChildView.class, View.class);
        if (!(view instanceof ImageView)) return;
        ((ImageView) view).setColorFilter(itemColor, Mode.SRC_ATOP);
    }

    public static void setActionBarOverflowColor(ActionMenuView actionMenuView, int itemColor) {
        if (actionMenuView == null) return;
        View overflowView = null;
        for (int i = 0, j = actionMenuView.getChildCount(); i < j; i++) {
            final View child = actionMenuView.getChildAt(i);
            final ActionMenuView.LayoutParams lp = (ActionMenuView.LayoutParams) child.getLayoutParams();
            if (lp.isOverflowButton) {
                overflowView = child;
                break;
            }
        }
        if (!(overflowView instanceof ImageView)) return;
        final Drawable drawable = ((ImageView) overflowView).getDrawable();
        if (drawable == null) return;
        drawable.setColorFilter(itemColor, Mode.SRC_ATOP);
    }

    public static void setActionBarSubtitleTextColor(Window window, int itemColor) {
        final View actionBarView = window.findViewById(android.support.v7.appcompat.R.id.action_bar);
        if (actionBarView instanceof Toolbar) {
            ((Toolbar) actionBarView).setSubtitleTextColor(itemColor);
        }
    }

    public static void setActionBarTitleTextColor(Window window, int titleColor) {
        final View actionBarView = window.findViewById(android.support.v7.appcompat.R.id.action_bar);
        if (actionBarView instanceof Toolbar) {
            ((Toolbar) actionBarView).setTitleTextColor(titleColor);
        }
    }

    public static void setCompatToolbarOverlay(Activity activity, Drawable overlay) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) return;
        final Window window = activity.getWindow();
        final View view = window.findViewById(android.support.v7.appcompat.R.id.decor_content_parent);
        if (!(view instanceof ActionBarOverlayLayout)) return;
        try {
            final Field field = ActionBarOverlayLayout.class.getDeclaredField("mWindowContentOverlay");
            field.setAccessible(true);
            field.set(view, overlay);
        } catch (Exception ignore) {
        }
    }

    public static void setCompatContentViewOverlay(Activity activity, Drawable overlay) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) return;
        final Window window = activity.getWindow();
		View contentLayout = window.findViewById(android.support.v7.appcompat.R.id.action_bar_activity_content);
		if (contentLayout == null) {
			contentLayout = window.findViewById(android.R.id.content);
		}
		if (contentLayout instanceof ContentFrameLayout) {
			((ContentFrameLayout) contentLayout).setForeground(overlay);
		}
    }

    public static void setWindowOverlayViewOverlay(Activity activity, Drawable overlay) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) return;
        final Window window = activity.getWindow();
        final View windowOverlay = window.findViewById(R.id.window_overlay);
        if (windowOverlay == null) {
            return;
        }
        ViewSupport.setBackground(windowOverlay, overlay);
    }

    public static void setupDrawerBackground(Context context, View view) {
        if (!(context instanceof IThemedActivity)) return;
        final int themeRes = ((IThemedActivity) context).getCurrentThemeResourceId();
        final int drawerThemeRes = getDrawerThemeResource(themeRes);
        final String backgroundOption = ((IThemedActivity) context).getThemeBackgroundOption();
        final int alpha = ((IThemedActivity) context).getCurrentThemeBackgroundAlpha();
        final Drawable d = getWindowBackgroundFromTheme(context, drawerThemeRes);
        if (d != null && isTransparentBackground(backgroundOption)) {
            d.setAlpha(alpha);
        }
        ViewSupport.setBackground(view, d);
    }

    public static void wrapMenuIcon(@NonNull Menu menu, int itemColor, int subItemColor, int... excludeGroups) {
        for (int i = 0, j = menu.size(); i < j; i++) {
            final MenuItem item = menu.getItem(i);
            wrapMenuItemIcon(item, itemColor, excludeGroups);
            if (item.hasSubMenu()) {
                wrapMenuIcon(item.getSubMenu(), subItemColor, subItemColor, excludeGroups);
            }
        }
    }

    public static void wrapMenuIcon(ActionMenuView view, int... excludeGroups) {
        final Resources resources = view.getResources();
        final int colorDark = resources.getColor(R.color.action_icon_dark);
        final int colorLight = resources.getColor(R.color.action_icon_light);
        wrapMenuIcon(view, colorDark, colorLight, excludeGroups);
    }

    public static void wrapMenuIcon(ActionMenuView view, int colorDark, int colorLight, int... excludeGroups) {
        final int itemBackgroundColor = ThemeUtils.getThemeBackgroundColor(view.getContext());
        final int popupItemBackgroundColor = ThemeUtils.getThemeBackgroundColor(view.getContext(), view.getPopupTheme());
        final int itemColor = TwidereColorUtils.getContrastYIQ(itemBackgroundColor, colorDark, colorLight);
        final int popupItemColor = TwidereColorUtils.getContrastYIQ(popupItemBackgroundColor, colorDark, colorLight);
        final Menu menu = view.getMenu();
        final int childCount = view.getChildCount();
        for (int i = 0, j = menu.size(), k = 0; i < j; i++) {
            final MenuItem item = menu.getItem(i);
            wrapMenuItemIcon(item, itemColor, excludeGroups);
            if (item.hasSubMenu()) {
                wrapMenuIcon(item.getSubMenu(), popupItemColor, popupItemColor, excludeGroups);
            }
            if (item.isVisible()) {
                k++;
            }
        }
    }

    public static void wrapMenuIcon(Context context, Menu menu, int... excludeGroups) {
        final int backgroundColor = ThemeUtils.getThemeBackgroundColor(context);
        wrapMenuIcon(context, backgroundColor, backgroundColor, menu, excludeGroups);
    }

    public static void wrapMenuIcon(Context context, int backgroundColor, int popupBackgroundColor,
                                    Menu menu, int... excludeGroups) {
        final Resources resources = context.getResources();
        final int colorDark = resources.getColor(R.color.action_icon_dark);
        final int colorLight = resources.getColor(R.color.action_icon_light);
        final int itemColor = TwidereColorUtils.getContrastYIQ(backgroundColor, colorDark, colorLight);
        final int popupItemColor = TwidereColorUtils.getContrastYIQ(popupBackgroundColor, colorDark, colorLight);
        for (int i = 0, j = menu.size(), k = 0; i < j; i++) {
            final MenuItem item = menu.getItem(i);
            wrapMenuItemIcon(item, itemColor, excludeGroups);
            if (item.hasSubMenu()) {
                wrapMenuIcon(menu, popupItemColor, popupItemColor, excludeGroups);
            }
            if (item.isVisible()) {
                k++;
            }
        }
    }

    public static void wrapMenuItemIcon(@NonNull MenuItem item, int itemColor, int... excludeGroups) {
        if (ArrayUtils.contains(excludeGroups, item.getGroupId())) return;
        final Drawable icon = item.getIcon();
        if (icon == null) return;
        if (icon instanceof ActionIconDrawable) {
            ((ActionIconDrawable) icon).setDefaultColor(itemColor);
            item.setIcon(icon);
            return;
        }
        icon.mutate();
        final Drawable.Callback callback = icon.getCallback();
        final ActionIconDrawable newIcon = new ActionIconDrawable(icon, itemColor);
        newIcon.setCallback(callback);
        item.setIcon(newIcon);
    }

    public static void wrapToolbarMenuIcon(@Nullable ActionMenuView view, int itemColor, int popupItemColor, int... excludeGroups) {
        if (view == null) return;
        final Menu menu = view.getMenu();
        final int childCount = view.getChildCount();
        for (int i = 0, j = menu.size(), k = 0; i < j; i++) {
            final MenuItem item = menu.getItem(i);
            wrapMenuItemIcon(item, itemColor, excludeGroups);
            if (item.hasSubMenu()) {
                wrapMenuIcon(item.getSubMenu(), popupItemColor, popupItemColor, excludeGroups);
            }
            if (item.isVisible()) {
                k++;
            }
        }
    }

    @NonNull
    private static SharedPreferencesWrapper getSharedPreferencesWrapper(Context context) {
        final Context appContext = context.getApplicationContext();
        return SharedPreferencesWrapper.getInstance(appContext, SHARED_PREFERENCES_NAME,
                Context.MODE_PRIVATE);
    }

    public static int getActionBarHeight(Context context) {
        final TypedValue tv = new TypedValue();
        final Resources.Theme theme = context.getTheme();
        final int attr = R.attr.actionBarSize;
        if (theme.resolveAttribute(attr, tv, true)) {
            return TypedValue.complexToDimensionPixelSize(tv.data, context.getResources().getDisplayMetrics());
        }
        return 0;
    }

    @Nullable
    public static Toolbar getToolbarFromActivity(Activity activity) {
        if (activity instanceof LinkHandlerActivity)
            return ((LinkHandlerActivity) activity).peekActionBarToolbar();
        return null;
    }

    public static Context getActionBarThemedContext(Context base) {
        final TypedValue outValue = new TypedValue();
        final Resources.Theme baseTheme = base.getTheme();
        baseTheme.resolveAttribute(android.support.v7.appcompat.R.attr.actionBarTheme, outValue, true);

        if (outValue.resourceId != 0) {
            final Resources.Theme actionBarTheme = base.getResources().newTheme();
            actionBarTheme.setTo(baseTheme);
            actionBarTheme.applyStyle(outValue.resourceId, true);

            final Context actionBarContext = new android.support.v7.internal.view.ContextThemeWrapper(base, 0);
            actionBarContext.getTheme().setTo(actionBarTheme);
            return actionBarContext;
        } else {
            return base;
        }
    }

    public static Context getActionBarThemedContext(Context base, int themeId, int actionBarColor) {
        final int actionBarThemeId;
        if (isDarkTheme(themeId) || TwidereColorUtils.getYIQLuminance(actionBarColor) <= ACCENT_COLOR_THRESHOLD) {
            actionBarThemeId = R.style.Theme_Twidere_Dark;
        } else {
            actionBarThemeId = R.style.Theme_Twidere_Light;
        }
        final Resources.Theme baseTheme = base.getTheme();
        final Resources.Theme actionBarTheme = base.getResources().newTheme();
        actionBarTheme.setTo(baseTheme);
        actionBarTheme.applyStyle(actionBarThemeId, true);

        final Context actionBarContext = new android.support.v7.internal.view.ContextThemeWrapper(base, 0);
        actionBarContext.getTheme().setTo(actionBarTheme);
        return actionBarContext;
    }
}