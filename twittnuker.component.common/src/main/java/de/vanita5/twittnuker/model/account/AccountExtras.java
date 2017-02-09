/*
 *  Twittnuker - Twitter client for Android
 *
 *  Copyright (C) 2013-2017 vanita5 <mail@vanit.as>
 *
 *  This program incorporates a modified version of Twidere.
 *  Copyright (C) 2012-2017 Mariotaku Lee <mariotaku.lee@gmail.com>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.vanita5.twittnuker.model.account;

import android.os.Parcelable;
import android.support.annotation.NonNull;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;

public interface AccountExtras extends Parcelable {

    @JsonObject
    class ImageLimit {

        @JsonField(name = "max_width")
        int maxWidth;
        @JsonField(name = "max_height")
        int maxHeight;

        public int getMaxWidth() {
            return maxWidth;
        }

        public void setMaxWidth(int maxWidth) {
            this.maxWidth = maxWidth;
        }

        public int getMaxHeight() {
            return maxHeight;
        }

        public void setMaxHeight(int maxHeight) {
            this.maxHeight = maxHeight;
        }

        @NonNull
        public static ImageLimit ofSize(int width, int height) {
            final ImageLimit limit = new ImageLimit();
            limit.setMaxWidth(width);
            limit.setMaxHeight(height);
            return limit;
        }
    }

    @JsonObject
    class VideoLimit {

        boolean supported;

        @JsonField(name = "min_width")
        int minWidth;
        @JsonField(name = "min_height")
        int minHeight;

        @JsonField(name = "max_width")
        int maxWidth;
        @JsonField(name = "max_height")
        int maxHeight;

        @JsonField(name = "can_rotate_geometry_limit")
        boolean canRotateGeometryLimit;

        @JsonField(name = "max_size_sync")
        long maxSizeSync;
        @JsonField(name = "max_size_async")
        long maxSizeAsync;

        @JsonField(name = "min_aspect_ratio")
        double minAspectRatio;
        @JsonField(name = "max_aspect_ratio")
        double maxAspectRatio;

        @JsonField(name = "min_frame_rate")
        double minFrameRate;
        @JsonField(name = "max_frame_rate")
        double maxFrameRate;

        @JsonField(name = "min_duration_sync")
        long minDurationSync;
        @JsonField(name = "min_duration_async")
        long minDurationAsync;
        @JsonField(name = "max_duration_sync")
        long maxDurationSync;
        @JsonField(name = "max_duration_async")
        long maxDurationAsync;

        public boolean isSupported() {
            return supported;
        }

        public void setSupported(boolean supported) {
            this.supported = supported;
        }

        public int getMinWidth() {
            return minWidth;
        }

        public void setMinWidth(int minWidth) {
            this.minWidth = minWidth;
        }

        public int getMinHeight() {
            return minHeight;
        }

        public void setMinHeight(int minHeight) {
            this.minHeight = minHeight;
        }

        public int getMaxWidth() {
            return maxWidth;
        }

        public void setMaxWidth(int maxWidth) {
            this.maxWidth = maxWidth;
        }

        public int getMaxHeight() {
            return maxHeight;
        }

        public void setMaxHeight(int maxHeight) {
            this.maxHeight = maxHeight;
        }

        public boolean canRotateGeometryLimit() {
            return canRotateGeometryLimit;
        }

        public void setCanRotateGeometryLimit(boolean canRotateGeometryLimit) {
            this.canRotateGeometryLimit = canRotateGeometryLimit;
        }

        public long getMaxSizeSync() {
            return maxSizeSync;
        }

        public void setMaxSizeSync(long maxSizeSync) {
            this.maxSizeSync = maxSizeSync;
        }

        public long getMaxSizeAsync() {
            return maxSizeAsync;
        }

        public void setMaxSizeAsync(long maxSizeAsync) {
            this.maxSizeAsync = maxSizeAsync;
        }

        public double getMinAspectRatio() {
            return minAspectRatio;
        }

        public void setMinAspectRatio(double minAspectRatio) {
            this.minAspectRatio = minAspectRatio;
        }

        public double getMaxAspectRatio() {
            return maxAspectRatio;
        }

        public void setMaxAspectRatio(double maxAspectRatio) {
            this.maxAspectRatio = maxAspectRatio;
        }

        public double getMinFrameRate() {
            return minFrameRate;
        }

        public void setMinFrameRate(double minFrameRate) {
            this.minFrameRate = minFrameRate;
        }

        public double getMaxFrameRate() {
            return maxFrameRate;
        }

        public void setMaxFrameRate(double maxFrameRate) {
            this.maxFrameRate = maxFrameRate;
        }

        public long getMinDurationSync() {
            return minDurationSync;
        }

        public void setMinDurationSync(long minDurationSync) {
            this.minDurationSync = minDurationSync;
        }

        public long getMinDurationAsync() {
            return minDurationAsync;
        }

        public void setMinDurationAsync(long minDurationAsync) {
            this.minDurationAsync = minDurationAsync;
        }

        public long getMaxDurationSync() {
            return maxDurationSync;
        }

        public void setMaxDurationSync(long maxDurationSync) {
            this.maxDurationSync = maxDurationSync;
        }

        public long getMaxDurationAsync() {
            return maxDurationAsync;
        }

        public void setMaxDurationAsync(long maxDurationAsync) {
            this.maxDurationAsync = maxDurationAsync;
        }

        @SuppressWarnings("RedundantIfStatement")
        public boolean checkGeometry(int width, int height) {
            // Check w & h
            boolean widthValid = inRange(width, getMinWidth(), getMaxWidth());
            boolean heightValid = inRange(height, getMinHeight(), getMaxHeight());
            if (canRotateGeometryLimit()) {
                widthValid |= inRange(height, getMinWidth(), getMaxWidth());
                heightValid |= inRange(width, getMinHeight(), getMaxHeight());
            }

            if (!widthValid || !heightValid) {
                return false;
            }

            // Check aspect ratio
            double aspectRatio = width / (double) height;
            if (!inRange(aspectRatio, getMinAspectRatio(), getMaxAspectRatio())) {
                return false;
            }

            return true;
        }

        public boolean checkFrameRate(double frameRate) {
            return inRange(frameRate, getMinFrameRate(), getMaxFrameRate());
        }

        public boolean checkMinDuration(long duration, boolean async) {
            final long limit = async ? getMinDurationAsync() : getMinDurationSync();
            if (limit <= 0) return true;
            return duration >= limit;
        }

        public boolean checkMaxDuration(long duration, boolean async) {
            final long limit = async ? getMaxDurationAsync() : getMaxDurationSync();
            if (limit <= 0) return true;
            return duration <= limit;
        }

        public boolean checkSize(long size, boolean async) {
            final long limit = async ? getMaxSizeAsync() : getMaxSizeSync();
            if (limit <= 0) return true;
            return size < limit;
        }

        @SuppressWarnings("RedundantIfStatement")
        private boolean inRange(int num, int min, int max) {
            if (min > 0 && num < min) return false;
            if (max > 0 && num > max) return false;
            return true;
        }

        @SuppressWarnings("RedundantIfStatement")
        private boolean inRange(double num, double min, double max) {
            if (min > 0 && num < min) return false;
            if (max > 0 && num > max) return false;
            return true;
        }

        public static VideoLimit twitterDefault() {
            VideoLimit videoLimit = new VideoLimit();
            videoLimit.setSupported(true);
            videoLimit.setMinWidth(32);
            videoLimit.setMinHeight(32);
            videoLimit.setMaxWidth(1280);
            videoLimit.setMaxHeight(1024);
            videoLimit.setCanRotateGeometryLimit(true);
            videoLimit.setMinAspectRatio(1.0 / 3);
            videoLimit.setMaxAspectRatio(3);
            videoLimit.setMaxSizeSync(15 * 1024 * 1024);
            videoLimit.setMaxSizeAsync(512 * 1024 * 1024);
            videoLimit.setMinFrameRate(0);
            videoLimit.setMaxFrameRate(40);
            videoLimit.setMinDurationSync(500); // 0.5s
            videoLimit.setMinDurationAsync(500); // 0.5s
            videoLimit.setMaxDurationSync(30000); // 30s
            videoLimit.setMaxDurationAsync(140000); // 140s
            return videoLimit;
        }

        public static VideoLimit unsupported() {
            VideoLimit videoLimit = new VideoLimit();
            videoLimit.setSupported(false);
            return videoLimit;
        }
    }
}