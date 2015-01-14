package de.vanita5.twittnuker.preference;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.preference.PreferenceManager;
import android.util.AttributeSet;

import de.vanita5.twittnuker.R;
import de.vanita5.twittnuker.util.TwidereArrayUtils;
import de.vanita5.twittnuker.util.ParseUtils;

import java.util.Map;

public class ValueDependencySeekBarDialogPreference extends SeekBarDialogPreference implements
        OnSharedPreferenceChangeListener {

    private final String mDependencyKey, mDependencyValueDefault;
    private final String[] mDependencyValues;

    public ValueDependencySeekBarDialogPreference(final Context context) {
        this(context, null);
    }

    public ValueDependencySeekBarDialogPreference(final Context context, final AttributeSet attrs) {
        this(context, attrs, android.R.attr.dialogPreferenceStyle);
    }

    public ValueDependencySeekBarDialogPreference(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        final Resources res = context.getResources();
        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ValueDependencyPreference, defStyle, 0);
        mDependencyKey = a.getString(R.styleable.ValueDependencyPreference_dependencyKey);
        final int dependencyValueRes = a.getResourceId(R.styleable.ValueDependencyPreference_dependencyValues, 0);
        mDependencyValues = dependencyValueRes > 0 ? res.getStringArray(dependencyValueRes) : null;
        mDependencyValueDefault = a.getString(R.styleable.ValueDependencyPreference_dependencyValueDefault);
        a.recycle();
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String key) {
        if (key.equals(mDependencyKey)) {
            updateEnableState();
        }
    }

    @Override
    protected void notifyHierarchyChanged() {
        super.notifyHierarchyChanged();
        updateEnableState();
    }

    @Override
    protected void onAttachedToHierarchy(final PreferenceManager preferenceManager) {
        super.onAttachedToHierarchy(preferenceManager);
        final SharedPreferences prefs = getSharedPreferences();
        if (prefs != null) {
            prefs.registerOnSharedPreferenceChangeListener(this);
        }
        updateEnableState();
    }

    private void updateEnableState() {
        final SharedPreferences prefs = getSharedPreferences();
        if (prefs == null || mDependencyKey == null || mDependencyValues == null) return;
        final Map<String, ?> all = prefs.getAll();
        final String valueString = ParseUtils.parseString(all.get(mDependencyKey), mDependencyValueDefault);
        setEnabled(TwidereArrayUtils.contains(mDependencyValues, valueString));
    }

}