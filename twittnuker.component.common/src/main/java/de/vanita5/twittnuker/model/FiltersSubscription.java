/*
 *          Twittnuker - Twitter client for Android
 *
 *          This program incorporates a modified version of
 *          Twidere - Twitter client for Android
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package de.vanita5.twittnuker.model;

import org.mariotaku.library.objectcursor.annotation.CursorField;
import org.mariotaku.library.objectcursor.annotation.CursorObject;
import de.vanita5.twittnuker.provider.TwidereDataStore;
import de.vanita5.twittnuker.provider.TwidereDataStore.Filters;


@CursorObject(valuesCreator = true, tableInfo = true)
public class FiltersSubscription {
    @CursorField(value = Filters.Subscriptions._ID, excludeWrite = true, type = TwidereDataStore.TYPE_PRIMARY_KEY)
    public long id;

    @CursorField(Filters.Subscriptions.NAME)
    public String name;

    @CursorField(Filters.Subscriptions.COMPONENT)
    public String component;

    @CursorField(Filters.Subscriptions.ARGUMENTS)
    public String arguments;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FiltersSubscription that = (FiltersSubscription) o;

        return id == that.id;

    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }

    @Override
    public String toString() {
        return "FiltersSubscription{" +
                "arguments='" + arguments + '\'' +
                ", id=" + id +
                ", name='" + name + '\'' +
                ", component='" + component + '\'' +
                '}';
    }
}