/*
 * Twittnuker - Twitter client for Android
 *
 * Copyright (C) 2013-2017 vanita5 <mail@vanit.as>
 *
 * This program incorporates a modified version of Twidere.
 * Copyright (C) 2012-2017 Mariotaku Lee <mariotaku.lee@gmail.com>
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

package de.vanita5.twittnuker.library.fanfou.callback;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.bluelinelabs.logansquare.LoganSquare;

import de.vanita5.twittnuker.library.MicroBlogException;
import de.vanita5.twittnuker.library.fanfou.model.FanfouStreamObject;
import de.vanita5.twittnuker.library.twitter.model.Status;
import de.vanita5.twittnuker.library.twitter.model.User;
import de.vanita5.twittnuker.library.util.CRLFLineReader;
import org.mariotaku.restfu.callback.RawCallback;
import org.mariotaku.restfu.http.HttpResponse;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;

@SuppressWarnings({"WeakerAccess"})
public abstract class FanfouUserStreamCallback implements RawCallback<MicroBlogException> {

    private boolean connected;

    private boolean disconnected;

    @Override
    public final void result(@NonNull final HttpResponse response) throws MicroBlogException, IOException {
        if (!response.isSuccessful()) {
            final MicroBlogException cause = new MicroBlogException();
            cause.setHttpResponse(response);
            onException(cause);
            return;
        }
        final CRLFLineReader reader = new CRLFLineReader(new InputStreamReader(response.getBody().stream(), "UTF-8"));
        try {
            for (String line; (line = reader.readLine()) != null && !disconnected; ) {
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
                if (!connected) {
                    onConnected();
                    connected = true;
                }
                if (TextUtils.isEmpty(line)) continue;
                FanfouStreamObject object = LoganSquare.parse(line, FanfouStreamObject.class);
                if (!handleEvent(object, line)) {
                    onUnhandledEvent(object.getEvent(), line);
                }
            }
        } catch (IOException e) {
            onException(e);
        } finally {
            reader.close();
        }
    }

    @Override
    public final void error(@NonNull final MicroBlogException cause) {
        onException(cause);
    }

    public final void disconnect() {
        disconnected = true;
    }

    private boolean handleEvent(final FanfouStreamObject object, final String json) throws IOException {
        switch (object.getEvent()) {
            case "message.create": {
                return onStatusCreation(object.getCreatedAt(), object.getSource(),
                        object.getTarget(), object.getObject(Status.class));
            }
        }
        return false;
    }

    protected abstract boolean onConnected();

    protected abstract boolean onDisconnect(int code, String reason);

    protected abstract boolean onException(@NonNull Throwable ex);

    protected abstract boolean onStatusCreation(@NonNull Date createdAt, @NonNull User source,
                                                @Nullable User target, @NonNull Status object);

    protected abstract void onUnhandledEvent(@NonNull String event, @NonNull String json)
            throws IOException;
}