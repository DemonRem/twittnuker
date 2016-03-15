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

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;

import de.vanita5.twittnuker.adapter.ParcelableUsersAdapter;
import de.vanita5.twittnuker.adapter.iface.ILoadMoreSupportAdapter.IndicatorPosition;
import de.vanita5.twittnuker.adapter.iface.IUsersAdapter.UserAdapterListener;
import de.vanita5.twittnuker.loader.iface.IExtendedLoader;
import de.vanita5.twittnuker.model.ParcelableUser;
import de.vanita5.twittnuker.model.util.UserKeyUtils;
import de.vanita5.twittnuker.util.IntentUtils;
import de.vanita5.twittnuker.util.KeyboardShortcutsHandler;
import de.vanita5.twittnuker.util.KeyboardShortcutsHandler.KeyboardShortcutCallback;
import de.vanita5.twittnuker.util.LinkCreator;
import de.vanita5.twittnuker.util.RecyclerViewNavigationHelper;
import de.vanita5.twittnuker.view.holder.UserViewHolder;

import java.util.List;

public abstract class ParcelableUsersFragment extends AbsContentListRecyclerViewFragment<ParcelableUsersAdapter>
        implements LoaderCallbacks<List<ParcelableUser>>, UserAdapterListener, KeyboardShortcutCallback {

    private RecyclerViewNavigationHelper mNavigationHelper;

    @Override
    public boolean isRefreshing() {
        if (getContext() == null || isDetached()) return false;
        final LoaderManager lm = getLoaderManager();
        return lm.hasRunningLoaders();
    }

    @NonNull
    @Override
    protected ParcelableUsersAdapter onCreateAdapter(Context context, boolean compact) {
        return new ParcelableUsersAdapter(context);
    }

    @NonNull
    @Override
    public ParcelableUsersAdapter getAdapter() {
        return super.getAdapter();
    }

    protected boolean hasMoreData(List<ParcelableUser> data) {
        return data == null || !data.isEmpty();
    }

    public void onLoadFinished(Loader<List<ParcelableUser>> loader, List<ParcelableUser> data) {
        final ParcelableUsersAdapter adapter = getAdapter();
        adapter.setData(data);
        if (!(loader instanceof IExtendedLoader) || ((IExtendedLoader) loader).isFromUser()) {
            adapter.setLoadMoreSupportedPosition(hasMoreData(data) ? IndicatorPosition.END : IndicatorPosition.NONE);
            setRefreshEnabled(true);
        }
        if (loader instanceof IExtendedLoader) {
            ((IExtendedLoader) loader).setFromUser(false);
        }
        showContent();
        setRefreshEnabled(true);
        setRefreshing(false);
        setLoadMoreIndicatorPosition(IndicatorPosition.NONE);
    }

    protected void removeUsers(String... ids) {
        //TODO remove from adapter
    }

    public final List<ParcelableUser> getData() {
        return getAdapter().getData();
    }

    @Override
    public boolean handleKeyboardShortcutSingle(@NonNull KeyboardShortcutsHandler handler, int keyCode, @NonNull KeyEvent event, int metaState) {
        return mNavigationHelper.handleKeyboardShortcutSingle(handler, keyCode, event, metaState);
    }

    @Override
    public boolean handleKeyboardShortcutRepeat(@NonNull KeyboardShortcutsHandler handler, int keyCode, int repeatCount, @NonNull KeyEvent event, int metaState) {
        return mNavigationHelper.handleKeyboardShortcutRepeat(handler, keyCode, repeatCount, event, metaState);
    }

    @Override
    public boolean isKeyboardShortcutHandled(@NonNull KeyboardShortcutsHandler handler, int keyCode, @NonNull KeyEvent event, int metaState) {
        return mNavigationHelper.isKeyboardShortcutHandled(handler, keyCode, event, metaState);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final ParcelableUsersAdapter adapter = getAdapter();
        final RecyclerView recyclerView = getRecyclerView();
        final LinearLayoutManager layoutManager = getLayoutManager();
        adapter.setUserAdapterListener(this);

        mNavigationHelper = new RecyclerViewNavigationHelper(recyclerView, layoutManager, adapter,
                this);
        final Bundle loaderArgs = new Bundle(getArguments());
        loaderArgs.putBoolean(EXTRA_FROM_USER, true);
        getLoaderManager().initLoader(0, loaderArgs, this);
    }

    @Override
    public final Loader<List<ParcelableUser>> onCreateLoader(int id, Bundle args) {
        final boolean fromUser = args.getBoolean(EXTRA_FROM_USER);
        args.remove(EXTRA_FROM_USER);
        return onCreateUsersLoader(getActivity(), args, fromUser);
    }

    @Override
    public void onLoaderReset(Loader<List<ParcelableUser>> loader) {
        if (loader instanceof IExtendedLoader) {
            ((IExtendedLoader) loader).setFromUser(false);
        }
    }

    @Override
    public void onUserClick(UserViewHolder holder, int position) {
        final ParcelableUser user = getAdapter().getUser(position);
        final FragmentActivity activity = getActivity();
        if (UserKeyUtils.isSameHost(user.account_key, user.key)) {
            IntentUtils.openUserProfile(activity, user, null, true, getUserReferral());
        } else if (user.extras != null && user.extras.statusnet_profile_url != null) {
            final Uri uri = Uri.parse(user.extras.statusnet_profile_url);
            final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        } else {
            final Uri uri = LinkCreator.getTwitterUserLink(user.screen_name);
            final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        }
    }

    @UserFragment.Referral
    protected String getUserReferral() {
        return null;
    }

    @Override
    public boolean onUserLongClick(UserViewHolder holder, int position) {
        return true;
    }

    protected abstract Loader<List<ParcelableUser>> onCreateUsersLoader(final Context context,
                                                                        @NonNull final Bundle args,
                                                                        final boolean fromUser);

    @Override
    protected void setupRecyclerView(Context context, boolean compact) {
        super.setupRecyclerView(context, true);
    }
}