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

package de.vanita5.twittnuker.fragment;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.mariotaku.restfu.http.Endpoint;
import org.xbill.DNS.ResolverConfig;

import de.vanita5.twittnuker.R;
import de.vanita5.twittnuker.api.twitter.Twitter;
import de.vanita5.twittnuker.api.twitter.TwitterException;
import de.vanita5.twittnuker.api.twitter.model.Paging;
import de.vanita5.twittnuker.model.ParcelableCredentials;
import de.vanita5.twittnuker.util.DataStoreUtils;
import de.vanita5.twittnuker.util.SharedPreferencesWrapper;
import de.vanita5.twittnuker.util.TwitterAPIFactory;
import de.vanita5.twittnuker.util.dagger.DependencyHolder;
import de.vanita5.twittnuker.util.net.TwidereDns;

import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

import okhttp3.Dns;

public class NetworkDiagnosticsFragment extends BaseFragment {

    private TextView mLogTextView;
    private Button mStartDiagnosticsButton;


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mStartDiagnosticsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLogTextView.setText(null);
                new DiagnosticsTask(NetworkDiagnosticsFragment.this).execute();
            }
        });
        mLogTextView.setMovementMethod(ScrollingMovementMethod.getInstance());
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mStartDiagnosticsButton = (Button) view.findViewById(R.id.start_diagnostics);
        mLogTextView = (TextView) view.findViewById(R.id.log_text);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_network_diagnostics, container, false);
    }

    private void appendMessage(String message) {
        mLogTextView.append(message);
    }

    static class DiagnosticsTask extends AsyncTask<Object, LogText, Object> {

        private final WeakReference<NetworkDiagnosticsFragment> mFragmentRef;

        private final Context mContext;
        private final ConnectivityManager mConnectivityManager;

        DiagnosticsTask(NetworkDiagnosticsFragment fragment) {
            mFragmentRef = new WeakReference<>(fragment);
            mContext = fragment.getActivity().getApplicationContext();
            mConnectivityManager = (ConnectivityManager)
                    mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        }

        @Override
        protected Object doInBackground(Object... params) {
            publishProgress(new LogText("Basic system information: "));
            publishProgress(new LogText(String.valueOf(mContext.getResources().getConfiguration())));
            publishProgress(LogText.LINEBREAK, LogText.LINEBREAK);
            publishProgress(new LogText("Active network info: "));
            publishProgress(new LogText(String.valueOf(mConnectivityManager.getActiveNetworkInfo())));
            publishProgress(LogText.LINEBREAK, LogText.LINEBREAK);
            publishProgress(new LogText("**** NOTICE ****"));
            publishProgress(LogText.LINEBREAK, LogText.LINEBREAK);
            publishProgress(new LogText("Text below may have personal information, BE CAREFUL TO MAKE IT PUBLIC"));
            publishProgress(LogText.LINEBREAK, LogText.LINEBREAK);
            DependencyHolder holder = DependencyHolder.get(mContext);
            final Dns dns = holder.getDns();
            final SharedPreferencesWrapper prefs = holder.getPreferences();
            publishProgress(new LogText("Network preferences"), LogText.LINEBREAK);
            publishProgress(new LogText("using_resolver: " + prefs.getBoolean(KEY_BUILTIN_DNS_RESOLVER)), LogText.LINEBREAK);
            publishProgress(new LogText("tcp_dns_query: " + prefs.getBoolean(KEY_TCP_DNS_QUERY)), LogText.LINEBREAK);
            publishProgress(new LogText("dns_server: " + prefs.getString(KEY_DNS_SERVER, null)), LogText.LINEBREAK);
            publishProgress(LogText.LINEBREAK);
            publishProgress(new LogText("System DNS servers"), LogText.LINEBREAK);


            final String[] servers = ResolverConfig.getCurrentConfig().servers();
            if (servers != null) {
                publishProgress(new LogText(Arrays.toString(servers)));
            } else {
                publishProgress(new LogText("null"));
            }
            publishProgress(LogText.LINEBREAK, LogText.LINEBREAK);

            for (long accountId : DataStoreUtils.getAccountIds(mContext)) {
                final ParcelableCredentials credentials = ParcelableCredentials.getCredentials(mContext, accountId);
                final Twitter twitter = TwitterAPIFactory.getTwitterInstance(mContext, accountId, false);
                if (credentials == null || twitter == null) continue;
                publishProgress(new LogText("Testing connection for account " + accountId));
                publishProgress(LogText.LINEBREAK);
                publishProgress(new LogText("api_url_format: " + credentials.api_url_format), LogText.LINEBREAK);
                publishProgress(new LogText("same_oauth_signing_url: " + credentials.same_oauth_signing_url), LogText.LINEBREAK);
                publishProgress(new LogText("auth_type: " + credentials.auth_type));

                publishProgress(LogText.LINEBREAK, LogText.LINEBREAK);

                publishProgress(new LogText("Testing DNS functionality"));
                publishProgress(LogText.LINEBREAK);
                final Endpoint endpoint = TwitterAPIFactory.getEndpoint(credentials, Twitter.class);
                final Uri uri = Uri.parse(endpoint.getUrl());
                final String host = uri.getHost();
                if (host != null) {
                    testDns(dns, host);
                    testNativeLookup(host);
                } else {
                    publishProgress(new LogText("API URL format is invalid"));
                    publishProgress(LogText.LINEBREAK);
                }

                publishProgress(LogText.LINEBREAK);

                publishProgress(new LogText("Testing API functionality"));
                publishProgress(LogText.LINEBREAK);
                testTwitter("verify_credentials", twitter, new TwitterTest() {
                    @Override
                    public void execute(Twitter twitter) throws TwitterException {
                        twitter.verifyCredentials();
                    }
                });
                testTwitter("get_home_timeline", twitter, new TwitterTest() {
                    @Override
                    public void execute(Twitter twitter) throws TwitterException {
                        twitter.getHomeTimeline(new Paging().count(1));
                    }
                });
                publishProgress(LogText.LINEBREAK);
            }

            publishProgress(LogText.LINEBREAK);

            publishProgress(new LogText("Testing common host names"));
            publishProgress(LogText.LINEBREAK, LogText.LINEBREAK);

            testDns(dns, "www.google.com");
            testNativeLookup("www.google.com");
            publishProgress(LogText.LINEBREAK);
            testDns(dns, "github.com");
            testNativeLookup("github.com");
            publishProgress(LogText.LINEBREAK);
            testDns(dns, "twitter.com");
            testNativeLookup("twitter.com");

            return null;
        }

        private void testDns(Dns dns, final String host) {
            publishProgress(new LogText(String.format("Lookup %s...", host)));
            try {
                final long start = SystemClock.uptimeMillis();
                if (dns instanceof TwidereDns) {
                    publishProgress(new LogText(String.valueOf(((TwidereDns) dns).lookupResolver(host))));
                } else {
                    publishProgress(new LogText(String.valueOf(dns.lookup(host))));
                }
                publishProgress(new LogText(String.format(" OK (%d ms)", SystemClock.uptimeMillis() - start)));
            } catch (UnknownHostException e) {
                publishProgress(new LogText("ERROR: " + e.getMessage()));
            }
            publishProgress(LogText.LINEBREAK);
        }

        private void testNativeLookup(final String host) {
            publishProgress(new LogText(String.format("Native lookup %s...", host)));
            try {
                final long start = SystemClock.uptimeMillis();
                publishProgress(new LogText(Arrays.toString(InetAddress.getAllByName(host))));
                publishProgress(new LogText(String.format(" OK (%d ms)", SystemClock.uptimeMillis() - start)));
            } catch (UnknownHostException e) {
                publishProgress(new LogText("ERROR: " + e.getMessage()));
            }
            publishProgress(LogText.LINEBREAK);
        }

        private void testTwitter(String name, Twitter twitter, TwitterTest test) {
            publishProgress(new LogText(String.format("Testing %s...", name)));
            try {
                final long start = SystemClock.uptimeMillis();
                test.execute(twitter);
                publishProgress(new LogText(String.format("OK (%d ms)", SystemClock.uptimeMillis() - start)));
            } catch (TwitterException e) {
                publishProgress(new LogText("ERROR: " + e.getMessage()));
            }
            publishProgress(LogText.LINEBREAK);
        }

        interface TwitterTest {
            void execute(Twitter twitter) throws TwitterException;
        }


        @Override
        protected void onProgressUpdate(LogText... values) {
            NetworkDiagnosticsFragment fragment = mFragmentRef.get();
            if (fragment == null) return;
            for (LogText value : values) {
                fragment.appendMessage(value.message);
            }
        }

        @Override
        protected void onPreExecute() {
            NetworkDiagnosticsFragment fragment = mFragmentRef.get();
            if (fragment == null) return;
            fragment.diagStart();
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Object o) {
            NetworkDiagnosticsFragment fragment = mFragmentRef.get();
            if (fragment == null) return;
            fragment.logReady();
            super.onPostExecute(o);
        }
    }

    private void diagStart() {
        mStartDiagnosticsButton.setText(R.string.please_wait);
        mStartDiagnosticsButton.setEnabled(false);
    }

    private void logReady() {
        mStartDiagnosticsButton.setText(R.string.send);
        mStartDiagnosticsButton.setEnabled(true);
        mStartDiagnosticsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Twittnuker Network Diagnostics");
                intent.putExtra(android.content.Intent.EXTRA_TEXT, mLogTextView.getText());
                startActivity(Intent.createChooser(intent, getString(R.string.send)));
            }
        });
    }

    static class LogText {
        static final LogText LINEBREAK = new LogText("\n");
        String message;
        int state;

        LogText(String message, int state) {
            this.message = message;
            this.state = state;
        }

        LogText(String message) {
            this.message = message;
        }
    }

}