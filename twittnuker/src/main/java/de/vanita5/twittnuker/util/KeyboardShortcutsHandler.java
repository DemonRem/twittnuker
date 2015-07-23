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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.SparseArrayCompat;
import android.text.TextUtils;
import android.view.KeyEvent;

import de.vanita5.twittnuker.Constants;
import de.vanita5.twittnuker.R;
import de.vanita5.twittnuker.activity.support.ComposeActivity;
import de.vanita5.twittnuker.activity.support.QuickSearchBarActivity;
import de.vanita5.twittnuker.app.TwittnukerApplication;
import de.vanita5.twittnuker.constant.KeyboardShortcutConstants;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map.Entry;

public class KeyboardShortcutsHandler implements Constants, KeyboardShortcutConstants {

    private static final String KEYCODE_STRING_PREFIX = "KEYCODE_";

	private static final HashMap<String, Integer> sActionLabelMap = new HashMap<>();
    private static final SparseArrayCompat<String> sMetaNameMap = new SparseArrayCompat<>();

	static {
        sActionLabelMap.put(ACTION_COMPOSE, R.string.compose);
        sActionLabelMap.put(ACTION_SEARCH, android.R.string.search_go);
        sActionLabelMap.put(ACTION_MESSAGE, R.string.new_direct_message);
        sActionLabelMap.put(ACTION_HOME_ACCOUNTS_DASHBOARD, R.string.open_accounts_dashboard);
        sActionLabelMap.put(ACTION_STATUS_REPLY, R.string.reply);
        sActionLabelMap.put(ACTION_STATUS_RETWEET, R.string.retweet);
        sActionLabelMap.put(ACTION_STATUS_FAVORITE, R.string.favorite);
        sActionLabelMap.put(ACTION_NAVIGATION_PREVIOUS, R.string.previous_item);
        sActionLabelMap.put(ACTION_NAVIGATION_NEXT, R.string.next_item);
        sActionLabelMap.put(ACTION_NAVIGATION_TOP, R.string.jump_to_top);
        sActionLabelMap.put(ACTION_NAVIGATION_REFRESH, R.string.refresh);
        sActionLabelMap.put(ACTION_NAVIGATION_PREVIOUS_TAB, R.string.previous_tab);
        sActionLabelMap.put(ACTION_NAVIGATION_NEXT_TAB, R.string.next_tab);
        sActionLabelMap.put(ACTION_NAVIGATION_BACK, R.string.keyboard_shortcut_back);

		sMetaNameMap.put(KeyEvent.META_FUNCTION_ON, "fn");
		sMetaNameMap.put(KeyEvent.META_META_ON, "meta");
		sMetaNameMap.put(KeyEvent.META_CTRL_ON, "ctrl");
		sMetaNameMap.put(KeyEvent.META_ALT_ON, "alt");
		sMetaNameMap.put(KeyEvent.META_SHIFT_ON, "shift");
	}

	private final SharedPreferencesWrapper mPreferences;

    public KeyboardShortcutsHandler(final TwittnukerApplication context) {
        mPreferences = SharedPreferencesWrapper.getInstance(context, KEYBOARD_SHORTCUTS_PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    public String findAction(@NonNull KeyboardShortcutSpec spec) {
        return mPreferences.getString(spec.getRawKey(), null);
    }

    public KeyboardShortcutSpec findKey(String action) {
        for (Entry<String, ?> entry : mPreferences.getAll().entrySet()) {
            if (action.equals(entry.getValue())) {
                final KeyboardShortcutSpec spec = new KeyboardShortcutSpec(entry.getKey(), action);
                if (spec.isValid()) return spec;
            }
        }
        return null;
    }

	public static String getActionLabel(Context context, String action) {
		if (!sActionLabelMap.containsKey(action)) return null;
		final int labelRes = sActionLabelMap.get(action);
		return context.getString(labelRes);
	}

    @Nullable
    public String getKeyAction(final String contextTag, final int keyCode, final KeyEvent event) {
        if (!isValidForHotkey(keyCode, event)) return null;
        final String key = getKeyEventKey(contextTag, keyCode, event);
        return mPreferences.getString(key, null);
    }

	public static String getKeyEventKey(String contextTag, int keyCode, KeyEvent event) {
        if (!isValidForHotkey(keyCode, event)) return null;
		final StringBuilder keyNameBuilder = new StringBuilder();
		if (!TextUtils.isEmpty(contextTag)) {
			keyNameBuilder.append(contextTag);
            keyNameBuilder.append(".");
		}
        final int metaState = KeyEvent.normalizeMetaState(event.getMetaState());

        for (int i = 0, j = sMetaNameMap.size(); i < j; i++) {
            if ((sMetaNameMap.keyAt(i) & metaState) != 0) {
                keyNameBuilder.append(sMetaNameMap.valueAt(i));
                keyNameBuilder.append("+");
			}
		}
		final String keyCodeString = KeyEvent.keyCodeToString(keyCode);
		if (keyCodeString.startsWith(KEYCODE_STRING_PREFIX)) {
			keyNameBuilder.append(keyCodeString.substring(KEYCODE_STRING_PREFIX.length()).toLowerCase(Locale.US));
		}
		return keyNameBuilder.toString();
	}

    public static String getKeyEventKey(String contextTag, int metaState, String keyName) {
        final StringBuilder keyNameBuilder = new StringBuilder();
        if (!TextUtils.isEmpty(contextTag)) {
            keyNameBuilder.append(contextTag);
            keyNameBuilder.append(".");
        }

        for (int i = 0, j = sMetaNameMap.size(); i < j; i++) {
            if ((sMetaNameMap.keyAt(i) & metaState) != 0) {
                keyNameBuilder.append(sMetaNameMap.valueAt(i));
                keyNameBuilder.append("+");
            }
        }
        keyNameBuilder.append(keyName);
        return keyNameBuilder.toString();
    }

    public static int getKeyEventMeta(String name) {
        for (int i = 0, j = sMetaNameMap.size(); i < j; i++) {
            if (sMetaNameMap.valueAt(i).equalsIgnoreCase(name)) return sMetaNameMap.keyAt(i);
        }
        return 0;
    }

    public static KeyboardShortcutSpec getKeyboardShortcutSpec(String contextTag, int keyCode, KeyEvent event) {
        if (!isValidForHotkey(keyCode, event)) return null;
        final int metaState = KeyEvent.normalizeMetaState(event.getMetaState());
        int metaStateNormalized = 0;
        for (int i = 0, j = sMetaNameMap.size(); i < j; i++) {
            if ((sMetaNameMap.keyAt(i) & metaState) != 0) {
                metaStateNormalized |= sMetaNameMap.keyAt(i);
            }
        }
        final String keyCodeString = KeyEvent.keyCodeToString(keyCode);
        if (keyCodeString.startsWith(KEYCODE_STRING_PREFIX)) {
            final String keyName = keyCodeString.substring(KEYCODE_STRING_PREFIX.length()).toLowerCase(Locale.US);
            return new KeyboardShortcutSpec(contextTag, metaStateNormalized, keyName, null);
        }
        return null;
    }

    public boolean handleKey(final Context context, final String contextTag, final int keyCode, final KeyEvent event) {
        final String action = getKeyAction(contextTag, keyCode, event);
		if (action == null) return false;
		switch (action) {
            case ACTION_COMPOSE: {
                context.startActivity(new Intent(context, ComposeActivity.class).setAction(INTENT_ACTION_COMPOSE));
				return true;
			}
            case ACTION_SEARCH: {
                context.startActivity(new Intent(context, QuickSearchBarActivity.class).setAction(INTENT_ACTION_QUICK_SEARCH));
				return true;
			}
            case ACTION_MESSAGE: {
                Utils.openMessageConversation(context, -1, -1);
                return true;
            }
		}
		return false;
	}

	public static boolean isValidForHotkey(int keyCode, KeyEvent event) {
        // These keys must use with modifiers
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
            case KeyEvent.KEYCODE_TAB: {
                if (event.hasNoModifiers()) return false;
                break;
            }
        }
        return !isNavigationKey(keyCode) && !KeyEvent.isModifierKey(keyCode) && keyCode != KeyEvent.KEYCODE_UNKNOWN;
	}

    private static boolean isNavigationKey(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_HOME
                || keyCode == KeyEvent.KEYCODE_MENU;
    }

    public static String metaToFriendlyString(int metaState) {
        final StringBuilder keyNameBuilder = new StringBuilder();
        for (int i = 0, j = sMetaNameMap.size(); i < j; i++) {
            if ((sMetaNameMap.keyAt(i) & metaState) != 0) {
                final String value = sMetaNameMap.valueAt(i);
                keyNameBuilder.append(value.substring(0, 1).toUpperCase(Locale.US));
                keyNameBuilder.append(value.substring(1));
                keyNameBuilder.append("+");
            }
        }
        return keyNameBuilder.toString();
    }

    public void register(KeyboardShortcutSpec spec, String action) {
        unregister(action);
        mPreferences.edit().putString(spec.getRawKey(), action).apply();
    }

    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        mPreferences.registerOnSharedPreferenceChangeListener(listener);
    }

    public void reset() {
        final Editor editor = mPreferences.edit();
        editor.clear();
        editor.putString("n", ACTION_COMPOSE);
        editor.putString("m", ACTION_MESSAGE);
        editor.putString("slash", ACTION_SEARCH);
        editor.putString("home.q", ACTION_HOME_ACCOUNTS_DASHBOARD);
        editor.putString("navigation.period", ACTION_NAVIGATION_REFRESH);
        editor.putString("navigation.j", ACTION_NAVIGATION_NEXT);
        editor.putString("navigation.k", ACTION_NAVIGATION_PREVIOUS);
        editor.putString("navigation.h", ACTION_NAVIGATION_PREVIOUS_TAB);
        editor.putString("navigation.l", ACTION_NAVIGATION_NEXT_TAB);
        editor.putString("navigation.u", ACTION_NAVIGATION_TOP);
        editor.putString("status.f", ACTION_STATUS_FAVORITE);
        editor.putString("status.r", ACTION_STATUS_REPLY);
        editor.putString("status.t", ACTION_STATUS_RETWEET);
        editor.apply();
    }

    public void unregister(String action) {
        final Editor editor = mPreferences.edit();
        for (Entry<String, ?> entry : mPreferences.getAll().entrySet()) {
            if (action.equals(entry.getValue())) {
                final KeyboardShortcutSpec spec = new KeyboardShortcutSpec(entry.getKey(), action);
                if (spec.isValid()) {
                    editor.remove(spec.getRawKey());
                }
            }
        }
        editor.apply();
    }

    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        mPreferences.unregisterOnSharedPreferenceChangeListener(listener);
    }

    public interface KeyboardShortcutCallback extends KeyboardShortcutConstants {

        boolean handleKeyboardShortcutRepeat(@NonNull KeyboardShortcutsHandler handler, int keyCode, int repeatCount, @NonNull KeyEvent event);

        boolean handleKeyboardShortcutSingle(@NonNull KeyboardShortcutsHandler handler, int keyCode, @NonNull KeyEvent event);
    }

    public interface TakeAllKeyboardShortcut {

    }

    /**
     * Created by mariotaku on 15/4/11.
     */
    public static final class KeyboardShortcutSpec {

        private String action;
        private String contextTag;
        private int keyMeta;
        private String keyName;

        public KeyboardShortcutSpec(String contextTag, int keyMeta, String keyName, String action) {
            this.contextTag = contextTag;
            this.keyMeta = keyMeta;
            this.keyName = keyName;
            this.action = action;
        }

        public KeyboardShortcutSpec(String key, String action) {
            final int contextDotIdx = key.indexOf('.');
            if (contextDotIdx != -1) {
                contextTag = key.substring(0, contextDotIdx);
            }
            int idx = contextDotIdx, previousIdx = idx;
            while ((idx = key.indexOf('+', idx + 1)) != -1) {
                keyMeta |= getKeyEventMeta(key.substring(previousIdx + 1, idx));
                previousIdx = idx;
            }
            keyName = key.substring(previousIdx + 1);
            this.action = action;
        }

        public KeyboardShortcutSpec copy() {
            return new KeyboardShortcutSpec(contextTag, keyMeta, keyName, action);
        }

        public String getAction() {
            return action;
        }

        public String getContextTag() {
            return contextTag;
        }

        public void setContextTag(String contextTag) {
            this.contextTag = contextTag;
        }

        public int getKeyMeta() {
            return keyMeta;
        }

        public String getKeyName() {
            return keyName;
        }

        public String getRawKey() {
            return getKeyEventKey(contextTag, keyMeta, keyName);
        }

        public String getValueName(Context context) {
            return getActionLabel(context, action);
        }

        public boolean isValid() {
            return keyName != null;
        }

        public String toKeyString() {
            return metaToFriendlyString(keyMeta) + keyToFriendlyString(keyName);
        }

        @Override
        public String toString() {
            return "KeyboardShortcutSpec{" +
                    "action='" + action + '\'' +
                    ", contextTag='" + contextTag + '\'' +
                    ", keyMeta=" + keyMeta +
                    ", keyName='" + keyName + '\'' +
                    '}';
        }

        private static String keyToFriendlyString(String keyName) {
            if (keyName == null) return null;
            final String upperName = keyName.toUpperCase(Locale.US);
            final int keyCode = KeyEvent.keyCodeFromString(KEYCODE_STRING_PREFIX + upperName);
            if (keyCode == KeyEvent.KEYCODE_UNKNOWN) return upperName;
            if (keyCode == KeyEvent.KEYCODE_DEL) return "Backspace";
            if (keyCode == KeyEvent.KEYCODE_FORWARD_DEL) return "Delete";
            final char displayLabel = new KeyEvent(KeyEvent.ACTION_DOWN, keyCode).getDisplayLabel();
            if (displayLabel == 0) return keyName.toUpperCase(Locale.US);
            return String.valueOf(displayLabel);
        }
    }
}