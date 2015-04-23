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
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.AsyncTask.Status;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.util.Pair;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.ShareActionProvider;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnGenericMotionListener;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.MediaController;
import android.widget.ProgressBar;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.pnikosis.materialishprogress.ProgressWheel;
import com.sprylab.android.widget.TextureVideoView;

import org.apache.commons.lang3.ArrayUtils;
import de.vanita5.twittnuker.Constants;
import de.vanita5.twittnuker.R;
import de.vanita5.twittnuker.adapter.support.SupportFixedFragmentStatePagerAdapter;
import de.vanita5.twittnuker.app.TwittnukerApplication;
import de.vanita5.twittnuker.fragment.support.BaseSupportFragment;
import de.vanita5.twittnuker.fragment.support.ViewStatusDialogFragment;
import de.vanita5.twittnuker.loader.support.TileImageLoader;
import de.vanita5.twittnuker.loader.support.TileImageLoader.DownloadListener;
import de.vanita5.twittnuker.loader.support.TileImageLoader.Result;
import de.vanita5.twittnuker.model.ParcelableMedia;
import de.vanita5.twittnuker.model.ParcelableMedia.VideoInfo.Variant;
import de.vanita5.twittnuker.model.ParcelableStatus;
import de.vanita5.twittnuker.util.AsyncTaskUtils;
import de.vanita5.twittnuker.util.MenuUtils;
import de.vanita5.twittnuker.util.SaveFileTask;
import de.vanita5.twittnuker.util.ThemeUtils;
import de.vanita5.twittnuker.util.Utils;
import de.vanita5.twittnuker.util.VideoLoader;
import de.vanita5.twittnuker.util.VideoLoader.VideoLoadingListener;
import de.vanita5.twittnuker.view.LinePageIndicator;

import java.io.File;

import pl.droidsonroids.gif.GifTextureView;
import pl.droidsonroids.gif.InputSource.FileSource;


public final class MediaViewerActivity extends ThemedActionBarActivity implements Constants, OnPageChangeListener {

    private static final String EXTRA_LOOP = "loop";

    private ViewPager mViewPager;
    private MediaPagerAdapter mAdapter;
    private ActionBar mActionBar;
    private View mMediaStatusContainer;
    private LinePageIndicator mIndicator;


	@Override
    public int getThemeColor() {
        return ThemeUtils.getUserAccentColor(this);
    }

    @Override
    public int getActionBarColor() {
        return ThemeUtils.getActionBarColor(this);
    }

    @Override
	public int getThemeResourceId() {
		return ThemeUtils.getViewerThemeResource(this);
	}

    public boolean hasStatus() {
        return getIntent().hasExtra(EXTRA_STATUS);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                finish();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        setBarVisibility(true);
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    @Override
    public void onSupportContentChanged() {
        super.onSupportContentChanged();
        mViewPager = (ViewPager) findViewById(R.id.view_pager);
        mIndicator = (LinePageIndicator) findViewById(R.id.pager_indicator);
        mMediaStatusContainer = findViewById(R.id.media_status_container);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionBar = getSupportActionBar();
        mActionBar.setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_media_viewer);
        mAdapter = new MediaPagerAdapter(this);
        mViewPager.setAdapter(mAdapter);
        mViewPager.setPageMargin(getResources().getDimensionPixelSize(R.dimen.element_spacing_normal));
        mViewPager.setOnPageChangeListener(this);
        mIndicator.setSelectedColor(getCurrentThemeColor());
        mIndicator.setViewPager(mViewPager);
        final Intent intent = getIntent();
        final long accountId = intent.getLongExtra(EXTRA_ACCOUNT_ID, -1);
        final ParcelableMedia[] media = Utils.newParcelableArray(intent.getParcelableArrayExtra(EXTRA_MEDIA), ParcelableMedia.CREATOR);
        final ParcelableMedia currentMedia = intent.getParcelableExtra(EXTRA_CURRENT_MEDIA);
        mAdapter.setMedia(accountId, media);
        mIndicator.notifyDataSetChanged();
        final int currentIndex = ArrayUtils.indexOf(media, currentMedia);
        if (currentIndex != -1) {
            mViewPager.setCurrentItem(currentIndex, false);
        }
        if (isMediaStatusEnabled() && intent.hasExtra(EXTRA_STATUS)) {
            mMediaStatusContainer.setVisibility(View.VISIBLE);
            final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            final Fragment f = new ViewStatusDialogFragment();
            final Bundle args = new Bundle();
            args.putParcelable(EXTRA_STATUS, intent.getParcelableExtra(EXTRA_STATUS));
            args.putBoolean(EXTRA_SHOW_MEDIA_PREVIEW, false);
            args.putBoolean(EXTRA_SHOW_EXTRA_TYPE, false);
            f.setArguments(args);
            ft.replace(R.id.media_status, f);
            ft.commit();
        } else {
            mMediaStatusContainer.setVisibility(View.GONE);
        }
    }

    private ParcelableStatus getStatus() {
        return getIntent().getParcelableExtra(EXTRA_STATUS);
    }

    private boolean isBarShowing() {
        if (mActionBar == null) return false;
        return mActionBar.isShowing();
	}

    private boolean isMediaStatusEnabled() {
        return false;
    }

    private void setBarVisibility(boolean visible) {
        if (mActionBar == null) return;
        if (visible) {
            mActionBar.show();
        } else {
            mActionBar.hide();
        }

        mIndicator.setVisibility(visible ? View.VISIBLE : View.GONE);
        mMediaStatusContainer.setVisibility(isMediaStatusEnabled() && visible ? View.VISIBLE : View.GONE);
    }

    private void toggleBar() {
        setBarVisibility(!isBarShowing());
    }

    public static final class ImagePageFragment extends BaseSupportFragment
            implements DownloadListener, LoaderCallbacks<Result>, OnClickListener {

        private SubsamplingScaleImageView mImageView;
        private GifTextureView mGifImageView;
        private ProgressWheel mProgressBar;
        private boolean mLoaderInitialized;
        private float mContentLength;
        private SaveFileTask mSaveFileTask;

        private File mImageFile;

	    @Override
        public void onBaseViewCreated(View view, @Nullable Bundle savedInstanceState) {
            super.onBaseViewCreated(view, savedInstanceState);
            mImageView = (SubsamplingScaleImageView) view.findViewById(R.id.image_view);
            mGifImageView = (GifTextureView) view.findViewById(R.id.gif_image_view);
            mProgressBar = (ProgressWheel) view.findViewById(R.id.progress);
        }

        @Override
        public void onClick(View v) {
            final MediaViewerActivity activity = (MediaViewerActivity) getActivity();
            if (activity == null) return;
            activity.toggleBar();
        }

        @Override
        public Loader<Result> onCreateLoader(final int id, final Bundle args) {
            mProgressBar.setVisibility(View.VISIBLE);
            mProgressBar.spin();
		    invalidateOptionsMenu();
            final ParcelableMedia media = getMedia();
		    final long accountId = args.getLong(EXTRA_ACCOUNT_ID, -1);
            return new TileImageLoader(getActivity(), this, accountId, Uri.parse(media.media_url));
	    }

	    @Override
        public void onLoadFinished(final Loader<TileImageLoader.Result> loader, final TileImageLoader.Result data) {
            if (data.hasData()) {
                mImageFile = data.file;
                if (data.useDecoder) {
                    mGifImageView.setVisibility(View.GONE);
                	mImageView.setVisibility(View.VISIBLE);
                    mImageView.setImage(ImageSource.uri(Uri.fromFile(data.file)));
                } else if ("image/gif".equals(data.options.outMimeType)) {
                    mGifImageView.setVisibility(View.VISIBLE);
                    mImageView.setVisibility(View.GONE);
                    mGifImageView.setInputSource(new FileSource(data.file));
                    updateScaleLimit();
                } else {
                    mGifImageView.setVisibility(View.GONE);
                    mImageView.setVisibility(View.VISIBLE);
                    mImageView.setImage(ImageSource.bitmap(data.bitmap));
                    updateScaleLimit();
                }
            } else {
                mImageView.recycle();
                mImageFile = null;
                mImageView.setVisibility(View.GONE);
                mGifImageView.setVisibility(View.GONE);
                Utils.showErrorMessage(getActivity(), null, data.exception, true);
            }
            mProgressBar.setVisibility(View.GONE);
            mProgressBar.setProgress(0);
            invalidateOptionsMenu();
        }

        @Override
        public void onLoaderReset(final Loader<TileImageLoader.Result> loader) {
//            final Drawable drawable = mImageView.getDrawable();
//            if (drawable instanceof GifDrawable) {
//                ((GifDrawable) drawable).recycle();
//            }
        }

        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_media_page_image, container, false);
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
            mProgressBar.spin();
        }

        @Override
        public void onProgressUpdate(final long downloaded) {
            if (mContentLength <= 0) {
                if (!mProgressBar.isSpinning()) {
                    mProgressBar.spin();
                }
                return;
            }
            mProgressBar.setProgress(downloaded / mContentLength);
        }

        public void onZoomIn() {
            final MediaViewerActivity activity = (MediaViewerActivity) getActivity();
            activity.setBarVisibility(false);
        }

        @Override
        public void onPrepareOptionsMenu(Menu menu) {
            super.onPrepareOptionsMenu(menu);
            final File file = mImageFile;
            final boolean isLoading = getLoaderManager().hasRunningLoaders();
            final boolean hasImage = file != null && file.exists();
            MenuUtils.setMenuItemAvailability(menu, R.id.refresh, !hasImage && !isLoading);
            MenuUtils.setMenuItemAvailability(menu, R.id.share, hasImage && !isLoading);
            MenuUtils.setMenuItemAvailability(menu, R.id.save, hasImage && !isLoading);
            if (!hasImage) return;
			final MenuItem shareItem = menu.findItem(R.id.share);
			final ShareActionProvider shareProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(shareItem);
			final Intent intent = new Intent(Intent.ACTION_SEND);
			final Uri fileUri = Uri.fromFile(file);
			intent.setDataAndType(fileUri, Utils.getImageMimeType(file));
			intent.putExtra(Intent.EXTRA_STREAM, fileUri);
			final MediaViewerActivity activity = (MediaViewerActivity) getActivity();
			if (activity.hasStatus()) {
				final ParcelableStatus status = activity.getStatus();
				intent.putExtra(Intent.EXTRA_TEXT, Utils.getStatusShareText(activity, status));
				intent.putExtra(Intent.EXTRA_SUBJECT, Utils.getStatusShareSubject(activity, status));
			}
			if (shareProvider != null) shareProvider.setShareIntent(intent);
        }

        public void onZoomOut() {
            final MediaViewerActivity activity = (MediaViewerActivity) getActivity();
            activity.setBarVisibility(true);
        }

        private ParcelableMedia getMedia() {
            final Bundle args = getArguments();
            return args.getParcelable(EXTRA_MEDIA);
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            inflater.inflate(R.menu.menu_media_viewer_image_page, menu);
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

        private void openInBrowser() {
            final ParcelableMedia media = getMedia();
            final Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            if (media.page_url != null) {
                intent.setData(Uri.parse(media.page_url));
            } else {
                intent.setData(Uri.parse(media.media_url));
            }
            startActivity(intent);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            switch (item.getItemId()) {
                case MENU_OPEN_IN_BROWSER: {
                    openInBrowser();
                    return true;
                }
                case MENU_SAVE: {
                    saveToGallery();
                    return true;
                }
                case MENU_REFRESH: {
                    loadImage();
                    return true;
                }
            }
            return super.onOptionsItemSelected(item);
        }

        private void saveToGallery() {
            if (mSaveFileTask != null && mSaveFileTask.getStatus() == Status.RUNNING) return;
            final File file = mImageFile;
            final boolean hasImage = file != null && file.exists();
            if (!hasImage) return;
            mSaveFileTask = SaveFileTask.saveImage(getActivity(), file);
            mSaveFileTask.execute();
        }

        private void updateScaleLimit() {
//            final int viewWidth = mImageView.getWidth(), viewHeight = mImageView.getHeight();
//            final Drawable drawable = mImageView.getDrawable();
//            if (drawable == null || viewWidth <= 0 || viewHeight <= 0) return;
//            final int drawableWidth = drawable.getIntrinsicWidth();
//            final int drawableHeight = drawable.getIntrinsicHeight();
//            if (drawableWidth <= 0 || drawableHeight <= 0) return;
//            final float widthRatio = viewWidth / (float) drawableWidth;
//            final float heightRatio = viewHeight / (float) drawableHeight;
//            mImageView.setMaxScale(Math.max(1, Math.max(heightRatio, widthRatio)));
//            mImageView.resetScale();
		}


        @Override
        public void onActivityCreated(@Nullable Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            setHasOptionsMenu(true);
            mImageView.setOnClickListener(this);
            mImageView.setOnGenericMotionListener(new OnGenericMotionListener() {
                @Override
                public boolean onGenericMotion(View v, MotionEvent event) {
                    final SubsamplingScaleImageView iv = (SubsamplingScaleImageView) v;
                    return false;
                }
            });
            loadImage();
        }


        @Override
        public void onStart() {
            super.onStart();
        }


        @Override
        public void onStop() {
            super.onStop();
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
            final ParcelableMedia media = mMedia[position];
            final Bundle args = new Bundle();
            args.putLong(EXTRA_ACCOUNT_ID, mAccountId);
            args.putParcelable(EXTRA_MEDIA, media);
            switch (media.type) {
                case ParcelableMedia.TYPE_CARD_ANIMATED_GIF: {
                    args.putBoolean(EXTRA_LOOP, true);
                    return Fragment.instantiate(mActivity, VideoPageFragment.class.getName(), args);
                }
                case ParcelableMedia.TYPE_VIDEO: {
                    return Fragment.instantiate(mActivity, VideoPageFragment.class.getName(), args);
                }
                default: {
					return Fragment.instantiate(mActivity, ImagePageFragment.class.getName(), args);
				}
            }
        }

        public void setMedia(long accountId, ParcelableMedia[] media) {
            mAccountId = accountId;
            mMedia = media;
            notifyDataSetChanged();
	    }
    }

    public static final class VideoPageFragment extends BaseSupportFragment
            implements VideoLoadingListener, OnPreparedListener, OnErrorListener, OnCompletionListener {

        private static final String[] SUPPORTED_VIDEO_TYPES;

        static {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                SUPPORTED_VIDEO_TYPES = new String[]{"video/mp4"};
            } else {
                SUPPORTED_VIDEO_TYPES = new String[]{"video/webm", "video/mp4"};
            }
        }

        private VideoLoader mVideoLoader;

        private TextureVideoView mVideoView;
        private ProgressBar mVideoViewProgress;

        private boolean mPlayAudio;
        private VideoPlayProgressRunnable mVideoProgressRunnable;
        private SaveFileTask mSaveFileTask;
        private File mVideoFile;
        private Pair<String, String> mVideoUrlAndType;

        public boolean isLoopEnabled() {
            return getArguments().getBoolean(EXTRA_LOOP, false);
        }

        public void loadVideo() {
            Pair<String, String> urlAndType = getBestVideoUrlAndType(getMedia());
            if (urlAndType == null) return;
            mVideoUrlAndType = urlAndType;
            mVideoLoader.loadVideo(urlAndType.first, this);
        }

        @Override
        public void onCompletion(MediaPlayer mp) {
//            mVideoViewProgress.removeCallbacks(mVideoProgressRunnable);
//            mVideoViewProgress.setVisibility(View.GONE);
        }

        @Override
        public void onBaseViewCreated(View view, Bundle savedInstanceState) {
            super.onBaseViewCreated(view, savedInstanceState);
            mVideoView = (TextureVideoView) view.findViewById(R.id.video_view);
            mVideoViewProgress = (ProgressBar) view.findViewById(R.id.video_view_progress);
        }

        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            mVideoViewProgress.removeCallbacks(mVideoProgressRunnable);
            mVideoViewProgress.setVisibility(View.GONE);
            return true;
        }

        @Override
        public void onPrepared(MediaPlayer mp) {
            if (getUserVisibleHint()) {
                mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
                if (mPlayAudio) {
                    mp.setVolume(1, 1);
                } else {
                    mp.setVolume(0, 0);
                }
                mp.setLooping(isLoopEnabled());
                mp.start();
                mVideoViewProgress.setVisibility(View.VISIBLE);
                mVideoViewProgress.post(mVideoProgressRunnable);
            }
        }

        @Override
        public void onActivityCreated(@Nullable Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            setHasOptionsMenu(true);
            mVideoLoader = TwittnukerApplication.getInstance(getActivity()).getVideoLoader();
            mVideoProgressRunnable = new VideoPlayProgressRunnable(mVideoViewProgress.getHandler(),
                    mVideoViewProgress, mVideoView);

            mVideoView.setOnPreparedListener(this);
            mVideoView.setOnErrorListener(this);
            mVideoView.setOnCompletionListener(this);

            loadVideo();
        }

        @Override
        public void onVideoLoadingCancelled(String uri, VideoLoadingListener listener) {
            invalidateOptionsMenu();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_media_page_video, container, false);
        }

        @Override
        public void onVideoLoadingComplete(String uri, VideoLoadingListener listener, File file) {
            mVideoView.setVideoURI(Uri.fromFile(file));
            mVideoFile = file;
            invalidateOptionsMenu();
        }

        @Override
        public void onVideoLoadingFailed(String uri, VideoLoadingListener listener, Exception e) {
            invalidateOptionsMenu();
        }

        @Override
        public void onVideoLoadingProgressUpdate(String uri, VideoLoadingListener listener, int current, int total) {

        }

        @Override
        public void onPrepareOptionsMenu(Menu menu) {
            super.onPrepareOptionsMenu(menu);
            final File file = mVideoFile;
            final Pair<String, String> linkAndType = mVideoUrlAndType;
            final boolean isLoading = getLoaderManager().hasRunningLoaders();
            final boolean hasVideo = file != null && file.exists() && linkAndType != null;
            MenuUtils.setMenuItemAvailability(menu, R.id.refresh, !hasVideo && !isLoading);
            MenuUtils.setMenuItemAvailability(menu, R.id.share, hasVideo && !isLoading);
            MenuUtils.setMenuItemAvailability(menu, R.id.save, hasVideo && !isLoading);
            if (!hasVideo) return;
            final MenuItem shareItem = menu.findItem(R.id.share);
            final ShareActionProvider shareProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(shareItem);
            final Intent intent = new Intent(Intent.ACTION_SEND);
            final Uri fileUri = Uri.fromFile(file);
            intent.setDataAndType(fileUri, linkAndType.second);
            intent.putExtra(Intent.EXTRA_STREAM, fileUri);
            final MediaViewerActivity activity = (MediaViewerActivity) getActivity();
            if (activity.hasStatus()) {
                final ParcelableStatus status = activity.getStatus();
                intent.putExtra(Intent.EXTRA_TEXT, Utils.getStatusShareText(activity, status));
                intent.putExtra(Intent.EXTRA_SUBJECT, Utils.getStatusShareSubject(activity, status));
            }
            shareProvider.setShareIntent(intent);
        }

        @Override
        public void onVideoLoadingStarted(String uri, VideoLoadingListener listener) {
            invalidateOptionsMenu();
        }

        private Pair<String, String> getBestVideoUrlAndType(ParcelableMedia media) {
            if (media == null) return null;
            switch (media.type) {
                case ParcelableMedia.TYPE_VIDEO: {
                    if (media.video_info == null) return null;
                    for (String supportedType : SUPPORTED_VIDEO_TYPES) {
                        for (Variant variant : media.video_info.variants) {
                            if (supportedType.equalsIgnoreCase(variant.content_type))
                                return new Pair<>(variant.url, variant.content_type);
                        }
                    }
                    return null;
                }
                case ParcelableMedia.TYPE_CARD_ANIMATED_GIF: {
                    return new Pair<>(media.media_url, "video/mp4");
                }
                default: {
                    return null;
                }
            }
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            inflater.inflate(R.menu.menu_media_viewer_video_page, menu);
        }

        private ParcelableMedia getMedia() {
            final Bundle args = getArguments();
            return args.getParcelable(EXTRA_MEDIA);
        }

        private void saveToGallery() {
            if (mSaveFileTask != null && mSaveFileTask.getStatus() == Status.RUNNING) return;
            final File file = mVideoFile;
            final Pair<String, String> urlAndType = mVideoUrlAndType;
            final boolean hasVideo = file != null && file.exists() && urlAndType != null;
            if (!hasVideo) return;
            final String mimeType = urlAndType.second;
            final MimeTypeMap map = MimeTypeMap.getSingleton();
            final String extension = map.getExtensionFromMimeType(mimeType);
            if (extension == null) return;
            final File pubDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
            final File saveDir = new File(pubDir, "Twidere");
            mSaveFileTask = AsyncTaskUtils.executeTask(new SaveFileTask(getActivity(), file, mimeType, saveDir));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            switch (item.getItemId()) {
                case MENU_SAVE: {
                    saveToGallery();
                    return true;
                }
                case MENU_REFRESH: {
                    loadVideo();
                    return true;
                }
            }
            return super.onOptionsItemSelected(item);
        }

        private static class VideoPlayProgressRunnable implements Runnable {

            private final Handler mHandler;
            private final ProgressBar mProgressBar;
            private final MediaController.MediaPlayerControl mMediaPlayerControl;

            VideoPlayProgressRunnable(Handler handler, ProgressBar progressBar,
                                      MediaController.MediaPlayerControl mediaPlayerControl) {
                mHandler = handler;
                mProgressBar = progressBar;
                mMediaPlayerControl = mediaPlayerControl;
                mProgressBar.setMax(1000);
            }

            @Override
            public void run() {
                final int duration = mMediaPlayerControl.getDuration();
                final int position = mMediaPlayerControl.getCurrentPosition();
                if (duration <= 0 || position < 0) return;
                mProgressBar.setProgress(Math.round(1000 * position / (float) duration));
                mHandler.postDelayed(this, 16);
            }
        }


        @Override
        public void onResume() {
            super.onResume();
        }

        @Override
        public void onDestroyView() {
            super.onDestroyView();
        }

        @Override
        public void onPause() {
            super.onPause();
        }

        @Override
        public void setUserVisibleHint(boolean isVisibleToUser) {
            super.setUserVisibleHint(isVisibleToUser);
            if (!isVisibleToUser && mVideoView != null && mVideoView.isPlaying()) {
                mVideoView.pause();
            }
        }


    }
}