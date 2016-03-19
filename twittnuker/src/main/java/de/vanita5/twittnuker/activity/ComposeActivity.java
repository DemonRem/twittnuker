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

package de.vanita5.twittnuker.activity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.view.SupportMenuInflater;
import android.support.v7.widget.ActionMenuView;
import android.support.v7.widget.ActionMenuView.OnMenuItemClickListener;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.FixedLinearLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ItemDecoration;
import android.support.v7.widget.RecyclerView.State;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.Editable;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ImageSpan;
import android.text.style.SuggestionSpan;
import android.text.style.UpdateAppearance;
import android.util.Log;
import android.view.ActionMode;
import android.view.ActionMode.Callback;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.appthemeengine.Config;
import com.afollestad.appthemeengine.customizers.ATEToolbarCustomizer;
import com.afollestad.appthemeengine.util.ATEUtil;
import com.github.johnpersano.supertoasts.SuperToast;
import com.github.johnpersano.supertoasts.SuperToast.Duration;
import com.github.johnpersano.supertoasts.SuperToast.OnDismissListener;
import com.nostra13.universalimageloader.utils.IoUtils;
import com.twitter.Extractor;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.mariotaku.restfu.RestFuUtils;

import de.vanita5.twittnuker.BuildConfig;
import de.vanita5.twittnuker.R;
import de.vanita5.twittnuker.adapter.ArrayRecyclerAdapter;
import de.vanita5.twittnuker.adapter.BaseRecyclerViewAdapter;
import de.vanita5.twittnuker.fragment.BaseSupportDialogFragment;
import de.vanita5.twittnuker.fragment.SupportProgressDialogFragment;
import de.vanita5.twittnuker.model.ConsumerKeyType;
import de.vanita5.twittnuker.model.Draft;
import de.vanita5.twittnuker.model.DraftValuesCreator;
import de.vanita5.twittnuker.model.ParcelableAccount;
import de.vanita5.twittnuker.model.ParcelableCredentials;
import de.vanita5.twittnuker.model.ParcelableLocation;
import de.vanita5.twittnuker.model.ParcelableMedia;
import de.vanita5.twittnuker.model.ParcelableMediaUpdate;
import de.vanita5.twittnuker.model.ParcelableStatus;
import de.vanita5.twittnuker.model.ParcelableStatusUpdate;
import de.vanita5.twittnuker.model.ParcelableUser;
import de.vanita5.twittnuker.model.UserKey;
import de.vanita5.twittnuker.model.draft.UpdateStatusActionExtra;
import de.vanita5.twittnuker.model.util.ParcelableAccountUtils;
import de.vanita5.twittnuker.model.util.ParcelableLocationUtils;
import de.vanita5.twittnuker.preference.ServicePickerPreference;
import de.vanita5.twittnuker.provider.TwidereDataStore.Drafts;
import de.vanita5.twittnuker.service.BackgroundOperationService;
import de.vanita5.twittnuker.text.MarkForDeleteSpan;
import de.vanita5.twittnuker.text.style.EmojiSpan;
import de.vanita5.twittnuker.util.AsyncTaskUtils;
import de.vanita5.twittnuker.util.DataStoreUtils;
import de.vanita5.twittnuker.util.EditTextEnterHandler;
import de.vanita5.twittnuker.util.EditTextEnterHandler.EnterListener;
import de.vanita5.twittnuker.util.IntentUtils;
import de.vanita5.twittnuker.util.KeyboardShortcutsHandler;
import de.vanita5.twittnuker.util.MediaLoaderWrapper;
import de.vanita5.twittnuker.util.MenuUtils;
import de.vanita5.twittnuker.util.ParseUtils;
import de.vanita5.twittnuker.util.PermissionUtils;
import de.vanita5.twittnuker.util.ThemeUtils;
import de.vanita5.twittnuker.util.TwidereArrayUtils;
import de.vanita5.twittnuker.util.TwidereValidator;
import de.vanita5.twittnuker.util.TwidereViewUtils;
import de.vanita5.twittnuker.util.TwitterContentUtils;
import de.vanita5.twittnuker.util.Utils;
import de.vanita5.twittnuker.util.dagger.GeneralComponentHelper;
import de.vanita5.twittnuker.view.BadgeView;
import de.vanita5.twittnuker.view.CheckableLinearLayout;
import de.vanita5.twittnuker.view.ComposeEditText;
import de.vanita5.twittnuker.view.IconActionView;
import de.vanita5.twittnuker.view.ShapedImageView;
import de.vanita5.twittnuker.view.StatusTextCountView;
import de.vanita5.twittnuker.view.helper.SimpleItemTouchHelperCallback;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javax.inject.Inject;

public class ComposeActivity extends BaseActivity implements OnMenuItemClickListener,
        OnClickListener, OnLongClickListener, Callback, ATEToolbarCustomizer {

    // Constants
    private static final String FAKE_IMAGE_LINK = "https://www.example.com/fake_image.jpg";
    private static final String EXTRA_SHOULD_SAVE_ACCOUNTS = "should_save_accounts";
    private static final String EXTRA_ORIGINAL_TEXT = "original_text";
    private static final String EXTRA_SHARE_SCREENSHOT = "share_screenshot";
    private static final String DISCARD_STATUS_DIALOG_FRAGMENT_TAG = "discard_status";

    // Utility classes
    @Inject
    Extractor mExtractor;
    @Inject
    TwidereValidator mValidator;
    private LocationManager mLocationManager;
    private AsyncTask<Object, Object, ?> mTask;
    private SupportMenuInflater mMenuInflater;
    private ItemTouchHelper mItemTouchHelper;
    private SetProgressVisibleRunnable mSetProgressVisibleRunnable;

    // Views
    private RecyclerView mAttachedMediaPreview;
    private ActionMenuView mMenuBar;
    private ComposeEditText mEditText;
    private View mSendView;
    private StatusTextCountView mSendTextCountView;
    private RecyclerView mAccountSelector;
    private View mAccountSelectorContainer;
    private Draft mDraft;
    private ShapedImageView mProfileImageView;
    private BadgeView mCountView;
    private View mAccountSelectorButton;
    private View mLocationContainer;
    private IconActionView mLocationIcon;
    private TextView mLocationText;
    private TextView mReplyLabel;
    private View mReplyLabelDivider;

    // Adapters
    private MediaPreviewAdapter mMediaPreviewAdapter;
    private AccountIconsAdapter mAccountsAdapter;

    // Data fields
    private ParcelableLocation mRecentLocation;
    private ParcelableStatus mInReplyToStatus;
    private ParcelableUser mMentionUser;
    private String mOriginalText;
    private boolean mIsPossiblySensitive, mShouldSaveAccounts;
    private boolean mImageUploaderUsed, mStatusShortenerUsed;
    private boolean mNavigateBackPressed;
    private boolean mTextChanged;
    private boolean mFragmentResumed;
    private int mKeyMetaState;

    // Listeners
    private LocationListener mLocationListener;
    private boolean mNameFirst;

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
        switch (requestCode) {
            case REQUEST_TAKE_PHOTO:
            case REQUEST_PICK_IMAGE:
            case REQUEST_OPEN_DOCUMENT: {
                if (resultCode == Activity.RESULT_OK) {
                    final Uri src = intent.getData();
                    mTask = AsyncTaskUtils.executeTask(new AddMediaTask(this, src,
                            createTempImageUri(), ParcelableMedia.Type.IMAGE, true));
                }
                break;
            }
            case REQUEST_EDIT_IMAGE: {
                if (resultCode == Activity.RESULT_OK) {
                    final Uri uri = intent.getData();
                    if (uri == null) {
                        break;
                    }
                    setMenu();
                    updateTextCount();
                }
                break;
            }
        }

    }

    @Override
    public void onBackPressed() {
        if (mTask != null && mTask.getStatus() == AsyncTask.Status.RUNNING) return;
        if (hasComposingStatus()) {
            saveToDrafts();
            Toast.makeText(this, R.string.status_saved_to_draft, Toast.LENGTH_SHORT).show();
            finish();
        } else {
            discardTweet();
        }
    }

    protected void discardTweet() {
        if (isFinishing() || mTask != null && mTask.getStatus() == AsyncTask.Status.RUNNING) return;
        mTask = AsyncTaskUtils.executeTask(new DiscardTweetTask(this));
    }

    protected boolean hasComposingStatus() {
        final String text = mEditText != null ? ParseUtils.parseString(mEditText.getText()) : null;
        final boolean textChanged = text != null && !text.isEmpty() && !text.equals(mOriginalText);
        final boolean isEditingDraft = INTENT_ACTION_EDIT_DRAFT.equals(getIntent().getAction());
        return textChanged || hasMedia() || isEditingDraft;
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        outState.putParcelableArray(EXTRA_ACCOUNT_KEYS, mAccountsAdapter.getSelectedAccountKeys());
        outState.putParcelableArrayList(EXTRA_MEDIA, new ArrayList<Parcelable>(getMediaList()));
        outState.putBoolean(EXTRA_IS_POSSIBLY_SENSITIVE, mIsPossiblySensitive);
        outState.putParcelable(EXTRA_STATUS, mInReplyToStatus);
        outState.putParcelable(EXTRA_USER, mMentionUser);
        outState.putParcelable(EXTRA_DRAFT, mDraft);
        outState.putBoolean(EXTRA_SHOULD_SAVE_ACCOUNTS, mShouldSaveAccounts);
        outState.putString(EXTRA_ORIGINAL_TEXT, mOriginalText);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mImageUploaderUsed = !ServicePickerPreference.isNoneValue(mPreferences.getString(KEY_MEDIA_UPLOADER, null));
        mStatusShortenerUsed = !ServicePickerPreference.isNoneValue(mPreferences.getString(KEY_STATUS_SHORTENER, null));
        requestOrUpdateLocation();
        setMenu();
        updateTextCount();
        final int textSize = mPreferences.getInt(KEY_TEXT_SIZE, Utils.getDefaultTextSize(this));
        mEditText.setTextSize(textSize * 1.25f);
    }

    @Override
    protected void onStop() {
        saveAccountSelection();
        try {
            if (mLocationListener != null) {
                mLocationManager.removeUpdates(mLocationListener);
                mLocationListener = null;
            }
        } catch (SecurityException ignore) {
            // That should not happen
        }
        super.onStop();
    }

    @Override
    public void onClick(final View view) {
        switch (view.getId()) {
            case R.id.send: {
                confirmAndUpdateStatus();
                break;
            }
            case R.id.account_selector_container: {
                setAccountSelectorVisible(false);
                break;
            }
            case R.id.account_selector_button: {
                setAccountSelectorVisible(!isAccountSelectorVisible());
                break;
            }
            case R.id.location_container: {
                toggleLocation();
                break;
            }
        }
    }

    private boolean isAccountSelectorVisible() {
        return mAccountSelectorContainer.getVisibility() == View.VISIBLE;
    }

    private void confirmAndUpdateStatus() {
        if (isQuotingProtectedStatus()) {
            new RetweetProtectedStatusWarnFragment().show(getSupportFragmentManager(),
                    "retweet_protected_status_warning_message");
        } else {
            updateStatus();
        }
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        final Window window = getWindow();
        final View contentView = window.findViewById(android.R.id.content);
        contentView.setPadding(contentView.getPaddingLeft(), 0,
                contentView.getPaddingRight(), contentView.getPaddingBottom());
    }

    @Override
    public boolean onLongClick(final View v) {
        switch (v.getId()) {
            case R.id.send: {
                Utils.showMenuItemToast(v, getString(R.string.send), true);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onMenuItemClick(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.take_photo: {
                takePhoto();
                break;
            }
            case R.id.add_image: {
                pickImage();
                break;
            }
            case R.id.add_location: {
                toggleLocation();
                break;
            }
            case R.id.drafts: {
                IntentUtils.openDrafts(this);
                break;
            }
            case R.id.delete: {
                AsyncTaskUtils.executeTask(new DeleteMediaTask(this));
                break;
            }
            case R.id.toggle_sensitive: {
                if (!hasMedia()) return false;
                mIsPossiblySensitive = !mIsPossiblySensitive;
                setMenu();
                updateTextCount();
                break;
            }

            case R.id.add_hashtag: {
                final int selectionEnd = mEditText.getSelectionEnd() + 1;
                final String withHashtag =
                        mEditText.getText().subSequence(0, mEditText.getSelectionEnd()) +
                                "#" +
                                mEditText.getText().subSequence(mEditText.getSelectionEnd(), mEditText.length());
                mEditText.setText(withHashtag);
                mEditText.setSelection(selectionEnd);
                break;
            }
            default: {
                break;
            }
        }
        return true;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                final float x = ev.getRawX(), y = ev.getRawY();
                if (isAccountSelectorVisible()) {
                    if (!TwidereViewUtils.hitView(x, y, mAccountSelectorButton)) {
                        boolean clickedItem = false;
                        final RecyclerView.LayoutManager layoutManager = mAccountSelector.getLayoutManager();
                        for (int i = 0, j = layoutManager.getChildCount(); i < j; i++) {
                            if (TwidereViewUtils.hitView(x, y, layoutManager.getChildAt(i))) {
                                clickedItem = true;
                                break;
                            }
                        }
                        if (!clickedItem) {
                            setAccountSelectorVisible(false);
                            return true;
                        }
                    }
                }
                break;
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            final float x = event.getRawX(), y = event.getRawY();
            final Window window = getWindow();
            if (!TwidereViewUtils.hitView(x, y, window.getDecorView())
                    && window.peekDecorView() != null && !hasComposingStatus()) {
                onBackPressed();
                return true;
            }
        }
        return super.onTouchEvent(event);
    }

    @Override
    public void onContentChanged() {
        super.onContentChanged();
        mEditText = (ComposeEditText) findViewById(R.id.edit_text);
        mAttachedMediaPreview = (RecyclerView) findViewById(R.id.attached_media_preview);
        mMenuBar = (ActionMenuView) findViewById(R.id.menu_bar);
        mSendView = findViewById(R.id.send);
        mSendTextCountView = (StatusTextCountView) mSendView.findViewById(R.id.status_text_count);
        mAccountSelector = (RecyclerView) findViewById(R.id.account_selector);
        mAccountSelectorContainer = findViewById(R.id.account_selector_container);
        mProfileImageView = (ShapedImageView) findViewById(R.id.account_profile_image);
        mCountView = (BadgeView) findViewById(R.id.accounts_count);
        mAccountSelectorButton = findViewById(R.id.account_selector_button);
        mLocationContainer = findViewById(R.id.location_container);
        mLocationIcon = (IconActionView) findViewById(R.id.location_icon);
        mLocationText = (TextView) findViewById(R.id.location_text);
        mReplyLabel = (TextView) findViewById(R.id.reply_label);
        mReplyLabelDivider = findViewById(R.id.reply_label_divider);
    }

    @NonNull
    @Override
    public MenuInflater getMenuInflater() {
        if (mMenuInflater == null) {
            mMenuInflater = new SupportMenuInflater(this);
        }
        return mMenuInflater;
    }

    public void removeAllMedia(final List<ParcelableMediaUpdate> list) {
        mMediaPreviewAdapter.removeAll(list);
    }

    public void saveToDrafts() {
        final String text = mEditText != null ? ParseUtils.parseString(mEditText.getText()) : null;
        final Draft draft = new Draft();

        draft.action_type = getDraftAction(getIntent().getAction());
        draft.account_ids = mAccountsAdapter.getSelectedAccountKeys();
        draft.text = text;
        final UpdateStatusActionExtra extra = new UpdateStatusActionExtra();
        extra.setInReplyToStatus(mInReplyToStatus);
        extra.setIsPossiblySensitive(mIsPossiblySensitive);
        draft.action_extras = extra;
        draft.media = getMedia();
        draft.location = mRecentLocation;
        final ContentValues values = DraftValuesCreator.create(draft);
        final Uri draftUri = getContentResolver().insert(Drafts.CONTENT_URI, values);
        displayNewDraftNotification(text, draftUri);
    }

    static String getDraftAction(String intentAction) {
        if (intentAction == null) {
            return Draft.Action.UPDATE_STATUS;
        }
        switch (intentAction) {
            case INTENT_ACTION_REPLY: {
                return Draft.Action.REPLY;
            }
            case INTENT_ACTION_QUOTE: {
                return Draft.Action.QUOTE;
            }
        }
        return Draft.Action.UPDATE_STATUS;
    }

    public void setSelectedAccounts(ParcelableAccount... accounts) {
        if (accounts.length == 1) {
            mCountView.setText(null);
            final ParcelableAccount account = accounts[0];
            mMediaLoader.displayProfileImage(mProfileImageView, account);
            mProfileImageView.setBorderColor(account.color);
        } else {
            mCountView.setText(String.valueOf(accounts.length));
            mMediaLoader.cancelDisplayTask(mProfileImageView);
            mProfileImageView.setImageDrawable(null);
            mProfileImageView.setBorderColors(Utils.getAccountColors(accounts));
        }
    }

    @Override
    public void onActionModeStarted(ActionMode mode) {
        super.onActionModeStarted(mode);
        ThemeUtils.applyColorFilterToMenuIcon(mode.getMenu(), ThemeUtils.getContrastActionBarItemColor(this),
                0, 0, Mode.MULTIPLY);
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        GeneralComponentHelper.build(this).inject(this);
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mNameFirst = mPreferences.getBoolean(KEY_NAME_FIRST);
        setContentView(R.layout.activity_compose);
        setFinishOnTouchOutside(false);
        final ParcelableCredentials[] accounts = DataStoreUtils.getCredentialsArray(this, false, false);
        if (accounts.length <= 0) {
            final Intent intent = new Intent(INTENT_ACTION_TWITTER_LOGIN);
            intent.setClass(this, SignInActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        final UserKey[] defaultAccountIds = ParcelableAccountUtils.getAccountKeys(accounts);
        mMenuBar.setOnMenuItemClickListener(this);
        setupEditText();
        mAccountSelectorContainer.setOnClickListener(this);
        mAccountSelectorButton.setOnClickListener(this);
        mLocationContainer.setOnClickListener(this);

        final LinearLayoutManager linearLayoutManager = new FixedLinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        linearLayoutManager.setStackFromEnd(true);
        mAccountSelector.setLayoutManager(linearLayoutManager);
        mAccountSelector.setItemAnimator(new DefaultItemAnimator());
        mAccountsAdapter = new AccountIconsAdapter(this);
        mAccountSelector.setAdapter(mAccountsAdapter);
        mAccountsAdapter.setAccounts(accounts);


        mMediaPreviewAdapter = new MediaPreviewAdapter(this, new PreviewGridOnStartDragListener(this));
        mItemTouchHelper = new ItemTouchHelper(new AttachedMediaItemTouchHelperCallback(mMediaPreviewAdapter));
        final LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        mAttachedMediaPreview.setLayoutManager(layoutManager);
        mAttachedMediaPreview.setAdapter(mMediaPreviewAdapter);
        mItemTouchHelper.attachToRecyclerView(mAttachedMediaPreview);
        final int previewGridSpacing = getResources().getDimensionPixelSize(R.dimen.element_spacing_small);
        mAttachedMediaPreview.addItemDecoration(new PreviewGridItemDecoration(previewGridSpacing));

        final Intent intent = getIntent();

        if (savedInstanceState != null) {
            // Restore from previous saved state
            mAccountsAdapter.setSelectedAccountIds(Utils.newParcelableArray(
                    savedInstanceState.getParcelableArray(EXTRA_ACCOUNT_KEY), UserKey.CREATOR));
            mIsPossiblySensitive = savedInstanceState.getBoolean(EXTRA_IS_POSSIBLY_SENSITIVE);
            final ArrayList<ParcelableMediaUpdate> mediaList = savedInstanceState.getParcelableArrayList(EXTRA_MEDIA);
            if (mediaList != null) {
                addMedia(mediaList);
            }
            mInReplyToStatus = savedInstanceState.getParcelable(EXTRA_STATUS);
            mMentionUser = savedInstanceState.getParcelable(EXTRA_USER);
            mDraft = savedInstanceState.getParcelable(EXTRA_DRAFT);
            mShouldSaveAccounts = savedInstanceState.getBoolean(EXTRA_SHOULD_SAVE_ACCOUNTS);
            mOriginalText = savedInstanceState.getString(EXTRA_ORIGINAL_TEXT);
            setLabel(intent);
        } else {
            // The context was first created
            final int notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1);
            if (notificationId != -1) {
                NotificationManager notificationManager =
                        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                if (notificationManager != null) {
                    notificationManager.cancel(notificationId);
                }
            }
            if (!handleIntent(intent)) {
                handleDefaultIntent(intent);
            }
            setLabel(intent);
            final UserKey[] selectedAccountIds = mAccountsAdapter.getSelectedAccountKeys();
            if (ArrayUtils.isEmpty(selectedAccountIds)) {
                final UserKey[] idsInPrefs = UserKey.arrayOf(mPreferences.getString(KEY_COMPOSE_ACCOUNTS, null));
                UserKey[] intersection = null;
                if (idsInPrefs != null) {
                    intersection = TwidereArrayUtils.intersection(idsInPrefs, defaultAccountIds);
                }

                mAccountsAdapter.setSelectedAccountIds(ArrayUtils.isEmpty(intersection) ? defaultAccountIds : intersection);
            }
            mOriginalText = ParseUtils.parseString(mEditText.getText());
        }

        final Menu menu = mMenuBar.getMenu();
        getMenuInflater().inflate(R.menu.menu_compose, menu);
        ThemeUtils.wrapMenuIcon(mMenuBar);

        mSendView.setOnClickListener(this);
        mSendView.setOnLongClickListener(this);
        setMenu();
        updateLocationState();
        notifyAccountSelectionChanged();

        mTextChanged = false;

        updateAttachedMediaView();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        final int keyCode = event.getKeyCode();
        if (KeyEvent.isModifierKey(keyCode)) {
            final int action = event.getAction();
            if (action == MotionEvent.ACTION_DOWN) {
                mKeyMetaState |= KeyboardShortcutsHandler.getMetaStateForKeyCode(keyCode);
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                mKeyMetaState &= ~KeyboardShortcutsHandler.getMetaStateForKeyCode(keyCode);
            }
        }
        return super.dispatchKeyEvent(event);
    }

    private void setupEditText() {
        final boolean sendByEnter = mPreferences.getBoolean(KEY_QUICK_SEND);
        EditTextEnterHandler.attach(mEditText, new ComposeEnterListener(this), sendByEnter);
        mEditText.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {

            }

            @Override
            public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
                setMenu();
                updateTextCount();
                if (s instanceof Spannable && count == 1 && before == 0) {
                    final ImageSpan[] imageSpans = ((Spannable) s).getSpans(start, start + count, ImageSpan.class);
                    List<String> imageSources = new ArrayList<>();
                    for (ImageSpan imageSpan : imageSpans) {
                        imageSources.add(imageSpan.getSource());
                        ((Spannable) s).setSpan(new MarkForDeleteSpan(), start, start + count,
                                Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                    }
                    if (!imageSources.isEmpty()) {
                        final Intent intent = ThemedImagePickerActivity.withThemed(ComposeActivity.this)
                                .getImage(Uri.parse(imageSources.get(0))).build();
                        startActivityForResult(intent, REQUEST_PICK_IMAGE);
                    }
                }
            }

            @Override
            public void afterTextChanged(final Editable s) {
                mTextChanged = s.length() == 0;
                final MarkForDeleteSpan[] deletes = s.getSpans(0, s.length(), MarkForDeleteSpan.class);
                for (MarkForDeleteSpan delete : deletes) {
                    s.delete(s.getSpanStart(delete), s.getSpanEnd(delete));
                    s.removeSpan(delete);
                }
                for (Object span : s.getSpans(0, s.length(), UpdateAppearance.class)) {
                    trimSpans(s, span);
                }
            }

            private void trimSpans(Editable s, Object span) {
                if (span instanceof EmojiSpan) return;
                if (span instanceof SuggestionSpan) return;
                s.removeSpan(span);
            }
        });
        mEditText.setCustomSelectionActionModeCallback(this);
    }

    @Override
    protected void onTitleChanged(final CharSequence title, final int color) {
        super.onTitleChanged(title, color);
    }

    @Override
    public boolean handleKeyboardShortcutSingle(@NonNull KeyboardShortcutsHandler handler, int keyCode, @NonNull KeyEvent event, int metaState) {
        final String action = handler.getKeyAction(CONTEXT_TAG_NAVIGATION, keyCode, event, metaState);
        if (ACTION_NAVIGATION_BACK.equals(action)) {
            if (mEditText.length() == 0 && !mTextChanged) {
                if (!mNavigateBackPressed) {
                    final SuperToast toast = SuperToast.create(this, getString(R.string.press_again_to_close), Duration.SHORT);
                    toast.setOnDismissListener(new OnDismissListener() {
                        @Override
                        public void onDismiss(View view) {
                            mNavigateBackPressed = false;
                        }
                    });
                    toast.show();
                } else {
                    onBackPressed();
                }
                mNavigateBackPressed = true;
            } else {
                mTextChanged = false;
            }
            return true;
        }
        return super.handleKeyboardShortcutSingle(handler, keyCode, event, metaState);
    }

    @Override
    public boolean handleKeyboardShortcutRepeat(@NonNull KeyboardShortcutsHandler handler, int keyCode,
                                                int repeatCount, @NonNull KeyEvent event, int metaState) {
        return super.handleKeyboardShortcutRepeat(handler, keyCode, repeatCount, event, metaState);
    }

    private void addMedia(final ParcelableMediaUpdate media) {
        mMediaPreviewAdapter.add(media);
        updateAttachedMediaView();
    }

    private void addMedia(final List<ParcelableMediaUpdate> media) {
        mMediaPreviewAdapter.addAll(media);
        updateAttachedMediaView();
    }

    private void clearMedia() {
        mMediaPreviewAdapter.clear();
        updateAttachedMediaView();
    }

    private void updateAttachedMediaView() {
        final boolean hasMedia = hasMedia();
        mAttachedMediaPreview.setVisibility(hasMedia ? View.VISIBLE : View.GONE);
        if (hasMedia) {
            mEditText.setMinLines(getResources().getInteger(R.integer.media_compose_min_lines));
        } else {
            mEditText.setMinLines(getResources().getInteger(R.integer.default_compose_min_lines));
        }
        setMenu();
    }

    private Uri createTempImageUri() {
        final File file = new File(getCacheDir(), "tmp_image_" + System.currentTimeMillis());
        return Uri.fromFile(file);
    }

    private void displayNewDraftNotification(String text, Uri draftUri) {
        final ContentValues values = new ContentValues();
        values.put(BaseColumns._ID, draftUri.getLastPathSegment());
        getContentResolver().insert(Drafts.CONTENT_URI_NOTIFICATIONS, values);
    }

    private ParcelableMediaUpdate[] getMedia() {
        final List<ParcelableMediaUpdate> list = getMediaList();
        return list.toArray(new ParcelableMediaUpdate[list.size()]);
    }

    private List<ParcelableMediaUpdate> getMediaList() {
        return mMediaPreviewAdapter.getAsList();
    }

    private boolean handleDefaultIntent(final Intent intent) {
        if (intent == null) return false;
        final String action = intent.getAction();
        final boolean hasAccountIds;
        if (intent.hasExtra(EXTRA_ACCOUNT_KEYS)) {
            final UserKey[] accountKeys = Utils.newParcelableArray(
                    intent.getParcelableArrayExtra(EXTRA_ACCOUNT_KEYS), UserKey.CREATOR);
            mAccountsAdapter.setSelectedAccountIds(accountKeys);
            hasAccountIds = true;
        } else if (intent.hasExtra(EXTRA_ACCOUNT_KEY)) {
            final UserKey accountKey = intent.getParcelableExtra(EXTRA_ACCOUNT_KEY);
            mAccountsAdapter.setSelectedAccountIds(accountKey);
            hasAccountIds = true;
        } else {
            hasAccountIds = false;
        }
        mShouldSaveAccounts = !Intent.ACTION_SEND.equals(action)
                && !Intent.ACTION_SEND_MULTIPLE.equals(action) && !hasAccountIds;
        final Uri data = intent.getData();
        final CharSequence extraSubject = intent.getCharSequenceExtra(Intent.EXTRA_SUBJECT);
        final CharSequence extraText = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
        final Uri extraStream = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        //TODO handle share_screenshot extra (Bitmap)
        if (extraStream != null) {
            AsyncTaskUtils.executeTask(new AddMediaTask(this, extraStream, createTempImageUri(), ParcelableMedia.Type.IMAGE, false));
        } else if (data != null) {
            AsyncTaskUtils.executeTask(new AddMediaTask(this, data, createTempImageUri(), ParcelableMedia.Type.IMAGE, false));
        } else if (intent.hasExtra(EXTRA_SHARE_SCREENSHOT) && Utils.useShareScreenshot()) {
            final Bitmap bitmap = intent.getParcelableExtra(EXTRA_SHARE_SCREENSHOT);
            if (bitmap != null) {
                try {
                    AsyncTaskUtils.executeTask(new AddBitmapTask(this, bitmap, createTempImageUri(), ParcelableMedia.Type.IMAGE));
                } catch (IOException e) {
                    // ignore
                    bitmap.recycle();
                }
            }
        }
        mEditText.setText(Utils.getShareStatus(this, extraSubject, extraText));
        final int selectionEnd = mEditText.length();
        mEditText.setSelection(selectionEnd);
        return true;
    }

    private boolean handleEditDraftIntent(final Draft draft) {
        if (draft == null) return false;
        mEditText.setText(draft.text);
        final int selectionEnd = mEditText.length();
        mEditText.setSelection(selectionEnd);
        mAccountsAdapter.setSelectedAccountIds(draft.account_ids);
        if (draft.media != null) {
            addMedia(Arrays.asList(draft.media));
        }
        mRecentLocation = draft.location;
        if (draft.action_extras instanceof UpdateStatusActionExtra) {
            final UpdateStatusActionExtra extra = (UpdateStatusActionExtra) draft.action_extras;
            mIsPossiblySensitive = extra.isPossiblySensitive();
            mInReplyToStatus = extra.getInReplyToStatus();
        }
        return true;
    }

    private boolean setLabel(final Intent intent) {
        final String action = intent.getAction();
        if (action == null) {
            hideLabel();
            return false;
        }
        switch (action) {
            case INTENT_ACTION_REPLY: {
                showReplyLabel(intent.<ParcelableStatus>getParcelableExtra(EXTRA_STATUS));
                return true;
            }
            case INTENT_ACTION_QUOTE: {
                showQuoteLabel(intent.<ParcelableStatus>getParcelableExtra(EXTRA_STATUS));
                return true;
            }
            case INTENT_ACTION_EDIT_DRAFT: {
                Draft draft = intent.getParcelableExtra(EXTRA_DRAFT);
                if (draft == null) {
                    hideLabel();
                    return false;
                }
                if (draft.action_type == null) {
                    draft.action_type = Draft.Action.UPDATE_STATUS;
                }
                switch (draft.action_type) {
                    case Draft.Action.REPLY: {
                        if (draft.action_extras instanceof UpdateStatusActionExtra) {
                            showReplyLabel(((UpdateStatusActionExtra) draft.action_extras).getInReplyToStatus());
                        } else {
                            hideLabel();
                            return false;
                        }
                        break;
                    }
                    case Draft.Action.QUOTE: {
                        if (draft.action_extras instanceof UpdateStatusActionExtra) {
                            showQuoteLabel(((UpdateStatusActionExtra) draft.action_extras).getInReplyToStatus());
                        } else {
                            hideLabel();
                            return false;
                        }
                        break;
                    }
                    default: {
                        hideLabel();
                        return false;
                    }
                }
                return true;
            }
        }
        hideLabel();
        return false;
    }

    private boolean handleIntent(final Intent intent) {
        final String action = intent.getAction();
        if (action == null) return false;
        mShouldSaveAccounts = false;
        mMentionUser = intent.getParcelableExtra(EXTRA_USER);
        mInReplyToStatus = intent.getParcelableExtra(EXTRA_STATUS);
        switch (action) {
            case INTENT_ACTION_REPLY: {
                return handleReplyIntent(mInReplyToStatus);
            }
            case INTENT_ACTION_QUOTE: {
                return handleQuoteIntent(mInReplyToStatus);
            }
            case INTENT_ACTION_EDIT_DRAFT: {
                mDraft = intent.getParcelableExtra(EXTRA_DRAFT);
                return handleEditDraftIntent(mDraft);
            }
            case INTENT_ACTION_MENTION: {
                return handleMentionIntent(mMentionUser);
            }
            case INTENT_ACTION_REPLY_MULTIPLE: {
                final String[] screenNames = intent.getStringArrayExtra(EXTRA_SCREEN_NAMES);
                final UserKey accountKey = intent.getParcelableExtra(EXTRA_ACCOUNT_KEYS);
                final ParcelableStatus inReplyToStatus = intent.getParcelableExtra(EXTRA_IN_REPLY_TO_STATUS);
                return handleReplyMultipleIntent(screenNames, accountKey, inReplyToStatus);
            }
            case INTENT_ACTION_COMPOSE_TAKE_PHOTO: {
                return takePhoto();
            }
            case INTENT_ACTION_COMPOSE_PICK_IMAGE: {
                return pickImage();
            }
        }
        // Unknown action or no intent extras
        return false;
    }

    private boolean handleMentionIntent(final ParcelableUser user) {
        if (user == null || user.key == null) return false;
        final String accountScreenName = DataStoreUtils.getAccountScreenName(this, user.account_key);
        if (TextUtils.isEmpty(accountScreenName)) return false;
        mEditText.setText(String.format("@%s ", user.screen_name));
        final int selection_end = mEditText.length();
        mEditText.setSelection(selection_end);
        mAccountsAdapter.setSelectedAccountIds(user.account_key);
        return true;
    }

    private boolean handleQuoteIntent(final ParcelableStatus status) {
        if (status == null) return false;
        mEditText.setText(Utils.getQuoteStatus(this, status.id, status.user_screen_name, status.text_plain));
        mEditText.setSelection(0);
        mAccountsAdapter.setSelectedAccountIds(status.account_key);
        showQuoteLabel(status);
        return true;
    }

    private void showQuoteLabel(ParcelableStatus status) {
        if (status == null) {
            hideLabel();
            return;
        }
        final String replyToName = mUserColorNameManager.getDisplayName(status, mNameFirst, false);
        mReplyLabel.setText(getString(R.string.quote_name_text, replyToName, status.text_unescaped));
        mReplyLabel.setVisibility(View.VISIBLE);
        mReplyLabelDivider.setVisibility(View.VISIBLE);
    }

    private void showReplyLabel(ParcelableStatus status) {
        if (status == null) {
            hideLabel();
            return;
        }
        final String replyToName = mUserColorNameManager.getDisplayName(status, mNameFirst, false);
        mReplyLabel.setText(getString(R.string.reply_to_name_text, replyToName, status.text_unescaped));
        mReplyLabel.setVisibility(View.VISIBLE);
        mReplyLabelDivider.setVisibility(View.VISIBLE);
    }

    private void hideLabel() {
        mReplyLabel.setVisibility(View.GONE);
        mReplyLabelDivider.setVisibility(View.GONE);
    }

    private boolean handleReplyIntent(final ParcelableStatus status) {
        if (status == null || status.id == null) return false;
        final String myScreenName = DataStoreUtils.getAccountScreenName(this, status.account_key);
        if (TextUtils.isEmpty(myScreenName)) return false;
        int selectionStart = 0;
        mEditText.append("@" + status.user_screen_name + " ");
        // If replying status from current user, just exclude it's screen name from selection.
        if (!status.account_key.equals(status.user_key)) {
            selectionStart = mEditText.length();
        }
        if (status.is_retweet) {
            mEditText.append("@" + status.retweeted_by_user_screen_name + " ");
        }
        if (status.is_quote) {
            mEditText.append("@" + status.quoted_user_screen_name + " ");
        }
        final Collection<String> mentions = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        mentions.addAll(mExtractor.extractMentionedScreennames(status.text_plain));
        mentions.addAll(mExtractor.extractMentionedScreennames(status.quoted_text_plain));

        for (final String mention : mentions) {
            if (mention.equalsIgnoreCase(status.user_screen_name) || mention.equalsIgnoreCase(myScreenName)
                    || mention.equalsIgnoreCase(status.retweeted_by_user_screen_name)) {
                continue;
            }
            mEditText.append("@" + mention + " ");
        }
        final int selectionEnd = mEditText.length();
        mEditText.setSelection(selectionStart, selectionEnd);
        mAccountsAdapter.setSelectedAccountIds(status.account_key);
        showReplyLabel(status);
        return true;
    }

    private boolean handleReplyMultipleIntent(final String[] screenNames, final UserKey accountId,
                                              final ParcelableStatus inReplyToStatus) {
        if (screenNames == null || screenNames.length == 0 || accountId == null) return false;
        final String myScreenName = DataStoreUtils.getAccountScreenName(this, accountId);
        if (TextUtils.isEmpty(myScreenName)) return false;
        for (final String screenName : screenNames) {
            if (screenName.equalsIgnoreCase(myScreenName)) {
                continue;
            }
            mEditText.append("@" + screenName + " ");
        }
        mEditText.setSelection(mEditText.length());
        mAccountsAdapter.setSelectedAccountIds(accountId);
        mInReplyToStatus = inReplyToStatus;
        return true;
    }

    private boolean hasMedia() {
        return mMediaPreviewAdapter.getItemCount() > 0;
    }

    private boolean isQuote() {
        return INTENT_ACTION_QUOTE.equals(getIntent().getAction());
    }

    private boolean isQuotingProtectedStatus() {
        final ParcelableStatus status = mInReplyToStatus;
        if (!isQuote() || status == null) return false;
        return status.user_is_protected && !status.account_key.equals(status.user_key);
    }

    private boolean noReplyContent(final String text) {
        if (text == null) return true;
        final String action = getIntent().getAction();
        final boolean is_reply = INTENT_ACTION_REPLY.equals(action) || INTENT_ACTION_REPLY_MULTIPLE.equals(action);
        return is_reply && text.equals(mOriginalText);
    }

    private void notifyAccountSelectionChanged() {
        final ParcelableCredentials[] accounts = mAccountsAdapter.getSelectedAccounts();
        setSelectedAccounts(accounts);
        if (ArrayUtils.isEmpty(accounts)) {
            mEditText.setAccountKey(Utils.getDefaultAccountKey(this));
        } else {
            mEditText.setAccountKey(accounts[0].account_key);
        }
        mSendTextCountView.setMaxLength(TwidereValidator.getTextLimit(accounts));
        setMenu();
//        mAccountActionProvider.setSelectedAccounts(mAccountsAdapter.getSelectedAccounts());
    }

    private boolean pickImage() {
        final Intent intent = ThemedImagePickerActivity.withThemed(this).pickImage().build();
        startActivityForResult(intent, REQUEST_PICK_IMAGE);
        return true;
    }

    private void saveAccountSelection() {
        if (!mShouldSaveAccounts) return;
        final SharedPreferences.Editor editor = mPreferences.edit();
        editor.putString(KEY_COMPOSE_ACCOUNTS, TwidereArrayUtils.toString(mAccountsAdapter.getSelectedAccountKeys(), ',', false));
        editor.apply();
    }

    private void setAccountSelectorVisible(boolean visible) {
        mAccountSelectorContainer.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void setMenu() {
        if (mMenuBar == null) return;
        final Menu menu = mMenuBar.getMenu();
        final boolean hasMedia = hasMedia();

        /*
         * No media & Not reply: [[Take photo][Add image]][Attach location][Drafts]
         * Has media & Not reply: [[Take photo][Add image]][Medias menu][Attach location][Drafts]
         * Is reply: [Medias menu][View status][Attach location][Drafts]
         */
        MenuUtils.setMenuItemAvailability(menu, R.id.take_photo, true); //always
        MenuUtils.setMenuItemAvailability(menu, R.id.add_image, true); //always
        MenuUtils.setMenuItemAvailability(menu, R.id.media_menu, hasMedia);
        MenuUtils.setMenuItemAvailability(menu, R.id.toggle_sensitive, hasMedia);
        MenuUtils.setMenuItemAvailability(menu, R.id.schedule, isScheduleSupported());

        MenuUtils.setMenuItemChecked(menu, R.id.toggle_sensitive, hasMedia && mIsPossiblySensitive);
        ThemeUtils.resetCheatSheet(mMenuBar);
//        mMenuBar.show();
    }

    private boolean isScheduleSupported() {
        for (ParcelableCredentials account : mAccountsAdapter.getSelectedAccounts()) {
            if (TwitterContentUtils.getOfficialKeyType(this, account.consumer_key, account.consumer_secret)
                    != ConsumerKeyType.TWEETDECK) {
                return false;
            }
        }
        return true;
    }

    private void setProgressVisible(final boolean visible) {
        if (isFinishing()) return;
        mSetProgressVisibleRunnable = new SetProgressVisibleRunnable(this, visible);
        if (mFragmentResumed) {
            runOnUiThread(mSetProgressVisibleRunnable);
            mSetProgressVisibleRunnable = null;
        }
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        mFragmentResumed = true;
        if (mSetProgressVisibleRunnable != null) {
            runOnUiThread(mSetProgressVisibleRunnable);
            mSetProgressVisibleRunnable = null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mFragmentResumed = false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (PermissionUtils.getPermission(permissions, grantResults, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                PermissionUtils.getPermission(permissions, grantResults, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                startLocationUpdateIfEnabled();
            } catch (SecurityException e) {
                // That should not happen
            }
        } else {
            Toast.makeText(this, R.string.cannot_get_location, Toast.LENGTH_SHORT).show();
        }
    }

    private void setRecentLocation(ParcelableLocation location) {
        if (location != null) {
            mLocationText.setText(ParcelableLocationUtils.getHumanReadableString(location, 3));
        } else {
            mLocationText.setText(R.string.unknown_location);
        }
        mRecentLocation = location;
    }

    /**
     * The Location Manager manages location providers. This code searches for
     * the best provider of data (GPS, WiFi/cell phone tower lookup, some other
     * mechanism) and finds the last known location.
     */
    private boolean startLocationUpdateIfEnabled() throws SecurityException {
        if (mLocationListener != null) return true;
        final boolean attachLocation = mPreferences.getBoolean(KEY_ATTACH_LOCATION);
        if (!attachLocation) {
            return false;
        }
        final Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        final String provider = mLocationManager.getBestProvider(criteria, true);
        if (provider != null) {
            mLocationText.setText(R.string.getting_location);
            mLocationListener = new ComposeLocationListener(this);
            mLocationManager.requestLocationUpdates(provider, 0, 0, mLocationListener);
            final Location location = Utils.getCachedLocation(this);
            if (location != null) {
                mLocationListener.onLocationChanged(location);
            }
        } else {
            Toast.makeText(this, R.string.cannot_get_location, Toast.LENGTH_SHORT).show();
        }
        return provider != null;
    }

    private boolean takePhoto() {
        final Intent intent = ThemedImagePickerActivity.withThemed(this).takePhoto().build();
        startActivityForResult(intent, REQUEST_TAKE_PHOTO);
        return true;
    }

    private void toggleLocation() throws SecurityException {
        final boolean attachLocation = !mPreferences.getBoolean(KEY_ATTACH_LOCATION, false);
        mPreferences.edit().putBoolean(KEY_ATTACH_LOCATION, attachLocation).apply();
        if (!attachLocation && mLocationListener != null) {
            mLocationManager.removeUpdates(mLocationListener);
            mLocationListener = null;
        }
        requestOrUpdateLocation();
        updateLocationState();
        setMenu();
        updateTextCount();
    }

    private void requestOrUpdateLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdateIfEnabled();
        } else {
            final String[] permissions = {Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION};
            ActivityCompat.requestPermissions(this, permissions, REQUEST_REQUEST_PERMISSIONS);
        }
    }

    private void updateLocationState() {
        final boolean attachLocation = mPreferences.getBoolean(KEY_ATTACH_LOCATION, false);
        mLocationIcon.setActivated(attachLocation);
        if (!attachLocation) {
            mLocationText.setText(R.string.no_location);
        }
    }

    private void updateStatus() {
        if (isFinishing()) return;
        final boolean hasMedia = hasMedia();
        final String text = mEditText != null ? ParseUtils.parseString(mEditText.getText()) : null;
        final int tweetLength = mValidator.getTweetLength(text), maxLength = mSendTextCountView.getMaxLength();
        if (!mStatusShortenerUsed && tweetLength > maxLength) {
            mEditText.setError(getString(R.string.error_message_status_too_long));
            final int textLength = mEditText.length();
            mEditText.setSelection(textLength - (tweetLength - maxLength), textLength);
            return;
        } else if (!hasMedia && (TextUtils.isEmpty(text) || noReplyContent(text))) {
            mEditText.setError(getString(R.string.error_message_no_content));
            return;
        } else if (mAccountsAdapter.isSelectionEmpty()) {
            mEditText.setError(getString(R.string.no_account_selected));
            return;
        }
        final boolean attachLocation = mPreferences.getBoolean(KEY_ATTACH_LOCATION, false);
        final UserKey[] accountKeys = mAccountsAdapter.getSelectedAccountKeys();
        final ParcelableLocation statusLocation = attachLocation ? mRecentLocation : null;
        final boolean isPossiblySensitive = hasMedia && mIsPossiblySensitive;
        final ParcelableStatusUpdate update = new ParcelableStatusUpdate();
        @Draft.Action String action;
        if (mDraft != null) {
            action = mDraft.action_type;
        } else {
            action = getDraftAction(getIntent().getAction());
        }
        update.accounts = ParcelableAccountUtils.getAccounts(this, accountKeys);
        update.text = text;
        update.location = statusLocation;
        update.media = getMedia();
        update.in_reply_to_status = mInReplyToStatus;
        update.is_possibly_sensitive = isPossiblySensitive;
        BackgroundOperationService.updateStatusesAsync(this, action, update);
        if (mPreferences.getBoolean(KEY_NO_CLOSE_AFTER_TWEET_SENT, false) && mInReplyToStatus == null) {
            mIsPossiblySensitive = false;
            mShouldSaveAccounts = true;
            mInReplyToStatus = null;
            mMentionUser = null;
            mDraft = null;
            mOriginalText = null;
            mEditText.setText(null);
            clearMedia();
            final Intent intent = new Intent(INTENT_ACTION_COMPOSE);
            setIntent(intent);
            handleIntent(intent);
            setLabel(intent);
            setMenu();
            updateTextCount();
        } else {
            setResult(Activity.RESULT_OK);
            finish();
        }
    }

    private void updateTextCount() {
        if (mSendTextCountView == null || mEditText == null) return;
        final String textOrig = ParseUtils.parseString(mEditText.getText());
        final String text = hasMedia() && textOrig != null ? mImageUploaderUsed ? Utils.getImageUploadStatus(this,
                new String[]{FAKE_IMAGE_LINK}, textOrig) : textOrig + " " + FAKE_IMAGE_LINK : textOrig;
        final int validatedCount = text != null ? mValidator.getTweetLength(text) : 0;
        mSendTextCountView.setTextCount(validatedCount);
    }

    @Override
    public int getLightToolbarMode(@Nullable Toolbar toolbar) {
        return Config.LIGHT_TOOLBAR_AUTO;
    }

    @Override
    public int getToolbarColor(@Nullable Toolbar toolbar) {
        return ATEUtil.resolveColor(this, android.R.attr.panelColorBackground);
    }

    static class ComposeLocationListener implements LocationListener {

        final WeakReference<ComposeActivity> mActivityRef;

        ComposeLocationListener(ComposeActivity activity) {
            mActivityRef = new WeakReference<>(activity);
        }

        @Override
        public void onLocationChanged(final Location location) {
            final ComposeActivity activity = mActivityRef.get();
            if (activity == null) return;
            activity.setRecentLocation(ParcelableLocationUtils.fromLocation(location));
        }

        @Override
        public void onStatusChanged(final String provider, final int status, final Bundle extras) {

        }

        @Override
        public void onProviderEnabled(final String provider) {
        }

        @Override
        public void onProviderDisabled(final String provider) {
        }

    }

    static class SetProgressVisibleRunnable implements Runnable {

        final WeakReference<ComposeActivity> activityRef;
        final boolean visible;

        SetProgressVisibleRunnable(ComposeActivity activity, boolean visible) {
            this.activityRef = new WeakReference<>(activity);
            this.visible = visible;
        }

        @Override
        public void run() {
            final ComposeActivity activity = activityRef.get();
            if (activity == null) return;
            final FragmentManager fm = activity.getSupportFragmentManager();
            final Fragment f = fm.findFragmentByTag(DISCARD_STATUS_DIALOG_FRAGMENT_TAG);
            if (!visible && f instanceof DialogFragment) {
                ((DialogFragment) f).dismiss();
            } else if (visible) {
                SupportProgressDialogFragment df = new SupportProgressDialogFragment();
                df.show(fm, DISCARD_STATUS_DIALOG_FRAGMENT_TAG);
                df.setCancelable(false);
            }
        }
    }

    static class AccountIconViewHolder extends ViewHolder implements OnClickListener {

        final AccountIconsAdapter adapter;
        final ShapedImageView iconView;
        final TextView nameView;

        public AccountIconViewHolder(AccountIconsAdapter adapter, View itemView) {
            super(itemView);
            this.adapter = adapter;
            iconView = (ShapedImageView) itemView.findViewById(android.R.id.icon);
            nameView = (TextView) itemView.findViewById(android.R.id.text1);
            itemView.setOnClickListener(this);
        }

        public void showAccount(AccountIconsAdapter adapter, ParcelableAccount account, boolean isSelected) {
            itemView.setAlpha(isSelected ? 1 : 0.33f);
            ((CheckableLinearLayout) itemView).setChecked(isSelected);
            final MediaLoaderWrapper loader = adapter.getImageLoader();
            if (ObjectUtils.notEqual(account, iconView.getTag()) || iconView.getDrawable() == null) {
                iconView.setTag(account);
                loader.displayProfileImage(iconView, account);
            }
            iconView.setBorderColor(account.color);
            iconView.setTag(account.profile_image_url);
            nameView.setText(adapter.isNameFirst() ? account.name : ("@" + account.screen_name));
        }

        @Override
        public void onClick(View v) {
            ((CheckableLinearLayout) itemView).toggle();
            adapter.toggleSelection(getAdapterPosition());
        }


    }

    static class AccountIconsAdapter extends BaseRecyclerViewAdapter<AccountIconViewHolder> {

        private final ComposeActivity mActivity;
        private final LayoutInflater mInflater;
        private final Map<UserKey, Boolean> mSelection;
        private final boolean mNameFirst;

        private ParcelableCredentials[] mAccounts;

        public AccountIconsAdapter(ComposeActivity activity) {
            super(activity);
            setHasStableIds(true);
            mActivity = activity;
            mInflater = activity.getLayoutInflater();
            mSelection = new HashMap<>();
            mNameFirst = mPreferences.getBoolean(KEY_NAME_FIRST);
        }

        public MediaLoaderWrapper getImageLoader() {
            return mMediaLoader;
        }

        @Override
        public long getItemId(int position) {
            return mAccounts[position].hashCode();
        }

        @NonNull
        public UserKey[] getSelectedAccountKeys() {
            if (mAccounts == null) return new UserKey[0];
            final UserKey[] temp = new UserKey[mAccounts.length];
            int selectedCount = 0;
            for (ParcelableAccount account : mAccounts) {
                if (Boolean.TRUE.equals(mSelection.get(account.account_key))) {
                    temp[selectedCount++] = account.account_key;
                }
            }
            final UserKey[] result = new UserKey[selectedCount];
            System.arraycopy(temp, 0, result, 0, result.length);
            return result;
        }

        public void setSelectedAccountIds(UserKey... accountKeys) {
            mSelection.clear();
            if (accountKeys != null) {
                for (UserKey accountKey : accountKeys) {
                    mSelection.put(accountKey, true);
                }
            }
            notifyDataSetChanged();
        }

        @NonNull
        public ParcelableCredentials[] getSelectedAccounts() {
            if (mAccounts == null) return new ParcelableCredentials[0];
            final ParcelableCredentials[] temp = new ParcelableCredentials[mAccounts.length];
            int selectedCount = 0;
            for (ParcelableCredentials account : mAccounts) {
                if (Boolean.TRUE.equals(mSelection.get(account.account_key))) {
                    temp[selectedCount++] = account;
                }
            }
            final ParcelableCredentials[] result = new ParcelableCredentials[selectedCount];
            System.arraycopy(temp, 0, result, 0, result.length);
            return result;
        }

        public boolean isSelectionEmpty() {
            return getSelectedAccountKeys().length == 0;
        }

        @Override
        public AccountIconViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            final View view = mInflater.inflate(R.layout.adapter_item_compose_account, parent, false);
            return new AccountIconViewHolder(this, view);
        }

        @Override
        public void onBindViewHolder(AccountIconViewHolder holder, int position) {
            final ParcelableAccount account = mAccounts[position];
            final boolean isSelected = Boolean.TRUE.equals(mSelection.get(account.account_key));
            holder.showAccount(this, account, isSelected);
        }

        @Override
        public int getItemCount() {
            return mAccounts != null ? mAccounts.length : 0;
        }

        public void setAccounts(ParcelableCredentials[] accounts) {
            mAccounts = accounts;
            notifyDataSetChanged();
        }

        private void toggleSelection(int position) {
            if (mAccounts == null) return;
            final ParcelableCredentials account = mAccounts[position];
            mSelection.put(account.account_key, !Boolean.TRUE.equals(mSelection.get(account.account_key)));
            mActivity.notifyAccountSelectionChanged();
            notifyDataSetChanged();
        }

        public boolean isNameFirst() {
            return mNameFirst;
        }
    }

    static class AddBitmapTask extends AddMediaTask {

        final Bitmap mBitmap;

        AddBitmapTask(ComposeActivity activity, Bitmap bitmap, Uri dst, int media_type) throws IOException {
            super(activity, Uri.fromFile(File.createTempFile("tmp_bitmap", null)), dst, media_type,
                    true);
            mBitmap = bitmap;
        }

        @Override
        protected Boolean doInBackground(Object... params) {
            if (mBitmap == null || mBitmap.isRecycled()) return false;
            FileOutputStream os = null;
            try {
                os = new FileOutputStream(getSrc().getPath());
                mBitmap.compress(Bitmap.CompressFormat.PNG, 0, os);
                mBitmap.recycle();
            } catch (IOException e) {
                return false;
            } finally {
                IoUtils.closeSilently(os);
            }
            return super.doInBackground(params);
        }

    }

    static class AddMediaTask extends AsyncTask<Object, Object, Boolean> {

        final WeakReference<ComposeActivity> mActivityRef;
        final int mMediaType;
        final Uri src, dst;
        final boolean mDeleteSrc;

        AddMediaTask(final ComposeActivity activity, final Uri src, final Uri dst, final int mediaType,
                     final boolean deleteSrc) {
            this.mActivityRef = new WeakReference<>(activity);
            this.src = src;
            this.dst = dst;
            this.mMediaType = mediaType;
            this.mDeleteSrc = deleteSrc;
        }

        @Override
        protected Boolean doInBackground(final Object... params) {
            final ComposeActivity activity = this.mActivityRef.get();
            if (activity == null) return false;
            InputStream is = null;
            OutputStream os = null;
            try {
                final ContentResolver resolver = activity.getContentResolver();
                is = resolver.openInputStream(src);
                os = resolver.openOutputStream(dst);
                if (is == null || os == null) throw new FileNotFoundException();
                RestFuUtils.copyStream(is, os);
                if (ContentResolver.SCHEME_FILE.equals(src.getScheme()) && mDeleteSrc) {
                    final File file = new File(src.getPath());
                    if (!file.delete()) {
                        Log.d(LOGTAG, String.format("Unable to delete %s", file));
                    }
                }
            } catch (final IOException e) {
                if (BuildConfig.DEBUG) {
                    Log.w(LOGTAG, e);
                }
                return false;
            } finally {
                Utils.closeSilently(os);
                Utils.closeSilently(is);
            }
            return true;
        }

        Uri getSrc() {
            return src;
        }

        @Override
        protected void onPostExecute(final Boolean result) {
            final ComposeActivity activity = mActivityRef.get();
            if (activity == null) return;
            activity.setProgressVisible(false);
            activity.addMedia(new ParcelableMediaUpdate(dst.toString(), mMediaType));
            activity.setMenu();
            activity.updateTextCount();
            if (!result) {
                Toast.makeText(activity, R.string.error_occurred, Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected void onPreExecute() {
            final ComposeActivity activity = mActivityRef.get();
            if (activity == null) return;
            activity.setProgressVisible(true);
        }
    }

    static class DeleteMediaTask extends AsyncTask<Object, Object, Boolean> {

        final WeakReference<ComposeActivity> mActivity;
        final ParcelableMediaUpdate[] mMedia;

        DeleteMediaTask(final ComposeActivity activity, final ParcelableMediaUpdate... media) {
            this.mActivity = new WeakReference<>(activity);
            this.mMedia = media;
        }

        @Override
        protected Boolean doInBackground(final Object... params) {
            if (mMedia == null) return false;
            try {
                for (final ParcelableMediaUpdate media : mMedia) {
                    final Uri uri = Uri.parse(media.uri);
                    if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
                        final File file = new File(uri.getPath());
                        if (!file.delete()) {
                            Log.d(LOGTAG, String.format("Unable to delete %s", file));
                        }
                    }
                }
            } catch (final Exception e) {
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean result) {
            final ComposeActivity activity = mActivity.get();
            if (activity == null) return;
            activity.setProgressVisible(false);
            activity.removeAllMedia(Arrays.asList(mMedia));
            activity.setMenu();
            if (!result) {
                Toast.makeText(activity, R.string.error_occurred, Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected void onPreExecute() {
            final ComposeActivity activity = mActivity.get();
            if (activity == null) return;
            activity.setProgressVisible(true);
        }
    }

    static class DiscardTweetTask extends AsyncTask<Object, Object, Object> {

        final WeakReference<ComposeActivity> activityRef;
        private final List<ParcelableMediaUpdate> media;

        DiscardTweetTask(final ComposeActivity activity) {
            this.activityRef = new WeakReference<>(activity);
            this.media = activity.getMediaList();
        }

        @Override
        protected Object doInBackground(final Object... params) {
            for (final ParcelableMediaUpdate item : media) {
                final Uri uri = Uri.parse(item.uri);
                if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
                    final File file = new File(uri.getPath());
                    if (!file.delete()) {
                        Log.d(LOGTAG, String.format("Unable to delete %s", file));
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(final Object result) {
            final ComposeActivity activity = activityRef.get();
            if (activity == null) return;
            activity.setProgressVisible(false);
            activity.finish();
        }

        @Override
        protected void onPreExecute() {
            final ComposeActivity activity = activityRef.get();
            if (activity == null) return;
            activity.setProgressVisible(true);
        }
    }

    static class MediaPreviewAdapter extends ArrayRecyclerAdapter<ParcelableMediaUpdate, MediaPreviewViewHolder>
            implements SimpleItemTouchHelperCallback.ItemTouchHelperAdapter {

        final LayoutInflater mInflater;

        final SimpleItemTouchHelperCallback.OnStartDragListener mDragStartListener;

        public MediaPreviewAdapter(final ComposeActivity activity, SimpleItemTouchHelperCallback.OnStartDragListener listener) {
            super(activity);
            setHasStableIds(true);
            mInflater = LayoutInflater.from(activity);
            mDragStartListener = listener;
        }

        public void onStartDrag(ViewHolder viewHolder) {
            mDragStartListener.onStartDrag(viewHolder);
        }

        public List<ParcelableMediaUpdate> getAsList() {
            return Collections.unmodifiableList(mData);
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).hashCode();
        }

        @Override
        public void onBindViewHolder(MediaPreviewViewHolder holder, int position, ParcelableMediaUpdate item) {
            final ParcelableMediaUpdate media = getItem(position);
            holder.displayMedia(this, media);
        }

        @Override
        public MediaPreviewViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            final View view = mInflater.inflate(R.layout.grid_item_media_editor, parent, false);
            return new MediaPreviewViewHolder(view);
        }

        @Override
        public void onViewAttachedToWindow(MediaPreviewViewHolder holder) {
            super.onViewAttachedToWindow(holder);
            holder.setAdapter(this);
        }

        @Override
        public void onViewDetachedFromWindow(MediaPreviewViewHolder holder) {
            holder.setAdapter(null);
            super.onViewDetachedFromWindow(holder);
        }

        @Override
        public boolean remove(int position) {
            boolean result = super.remove(position);
            if (result) {
                ((ComposeActivity) getContext()).updateAttachedMediaView();
            }
            return result;
        }

        @Override
        public void onItemDismiss(int position) {
            // No-op
        }

        @Override
        public boolean onItemMove(int fromPosition, int toPosition) {
            Collections.swap(mData, fromPosition, toPosition);
            notifyItemMoved(fromPosition, toPosition);
            return true;
        }
    }

    static class MediaPreviewViewHolder extends ViewHolder implements OnLongClickListener, OnClickListener {

        final ImageView image;
        final View remove;
        MediaPreviewAdapter adapter;

        public MediaPreviewViewHolder(View itemView) {
            super(itemView);
            itemView.setOnLongClickListener(this);
            image = (ImageView) itemView.findViewById(R.id.image);
            remove = itemView.findViewById(R.id.remove);
            remove.setOnClickListener(this);
        }

        public void displayMedia(MediaPreviewAdapter adapter, ParcelableMediaUpdate media) {
            adapter.getMediaLoader().displayPreviewImage(media.uri, image);
        }

        @Override
        public boolean onLongClick(View v) {
            if (adapter == null) return false;
            adapter.onStartDrag(this);
            return true;
        }

        public void setAdapter(final MediaPreviewAdapter adapter) {
            this.adapter = adapter;
        }

        @Override
        public void onClick(View v) {
            if (adapter == null) return;
            switch (v.getId()) {
                case R.id.remove: {
                    adapter.remove(getLayoutPosition());
                }
            }
        }
    }

    public static class RetweetProtectedStatusWarnFragment extends BaseSupportDialogFragment implements
            DialogInterface.OnClickListener {

        @Override
        public void onClick(final DialogInterface dialog, final int which) {
            final Activity activity = getActivity();
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE: {
                    if (activity instanceof ComposeActivity) {
                        ((ComposeActivity) activity).updateStatus();
                    }
                    break;
                }
            }

        }

        @NonNull
        @Override
        public Dialog onCreateDialog(final Bundle savedInstanceState) {
            final Context wrapped = ThemeUtils.getDialogThemedContext(getActivity());
            final AlertDialog.Builder builder = new AlertDialog.Builder(wrapped);
            builder.setMessage(R.string.quote_protected_status_warning_message);
            builder.setPositiveButton(R.string.send_anyway, this);
            builder.setNegativeButton(android.R.string.cancel, null);
            return builder.create();
        }
    }

    static class SpacingItemDecoration extends ItemDecoration {

        final int mSpacingSmall, mSpacingNormal;

        SpacingItemDecoration(Context context) {
            final Resources resources = context.getResources();
            mSpacingSmall = resources.getDimensionPixelSize(R.dimen.element_spacing_small);
            mSpacingNormal = resources.getDimensionPixelSize(R.dimen.element_spacing_normal);
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, State state) {
            final int pos = parent.getChildAdapterPosition(view);
            if (pos == 0) {
                outRect.set(0, mSpacingNormal, 0, 0);
            } else if (pos == parent.getAdapter().getItemCount() - 1) {
                outRect.set(0, 0, 0, mSpacingNormal);
            } else {
                outRect.set(0, 0, 0, 0);
            }
        }
    }

    public static class AttachedMediaItemTouchHelperCallback extends SimpleItemTouchHelperCallback {
        public static final float ALPHA_FULL = 1.0f;

        public AttachedMediaItemTouchHelperCallback(ItemTouchHelperAdapter adapter) {
            super(adapter);
        }

        @Override
        public boolean isLongPressDragEnabled() {
            return true;
        }

        @Override
        public boolean isItemViewSwipeEnabled() {
            return false;
        }

        @Override
        public int getMovementFlags(RecyclerView recyclerView, ViewHolder viewHolder) {
            // Set movement flags based on the layout manager
            final int dragFlags = ItemTouchHelper.START | ItemTouchHelper.END;
            final int swipeFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
            return makeMovementFlags(dragFlags, swipeFlags);
        }

        @Override
        public void onChildDraw(Canvas c, RecyclerView recyclerView, ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
            if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                // Fade out the view as it is swiped out of the parent's bounds
                final float alpha = ALPHA_FULL - Math.abs(dY) / (float) viewHolder.itemView.getHeight();
                viewHolder.itemView.setAlpha(alpha);
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            } else {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        }

        @Override
        public float getSwipeThreshold(ViewHolder viewHolder) {
            return 0.75f;
        }

        @Override
        public void clearView(RecyclerView recyclerView, ViewHolder viewHolder) {
            super.clearView(recyclerView, viewHolder);
            viewHolder.itemView.setAlpha(ALPHA_FULL);
        }
    }

    private static class PreviewGridItemDecoration extends ItemDecoration {
        private final int previewGridSpacing;

        public PreviewGridItemDecoration(int previewGridSpacing) {
            this.previewGridSpacing = previewGridSpacing;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, State state) {
            outRect.left = outRect.right = previewGridSpacing;
        }
    }

    private static class PreviewGridOnStartDragListener implements SimpleItemTouchHelperCallback.OnStartDragListener {
        @NonNull
        private final ComposeActivity activity;

        public PreviewGridOnStartDragListener(@NonNull ComposeActivity activity) {
            this.activity = activity;
        }

        @Override
        public void onStartDrag(ViewHolder viewHolder) {
            final ItemTouchHelper helper = activity.mItemTouchHelper;
            if (helper == null) return;
            helper.startDrag(viewHolder);
        }
    }

    private static class ComposeEnterListener implements EnterListener {
        private final ComposeActivity activity;

        public ComposeEnterListener(ComposeActivity activity) {
            this.activity = activity;
        }

        @Override
        public boolean shouldCallListener() {
            return activity != null && activity.mKeyMetaState == 0;
        }

        @Override
        public boolean onHitEnter() {
            if (activity == null) return false;
            activity.confirmAndUpdateStatus();
            return true;
        }
    }
}