/*
 * Twittnuker - Twitter client for Android
 *
 * Copyright (C) 2013-2015 vanita5 <mail@vanita5.de>
 *
 * This program incorporates a modified version of Twidere.
 * Copyright (C) 2012-2015 Mariotaku Lee <mariotaku.lee@gmail.com>
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

package de.vanita5.twittnuker.util.imageloader;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Pair;
import android.webkit.URLUtil;

import com.nostra13.universalimageloader.core.assist.ContentLengthInputStream;
import com.nostra13.universalimageloader.core.download.BaseImageDownloader;

import org.mariotaku.simplerestapi.http.Authorization;
import org.mariotaku.simplerestapi.http.RestHttpClient;
import org.mariotaku.simplerestapi.http.RestResponse;
import org.mariotaku.simplerestapi.http.mime.TypedData;
import de.vanita5.twittnuker.Constants;
import de.vanita5.twittnuker.R;
import de.vanita5.twittnuker.constant.SharedPreferenceConstants;
import de.vanita5.twittnuker.model.ParcelableAccount;
import de.vanita5.twittnuker.model.ParcelableAccount.ParcelableCredentials;
import de.vanita5.twittnuker.model.ParcelableMedia;
import de.vanita5.twittnuker.util.MediaPreviewUtils;
import de.vanita5.twittnuker.util.SharedPreferencesWrapper;
import de.vanita5.twittnuker.util.TwidereLinkify;
import de.vanita5.twittnuker.util.TwitterAPIUtils;
import de.vanita5.twittnuker.util.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import twitter4j.TwitterException;

public class TwidereImageDownloader extends BaseImageDownloader implements Constants {

	private final Context mContext;
    private final SharedPreferencesWrapper mPreferences;
    private RestHttpClient mClient;
	private boolean mFastImageLoading;
	private final boolean mFullImage;
	private final String mTwitterProfileImageSize;

	public TwidereImageDownloader(final Context context, final boolean fullImage) {
		super(context);
		mContext = context;
        mPreferences = SharedPreferencesWrapper.getInstance(context, SHARED_PREFERENCES_NAME,
                Context.MODE_PRIVATE, SharedPreferenceConstants.class);
		mFullImage = fullImage;
		mTwitterProfileImageSize = context.getString(R.string.profile_image_size);
		reloadConnectivitySettings();

	}

	public void reloadConnectivitySettings() {
        mClient = TwitterAPIUtils.getDefaultHttpClient(mContext);
        mFastImageLoading = mPreferences.getBoolean(KEY_FAST_IMAGE_LOADING);
	}

	@Override
	protected InputStream getStreamFromNetwork(final String uriString, final Object extras) throws IOException {
		if (uriString == null) return null;
		final ParcelableMedia media = MediaPreviewUtils.getAllAvailableImage(uriString, mFullImage, mFullImage
				|| !mFastImageLoading ? mClient : null);
		try {
			final String mediaUrl = media != null ? media.media_url : uriString;
			if (isTwitterProfileImage(uriString)) {
                final String replaced = Utils.getTwitterProfileImageOfSize(mediaUrl, mTwitterProfileImageSize);
				return getStreamFromNetworkInternal(replaced, extras);
			} else
				return getStreamFromNetworkInternal(mediaUrl, extras);
		} catch (final TwitterException e) {
			final int statusCode = e.getStatusCode();
			if (statusCode != -1 && isTwitterProfileImage(uriString) && !uriString.contains("_normal.")) {
				try {
                    return getStreamFromNetworkInternal(Utils.getNormalTwitterProfileImage(uriString), extras);
                } catch (final TwitterException ignored) {
				}
			}
			throw new IOException(String.format(Locale.US, "Error downloading image %s, error code: %d", uriString,
									statusCode));
		}
	}

    private String getReplacedUri(@NonNull final Uri uri, final String apiUrlFormat) {
		if (apiUrlFormat == null) return uri.toString();
		if (isTwitterUri(uri)) {
			final StringBuilder sb = new StringBuilder();
			final String domain = uri.getHost().replaceAll("\\.?twitter.com", "");
			final String path = uri.getPath();
			sb.append(Utils.getApiUrl(apiUrlFormat, domain, path));
			final String query = uri.getQuery();
			if (!TextUtils.isEmpty(query)) {
				sb.append("?");
				sb.append(query);
	}
			final String fragment = uri.getFragment();
			if (!TextUtils.isEmpty(fragment)) {
				sb.append("#");
				sb.append(fragment);
	}
			return sb.toString();
		}
		return uri.toString();
	}

	private ContentLengthInputStream getStreamFromNetworkInternal(final String uriString, final Object extras)
			throws IOException, TwitterException {
		final Uri uri = Uri.parse(uriString);
		final Authorization auth;
		final ParcelableCredentials account;
		if (isTwitterAuthRequired(uri) && extras instanceof AccountExtra) {
			final AccountExtra accountExtra = (AccountExtra) extras;
			account = ParcelableAccount.getCredentials(mContext, accountExtra.account_id);
            auth = Utils.getTwitterAuthorization(mContext, accountExtra.account_id);
		} else {
			account = null;
			auth = null;
		}
        String modifiedUri = getReplacedUri(uri, account != null ? account.api_url_format : null);
        final List<Pair<String, String>> additionalHeaders = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            additionalHeaders.add(Pair.create("Accept", "image/webp, */*"));
        }
        final RestResponse resp = TwitterAPIUtils.getRedirectedHttpResponse(mClient, modifiedUri, uriString, auth, additionalHeaders);
        final TypedData body = resp.getBody();
        return new ContentLengthInputStream(body.stream(), (int) body.length());
	}

	private boolean isTwitterAuthRequired(final Uri uri) {
		if (uri == null) return false;
		return "ton.twitter.com".equalsIgnoreCase(uri.getHost());
	}

	private boolean isTwitterProfileImage(final String uriString) {
		if (TextUtils.isEmpty(uriString)) return false;
        return TwidereLinkify.PATTERN_TWITTER_PROFILE_IMAGES.matcher(uriString).matches();
	}

	private boolean isTwitterUri(final Uri uri) {
		if (uri == null) return false;
		return "ton.twitter.com".equalsIgnoreCase(uri.getHost());
	}

}