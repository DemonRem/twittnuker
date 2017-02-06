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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.vanita5.twittnuker.util

import android.app.Application
import de.vanita5.twittnuker.annotation.AccountType

abstract class Analyzer {

    protected abstract fun log(priority: Int, tag: String, msg: String)

    protected abstract fun log(event: Event)

    protected abstract fun logException(throwable: Throwable)

    protected abstract fun init(application: Application)

    interface Event {
        val name: String
            get() = "Custom Event"
        @AccountType val accountType: String?
        @AccountType val accountHost: String?
            get() = null

        fun forEachValues(action: (key: String, value: String?) -> Unit) {

        }
    }

    companion object {

        var implementation: Analyzer? = null

        fun init(application: Application) {
            implementation?.init(application)
        }

        fun log(event: Event) {
            implementation?.log(event)
        }

        fun log(priority: Int, tag: String, msg: String) {
            implementation?.log(priority, tag, msg)
        }

        fun logException(throwable: Throwable) {
            implementation?.logException(throwable)
        }
    }
}