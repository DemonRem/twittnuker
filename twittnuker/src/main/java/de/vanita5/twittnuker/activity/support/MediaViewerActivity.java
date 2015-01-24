/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.vanita5.twittnuker.activity.support;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.diegocarloslima.byakugallery.lib.TileBitmapDrawable;
import com.diegocarloslima.byakugallery.lib.TileBitmapDrawable.OnInitializeListener;
import com.diegocarloslima.byakugallery.lib.TouchImageView;

import org.apache.commons.lang3.ArrayUtils;
import de.vanita5.twittnuker.Constants;
import de.vanita5.twittnuker.R;
import de.vanita5.twittnuker.adapter.support.SupportFixedFragmentStatePagerAdapter;
import de.vanita5.twittnuker.fragment.support.BaseSupportFragment;
import de.vanita5.twittnuker.loader.support.TileImageLoader;
import de.vanita5.twittnuker.loader.support.TileImageLoader.Result;
import de.vanita5.twittnuker.model.ParcelableMedia;
import de.vanita5.twittnuker.util.ThemeUtils;
import de.vanita5.twittnuker.util.Utils;

public final class MediaViewerActivity extends BaseSupportActivity implements Constants {

    private ViewPager mViewPager;
    private MediaPagerAdapter mAdapter;

	@Override
	public int getThemeResourceId() {
		return ThemeUtils.getViewerThemeResource(this);
	}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_viewer);
        mAdapter = new MediaPagerAdapter(this);
        mViewPager.setAdapter(mAdapter);
        final Intent intent = getIntent();
        final long accountId = intent.getLongExtra(EXTRA_ACCOUNT_ID, -1);
        final ParcelableMedia[] media = Utils.newParcelableArray(intent.getParcelableArrayExtra(EXTRA_MEDIA), ParcelableMedia.CREATOR);
        final ParcelableMedia currentMedia = intent.getParcelableExtra(EXTRA_CURRENT_MEDIA);
        mAdapter.setMedia(accountId, media);
        final int currentIndex = ArrayUtils.indexOf(media, currentMedia);
        if (currentIndex != -1) {
            mViewPager.setCurrentItem(currentIndex, false);
        }
    }

	@Override
    public void onSupportContentChanged() {
        super.onSupportContentChanged();
        mViewPager = (ViewPager) findViewById(R.id.view_pager);
	}

    public static final class MediaPageFragment extends BaseSupportFragment
            implements TileImageLoader.DownloadListener, LoaderManager.LoaderCallbacks<TileImageLoader.Result> {

        private TouchImageView mImageView;
        private ProgressBar mProgressBar;
        private boolean mLoaderInitialized;
        private long mContentLength;

	    @Override
        public Loader<Result> onCreateLoader(final int id, final Bundle args) {
            mProgressBar.setVisibility(View.VISIBLE);
            mProgressBar.setIndeterminate(true);
		    invalidateOptionsMenu();
            final ParcelableMedia media = args.getParcelable(EXTRA_MEDIA);
		    final long accountId = args.getLong(EXTRA_ACCOUNT_ID, -1);
            return new TileImageLoader(getActivity(), this, accountId, Uri.parse(media.media_url));
	    }

	    @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_media_page, container, false);
	    }

        @Override
        public void onDownloadError(final Throwable t) {
            mContentLength = 0;
        }

        @Override
        public void onDownloadFinished() {
            mContentLength = 0;
        }

        @Override
        public void onDownloadStart(final long total) {
            mContentLength = total;
            mProgressBar.setIndeterminate(total <= 0);
            mProgressBar.setMax(total > 0 ? (int) (total / 1024) : 0);
	    }


        @Override
        public void onLoaderReset(final Loader<TileImageLoader.Result> loader) {

        }

        @Override
        public void onLoadFinished(final Loader<TileImageLoader.Result> loader, final TileImageLoader.Result data) {
            if (data.hasData()) {
                mImageView.setVisibility(View.VISIBLE);
                if (data.useDecoder) {
                    TileBitmapDrawable.attachTileBitmapDrawable(mImageView, data.file.getAbsolutePath(), null, new OnInitializeListener() {
                        @Override
                        public void onStartInitialization() {

                        }

                        @Override
                        public void onEndInitialization() {

                        }
                    });
                } else {
                    mImageView.setImageBitmap(data.bitmap);
                }
            } else {
                mImageView.setVisibility(View.GONE);
                Utils.showErrorMessage(getActivity(), null, data.exception, true);
		    }
            mProgressBar.setVisibility(View.GONE);
            mProgressBar.setProgress(0);
		    invalidateOptionsMenu();
	    }

        @Override
        public void onProgressUpdate(final long downloaded) {
		    if (mContentLength == 0) {
                mProgressBar.setIndeterminate(true);
			    return;
		    }
            mProgressBar.setIndeterminate(false);
            mProgressBar.setProgress((int) (downloaded / 1024));
	    }

        @Override
        public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            mImageView = (TouchImageView) view.findViewById(R.id.image_view);
            mProgressBar = (ProgressBar) view.findViewById(R.id.progress);
	    }

	    @Override
        public void onActivityCreated(@Nullable Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
			loadImage();
		}

        private void loadImage() {
            getLoaderManager().destroyLoader(0);
            if (!mLoaderInitialized) {
                getLoaderManager().initLoader(0, getArguments(), this);
                mLoaderInitialized = true;
            } else {
                getLoaderManager().restartLoader(0, getArguments(), this);
	        }
        }
    }

    private static class MediaPagerAdapter extends SupportFixedFragmentStatePagerAdapter {

        private final MediaViewerActivity mActivity;
        private long mAccountId;
        private ParcelableMedia[] mMedia;

        public MediaPagerAdapter(MediaViewerActivity activity) {
            super(activity.getSupportFragmentManager());
            mActivity = activity;
	    }

	    @Override
        public int getCount() {
            if (mMedia == null) return 0;
            return mMedia.length;
	    }

	    @Override
        public Fragment getItem(int position) {
            final Bundle args = new Bundle();
            args.putLong(EXTRA_ACCOUNT_ID, mAccountId);
            args.putParcelable(EXTRA_MEDIA, mMedia[position]);
            return Fragment.instantiate(mActivity, MediaPageFragment.class.getName(), args);
	    }

        public void setMedia(long accountId, ParcelableMedia[] media) {
            mAccountId = accountId;
            mMedia = media;
            notifyDataSetChanged();
	    }
    }
}