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

package de.vanita5.twittnuker.model;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import org.apache.commons.lang3.builder.ToStringBuilder;

import de.vanita5.twittnuker.TwittnukerConstants;
import de.vanita5.twittnuker.annotation.CustomTabType;

import static de.vanita5.twittnuker.util.CompareUtils.bundleEquals;
import static de.vanita5.twittnuker.util.CompareUtils.classEquals;
import static de.vanita5.twittnuker.util.CompareUtils.objectEquals;

public class SupportTabSpec implements Comparable<SupportTabSpec>, TwittnukerConstants {

    public CharSequence name;
    public final Object icon;
    @CustomTabType
    @Nullable
    public final String type;
    public final Class<? extends Fragment> cls;
    public final Bundle args;
    public final int position;
    public final String tag;

    public SupportTabSpec(final String name, final Object icon, final Class<? extends Fragment> cls, final Bundle args,
                          final int position, String tag) {
        this(name, icon, null, cls, args, position, tag);
    }

    public SupportTabSpec(final String name, final Object icon, @CustomTabType @Nullable final String type,
                          final Class<? extends Fragment> cls, final Bundle args, final int position,
                          final String tag) {
        if (cls == null) throw new IllegalArgumentException("Fragment cannot be null!");
        if (name == null && icon == null)
            throw new IllegalArgumentException("You must specify a name or icon for this tab!");
        this.name = name;
        this.icon = icon;
        this.type = type;
        this.cls = cls;
        this.args = args;
        this.position = position;
        this.tag = tag;
    }

    @Override
    public int compareTo(@NonNull final SupportTabSpec another) {
        return position - another.position;
    }

    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof SupportTabSpec)) return false;
        final SupportTabSpec spec = (SupportTabSpec) o;
        return objectEquals(name, spec.name) && objectEquals(icon, spec.icon) && classEquals(cls, spec.cls)
                && bundleEquals(args, spec.args) && position == spec.position;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("name", name)
                .append("icon", icon)
                .append("type", type)
                .append("cls", cls)
                .append("args", args)
                .append("position", position)
                .append("tag", tag)
                .toString();
    }

}