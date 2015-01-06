/*
 * Twittnuker - Twitter client for Android
 *
 * Copyright (C) 2013-2015 vanita5 <mail@vanita5.de>
 *
 * This program incorporates a modified version of Twidere.
 * Copyright (C) 2012-2014 Mariotaku Lee <mariotaku.lee@gmail.com>
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

package de.vanita5.twittnuker.activity.support;

import android.app.ActionBar;
import android.app.ActionBar.OnMenuVisibilityListener;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnLayoutChangeListener;
import android.widget.ImageView;
import android.widget.ProgressBar;

import org.mariotaku.menucomponent.widget.MenuBar;
import org.mariotaku.menucomponent.widget.MenuBar.MenuBarListener;
import de.vanita5.twittnuker.Constants;
import de.vanita5.twittnuker.R;
import de.vanita5.twittnuker.loader.support.TileImageLoader;
import org.mariotaku.tileimageview.widget.TileImageView;
import de.vanita5.twittnuker.util.SaveImageTask;
import de.vanita5.twittnuker.util.ThemeUtils;
import de.vanita5.twittnuker.util.Utils;

import java.io.File;

public final class MediaViewerActivity extends BaseSupportActivity implements Constants,
		TileImageLoader.DownloadListener, LoaderManager.LoaderCallbacks<TileImageLoader.Result>,
		OnMenuVisibilityListener, MenuBarListener {


	private ActionBar mActionBar;

	private ProgressBar mProgress;
	private ImageView mImageView;
	private MenuBar mMenuBar;

	private long mContentLength;

	private File mImageFile;
	private boolean mLoaderInitialized;

	@Override
	public int getThemeResourceId() {
		return ThemeUtils.getViewerThemeResource(this);
	}


	@Override
	public void onContentChanged() {
		super.onContentChanged();
		mImageView = (ImageView) findViewById(R.id.image_viewer);
		mProgress = (ProgressBar) findViewById(R.id.progress);
		mMenuBar = (MenuBar) findViewById(R.id.menu_bar);
	}

	@Override
	public Loader<TileImageLoader.Result> onCreateLoader(final int id, final Bundle args) {
		mProgress.setVisibility(View.VISIBLE);
		mProgress.setIndeterminate(true);
		invalidateOptionsMenu();
		final Uri uri = args.getParcelable(EXTRA_URI);
		final long accountId = args.getLong(EXTRA_ACCOUNT_ID, -1);
		return new TileImageLoader(this, this, accountId, uri);
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		getMenuInflater().inflate(R.menu.menu_image_viewer_action_bar, menu);
		return true;
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
		mProgress.setIndeterminate(total <= 0);
		mProgress.setMax(total > 0 ? (int) (total / 1024) : 0);
	}


	@Override
	public void onLoaderReset(final Loader<TileImageLoader.Result> loader) {

	}

	@Override
	public void onLoadFinished(final Loader<TileImageLoader.Result> loader, final TileImageLoader.Result data) {
		if (data.hasData()) {
			mImageView.setVisibility(View.VISIBLE);
//            mImageView.setBitmapRegionDecoder(data.decoder, data.bitmap);
//            mImageView.setScale(1);
			mImageView.setImageBitmap(data.bitmap);
			mImageFile = data.file;
		} else {
			mImageView.setVisibility(View.GONE);
			mImageFile = null;
			Utils.showErrorMessage(this, null, data.exception, true);
		}
		mProgress.setVisibility(View.GONE);
		mProgress.setProgress(0);
		invalidateOptionsMenu();
		updateShareIntent();
	}

	@Override
	public boolean onMenuItemClick(final MenuItem item) {
		switch (item.getItemId()) {
			case MENU_SAVE: {
				if (mImageFile != null) {
					new SaveImageTask(this, mImageFile).execute();
				}
				break;
			}
			case MENU_OPEN_IN_BROWSER: {
				final Intent intent = getIntent();
				intent.setExtrasClassLoader(getClassLoader());
				final Uri uri = intent.getData();
				final Uri orig = intent.getParcelableExtra(EXTRA_URI_ORIG);
				final Uri uriPreferred = orig != null ? orig : uri;
				if (uriPreferred == null) return false;
				final String scheme = uriPreferred.getScheme();
				if ("http".equals(scheme) || "https".equals(scheme)) {
					final Intent open_intent = new Intent(Intent.ACTION_VIEW, uriPreferred);
					open_intent.addCategory(Intent.CATEGORY_BROWSABLE);
					try {
						startActivity(open_intent);
					} catch (final ActivityNotFoundException e) {
						// Ignore.
					}
				}
				break;
			}
			default: {
				final Intent intent = item.getIntent();
				if (intent != null) {
					try {
						startActivity(intent);
					} catch (final ActivityNotFoundException e) {
						// Ignore.
					}
					return true;
				}
				return false;
			}
		}
		return true;
	}

	@Override
	public void onMenuVisibilityChanged(final boolean isVisible) {
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
			case MENU_HOME: {
				onBackPressed();
				break;
			}
			case MENU_REFRESH: {
				loadImage();
				break;
			}
		}
		return true;
	}


	@Override
	public boolean onPrepareOptionsMenu(final Menu menu) {
		final LoaderManager lm = getSupportLoaderManager();
		Utils.setMenuItemAvailability(menu, MENU_REFRESH, !lm.hasRunningLoaders());
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public void onProgressUpdate(final long downloaded) {
		if (mContentLength == 0) {
			mProgress.setIndeterminate(true);
			return;
		}
		mProgress.setIndeterminate(false);
		mProgress.setProgress((int) (downloaded / 1024));
	}


	public void showProgress() {
		mProgress.setVisibility(View.VISIBLE);
		mProgress.setIndeterminate(true);
	}

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.image_viewer);
		mActionBar = getActionBar();
		mActionBar.setDisplayHomeAsUpEnabled(true);
		mActionBar.addOnMenuVisibilityListener(this);
		if (savedInstanceState == null) {
			loadImage();
		}

//        mImageView.setScaleToFit(false);
		mImageView.addOnLayoutChangeListener(new TileImageViewLayoutListener());

		mMenuBar.setMenuBarListener(this);
		mMenuBar.inflate(R.menu.menu_image_viewer);
		mMenuBar.setIsBottomBar(true);
		mMenuBar.show();
	}


	private static class TileImageViewLayoutListener implements OnLayoutChangeListener {
		@Override
		public void onLayoutChange(final View v, final int left, final int top, final int right, final int bottom,
								   final int oldLeft, final int oldTop, final int oldRight, final int oldBottom) {
			if (!(v instanceof TileImageView)) return;
			final TileImageView tileView = (TileImageView) v;
			final int baseWidth = tileView.getBaseWidth(), baseHeight = tileView.getBaseHeight();
			final double scaleMin = getMinScale(left, top, right, bottom, baseWidth, baseHeight);
			tileView.setScaleLimits(scaleMin, Math.max(scaleMin, 2.0));
			final double oldScaleMin = getMinScale(oldLeft, oldTop, oldRight, oldBottom, baseWidth, baseHeight);
			final double oldScale = tileView.getScale();
			tileView.setScaleLimits(scaleMin, Math.max(scaleMin, 2.0));
			if (oldScale == oldScaleMin) {
				tileView.setScale(scaleMin);
			}
		}

		private static double getMinScale(final int left, final int top, final int right, final int bottom,
										  final int baseWidth, final int baseHeight) {
			final double viewWidth = right - left, viewHeight = bottom - top;
			if (viewWidth <= 0 || viewHeight <= 0) return 0;
			final double widthScale = Math.min(1, baseWidth / viewWidth), heightScale = Math.min(1, baseHeight
					/ viewHeight);
			return Math.min(widthScale, heightScale);
		}

	}

	@Override
	protected void onDestroy() {
		mActionBar.removeOnMenuVisibilityListener(this);
		super.onDestroy();

	}

	@Override
	protected void onNewIntent(final Intent intent) {
		setIntent(intent);
		loadImage();
	}

	@Override
	protected void onPause() {
		super.onPause();

	}

	@Override
	protected void onResume() {
		super.onResume();

	}


	private void loadImage() {
		getSupportLoaderManager().destroyLoader(0);
		final Intent intent = getIntent();
		final Uri uri = intent.getData();
		final long accountId = intent.getLongExtra(EXTRA_ACCOUNT_ID, -1);
		if (uri == null) {
			finish();
			return;
		}
		final Bundle args = new Bundle();
		args.putParcelable(EXTRA_URI, uri);
		args.putLong(EXTRA_ACCOUNT_ID, accountId);
		if (!mLoaderInitialized) {
			getSupportLoaderManager().initLoader(0, args, this);
			mLoaderInitialized = true;
		} else {
			getSupportLoaderManager().restartLoader(0, args, this);
		}
	}


	void updateShareIntent() {
		final MenuItem item = mMenuBar.getMenu().findItem(MENU_SHARE);
		if (item == null || !item.hasSubMenu()) return;
		final SubMenu subMenu = item.getSubMenu();
		subMenu.clear();
		final Intent intent = getIntent();
		final Uri uri = intent.getData();
		final Intent shareIntent = new Intent(Intent.ACTION_SEND);
		if (mImageFile != null && mImageFile.exists()) {
			shareIntent.setType("image/*");
			shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(mImageFile));
		} else {
			shareIntent.setType("text/plain");
			shareIntent.putExtra(Intent.EXTRA_TEXT, uri.toString());
		}
		Utils.addIntentToMenu(this, subMenu, shareIntent);
	}

	@Override
	public void onPreShowMenu(Menu menu) {

	}

	@Override
	public void onPostShowMenu(Menu menu) {

	}

}