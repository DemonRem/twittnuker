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

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.Toast;

import de.vanita5.twittnuker.R;
import de.vanita5.twittnuker.fragment.BaseSupportDialogFragment;
import de.vanita5.twittnuker.model.CustomAPIConfig;
import de.vanita5.twittnuker.model.ParcelableCredentials;
import de.vanita5.twittnuker.provider.TwidereDataStore.Accounts;
import de.vanita5.twittnuker.util.ParseUtils;
import de.vanita5.twittnuker.util.TwitterAPIFactory;

import static de.vanita5.twittnuker.util.Utils.getNonEmptyString;
import static de.vanita5.twittnuker.util.Utils.trim;

public class APIEditorActivity extends BaseActivity implements OnCheckedChangeListener,
        OnClickListener, CompoundButton.OnCheckedChangeListener {

    private EditText mEditAPIUrlFormat;
    private CheckBox mEditSameOAuthSigningUrl, mEditNoVersionSuffix;
    private EditText mEditConsumerKey, mEditConsumerSecret;
    private RadioGroup mEditAuthType;
    private RadioButton mButtonOAuth, mButtonXAuth, mButtonBasic, mButtonTWIPOMode;
    private Button mSaveButton;
    private Button mLoadDefaultsButton;
    private View mAPIFormatHelpButton;
    private boolean mEditNoVersionSuffixChanged;

    public static int getCheckedAuthType(final int checkedId) {
        switch (checkedId) {
            case R.id.xauth: {
                return ParcelableCredentials.AUTH_TYPE_XAUTH;
            }
            case R.id.basic: {
                return ParcelableCredentials.AUTH_TYPE_BASIC;
            }
            case R.id.twip_o: {
                return ParcelableCredentials.AUTH_TYPE_TWIP_O_MODE;
            }
            default: {
                return ParcelableCredentials.AUTH_TYPE_OAUTH;
            }
        }
    }

    @Override
    public void onCheckedChanged(final RadioGroup group, final int checkedId) {
        final int authType = getCheckedAuthType(checkedId);
        final boolean isOAuth = authType == ParcelableCredentials.AUTH_TYPE_OAUTH || authType == ParcelableCredentials.AUTH_TYPE_XAUTH;
        mEditSameOAuthSigningUrl.setVisibility(isOAuth ? View.VISIBLE : View.GONE);
        mEditConsumerKey.setVisibility(isOAuth ? View.VISIBLE : View.GONE);
        mEditConsumerSecret.setVisibility(isOAuth ? View.VISIBLE : View.GONE);
        if (!mEditNoVersionSuffixChanged) {
            mEditNoVersionSuffix.setChecked(authType == ParcelableCredentials.AUTH_TYPE_TWIP_O_MODE);
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        mEditNoVersionSuffixChanged = true;
    }

    @Override
    public void onClick(final View v) {
        switch (v.getId()) {
            case R.id.save: {
                if (checkApiUrl()) {
                    saveAndFinish();
                } else {
                    mEditAPIUrlFormat.setError(getString(R.string.wrong_url_format));
                }
                break;
            }
            case R.id.api_url_format_help: {
                Toast.makeText(this, R.string.api_url_format_help, Toast.LENGTH_LONG).show();
                break;
            }
            case R.id.load_defaults: {
                final LoadDefaultsChooserDialogFragment df = new LoadDefaultsChooserDialogFragment();
                df.show(getSupportFragmentManager(), "load_defaults");
                break;
            }
        }
    }

    private boolean checkApiUrl() {
        return TwitterAPIFactory.verifyApiFormat(String.valueOf(mEditAPIUrlFormat.getText()));
    }

    @Override
    public void onContentChanged() {
        super.onContentChanged();
        mLoadDefaultsButton = (Button) findViewById(R.id.load_defaults);
        mEditAPIUrlFormat = (EditText) findViewById(R.id.api_url_format);
        mEditAuthType = (RadioGroup) findViewById(R.id.auth_type);
        mButtonOAuth = (RadioButton) findViewById(R.id.oauth);
        mButtonXAuth = (RadioButton) findViewById(R.id.xauth);
        mButtonBasic = (RadioButton) findViewById(R.id.basic);
        mButtonTWIPOMode = (RadioButton) findViewById(R.id.twip_o);
        mEditSameOAuthSigningUrl = (CheckBox) findViewById(R.id.same_oauth_signing_url);
        mEditNoVersionSuffix = (CheckBox) findViewById(R.id.no_version_suffix);
        mEditConsumerKey = (EditText) findViewById(R.id.consumer_key);
        mEditConsumerSecret = (EditText) findViewById(R.id.consumer_secret);
        mSaveButton = (Button) findViewById(R.id.save);
        mAPIFormatHelpButton = findViewById(R.id.api_url_format_help);
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        final String apiUrlFormat = ParseUtils.parseString(mEditAPIUrlFormat.getText());
        final int authType = getCheckedAuthType(mEditAuthType.getCheckedRadioButtonId());
        final boolean sameOAuthSigningUrl = mEditSameOAuthSigningUrl.isChecked();
        final boolean noVersionSuffix = mEditNoVersionSuffix.isChecked();
        final String consumerKey = ParseUtils.parseString(mEditConsumerKey.getText());
        final String consumerSecret = ParseUtils.parseString(mEditConsumerSecret.getText());
        outState.putString(Accounts.API_URL_FORMAT, apiUrlFormat);
        outState.putInt(Accounts.AUTH_TYPE, authType);
        outState.putBoolean(Accounts.SAME_OAUTH_SIGNING_URL, sameOAuthSigningUrl);
        outState.putBoolean(Accounts.NO_VERSION_SUFFIX, noVersionSuffix);
        outState.putString(Accounts.CONSUMER_KEY, consumerKey);
        outState.putString(Accounts.CONSUMER_SECRET, consumerSecret);
        super.onSaveInstanceState(outState);
    }

    public void saveAndFinish() {
        final String apiUrlFormat = ParseUtils.parseString(mEditAPIUrlFormat.getText());
        final int authType = getCheckedAuthType(mEditAuthType.getCheckedRadioButtonId());
        final boolean sameOAuthSigningUrl = mEditSameOAuthSigningUrl.isChecked();
        final boolean noVersionSuffix = mEditNoVersionSuffix.isChecked();
        final String consumerKey = ParseUtils.parseString(mEditConsumerKey.getText());
        final String consumerSecret = ParseUtils.parseString(mEditConsumerSecret.getText());
        final Intent intent = new Intent();
        intent.putExtra(Accounts.API_URL_FORMAT, apiUrlFormat);
        intent.putExtra(Accounts.AUTH_TYPE, authType);
        intent.putExtra(Accounts.SAME_OAUTH_SIGNING_URL, sameOAuthSigningUrl);
        intent.putExtra(Accounts.NO_VERSION_SUFFIX, noVersionSuffix);
        intent.putExtra(Accounts.CONSUMER_KEY, consumerKey);
        intent.putExtra(Accounts.CONSUMER_SECRET, consumerSecret);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();
        final Bundle extras = intent.getExtras();

        setContentView(R.layout.activity_api_editor);

        String apiUrlFormat;
        int authType;
        boolean sameOAuthSigningUrl, noVersionSuffix;
        String consumerKey, consumerSecret;

        final SharedPreferences pref = getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        final String prefApiUrlFormat = getNonEmptyString(pref, KEY_API_URL_FORMAT, DEFAULT_TWITTER_API_URL_FORMAT);
        final int prefAuthType = pref.getInt(KEY_AUTH_TYPE, ParcelableCredentials.AUTH_TYPE_OAUTH);
        final boolean prefSameOAuthSigningUrl = pref.getBoolean(KEY_SAME_OAUTH_SIGNING_URL, false);
        final boolean prefNoVersionSuffix = pref.getBoolean(KEY_NO_VERSION_SUFFIX, false);
        final String prefConsumerKey = getNonEmptyString(pref, KEY_CONSUMER_KEY, TWITTER_CONSUMER_KEY);
        final String prefConsumerSecret = getNonEmptyString(pref, KEY_CONSUMER_SECRET, TWITTER_CONSUMER_SECRET);
        final Bundle bundle;
        if (savedInstanceState != null) {
            bundle = savedInstanceState;
        } else if (extras != null) {
            bundle = extras;
        } else {
            bundle = new Bundle();
        }
        apiUrlFormat = trim(bundle.getString(Accounts.API_URL_FORMAT, prefApiUrlFormat));
        authType = bundle.getInt(Accounts.AUTH_TYPE, prefAuthType);
        sameOAuthSigningUrl = bundle.getBoolean(Accounts.SAME_OAUTH_SIGNING_URL, prefSameOAuthSigningUrl);
        noVersionSuffix = bundle.getBoolean(Accounts.NO_VERSION_SUFFIX, prefNoVersionSuffix);
        consumerKey = trim(bundle.getString(Accounts.CONSUMER_KEY, prefConsumerKey));
        consumerSecret = trim(bundle.getString(Accounts.CONSUMER_SECRET, prefConsumerSecret));

        mEditAuthType.setOnCheckedChangeListener(this);
        mEditNoVersionSuffix.setOnCheckedChangeListener(this);
        mSaveButton.setOnClickListener(this);
        mAPIFormatHelpButton.setOnClickListener(this);

        mLoadDefaultsButton.setVisibility(View.VISIBLE);
        mLoadDefaultsButton.setOnClickListener(this);

        mEditAPIUrlFormat.setText(apiUrlFormat);
        mEditSameOAuthSigningUrl.setChecked(sameOAuthSigningUrl);
        mEditNoVersionSuffix.setChecked(noVersionSuffix);
        mEditConsumerKey.setText(consumerKey);
        mEditConsumerSecret.setText(consumerSecret);

        mButtonOAuth.setChecked(authType == ParcelableCredentials.AUTH_TYPE_OAUTH);
        mButtonXAuth.setChecked(authType == ParcelableCredentials.AUTH_TYPE_XAUTH);
        mButtonBasic.setChecked(authType == ParcelableCredentials.AUTH_TYPE_BASIC);
        mButtonTWIPOMode.setChecked(authType == ParcelableCredentials.AUTH_TYPE_TWIP_O_MODE);
        if (mEditAuthType.getCheckedRadioButtonId() == -1) {
            mButtonOAuth.setChecked(true);
        }
    }


    private int getAuthTypeId(final int authType) {
        switch (authType) {
            case ParcelableCredentials.AUTH_TYPE_XAUTH: {
                return R.id.xauth;
            }
            case ParcelableCredentials.AUTH_TYPE_BASIC: {
                return R.id.basic;
            }
            case ParcelableCredentials.AUTH_TYPE_TWIP_O_MODE: {
                return R.id.twip_o;
            }
            default: {
                return R.id.oauth;
            }
        }
    }

    private void setAPIConfig(CustomAPIConfig apiConfig) {
        mEditAPIUrlFormat.setText(apiConfig.getApiUrlFormat());
        mEditAuthType.check(getAuthTypeId(apiConfig.getAuthType()));
        mEditSameOAuthSigningUrl.setChecked(apiConfig.isSameOAuthUrl());
        mEditNoVersionSuffix.setChecked(apiConfig.isNoVersionSuffix());
        mEditConsumerKey.setText(apiConfig.getConsumerKey());
        mEditConsumerSecret.setText(apiConfig.getConsumerSecret());
    }

    public static class LoadDefaultsChooserDialogFragment extends BaseSupportDialogFragment
            implements DialogInterface.OnClickListener {
        private CustomAPIConfig[] mAPIConfigs;

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            mAPIConfigs = CustomAPIConfig.listDefault(getContext());
            final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            String[] entries = new String[mAPIConfigs.length];
            for (int i = 0, mAPIConfigsLength = mAPIConfigs.length; i < mAPIConfigsLength; i++) {
                entries[i] = mAPIConfigs[i].getName();
            }
            builder.setItems(entries, this);
            return builder.create();
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            ((APIEditorActivity) getActivity()).setAPIConfig(mAPIConfigs[which]);
        }
    }
}