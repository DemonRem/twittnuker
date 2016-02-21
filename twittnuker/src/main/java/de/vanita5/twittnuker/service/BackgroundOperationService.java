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

package de.vanita5.twittnuker.service;

import android.app.IntentService;
import android.app.Notification;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Parcelable;
import android.provider.BaseColumns;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.nostra13.universalimageloader.utils.IoUtils;
import com.twitter.Extractor;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.mariotaku.restfu.http.ContentType;
import org.mariotaku.restfu.http.mime.FileBody;
import org.mariotaku.sqliteqb.library.Expression;

import de.vanita5.twittnuker.BuildConfig;
import de.vanita5.twittnuker.Constants;
import de.vanita5.twittnuker.R;
import de.vanita5.twittnuker.api.twitter.Twitter;
import de.vanita5.twittnuker.api.twitter.TwitterException;
import de.vanita5.twittnuker.api.twitter.TwitterUpload;
import de.vanita5.twittnuker.api.twitter.model.ErrorInfo;
import de.vanita5.twittnuker.api.twitter.model.MediaUploadResponse;
import de.vanita5.twittnuker.api.twitter.model.Status;
import de.vanita5.twittnuker.api.twitter.model.StatusUpdate;
import de.vanita5.twittnuker.api.twitter.model.UserMentionEntity;
import de.vanita5.twittnuker.app.TwittnukerApplication;
import de.vanita5.twittnuker.model.Draft;
import de.vanita5.twittnuker.model.DraftCursorIndices;
import de.vanita5.twittnuker.model.DraftValuesCreator;
import de.vanita5.twittnuker.model.MediaUploadResult;
import de.vanita5.twittnuker.model.ParcelableAccount;
import de.vanita5.twittnuker.model.ParcelableDirectMessage;
import de.vanita5.twittnuker.model.ParcelableLocation;
import de.vanita5.twittnuker.model.ParcelableMediaUpdate;
import de.vanita5.twittnuker.model.ParcelableStatus;
import de.vanita5.twittnuker.model.ParcelableStatusUpdate;
import de.vanita5.twittnuker.model.SingleResponse;
import de.vanita5.twittnuker.model.StatusShortenResult;
import de.vanita5.twittnuker.model.UploaderMediaItem;
import de.vanita5.twittnuker.model.draft.SendDirectMessageActionExtra;
import de.vanita5.twittnuker.model.draft.UpdateStatusActionExtra;
import de.vanita5.twittnuker.model.util.ParcelableAccountUtils;
import de.vanita5.twittnuker.model.util.ParcelableDirectMessageUtils;
import de.vanita5.twittnuker.model.util.ParcelableStatusUpdateUtils;
import de.vanita5.twittnuker.model.util.ParcelableStatusUtils;
import de.vanita5.twittnuker.preference.ServicePickerPreference;
import de.vanita5.twittnuker.provider.TwidereDataStore.CachedHashtags;
import de.vanita5.twittnuker.provider.TwidereDataStore.DirectMessages;
import de.vanita5.twittnuker.provider.TwidereDataStore.Drafts;
import de.vanita5.twittnuker.util.AsyncTwitterWrapper;
import de.vanita5.twittnuker.util.BitmapUtils;
import de.vanita5.twittnuker.util.ContentValuesCreator;
import de.vanita5.twittnuker.util.DataStoreUtils;
import de.vanita5.twittnuker.util.MediaUploaderInterface;
import de.vanita5.twittnuker.util.NotificationManagerWrapper;
import de.vanita5.twittnuker.util.SharedPreferencesWrapper;
import de.vanita5.twittnuker.util.StatusShortenerInterface;
import de.vanita5.twittnuker.util.TwidereListUtils;
import de.vanita5.twittnuker.util.TwidereValidator;
import de.vanita5.twittnuker.util.TwitterAPIFactory;
import de.vanita5.twittnuker.util.Utils;
import de.vanita5.twittnuker.util.dagger.GeneralComponentHelper;
import de.vanita5.twittnuker.util.io.ContentLengthInputStream;
import de.vanita5.twittnuker.util.io.ContentLengthInputStream.ReadListener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import static android.text.TextUtils.isEmpty;
import static de.vanita5.twittnuker.util.ContentValuesCreator.createMessageDraft;
import static de.vanita5.twittnuker.util.Utils.getImagePathFromUri;
import static de.vanita5.twittnuker.util.Utils.getImageUploadStatus;

public class BackgroundOperationService extends IntentService implements Constants {


    private Handler mHandler;
    @Inject
    SharedPreferencesWrapper mPreferences;
    @Inject
    AsyncTwitterWrapper mTwitter;
    @Inject
    NotificationManagerWrapper mNotificationManager;
    @Inject
    TwidereValidator mValidator;
    @Inject
    Extractor mExtractor;


    public BackgroundOperationService() {
        super("background_operation");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        GeneralComponentHelper.build(this).inject(this);
        mHandler = new Handler();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    public void showErrorMessage(final CharSequence message, final boolean longMessage) {
        mHandler.post(new Runnable() {

            @Override
            public void run() {
                Utils.showErrorMessage(BackgroundOperationService.this, message, longMessage);
            }
        });
    }

    public void showErrorMessage(final int actionRes, final Exception e, final boolean longMessage) {
        mHandler.post(new Runnable() {

            @Override
            public void run() {
                Utils.showErrorMessage(BackgroundOperationService.this, actionRes, e, longMessage);
            }
        });
    }

    public void showErrorMessage(final int actionRes, final String message, final boolean longMessage) {
        mHandler.post(new Runnable() {

            @Override
            public void run() {
                Utils.showErrorMessage(BackgroundOperationService.this, actionRes, message, longMessage);
            }
        });
    }

    public void showOkMessage(final int messageRes, final boolean longMessage) {
        showToast(getString(messageRes), longMessage);
    }

    private void showToast(final CharSequence message, final boolean longMessage) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(BackgroundOperationService.this, message, longMessage ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onHandleIntent(final Intent intent) {
        if (intent == null) return;
        final String action = intent.getAction();
        if (action == null) return;
        switch (action) {
            case INTENT_ACTION_UPDATE_STATUS:
                handleUpdateStatusIntent(intent);
                break;
            case INTENT_ACTION_SEND_DIRECT_MESSAGE:
                handleSendDirectMessageIntent(intent);
                break;
            case INTENT_ACTION_DISCARD_DRAFT:
                handleDiscardDraftIntent(intent);
                break;
            case INTENT_ACTION_SEND_DRAFT: {
                handleSendDraftIntent(intent);
            }
        }
    }

    private void handleSendDraftIntent(Intent intent) {
        final Uri uri = intent.getData();
        if (uri == null) return;
        mNotificationManager.cancel(uri.toString(), NOTIFICATION_ID_DRAFTS);
        final long def = -1;
        final long draftId = NumberUtils.toLong(uri.getLastPathSegment(), def);
        if (draftId == -1) return;
        final Expression where = Expression.equals(Drafts._ID, draftId);
        final ContentResolver cr = getContentResolver();
        final Cursor c = cr.query(Drafts.CONTENT_URI, Drafts.COLUMNS, where.getSQL(), null, null);
        if (c == null) return;
        final DraftCursorIndices i = new DraftCursorIndices(c);
        final Draft item;
        try {
            if (!c.moveToFirst()) return;
            item = i.newObject(c);
        } finally {
            c.close();
        }
        cr.delete(Drafts.CONTENT_URI, where.getSQL(), null);
        if (TextUtils.isEmpty(item.action_type)) {
            item.action_type = Draft.Action.UPDATE_STATUS;
        }
        switch (item.action_type) {
            case Draft.Action.UPDATE_STATUS_COMPAT_1:
            case Draft.Action.UPDATE_STATUS_COMPAT_2:
            case Draft.Action.UPDATE_STATUS:
            case Draft.Action.REPLY:
            case Draft.Action.QUOTE: {
                updateStatuses(ParcelableStatusUpdateUtils.fromDraftItem(this, item));
                break;
            }
            case Draft.Action.SEND_DIRECT_MESSAGE_COMPAT:
            case Draft.Action.SEND_DIRECT_MESSAGE: {
                long recipientId = -1;
                if (item.action_extras instanceof SendDirectMessageActionExtra) {
                    recipientId = ((SendDirectMessageActionExtra) item.action_extras).getRecipientId();
                }
                if (ArrayUtils.isEmpty(item.account_ids) || recipientId <= 0) {
                    return;
                }
                final long accountId = item.account_ids[0];
                final String imageUri = item.media != null && item.media.length > 0 ? item.media[0].uri : null;
                sendMessage(accountId, recipientId, item.text, imageUri);
                break;
            }
        }
    }

    private void handleDiscardDraftIntent(Intent intent) {
        final Uri data = intent.getData();
        if (data == null) return;
        mNotificationManager.cancel(data.toString(), NOTIFICATION_ID_DRAFTS);
        final ContentResolver cr = getContentResolver();
        final long def = -1;
        final long id = NumberUtils.toLong(data.getLastPathSegment(), def);
        final Expression where = Expression.equals(Drafts._ID, id);
        cr.delete(Drafts.CONTENT_URI, where.getSQL(), null);
    }

    private void handleSendDirectMessageIntent(final Intent intent) {
        final long accountId = intent.getLongExtra(EXTRA_ACCOUNT_ID, -1);
        final long recipientId = intent.getLongExtra(EXTRA_RECIPIENT_ID, -1);
        final String imageUri = intent.getStringExtra(EXTRA_IMAGE_URI);
        final String text = intent.getStringExtra(EXTRA_TEXT);
        sendMessage(accountId, recipientId, text, imageUri);
    }

    private void sendMessage(long accountId, long recipientId, String text, String imageUri) {
        if (accountId <= 0 || recipientId <= 0 || isEmpty(text)) return;
        final String title = getString(R.string.sending_direct_message);
        final Builder builder = new Builder(this);
        builder.setSmallIcon(R.drawable.ic_stat_send);
        builder.setProgress(100, 0, true);
        builder.setTicker(title);
        builder.setContentTitle(title);
        builder.setContentText(text);
        builder.setCategory(NotificationCompat.CATEGORY_PROGRESS);
        builder.setOngoing(true);
        final Notification notification = builder.build();
        startForeground(NOTIFICATION_ID_SEND_DIRECT_MESSAGE, notification);
        final SingleResponse<ParcelableDirectMessage> result = sendDirectMessage(builder, accountId,
                recipientId, text, imageUri);

        final ContentResolver resolver = getContentResolver();
        if (result.getData() != null && result.getData().id > 0) {
            final ContentValues values = ContentValuesCreator.createDirectMessage(result.getData());
            final String delete_where = DirectMessages.ACCOUNT_ID + " = " + accountId + " AND "
                    + DirectMessages.MESSAGE_ID + " = " + result.getData().id;
            resolver.delete(DirectMessages.Outbox.CONTENT_URI, delete_where, null);
            resolver.insert(DirectMessages.Outbox.CONTENT_URI, values);
            showOkMessage(R.string.direct_message_sent, false);
        } else {
            final ContentValues values = createMessageDraft(accountId, recipientId, text, imageUri);
            resolver.insert(Drafts.CONTENT_URI, values);
            showErrorMessage(R.string.action_sending_direct_message, result.getException(), true);
        }
        stopForeground(false);
        mNotificationManager.cancel(NOTIFICATION_ID_SEND_DIRECT_MESSAGE);
    }

    private void handleUpdateStatusIntent(final Intent intent) {
        final ParcelableStatusUpdate status = intent.getParcelableExtra(EXTRA_STATUS);
        final Parcelable[] status_parcelables = intent.getParcelableArrayExtra(EXTRA_STATUSES);
        final ParcelableStatusUpdate[] statuses;
        if (status_parcelables != null) {
            statuses = new ParcelableStatusUpdate[status_parcelables.length];
            for (int i = 0, j = status_parcelables.length; i < j; i++) {
                statuses[i] = (ParcelableStatusUpdate) status_parcelables[i];
            }
        } else if (status != null) {
            statuses = new ParcelableStatusUpdate[1];
            statuses[0] = status;
        } else
            return;
        updateStatuses(statuses);
    }

    private void updateStatuses(ParcelableStatusUpdate... statuses) {
        final Builder builder = new Builder(this);
        startForeground(NOTIFICATION_ID_UPDATE_STATUS, updateUpdateStatusNotification(this, builder, 0, null));
        for (final ParcelableStatusUpdate item : statuses) {
            mNotificationManager.notify(NOTIFICATION_ID_UPDATE_STATUS,
                    updateUpdateStatusNotification(this, builder, 0, item));
            final Draft draft = new Draft();
            draft.account_ids = ParcelableAccountUtils.getAccountIds(item.accounts);
            draft.text = item.text;
            draft.location = item.location;
            draft.media = item.media;
            final UpdateStatusActionExtra extra = new UpdateStatusActionExtra();
            extra.setInReplyToStatus(item.in_reply_to_status);
            extra.setIsPossiblySensitive(item.is_possibly_sensitive);
            draft.action_extras = extra;
            final ContentResolver resolver = getContentResolver();
            final Uri draftUri = resolver.insert(Drafts.CONTENT_URI, DraftValuesCreator.create(draft));
            final long def = -1;
            final long draftId = draftUri != null ? NumberUtils.toLong(draftUri.getLastPathSegment(), def) : -1;
            mTwitter.addSendingDraftId(draftId);
            final List<SingleResponse<ParcelableStatus>> result = updateStatus(builder, item);
            boolean failed = false;
            Exception exception = null;
            final Expression where = Expression.equals(Drafts._ID, draftId);
            final List<Long> failedAccountIds = TwidereListUtils.fromArray(DataStoreUtils.getAccountIds(item.accounts));

            for (final SingleResponse<ParcelableStatus> response : result) {
                final ParcelableStatus data = response.getData();
                if (data == null) {
                    failed = true;
                    if (exception == null) {
                        exception = response.getException();
                    }
                } else if (data.account_id > 0) {
                    failedAccountIds.remove(data.account_id);
                }
            }

            if (result.isEmpty()) {
                showErrorMessage(R.string.action_updating_status, getString(R.string.no_account_selected), false);
            } else if (failed) {
                // If the status is a duplicate, there's no need to save it to
                // drafts.
                if (exception instanceof TwitterException
                        && ((TwitterException) exception).getErrorCode() == ErrorInfo.STATUS_IS_DUPLICATE) {
                    showErrorMessage(getString(R.string.status_is_duplicate), false);
                } else {
                    final ContentValues accountIdsValues = new ContentValues();
                    accountIdsValues.put(Drafts.ACCOUNT_IDS, TwidereListUtils.toString(failedAccountIds, ',', false));
                    resolver.update(Drafts.CONTENT_URI, accountIdsValues, where.getSQL(), null);
                    showErrorMessage(R.string.action_updating_status, exception, true);
                    final ContentValues notifValues = new ContentValues();
                    notifValues.put(BaseColumns._ID, draftId);
                    resolver.insert(Drafts.CONTENT_URI_NOTIFICATIONS, notifValues);
                }
            } else {
                showOkMessage(R.string.status_updated, false);
                resolver.delete(Drafts.CONTENT_URI, where.getSQL(), null);
                if (item.media != null) {
                    for (final ParcelableMediaUpdate media : item.media) {
                        final String path = getImagePathFromUri(this, Uri.parse(media.uri));
                        if (path != null) {
                            if (!new File(path).delete()) {
                                Log.d(LOGTAG, String.format("unable to delete %s", path));
                            }
                        }
                    }
                }
            }
            mTwitter.removeSendingDraftId(draftId);
            if (mPreferences.getBoolean(KEY_REFRESH_AFTER_TWEET, false)) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mTwitter.refreshAll();
                    }
                });
            }
        }
        stopForeground(false);
        mNotificationManager.cancel(NOTIFICATION_ID_UPDATE_STATUS);
    }


    private SingleResponse<ParcelableDirectMessage> sendDirectMessage(final NotificationCompat.Builder builder,
                                                                      final long accountId, final long recipientId,
                                                                      final String text, final String imageUri) {
        final Twitter twitter = TwitterAPIFactory.getTwitterInstance(this, accountId, true, true);
        final TwitterUpload twitterUpload = TwitterAPIFactory.getTwitterInstance(this, accountId, true, true, TwitterUpload.class);
        if (twitter == null || twitterUpload == null) return SingleResponse.getInstance();
        try {
            final ParcelableDirectMessage directMessage;
            if (imageUri != null) {
                final String path = getImagePathFromUri(this, Uri.parse(imageUri));
                if (path == null) throw new FileNotFoundException();
                final BitmapFactory.Options o = new BitmapFactory.Options();
                o.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(path, o);
                final File file = new File(path);
                BitmapUtils.downscaleImageIfNeeded(file, 100);
                final ContentLengthInputStream is = new ContentLengthInputStream(file);
                is.setReadListener(new MessageMediaUploadListener(this, mNotificationManager, builder, text));
//                final MediaUploadResponse uploadResp = twitter.uploadMedia(file.getName(), is, o.outMimeType);
                final MediaUploadResponse uploadResp = twitterUpload.uploadMedia(file);
                directMessage = ParcelableDirectMessageUtils.fromDirectMessage(twitter.sendDirectMessage(recipientId, text,
                        uploadResp.getId()), accountId, true);
                if (!file.delete()) {
                    Log.d(LOGTAG, String.format("unable to delete %s", path));
                }
            } else {
                directMessage = ParcelableDirectMessageUtils.fromDirectMessage(twitter.sendDirectMessage(recipientId, text), accountId, true);
            }
            Utils.setLastSeen(this, recipientId, System.currentTimeMillis());


            return SingleResponse.getInstance(directMessage);
        } catch (final IOException e) {
            return SingleResponse.getInstance(e);
        } catch (final TwitterException e) {
            return SingleResponse.getInstance(e);
        }
    }

    private void showToast(final int resId, final int duration) {
        mHandler.post(new ToastRunnable(this, resId, duration));
    }

    private List<SingleResponse<ParcelableStatus>> updateStatus(final Builder builder,
                                                                final ParcelableStatusUpdate statusUpdate) {
        final ArrayList<ContentValues> hashTagValues = new ArrayList<>();
        final Collection<String> hashTags = mExtractor.extractHashtags(statusUpdate.text);
        for (final String hashTag : hashTags) {
            final ContentValues values = new ContentValues();
            values.put(CachedHashtags.NAME, hashTag);
            hashTagValues.add(values);
        }
        boolean notReplyToOther = false;
        final ContentResolver resolver = getContentResolver();
        resolver.bulkInsert(CachedHashtags.CONTENT_URI,
                hashTagValues.toArray(new ContentValues[hashTagValues.size()]));

        final List<SingleResponse<ParcelableStatus>> results = new ArrayList<>();

        if (statusUpdate.accounts.length == 0) return Collections.emptyList();

        try {
            final TwittnukerApplication app = TwittnukerApplication.getInstance(this);
            final String uploaderComponent = mPreferences.getString(KEY_MEDIA_UPLOADER, null);
            final String shortenerComponent = mPreferences.getString(KEY_STATUS_SHORTENER, null);

            // Try find uploader and shortener, show error if set but not found
            MediaUploaderInterface uploader = null;
            StatusShortenerInterface shortener = null;
            if (!ServicePickerPreference.isNoneValue(uploaderComponent)) {
                uploader = MediaUploaderInterface.getInstance(app, uploaderComponent);
                if (uploader == null) {
                    throw new UploaderNotFoundException(getString(R.string.error_message_media_uploader_not_found));
                }
            }
            if (!ServicePickerPreference.isNoneValue(shortenerComponent)) {
                shortener = StatusShortenerInterface.getInstance(app, shortenerComponent);
                if (shortener == null) throw new ShortenerNotFoundException(this);
            }

            final boolean hasMedia = statusUpdate.media != null && statusUpdate.media.length > 0;

            // Uploader handles media scaling, if no uploader present, we will handle them instead.
            if (uploader == null && statusUpdate.media != null) {
                for (final ParcelableMediaUpdate media : statusUpdate.media) {
                    final String path = getImagePathFromUri(this, Uri.parse(media.uri));
                    final File file = path != null ? new File(path) : null;
                    if (file != null && file.exists()) {
                        BitmapUtils.downscaleImageIfNeeded(file, 95);
                    }
                }
            }
            try {
                if (uploader != null && hasMedia) {
                    // Wait for uploader service binding
                    uploader.waitForService();
                }
                if (shortener != null) {
                    // Wait for shortener service binding
                    shortener.waitForService();
                }
                for (final ParcelableAccount account : statusUpdate.accounts) {
                    // Get Twitter instance corresponding to account
                    final Twitter twitter = TwitterAPIFactory.getTwitterInstance(this, account.account_id,
                            true, true);
                    final TwitterUpload upload = TwitterAPIFactory.getTwitterInstance(this, account.account_id,
                            true, true, TwitterUpload.class);

                    // Shouldn't happen
                    if (twitter == null || upload == null) {
                        throw new UpdateStatusException("No account found");
                    }

                    String statusText = statusUpdate.text;

                    // Use custom uploader to upload media
                    if (uploader != null && hasMedia) {
                        final MediaUploadResult uploadResult;
                        try {
                            uploadResult = uploader.upload(statusUpdate,
                            UploaderMediaItem.getFromStatusUpdate(this, statusUpdate));
                        } catch (final Exception e) {
                            throw new UploadException(getString(R.string.error_message_media_upload_failed));
                        }
                        // Shouldn't return null, but handle that case for shitty extensions.
                        if (uploadResult == null) {
                            throw new UploadException(getString(R.string.error_message_media_upload_failed));
                        }
                        if (uploadResult.error_code != 0)
                            throw new UploadException(uploadResult.error_message);

                        // Replace status text to uploaded
                        statusText = getImageUploadStatus(uploadResult.media_uris,
                                statusText);
                    }

                    final boolean shouldShorten = mValidator.getTweetLength(statusText) > mValidator.getMaxTweetLength();
                    StatusShortenResult shortenedResult = null;
                    if (shouldShorten && shortener != null) {
                        try {
                            shortenedResult = shortener.shorten(statusUpdate, account, statusText);
                        } catch (final Exception e) {
                            throw new ShortenException(getString(R.string.error_message_tweet_shorten_failed), e);
                        }
                        // Shouldn't return null, but handle that case for shitty extensions.
                        if (shortenedResult == null)
                            throw new ShortenException(getString(R.string.error_message_tweet_shorten_failed));
                        if (shortenedResult.error_code != 0)
                            throw new ShortenException(shortenedResult.error_message);
                        if (shortenedResult.shortened == null)
                            throw new ShortenException(getString(R.string.error_message_tweet_shorten_failed));
                        statusText = shortenedResult.shortened;
                    }

                    final StatusUpdate status = new StatusUpdate(statusText);
                    if (statusUpdate.in_reply_to_status != null) {
                        status.inReplyToStatusId(statusUpdate.in_reply_to_status.id);
                    }
                    if (statusUpdate.location != null) {
                        status.location(ParcelableLocation.toGeoLocation(statusUpdate.location));
                    }
                    if (uploader == null && hasMedia) {
                        final long[] mediaIds = new long[statusUpdate.media.length];
                        ContentLengthInputStream cis = null;
                        try {
                            for (int i = 0, j = mediaIds.length; i < j; i++) {
                                final ParcelableMediaUpdate media = statusUpdate.media[i];
                                final Uri mediaUri = Uri.parse(media.uri);
                                final String mediaType = resolver.getType(mediaUri);
                                final InputStream is = resolver.openInputStream(mediaUri);
                                final long length = is.available();
                                cis = new ContentLengthInputStream(is, length);
                                cis.setReadListener(new StatusMediaUploadListener(this, mNotificationManager, builder,
                                        statusUpdate));
                                final ContentType contentType;
                                if (TextUtils.isEmpty(mediaType)) {
                                    contentType = ContentType.parse("application/octet-stream");
                                } else {
                                    contentType = ContentType.parse(mediaType);
                                }
                                final FileBody body = new FileBody(cis, "attachment", length, contentType);
                                final MediaUploadResponse uploadResp = upload.uploadMedia(body);
                                mediaIds[i] = uploadResp.getId();
                            }
                        } catch (final IOException e) {
                            if (BuildConfig.DEBUG) {
                                Log.w(LOGTAG, e);
                            }
                        } catch (final TwitterException e) {
                            if (BuildConfig.DEBUG) {
                                Log.w(LOGTAG, e);
                            }
                            final SingleResponse<ParcelableStatus> response = SingleResponse.getInstance(e);
                            results.add(response);
                            continue;
                        } finally {
                            IoUtils.closeSilently(cis);
                        }
                        status.mediaIds(mediaIds);
                    }
                    status.possiblySensitive(statusUpdate.is_possibly_sensitive);

                    try {
                        final Status resultStatus = twitter.updateStatus(status);
                        final UserMentionEntity[] entities = resultStatus.getUserMentionEntities();
                        Utils.setLastSeen(this, entities, System.currentTimeMillis());
                        if (!notReplyToOther) {
                            final long inReplyToUserId = resultStatus.getInReplyToUserId();
                            if (inReplyToUserId <= 0) {
                                notReplyToOther = true;
                            }
                        }
                        final ParcelableStatus result = ParcelableStatusUtils.fromStatus(resultStatus, account.account_id, false);
                        if (shouldShorten && shortener != null && shortenedResult != null) {
                            shortener.callback(shortenedResult, result);
                        }
                        results.add(SingleResponse.getInstance(result));
                    } catch (final TwitterException e) {
                        Log.w(LOGTAG, e);
                        final SingleResponse<ParcelableStatus> response = SingleResponse.getInstance(e);
                        results.add(response);
                    }
                }
            } finally {
                // Unbind uploader and shortener
                if (uploader != null) {
                    uploader.unbindService();
                }
                if (shortener != null) {
                    shortener.unbindService();
                }
            }
        } catch (final UpdateStatusException e) {
            Log.w(LOGTAG, e);
            final SingleResponse<ParcelableStatus> response = SingleResponse.getInstance(e);
            results.add(response);
        }
        return results;
    }

    private static Notification updateSendDirectMessageNotification(final Context context,
                                                                    final NotificationCompat.Builder builder, final int progress, final String message) {
        builder.setContentTitle(context.getString(R.string.sending_direct_message));
        if (message != null) {
            builder.setContentText(message);
        }
        builder.setSmallIcon(R.drawable.ic_stat_send);
        builder.setProgress(100, progress, progress >= 100 || progress <= 0);
        builder.setOngoing(true);
        return builder.build();
    }

    private static Notification updateUpdateStatusNotification(final Context context,
                                                               final NotificationCompat.Builder builder, final int progress, final ParcelableStatusUpdate status) {
        builder.setContentTitle(context.getString(R.string.updating_status_notification));
        if (status != null) {
            builder.setContentText(status.text);
        }
        builder.setSmallIcon(R.drawable.ic_stat_send);
        builder.setProgress(100, progress, progress >= 100 || progress <= 0);
        builder.setOngoing(true);
        return builder.build();
    }

    private static class ToastRunnable implements Runnable {
        private final Context context;
        private final int resId;
        private final int duration;

        public ToastRunnable(final Context context, final int resId, final int duration) {
            this.context = context;
            this.resId = resId;
            this.duration = duration;
        }

        @Override
        public void run() {
            Toast.makeText(context, resId, duration).show();

        }

    }

    static class MessageMediaUploadListener implements ReadListener {
        private final Context context;
        private final NotificationManagerWrapper manager;

        int percent;

        private final Builder builder;
        private final String message;

        MessageMediaUploadListener(final Context context, final NotificationManagerWrapper manager,
                                   final NotificationCompat.Builder builder, final String message) {
            this.context = context;
            this.manager = manager;
            this.builder = builder;
            this.message = message;
        }

        @Override
        public void onRead(final long length, final long position) {
            final int percent = length > 0 ? (int) (position * 100 / length) : 0;
            if (this.percent != percent) {
                manager.notify(NOTIFICATION_ID_SEND_DIRECT_MESSAGE,
                        updateSendDirectMessageNotification(context, builder, percent, message));
            }
            this.percent = percent;
        }
    }

    static class ShortenerNotFoundException extends UpdateStatusException {
        private static final long serialVersionUID = -7262474256595304566L;

        public ShortenerNotFoundException(final Context context) {
            super(context.getString(R.string.error_message_tweet_shortener_not_found));
        }
    }

    static class ShortenException extends UpdateStatusException {

        public ShortenException() {
            super();
        }

        public ShortenException(String detailMessage, Throwable throwable) {
            super(detailMessage, throwable);
        }

        public ShortenException(Throwable throwable) {
            super(throwable);
        }

        public ShortenException(final String message) {
            super(message);
        }
    }

    static class StatusMediaUploadListener implements ReadListener {
        private final Context context;
        private final NotificationManagerWrapper manager;

        int percent;

        private final Builder builder;
        private final ParcelableStatusUpdate statusUpdate;

        StatusMediaUploadListener(final Context context, final NotificationManagerWrapper manager,
                                  final NotificationCompat.Builder builder, final ParcelableStatusUpdate statusUpdate) {
            this.context = context;
            this.manager = manager;
            this.builder = builder;
            this.statusUpdate = statusUpdate;
        }

        @Override
        public void onRead(final long length, final long position) {
            final int percent = length > 0 ? (int) (position * 100 / length) : 0;
            if (this.percent != percent) {
                manager.notify(NOTIFICATION_ID_UPDATE_STATUS,
                        updateUpdateStatusNotification(context, builder, percent, statusUpdate));
            }
            this.percent = percent;
        }
    }

    static class StatusTooLongException extends UpdateStatusException {
        private static final long serialVersionUID = -6469920130856384219L;

        public StatusTooLongException(final Context context) {
            super(context.getString(R.string.error_message_status_too_long));
        }
    }

    static class UpdateStatusException extends Exception {
        public UpdateStatusException() {
            super();
        }

        public UpdateStatusException(String detailMessage, Throwable throwable) {
            super(detailMessage, throwable);
        }

        public UpdateStatusException(Throwable throwable) {
            super(throwable);
        }

        public UpdateStatusException(final String message) {
            super(message);
        }
    }

    static class UploaderNotFoundException extends UpdateStatusException {

        public UploaderNotFoundException() {
            super();
        }

        public UploaderNotFoundException(String detailMessage, Throwable throwable) {
            super(detailMessage, throwable);
        }

        public UploaderNotFoundException(Throwable throwable) {
            super(throwable);
        }

        public UploaderNotFoundException(String message) {
            super(message);
        }
    }

    static class UploadException extends UpdateStatusException {

        public UploadException() {
            super();
        }

        public UploadException(String detailMessage, Throwable throwable) {
            super(detailMessage, throwable);
        }

        public UploadException(Throwable throwable) {
            super(throwable);
        }

        public UploadException(String message) {
            super(message);
        }
    }
}