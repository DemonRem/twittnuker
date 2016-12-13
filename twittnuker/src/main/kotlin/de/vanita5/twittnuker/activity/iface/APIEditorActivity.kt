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

package de.vanita5.twittnuker.activity.iface

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.LoaderManager
import android.support.v4.content.AsyncTaskLoader
import android.support.v4.content.Loader
import android.support.v7.app.AlertDialog
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.RadioGroup
import android.widget.RadioGroup.OnCheckedChangeListener
import android.widget.TextView
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_api_editor.*
import kotlinx.android.synthetic.main.layout_api_editor.*
import kotlinx.android.synthetic.main.layout_api_editor_advanced_fields.*
import org.mariotaku.restfu.annotation.method.GET
import org.mariotaku.restfu.http.HttpRequest
import org.mariotaku.restfu.http.HttpResponse
import org.mariotaku.restfu.http.RestHttpClient
import de.vanita5.twittnuker.BuildConfig
import de.vanita5.twittnuker.R
import de.vanita5.twittnuker.activity.BaseActivity
import de.vanita5.twittnuker.adapter.ArrayAdapter
import de.vanita5.twittnuker.constant.IntentConstants.EXTRA_API_CONFIG
import de.vanita5.twittnuker.constant.defaultAPIConfigKey
import de.vanita5.twittnuker.fragment.BaseDialogFragment
import de.vanita5.twittnuker.model.CustomAPIConfig
import de.vanita5.twittnuker.model.account.cred.Credentials
import de.vanita5.twittnuker.util.JsonSerializer
import de.vanita5.twittnuker.util.MicroBlogAPIFactory
import de.vanita5.twittnuker.util.Utils
import de.vanita5.twittnuker.util.dagger.GeneralComponentHelper

import java.io.IOException
import javax.inject.Inject

class APIEditorActivity : BaseActivity(), OnCheckedChangeListener, OnClickListener, CompoundButton.OnCheckedChangeListener {
    private var editNoVersionSuffixChanged: Boolean = false

    override fun onCheckedChanged(group: RadioGroup, checkedId: Int) {
        val authType = getCheckedAuthType(checkedId)
        val isOAuth = authType == Credentials.Type.OAUTH || authType == Credentials.Type.XAUTH
        editSameOAuthSigningUrl.visibility = if (isOAuth) View.VISIBLE else View.GONE
        editConsumerKey.visibility = if (isOAuth) View.VISIBLE else View.GONE
        editConsumerSecret.visibility = if (isOAuth) View.VISIBLE else View.GONE
        if (!editNoVersionSuffixChanged) {
            editNoVersionSuffix.isChecked = authType == Credentials.Type.EMPTY
        }
    }

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        editNoVersionSuffixChanged = true
    }

    override fun onClick(v: View) {
        when (v) {
            save -> {
                if (checkApiUrl()) {
                    saveAndFinish()
                } else {
                    editApiUrlFormat.error = getString(R.string.wrong_url_format)
                }
            }
            apiUrlFormatHelp -> {
                Toast.makeText(this, R.string.api_url_format_help, Toast.LENGTH_LONG).show()
            }
            loadDefaults -> {
                val df = LoadDefaultsChooserDialogFragment()
                df.show(supportFragmentManager, "load_defaults")
            }
        }
    }

    private fun checkApiUrl(): Boolean {
        return MicroBlogAPIFactory.verifyApiFormat(editApiUrlFormat.text.toString())
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable(EXTRA_API_CONFIG, createCustomAPIConfig())
        super.onSaveInstanceState(outState)
    }

    fun saveAndFinish() {
        val intent = Intent()
        intent.putExtra(EXTRA_API_CONFIG, createCustomAPIConfig())
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    private fun createCustomAPIConfig(): CustomAPIConfig {
        return CustomAPIConfig().apply {
            this.apiUrlFormat = editApiUrlFormat.text.toString()
            this.credentialsType = getCheckedAuthType(editAuthType.checkedRadioButtonId)
            this.isSameOAuthUrl = editSameOAuthSigningUrl.isChecked
            this.isNoVersionSuffix = editNoVersionSuffix.isChecked
            this.consumerKey = editConsumerKey.text.toString()
            this.consumerSecret = editConsumerSecret.text.toString()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_api_editor)

        val apiConfig: CustomAPIConfig
        if (savedInstanceState != null) {
            apiConfig = savedInstanceState.getParcelable(EXTRA_API_CONFIG)
        } else {
            apiConfig = intent.getParcelableExtra(EXTRA_API_CONFIG) ?: kPreferences[defaultAPIConfigKey]
        }

        editAuthType.setOnCheckedChangeListener(this)
        editNoVersionSuffix.setOnCheckedChangeListener(this)
        save.setOnClickListener(this)
        apiUrlFormatHelp.setOnClickListener(this)

        loadDefaults.visibility = View.VISIBLE
        loadDefaults.setOnClickListener(this)

        editApiUrlFormat.setText(apiConfig.apiUrlFormat)
        editSameOAuthSigningUrl.isChecked = apiConfig.isSameOAuthUrl
        editNoVersionSuffix.isChecked = apiConfig.isNoVersionSuffix
        editConsumerKey.setText(apiConfig.consumerKey)
        editConsumerSecret.setText(apiConfig.consumerSecret)

        editAuthType.check(getAuthTypeId(apiConfig.credentialsType))
        if (editAuthType.checkedRadioButtonId == -1) {
            oauth.isChecked = true
        }
    }


    private fun setAPIConfig(apiConfig: CustomAPIConfig) {
        editApiUrlFormat.setText(apiConfig.apiUrlFormat)
        editAuthType.check(getAuthTypeId(apiConfig.credentialsType))
        editSameOAuthSigningUrl.isChecked = apiConfig.isSameOAuthUrl
        editNoVersionSuffix.isChecked = apiConfig.isNoVersionSuffix
        editConsumerKey.setText(apiConfig.consumerKey)
        editConsumerSecret.setText(apiConfig.consumerSecret)
    }

    class LoadDefaultsChooserDialogFragment : BaseDialogFragment(), DialogInterface.OnClickListener,
            LoaderManager.LoaderCallbacks<List<CustomAPIConfig>?> {
        private lateinit var adapter: ArrayAdapter<CustomAPIConfig>

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val context = context
            val configs = CustomAPIConfig.listDefault(context)
            adapter = CustomAPIConfigArrayAdapter(context, configs)
            val builder = AlertDialog.Builder(context)
            builder.setAdapter(adapter, this)
            if (!BuildConfig.DEBUG) {
                loaderManager.initLoader(0, null, this)
            }
            return builder.create()
        }

        override fun onClick(dialog: DialogInterface, which: Int) {
            (activity as APIEditorActivity).setAPIConfig(adapter.getItem(which))
            dismiss()
        }

        override fun onCreateLoader(id: Int, args: Bundle?): Loader<List<CustomAPIConfig>?> {
            return DefaultAPIConfigLoader(context)
        }

        override fun onLoadFinished(loader: Loader<List<CustomAPIConfig>?>, data: List<CustomAPIConfig>?) {
            if (data != null) {
                adapter.clear()
                adapter.addAll(data)
            }
        }

        override fun onLoaderReset(loader: Loader<List<CustomAPIConfig>?>) {

        }

        class DefaultAPIConfigLoader(context: Context) : AsyncTaskLoader<List<CustomAPIConfig>?>(context) {
            @Inject
            lateinit var client: RestHttpClient

            init {
                GeneralComponentHelper.build(context).inject(this)
            }

            override fun loadInBackground(): List<CustomAPIConfig>? {
                val request = HttpRequest(GET.METHOD, DEFAULT_API_CONFIGS_URL,
                        null, null, null)
                var response: HttpResponse? = null
                try {
                    response = client.newCall(request).execute()
                    if (response!!.isSuccessful) {
                        val `is` = response.body.stream()
                        return JsonSerializer.parseList(`is`, CustomAPIConfig::class.java)
                    }
                } catch (e: IOException) {
                    // Ignore
                } finally {
                    Utils.closeSilently(response)
                }
                return null
            }

            override fun onStartLoading() {
                forceLoad()
            }

            companion object {
                val DEFAULT_API_CONFIGS_URL = "https://raw.githubusercontent.com/vanita5/twittnuker/master/twittnuker/src/main/assets/data/default_api_configs.json"
            }
        }

        private inner class CustomAPIConfigArrayAdapter(context: Context, defaultItems: List<CustomAPIConfig>) : ArrayAdapter<CustomAPIConfig>(context, android.R.layout.simple_list_item_1, defaultItems) {

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val config = getItem(position)
                (view.findViewById(android.R.id.text1) as TextView).text = config.getLocalizedName(context)
                return view
            }
        }
    }

    companion object {

        @Credentials.Type
        fun getCheckedAuthType(checkedId: Int): String {
            when (checkedId) {
                R.id.xauth -> {
                    return Credentials.Type.XAUTH
                }
                R.id.basic -> {
                    return Credentials.Type.BASIC
                }
                R.id.twipO -> {
                    return Credentials.Type.EMPTY
                }
                else -> {
                    return Credentials.Type.OAUTH
                }
            }
        }

        fun getAuthTypeId(authType: String): Int {
            when (authType) {
                Credentials.Type.XAUTH -> {
                    return R.id.xauth
                }
                Credentials.Type.BASIC -> {
                    return R.id.basic
                }
                Credentials.Type.EMPTY -> {
                    return R.id.twipO
                }
                else -> {
                    return R.id.oauth
                }
            }
        }
    }
}