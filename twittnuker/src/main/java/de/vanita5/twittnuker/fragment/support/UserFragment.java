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

package de.vanita5.twittnuker.fragment.support;

import android.animation.ArgbEvaluator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter.CreateNdefMessageCallback;
import android.nfc.NfcEvent;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.app.SharedElementCallback;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.ActionMenuView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.mariotaku.querybuilder.Expression;
import de.vanita5.twittnuker.R;
import de.vanita5.twittnuker.activity.iface.IThemedActivity;
import de.vanita5.twittnuker.activity.support.AccountSelectorActivity;
import de.vanita5.twittnuker.activity.support.ColorPickerDialogActivity;
import de.vanita5.twittnuker.activity.support.LinkHandlerActivity;
import de.vanita5.twittnuker.activity.support.ThemedActionBarActivity;
import de.vanita5.twittnuker.activity.support.UserListSelectorActivity;
import de.vanita5.twittnuker.activity.support.UserProfileEditorActivity;
import de.vanita5.twittnuker.adapter.support.SupportTabsAdapter;
import de.vanita5.twittnuker.app.TwittnukerApplication;
import de.vanita5.twittnuker.fragment.iface.IBaseFragment.SystemWindowsInsetsCallback;
import de.vanita5.twittnuker.fragment.iface.RefreshScrollTopInterface;
import de.vanita5.twittnuker.fragment.iface.SupportFragmentCallback;
import de.vanita5.twittnuker.graphic.ActionBarColorDrawable;
import de.vanita5.twittnuker.graphic.ActionIconDrawable;
import de.vanita5.twittnuker.loader.support.ParcelableUserLoader;
import de.vanita5.twittnuker.model.ParcelableAccount.ParcelableCredentials;
import de.vanita5.twittnuker.model.ParcelableMedia;
import de.vanita5.twittnuker.model.ParcelableUser;
import de.vanita5.twittnuker.model.ParcelableUserList;
import de.vanita5.twittnuker.model.SingleResponse;
import de.vanita5.twittnuker.model.SupportTabSpec;
import de.vanita5.twittnuker.provider.TwidereDataStore.CachedUsers;
import de.vanita5.twittnuker.provider.TwidereDataStore.Filters;
import de.vanita5.twittnuker.util.AsyncTwitterWrapper;
import de.vanita5.twittnuker.util.ColorUtils;
import de.vanita5.twittnuker.util.ContentValuesCreator;
import de.vanita5.twittnuker.util.KeyboardShortcutsHandler;
import de.vanita5.twittnuker.util.KeyboardShortcutsHandler.KeyboardShortcutCallback;
import de.vanita5.twittnuker.util.LinkCreator;
import de.vanita5.twittnuker.util.MathUtils;
import de.vanita5.twittnuker.util.MediaLoaderWrapper;
import de.vanita5.twittnuker.util.MenuUtils;
import de.vanita5.twittnuker.util.ParseUtils;
import de.vanita5.twittnuker.util.ThemeUtils;
import de.vanita5.twittnuker.util.TwidereLinkify;
import de.vanita5.twittnuker.util.TwidereLinkify.OnLinkClickListener;
import de.vanita5.twittnuker.util.UserColorNameUtils;
import de.vanita5.twittnuker.util.Utils;
import de.vanita5.twittnuker.util.ViewUtils;
import de.vanita5.twittnuker.util.accessor.ActivityAccessor;
import de.vanita5.twittnuker.util.accessor.ActivityAccessor.TaskDescriptionCompat;
import de.vanita5.twittnuker.util.menu.TwidereMenuInfo;
import de.vanita5.twittnuker.util.message.FriendshipUpdatedEvent;
import de.vanita5.twittnuker.util.message.ProfileUpdatedEvent;
import de.vanita5.twittnuker.util.message.TaskStateChangedEvent;
import de.vanita5.twittnuker.view.ColorLabelRelativeLayout;
import de.vanita5.twittnuker.view.HeaderDrawerLayout;
import de.vanita5.twittnuker.view.HeaderDrawerLayout.DrawerCallback;
import de.vanita5.twittnuker.view.ProfileBannerImageView;
import de.vanita5.twittnuker.view.ShapedImageView;
import de.vanita5.twittnuker.view.TabPagerIndicator;
import de.vanita5.twittnuker.view.TintedStatusFrameLayout;
import de.vanita5.twittnuker.view.iface.IExtendedView.OnSizeChangedListener;

import java.util.List;
import java.util.Locale;

import twitter4j.Relationship;
import twitter4j.Twitter;
import twitter4j.TwitterException;

public class UserFragment extends BaseSupportFragment implements OnClickListener,
		OnLinkClickListener, OnSizeChangedListener, OnSharedPreferenceChangeListener,
        OnTouchListener, DrawerCallback, SupportFragmentCallback, SystemWindowsInsetsCallback,
        RefreshScrollTopInterface, OnPageChangeListener, KeyboardShortcutCallback {

    private static final ArgbEvaluator sArgbEvaluator = new ArgbEvaluator();

	public static final String TRANSITION_NAME_PROFILE_IMAGE = "profile_image";
	public static final String TRANSITION_NAME_PROFILE_TYPE = "profile_type";
    public static final String TRANSITION_NAME_CARD = "card";

	private static final int LOADER_ID_USER = 1;
	private static final int LOADER_ID_FRIENDSHIP = 2;

    private static final int TAB_POSITION_STATUSES = 0;
    private static final int TAB_POSITION_MEDIA = 1;
    private static final int TAB_POSITION_FAVORITES = 2;
    private static final String TAB_TYPE_STATUSES = "statuses";
    private static final String TAB_TYPE_MEDIA = "media";
    private static final String TAB_TYPE_FAVORITES = "favorites";

	private MediaLoaderWrapper mProfileImageLoader;
    private ShapedImageView mProfileImageView;
	private ImageView mProfileTypeView;
	private ProfileBannerImageView mProfileBannerView;
	private TextView mNameView, mScreenNameView, mDescriptionView, mLocationView, mURLView, mCreatedAtView,
			mTweetCount, mFollowersCount, mFriendsCount, mErrorMessageView;
	private View mDescriptionContainer, mLocationContainer, mURLContainer, mTweetsContainer, mFollowersContainer,
			mFriendsContainer;
	private Button mRetryButton;
    private ColorLabelRelativeLayout mProfileNameContainer;
	private View mProgressContainer, mErrorRetryContainer;
    private View mCardContent;
	private View mProfileBannerSpace;
    private TintedStatusFrameLayout mTintedStatusContent;
    private HeaderDrawerLayout mHeaderDrawerLayout;
	private ViewPager mViewPager;
    private TabPagerIndicator mPagerIndicator;

    private View mProfileBannerContainer;
    private Button mFollowButton;
    private ProgressBar mFollowProgress;
    private View mPagesContent, mPagesErrorContainer;
    private ImageView mPagesErrorIcon;
    private TextView mPagesErrorText;
    private View mProfileNameBackground;
    private View mProfileDetailsContainer;
    private Relationship mRelationship;

	private SupportTabsAdapter mPagerAdapter;
    private KeyboardShortcutsHandler mKeyboardShortcutsHandler;

	private ParcelableUser mUser = null;
    private Locale mLocale;
    private boolean mGetUserInfoLoaderInitialized, mGetFriendShipLoaderInitialized;
    private int mBannerWidth;
    private int mCardBackgroundColor;
    private int mActionBarShadowColor;
    private int mUserUiColor;

    private ActionBarDrawable mActionBarBackground;
    private Drawable mActionBarHomeAsUpIndicator;


	private final LoaderCallbacks<SingleResponse<Relationship>> mFriendshipLoaderCallbacks = new LoaderCallbacks<SingleResponse<Relationship>>() {

		@Override
		public Loader<SingleResponse<Relationship>> onCreateLoader(final int id, final Bundle args) {
			invalidateOptionsMenu();
            mFollowButton.setVisibility(View.GONE);
            mFollowProgress.setVisibility(View.VISIBLE);
			final long accountId = args.getLong(EXTRA_ACCOUNT_ID, -1);
			final long userId = args.getLong(EXTRA_USER_ID, -1);
            return new RelationshipLoader(getActivity(), accountId, userId);
		}

		@Override
		public void onLoaderReset(final Loader<SingleResponse<Relationship>> loader) {

		}

		@Override
		public void onLoadFinished(final Loader<SingleResponse<Relationship>> loader,
								   final SingleResponse<Relationship> data) {
            mFollowProgress.setVisibility(View.GONE);
            final ParcelableUser user = getUser();
            final Relationship relationship = data.getData();
            mRelationship = relationship;
			if (user == null) return;
			invalidateOptionsMenu();
            final boolean isMyself = user.account_id == user.id;
            if (isMyself) {
                mFollowButton.setText(R.string.edit);
                mFollowButton.setVisibility(View.VISIBLE);
            } else if (relationship != null) {
                final int drawableRes;
                mFollowButton.setEnabled(!relationship.isSourceBlockedByTarget());
                if (relationship.isSourceBlockedByTarget()) {
                    mPagesErrorContainer.setVisibility(View.VISIBLE);
                    final String displayName = UserColorNameUtils.getDisplayName(getActivity(), user);
                    mPagesErrorText.setText(getString(R.string.blocked_by_user_summary, displayName));
                    mPagesErrorIcon.setImageResource(R.drawable.ic_info_error_generic);
                    mPagesContent.setVisibility(View.GONE);
                } else if (!relationship.isSourceFollowingTarget() && user.is_protected) {
                    mPagesErrorContainer.setVisibility(View.VISIBLE);
                    final String displayName = UserColorNameUtils.getDisplayName(getActivity(), user);
                    mPagesErrorText.setText(getString(R.string.user_protected_summary, displayName));
                    mPagesErrorIcon.setImageResource(R.drawable.ic_info_locked);
                    mPagesContent.setVisibility(View.GONE);
                } else {
                    mPagesErrorContainer.setVisibility(View.GONE);
                    mPagesErrorText.setText(null);
                    mPagesContent.setVisibility(View.VISIBLE);
                }
                if (relationship.isSourceBlockingTarget()) {
                    mFollowButton.setText(R.string.unblock);
                    drawableRes = R.drawable.ic_follow_blocked;
                } else if (relationship.isSourceFollowingTarget()) {
                    mFollowButton.setText(R.string.unfollow);
                    if (relationship.isTargetFollowingSource()) {
                        drawableRes = R.drawable.ic_follow_bidirectional;
                    } else {
                        drawableRes = R.drawable.ic_follow_outgoing;
                    }
                } else if (user.is_follow_request_sent) {
                    mFollowButton.setText(R.string.requested);
                    if (relationship.isTargetFollowingSource()) {
                        drawableRes = R.drawable.ic_follow_incoming;
                    } else {
                        drawableRes = R.drawable.ic_follow_requested;
                    }
                } else {
                    mFollowButton.setText(R.string.follow);
                    if (relationship.isTargetFollowingSource()) {
                        drawableRes = R.drawable.ic_follow_incoming;
                    } else {
                        drawableRes = R.drawable.ic_follow_none;
                    }
                }
                final Drawable icon = ResourcesCompat.getDrawable(getResources(), drawableRes, null);
                final int iconSize = Math.round(mFollowButton.getTextSize() * 1.4f);
                icon.setBounds(0, 0, iconSize, iconSize);
                icon.setColorFilter(mFollowButton.getCurrentTextColor(), Mode.SRC_ATOP);
                mFollowButton.setCompoundDrawables(icon, null, null, null);
                mFollowButton.setCompoundDrawablePadding(Math.round(mFollowButton.getTextSize() * 0.25f));

				final ContentResolver resolver = getContentResolver();
                final ContentValues cachedValues = ParcelableUser.makeCachedUserContentValues(user);
                resolver.insert(CachedUsers.CONTENT_URI, cachedValues);
                mFollowButton.setVisibility(View.VISIBLE);
			} else {
                mFollowButton.setText(null);
                mFollowButton.setVisibility(View.GONE);
                mPagesErrorContainer.setVisibility(View.GONE);
                mPagesContent.setVisibility(View.VISIBLE);
//                mFollowingYouIndicator.setVisibility(View.GONE);
			}
		}

	};

    private final LoaderCallbacks<SingleResponse<ParcelableUser>> mUserInfoLoaderCallbacks = new LoaderCallbacks<SingleResponse<ParcelableUser>>() {

        @Override
        public Loader<SingleResponse<ParcelableUser>> onCreateLoader(final int id, final Bundle args) {
            final boolean omitIntentExtra = args.getBoolean(EXTRA_OMIT_INTENT_EXTRA, true);
            final long accountId = args.getLong(EXTRA_ACCOUNT_ID, -1);
            final long userId = args.getLong(EXTRA_USER_ID, -1);
            final String screenName = args.getString(EXTRA_SCREEN_NAME);
            if (mUser == null && (!omitIntentExtra || !args.containsKey(EXTRA_USER))) {
                mCardContent.setVisibility(View.GONE);
                mErrorRetryContainer.setVisibility(View.GONE);
                mProgressContainer.setVisibility(View.VISIBLE);
                mErrorMessageView.setText(null);
                mErrorMessageView.setVisibility(View.GONE);
                setListShown(false);
            }
            setProgressBarIndeterminateVisibility(true);
            final ParcelableUser user = mUser;
            return new ParcelableUserLoader(getActivity(), accountId, userId, screenName, getArguments(),
                    omitIntentExtra, user == null || !user.is_cache && userId != user.id);
        }

        @Override
        public void onLoaderReset(final Loader<SingleResponse<ParcelableUser>> loader) {

        }

        @Override
        public void onLoadFinished(final Loader<SingleResponse<ParcelableUser>> loader,
                                   final SingleResponse<ParcelableUser> data) {
            if (getActivity() == null) return;
            if (data.hasData()) {
                final ParcelableUser user = data.getData();
                mCardContent.setVisibility(View.VISIBLE);
                mErrorRetryContainer.setVisibility(View.GONE);
                mProgressContainer.setVisibility(View.GONE);
                setListShown(true);
                displayUser(user);
                if (user.is_cache) {
                    final Bundle args = new Bundle();
                    args.putLong(EXTRA_ACCOUNT_ID, user.account_id);
                    args.putLong(EXTRA_USER_ID, user.id);
                    args.putString(EXTRA_SCREEN_NAME, user.screen_name);
                    args.putBoolean(EXTRA_OMIT_INTENT_EXTRA, true);
                    getLoaderManager().restartLoader(LOADER_ID_USER, args, this);
                }
            } else if (mUser != null && mUser.is_cache) {
                mCardContent.setVisibility(View.VISIBLE);
                mErrorRetryContainer.setVisibility(View.GONE);
                mProgressContainer.setVisibility(View.GONE);
                setListShown(true);
                displayUser(mUser);
            } else {
                if (data.hasException()) {
                    mErrorMessageView.setText(Utils.getErrorMessage(getActivity(), data.getException()));
                    mErrorMessageView.setVisibility(View.VISIBLE);
                }
                mCardContent.setVisibility(View.GONE);
                mErrorRetryContainer.setVisibility(View.VISIBLE);
                mProgressContainer.setVisibility(View.GONE);
            }
            setProgressBarIndeterminateVisibility(false);
        }

    };

    @Override
    public boolean canScroll(float dy) {
        final Fragment fragment = getCurrentVisibleFragment();
        return fragment instanceof DrawerCallback && ((DrawerCallback) fragment).canScroll(dy);
    }

    @Override
    public void cancelTouch() {
        final Fragment fragment = getCurrentVisibleFragment();
        if (fragment instanceof DrawerCallback) {
            ((DrawerCallback) fragment).cancelTouch();
        }
    }

    @Override
    public void fling(float velocity) {
        final Fragment fragment = getCurrentVisibleFragment();
        if (fragment instanceof DrawerCallback) {
            ((DrawerCallback) fragment).fling(velocity);
        }
    }

    @Override
    public boolean isScrollContent(float x, float y) {
        final ViewPager v = mViewPager;
        final int[] location = new int[2];
        v.getLocationInWindow(location);
        return x >= location[0] && x <= location[0] + v.getWidth()
                && y >= location[1] && y <= location[1] + v.getHeight();
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        updateSubtitle();
    }

    private void updateSubtitle() {
        final ActionBarActivity activity = (ActionBarActivity) getActivity();
        if (activity == null) return;
        final ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar == null) return;
        final ParcelableUser user = mUser;
        if (user == null) {
            actionBar.setSubtitle(null);
            return;
        }
        final SupportTabSpec spec = mPagerAdapter.getTab(mViewPager.getCurrentItem());
        switch (spec.type) {
            case TAB_TYPE_STATUSES: {
                actionBar.setSubtitle(getResources().getQuantityString(R.plurals.N_statuses, user.statuses_count, user.statuses_count));
                break;
            }
            case TAB_TYPE_MEDIA: {
                actionBar.setSubtitle(getResources().getQuantityString(R.plurals.N_media, user.media_count, user.media_count));
                break;
            }
            case TAB_TYPE_FAVORITES: {
                actionBar.setSubtitle(getResources().getQuantityString(R.plurals.N_favorites, user.favorites_count, user.favorites_count));
                break;
            }
            default: {
                actionBar.setSubtitle(null);
                break;
            }
        }
        updateTitleAlpha();
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    @Override
    public void scrollBy(float dy) {
        final Fragment fragment = getCurrentVisibleFragment();
        if (fragment instanceof DrawerCallback) {
            ((DrawerCallback) fragment).scrollBy(dy);
        }
    }

    @Override
    public boolean shouldLayoutHeaderBottom() {
        final HeaderDrawerLayout drawer = mHeaderDrawerLayout;
        final View card = mProfileDetailsContainer;
        if (drawer == null || card == null) return false;
        return card.getTop() + drawer.getHeaderTop() - drawer.getPaddingTop() <= 0;
    }

    @Override
    public void topChanged(int top) {
        final HeaderDrawerLayout drawer = mHeaderDrawerLayout;
        if (drawer == null) return;
        final int offset = drawer.getPaddingTop() - top;
        updateScrollOffset(offset);

        final Fragment fragment = getCurrentVisibleFragment();
        if (fragment instanceof DrawerCallback) {
            ((DrawerCallback) fragment).topChanged(top);
        }
    }

	public void displayUser(final ParcelableUser user) {
        mUser = user;
        final FragmentActivity activity = getActivity();
        if (user == null || user.id <= 0 || activity == null) return;
        final Resources resources = getResources();
        final Resources res = resources;
		final LoaderManager lm = getLoaderManager();
		lm.destroyLoader(LOADER_ID_USER);
		lm.destroyLoader(LOADER_ID_FRIENDSHIP);
		final boolean userIsMe = user.account_id == user.id;
        mCardContent.setVisibility(View.VISIBLE);
		mErrorRetryContainer.setVisibility(View.GONE);
		mProgressContainer.setVisibility(View.GONE);
		mUser = user;
        final int userColor = UserColorNameUtils.getUserColor(activity, user.id, true);
        mProfileImageView.setBorderColor(userColor != 0 ? userColor : Color.WHITE);
        mProfileNameContainer.drawEnd(Utils.getAccountColor(activity, user.account_id));
		mNameView.setText(user.name);
		final int typeIconRes = Utils.getUserTypeIconRes(user.is_verified, user.is_protected);
		if (typeIconRes != 0) {
			mProfileTypeView.setImageResource(typeIconRes);
			mProfileTypeView.setVisibility(View.VISIBLE);
		} else {
			mProfileTypeView.setImageDrawable(null);
			mProfileTypeView.setVisibility(View.GONE);
		}
		mScreenNameView.setText("@" + user.screen_name);
        mDescriptionContainer.setVisibility(TextUtils.isEmpty(user.description_html) ? View.GONE : View.VISIBLE);
        mDescriptionView.setText(user.description_html != null ? Html.fromHtml(user.description_html) : user.description_plain);
		final TwidereLinkify linkify = new TwidereLinkify(this);
		linkify.applyAllLinks(mDescriptionView, user.account_id, false);
		mDescriptionView.setMovementMethod(null);
        mLocationContainer.setVisibility(TextUtils.isEmpty(user.location) ? View.GONE : View.VISIBLE);
		mLocationView.setText(user.location);
        mURLContainer.setVisibility(TextUtils.isEmpty(user.url) && TextUtils.isEmpty(user.url_expanded) ? View.GONE : View.VISIBLE);
        mURLView.setText(TextUtils.isEmpty(user.url_expanded) ? user.url : user.url_expanded);
		mURLView.setMovementMethod(null);
        final String createdAt = Utils.formatToLongTimeString(activity, user.created_at);
        final float daysSinceCreation = (System.currentTimeMillis() - user.created_at) / 1000 / 60 / 60 / 24;
        final int dailyTweets = Math.round(user.statuses_count / Math.max(1, daysSinceCreation));
		mCreatedAtView.setText(res.getQuantityString(R.plurals.created_at_with_N_tweets_per_day, dailyTweets,
				createdAt, dailyTweets));
		mTweetCount.setText(Utils.getLocalizedNumber(mLocale, user.statuses_count));
		mFollowersCount.setText(Utils.getLocalizedNumber(mLocale, user.followers_count));
		mFriendsCount.setText(Utils.getLocalizedNumber(mLocale, user.friends_count));

        mProfileImageLoader.displayProfileImage(mProfileImageView, Utils.getOriginalTwitterProfileImage(user.profile_image_url));
		if (userColor != 0) {
            setUserUiColor(userColor);
		} else {
            setUserUiColor(user.link_color);
		}
		final int defWidth = res.getDisplayMetrics().widthPixels;
		final int width = mBannerWidth > 0 ? mBannerWidth : defWidth;
		mProfileImageLoader.displayProfileBanner(mProfileBannerView, user.profile_banner_url, width);
        final Relationship relationship = mRelationship;
        if (relationship == null || relationship.getTargetUserId() != user.id) {
			getFriendship();
		}
        activity.setTitle(UserColorNameUtils.getDisplayName(activity, user));

        updateTitleAlpha();
		invalidateOptionsMenu();
        updateSubtitle();
	}

    @Override
    public Fragment getCurrentVisibleFragment() {
        final int currentItem = mViewPager.getCurrentItem();
        if (currentItem < 0 || currentItem >= mPagerAdapter.getCount()) return null;
        return (Fragment) mPagerAdapter.instantiateItem(mViewPager, currentItem);
    }

    @Override
    public boolean triggerRefresh(int position) {
        return false;
    }

    @Override
    public boolean getSystemWindowsInsets(Rect insets) {
        return false;
    }

    public ParcelableUser getUser() {
        return mUser;
    }

	public void getUserInfo(final long accountId, final long userId, final String screenName,
							final boolean omitIntentExtra) {
		final LoaderManager lm = getLoaderManager();
		lm.destroyLoader(LOADER_ID_USER);
		lm.destroyLoader(LOADER_ID_FRIENDSHIP);
		final Bundle args = new Bundle();
		args.putLong(EXTRA_ACCOUNT_ID, accountId);
		args.putLong(EXTRA_USER_ID, userId);
		args.putString(EXTRA_SCREEN_NAME, screenName);
		args.putBoolean(EXTRA_OMIT_INTENT_EXTRA, omitIntentExtra);
		if (!mGetUserInfoLoaderInitialized) {
			lm.initLoader(LOADER_ID_USER, args, mUserInfoLoaderCallbacks);
			mGetUserInfoLoaderInitialized = true;
		} else {
			lm.restartLoader(LOADER_ID_USER, args, mUserInfoLoaderCallbacks);
		}
		if (accountId == -1 || userId == -1 && screenName == null) {
            mCardContent.setVisibility(View.GONE);
			mErrorRetryContainer.setVisibility(View.GONE);
		}
	}

    @Subscribe
    public void notifyFriendshipUpdated(FriendshipUpdatedEvent event) {
        if (!event.user.equals(mUser)) return;
        getFriendship();
    }

    @Subscribe
    public void notifyProfileUpdated(ProfileUpdatedEvent event) {
        final ParcelableUser user = getUser();
        if (user == null || !user.equals(event.user)) return;
        displayUser(event.user);
    }

    @Subscribe
    public void notifyTaskStateChanged(TaskStateChangedEvent event) {
        updateRefreshState();
    }

	@Override
	public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        final ParcelableUser user = getUser();
		switch (requestCode) {
			case REQUEST_SET_COLOR: {
				if (user == null) return;
				if (resultCode == Activity.RESULT_OK) {
					if (data == null) return;
					final int color = data.getIntExtra(EXTRA_COLOR, Color.TRANSPARENT);
                    UserColorNameUtils.setUserColor(getActivity(), mUser.id, color);
				} else if (resultCode == ColorPickerDialogActivity.RESULT_CLEARED) {
                    UserColorNameUtils.clearUserColor(getActivity(), mUser.id);
				}
				break;
			}
			case REQUEST_ADD_TO_LIST: {
				if (user == null) return;
				if (resultCode == Activity.RESULT_OK && data != null) {
					final AsyncTwitterWrapper twitter = getTwitterWrapper();
					final ParcelableUserList list = data.getParcelableExtra(EXTRA_USER_LIST);
					if (list == null || twitter == null) return;
					twitter.addUserListMembersAsync(user.account_id, list.id, user);
				}
				break;
			}
			case REQUEST_SELECT_ACCOUNT: {
				if (user == null) return;
				if (resultCode == Activity.RESULT_OK) {
					if (data == null || !data.hasExtra(EXTRA_ID)) return;
					final long accountId = data.getLongExtra(EXTRA_ID, -1);
                    Utils.openUserProfile(getActivity(), accountId, user.id, user.screen_name, null);
				}
				break;
			}
		}

	}

	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mTintedStatusContent = (TintedStatusFrameLayout) activity.findViewById(R.id.main_content);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_user, container, false);
//        final ViewGroup profileDetailsContainer = (ViewGroup) view.findViewById(R.id.profile_details_container);
//        final boolean isCompact = Utils.isCompactCards(getActivity());
//        if (isCompact) {
//            inflater.inflate(R.layout.layout_user_details_compact, profileDetailsContainer);
//        } else {
//            inflater.inflate(R.layout.layout_user_details, profileDetailsContainer);
//        }
        return view;
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final FragmentActivity activity = getActivity();
        setHasOptionsMenu(true);
        getSharedPreferences(USER_COLOR_PREFERENCES_NAME, Context.MODE_PRIVATE)
                .registerOnSharedPreferenceChangeListener(this);
        mLocale = getResources().getConfiguration().locale;
        mCardBackgroundColor = ThemeUtils.getCardBackgroundColor(activity,
                ThemeUtils.getThemeBackgroundOption(activity),
                ThemeUtils.getUserThemeBackgroundAlpha(activity));
        mActionBarShadowColor = 0xA0000000;
        final TwittnukerApplication app = TwittnukerApplication.getInstance(activity);
        mProfileImageLoader = app.getMediaLoaderWrapper();
        mKeyboardShortcutsHandler = app.getKeyboardShortcutsHandler();
        final Bundle args = getArguments();
        long accountId = -1, userId = -1;
        String screenName = null;
        if (savedInstanceState != null) {
            args.putAll(savedInstanceState);
        } else {
            accountId = args.getLong(EXTRA_ACCOUNT_ID, -1);
            userId = args.getLong(EXTRA_USER_ID, -1);
            screenName = args.getString(EXTRA_SCREEN_NAME);
        }

        Utils.setNdefPushMessageCallback(activity, new CreateNdefMessageCallback() {

            @Override
            public NdefMessage createNdefMessage(NfcEvent event) {
                final ParcelableUser user = getUser();
                if (user == null) return null;
                return new NdefMessage(new NdefRecord[]{
                        NdefRecord.createUri(LinkCreator.getTwitterUserLink(user.screen_name)),
                });
            }
        });

        activity.setEnterSharedElementCallback(new SharedElementCallback() {

            @Override
            public void onSharedElementStart(List<String> sharedElementNames, List<View> sharedElements, List<View> sharedElementSnapshots) {
                final int idx = sharedElementNames.indexOf(TRANSITION_NAME_PROFILE_IMAGE);
                if (idx != -1) {
                    final View view = sharedElements.get(idx);
                    int[] location = new int[2];
                    final RectF bounds = new RectF(0, 0, view.getWidth(), view.getHeight());
                    view.getLocationOnScreen(location);
                    bounds.offsetTo(location[0], location[1]);
                    mProfileImageView.setTransitionSource(bounds);
                }
                super.onSharedElementStart(sharedElementNames, sharedElements, sharedElementSnapshots);
            }

            @Override
            public void onSharedElementEnd(List<String> sharedElementNames, List<View> sharedElements, List<View> sharedElementSnapshots) {
                int idx = sharedElementNames.indexOf(TRANSITION_NAME_PROFILE_IMAGE);
                if (idx != -1) {
                    final View view = sharedElements.get(idx);
                    int[] location = new int[2];
                    final RectF bounds = new RectF(0, 0, view.getWidth(), view.getHeight());
                    view.getLocationOnScreen(location);
                    bounds.offsetTo(location[0], location[1]);
                    mProfileImageView.setTransitionDestination(bounds);
                }
                super.onSharedElementEnd(sharedElementNames, sharedElements, sharedElementSnapshots);
            }

        });

        ViewCompat.setTransitionName(mProfileImageView, TRANSITION_NAME_PROFILE_IMAGE);
        ViewCompat.setTransitionName(mProfileTypeView, TRANSITION_NAME_PROFILE_TYPE);
//        ViewCompat.setTransitionName(mCardView, TRANSITION_NAME_CARD);

        mHeaderDrawerLayout.setDrawerCallback(this);

        mPagerAdapter = new SupportTabsAdapter(activity, getChildFragmentManager());

        mViewPager.setOffscreenPageLimit(3);
        mViewPager.setAdapter(mPagerAdapter);
        mPagerIndicator.setViewPager(mViewPager);
        mPagerIndicator.setTabDisplayOption(TabPagerIndicator.LABEL);
        mPagerIndicator.setOnPageChangeListener(this);

        mFollowButton.setOnClickListener(this);
        mProfileImageView.setOnClickListener(this);
        mProfileBannerView.setOnClickListener(this);
		mTweetsContainer.setOnClickListener(this);
        mFollowersContainer.setOnClickListener(this);
        mFriendsContainer.setOnClickListener(this);
        mRetryButton.setOnClickListener(this);
        mProfileBannerView.setOnSizeChangedListener(this);
        mProfileBannerSpace.setOnTouchListener(this);


        mProfileNameBackground.setBackgroundColor(mCardBackgroundColor);
        mProfileDetailsContainer.setBackgroundColor(mCardBackgroundColor);
        mPagerIndicator.setBackgroundColor(mCardBackgroundColor);

        getUserInfo(accountId, userId, screenName, false);

        final float actionBarElevation = ThemeUtils.getSupportActionBarElevation(activity);
        ViewCompat.setElevation(mPagerIndicator, actionBarElevation);

        setupBaseActionBar();

        setupUserPages();
    }

    @Override
    public void onStart() {
        super.onStart();
        final Bus bus = TwittnukerApplication.getInstance(getActivity()).getMessageBus();
        bus.register(this);
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        outState.putParcelable(EXTRA_USER, getUser());
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onStop() {
        final Bus bus = TwittnukerApplication.getInstance(getActivity()).getMessageBus();
        bus.unregister(this);
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        mUser = null;
        mRelationship = null;
        final LoaderManager lm = getLoaderManager();
        lm.destroyLoader(LOADER_ID_USER);
        lm.destroyLoader(LOADER_ID_FRIENDSHIP);
        super.onDestroyView();
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        inflater.inflate(R.menu.menu_user_profile, menu);
    }

    @Override
    public void onPrepareOptionsMenu(final Menu menu) {
		final AsyncTwitterWrapper twitter = getTwitterWrapper();
        final ParcelableUser user = getUser();
		final Relationship relationship = mRelationship;
		if (twitter == null || user == null) return;
		final boolean isMyself = user.account_id == user.id;
		final MenuItem mentionItem = menu.findItem(MENU_MENTION);
		if (mentionItem != null) {
            mentionItem.setTitle(getString(R.string.mention_user_name, UserColorNameUtils.getDisplayName(getActivity(), user)));
		}
        MenuUtils.setMenuItemAvailability(menu, MENU_MENTION, !isMyself);
        MenuUtils.setMenuItemAvailability(menu, R.id.incoming_friendships, isMyself);
        MenuUtils.setMenuItemAvailability(menu, R.id.saved_searches, isMyself);
//        final MenuItem followItem = menu.findItem(MENU_FOLLOW);
//        followItem.setVisible(!isMyself);
//        final boolean shouldShowFollowItem = !creatingFriendship && !destroyingFriendship && !isMyself
//                && relationship != null;
//        followItem.setEnabled(shouldShowFollowItem);
//        if (shouldShowFollowItem) {
//            followItem.setTitle(isFollowing ? R.string.unfollow : isProtected ? R.string.send_follow_request
//                    : R.string.follow);
//            followItem.setIcon(isFollowing ? R.drawable.ic_action_cancel : R.drawable.ic_action_add);
//        } else {
//            followItem.setTitle(null);
//            followItem.setIcon(null);
//        }
		if (!isMyself && relationship != null) {
            MenuUtils.setMenuItemAvailability(menu, MENU_SEND_DIRECT_MESSAGE, relationship.canSourceDMTarget());
            MenuUtils.setMenuItemAvailability(menu, MENU_BLOCK, true);
            MenuUtils.setMenuItemAvailability(menu, MENU_MUTE_USER, true);
			final MenuItem blockItem = menu.findItem(MENU_BLOCK);
			if (blockItem != null) {
				final boolean blocking = relationship.isSourceBlockingTarget();
                ActionIconDrawable.setMenuHighlight(blockItem, new TwidereMenuInfo(blocking));
				blockItem.setTitle(blocking ? R.string.unblock : R.string.block);
			}
			final MenuItem muteItem = menu.findItem(MENU_MUTE_USER);
			if (muteItem != null) {
				final boolean muting = relationship.isSourceMutingTarget();
                ActionIconDrawable.setMenuHighlight(muteItem, new TwidereMenuInfo(muting));
				muteItem.setTitle(muting ? R.string.unmute : R.string.mute);
			}
			final MenuItem filterItem = menu.findItem(MENU_ADD_TO_FILTER);
			if (filterItem != null) {
				final boolean filtering = Utils.isFilteringUser(getActivity(), user.id);
                ActionIconDrawable.setMenuHighlight(filterItem, new TwidereMenuInfo(filtering));
				filterItem.setTitle(filtering ? R.string.remove_from_filter : R.string.add_to_filter);
			}
		} else {
            MenuUtils.setMenuItemAvailability(menu, MENU_SEND_DIRECT_MESSAGE, false);
            MenuUtils.setMenuItemAvailability(menu, MENU_BLOCK, false);
            MenuUtils.setMenuItemAvailability(menu, MENU_MUTE_USER, false);
            MenuUtils.setMenuItemAvailability(menu, MENU_REPORT_SPAM, false);
		}
        MenuUtils.setMenuItemAvailability(menu, R.id.muted_users, isMyself);
        MenuUtils.setMenuItemAvailability(menu, R.id.blocked_users, isMyself);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
		final AsyncTwitterWrapper twitter = getTwitterWrapper();
        final ParcelableUser user = getUser();
		final Relationship relationship = mRelationship;
		if (user == null || twitter == null) return false;
		switch (item.getItemId()) {
			case MENU_BLOCK: {
				if (mRelationship != null) {
					if (mRelationship.isSourceBlockingTarget()) {
						twitter.destroyBlockAsync(user.account_id, user.id);
					} else {
						CreateUserBlockDialogFragment.show(getFragmentManager(), user);
					}
				}
				break;
			}
			case MENU_REPORT_SPAM: {
				ReportSpamDialogFragment.show(getFragmentManager(), user);
				break;
			}
			case MENU_ADD_TO_FILTER: {
				final boolean filtering = Utils.isFilteringUser(getActivity(), user.id);
				final ContentResolver cr = getContentResolver();
				if (filtering) {
					final Expression where = Expression.equals(Filters.Users.USER_ID, user.id);
					cr.delete(Filters.Users.CONTENT_URI, where.getSQL(), null);
                    Utils.showInfoMessage(getActivity(), R.string.message_user_unmuted, false);
				} else {
                    cr.insert(Filters.Users.CONTENT_URI, ContentValuesCreator.createFilteredUser(user));
                    Utils.showInfoMessage(getActivity(), R.string.message_user_muted, false);
				}
				break;
			}
			case MENU_MUTE_USER: {
				if (mRelationship != null) {
					if (mRelationship.isSourceMutingTarget()) {
						twitter.destroyMuteAsync(user.account_id, user.id);
					} else {
						CreateUserMuteDialogFragment.show(getFragmentManager(), user);
					}
				}
				break;
			}
			case MENU_MENTION: {
				final Intent intent = new Intent(INTENT_ACTION_MENTION);
				final Bundle bundle = new Bundle();
				bundle.putParcelable(EXTRA_USER, user);
				intent.putExtras(bundle);
				startActivity(intent);
				break;
			}
			case MENU_SEND_DIRECT_MESSAGE: {
				final Uri.Builder builder = new Uri.Builder();
				builder.scheme(SCHEME_TWITTNUKER);
				builder.authority(AUTHORITY_DIRECT_MESSAGES_CONVERSATION);
				builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_ID, String.valueOf(user.account_id));
                builder.appendQueryParameter(QUERY_PARAM_USER_ID, String.valueOf(user.id));
                final Intent intent = new Intent(Intent.ACTION_VIEW, builder.build());
                intent.putExtra(EXTRA_ACCOUNT, ParcelableCredentials.getAccount(getActivity(), user.account_id));
                intent.putExtra(EXTRA_USER, user);
                startActivity(intent);
				break;
			}
			case MENU_SET_COLOR: {
				final Intent intent = new Intent(getActivity(), ColorPickerDialogActivity.class);
                intent.putExtra(EXTRA_COLOR, UserColorNameUtils.getUserColor(getActivity(), user.id, true));
				intent.putExtra(EXTRA_ALPHA_SLIDER, false);
				intent.putExtra(EXTRA_CLEAR_BUTTON, true);
				startActivityForResult(intent, REQUEST_SET_COLOR);
				break;
			}
			case MENU_ADD_TO_LIST: {
				final Intent intent = new Intent(INTENT_ACTION_SELECT_USER_LIST);
				intent.setClass(getActivity(), UserListSelectorActivity.class);
				intent.putExtra(EXTRA_ACCOUNT_ID, user.account_id);
                intent.putExtra(EXTRA_SCREEN_NAME, Utils.getAccountScreenName(getActivity(), user.account_id));
				startActivityForResult(intent, REQUEST_ADD_TO_LIST);
				break;
			}
			case MENU_OPEN_WITH_ACCOUNT: {
				final Intent intent = new Intent(INTENT_ACTION_SELECT_ACCOUNT);
				intent.setClass(getActivity(), AccountSelectorActivity.class);
				intent.putExtra(EXTRA_SINGLE_SELECTION, true);
				startActivityForResult(intent, REQUEST_SELECT_ACCOUNT);
				break;
			}
			case MENU_FOLLOW: {
				if (relationship == null) return false;
				final boolean isFollowing = relationship.isSourceFollowingTarget();
				final boolean isCreatingFriendship = twitter.isCreatingFriendship(user.account_id, user.id);
				final boolean isDestroyingFriendship = twitter.isDestroyingFriendship(user.account_id, user.id);
				if (!isCreatingFriendship && !isDestroyingFriendship) {
					if (isFollowing) {
						DestroyFriendshipDialogFragment.show(getFragmentManager(), user);
					} else {
						twitter.createFriendshipAsync(user.account_id, user.id);
					}
				}
				return true;
			}
            case R.id.muted_users: {
                Utils.openMutesUsers(getActivity(), user.account_id);
                return true;
            }
            case R.id.blocked_users: {
                Utils.openUserBlocks(getActivity(), user.account_id);
                return true;
            }
            case R.id.incoming_friendships: {
                Utils.openIncomingFriendships(getActivity(), user.account_id);
                return true;
            }
            case R.id.user_mentions: {
                Utils.openUserMentions(getActivity(), user.account_id, user.screen_name);
                return true;
            }
            case R.id.saved_searches: {
                Utils.openSavedSearches(getActivity(), user.account_id);
                return true;
            }
			default: {
				if (item.getIntent() != null) {
					try {
						startActivity(item.getIntent());
					} catch (final ActivityNotFoundException e) {
						if (Utils.isDebugBuild()) Log.w(LOGTAG, e);
						return false;
					}
				}
				break;
			}
		}
		return true;
    }

    @Override
    public void onBaseViewCreated(final View view, final Bundle savedInstanceState) {
        super.onBaseViewCreated(view, savedInstanceState);
        mHeaderDrawerLayout = (HeaderDrawerLayout) view.findViewById(R.id.user_profile_drawer);
        final View headerView = mHeaderDrawerLayout.getHeader();
        final View contentView = mHeaderDrawerLayout.getContent();
        mCardContent = headerView.findViewById(R.id.card_content);
        mErrorRetryContainer = headerView.findViewById(R.id.error_retry_container);
        mProgressContainer = headerView.findViewById(R.id.progress_container);
        mRetryButton = (Button) headerView.findViewById(R.id.retry);
        mErrorMessageView = (TextView) headerView.findViewById(R.id.error_message);
        mProfileBannerView = (ProfileBannerImageView) view.findViewById(R.id.profile_banner);
        mProfileBannerContainer = view.findViewById(R.id.profile_banner_container);
        mNameView = (TextView) headerView.findViewById(R.id.name);
        mScreenNameView = (TextView) headerView.findViewById(R.id.screen_name);
        mDescriptionView = (TextView) headerView.findViewById(R.id.description);
        mLocationView = (TextView) headerView.findViewById(R.id.location);
        mURLView = (TextView) headerView.findViewById(R.id.url);
        mCreatedAtView = (TextView) headerView.findViewById(R.id.created_at);
        mTweetsContainer = headerView.findViewById(R.id.tweets_container);
        mTweetCount = (TextView) headerView.findViewById(R.id.statuses_count);
        mFollowersContainer = headerView.findViewById(R.id.followers_container);
        mFollowersCount = (TextView) headerView.findViewById(R.id.followers_count);
        mFriendsContainer = headerView.findViewById(R.id.friends_container);
        mFriendsCount = (TextView) headerView.findViewById(R.id.friends_count);
        mProfileNameContainer = (ColorLabelRelativeLayout) headerView.findViewById(R.id.profile_name_container);
        mProfileImageView = (ShapedImageView) headerView.findViewById(R.id.profile_image);
        mProfileTypeView = (ImageView) headerView.findViewById(R.id.profile_type);
        mDescriptionContainer = headerView.findViewById(R.id.description_container);
        mLocationContainer = headerView.findViewById(R.id.location_container);
        mURLContainer = headerView.findViewById(R.id.url_container);
        mProfileBannerSpace = headerView.findViewById(R.id.profile_banner_space);
        mViewPager = (ViewPager) contentView.findViewById(R.id.view_pager);
        mPagerIndicator = (TabPagerIndicator) contentView.findViewById(R.id.view_pager_tabs);
        mFollowButton = (Button) headerView.findViewById(R.id.follow);
        mFollowProgress = (ProgressBar) headerView.findViewById(R.id.follow_progress);
        mPagesContent = view.findViewById(R.id.pages_content);
        mPagesErrorContainer = view.findViewById(R.id.pages_error_container);
        mPagesErrorIcon = (ImageView) view.findViewById(R.id.pages_error_icon);
        mPagesErrorText = (TextView) view.findViewById(R.id.pages_error_text);
        mProfileNameBackground = view.findViewById(R.id.profile_name_background);
        mProfileDetailsContainer = view.findViewById(R.id.profile_details_container);
    }

    @Override
    public boolean handleKeyboardShortcutSingle(int keyCode, @NonNull KeyEvent event) {
        if (handleFragmentKeyboardShortcutSingle(keyCode, event)) return true;
        final String action = mKeyboardShortcutsHandler.getKeyAction("navigation", keyCode, event);
        if (action != null) {
            switch (action) {
                case "navigation.previous_tab": {
                    final int previous = mViewPager.getCurrentItem() - 1;
                    if (previous >= 0 && previous < mPagerAdapter.getCount()) {
                        mViewPager.setCurrentItem(previous, true);
                    }
                    return true;
                }
                case "navigation.next_tab": {
                    final int next = mViewPager.getCurrentItem() + 1;
                    if (next >= 0 && next < mPagerAdapter.getCount()) {
                        mViewPager.setCurrentItem(next, true);
                    }
                    return true;
                }
            }
        }
        return mKeyboardShortcutsHandler.handleKey(getActivity(), null, keyCode, event);
    }

    @Override
    public boolean handleKeyboardShortcutRepeat(int keyCode, int repeatCount, @NonNull KeyEvent event) {
        return handleFragmentKeyboardShortcutRepeat(keyCode, repeatCount, event);
    }

    private boolean handleFragmentKeyboardShortcutRepeat(int keyCode, int repeatCount, @NonNull KeyEvent event) {
        final Fragment fragment = getKeyboardShortcutRecipient();
        if (fragment instanceof KeyboardShortcutCallback) {
            return ((KeyboardShortcutCallback) fragment).handleKeyboardShortcutRepeat(keyCode, repeatCount, event);
        }
        return false;
    }

    private boolean handleFragmentKeyboardShortcutSingle(int keyCode, @NonNull KeyEvent event) {
        final Fragment fragment = getKeyboardShortcutRecipient();
        if (fragment instanceof KeyboardShortcutCallback) {
            return ((KeyboardShortcutCallback) fragment).handleKeyboardShortcutSingle(keyCode, event);
        }
        return false;
    }

    private Fragment getKeyboardShortcutRecipient() {
        return getCurrentVisibleFragment();
    }

    @Override
    protected void fitSystemWindows(Rect insets) {
        super.fitSystemWindows(insets);
        mHeaderDrawerLayout.setPadding(insets.left, insets.top, insets.right, insets.bottom);
        final FragmentActivity activity = getActivity();
        final boolean isTransparentBackground;
        if (activity instanceof IThemedActivity) {
            final String backgroundOption = ((IThemedActivity) activity).getCurrentThemeBackgroundOption();
            isTransparentBackground = ThemeUtils.isTransparentBackground(backgroundOption);
        } else {
            isTransparentBackground = ThemeUtils.isTransparentBackground(getActivity());
        }
        mHeaderDrawerLayout.setClipToPadding(isTransparentBackground);
    }

    @Override
	public void onClick(final View view) {
		final FragmentActivity activity = getActivity();
        final ParcelableUser user = getUser();
		if (activity == null || user == null) return;
		switch (view.getId()) {
			case R.id.retry: {
				getUserInfo(true);
				break;
			}
            case R.id.follow: {
                if (user.id == user.account_id) {
                    final Bundle extras = new Bundle();
                    extras.putLong(EXTRA_ACCOUNT_ID, user.account_id);
                    final Intent intent = new Intent(INTENT_ACTION_EDIT_USER_PROFILE);
                    intent.setClass(getActivity(), UserProfileEditorActivity.class);
                    intent.putExtras(extras);
                    startActivity(intent);
                    break;
                }
                final Relationship relationship = mRelationship;
                final AsyncTwitterWrapper twitter = getTwitterWrapper();
                if (relationship == null || twitter == null) return;
                if (relationship.isSourceBlockingTarget()) {
                    twitter.destroyBlockAsync(user.account_id, user.id);
                } else if (relationship.isSourceFollowingTarget()) {
                    DestroyFriendshipDialogFragment.show(getFragmentManager(), user);
                } else {
                    twitter.createFriendshipAsync(user.account_id, user.id);
                }
                break;
            }
			case R.id.profile_image: {
                final String url = Utils.getOriginalTwitterProfileImage(user.profile_image_url);
                final ParcelableMedia[] media = {ParcelableMedia.newImage(url, url)};
                //TODO open media animation
                Bundle options = null;
                Utils.openMedia(activity, user.account_id, false, null, media, options);
				break;
			}
			case R.id.profile_banner: {
                if (user.profile_banner_url == null) return;
                final String url = user.profile_banner_url + "/ipad_retina";
                final ParcelableMedia[] media = {ParcelableMedia.newImage(url, url)};
                //TODO open media animation
                Bundle options = null;
                Utils.openMedia(activity, user.account_id, false, null, media, options);
				break;
			}
			case R.id.tweets_container: {
                Utils.openUserTimeline(getActivity(), user.account_id, user.id, user.screen_name);
				break;
			}
			case R.id.followers_container: {
                Utils.openUserFollowers(getActivity(), user.account_id, user.id, user.screen_name);
				break;
			}
			case R.id.friends_container: {
                Utils.openUserFriends(getActivity(), user.account_id, user.id, user.screen_name);
				break;
			}
			case R.id.name_container: {
				if (user.account_id != user.id) return;
				startActivity(new Intent(getActivity(), UserProfileEditorActivity.class));
				break;
			}
		}

	}

	@Override
    public void onLinkClick(final String link, final String orig, final long accountId, long extraId, final int type,
                            final boolean sensitive, int start, int end) {
        final ParcelableUser user = getUser();
		if (user == null) return;
		switch (type) {
			case TwidereLinkify.LINK_TYPE_MENTION: {
                Utils.openUserProfile(getActivity(), user.account_id, -1, link, null);
				break;
			}
			case TwidereLinkify.LINK_TYPE_HASHTAG: {
                Utils.openTweetSearch(getActivity(), user.account_id, "#" + link);
				break;
			}
			case TwidereLinkify.LINK_TYPE_LINK: {
				final Uri uri = Uri.parse(link);
				final Intent intent;
				if (uri.getScheme() != null) {
					intent = new Intent(Intent.ACTION_VIEW, uri);
				} else {
					intent = new Intent(Intent.ACTION_VIEW, uri.buildUpon().scheme("http").build());
				}
				startActivity(intent);
				break;
			}
			case TwidereLinkify.LINK_TYPE_LIST: {
				if (link == null) break;
				final String[] mentionList = link.split("/");
				if (mentionList.length != 2) {
					break;
				}
				break;
			}
			case TwidereLinkify.LINK_TYPE_STATUS: {
                Utils.openStatus(getActivity(), accountId, ParseUtils.parseLong(link));
				break;
			}
		}
	}

	@Override
	public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String key) {
		if (mUser == null || !ParseUtils.parseString(mUser.id).equals(key)) return;
		displayUser(mUser);
	}

	@Override
	public void onSizeChanged(final View view, final int w, final int h, final int oldw, final int oldh) {
		mBannerWidth = w;
		if (w != oldw || h != oldh) {
			requestFitSystemWindows();
		}
	}

	@Override
	public boolean onTouch(final View v, final MotionEvent event) {
		return mProfileBannerView.dispatchTouchEvent(event);
	}

	@Override
    public boolean scrollToStart() {
        final Fragment fragment = getCurrentVisibleFragment();
        if (!(fragment instanceof RefreshScrollTopInterface)) return false;
        ((RefreshScrollTopInterface) fragment).scrollToStart();
        return true;
	}

    @Override
    public boolean triggerRefresh() {
        final Fragment fragment = getCurrentVisibleFragment();
        if (!(fragment instanceof RefreshScrollTopInterface)) return false;
        ((RefreshScrollTopInterface) fragment).triggerRefresh();
        return true;
    }

    public void setListShown(boolean shown) {
        final TintedStatusFrameLayout tintedStatus = mTintedStatusContent;
        if (tintedStatus == null) return;
        tintedStatus.setDrawShadow(shown);
    }

	private void getFriendship() {
        mRelationship = null;
        final ParcelableUser user = getUser();
		final LoaderManager lm = getLoaderManager();
		lm.destroyLoader(LOADER_ID_FRIENDSHIP);
		final Bundle args = new Bundle();
		args.putLong(EXTRA_ACCOUNT_ID, user.account_id);
		args.putLong(EXTRA_USER_ID, user.id);
		if (!mGetFriendShipLoaderInitialized) {
			lm.initLoader(LOADER_ID_FRIENDSHIP, args, mFriendshipLoaderCallbacks);
			mGetFriendShipLoaderInitialized = true;
		} else {
			lm.restartLoader(LOADER_ID_FRIENDSHIP, args, mFriendshipLoaderCallbacks);
		}
	}

	private void getUserInfo(final boolean omitIntentExtra) {
		final ParcelableUser user = mUser;
		if (user == null) return;
		getUserInfo(user.account_id, user.id, user.screen_name, omitIntentExtra);
	}

    private static void setCompatToolbarOverlayAlpha(FragmentActivity activity, float alpha) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) return;
        final Drawable drawable = ThemeUtils.getCompatToolbarOverlay(activity);
        if (drawable == null) return;
        drawable.setAlpha(Math.round(alpha * 255));
    }

    private void setUserUiColor(int color) {
        mUserUiColor = color;
        if (mActionBarBackground == null) {
            setupBaseActionBar();
	    }
        final ActionBarActivity activity = (ActionBarActivity) getActivity();
        final IThemedActivity themed = (IThemedActivity) activity;
        final int themeRes = themed.getCurrentThemeResourceId();
        //custom action bar color and stuff! (twittnuker)
		mTintedStatusContent.setColor(color, themed.getCurrentThemeBackgroundAlpha());
		mActionBarBackground.setColor(color);
        ActivityAccessor.setTaskDescription(activity, new TaskDescriptionCompat(null, null, color));
        mDescriptionView.setLinkTextColor(color);
        mProfileBannerView.setBackgroundColor(color);
        mLocationView.setLinkTextColor(color);
        mURLView.setLinkTextColor(color);
        ViewUtils.setBackground(mPagerIndicator, ThemeUtils.getActionBarStackedBackground(activity, themeRes, color, true));

        final HeaderDrawerLayout drawer = mHeaderDrawerLayout;
        if (drawer != null) {
            final int offset = drawer.getPaddingTop() - drawer.getHeaderTop();
            updateScrollOffset(offset);
        }
	}

    private void setupBaseActionBar() {
        final FragmentActivity activity = getActivity();
        if (!(activity instanceof LinkHandlerActivity)) return;
        final LinkHandlerActivity linkHandler = (LinkHandlerActivity) activity;
        final ActionBar actionBar = linkHandler.getSupportActionBar();
        if (actionBar == null) return;
        final Drawable shadow = ResourcesCompat.getDrawable(activity.getResources(), R.drawable.shadow_user_banner_action_bar, null);
        mActionBarBackground = new ActionBarDrawable(shadow);
        if (!ThemeUtils.isWindowFloating(linkHandler, linkHandler.getCurrentThemeResourceId())
                && ThemeUtils.isTransparentBackground(linkHandler.getCurrentThemeBackgroundOption())) {
			mActionBarBackground.setAlpha(linkHandler.getCurrentThemeBackgroundAlpha());
			mProfileBannerView.setAlpha(linkHandler.getCurrentThemeBackgroundAlpha() / 255f);
        }
        actionBar.setBackgroundDrawable(mActionBarBackground);
        mActionBarHomeAsUpIndicator = ThemeUtils.getActionBarHomeAsUpIndicator(actionBar);
        actionBar.setHomeAsUpIndicator(mActionBarHomeAsUpIndicator);
    }

    private void setupUserPages() {
        final Context context = getActivity();
        final Bundle args = getArguments(), tabArgs = new Bundle();
        final long accountId;
		final ParcelableUser user = args.getParcelable(EXTRA_USER);
        if (user != null) {
            tabArgs.putLong(EXTRA_ACCOUNT_ID, accountId = user.account_id);
            tabArgs.putLong(EXTRA_USER_ID, user.id);
            tabArgs.putString(EXTRA_SCREEN_NAME, user.screen_name);
        } else {
            accountId = args.getLong(EXTRA_ACCOUNT_ID, -1);
            tabArgs.putLong(EXTRA_ACCOUNT_ID, accountId);
            tabArgs.putLong(EXTRA_USER_ID, args.getLong(EXTRA_USER_ID, -1));
            tabArgs.putString(EXTRA_SCREEN_NAME, args.getString(EXTRA_SCREEN_NAME));
			}
        mPagerAdapter.addTab(UserTimelineFragment.class, tabArgs, getString(R.string.statuses), R.drawable.ic_action_quote, TAB_TYPE_STATUSES, TAB_POSITION_STATUSES, null);
        if (Utils.isOfficialKeyAccount(context, accountId)) {
            mPagerAdapter.addTab(UserMediaTimelineFragment.class, tabArgs, getString(R.string.media), R.drawable.ic_action_gallery, TAB_TYPE_MEDIA, TAB_POSITION_MEDIA, null);
	    }
        mPagerAdapter.addTab(UserFavoritesFragment.class, tabArgs, getString(R.string.favorites), R.drawable.ic_action_star, TAB_TYPE_FAVORITES, TAB_POSITION_FAVORITES, null);
	}

    private void updateFollowProgressState() {
        final AsyncTwitterWrapper twitter = getTwitterWrapper();
        final ParcelableUser user = getUser();
        if (twitter == null || user == null) {
            mFollowButton.setVisibility(View.GONE);
            mFollowProgress.setVisibility(View.GONE);
            return;
        }
        final LoaderManager lm = getLoaderManager();
        final boolean loadingRelationship = lm.getLoader(LOADER_ID_FRIENDSHIP) != null;
        final boolean creatingFriendship = twitter.isCreatingFriendship(user.account_id, user.id);
        final boolean destroyingFriendship = twitter.isDestroyingFriendship(user.account_id, user.id);
        final boolean creatingBlock = twitter.isCreatingFriendship(user.account_id, user.id);
        final boolean destroyingBlock = twitter.isDestroyingFriendship(user.account_id, user.id);
        if (loadingRelationship || creatingFriendship || destroyingFriendship || creatingBlock || destroyingBlock) {
            mFollowButton.setVisibility(View.GONE);
            mFollowProgress.setVisibility(View.VISIBLE);
        } else if (mRelationship != null) {
            mFollowButton.setVisibility(View.VISIBLE);
            mFollowProgress.setVisibility(View.GONE);
        } else {
            mFollowButton.setVisibility(View.GONE);
            mFollowProgress.setVisibility(View.GONE);
        }
    }

    private void updateRefreshState() {
        final ParcelableUser user = getUser();
        if (user == null) return;
        final AsyncTwitterWrapper twitter = getTwitterWrapper();
        final boolean is_creating_friendship = twitter != null
                && twitter.isCreatingFriendship(user.account_id, user.id);
        final boolean is_destroying_friendship = twitter != null
                && twitter.isDestroyingFriendship(user.account_id, user.id);
        setProgressBarIndeterminateVisibility(is_creating_friendship || is_destroying_friendship);
        invalidateOptionsMenu();
    }

    private void updateScrollOffset(int offset) {
		final View space = mProfileBannerSpace;
        final ProfileBannerImageView profileBannerView = mProfileBannerView;
        final View profileBannerContainer = mProfileBannerContainer;
        final int spaceHeight = space.getHeight();
        final float factor = MathUtils.clamp(offset / (float) spaceHeight, 0, 1);
        profileBannerContainer.setTranslationY(Math.max(-offset, -spaceHeight));
        profileBannerView.setTranslationY(Math.min(offset, spaceHeight) / 2);

        if (mActionBarBackground != null && mTintedStatusContent != null) {
			mActionBarBackground.setFactor(factor);
            mTintedStatusContent.setFactor(factor);

            final float profileContentHeight = mProfileNameContainer.getHeight() + mProfileDetailsContainer.getHeight();
            final float tabOutlineAlphaFactor;
            if ((offset - spaceHeight) > 0) {
                tabOutlineAlphaFactor = 1f - MathUtils.clamp((offset - spaceHeight) / profileContentHeight, 0, 1);
            } else {
                tabOutlineAlphaFactor = 1f;
            }
            mActionBarBackground.setOutlineAlphaFactor(tabOutlineAlphaFactor);

            final ThemedActionBarActivity activity = (ThemedActionBarActivity) getActivity();

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                setCompatToolbarOverlayAlpha(activity, factor * tabOutlineAlphaFactor);
            }

			final Drawable drawable = mPagerIndicator.getBackground();
			final int stackedTabColor;
            final int themeId = activity.getCurrentThemeResourceId();
            if (drawable instanceof ColorDrawable) {
				stackedTabColor = mUserUiColor;
				final int tabColor = (Integer) sArgbEvaluator.evaluate(tabOutlineAlphaFactor, stackedTabColor, mCardBackgroundColor);
				((ColorDrawable) drawable).setColor(tabColor);
				final int contrastColor = ColorUtils.getContrastYIQ(tabColor, 192);
				mPagerIndicator.setIconColor(contrastColor);
				mPagerIndicator.setLabelColor(contrastColor);
				mPagerIndicator.setStripColor(contrastColor);
            } else {
                // This shouldn't happen, return
                return;
            }
            final int barColor = (Integer) sArgbEvaluator.evaluate(factor, mActionBarShadowColor, stackedTabColor);
            final int itemColor = ThemeUtils.getContrastActionBarItemColor(activity, themeId, barColor);
            if (mActionBarHomeAsUpIndicator != null) {
                mActionBarHomeAsUpIndicator.setColorFilter(itemColor, Mode.SRC_ATOP);
            }
            final View actionBarView = activity.getWindow().findViewById(android.support.v7.appcompat.R.id.action_bar);
            if (actionBarView instanceof Toolbar) {
                final Toolbar toolbar = (Toolbar) actionBarView;
                toolbar.setTitleTextColor(itemColor);
                toolbar.setSubtitleTextColor(itemColor);
                ThemeUtils.setActionBarOverflowColor(toolbar, itemColor);
                ThemeUtils.wrapToolbarMenuIcon(ViewUtils.findViewByType(actionBarView, ActionMenuView.class), itemColor, itemColor);
			}
            mPagerIndicator.updateAppearance();
        }
        updateTitleAlpha();
    }

    private void updateTitleAlpha() {
        final int[] location = new int[2];
        mNameView.getLocationInWindow(location);
        final float nameShowingRatio = (mHeaderDrawerLayout.getPaddingTop() - location[1])
                / (float) mNameView.getHeight();
        final float textAlpha = MathUtils.clamp(nameShowingRatio, 0, 1);
        final FragmentActivity activity = getActivity();
        final View actionBarView = activity.getWindow().findViewById(android.support.v7.appcompat.R.id.action_bar);
        if (actionBarView instanceof Toolbar) {
            final Toolbar toolbar = (Toolbar) actionBarView;
            final TextView titleView = ViewUtils.findViewByText(toolbar, toolbar.getTitle());
            if (titleView != null) {
                titleView.setAlpha(textAlpha);
        	}
            final TextView subtitleView = ViewUtils.findViewByText(toolbar, toolbar.getSubtitle());
            if (subtitleView != null) {
                subtitleView.setAlpha(textAlpha);
        	}
        }
	}

	private static class ActionBarDrawable extends LayerDrawable {

		private final Drawable mShadowDrawable;
		private final ColorDrawable mColorDrawable;

		private float mFactor;
		private int mColor;
        private int mAlpha;
        private float mOutlineAlphaFactor;

        public ActionBarDrawable(Drawable shadow) {
            super(new Drawable[]{shadow, ActionBarColorDrawable.create(true)});
            mShadowDrawable = getDrawable(0);
            mColorDrawable = (ColorDrawable) getDrawable(1);
            setAlpha(0xFF);
            setOutlineAlphaFactor(1);
        }

        public int getColor() {
            return mColor;
		}

        public void setColor(int color) {
            mColor = color;
            mColorDrawable.setColor(color);
            setFactor(mFactor);
        }

		@TargetApi(Build.VERSION_CODES.LOLLIPOP)
		@Override
		public void getOutline(Outline outline) {
			mColorDrawable.getOutline(outline);
            outline.setAlpha(mFactor * mOutlineAlphaFactor * 0.99f);
		}

		@Override
        public void setAlpha(int alpha) {
            mAlpha = alpha;
            setFactor(mFactor);
        }

        @Override
		public int getIntrinsicWidth() {
			return mColorDrawable.getIntrinsicWidth();
		}

		@Override
		public int getIntrinsicHeight() {
			return mColorDrawable.getIntrinsicHeight();
		}

		public void setFactor(float f) {
			mFactor = f;
            mShadowDrawable.setAlpha(Math.round(mAlpha * MathUtils.clamp(1 - f, 0, 1)));
			final boolean hasColor = mColor != 0;
            mColorDrawable.setAlpha(hasColor ? Math.round(mAlpha * MathUtils.clamp(f, 0, 1)) : 0);
		}

        public void setOutlineAlphaFactor(float f) {
            mOutlineAlphaFactor = f;
            invalidateSelf();
		}

    }

    static class RelationshipLoader extends AsyncTaskLoader<SingleResponse<Relationship>> {

        private final Context context;
        private final long account_id, user_id;

        public RelationshipLoader(final Context context, final long account_id, final long user_id) {
            super(context);
            this.context = context;
            this.account_id = account_id;
            this.user_id = user_id;
        }

        @Override
        public SingleResponse<Relationship> loadInBackground() {
            if (account_id == user_id) return SingleResponse.getInstance();
            final Twitter twitter = Utils.getTwitterInstance(context, account_id, false);
            if (twitter == null) return SingleResponse.getInstance();
            try {
                final Relationship relationship = twitter.showFriendship(account_id, user_id);
                if (relationship.isSourceBlockingTarget() || relationship.isSourceBlockedByTarget()) {
                    Utils.setLastSeen(context, user_id, -1);
                } else {
                    Utils.setLastSeen(context, user_id, System.currentTimeMillis());
                }
                Utils.updateRelationship(context, relationship, account_id);
                return SingleResponse.getInstance(relationship);
            } catch (final TwitterException e) {
                return SingleResponse.getInstance(e);
            }
        }

        @Override
        protected void onStartLoading() {
            forceLoad();
        }
    }

}