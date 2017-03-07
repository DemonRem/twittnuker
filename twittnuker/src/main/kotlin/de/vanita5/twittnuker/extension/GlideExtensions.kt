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

package de.vanita5.twittnuker.extension

import android.content.Context
import com.bumptech.glide.DrawableRequestBuilder
import com.bumptech.glide.DrawableTypeRequest
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.engine.DiskCacheStrategy
import jp.wasabeef.glide.transformations.CropCircleTransformation
import de.vanita5.twittnuker.R
import de.vanita5.twittnuker.annotation.ImageShapeStyle
import de.vanita5.twittnuker.extension.model.getBestProfileBanner
import de.vanita5.twittnuker.extension.model.user
import de.vanita5.twittnuker.model.*
import de.vanita5.twittnuker.util.Utils
import de.vanita5.twittnuker.view.ShapedImageView

fun RequestManager.loadProfileImage(
        context: Context,
        url: String?,
        @ImageShapeStyle style: Int = ImageShapeStyle.SHAPE_CIRCLE,
        size: String = context.getString(R.string.profile_image_size)
): DrawableRequestBuilder<String?> {
    return configureLoadProfileImage(context, style) { load(Utils.getTwitterProfileImageOfSize(url, size)) }
}

fun RequestManager.loadProfileImage(context: Context, resourceId: Int,
        @ImageShapeStyle shapeStyle: Int = ImageShapeStyle.SHAPE_CIRCLE): DrawableRequestBuilder<Int> {
    return configureLoadProfileImage(context, shapeStyle) { load(resourceId) }
}

fun RequestManager.loadProfileImage(context: Context, account: AccountDetails,
        @ImageShapeStyle shapeStyle: Int = ImageShapeStyle.SHAPE_CIRCLE): DrawableRequestBuilder<String?> {
    return loadProfileImage(context, account.user, shapeStyle)
}

fun RequestManager.loadProfileImage(context: Context, user: ParcelableUser,
        @ImageShapeStyle shapeStyle: Int = ImageShapeStyle.SHAPE_CIRCLE): DrawableRequestBuilder<String?> {
    if (user.extras != null && user.extras.profile_image_url_fallback == null) {
        // No fallback image, use compatible logic
        return loadProfileImage(context, user.profile_image_url)
    }
    return configureLoadProfileImage(context, shapeStyle) { load(user.profile_image_url) }
}

fun RequestManager.loadProfileImage(context: Context, userList: ParcelableUserList,
        @ImageShapeStyle shapeStyle: Int = ImageShapeStyle.SHAPE_CIRCLE): DrawableRequestBuilder<String?> {
    return configureLoadProfileImage(context, shapeStyle) { load(userList.user_profile_image_url) }
}

fun RequestManager.loadProfileImage(context: Context, group: ParcelableGroup,
        @ImageShapeStyle shapeStyle: Int = ImageShapeStyle.SHAPE_CIRCLE): DrawableRequestBuilder<String?> {
    return configureLoadProfileImage(context, shapeStyle) { load(group.homepage_logo) }
}

fun RequestManager.loadProfileImage(context: Context, status: ParcelableStatus,
        @ImageShapeStyle shapeStyle: Int = ImageShapeStyle.SHAPE_CIRCLE): DrawableRequestBuilder<String?> {
    if (status.extras != null && status.extras.user_profile_image_url_fallback == null) {
        // No fallback image, use compatible logic
        return loadProfileImage(context, status.user_profile_image_url)
    }
    return configureLoadProfileImage(context, shapeStyle) { load(status.user_profile_image_url) }
}

fun RequestManager.loadProfileImage(context: Context, conversation: ParcelableMessageConversation): DrawableRequestBuilder<*> {
    if (conversation.conversation_type == ParcelableMessageConversation.ConversationType.ONE_TO_ONE) {
        val user = conversation.user
        if (user != null) {
            return loadProfileImage(context, user)
        } else {
            // TODO: show default conversation icon
            return loadProfileImage(context, de.vanita5.twittnuker.R.drawable.ic_profile_image_default_group)
        }
    } else {
        return loadProfileImage(context, conversation.conversation_avatar).placeholder(R.drawable.ic_profile_image_default_group)
    }
}

fun RequestManager.loadOriginalProfileImage(context: Context, user: ParcelableUser,
        @ImageShapeStyle shapeStyle: Int = ImageShapeStyle.SHAPE_CIRCLE): DrawableRequestBuilder<String> {
    val original = user.extras.profile_image_url_original?.let { if (it.isEmpty()) null else it }
            ?: Utils.getOriginalTwitterProfileImage(user.profile_image_url)
    return configureLoadProfileImage(context, shapeStyle) { load(original) }
}

fun RequestManager.loadProfileBanner(context: Context, user: ParcelableUser, width: Int): DrawableTypeRequest<String?> {
    return load(user.getBestProfileBanner(width))
}

internal inline fun <T> configureLoadProfileImage(context: Context, shapeStyle: Int,
        create: () -> DrawableTypeRequest<T>): DrawableRequestBuilder<T> {
    val builder = create()
    builder.diskCacheStrategy(DiskCacheStrategy.RESULT)
    builder.dontAnimate()
    if (!ShapedImageView.OUTLINE_DRAW) {
        when (shapeStyle) {
            ImageShapeStyle.SHAPE_CIRCLE -> {
                builder.bitmapTransform(CropCircleTransformation(context))
            }
            ImageShapeStyle.SHAPE_RECTANGLE -> {
            }
        }
    }
    return builder
}