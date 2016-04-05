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

package de.vanita5.twittnuker.util.support;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager.TaskDescription;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Backward compatibility utilities for {@link Activity}
 * Created by mariotaku on 14/11/4.
 */
public class ActivitySupport {

    public static void setTaskDescription(Activity activity, TaskDescriptionCompat taskDescription) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return;
        ActivityAccessorL.setTaskDescription(activity, taskDescription);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    static class ActivityAccessorL {
        public static void setTaskDescription(Activity activity, TaskDescriptionCompat taskDescription) {
            activity.setTaskDescription(toNativeTaskDescription(taskDescription));
        }

        private static TaskDescription toNativeTaskDescription(TaskDescriptionCompat taskDescription) {
            return new TaskDescription(taskDescription.getLabel(), taskDescription.getIcon(), taskDescription.getPrimaryColor());
        }
    }

    /**
     * Information you can set and retrieve about the current context within the recent task list.
     */
    public static class TaskDescriptionCompat implements Parcelable {

        private String mLabel;
        private Bitmap mIcon;
        private int mColorPrimary;

        /**
         * Creates the TaskDescription to the specified values.
         *
         * @param label        A label and description of the current state of this task.
         * @param icon         An icon that represents the current state of this task.
         * @param colorPrimary A color to override the theme's primary color.  This color must be opaque.
         */
        public TaskDescriptionCompat(String label, Bitmap icon, int colorPrimary) {
            if ((colorPrimary != 0) && (Color.alpha(colorPrimary) != 255)) {
                throw new RuntimeException("A TaskDescription's primary color should be opaque");
            }

            mLabel = label;
            mIcon = icon;
            mColorPrimary = colorPrimary;
        }

        /**
         * Creates the TaskDescription to the specified values.
         *
         * @param label A label and description of the current state of this context.
         * @param icon  An icon that represents the current state of this context.
         */
        public TaskDescriptionCompat(String label, Bitmap icon) {
            this(label, icon, 0);
        }

        /**
         * Creates the TaskDescription to the specified values.
         *
         * @param label A label and description of the current state of this context.
         */
        public TaskDescriptionCompat(String label) {
            this(label, null, 0);
        }

        /**
         * Creates an empty TaskDescription.
         */
        public TaskDescriptionCompat() {
            this(null, null, 0);
        }

        /**
         * Creates a copy of another TaskDescription.
         */
        public TaskDescriptionCompat(TaskDescriptionCompat td) {
            mLabel = td.mLabel;
            mIcon = td.mIcon;
            mColorPrimary = td.mColorPrimary;
        }

        private TaskDescriptionCompat(Parcel source) {
            readFromParcel(source);
        }

        /**
         * @return The label and description of the current state of this task.
         */
        public String getLabel() {
            return mLabel;
        }

        /**
         * @return The icon that represents the current state of this task.
         */
        public Bitmap getIcon() {
            if (mIcon != null) {
                return mIcon;
            }
            return null;
        }

        /**
         * @return The color override on the theme's primary color.
         */
        public int getPrimaryColor() {
            return mColorPrimary;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            if (mLabel == null) {
                dest.writeInt(0);
            } else {
                dest.writeInt(1);
                dest.writeString(mLabel);
            }
            if (mIcon == null) {
                dest.writeInt(0);
            } else {
                dest.writeInt(1);
                mIcon.writeToParcel(dest, 0);
            }
            dest.writeInt(mColorPrimary);
        }

        public void readFromParcel(Parcel source) {
            mLabel = source.readInt() > 0 ? source.readString() : null;
            mIcon = source.readInt() > 0 ? Bitmap.CREATOR.createFromParcel(source) : null;
            mColorPrimary = source.readInt();
        }

        public static final Creator<TaskDescriptionCompat> CREATOR
                = new Creator<TaskDescriptionCompat>() {
            @Override
            public TaskDescriptionCompat createFromParcel(Parcel source) {
                return new TaskDescriptionCompat(source);
            }

            @Override
            public TaskDescriptionCompat[] newArray(int size) {
                return new TaskDescriptionCompat[size];
            }
        };

        @Override
        public String toString() {
            return "TaskDescription Label: " + mLabel + " Icon: " + mIcon +
                    " colorPrimary: " + mColorPrimary;
        }
    }
}