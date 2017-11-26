/*
 * Twittnuker - Twitter client for Android
 *
 * Copyright (C) 2013-2017 vanita5 <mail@vanit.as>
 *
 * This program incorporates a modified version of Twidere.
 * Copyright (C) 2012-2017 Mariotaku Lee <mariotaku.lee@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.vanita5.twittnuker.fragment.filter

import android.accounts.AccountManager
import android.app.Dialog
import android.content.DialogInterface
import android.net.Uri
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.support.v7.app.AlertDialog
import android.view.View
import android.view.WindowManager
import android.widget.CheckBox
import android.widget.Toast
import kotlinx.android.synthetic.main.dialog_filter_rule_editor.*
import org.mariotaku.ktextension.ContentValues
import org.mariotaku.ktextension.contains
import org.mariotaku.ktextension.set
import org.mariotaku.ktextension.string
import org.mariotaku.sqliteqb.library.Expression
import de.vanita5.twittnuker.R
import de.vanita5.twittnuker.adapter.ComposeAutoCompleteAdapter
import de.vanita5.twittnuker.adapter.SourceAutoCompleteAdapter
import de.vanita5.twittnuker.annotation.FilterScope
import de.vanita5.twittnuker.constant.IntentConstants.*
import de.vanita5.twittnuker.extension.applyOnShow
import de.vanita5.twittnuker.extension.applyTheme
import de.vanita5.twittnuker.extension.queryLong
import de.vanita5.twittnuker.extension.setVisible
import de.vanita5.twittnuker.fragment.BaseDialogFragment
import de.vanita5.twittnuker.model.util.AccountUtils
import de.vanita5.twittnuker.provider.TwidereDataStore.Filters

class AddEditItemFragment : BaseDialogFragment(), DialogInterface.OnClickListener {

    private val contentUri: Uri
        get() = arguments.getParcelable(EXTRA_URI)

    private val rowId: Long
        get() = arguments.getLong(EXTRA_ID, -1)

    private val defaultValue: String?
        get() = arguments.getString(EXTRA_VALUE)

    private val defaultScope: FilterScopes
        get() = FilterScopes(filterMasks, arguments.getInt(EXTRA_SCOPE, FilterScope.DEFAULT))

    private val filterMasks: Int
        get() = when (contentUri) {
            Filters.Users.CONTENT_URI -> FilterScope.VALID_MASKS_USERS
            Filters.Keywords.CONTENT_URI -> FilterScope.VALID_MASKS_KEYWORDS
            Filters.Sources.CONTENT_URI -> FilterScope.VALID_MASKS_SOURCES
            Filters.Links.CONTENT_URI -> FilterScope.VALID_MASKS_LINKS
            else -> 0
        }

    private var Dialog.value: String?
        get() = editText.string?.takeIf(String::isNotEmpty)
        set(value) {
            editText.string = value
        }

    private var Dialog.scope: FilterScopes?
        get() = defaultScope.also { saveScopes(it) }
        set(value) {
            loadScopes(value ?: defaultScope)
        }

    private var Dialog.advancedExpanded: Boolean
        get() = advancedContainer.visibility == View.VISIBLE
        set(value) {
            advancedContainer.setVisible(value)
            advancedCollapseIndicator.rotation = if (value) 90f else 0f
        }

    override fun onClick(dialog: DialogInterface, which: Int) {
        dialog as AlertDialog
        when (which) {
            DialogInterface.BUTTON_POSITIVE -> {
                val value = dialog.value ?: return
                val scope = dialog.scope ?: return

                val values = ContentValues {
                    this[Filters.VALUE] = value
                    this[Filters.SCOPE] = scope.value
                }
                val uri = contentUri
                val id = rowId
                val resolver = context.contentResolver
                if (id >= 0) {
                    val valueWhere = Expression.equalsArgs(Filters.VALUE).sql
                    val valueWhereArgs = arrayOf(value)
                    val matchedId = resolver.queryLong(uri, Filters._ID, valueWhere, valueWhereArgs,
                            -1)
                    if (matchedId != -1L && matchedId != id) {
                        Toast.makeText(context, R.string.message_toast_duplicate_filter_rule,
                                Toast.LENGTH_SHORT).show()
                    } else {
                        val idWhere = Expression.equals(Filters._ID, id).sql
                        resolver.update(uri, values, idWhere, null)
                    }
                } else {
                    resolver.insert(uri, values)
                }
            }
        }

    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(context)
        builder.setView(R.layout.dialog_filter_rule_editor)

        if (arguments.getLong(EXTRA_ID, -1) >= 0) {
            builder.setTitle(R.string.action_edit_filter_rule)
        } else {
            builder.setTitle(R.string.action_add_filter_rule)
        }
        builder.setPositiveButton(android.R.string.ok, this)
        builder.setNegativeButton(android.R.string.cancel, this)
        val dialog = builder.create()
        dialog.applyOnShow {
            applyTheme()
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            editText.setAdapter(when (contentUri) {
                Filters.Sources.CONTENT_URI -> SourceAutoCompleteAdapter(activity)
                Filters.Users.CONTENT_URI -> ComposeAutoCompleteAdapter(activity, requestManager).apply {
                    val am = AccountManager.get(activity)
                    account = AccountUtils.getDefaultAccountDetails(activity, am, false)
                }
                else -> null
            })
            editText.threshold = 1
            advancedToggle.setOnClickListener {
                advancedExpanded = !advancedExpanded
            }
            advancedExpanded = false

            if (savedInstanceState == null) {
                value = defaultValue
                scope = defaultScope
            } else {
                value = savedInstanceState.getString(EXTRA_VALUE)
                scope = savedInstanceState.getParcelable(EXTRA_SCOPE)
            }
        }
        return dialog
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(EXTRA_VALUE, dialog.value)
        outState.putParcelable(EXTRA_SCOPE, dialog.scope)
    }

    private fun Dialog.saveScopes(scopes: FilterScopes) {
        targetText.saveScope(scopes, FilterScope.FLAG_MATCH_TEXT)
        targetName.saveScope(scopes, FilterScope.FLAG_MATCH_NAME)
        targetDescription.saveScope(scopes, FilterScope.FLAG_MATCH_DESCRIPTION)
        scopeHome.saveScope(scopes, FilterScope.HOME)
        scopeInteractions.saveScope(scopes, FilterScope.INTERACTIONS)
        scopeMessages.saveScope(scopes, FilterScope.MESSAGES)
        scopeSearchResults.saveScope(scopes, FilterScope.SEARCH_RESULTS)
        scopeOther.saveScope(scopes, FilterScope.UGC_TIMELINE)
    }

    private fun Dialog.loadScopes(scopes: FilterScopes) {
        targetText.loadScope(scopes, FilterScope.FLAG_MATCH_TEXT)
        targetName.loadScope(scopes, FilterScope.FLAG_MATCH_NAME)
        targetDescription.loadScope(scopes, FilterScope.FLAG_MATCH_DESCRIPTION)
        scopeHome.loadScope(scopes, FilterScope.HOME)
        scopeInteractions.loadScope(scopes, FilterScope.INTERACTIONS)
        scopeMessages.loadScope(scopes, FilterScope.MESSAGES)
        scopeSearchResults.loadScope(scopes, FilterScope.SEARCH_RESULTS)
        scopeOther.loadScope(scopes, FilterScope.UGC_TIMELINE)
    }

    private fun CheckBox.saveScope(scopes: FilterScopes, scope: Int) {
        if (!isEnabled || visibility != View.VISIBLE) return
        scopes[scope] = isChecked
    }

    private fun CheckBox.loadScope(scopes: FilterScopes, scope: Int) {
        if (scope in scopes) {
            isEnabled = true
            visibility = View.VISIBLE
        } else {
            isEnabled = false
            visibility = View.GONE
            return
        }
        isChecked = scopes[scope]
    }

    class FilterScopes(val masks: Int, value: Int = 0) : Parcelable {

        var value: Int = 0
            get() = field and masks
            private set(v) {
                field = v and masks
            }

        constructor(parcel: Parcel) : this(parcel.readInt(), parcel.readInt())

        init {
            this.value = value
        }

        operator fun set(scope: Int, enabled: Boolean) {
            value = if (enabled) {
                value or scope
            } else {
                value and scope.inv()
            }
        }

        operator fun get(scope: Int): Boolean {
            return scope in value
        }

        operator fun contains(scope: Int): Boolean {
            return scope in masks
        }

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeInt(masks)
            parcel.writeInt(value)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<FilterScopes> {
            override fun createFromParcel(parcel: Parcel): FilterScopes {
                return FilterScopes(parcel)
            }

            override fun newArray(size: Int): Array<FilterScopes?> {
                return arrayOfNulls(size)
            }
        }

    }

}