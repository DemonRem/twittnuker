/*
 * Twittnuker - Twitter client for Android
 *
 * Copyright (C) 2013-2014 vanita5 <mail@vanita5.de>
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

package de.vanita5.twittnuker.fragment.support;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.util.Pair;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.support.v7.widget.RecyclerView.LayoutParams;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;

import org.apache.http.protocol.HTTP;
import de.vanita5.twittnuker.R;
import de.vanita5.twittnuker.activity.support.AccountSelectorActivity;
import de.vanita5.twittnuker.activity.support.ColorPickerDialogActivity;
import de.vanita5.twittnuker.adapter.AbsStatusesAdapter.StatusAdapterListener;
import de.vanita5.twittnuker.adapter.decorator.DividerItemDecoration;
import de.vanita5.twittnuker.adapter.iface.IStatusesAdapter;
import de.vanita5.twittnuker.app.TwittnukerApplication;
import de.vanita5.twittnuker.constant.IntentConstants;
import de.vanita5.twittnuker.loader.support.ParcelableStatusLoader;
import de.vanita5.twittnuker.loader.support.StatusRepliesLoader;
import de.vanita5.twittnuker.model.ListResponse;
import de.vanita5.twittnuker.model.ParcelableAccount;
import de.vanita5.twittnuker.model.ParcelableAccount.ParcelableCredentials;
import de.vanita5.twittnuker.model.ParcelableMedia;
import de.vanita5.twittnuker.model.ParcelableStatus;
import de.vanita5.twittnuker.model.SingleResponse;
import de.vanita5.twittnuker.task.TwidereAsyncTask;
import de.vanita5.twittnuker.task.TwidereAsyncTask.Status;
import de.vanita5.twittnuker.text.method.StatusContentMovementMethod;
import de.vanita5.twittnuker.util.AsyncTwitterWrapper;
import de.vanita5.twittnuker.util.ClipboardUtils;
import de.vanita5.twittnuker.util.CompareUtils;
import de.vanita5.twittnuker.util.ImageLoaderWrapper;
import de.vanita5.twittnuker.util.ImageLoadingHandler;
import de.vanita5.twittnuker.util.MediaPreviewUtils.OnMediaClickListener;
import de.vanita5.twittnuker.util.OnLinkClickHandler;
import de.vanita5.twittnuker.util.StatisticUtils;
import de.vanita5.twittnuker.util.ThemeUtils;
import de.vanita5.twittnuker.util.TwidereLinkify;
import de.vanita5.twittnuker.util.TwitterCardUtils;
import de.vanita5.twittnuker.util.TwitterContentUtils;
import de.vanita5.twittnuker.util.Utils;
import de.vanita5.twittnuker.view.ShapedImageView;
import de.vanita5.twittnuker.view.StatusTextView;
import de.vanita5.twittnuker.view.TwidereMenuBar;
import de.vanita5.twittnuker.view.TwitterCardContainer;
import de.vanita5.twittnuker.view.holder.GapViewHolder;
import de.vanita5.twittnuker.view.holder.LoadIndicatorViewHolder;
import de.vanita5.twittnuker.view.holder.StatusViewHolder;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import twitter4j.TwitterException;

import static android.text.TextUtils.isEmpty;
import static de.vanita5.twittnuker.util.UserColorNameUtils.clearUserColor;
import static de.vanita5.twittnuker.util.UserColorNameUtils.getUserColor;
import static de.vanita5.twittnuker.util.UserColorNameUtils.setUserColor;
import static de.vanita5.twittnuker.util.Utils.favorite;
import static de.vanita5.twittnuker.util.Utils.findStatus;
import static de.vanita5.twittnuker.util.Utils.formatToLongTimeString;
import static de.vanita5.twittnuker.util.Utils.getLocalizedNumber;
import static de.vanita5.twittnuker.util.Utils.getUserTypeIconRes;
import static de.vanita5.twittnuker.util.Utils.openStatus;
import static de.vanita5.twittnuker.util.Utils.openUserProfile;
import static de.vanita5.twittnuker.util.Utils.retweet;
import static de.vanita5.twittnuker.util.Utils.setMenuForStatus;
import static de.vanita5.twittnuker.util.Utils.showErrorMessage;
import static de.vanita5.twittnuker.util.Utils.showOkMessage;
import static de.vanita5.twittnuker.util.Utils.startStatusShareChooser;

public class StatusFragment extends BaseSupportFragment
        implements LoaderCallbacks<SingleResponse<ParcelableStatus>>, OnMediaClickListener, StatusAdapterListener {

    private static final int LOADER_ID_DETAIL_STATUS = 1;
    private static final int LOADER_ID_STATUS_REPLIES = 2;
    private static final int STATE_LOADED = 1;
    private static final int STATE_LOADING = 2;
    private static final int STATE_ERROR = 3;
    private RecyclerView mRecyclerView;
    private StatusAdapter mStatusAdapter;
    private boolean mRepliesLoaderInitialized;
    private LoadConversationTask mLoadConversationTask;
    private LinearLayoutManager mLayoutManager;
    private View mStatusContent;
    private View mProgressContainer;
    private View mErrorContainer;
    private DividerItemDecoration mItemDecoration;

    private LoaderCallbacks<List<ParcelableStatus>> mRepliesLoaderCallback = new LoaderCallbacks<List<ParcelableStatus>>() {
		@Override
        public Loader<List<ParcelableStatus>> onCreateLoader(int id, Bundle args) {
            final long accountId = args.getLong(EXTRA_ACCOUNT_ID, -1);
            final String screenName = args.getString(EXTRA_SCREEN_NAME);
            final long statusId = args.getLong(EXTRA_STATUS_ID, -1);
            final long maxId = args.getLong(EXTRA_MAX_ID, -1);
            final long sinceId = args.getLong(EXTRA_SINCE_ID, -1);
            final StatusRepliesLoader loader = new StatusRepliesLoader(getActivity(), accountId,
                    screenName, statusId, maxId, sinceId, null, null, 0, false);
            loader.setComparator(ParcelableStatus.REVERSE_ID_COMPARATOR);
            return loader;
					}

        @Override
        public void onLoadFinished(Loader<List<ParcelableStatus>> loader, List<ParcelableStatus> data) {
            setReplies(data);
        }

        @Override
        public void onLoaderReset(Loader<List<ParcelableStatus>> loader) {

        }
	};

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        switch (requestCode) {
            case REQUEST_SET_COLOR: {
                final ParcelableStatus status = mStatusAdapter.getStatus();
                if (status == null) return;
                if (resultCode == Activity.RESULT_OK) {
                    if (data == null) return;
                    final int color = data.getIntExtra(EXTRA_COLOR, Color.TRANSPARENT);
                    setUserColor(getActivity(), status.user_id, color);
                } else if (resultCode == ColorPickerDialogActivity.RESULT_CLEARED) {
                    clearUserColor(getActivity(), status.user_id);
                }
                break;
            }
            case REQUEST_SELECT_ACCOUNT: {
                final ParcelableStatus status = mStatusAdapter.getStatus();
                if (status == null) return;
                if (resultCode == Activity.RESULT_OK) {
                    if (data == null || !data.hasExtra(EXTRA_ID)) return;
                    final long accountId = data.getLongExtra(EXTRA_ID, -1);
                    openStatus(getActivity(), accountId, status.id);
                }
                break;
            }
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, @Nullable final ViewGroup container,
                             @Nullable final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_status, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final View view = getView();
        if (view == null) throw new AssertionError();
        final Context context = view.getContext();
        final boolean compact = Utils.isCompactCards(context);
        mLayoutManager = new StatusListLinearLayoutManager(context, mRecyclerView);
        mItemDecoration = new DividerItemDecoration(context, mLayoutManager.getOrientation());
        if (compact) {
            mRecyclerView.addItemDecoration(mItemDecoration);
        }
        mLayoutManager.setRecycleChildrenOnDetach(true);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setClipToPadding(false);
        mStatusAdapter = new StatusAdapter(this, compact);
        mStatusAdapter.setEventListener(this);
        mRecyclerView.setAdapter(mStatusAdapter);

        setState(STATE_LOADING);

        getLoaderManager().initLoader(LOADER_ID_DETAIL_STATUS, getArguments(), this);
    }

    @Override
    public void onGapClick(GapViewHolder holder, int position) {

    }

    @Override
    public void onStatusActionClick(StatusViewHolder holder, int id, int position) {
        final ParcelableStatus status = mStatusAdapter.getStatus(position);
        if (status == null) return;
        switch (id) {
            case R.id.reply_count: {
                final Context context = getActivity();
                final Intent intent = new Intent(IntentConstants.INTENT_ACTION_REPLY);
                intent.setPackage(context.getPackageName());
                intent.putExtra(IntentConstants.EXTRA_STATUS, status);
                context.startActivity(intent);
                break;
            }
            case R.id.retweet_count: {
                RetweetQuoteDialogFragment.show(getFragmentManager(), status);
                break;
            }
            case R.id.favorite_count: {
                final AsyncTwitterWrapper twitter = getTwitterWrapper();
                if (twitter == null) return;
                if (status.is_favorite) {
                    twitter.destroyFavoriteAsync(status.account_id, status.id);
                } else {
                    twitter.createFavoriteAsync(status.account_id, status.id);
                }
                break;
            }
        }
    }

    @Override
    public void onStatusClick(StatusViewHolder holder, int position) {
        openStatus(getActivity(), mStatusAdapter.getStatus(position), null);
    }

    @Override
    public void onStatusMenuClick(StatusViewHolder holder, View itemView, int position) {
        final Bundle args = new Bundle();
        args.putParcelable(EXTRA_STATUS, mStatusAdapter.getStatus(position));
        final StatusMenuDialogFragment f = new StatusMenuDialogFragment();
        f.setArguments(args);
        f.show(getActivity().getSupportFragmentManager(), "status_menu");
    }

    @Override
    public Loader<SingleResponse<ParcelableStatus>> onCreateLoader(final int id, final Bundle args) {
        final Bundle fragmentArgs = getArguments();
        final long accountId = fragmentArgs.getLong(EXTRA_ACCOUNT_ID, -1);
        final long statusId = fragmentArgs.getLong(EXTRA_STATUS_ID, -1);
        return new ParcelableStatusLoader(getActivity(), false, fragmentArgs, accountId, statusId);
    }

    @Override
    public void onMediaClick(View view, ParcelableMedia media, long accountId) {
        Utils.openImageDirectly(getActivity(), accountId, media.url);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mStatusContent = view.findViewById(R.id.status_content);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        mProgressContainer = view.findViewById(R.id.progress_container);
        mErrorContainer = view.findViewById(R.id.error_retry_container);
    }

    @Override
    protected void fitSystemWindows(Rect insets) {
        super.fitSystemWindows(insets);
//        mRecyclerView.setPadding(insets.left, insets.top, insets.right, insets.bottom);
        getView().setPadding(insets.left, insets.top, insets.right, insets.bottom);
    }

    @Override
    public void onLoadFinished(final Loader<SingleResponse<ParcelableStatus>> loader,
            final SingleResponse<ParcelableStatus> data) {
        if (data.hasData()) {
            final long itemId = mStatusAdapter.getItemId(mLayoutManager.findFirstVisibleItemPosition());
            final View firstChild = mLayoutManager.getChildAt(0);
            final int top = firstChild != null ? firstChild.getTop() : 0;
            final ParcelableStatus status = data.getData();
            if (mStatusAdapter.setStatus(status)) {
                mLayoutManager.scrollToPositionWithOffset(1, 0);
                mStatusAdapter.setConversation(null);
                mStatusAdapter.setReplies(null);
                loadReplies(status);
                loadConversation(status);
            } else {
                final int position = mStatusAdapter.findPositionById(itemId);
                mLayoutManager.scrollToPositionWithOffset(position, top);
            }
            try {
                StatisticUtils.writeStatusOpen(status, null, 0);
            } catch (IOException e) {
                e.printStackTrace();
            }
            setState(STATE_LOADED);
        } else {
            //TODO show errors
            setState(STATE_ERROR);
        }
    }

    private void loadConversation(ParcelableStatus status) {
        if (mLoadConversationTask != null && mLoadConversationTask.getStatus() == Status.RUNNING) {
            mLoadConversationTask.cancel(true);
        }
        mLoadConversationTask = new LoadConversationTask(this);
        mLoadConversationTask.executeTask(status);
    }

    private void loadReplies(ParcelableStatus status) {
        if (status == null) return;
        final Bundle args = new Bundle();
        args.putLong(EXTRA_ACCOUNT_ID, status.account_id);
        args.putLong(EXTRA_STATUS_ID, status.retweet_id > 0 ? status.retweet_id : status.id);
        args.putString(EXTRA_SCREEN_NAME, status.user_screen_name);
        if (mRepliesLoaderInitialized) {
            getLoaderManager().restartLoader(LOADER_ID_STATUS_REPLIES, args, mRepliesLoaderCallback);
            return;
        }
        getLoaderManager().initLoader(LOADER_ID_STATUS_REPLIES, args, mRepliesLoaderCallback);
        mRepliesLoaderInitialized = true;
    }

    private void setConversation(List<ParcelableStatus> data) {
        if (mLayoutManager.getChildCount() != 0) {
            final long itemId = mStatusAdapter.getItemId(mLayoutManager.findFirstVisibleItemPosition());
            final int top = mLayoutManager.getChildAt(0).getTop();
            mStatusAdapter.setConversation(data);
            final int position = mStatusAdapter.findPositionById(itemId);
            mLayoutManager.scrollToPositionWithOffset(position, top);
        } else {
            mStatusAdapter.setConversation(data);
        }
    }

    private void setReplies(List<ParcelableStatus> data) {
        if (mLayoutManager.getChildCount() != 0) {
            final long itemId = mStatusAdapter.getItemId(mLayoutManager.findFirstVisibleItemPosition());
            final int top = mLayoutManager.getChildAt(0).getTop();
            mStatusAdapter.setReplies(data);
            final int position = mStatusAdapter.findPositionById(itemId);
            mLayoutManager.scrollToPositionWithOffset(position, top);
        } else {
            mStatusAdapter.setReplies(data);
        }
    }

    private void setState(int state) {
        mStatusContent.setVisibility(state == STATE_LOADED ? View.VISIBLE : View.GONE);
        mProgressContainer.setVisibility(state == STATE_LOADING ? View.VISIBLE : View.GONE);
        mErrorContainer.setVisibility(state == STATE_ERROR ? View.VISIBLE : View.GONE);
    }

    private static class StatusAdapter extends Adapter<ViewHolder> implements IStatusesAdapter<List<ParcelableStatus>> {

        private static final int VIEW_TYPE_DETAIL_STATUS = 0;
        private static final int VIEW_TYPE_LIST_STATUS = 1;
        private static final int VIEW_TYPE_CONVERSATION_LOAD_INDICATOR = 2;
        private static final int VIEW_TYPE_REPLIES_LOAD_INDICATOR = 3;
        private static final int VIEW_TYPE_SPACE = 4;

        private final Context mContext;
        private final StatusFragment mFragment;
        private final LayoutInflater mInflater;
        private final ImageLoaderWrapper mImageLoader;
        private final ImageLoadingHandler mImageLoadingHandler;

        private final boolean mNameFirst;
        private final int mCardLayoutResource;
        private final int mTextSize;
        private final int mCardBackgroundColor;
        private final boolean mIsCompact;
        private final int mProfileImageStyle;

        private ParcelableStatus mStatus;
        private ParcelableCredentials mStatusAccount;
        private List<ParcelableStatus> mConversation, mReplies;
		private StatusAdapterListener mStatusAdapterListener;
		private DetailStatusViewHolder mCachedHolder;

        public StatusAdapter(StatusFragment fragment, boolean compact) {
            final Context context = fragment.getActivity();
            final Resources res = context.getResources();
            final SharedPreferences preferences = context.getSharedPreferences(SHARED_PREFERENCES_NAME,
                    Context.MODE_PRIVATE);
            mFragment = fragment;
            mContext = context;
            mInflater = LayoutInflater.from(context);
            mImageLoader = TwittnukerApplication.getInstance(context).getImageLoaderWrapper();
            mImageLoadingHandler = new ImageLoadingHandler(R.id.media_preview_progress);
            mCardBackgroundColor = ThemeUtils.getCardBackgroundColor(context);
            mNameFirst = preferences.getBoolean(KEY_NAME_FIRST, true);
            mTextSize = preferences.getInt(KEY_TEXT_SIZE, res.getInteger(R.integer.default_text_size));
            mProfileImageStyle = Utils.getProfileImageStyle(preferences.getString(KEY_PROFILE_IMAGE_STYLE, null));
            mIsCompact = compact;
            if (compact) {
                mCardLayoutResource = R.layout.card_item_status_compact;
		    } else {
                mCardLayoutResource = R.layout.card_item_status;
		    }
		}

        public int findPositionById(long itemId) {
            for (int i = 0, j = getItemCount(); i < j; i++) {
                if (getItemId(i) == itemId) return i;
			}
            return -1;
		}

        public StatusFragment getFragment() {
            return mFragment;
        }

        public ImageLoaderWrapper getImageLoader() {
            return mImageLoader;
        }

        public Context getContext() {
            return mContext;
		}

	    @Override
        public ImageLoadingHandler getImageLoadingHandler() {
            return mImageLoadingHandler;
	    }

	    @Override
        public ParcelableStatus getStatus(int position) {
            final int conversationCount = getConversationCount();
            if (position == getItemCount() - 1) {
                return null;
            } else if (position < conversationCount) {
                return mConversation.get(position);
            } else if (position > conversationCount) {
                return mReplies.get(position - conversationCount - 1);
            } else {
                return mStatus;
			}
		}

	    @Override
        public int getStatusCount() {
            return getConversationCount() + 1 + getRepliesCount() + 1;
	    }

        @Override
        public long getStatusId(int position) {
            final ParcelableStatus status = getStatus(position);
            return status != null ? status.hashCode() : position;
        }

        @Override
        public int getProfileImageStyle() {
            return mProfileImageStyle;
        }

	    @Override
        public int getMediaPreviewStyle() {
            return 0;
	    }

	    @Override
        public final void onStatusClick(StatusViewHolder holder, int position) {
            if (mStatusAdapterListener != null) {
                mStatusAdapterListener.onStatusClick(holder, position);
            }
        }

        @Override
        public void onUserProfileClick(StatusViewHolder holder, int position) {
            final Context context = getContext();
            final ParcelableStatus status = getStatus(position);
            final View profileImageView = holder.getProfileImageView();
            final View profileTypeView = holder.getProfileTypeView();
            if (context instanceof FragmentActivity) {
                final Bundle options = Utils.makeSceneTransitionOption((FragmentActivity) context,
                        new Pair<>(profileImageView, UserFragment.TRANSITION_NAME_PROFILE_IMAGE),
                        new Pair<>(profileTypeView, UserFragment.TRANSITION_NAME_PROFILE_TYPE));
                Utils.openUserProfile(context, status.account_id, status.user_id, status.user_screen_name, options);
            } else {
                Utils.openUserProfile(context, status.account_id, status.user_id, status.user_screen_name, null);
	        }
        }

	    @Override
        public void setData(List<ParcelableStatus> data) {

	    }

        @Override
        public AsyncTwitterWrapper getTwitterWrapper() {
            return mFragment.getTwitterWrapper();
        }

        public float getTextSize() {
            return mTextSize;
        }

        public ParcelableStatus getStatus() {
            return mStatus;
        }

        public ParcelableCredentials getStatusAccount() {
            return mStatusAccount;
        }

		private void updateItemDecoration() {
			final DividerItemDecoration decoration = mFragment.getItemDecoration();
			decoration.setDecorationStart(0);
			if (mReplies != null) {
//                decoration.setDecorationEndOffset(2);
				decoration.setDecorationEnd(getItemCount() - 2);
			} else {
//                decoration.setDecorationEndOffset(3);
				decoration.setDecorationEnd(getItemCount() - 3);
			}
			mFragment.mRecyclerView.invalidateItemDecorations();
		}


	    @Override
        public boolean isGapItem(int position) {
            return false;
	    }

	    @Override
        public final void onGapClick(ViewHolder holder, int position) {
            if (mStatusAdapterListener != null) {
                mStatusAdapterListener.onGapClick((GapViewHolder) holder, position);
	        }
        }

        public boolean isNameFirst() {
            return mNameFirst;
	    }

	    @Override
        public void onViewDetachedFromWindow(ViewHolder holder) {
            super.onViewDetachedFromWindow(holder);
            if (holder instanceof DetailStatusViewHolder) {
                mCachedHolder = (DetailStatusViewHolder) holder;
            }
        }

        @Override
        public void onViewAttachedToWindow(ViewHolder holder) {
            super.onViewAttachedToWindow(holder);
            if (mCachedHolder == holder) {
                mCachedHolder = null;
            }
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            switch (viewType) {
                case VIEW_TYPE_DETAIL_STATUS: {
                    if (mCachedHolder != null) return mCachedHolder;
                    final View view;
                    if (mIsCompact) {
                        view = mInflater.inflate(R.layout.header_status_compact, parent, false);
                        final View cardView = view.findViewById(R.id.compact_card);
                        cardView.setBackgroundColor(mCardBackgroundColor);
                    } else {
                        view = mInflater.inflate(R.layout.header_status, parent, false);
                        final CardView cardView = (CardView) view.findViewById(R.id.card);
                        cardView.setCardBackgroundColor(mCardBackgroundColor);
                    }
                    return new DetailStatusViewHolder(this, view);
	            }
                case VIEW_TYPE_LIST_STATUS: {
                    final View view = mInflater.inflate(mCardLayoutResource, parent, false);
                    final CardView cardView = (CardView) view.findViewById(R.id.card);
                    if (cardView != null) {
                        cardView.setCardBackgroundColor(mCardBackgroundColor);
                    }
                    final StatusViewHolder holder = new StatusViewHolder(this, view);
                    holder.setOnClickListeners();
                    return holder;
			    }
                case VIEW_TYPE_CONVERSATION_LOAD_INDICATOR:
                case VIEW_TYPE_REPLIES_LOAD_INDICATOR: {
                    final View view = mInflater.inflate(R.layout.card_item_load_indicator, parent,
                            false);
                    return new LoadIndicatorViewHolder(view);
				}
                case VIEW_TYPE_SPACE: {
                    return new SpaceViewHolder(new Space(mContext));
			    }
			}
            return null;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            switch (getItemViewType(position)) {
                case VIEW_TYPE_DETAIL_STATUS: {
                    final ParcelableStatus status = getStatus(position);
                    final DetailStatusViewHolder detailHolder = (DetailStatusViewHolder) holder;
                    detailHolder.showStatus(status);
                    break;
			    }
                case VIEW_TYPE_LIST_STATUS: {
                    final ParcelableStatus status = getStatus(position);
                    final StatusViewHolder statusHolder = (StatusViewHolder) holder;
                    // Display 'in reply to' for first item
                    // useful to indicate whether first tweet has reply or not
                    statusHolder.displayStatus(status, position == 0);
                    break;
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            final int conversationCount = getConversationCount();
            if (position == getItemCount() - 1) {
                return VIEW_TYPE_SPACE;
            } else if (position < conversationCount) {
                return mConversation != null ? VIEW_TYPE_LIST_STATUS : VIEW_TYPE_CONVERSATION_LOAD_INDICATOR;
            } else if (position > conversationCount) {
                return mReplies != null ? VIEW_TYPE_LIST_STATUS : VIEW_TYPE_REPLIES_LOAD_INDICATOR;
            } else {
                return VIEW_TYPE_DETAIL_STATUS;
			}
        }

        @Override
        public long getItemId(int position) {
            final int conversationCount = getConversationCount();
            if (position == getItemCount() - 1) {
                return 4;
            } else if (position < conversationCount) {
                return mConversation != null ? mConversation.get(position).id : 2;
            } else if (position > conversationCount) {
                return mReplies != null ? mReplies.get(position - conversationCount - 1).id : 3;
            } else {
                return mStatus != null ? mStatus.id : 1;
            }
        }

        @Override
        public int getItemCount() {
            return getStatusCount();
	    }

        @Override
        public void onItemActionClick(ViewHolder holder, int id, int position) {
            if (mStatusAdapterListener != null) {
                mStatusAdapterListener.onStatusActionClick((StatusViewHolder) holder, id, position);
	        }
        }

        @Override
        public void onItemMenuClick(ViewHolder holder, View itemView, int position) {
            if (mStatusAdapterListener != null) {
                mStatusAdapterListener.onStatusMenuClick((StatusViewHolder) holder, itemView, position);
	        }
        }

        public void setConversation(List<ParcelableStatus> conversation) {
            mConversation = conversation;
            notifyDataSetChanged();
            updateItemDecoration();
	    }

        public void setEventListener(StatusAdapterListener listener) {
            mStatusAdapterListener = listener;
        }

        public void setReplies(List<ParcelableStatus> replies) {
            mReplies = replies;
            notifyDataSetChanged();
            updateItemDecoration();
	    }

        public boolean setStatus(ParcelableStatus status) {
            final ParcelableStatus old = mStatus;
            mStatus = status;
            if (status != null) {
                mStatusAccount = ParcelableAccount.getCredentials(mContext, status.account_id);
            } else {
                mStatusAccount = null;
            }
            notifyDataSetChanged();
            updateItemDecoration();
            return !CompareUtils.objectEquals(old, status);
        }

        private int getConversationCount() {
            return mConversation != null ? mConversation.size() : 1;
	    }

        private int getRepliesCount() {
            return mReplies != null ? mReplies.size() : 1;
	    }
    }

    private DividerItemDecoration getItemDecoration() {
        return mItemDecoration;
    }

    @Override
    public void onLoaderReset(final Loader<SingleResponse<ParcelableStatus>> loader) {

    }

    static class LoadConversationTask extends TwidereAsyncTask<ParcelableStatus, ParcelableStatus,
			ListResponse<ParcelableStatus>> {

        final Context context;
        final StatusFragment fragment;

        LoadConversationTask(final StatusFragment fragment) {
            context = fragment.getActivity();
            this.fragment = fragment;
		}

        @Override
        protected ListResponse<ParcelableStatus> doInBackground(final ParcelableStatus... params) {
            final ArrayList<ParcelableStatus> list = new ArrayList<>();
            try {
                ParcelableStatus status = params[0];
                final long account_id = status.account_id;
                while (status.in_reply_to_status_id > 0 && !isCancelled()) {
                    status = findStatus(context, account_id, status.in_reply_to_status_id);
                    publishProgress(status);
                    list.add(0, status);
		        }
            } catch (final TwitterException e) {
                return ListResponse.getListInstance(e);
	        }
            return ListResponse.getListInstance(list);
	    }

        @Override
        protected void onCancelled() {
	    }

		@Override
        protected void onPostExecute(final ListResponse<ParcelableStatus> data) {
            if (data.hasData()) {
                fragment.setConversation(data.getData());
            } else {
                showErrorMessage(context, context.getString(R.string.action_getting_status), data.getException(), true);
            }
        }

        @Override
        protected void onProgressUpdate(ParcelableStatus... values) {
            super.onProgressUpdate(values);
        }

    }

    private static class SpaceViewHolder extends ViewHolder {

        public SpaceViewHolder(View itemView) {
            super(itemView);
        }
    }

    private static class DetailStatusViewHolder extends ViewHolder implements OnClickListener, OnMenuItemClickListener {

        private final StatusAdapter adapter;

        private final CardView cardView;

        private final TwidereMenuBar menuBar;
        private final TextView nameView, screenNameView;
        private final StatusTextView textView;
        private final ShapedImageView profileImageView;
        private final ImageView profileTypeView;
        private final TextView timeSourceView;
        private final TextView replyRetweetStatusView;
        private final View repliesContainer, retweetsContainer, favoritesContainer;
        private final TextView repliesCountView, retweetsCountView, favoritesCountView;

        private final View profileContainer;
        private final View mediaPreviewContainer;
        private final LinearLayout mediaPreviewGrid;

        private final View locationContainer;
        private final TwitterCardContainer twitterCard;

        public DetailStatusViewHolder(StatusAdapter adapter, View itemView) {
            super(itemView);
            this.adapter = adapter;
            cardView = (CardView) itemView.findViewById(R.id.card);
            menuBar = (TwidereMenuBar) itemView.findViewById(R.id.menu_bar);
            nameView = (TextView) itemView.findViewById(R.id.name);
            screenNameView = (TextView) itemView.findViewById(R.id.screen_name);
            textView = (StatusTextView) itemView.findViewById(R.id.text);
            profileImageView = (ShapedImageView) itemView.findViewById(R.id.profile_image);
            profileTypeView = (ImageView) itemView.findViewById(R.id.profile_type);
            timeSourceView = (TextView) itemView.findViewById(R.id.time_source);
            replyRetweetStatusView = (TextView) itemView.findViewById(R.id.reply_retweet_status);
            repliesContainer = itemView.findViewById(R.id.replies_container);
            retweetsContainer = itemView.findViewById(R.id.retweets_container);
            favoritesContainer = itemView.findViewById(R.id.favorites_container);
            repliesCountView = (TextView) itemView.findViewById(R.id.replies_count);
            retweetsCountView = (TextView) itemView.findViewById(R.id.retweets_count);
            favoritesCountView = (TextView) itemView.findViewById(R.id.favorites_count);
            mediaPreviewContainer = itemView.findViewById(R.id.media_preview);
            mediaPreviewGrid = (LinearLayout) itemView.findViewById(R.id.media_preview_grid);
            locationContainer = itemView.findViewById(R.id.location_container);
            profileContainer = itemView.findViewById(R.id.profile_container);
            twitterCard = (TwitterCardContainer) itemView.findViewById(R.id.twitter_card);

            setIsRecyclable(false);
            initViews();
		}

		@Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.profile_container: {
                    final ParcelableStatus status = adapter.getStatus(getPosition());
                    final Fragment fragment = adapter.getFragment();
                    final FragmentActivity activity = fragment.getActivity();
                    final Bundle activityOption = Utils.makeSceneTransitionOption(activity,
							new Pair<View, String>(profileImageView, UserFragment.TRANSITION_NAME_PROFILE_IMAGE),
							new Pair<View, String>(profileTypeView, UserFragment.TRANSITION_NAME_PROFILE_TYPE));
                    openUserProfile(activity, status.account_id, status.user_id, status.user_screen_name,
							activityOption);
                    break;
                }
                case R.id.retweets_container: {
                    final ParcelableStatus status = adapter.getStatus(getPosition());
                    final Fragment fragment = adapter.getFragment();
                    final FragmentActivity activity = fragment.getActivity();
                    Utils.openStatusRetweeters(activity, status.account_id, status.id);
                    break;
                }
            }
        }

		@Override
        public boolean onMenuItemClick(MenuItem item) {
            final StatusFragment fragment = adapter.getFragment();
            final ParcelableStatus status = adapter.getStatus(getPosition());
            if (status == null || fragment == null) return false;
            final AsyncTwitterWrapper twitter = fragment.getTwitterWrapper();
            final FragmentActivity activity = fragment.getActivity();
            switch (item.getItemId()) {
                case MENU_SHARE: {
                    startStatusShareChooser(activity, status);
                    break;
                }
                case MENU_COPY: {
                    if (ClipboardUtils.setText(activity, status.text_plain)) {
                        showOkMessage(activity, R.string.text_copied, false);
                    }
                    break;
                }
                case MENU_RETWEET: {
                    retweet(status, twitter);
                    break;
                }
                case MENU_QUOTE: {
                    final Intent intent = new Intent(INTENT_ACTION_QUOTE);
                    intent.putExtra(EXTRA_STATUS, status);
                    fragment.startActivity(intent);
                    break;
                }
                case MENU_REPLY: {
                    final Intent intent = new Intent(INTENT_ACTION_REPLY);
                    intent.putExtra(EXTRA_STATUS, status);
                    fragment.startActivity(intent);
                    break;
                }
                case MENU_FAVORITE: {
                    favorite(status, twitter);
                    break;
                }
				case MENU_LOVE: {
					retweet(status, twitter);
					favorite(status, twitter);
					break;
				}
                case MENU_DELETE: {
                    DestroyStatusDialogFragment.show(fragment.getFragmentManager(), status);
                    break;
                }
                case MENU_ADD_TO_FILTER: {
                    AddStatusFilterDialogFragment.show(fragment.getFragmentManager(), status);
                    break;
                }
                case MENU_SET_COLOR: {
                    final Intent intent = new Intent(activity, ColorPickerDialogActivity.class);
                    final int color = getUserColor(activity, status.user_id, true);
                    if (color != 0) {
                        intent.putExtra(EXTRA_COLOR, color);
                    }
                    intent.putExtra(EXTRA_CLEAR_BUTTON, color != 0);
                    intent.putExtra(EXTRA_ALPHA_SLIDER, false);
                    fragment.startActivityForResult(intent, REQUEST_SET_COLOR);
                    break;
                }
                case MENU_TRANSLATE: {
                    final ParcelableCredentials account
                            = ParcelableAccount.getCredentials(activity, status.account_id);
                    if (TwitterContentUtils.isOfficialKey(activity, account.consumer_key, account.consumer_secret)) {
                        StatusTranslateDialogFragment.show(fragment.getFragmentManager(), status);
                    } else {
                        final Resources resources = fragment.getResources();
                        final Locale locale = resources.getConfiguration().locale;
                        try {
                            final String template = "http://translate.google.com/#%s|%s|%s";
                            final String sourceLang = "auto";
                            final String targetLang = URLEncoder.encode(locale.getLanguage(), HTTP.UTF_8);
                            final String text = URLEncoder.encode(status.text_unescaped, HTTP.UTF_8);
                            final Uri uri = Uri.parse(String.format(Locale.ROOT, template, sourceLang, targetLang, text));
                            final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                            intent.addCategory(Intent.CATEGORY_BROWSABLE);
                            fragment.startActivity(intent);
                        } catch (UnsupportedEncodingException ignore) {

                        }
                    }
                    break;
                }
                case MENU_OPEN_WITH_ACCOUNT: {
                    final Intent intent = new Intent(INTENT_ACTION_SELECT_ACCOUNT);
                    intent.setClass(activity, AccountSelectorActivity.class);
                    intent.putExtra(EXTRA_SINGLE_SELECTION, true);
                    fragment.startActivityForResult(intent, REQUEST_SELECT_ACCOUNT);
                    break;
                }
                default: {
                    if (item.getIntent() != null) {
                        try {
                            fragment.startActivity(item.getIntent());
                        } catch (final ActivityNotFoundException e) {
                            Log.w(LOGTAG, e);
                            return false;
                        }
                    }
                    break;
                }
            }
            return true;
        }

        public void showStatus(ParcelableStatus status) {
            if (status == null) return;
            final StatusFragment fragment = adapter.getFragment();
            final Context context = adapter.getContext();
            final Resources resources = context.getResources();
            final ImageLoaderWrapper loader = adapter.getImageLoader();
            nameView.setText(status.user_name);
            screenNameView.setText("@" + status.user_screen_name);

            textView.setText(Html.fromHtml(status.text_html));
            final TwidereLinkify linkify = new TwidereLinkify(new OnLinkClickHandler(context, null));
            linkify.applyAllLinks(textView, status.account_id, status.is_possibly_sensitive);
            ThemeUtils.applyParagraphSpacing(textView, 1.1f);

            textView.setMovementMethod(StatusContentMovementMethod.getInstance());
//            textView.setCustomSelectionActionModeCallback(this);


            final String timeString = formatToLongTimeString(context, status.timestamp);
            final String sourceHtml = status.source;
            if (!isEmpty(timeString) && !isEmpty(sourceHtml)) {
                timeSourceView.setText(Html.fromHtml(context.getString(R.string.time_source,
                        timeString, sourceHtml)));
            } else if (isEmpty(timeString) && !isEmpty(sourceHtml)) {
                timeSourceView.setText(Html.fromHtml(context.getString(R.string.source,
                        sourceHtml)));
            } else if (!isEmpty(timeString) && isEmpty(sourceHtml)) {
                timeSourceView.setText(timeString);
			}
            timeSourceView.setMovementMethod(LinkMovementMethod.getInstance());


            retweetsContainer.setVisibility(!status.user_is_protected ? View.VISIBLE : View.GONE);
            repliesContainer.setVisibility(status.reply_count < 0 ? View.GONE : View.VISIBLE);
            final Locale locale = context.getResources().getConfiguration().locale;
            repliesCountView.setText(getLocalizedNumber(locale, status.reply_count));
            retweetsCountView.setText(getLocalizedNumber(locale, status.retweet_count));
            favoritesCountView.setText(getLocalizedNumber(locale, status.favorite_count));

            loader.displayProfileImage(profileImageView, status.user_profile_image_url);

            final int typeIconRes = getUserTypeIconRes(status.user_is_verified, status.user_is_protected);
            if (typeIconRes != 0) {
                profileTypeView.setImageResource(typeIconRes);
                profileTypeView.setVisibility(View.VISIBLE);
            } else {
                profileTypeView.setImageDrawable(null);
                profileTypeView.setVisibility(View.GONE);
            }

            if (status.media == null) {
                mediaPreviewContainer.setVisibility(View.GONE);
            } else {
                mediaPreviewContainer.setVisibility(View.VISIBLE);
                mediaPreviewGrid.setVisibility(View.VISIBLE);
                mediaPreviewGrid.removeAllViews();
                final int maxColumns = resources.getInteger(R.integer.grid_column_image_preview);
//                MediaPreviewUtils.addToLinearLayout(mediaPreviewGrid, loader, status.media,
//						status.account_id, maxColumns, adapter.getFragment());
            }

            if (TwitterCardUtils.isCardSupported(status.card)) {
                final Point size = TwitterCardUtils.getCardSize(status.card);
                twitterCard.setVisibility(View.VISIBLE);
                if (size != null) {
                    twitterCard.setCardSize(size.x, size.y);
                } else {
                    twitterCard.setCardSize(0, 0);
                }
                final Fragment cardFragment = TwitterCardUtils.createCardFragment(status.card);
                final FragmentManager fm = fragment.getChildFragmentManager();
                final FragmentTransaction ft = fm.beginTransaction();
                ft.replace(R.id.twitter_card, cardFragment);
                ft.commit();
            } else {
                twitterCard.setVisibility(View.GONE);
                final FragmentManager fm = fragment.getChildFragmentManager();
//                final FragmentTransaction ft = fm.beginTransaction();
            }

            setMenuForStatus(context, menuBar.getMenu(), status, adapter.getStatusAccount());
            menuBar.show();
		}

        private void initViews() {
            menuBar.setOnMenuItemClickListener(this);
            menuBar.inflate(R.menu.menu_status);
            profileContainer.setOnClickListener(this);

            retweetsContainer.setOnClickListener(this);

            final float defaultTextSize = adapter.getTextSize();
            nameView.setTextSize(defaultTextSize * 1.25f);
            textView.setTextSize(defaultTextSize * 1.25f);
            screenNameView.setTextSize(defaultTextSize * 0.85f);
            timeSourceView.setTextSize(defaultTextSize * 0.85f);
    	}


    }

    private static class StatusListLinearLayoutManager extends LinearLayoutManager {

        private final RecyclerView recyclerView;

        public StatusListLinearLayoutManager(Context context, RecyclerView recyclerView) {
			super(context);
            setOrientation(LinearLayoutManager.VERTICAL);
            this.recyclerView = recyclerView;
        }

        @Override
        public int getDecoratedMeasuredHeight(View child) {
            final int height = super.getDecoratedMeasuredHeight(child);
            int heightBeforeSpace = 0;
            if (getItemViewType(child) == StatusAdapter.VIEW_TYPE_SPACE) {
                for (int i = 0, j = getChildCount(); i < j; i++) {
                    final View childToMeasure = getChildAt(i);
                    final LayoutParams paramsToMeasure = (LayoutParams) childToMeasure.getLayoutParams();
                    final int typeToMeasure = getItemViewType(childToMeasure);
                    if (typeToMeasure == StatusAdapter.VIEW_TYPE_DETAIL_STATUS || heightBeforeSpace != 0) {
                        heightBeforeSpace += super.getDecoratedMeasuredHeight(childToMeasure)
                                + paramsToMeasure.topMargin + paramsToMeasure.bottomMargin;
			        }
                    if (typeToMeasure == StatusAdapter.VIEW_TYPE_REPLIES_LOAD_INDICATOR) {
                        break;
                    }
                }
                if (heightBeforeSpace != 0) {
                    final int spaceHeight = recyclerView.getMeasuredHeight() - heightBeforeSpace;
                    return Math.max(0, spaceHeight);
		        }
            }
            return height;
        }

        @Override
        public void setOrientation(int orientation) {
            if (orientation != VERTICAL)
                throw new IllegalArgumentException("Only VERTICAL orientation supported");
            super.setOrientation(orientation);
        }

	}

}
