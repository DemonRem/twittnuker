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

package de.vanita5.twittnuker.fragment.support;

import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Rect;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter.CreateNdefMessageCallback;
import android.nfc.NfcEvent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;

import com.rengwuxian.materialedittext.MaterialEditText;

import de.vanita5.twittnuker.BuildConfig;
import de.vanita5.twittnuker.R;
import de.vanita5.twittnuker.activity.iface.IThemedActivity;
import de.vanita5.twittnuker.activity.support.AccountSelectorActivity;
import de.vanita5.twittnuker.activity.support.UserListSelectorActivity;
import de.vanita5.twittnuker.adapter.support.SupportTabsAdapter;
import de.vanita5.twittnuker.api.twitter.Twitter;
import de.vanita5.twittnuker.api.twitter.TwitterException;
import de.vanita5.twittnuker.api.twitter.model.UserList;
import de.vanita5.twittnuker.api.twitter.model.UserListUpdate;
import de.vanita5.twittnuker.fragment.iface.IBaseFragment.SystemWindowsInsetsCallback;
import de.vanita5.twittnuker.fragment.iface.SupportFragmentCallback;
import de.vanita5.twittnuker.graphic.EmptyDrawable;
import de.vanita5.twittnuker.model.UserKey;
import de.vanita5.twittnuker.model.ParcelableUser;
import de.vanita5.twittnuker.model.ParcelableUserList;
import de.vanita5.twittnuker.model.SingleResponse;
import de.vanita5.twittnuker.model.util.ParcelableUserListUtils;
import de.vanita5.twittnuker.text.validator.UserListNameValidator;
import de.vanita5.twittnuker.util.AsyncTwitterWrapper;
import de.vanita5.twittnuker.util.IntentUtils;
import de.vanita5.twittnuker.util.LinkCreator;
import de.vanita5.twittnuker.util.MenuUtils;
import de.vanita5.twittnuker.util.ParseUtils;
import de.vanita5.twittnuker.util.ThemeUtils;
import de.vanita5.twittnuker.util.TwitterAPIFactory;
import de.vanita5.twittnuker.util.Utils;
import de.vanita5.twittnuker.view.TabPagerIndicator;

public class UserListFragment extends BaseSupportFragment implements OnClickListener,
        LoaderCallbacks<SingleResponse<ParcelableUserList>>, SystemWindowsInsetsCallback,
        SupportFragmentCallback {

    private ViewPager mViewPager;
    private TabPagerIndicator mPagerIndicator;
    private View mPagerOverlay;

    private SupportTabsAdapter mPagerAdapter;
    private boolean mUserListLoaderInitialized;

    private ParcelableUserList mUserList;
    private final BroadcastReceiver mStatusReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(final Context context, final Intent intent) {
            if (getActivity() == null || !isAdded() || isDetached()) return;
            final String action = intent.getAction();
            final ParcelableUserList userList = intent.getParcelableExtra(EXTRA_USER_LIST);
            if (userList == null || mUserList == null)
                return;
            if (BROADCAST_USER_LIST_DETAILS_UPDATED.equals(action)) {
                if (userList.id == mUserList.id) {
                    getUserListInfo(true);
                }
            } else if (BROADCAST_USER_LIST_SUBSCRIBED.equals(action) || BROADCAST_USER_LIST_UNSUBSCRIBED.equals(action)) {
                if (userList.id == mUserList.id) {
                    getUserListInfo(true);
                }
            }
        }
    };

    public void displayUserList(final ParcelableUserList userList) {
        final FragmentActivity activity = getActivity();
        if (activity == null) return;
        getLoaderManager().destroyLoader(0);
        mUserList = userList;

        if (userList != null) {
            activity.setTitle(userList.name);
        } else {
            activity.setTitle(R.string.user_list);
        }
        invalidateOptionsMenu();
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

    public void getUserListInfo(final boolean omit_intent_extra) {
        final LoaderManager lm = getLoaderManager();
        lm.destroyLoader(0);
        final Bundle args = new Bundle(getArguments());
        args.putBoolean(EXTRA_OMIT_INTENT_EXTRA, omit_intent_extra);
        if (!mUserListLoaderInitialized) {
            lm.initLoader(0, args, this);
            mUserListLoaderInitialized = true;
        } else {
            lm.restartLoader(0, args, this);
        }
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        final AsyncTwitterWrapper twitter = mTwitterWrapper;
        switch (requestCode) {
            case REQUEST_SELECT_USER: {
                final ParcelableUserList userList = mUserList;
                if (resultCode != Activity.RESULT_OK || !data.hasExtra(EXTRA_USER) || twitter == null
                        || userList == null) return;
                final ParcelableUser user = data.getParcelableExtra(EXTRA_USER);
                twitter.addUserListMembersAsync(userList.account_key, userList.id, user);
                return;
            }
            case REQUEST_SELECT_ACCOUNT: {
                if (resultCode == Activity.RESULT_OK) {
                    if (data == null || !data.hasExtra(EXTRA_ID)) return;
                    final ParcelableUserList userList = mUserList;
                    final UserKey accountKey = data.getParcelableExtra(EXTRA_KEY);
                    IntentUtils.openUserListDetails(getActivity(), accountKey, userList.id,
                            userList.user_key.getId(), userList.user_screen_name, userList.name);
                }
                break;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_content_pages, container, false);
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final FragmentActivity activity = getActivity();
        setHasOptionsMenu(true);

        Utils.setNdefPushMessageCallback(activity, new CreateNdefMessageCallback() {

            @Override
            public NdefMessage createNdefMessage(NfcEvent event) {
                final ParcelableUserList userList = getUserList();
                if (userList == null) return null;
                return new NdefMessage(new NdefRecord[]{
                        NdefRecord.createUri(LinkCreator.getTwitterUserListLink(userList.user_screen_name, userList.name)),
                });
            }
        });

        mPagerAdapter = new SupportTabsAdapter(activity, getChildFragmentManager());

        mViewPager.setAdapter(mPagerAdapter);
        mPagerIndicator.setViewPager(mViewPager);
        mPagerIndicator.setTabDisplayOption(TabPagerIndicator.LABEL);
        getUserListInfo(false);
        setupUserPages();
    }

    private ParcelableUserList getUserList() {
        return mUserList;
    }

    @Override
    public void onStart() {
        super.onStart();
        final IntentFilter filter = new IntentFilter(BROADCAST_USER_LIST_DETAILS_UPDATED);
        filter.addAction(BROADCAST_USER_LIST_SUBSCRIBED);
        filter.addAction(BROADCAST_USER_LIST_UNSUBSCRIBED);
        registerReceiver(mStatusReceiver, filter);
    }

    @Override
    public void onStop() {
        unregisterReceiver(mStatusReceiver);
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        mUserList = null;
        getLoaderManager().destroyLoader(0);
        super.onDestroyView();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_user_list, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        final ParcelableUserList userList = mUserList;
        MenuUtils.setMenuItemAvailability(menu, R.id.info, userList != null);
        if (userList != null) {
            final boolean isMyList = userList.user_key.equals(userList.account_key);
            final boolean isFollowing = userList.is_following;
            MenuUtils.setMenuItemAvailability(menu, R.id.edit, isMyList);
            MenuUtils.setMenuItemAvailability(menu, R.id.follow, !isMyList);
            MenuUtils.setMenuItemAvailability(menu, R.id.add, isMyList);
            MenuUtils.setMenuItemAvailability(menu, R.id.delete, isMyList);
            final MenuItem followItem = menu.findItem(R.id.follow);
            if (isFollowing) {
                followItem.setIcon(R.drawable.ic_action_cancel);
                followItem.setTitle(R.string.unsubscribe);
            } else {
                followItem.setIcon(R.drawable.ic_action_add);
                followItem.setTitle(R.string.subscribe);
            }
            final Intent extensionsIntent = new Intent(INTENT_ACTION_EXTENSION_OPEN_USER_LIST);
            extensionsIntent.setExtrasClassLoader(getActivity().getClassLoader());
            extensionsIntent.putExtra(EXTRA_USER_LIST, userList);
        } else {
            MenuUtils.setMenuItemAvailability(menu, R.id.edit, false);
            MenuUtils.setMenuItemAvailability(menu, R.id.follow, false);
            MenuUtils.setMenuItemAvailability(menu, R.id.add, false);
            MenuUtils.setMenuItemAvailability(menu, R.id.delete, false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final AsyncTwitterWrapper twitter = mTwitterWrapper;
        final ParcelableUserList userList = mUserList;
        if (twitter == null || userList == null) return false;
        switch (item.getItemId()) {
            case R.id.add: {
                if (!userList.user_key.equals(userList.account_key)) return false;
                final Intent intent = new Intent(INTENT_ACTION_SELECT_USER);
                intent.setClass(getActivity(), UserListSelectorActivity.class);
                intent.putExtra(EXTRA_ACCOUNT_KEY, userList.account_key);
                startActivityForResult(intent, REQUEST_SELECT_USER);
                break;
            }
            case R.id.delete: {
                if (!userList.user_key.equals(userList.account_key)) return false;
                DestroyUserListDialogFragment.show(getFragmentManager(), userList);
                break;
            }
            case R.id.edit: {
                final Bundle args = new Bundle();
                args.putParcelable(EXTRA_ACCOUNT_KEY, userList.account_key);
                args.putString(EXTRA_LIST_NAME, userList.name);
                args.putString(EXTRA_DESCRIPTION, userList.description);
                args.putBoolean(EXTRA_IS_PUBLIC, userList.is_public);
                args.putLong(EXTRA_LIST_ID, userList.id);
                final DialogFragment f = new EditUserListDialogFragment();
                f.setArguments(args);
                f.show(getFragmentManager(), "edit_user_list_details");
                return true;
            }
            case R.id.follow: {
                if (userList.is_following) {
                    DestroyUserListSubscriptionDialogFragment.show(getFragmentManager(), userList);
                } else {
                    twitter.createUserListSubscriptionAsync(userList.account_key, userList.id);
                }
                return true;
            }
            case R.id.open_with_account: {
                final Intent intent = new Intent(INTENT_ACTION_SELECT_ACCOUNT);
                intent.setClass(getActivity(), AccountSelectorActivity.class);
                intent.putExtra(EXTRA_SINGLE_SELECTION, true);
                startActivityForResult(intent, REQUEST_SELECT_ACCOUNT);
                break;
            }
            default: {
                if (item.getIntent() != null) {
                    try {
                        startActivity(item.getIntent());
                    } catch (final ActivityNotFoundException e) {
                        if (BuildConfig.DEBUG) Log.w(LOGTAG, e);
                        return false;
                    }
                }
                break;
            }
        }
        return true;
    }

    @Override
    public void onClick(final View view) {
        switch (view.getId()) {
            case R.id.error_container: {
                getUserListInfo(true);
                break;
            }
            case R.id.profile_image: {
                final ParcelableUserList userList = mUserList;
                if (userList == null) return;
                IntentUtils.openUserProfile(getActivity(), userList.account_key,
                        userList.user_key.getId(), userList.user_screen_name, null, true, null);
                break;
            }
        }

    }

    @Override
    public Loader<SingleResponse<ParcelableUserList>> onCreateLoader(final int id, final Bundle args) {
        setProgressBarIndeterminateVisibility(true);
        final UserKey accountKey = args.getParcelable(EXTRA_ACCOUNT_KEY);
        final long userId = args.getLong(EXTRA_USER_ID, -1);
        final long listId = args.getLong(EXTRA_LIST_ID, -1);
        final String listName = args.getString(EXTRA_LIST_NAME);
        final String screenName = args.getString(EXTRA_SCREEN_NAME);
        final boolean omitIntentExtra = args.getBoolean(EXTRA_OMIT_INTENT_EXTRA, true);
        return new ParcelableUserListLoader(getActivity(), omitIntentExtra, getArguments(), accountKey, listId,
                listName, userId, screenName);
    }

    @Override
    public void onLoadFinished(final Loader<SingleResponse<ParcelableUserList>> loader,
                               final SingleResponse<ParcelableUserList> data) {
        if (data == null) return;
        if (getActivity() == null) return;
        if (data.hasData()) {
            final ParcelableUserList list = data.getData();
            displayUserList(list);
        } else if (data.hasException()) {
        }
        setProgressBarIndeterminateVisibility(false);
    }

    @Override
    public void onLoaderReset(final Loader<SingleResponse<ParcelableUserList>> loader) {

    }

    @Override
    public void onBaseViewCreated(final View view, final Bundle savedInstanceState) {
        super.onBaseViewCreated(view, savedInstanceState);
        mViewPager = (ViewPager) view.findViewById(R.id.view_pager);
        mPagerIndicator = (TabPagerIndicator) view.findViewById(R.id.view_pager_tabs);
        mPagerOverlay = view.findViewById(R.id.pager_window_overlay);
    }

    private void setupUserPages() {
        final Bundle args = getArguments(), tabArgs = new Bundle();
        if (args.containsKey(EXTRA_USER_LIST)) {
            final ParcelableUserList userList = args.getParcelable(EXTRA_USER_LIST);
            assert userList != null;
            tabArgs.putParcelable(EXTRA_ACCOUNT_KEY, userList.account_key);
            tabArgs.putLong(EXTRA_USER_ID, userList.user_key.getId());
            tabArgs.putString(EXTRA_SCREEN_NAME, userList.user_screen_name);
            tabArgs.putLong(EXTRA_LIST_ID, userList.id);
            tabArgs.putString(EXTRA_LIST_NAME, userList.name);
        } else {
            tabArgs.putParcelable(EXTRA_ACCOUNT_KEY, args.getParcelable(EXTRA_ACCOUNT_KEY));
            tabArgs.putLong(EXTRA_USER_ID, args.getLong(EXTRA_USER_ID, -1));
            tabArgs.putString(EXTRA_SCREEN_NAME, args.getString(EXTRA_SCREEN_NAME));
            tabArgs.putLong(EXTRA_LIST_ID, args.getLong(EXTRA_LIST_ID, -1));
            tabArgs.putString(EXTRA_LIST_NAME, args.getString(EXTRA_LIST_NAME));
        }
        mPagerAdapter.addTab(UserListTimelineFragment.class, tabArgs, getString(R.string.statuses), null, 0, null);
        mPagerAdapter.addTab(UserListMembersFragment.class, tabArgs, getString(R.string.members), null, 1, null);
        mPagerAdapter.addTab(UserListSubscribersFragment.class, tabArgs, getString(R.string.subscribers), null, 2, null);

        final FragmentActivity activity = getActivity();
        ThemeUtils.initPagerIndicatorAsActionBarTab(activity, mPagerIndicator, mPagerOverlay);
        ThemeUtils.setCompatToolbarOverlay(activity, new EmptyDrawable());
        ThemeUtils.setCompatContentViewOverlay(activity, new EmptyDrawable());
        ThemeUtils.setWindowOverlayViewOverlay(activity, new EmptyDrawable());

        if (activity instanceof IThemedActivity) {
            final String backgroundOption = ((IThemedActivity) activity).getCurrentThemeBackgroundOption();
            final boolean isTransparent = ThemeUtils.isTransparentBackground(backgroundOption);
            final int actionBarAlpha = isTransparent ? ThemeUtils.getActionBarAlpha(ThemeUtils.getUserThemeBackgroundAlpha(activity)) : 0xFF;
            mPagerIndicator.setAlpha(actionBarAlpha / 255f);
        }
    }

    public static class EditUserListDialogFragment extends BaseSupportDialogFragment implements
            DialogInterface.OnClickListener {

        private String mName, mDescription;
        private UserKey mAccountKey;
        private long mListId;
        private boolean mIsPublic;

        @Override
        public void onClick(final DialogInterface dialog, final int which) {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE: {
                    final AlertDialog alertDialog = (AlertDialog) dialog;
                    final MaterialEditText editName = (MaterialEditText) alertDialog.findViewById(R.id.name);
                    final MaterialEditText editDescription = (MaterialEditText) alertDialog.findViewById(R.id.description);
                    final CheckBox editIsPublic = (CheckBox) alertDialog.findViewById(R.id.is_public);
                    final String name = ParseUtils.parseString(editName.getText());
                    final String description = ParseUtils.parseString(editDescription.getText());
                    final boolean isPublic = editIsPublic.isChecked();
                    if (TextUtils.isEmpty(name)) return;
                    final UserListUpdate update = new UserListUpdate();
                    update.setMode(isPublic ? UserList.Mode.PUBLIC : UserList.Mode.PRIVATE);
                    update.setName(name);
                    update.setDescription(description);
                    mTwitterWrapper.updateUserListDetails(mAccountKey, mListId, update);
                    break;
                }
            }

        }

        @NonNull
        @Override
        public Dialog onCreateDialog(final Bundle savedInstanceState) {
            final Bundle bundle = savedInstanceState == null ? getArguments() : savedInstanceState;
            mAccountKey = bundle != null ? bundle.<UserKey>getParcelable(EXTRA_ACCOUNT_KEY) : null;
            mListId = bundle != null ? bundle.getLong(EXTRA_LIST_ID, -1) : -1;
            mName = bundle != null ? bundle.getString(EXTRA_LIST_NAME) : null;
            mDescription = bundle != null ? bundle.getString(EXTRA_DESCRIPTION) : null;
            mIsPublic = bundle == null || bundle.getBoolean(EXTRA_IS_PUBLIC, true);
            final Context wrapped = ThemeUtils.getDialogThemedContext(getActivity());
            final AlertDialog.Builder builder = new AlertDialog.Builder(wrapped);
            builder.setView(R.layout.dialog_user_list_detail_editor);
            builder.setTitle(R.string.user_list);
            builder.setPositiveButton(android.R.string.ok, this);
            builder.setNegativeButton(android.R.string.cancel, this);
            final AlertDialog dialog = builder.create();
            dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialog) {

                    AlertDialog alertDialog = (AlertDialog) dialog;
                    MaterialEditText mEditName = (MaterialEditText) alertDialog.findViewById(R.id.name);
                    MaterialEditText mEditDescription = (MaterialEditText) alertDialog.findViewById(R.id.description);
                    CheckBox mPublicCheckBox = (CheckBox) alertDialog.findViewById(R.id.is_public);

                    mEditName.addValidator(new UserListNameValidator(getString(R.string.invalid_list_name)));
                    if (mName != null) {
                        mEditName.setText(mName);
                    }
                    if (mDescription != null) {
                        mEditDescription.setText(mDescription);
                    }
                    mPublicCheckBox.setChecked(mIsPublic);
                }
            });
            return dialog;
        }

        @Override
        public void onSaveInstanceState(final Bundle outState) {
            outState.putParcelable(EXTRA_ACCOUNT_KEY, mAccountKey);
            outState.putLong(EXTRA_LIST_ID, mListId);
            outState.putString(EXTRA_LIST_NAME, mName);
            outState.putString(EXTRA_DESCRIPTION, mDescription);
            outState.putBoolean(EXTRA_IS_PUBLIC, mIsPublic);
            super.onSaveInstanceState(outState);
        }

    }

    static class ParcelableUserListLoader extends AsyncTaskLoader<SingleResponse<ParcelableUserList>> {

        private final boolean mOmitIntentExtra;
        private final Bundle mExtras;
        private final UserKey mAccountKey;
        private final long mUserId;
        private final long mListId;
        private final String mScreenName, mListName;

        private ParcelableUserListLoader(final Context context, final boolean omitIntentExtra, final Bundle extras,
                                         final UserKey accountKey, final long listId, final String listName, final long userId,
                                         final String screenName) {
            super(context);
            mOmitIntentExtra = omitIntentExtra;
            mExtras = extras;
            mAccountKey = accountKey;
            mUserId = userId;
            mListId = listId;
            mScreenName = screenName;
            mListName = listName;
        }

        @Override
        public SingleResponse<ParcelableUserList> loadInBackground() {
            if (!mOmitIntentExtra && mExtras != null) {
                final ParcelableUserList cache = mExtras.getParcelable(EXTRA_USER_LIST);
                if (cache != null) return SingleResponse.getInstance(cache);
            }
            final Twitter twitter = TwitterAPIFactory.getTwitterInstance(getContext(), mAccountKey,
                    true);
            if (twitter == null) return SingleResponse.getInstance();
            try {
                final UserList list;
                if (mListId > 0) {
                    list = twitter.showUserList(mListId);
                } else if (mUserId > 0) {
                    list = twitter.showUserList(mListName, mUserId);
                } else if (mScreenName != null) {
                    list = twitter.showUserList(mListName, mScreenName);
                } else
                    return SingleResponse.getInstance();
                return SingleResponse.getInstance(ParcelableUserListUtils.from(list, mAccountKey));
            } catch (final TwitterException e) {
                return SingleResponse.getInstance(e);
            }
        }

        @Override
        public void onStartLoading() {
            forceLoad();
        }

    }

}