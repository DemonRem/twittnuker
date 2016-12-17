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

package de.vanita5.twittnuker.preference;

import android.accounts.AccountManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.internal.widget.PreferenceImageView;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.preference.PreferenceViewHolder;
import android.support.v7.widget.SwitchCompat;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;

import de.vanita5.twittnuker.Constants;
import de.vanita5.twittnuker.R;
import de.vanita5.twittnuker.model.AccountDetails;
import de.vanita5.twittnuker.model.util.AccountUtils;
import de.vanita5.twittnuker.util.BitmapUtils;
import de.vanita5.twittnuker.util.MediaLoaderWrapper;
import de.vanita5.twittnuker.util.dagger.GeneralComponentHelper;

import javax.inject.Inject;

public abstract class AccountsListPreference extends PreferenceCategory implements Constants {

    @Nullable
    private final String mSwitchKey;
    private final boolean mSwitchDefault;

    public AccountsListPreference(final Context context) {
        this(context, null);
    }

    public AccountsListPreference(final Context context, final AttributeSet attrs) {
        this(context, attrs, R.attr.preferenceCategoryStyle);
    }

    public AccountsListPreference(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AccountsListPreference);
        mSwitchKey = a.getString(R.styleable.AccountsListPreference_switchKey);
        mSwitchDefault = a.getBoolean(R.styleable.AccountsListPreference_switchDefault, false);
        a.recycle();
    }

    public void setAccountsData(final AccountDetails[] accounts) {
        removeAll();
        for (final AccountDetails account : accounts) {
            final AccountItemPreference preference = new AccountItemPreference(getContext(), account,
                    mSwitchKey, getSwitchDefault());
            setupPreference(preference, account);
            addPreference(preference);
        }
        final Preference preference = new Preference(getContext());
        preference.setLayoutResource(R.layout.settings_layout_click_to_config);
        addPreference(preference);
    }

    @Override
    protected void onAttachedToHierarchy(@NonNull final PreferenceManager preferenceManager) {
        super.onAttachedToHierarchy(preferenceManager);
        if (getPreferenceCount() > 0) return;
        setAccountsData(AccountUtils.getAllAccountDetails(AccountManager.get(getContext()), true));
    }

    protected abstract void setupPreference(AccountItemPreference preference, AccountDetails account);

    protected boolean getSwitchDefault() {
        return mSwitchDefault;
    }

    public static final class AccountItemPreference extends Preference implements ImageLoadingListener,
            OnSharedPreferenceChangeListener {

        private final AccountDetails mAccount;
        @Nullable
        private final String mSwitchKey;
        private final boolean mSwitchDefault;
        private final SharedPreferences mSwitchPreference;

        @Inject
        MediaLoaderWrapper mImageLoader;

        public AccountItemPreference(final Context context, final AccountDetails account,
                                     @Nullable final String switchKey, final boolean switchDefault) {
            super(context);
            GeneralComponentHelper.build(context).inject(this);
            final String switchPreferenceName = ACCOUNT_PREFERENCES_NAME_PREFIX + account.key;
            mAccount = account;
            mSwitchKey = switchKey;
            mSwitchDefault = switchDefault;
            mSwitchPreference = context.getSharedPreferences(switchPreferenceName, Context.MODE_PRIVATE);
            mSwitchPreference.registerOnSharedPreferenceChangeListener(this);
            setTitle(mAccount.user.name);
            setSummary(String.format("@%s", mAccount.user.screen_name));
            mImageLoader.loadProfileImage(mAccount, this);
            setWidgetLayoutResource(R.layout.layout_preference_switch_indicator);
        }

        @Override
        public void onLoadingCancelled(final String imageUri, final View view) {
//            setIcon(R.drawable.ic_profile_image_default);
        }

        @Override
        public void onLoadingComplete(final String imageUri, final View view, final Bitmap loadedImage) {
            final Bitmap roundedBitmap = BitmapUtils.getCircleBitmap(loadedImage);
            final BitmapDrawable icon = new BitmapDrawable(getContext().getResources(), roundedBitmap);
            icon.setGravity(Gravity.FILL);
            setIcon(icon);
        }

        @Override
        public void onLoadingFailed(final String imageUri, final View view, final FailReason failReason) {
//            setIcon(R.drawable.ic_profile_image_default);
        }

        @Override
        public void onLoadingStarted(final String imageUri, final View view) {
//            setIcon(R.drawable.ic_profile_image_default);
        }

        @Override
        public void onSharedPreferenceChanged(final SharedPreferences preferences, final String key) {
            notifyChanged();
        }


        @Override
        public void onBindViewHolder(PreferenceViewHolder holder) {
            super.onBindViewHolder(holder);
            final View iconView = holder.findViewById(android.R.id.icon);
            if (iconView instanceof PreferenceImageView) {
                final PreferenceImageView imageView = (PreferenceImageView) iconView;
                final int maxSize = getContext().getResources().getDimensionPixelSize(R.dimen.element_size_normal);
                imageView.setMinimumWidth(maxSize);
                imageView.setMinimumHeight(maxSize);
                imageView.setMaxWidth(maxSize);
                imageView.setMaxHeight(maxSize);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            }
            final View titleView = holder.findViewById(android.R.id.title);
            if (titleView instanceof TextView) {
                ((TextView) titleView).setSingleLine(true);
            }
            final View summaryView = holder.findViewById(android.R.id.summary);
            if (summaryView instanceof TextView) {
                ((TextView) summaryView).setSingleLine(true);
            }
            final SwitchCompat switchView = (SwitchCompat) holder.findViewById(android.R.id.toggle);
            if (mSwitchKey != null) {
                switchView.setChecked(mSwitchPreference.getBoolean(mSwitchKey, mSwitchDefault));
                switchView.setVisibility(View.VISIBLE);
            } else {
                switchView.setVisibility(View.GONE);
            }
        }
    }

}