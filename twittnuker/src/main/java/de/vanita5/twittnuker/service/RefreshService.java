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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.apache.commons.lang3.math.NumberUtils;

import de.vanita5.twittnuker.BuildConfig;
import de.vanita5.twittnuker.Constants;
import de.vanita5.twittnuker.model.AccountKey;
import de.vanita5.twittnuker.model.AccountPreferences;
import de.vanita5.twittnuker.model.SimpleRefreshTaskParam;
import de.vanita5.twittnuker.provider.TwidereDataStore.Activities;
import de.vanita5.twittnuker.provider.TwidereDataStore.DirectMessages;
import de.vanita5.twittnuker.provider.TwidereDataStore.Statuses;
import de.vanita5.twittnuker.util.AsyncTwitterWrapper;
import de.vanita5.twittnuker.util.DataStoreUtils;
import de.vanita5.twittnuker.util.SharedPreferencesWrapper;
import de.vanita5.twittnuker.util.dagger.GeneralComponentHelper;

import java.util.Arrays;

import javax.inject.Inject;

import static de.vanita5.twittnuker.util.Utils.getDefaultAccountKey;
import static de.vanita5.twittnuker.util.Utils.hasAutoRefreshAccounts;
import static de.vanita5.twittnuker.util.Utils.isBatteryOkay;
import static de.vanita5.twittnuker.util.Utils.isNetworkAvailable;
import static de.vanita5.twittnuker.util.Utils.shouldStopAutoRefreshOnBatteryLow;

public class RefreshService extends Service implements Constants {

    @Inject
    SharedPreferencesWrapper mPreferences;

    private AlarmManager mAlarmManager;
    @Inject
    AsyncTwitterWrapper mTwitterWrapper;
    private PendingIntent mPendingRefreshHomeTimelineIntent, mPendingRefreshMentionsIntent,
            mPendingRefreshDirectMessagesIntent, mPendingRefreshTrendsIntent;

    private final BroadcastReceiver mStateReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(final Context context, final Intent intent) {
            final String action = intent.getAction();
            if (BuildConfig.DEBUG) {
                Log.d(LOGTAG, String.format("Refresh service received action %s", action));
            }
            if (BROADCAST_RESCHEDULE_HOME_TIMELINE_REFRESHING.equals(action)) {
                rescheduleHomeTimelineRefreshing();
            } else if (BROADCAST_RESCHEDULE_MENTIONS_REFRESHING.equals(action)) {
                rescheduleMentionsRefreshing();
            } else if (BROADCAST_RESCHEDULE_DIRECT_MESSAGES_REFRESHING.equals(action)) {
                rescheduleDirectMessagesRefreshing();
            } else if (BROADCAST_RESCHEDULE_TRENDS_REFRESHING.equals(action)) {
                rescheduleTrendsRefreshing();
            } else if (isAutoRefreshAllowed()) {
                if (BROADCAST_REFRESH_HOME_TIMELINE.equals(action)) {
                    if (!isHomeTimelineRefreshing()) {
                        mTwitterWrapper.getHomeTimelineAsync(new SimpleRefreshTaskParam() {
                            private AccountKey[] accountIds;

                            @NonNull
                            @Override
                            public AccountKey[] getAccountKeys() {
                                if (accountIds != null) return accountIds;
                                final AccountPreferences[] prefs = AccountPreferences.getAccountPreferences(context,
                                        DataStoreUtils.getAccountKeys(context));
                                return accountIds = getRefreshableIds(prefs, HomeRefreshableFilter.INSTANCE);
                            }

                            @Nullable
                            @Override
                            public long[] getSinceIds() {
                                return DataStoreUtils.getNewestStatusIds(context,
                                        Statuses.CONTENT_URI, getAccountKeys());
                            }
                        });
                    }
                } else if (BROADCAST_REFRESH_NOTIFICATIONS.equals(action)) {
                    mTwitterWrapper.getActivitiesAboutMeAsync(new SimpleRefreshTaskParam() {
                        private AccountKey[] accountIds;

                        @NonNull
                        @Override
                        public AccountKey[] getAccountKeys() {
                            if (accountIds != null) return accountIds;
                            final AccountPreferences[] prefs = AccountPreferences.getAccountPreferences(context,
                                    DataStoreUtils.getAccountKeys(context));
                            return accountIds = getRefreshableIds(prefs, MentionsRefreshableFilter.INSTANCE);
                        }

                        @Nullable
                        @Override
                        public long[] getSinceIds() {
                            return DataStoreUtils.getNewestActivityMaxPositions(context,
                                    Activities.AboutMe.CONTENT_URI, getAccountKeys());
                        }
                    });
                } else if (BROADCAST_REFRESH_DIRECT_MESSAGES.equals(action)) {
                    if (!isReceivedDirectMessagesRefreshing()) {
                        mTwitterWrapper.getReceivedDirectMessagesAsync(new SimpleRefreshTaskParam() {
                            private AccountKey[] accountIds;

                            @NonNull
                            @Override
                            public AccountKey[] getAccountKeys() {
                                if (accountIds != null) return accountIds;
                                final AccountPreferences[] prefs = AccountPreferences.getAccountPreferences(context,
                                        DataStoreUtils.getAccountKeys(context));
                                return accountIds = getRefreshableIds(prefs, MessagesRefreshableFilter.INSTANCE);
                            }

                            @Nullable
                            @Override
                            public long[] getSinceIds() {
                                return DataStoreUtils.getNewestMessageIds(context,
                                        DirectMessages.Inbox.CONTENT_URI, getAccountKeys());
                            }
                        });
                    }
                } else if (BROADCAST_REFRESH_TRENDS.equals(action)) {
                    final AccountPreferences[] prefs = AccountPreferences.getAccountPreferences(context,
                            DataStoreUtils.getAccountKeys(context));
                    final AccountKey[] refreshIds = getRefreshableIds(prefs, TrendsRefreshableFilter.INSTANCE);
                    if (BuildConfig.DEBUG) {
                        Log.d(LOGTAG, String.format("Auto refreshing trends for %s", Arrays.toString(refreshIds)));
                    }
                    if (!isLocalTrendsRefreshing()) {
                        getLocalTrends(refreshIds);
                    }
                }
            }
        }

    };

    /*
    private final BroadcastReceiver mPowerStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //
        }
    };
     */

    @Override
    public IBinder onBind(final Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        GeneralComponentHelper.build(this).inject(this);
        mAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        mPendingRefreshHomeTimelineIntent = PendingIntent.getBroadcast(this, 0, new Intent(
                BROADCAST_REFRESH_HOME_TIMELINE), 0);
        mPendingRefreshMentionsIntent = PendingIntent.getBroadcast(this, 0, new Intent(BROADCAST_REFRESH_NOTIFICATIONS), 0);
        mPendingRefreshDirectMessagesIntent = PendingIntent.getBroadcast(this, 0, new Intent(
                BROADCAST_REFRESH_DIRECT_MESSAGES), 0);
        mPendingRefreshTrendsIntent = PendingIntent.getBroadcast(this, 0, new Intent(BROADCAST_REFRESH_TRENDS), 0);
        final IntentFilter refreshFilter = new IntentFilter(BROADCAST_NOTIFICATION_DELETED);
        refreshFilter.addAction(BROADCAST_REFRESH_HOME_TIMELINE);
        refreshFilter.addAction(BROADCAST_REFRESH_NOTIFICATIONS);
        refreshFilter.addAction(BROADCAST_REFRESH_DIRECT_MESSAGES);
        refreshFilter.addAction(BROADCAST_RESCHEDULE_HOME_TIMELINE_REFRESHING);
        refreshFilter.addAction(BROADCAST_RESCHEDULE_MENTIONS_REFRESHING);
        refreshFilter.addAction(BROADCAST_RESCHEDULE_DIRECT_MESSAGES_REFRESHING);
        registerReceiver(mStateReceiver, refreshFilter);
        /*
        final IntentFilter batteryFilter = new IntentFilter();
        batteryFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        batteryFilter.addAction(Intent.ACTION_BATTERY_OKAY);
        batteryFilter.addAction(Intent.ACTION_BATTERY_LOW);
        batteryFilter.addAction(Intent.ACTION_POWER_CONNECTED);
        batteryFilter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        registerReceiver(mPowerStateReceiver, batteryFilter);
        PowerStateReceiver.setServiceReceiverStarted(true);
        */
        if (hasAutoRefreshAccounts(this)) {
            startAutoRefresh();
        } else {
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        //PowerStateReceiver.setServiceReceiverStarted(false);
        //unregisterReceiver(mPowerStateReceiver);
        unregisterReceiver(mStateReceiver);
        if (hasAutoRefreshAccounts(this)) {
            // Auto refresh enabled, so I will try to start service after it was
            // stopped.
            startService(new Intent(this, getClass()));
        }
        super.onDestroy();
    }

    protected boolean isAutoRefreshAllowed() {
        return isNetworkAvailable(this) && (isBatteryOkay(this) || !shouldStopAutoRefreshOnBatteryLow(this));
    }

    private void getLocalTrends(final AccountKey[] accountIds) {
        final AccountKey account_id = getDefaultAccountKey(this);
        final int woeid = mPreferences.getInt(KEY_LOCAL_TRENDS_WOEID, 1);
        mTwitterWrapper.getLocalTrendsAsync(account_id, woeid);
    }

    private AccountKey[] getRefreshableIds(final AccountPreferences[] prefs, final RefreshableAccountFilter filter) {
        if (prefs == null) return null;
        final AccountKey[] temp = new AccountKey[prefs.length];
        int i = 0;
        for (final AccountPreferences pref : prefs) {
            if (pref.isAutoRefreshEnabled() && filter.isRefreshable(pref)) {
                temp[i++] = pref.getAccountKey();
            }
        }
        final AccountKey[] result = new AccountKey[i];
        System.arraycopy(temp, 0, result, 0, i);
        return result;
    }

    private long getRefreshInterval() {
        if (mPreferences == null) return 0;
        final int prefValue = NumberUtils.toInt(mPreferences.getString(KEY_REFRESH_INTERVAL, DEFAULT_REFRESH_INTERVAL), -1);
        return Math.max(prefValue, 3) * 60 * 1000;
    }

    private boolean isHomeTimelineRefreshing() {
        return mTwitterWrapper.isHomeTimelineRefreshing();
    }

    private boolean isLocalTrendsRefreshing() {
        return mTwitterWrapper.isLocalTrendsRefreshing();
    }

    private boolean isReceivedDirectMessagesRefreshing() {
        return mTwitterWrapper.isReceivedDirectMessagesRefreshing();
    }

    private void rescheduleDirectMessagesRefreshing() {
        mAlarmManager.cancel(mPendingRefreshDirectMessagesIntent);
        final long refreshInterval = getRefreshInterval();
        if (refreshInterval > 0) {
            mAlarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + refreshInterval,
                    refreshInterval, mPendingRefreshDirectMessagesIntent);
        }
    }

    private void rescheduleHomeTimelineRefreshing() {
        mAlarmManager.cancel(mPendingRefreshHomeTimelineIntent);
        final long refreshInterval = getRefreshInterval();
        if (refreshInterval > 0) {
            mAlarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + refreshInterval,
                    refreshInterval, mPendingRefreshHomeTimelineIntent);
        }
    }

    private void rescheduleMentionsRefreshing() {
        mAlarmManager.cancel(mPendingRefreshMentionsIntent);
        final long refreshInterval = getRefreshInterval();
        if (refreshInterval > 0) {
            mAlarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + refreshInterval,
                    refreshInterval, mPendingRefreshMentionsIntent);
        }
    }

    private void rescheduleTrendsRefreshing() {
        mAlarmManager.cancel(mPendingRefreshTrendsIntent);
        final long refreshInterval = getRefreshInterval();
        if (refreshInterval > 0) {
            mAlarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + refreshInterval,
                    refreshInterval, mPendingRefreshTrendsIntent);
        }
    }

    private boolean startAutoRefresh() {
        stopAutoRefresh();
        final long refreshInterval = getRefreshInterval();
        if (refreshInterval <= 0) return false;
        rescheduleHomeTimelineRefreshing();
        rescheduleMentionsRefreshing();
        rescheduleDirectMessagesRefreshing();
        rescheduleTrendsRefreshing();
        return true;
    }

    private void stopAutoRefresh() {
        mAlarmManager.cancel(mPendingRefreshHomeTimelineIntent);
        mAlarmManager.cancel(mPendingRefreshMentionsIntent);
        mAlarmManager.cancel(mPendingRefreshDirectMessagesIntent);
        mAlarmManager.cancel(mPendingRefreshTrendsIntent);
    }

    private interface RefreshableAccountFilter {
        boolean isRefreshable(AccountPreferences pref);
    }

    private static class HomeRefreshableFilter implements RefreshableAccountFilter {
        public static final RefreshableAccountFilter INSTANCE = new HomeRefreshableFilter();

        @Override
        public boolean isRefreshable(final AccountPreferences pref) {
            return pref.isAutoRefreshHomeTimelineEnabled();
        }
    }

    private static class MentionsRefreshableFilter implements RefreshableAccountFilter {

        static final RefreshableAccountFilter INSTANCE = new MentionsRefreshableFilter();

        @Override
        public boolean isRefreshable(final AccountPreferences pref) {
            return pref.isAutoRefreshMentionsEnabled();
        }

    }

    private static class MessagesRefreshableFilter implements RefreshableAccountFilter {
        public static final RefreshableAccountFilter INSTANCE = new MentionsRefreshableFilter();

        @Override
        public boolean isRefreshable(final AccountPreferences pref) {
            return pref.isAutoRefreshDirectMessagesEnabled();
        }
    }

    private static class TrendsRefreshableFilter implements RefreshableAccountFilter {
        public static final RefreshableAccountFilter INSTANCE = new TrendsRefreshableFilter();

        @Override
        public boolean isRefreshable(final AccountPreferences pref) {
            return pref.isAutoRefreshTrendsEnabled();
        }
    }
}